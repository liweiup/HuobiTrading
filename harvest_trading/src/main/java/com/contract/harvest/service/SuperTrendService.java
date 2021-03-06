package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.contract.harvest.common.OpenInfo;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.entity.Candlestick;
import com.contract.harvest.entity.CandlestickData;
import com.contract.harvest.tools.*;
import com.huobi.api.enums.DirectionEnum;
import com.huobi.api.enums.OffsetEnum;
import com.huobi.api.exception.ApiException;
import com.huobi.api.request.trade.ContractOrderRequest;
import com.huobi.api.response.account.ContractPositionInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author liwei
 */
@Slf4j
@Service
public class SuperTrendService {

    @Resource
    DeliveryDataService deliveryDataService;
    @Resource
    DataService dataService;
    @Resource RedisService redisService;
    @Resource CacheService cacheService;
    //是否有订单在处理
    public volatile Map<String,Integer> mapFlag = new HashMap<>();

    public void trading(String symbol) throws InterruptedException, NullPointerException {
        String symbolFlag = symbol + PubConst.DEFAULT_CS;
        if (mapFlag.get(symbol) != null && mapFlag.get(symbol) == 1) {
            log.info("..............."+ symbolFlag +"有订单等待成交...............");
            return;
        }
        //开仓参数
        OpenInfo openInfo = dataService.getOpenInfo(symbolFlag);
        if (openInfo == null) {
            log.info("..............."+ symbolFlag +"未设置开仓参数...............");
            return;
        }
        double atrMultiplier = openInfo.getAtrMultiplier(),
                limitPercent = openInfo.getLimitPercent(),
                stopPercent = openInfo.getStopPercent();
        stopPercent = dataService.getStopPercent(symbol,stopPercent,"DELI");
        int atrLen = openInfo.getAtrLen(),topicIndex = openInfo.getTopicIndex();
        List<Candlestick.DataBean> candlestickList = dataService.getKlineList(symbolFlag,topicIndex,0);
        //kline的列值
        CandlestickData tickColumnData = new CandlestickData(candlestickList);
        //计算atr
        double[] atr = IndexCalculation.volatilityIndicators(tickColumnData.open,tickColumnData.high,tickColumnData.low,tickColumnData.close,tickColumnData.id,atrLen,"atr");
        //获取休息状态
        boolean timeFlag = "0".equals(cacheService.getTimeFlag(symbolFlag));
        //计算可以做多的k线
        List<Long> klineIdList = IndexCalculation.superTrend(tickColumnData.hl2,atr,tickColumnData.close,tickColumnData.id,atrMultiplier,1);
        if (klineIdList.size() == 0) {
            return;
        }
        int dateIndex = PubConst.DATE_INDEX[topicIndex];
        //时间周期序列
        List<Long> dateList = TakeDate.getDateList(dateIndex);
        //做多条件
        long lastKlineId = klineIdList.get(klineIdList.size() - 1);
        long lastDateId = dateList.get(dateList.size() - 1);
        long secondTimestamp = FormatParam.getSecondTimestamp();
        //k线秒数
        int klineSecond = PubConst.DATE_INDEX[topicIndex] * 60;
        //这条k线等于当前时间最近的周期 或者 等于最近的时间减去一根k线的时间
        boolean tradingFlag = lastKlineId == lastDateId || lastDateId - lastKlineId == klineSecond;
        //信号k线结束的前10秒,后80秒之内交易
        long flagTimeNum = klineSecond + lastKlineId - secondTimestamp;
        boolean klineTimeFlag = (flagTimeNum > 0 && flagTimeNum < PubConst.PRE_SECOND) || (flagTimeNum < 0 && Math.abs(flagTimeNum) < PubConst.LATER_SECOND);
        //获取最后一根可以做多k线的数据
        Candlestick.DataBean candlestickRow = candlestickList.stream().filter(c->c.getId().equals(lastKlineId)).collect(Collectors.toList()).get(0);
        double tickRowHl2 = ValueAccessor.hl2(candlestickRow);
        //最后一根k线的数据
        Candlestick.DataBean candlestickLastRow = candlestickList.get(candlestickList.size()-1);
        double tickLastRowHl2 = ValueAccessor.hl2(candlestickLastRow);
        //信号k线结束的8分钟之内，当前价格小于k线价格交易
        boolean priceSignalFlag = Arith.compareNum(tickRowHl2,tickLastRowHl2);
        boolean prieKlineTimeFlag = flagTimeNum < 0 && Math.abs(flagTimeNum) < PubConst.LAST_SECOND;
        //当前持仓量
        List<ContractPositionInfoResponse.DataBean> contractPositionInfo = deliveryDataService.getContractPositionInfo(symbol, PubConst.DEFAULT_CS,"");
        int volume = contractPositionInfo != null && contractPositionInfo.size() > 0 ? contractPositionInfo.get(0).getVolume().intValue() : 0;
        //最大持仓
        int maxVolume = deliveryDataService.getMaxOpenVolume(symbol);
        //可开仓量
        int openVolume = maxVolume - volume;
        boolean volumeFlag = volume < maxVolume && openVolume > 5;
        //信号确认
        boolean affirmTradingFlag = (tradingFlag && klineTimeFlag) || (priceSignalFlag && prieKlineTimeFlag);
        DirectionEnum direction = DirectionEnum.BUY;
        //交易
        if (affirmTradingFlag && volumeFlag && timeFlag) {
            log.info("...生成订单....."+"ing："+(tradingFlag && klineTimeFlag) + "------ed:"+(priceSignalFlag && prieKlineTimeFlag));
            //生成订单
            ContractOrderRequest order = deliveryDataService.getPlanOrder(symbol,PubConst.DEFAULT_CS,"", OffsetEnum.OPEN, direction,openVolume,stopPercent,limitPercent);
            redisService.lpush(CacheService.WAIT_ORDER_QUEUE + symbol, JSON.toJSONString(order));
            //订阅通知
            redisService.convertAndSend("order_queue","hadleQueueOrder:" + symbol);
            //加锁
            mapFlag.put(symbol,1);
        }
        cacheService.inform("DELI-trading-"+symbolFlag,"...............正在运行...............");
    }
    /**
     * 处理订单
     */
    public void hadleQueueOrder(String symbol) throws ApiException,InterruptedException {
        //队列是否有订单等待处理
        Long queLen = redisService.getListLen(CacheService.WAIT_ORDER_QUEUE + symbol);
        if (queLen == 0) {
            log.info("...............订单队列是空的...............");
            return;
        }
        //获取订单
        String orderStr = redisService.getListByIndex(CacheService.WAIT_ORDER_QUEUE+symbol, (long) 0);
        ContractOrderRequest order = JSON.parseObject(orderStr, ContractOrderRequest.class);
        try {
            deliveryDataService.handleOrder(order);
            //刷新仓位
            deliveryDataService.setContractPositionInfo(symbol);
        }catch (ApiException e) {
            log.error(e.getMessage());
        }
        //移除订单队列
        redisService.leftPop(CacheService.WAIT_ORDER_QUEUE + symbol);
        //订单id放入set
        redisService.addSet(CacheService.ORDER_DEAL_CLIENTID + symbol,order.getClientOrderId().toString());
        //释放锁
        mapFlag.put(symbol,0);
        log.info("...............订单处理结束...释放锁...............");
    }
}
