package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.entity.Candlestick;
import com.contract.harvest.entity.CandlestickData;
import com.contract.harvest.tools.FormatParam;
import com.contract.harvest.tools.IndexCalculation;
import com.contract.harvest.tools.TakeDate;
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

/**
 * @author liwei
 */
@Slf4j
@Service
public class SuperTrendService {

    @Resource
    DeliveryDataService deliveryDataService;
    @Resource RedisService redisService;
    @Resource CacheService cacheService;
    //止损点
    private final static double STOP_PERCENT = 0.06;
    //止盈点
    private final static double LIMIT_PERCENT = 0.03;
    //是否有订单在处理
    public volatile Map<String,Integer> mapFlag = new HashMap<>();

    public void trading(String symbol) throws InterruptedException, NullPointerException {
        if (mapFlag.get(symbol) != null && mapFlag.get(symbol) == 1) {
            log.info("..............."+ symbol +"有订单等待成交...............");
            return;
        }
        String symbolFlag = symbol + PubConst.DEFAULT_CS;
        List<Candlestick.DataBean> candlestickList = deliveryDataService.getKlineList(symbolFlag,PubConst.TOPIC_INDEX);
        //kline的列值
        CandlestickData tickColumnData = new CandlestickData(candlestickList);
        //计算atr
        double[] atr = IndexCalculation.volatilityIndicators(tickColumnData.open,tickColumnData.high,tickColumnData.low,tickColumnData.close,tickColumnData.id,14,"atr");
        //计算可以做空的k线
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
        //k线结束的前30秒,后80秒之内交易
        long flagTimeNum = (PubConst.DATE_INDEX[PubConst.TOPIC_INDEX] * 60) + lastKlineId - secondTimestamp;
        boolean klineTimeFlag = (flagTimeNum > 0 && flagTimeNum < 30) || (flagTimeNum < 0 && Math.abs(flagTimeNum) < 80);
//        if (klineTimeFlag) {
//            log.info(lastDateId+"---"+secondTimestamp+"----klineTimeFlag-----"+ true);
//        }
        //当前持仓量
        List<ContractPositionInfoResponse.DataBean> contractPositionInfo = deliveryDataService.getContractPositionInfo(symbol, PubConst.DEFAULT_CS,"");
        int volume = contractPositionInfo != null && contractPositionInfo.size() > 0 ? contractPositionInfo.get(0).getVolume().intValue() : 0;
        //最大持仓
        int maxVolume = deliveryDataService.getMaxOpenVolume(symbol);
        //可开仓量
        int openVolume = maxVolume - volume;
        boolean volumeFlag = volume < maxVolume;
        //队列是否有订单等待处理
//        Long queLen = redisService.getListLen(CacheService.WAIT_ORDER_QUEUE + symbol);
//        if (queLen > 0) {
//            log.info("已有订单等待处理,币:" + symbol);
//            Thread.sleep(3000);
//            return;
//        }
        //交易
        if (tradingFlag && klineTimeFlag && volumeFlag) {
            log.info("...............生成订单...............");
            //生成订单
            ContractOrderRequest order = deliveryDataService.getPlanOrder(symbol,PubConst.DEFAULT_CS,"", OffsetEnum.OPEN, DirectionEnum.BUY,openVolume,STOP_PERCENT,LIMIT_PERCENT);
            redisService.lpush(CacheService.WAIT_ORDER_QUEUE + symbol, JSON.toJSONString(order));
            //订阅通知
            redisService.convertAndSend("order_queue","hadleQueueOrder:" + symbol);
            //加锁
            mapFlag.put(symbol,1);
        }
        cacheService.inform("...............正常运行...............");
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
        }catch (ApiException e) {
            log.error(e.getMessage());
        }
        //移除订单队列
        redisService.leftPop(CacheService.WAIT_ORDER_QUEUE + symbol);
        //订单id放入set
        redisService.addSet(CacheService.ORDER_DEAL_CLIENTID + symbol,order.getClientOrderId().toString());
        //刷新仓位
        deliveryDataService.setContractPositionInfo(symbol);
        //释放锁
        mapFlag.put(symbol,0);
    }
}
