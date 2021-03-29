package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.entity.Candlestick;
import com.contract.harvest.entity.CandlestickData;
import com.contract.harvest.tools.*;
import com.huobiswap.api.enums.DirectionEnum;
import com.huobiswap.api.enums.OffsetEnum;
import com.huobiswap.api.exception.ApiException;
import com.huobiswap.api.request.trade.SwapOrderRequest;
import com.huobiswap.api.response.account.SwapPositionInfoResponse;
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
        List<Candlestick.DataBean> candlestickList = dataService.getKlineList(symbol,PubConst.TOPIC_INDEX);
        //kline的列值
        CandlestickData tickColumnData = new CandlestickData(candlestickList);
        //计算atr
        double[] atr = IndexCalculation.volatilityIndicators(tickColumnData.open,tickColumnData.high,tickColumnData.low,tickColumnData.close,tickColumnData.id,14,"atr");
        //计算可以做多的k线
        List<Long> klineIdList = IndexCalculation.superTrend(tickColumnData.hl2,atr,tickColumnData.close,tickColumnData.id);
        if (klineIdList.size() == 0) {
            return;
        }
        int dateIndex = PubConst.DATE_INDEX[PubConst.TOPIC_INDEX];
        //时间周期序列
        List<Long> dateList = TakeDate.getDateList(dateIndex);
        //做空条件
        long lastKlineId = klineIdList.get(klineIdList.size() - 1);
        long lastDateId = dateList.get(dateList.size() - 1);
        long secondTimestamp = FormatParam.getSecondTimestamp();
        //如果最后一根k线可以做空 && 这条k线等于当前时间最近的周期
        boolean tradingFlag = lastKlineId == lastDateId;
        //信号k线结束的前10秒,后80秒之内交易
        long flagTimeNum = (PubConst.DATE_INDEX[PubConst.TOPIC_INDEX] * 60) + lastKlineId - secondTimestamp;
        boolean klineTimeFlag = (flagTimeNum > 0 && flagTimeNum < 10) || (flagTimeNum < 0 && Math.abs(flagTimeNum) < 60);
        //获取最后一根可以做多k线的数据
        Candlestick.DataBean candlestickRow = candlestickList.stream().filter(c->c.getId().equals(lastKlineId)).collect(Collectors.toList()).get(0);
        double tickRowHl2 = ValueAccessor.hl2(candlestickRow);
        //最后一根k线的数据
        Candlestick.DataBean candlestickLastRow = candlestickList.get(candlestickList.size()-1);
        double tickLastRowHl2 = ValueAccessor.hl2(candlestickLastRow);
        //信号k线结束的8分钟之内，当前价格小于k线价格交易
        boolean priceSignalFlag = Arith.compareNum(tickRowHl2,tickLastRowHl2);
        boolean prieKlineTimeFlag = flagTimeNum < 0 && Math.abs(flagTimeNum) < 480;
        //当前持仓量
        List<SwapPositionInfoResponse.DataBean> contractPositionInfo = swapDataService.getContractPositionInfo(symbol);
        int volume = contractPositionInfo != null && contractPositionInfo.size() > 0 ? contractPositionInfo.get(0).getVolume().intValue() : 0;
        //最大持仓
        int maxVolume = swapDataService.getMaxOpenVolume(symbol);
        //可开仓量
        int openVolume = maxVolume - volume;
        boolean volumeFlag = volume < maxVolume;
        //信号确认
        boolean affirmTradingFlag = (tradingFlag && klineTimeFlag) || (priceSignalFlag && prieKlineTimeFlag);
        //交易
        if (affirmTradingFlag && volumeFlag) {
            log.info("...SWAP-生成订单....."+"ing："+(tradingFlag && klineTimeFlag) + "------ed:"+(priceSignalFlag && prieKlineTimeFlag));
            //生成订单
            SwapOrderRequest order = swapDataService.getPlanOrder(symbol, OffsetEnum.OPEN, DirectionEnum.BUY,openVolume,PubConst.STOP_PERCENT,PubConst.LIMIT_PERCENT);
            redisService.lpush(CacheService.SWAP_WAIT_ORDER_QUEUE + symbol, JSON.toJSONString(order));
            //订阅通知
            redisService.convertAndSend("order_queue","swapHadleQueueOrder:" + symbol);
            //加锁
            mapFlag.put(symbol,1);
        }
        cacheService.inform("SWAP-trading","...............正常运行...............");
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
            swapDataService.handleOrder(order);
        }catch (ApiException e) {
            log.error(e.getMessage());
        }
        //移除订单队列
        redisService.leftPop(CacheService.SWAP_WAIT_ORDER_QUEUE + symbol);
        //订单id放入set
        redisService.addSet(CacheService.SWAP_ORDER_DEAL_CLIENTID + symbol,order.getClientOrderId().toString());
        //刷新仓位
        swapDataService.setContractPositionInfo(symbol);
        //释放锁
        mapFlag.put(symbol,0);
        log.info("...............SWAP-订单处理结束...释放锁...............");
    }

}
