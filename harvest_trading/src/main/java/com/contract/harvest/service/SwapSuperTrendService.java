package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.contract.harvest.common.OpenInfo;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.entity.Candlestick;
import com.contract.harvest.entity.CandlestickData;
import com.contract.harvest.tools.*;
import com.huobiswap.api.enums.DirectionEnum;
import com.huobiswap.api.enums.OffsetEnum;
import com.huobiswap.api.exception.ApiException;
import com.huobiswap.api.request.trade.SwapOrderRequest;
import com.huobiswap.api.response.account.SwapPositionInfoResponse;
import com.huobiswap.api.response.market.SwapContractInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author liwei
 */
@Slf4j
@Service
public class SwapSuperTrendService {

    @Resource
    SwapDataService swapDataService;
    @Resource
    DataService dataService;
    @Resource RedisService redisService;
    @Resource CacheService cacheService;
    //是否有订单在处理
    public volatile Map<String,Integer> mapFlag = new HashMap<>();

    public void trading(String symbol) throws InterruptedException, NullPointerException {
        if (mapFlag.get(symbol) != null && mapFlag.get(symbol) == 1) {
            log.info("...............SWAP-"+ symbol +"有订单等待成交...............");
            return;
        }
        //开仓参数
        OpenInfo openInfo = dataService.getOpenInfo(symbol);
        if (openInfo == null) {
            log.info("...............SWAP-"+ symbol +"未设置开仓参数...............");
            return;
        }
        double atrMultiplier = openInfo.getAtrMultiplier(),
                limitPercent = openInfo.getLimitPercent(),
                stopPercent = openInfo.getStopPercent();
        stopPercent = dataService.getStopPercent(symbol,stopPercent,"SWAP");
        int atrLen = openInfo.getAtrLen(),topicIndex = openInfo.getTopicIndex();
        List<Candlestick.DataBean> candlestickList = dataService.getKlineList(symbol,topicIndex,0);
        //kline的列值
        CandlestickData tickColumnData = new CandlestickData(candlestickList);
        //计算atr
        double[] atr = IndexCalculation.volatilityIndicators(tickColumnData.open,tickColumnData.high,tickColumnData.low,tickColumnData.close,tickColumnData.id,atrLen,"atr");
        //当前持仓量
        List<SwapPositionInfoResponse.DataBean> contractPositionInfo = swapDataService.getContractPositionInfo(symbol);
        int volume = contractPositionInfo != null && contractPositionInfo.size() > 0 ? contractPositionInfo.get(0).getVolume().intValue() : 0;
        int bf = Arith.compareEqualNum(stopPercent,0) && volume > 0 ? -1 : 1;
        //计算可以做多|空的k线
        List<Long> klineIdList = IndexCalculation.superTrend(tickColumnData.hl2,atr,tickColumnData.close,tickColumnData.id,atrMultiplier,bf);
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
        //如果最后一根k线可以做空 && 这条k线等于当前时间最近的周期
        boolean tradingFlag = lastKlineId == lastDateId  || lastDateId - lastKlineId == klineSecond;
        //获取休息状态
        boolean timeFlag = "0".equals(cacheService.getTimeFlag(symbol)) || bf == -1;
        //信号k线结束的前10秒,后80秒之内交易
        long flagTimeNum = klineSecond + lastKlineId - secondTimestamp;
        boolean klineTimeFlag = (flagTimeNum > 0 && flagTimeNum < PubConst.PRE_SECOND) || (flagTimeNum < 0 && Math.abs(flagTimeNum) < PubConst.LATER_SECOND);
        //获取最后一根可以做多|空k线的数据
        Candlestick.DataBean candlestickRow = candlestickList.stream().filter(c->c.getId().equals(lastKlineId)).collect(Collectors.toList()).get(0);
        double tickRowHl2 = ValueAccessor.hl2(candlestickRow);
        //最后一根k线的数据
        Candlestick.DataBean candlestickLastRow = candlestickList.get(candlestickList.size()-1);
        double tickLastRowHl2 = ValueAccessor.hl2(candlestickLastRow);
        //信号k线结束的8分钟之内，当前价格 小于|大于 k线价格交易
        boolean priceSignalFlag = bf == 1 ? Arith.compareNum(tickRowHl2,tickLastRowHl2) : Arith.compareNum(tickLastRowHl2,tickRowHl2);
        boolean prieKlineTimeFlag = flagTimeNum < 0 && Math.abs(flagTimeNum) < PubConst.LAST_SECOND;
        //最大持仓
        int maxVolume = swapDataService.getMaxOpenVolume(symbol);
        SwapContractInfoResponse.DataBean contractInfo = swapDataService.getContractInfo(symbol);
        maxVolume = swapDataService.getDealOpenVol(candlestickLastRow.getClose().doubleValue(),contractInfo.getContractSize().doubleValue(),maxVolume);
        //可开仓量
        int openVolume = maxVolume - volume;
        //可开仓量
        boolean volumeFlag = (volume < maxVolume  && openVolume > 5) || bf == -1;
        //信号确认
        boolean affirmTradingFlag = (tradingFlag && klineTimeFlag) || (priceSignalFlag && prieKlineTimeFlag);
        //开多仓
        boolean dealTradingFlag = affirmTradingFlag && volumeFlag && timeFlag;
        //交易
        if (dealTradingFlag) {
            SwapOrderRequest order = null;
            if (bf == 1) {
                log.info("...SWAP-生成订单开多....."+symbol+"ing："+(tradingFlag && klineTimeFlag) + "------ed:"+(priceSignalFlag && prieKlineTimeFlag));
                //生成订单
                order = swapDataService.getPlanOrder(symbol, OffsetEnum.OPEN, DirectionEnum.BUY,openVolume,stopPercent,limitPercent);
            }
            if (bf == -1) {
                log.info("...SWAP-生成订单平多....."+symbol+"ing："+(tradingFlag && klineTimeFlag) + "------ed:"+(priceSignalFlag && prieKlineTimeFlag));
                //平仓
                order = swapDataService.getPlanOrder(symbol, OffsetEnum.CLOSE, DirectionEnum.SELL,volume,0,0);
            }
            redisService.lpush(CacheService.SWAP_WAIT_ORDER_QUEUE + symbol, JSON.toJSONString(order));
            //订阅通知
            redisService.convertAndSend("order_queue","swapHadleQueueOrder:" + symbol);
            //加锁
            mapFlag.put(symbol,1);
        }
        cacheService.inform("SWAP-trading-" + symbol,"...............正常运行...............");
    }
    /**
     * 处理订单
     */
    public void hadleQueueOrder(String symbol) throws ApiException,InterruptedException {
        //队列是否有订单等待处理
        Long queLen = redisService.getListLen(CacheService.SWAP_WAIT_ORDER_QUEUE + symbol);
        if (queLen == 0) {
            log.info("...............SWAP-订单队列是空的...............");
            return;
        }
        //获取订单
        String orderStr = redisService.getListByIndex(CacheService.SWAP_WAIT_ORDER_QUEUE+symbol, (long) 0);
        SwapOrderRequest order = JSON.parseObject(orderStr, SwapOrderRequest.class);
        try {
            //处理订单
            swapDataService.handleOrder(order);
            //刷新仓位
            swapDataService.setContractPositionInfo(symbol);
        }catch (ApiException e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        //移除订单队列
        redisService.leftPop(CacheService.SWAP_WAIT_ORDER_QUEUE + symbol);
        //订单id放入set
        redisService.addSet(CacheService.SWAP_ORDER_DEAL_CLIENTID + symbol,order.getClientOrderId().toString());
        //释放锁
        mapFlag.put(symbol,0);
        log.info("...............SWAP-"+symbol+"订单处理结束...释放锁...............");
    }

}
