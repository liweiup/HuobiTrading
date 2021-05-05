package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.contract.harvest.common.Depth;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.common.Topic;
import com.contract.harvest.entity.HuobiSwapEntity;
import com.contract.harvest.service.inter.SwapServiceInter;
import com.contract.harvest.tools.Arith;
import com.huobiswap.api.enums.DirectionEnum;
import com.huobiswap.api.enums.OffsetEnum;
import com.huobiswap.api.exception.ApiException;
import com.huobiswap.api.request.trade.SwapOrderRequest;
import com.huobiswap.api.response.account.SwapPositionInfoResponse;
import com.huobiswap.api.response.market.SwapContractInfoResponse;
import com.huobiswap.api.response.trade.SwapMatchresultsResponse;
import com.huobiswap.api.response.trade.SwapOrderInfoResponse;
import com.huobiswap.api.response.trade.SwapOrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liwei
 */
@Slf4j
@Service
public class SwapDataService implements SwapServiceInter {

    @Resource
    private RedisService redisService;
    @Resource
    private DataService dataService;
    @Resource
    private HuobiSwapEntity huobiSwapEntity;
    @Resource
    private MailService mailService;

    @Override
    public SwapContractInfoResponse.DataBean getContractInfo(String contractCode) throws NullPointerException, ApiException {
        String contractInfo = huobiSwapEntity.getSwapContractInfo("","");
        SwapContractInfoResponse response = JSON.parseObject(contractInfo,SwapContractInfoResponse.class);
        List<SwapContractInfoResponse.DataBean> contractCodeData = response.getData();
        if (!"".equals(contractCode)) {
            contractCodeData = contractCodeData.stream().filter( f -> f.getContractCode().equals(contractCode)).collect(Collectors.toList());
        }
        return contractCodeData.size() > 0 ? contractCodeData.get(0) : null;
    }
    /**
     * 获取下单实例
     * @param contractCode 合约代码
     * @param offset open or close
     * @param direction 交易方向
     */
    @Override
    public SwapOrderRequest getPlanOrder(String contractCode, OffsetEnum offset, DirectionEnum direction, long dealVolume, double stopPercent, double limitPercent) throws InterruptedException, NullPointerException,ApiException {
        SwapContractInfoResponse.DataBean contractInfo = getContractInfo(contractCode);
        //获取最新的价格
        String depthSubKey = Topic.formatChannel(Topic.DEPTH_SUB,contractCode,PubConst.DEPTH_SUB_INDEX);
        Depth.TickBean depth = dataService.getBidAskPrice(depthSubKey).getTick();
        List<List<BigDecimal>> depthPriceVol = direction == DirectionEnum.BUY ? depth.getBids() : depth.getAsks();
        double price = depthPriceVol.get(0).get(0).doubleValue();
        if (direction == DirectionEnum.BUY) {
            price = Arith.add(price,contractInfo.getPriceTick().doubleValue());
        } else {
            price = Arith.sub(price,contractInfo.getPriceTick().doubleValue());
        }
        dealVolume = 0 == dealVolume ? PubConst.VOLUME : dealVolume;
        contractCode = contractInfo.getContractCode();
        SwapOrderRequest order =  SwapOrderRequest.builder()
                .contractCode(contractCode)
                .clientOrderId(dataService.getClientOrderId())
                .price(BigDecimal.valueOf(price))
                .volume(dealVolume)
                .direction(direction)
                .offset(offset)
                .leverRate(PubConst.LEVER_RATE)
                .orderPriceType(PubConst.ORDER_PRICE_TYPE)
                .build();
        //多仓止盈损
        BigDecimal stopPrice = null,limitPrice = null;
        if (direction == DirectionEnum.BUY) {
            stopPrice = BigDecimal.valueOf(Arith.mul(price,Arith.sub(1,stopPercent))).setScale(2,BigDecimal.ROUND_HALF_UP);
            limitPrice = BigDecimal.valueOf(Arith.mul(price,Arith.add(1,limitPercent))).setScale(2,BigDecimal.ROUND_HALF_UP);
        }
        //空仓止盈损
        if (direction == DirectionEnum.SELL) {
            stopPrice = BigDecimal.valueOf(Arith.mul(price,Arith.add(1,stopPercent))).setScale(2,BigDecimal.ROUND_HALF_UP);
            limitPrice = BigDecimal.valueOf(Arith.mul(price,Arith.sub(1,limitPercent))).setScale(2,BigDecimal.ROUND_HALF_UP);
        }
        //设置止盈 止损
        if (!Arith.compareEqualNum(stopPercent,0)) {
            order.setTpTriggerPrice(limitPrice);
            order.setTpOrderPrice(limitPrice);
            order.setTpOrderPriceType(PubConst.ORDER_STOPLIMIT_TYPE);
        }
        if (!Arith.compareEqualNum(limitPercent,0)) {
            order.setSlTriggerPrice(stopPrice);
            order.setSlOrderPrice(stopPrice);
            order.setSlOrderPriceType(PubConst.ORDER_STOPLIMIT_TYPE);
        }
        return order;
    }

    /**
     * 处理订单
     * @param order 订单实体
     */
    @Override
    public void handleOrder(SwapOrderRequest order) throws InterruptedException, NullPointerException, ApiException {
        String contractCode = order.getContractCode();
        Long clientOrderId = order.getClientOrderId();
        String orderInfoStr,orderIdStr ,contractOrder = "";
        SwapOrderInfoResponse.DataBean orderInfo = null;
        //订单成交信息key
        String orderInfoKey = CacheService.SWAP_ORDER_INFO + contractCode;
        //获取订单成交信息
        orderInfoStr = redisService.hashGet(orderInfoKey,clientOrderId.toString());
        if ("".equals(orderInfoStr)) {
            //下单-存放订单成交信息
            contractOrder = huobiSwapEntity.futureContractOrder(order);
            SwapOrderResponse contractOrderResponse = JSON.parseObject(contractOrder, SwapOrderResponse.class);
            orderIdStr = contractOrderResponse.getData().getOrderIdStr();
        } else {
            orderIdStr = JSONObject.parseObject(orderInfoStr).getString("orderIdStr");
        }
        if ("".equals(orderIdStr) || null == orderIdStr) {
            log.info("SWAP-订单id为空" + contractOrder);
            return;
        }
        int i = 0;
        while (i < PubConst.ORDER_TRY_NUM) {
            orderInfoStr = huobiSwapEntity.getcontractOrderInfo(orderIdStr,"",contractCode);
            SwapOrderInfoResponse orderInfoResponse = JSON.parseObject(orderInfoStr,SwapOrderInfoResponse.class);
            orderInfo = orderInfoResponse.getData().get(0);
            if (null == orderInfo) {
                continue;
            }
            log.info("SWAP-订单信息" + JSON.toJSONString(orderInfo) + "重试次数："+ i);
            //如果全部成交
            if (orderInfo.getStatus() == 6 || orderInfo.getStatus() == 7) {
                redisService.hashSet(orderInfoKey,clientOrderId.toString(),JSON.toJSONString(orderInfo));
                break;
            }
            Thread.sleep(3000L);
            i++;
        }
        int orderStatus = orderInfo.getStatus();
        int[] partSuccessArr = {4,5};
        if (orderStatus == 6) {
            log.info("SWAP-全部成交:"+orderInfo);
        } else if (Arrays.binarySearch(partSuccessArr,orderStatus) >= 0) {
            //撤单
            String contractCancel = huobiSwapEntity.swapCancelRequest(orderInfo.getOrderIdStr(),"",contractCode);
            log.info("SWAP-部分成交:"+orderInfo+"撤单信息:"+contractCancel);
        } else {
            //撤单
            String contractCancel = huobiSwapEntity.swapCancelRequest(orderInfo.getOrderIdStr(),"",contractCode);
            log.info("SWAP-撤单:"+orderInfo+"撤单信息:"+contractCancel);
        }
        int tradeVolume = orderInfo.getTradeVolume().intValue();
        if (tradeVolume > 0) {
            mailService.sendMail("SWAP-成功下单-成交量"+tradeVolume,"订单信息:"+JSON.toJSONString(orderInfo),"");
        }
    }
    /**
     * 获取持仓信息
     * @param contractCode String 合约代码
     */
    @Override
    public List<SwapPositionInfoResponse.DataBean> getContractPositionInfo(String contractCode) throws ApiException, NullPointerException {
        String positionInfo = redisService.hashGet(CacheService.SPACE_INFO,contractCode);
        SwapPositionInfoResponse contractPositionInfo = JSON.parseObject(positionInfo, SwapPositionInfoResponse.class);
        if (contractPositionInfo == null) {
            return null;
        }
        return contractPositionInfo.getData().stream().filter( (f) -> f.getContractCode().equals(contractCode)).collect(Collectors.toList());
    }

    /**
     * 获取最大可开仓张数
     * @param contractCode String
     * @return int
     */
    @Override
    public int getMaxOpenVolume(String contractCode) {
        //获取最新的开仓张数
        String firstVolume = redisService.getListByIndex(CacheService.SWAP_OPEN_VOLUME + contractCode,Long.parseLong("0"));
        String secondVolume = redisService.getListByIndex(CacheService.SWAP_OPEN_VOLUME + contractCode,Long.parseLong("1"));
        return Integer.parseInt(firstVolume) + Integer.parseInt(secondVolume);
    }
    /**
     * 设置持仓信息
     * @param contractCode String
     */
    @Override
    public void setContractPositionInfo(String contractCode) throws ApiException, NullPointerException {
        String positionInfo = huobiSwapEntity.getContractPositionInfo(contractCode);
        redisService.hashSet(CacheService.SPACE_INFO,contractCode,positionInfo);
    }

    /**
     * 将订单拆分成盈利订单 和亏损订单
     */
    @Override
    public void contractLossWinOrder(String contractCode, PubConst.UPSTRATGY upStratgy) {
        //取到所有的成交订单
        String contractMatchresultsStr = huobiSwapEntity.swapMatchresultsRequest(contractCode,0,10,1,50);
        SwapMatchresultsResponse contractMatchresultsResponse = JSON.parseObject(contractMatchresultsStr,SwapMatchresultsResponse.class);
        List<SwapMatchresultsResponse.DataBean.TradesBean> historyData = contractMatchresultsResponse.getData().getTrades();
        String openVolumeKey = CacheService.SWAP_OPEN_VOLUME,
                orderDealOidKey = CacheService.SWAP_ORDER_DEAL_OID + contractCode,
                lossKey = CacheService.SWAP_ORDER_LOSS + contractCode,
                winKey = CacheService.SWAP_ORDER_WIN + contractCode;
        int winVolume = 0,lossVolume = 0;
        if (historyData.size() == 0) {
            return;
        }
        //最新的订单是否止损了
        boolean lossFlag = historyData.get(0).getRealProfit().doubleValue() < 0,
                onlyNewestLossFlag = true,
                onlyNewestWinFlag = true;
        String direction = "";
        for (SwapMatchresultsResponse.DataBean.TradesBean historyRow : historyData) {
            //订单id
            String orderIdStr = historyRow.getOrderIdStr().toString();
            //订单id相同的订单成交张数
            int tradeVolume = historyData.stream().filter(h-> orderIdStr.equals(h.getOrderIdStr().toString())).mapToInt(h->h.getTradeVolume().intValue()).sum();
            //订单id相同的订单真实收益
            double realProfit = historyData.stream().filter(h-> orderIdStr.equals(h.getOrderIdStr().toString())).mapToDouble(h->h.getRealProfit().doubleValue()).sum();
            //开仓或平仓
            String offset = historyRow.getOffset();
            historyRow.setRealProfit(BigDecimal.valueOf(realProfit));
            historyRow.setTradeVolume(BigDecimal.valueOf(tradeVolume));
            direction = historyRow.getDirection();
            //订单信息json
            String historyRowJson = JSON.toJSONString(historyRow);
            //如果是平仓单
            if ("close".equals(offset)) {
                //hash值存在就跳过
                if (redisService.hashExists(lossKey,orderIdStr) || redisService.hashExists(winKey,orderIdStr)) {
                    continue;
                }
                //如果是止损的订单
                if (realProfit < 0) {
                    //计算止损的张数
                    if (lossFlag && onlyNewestLossFlag) {
                        lossVolume += tradeVolume;
                    }
                    onlyNewestWinFlag = false;
                    //把止损的订单放入队列
                    redisService.hashSet(lossKey,orderIdStr,historyRowJson);
                    continue;
                }
                //如果是止盈的订单
                if (realProfit > 0) {
                    //计算止盈的张数
                    if (!lossFlag && onlyNewestWinFlag) {
                        winVolume += tradeVolume;
                    }
                    onlyNewestLossFlag = false;
                    //把止盈的订单放入队列
                    redisService.hashSet(winKey,orderIdStr,historyRowJson);
                }
            }
            //如果是开仓单
            if ("open".equals(offset)) {
                if (redisService.hashExists(orderDealOidKey,orderIdStr)) {
                    continue;
                }
                redisService.hashSet(orderDealOidKey,orderIdStr,historyRowJson);
            }
        }
        //获取openVolume的长度
        Long openVolumeLen = redisService.getListLen(openVolumeKey + contractCode);
        String logStr = "";
        //如果止损了
        if (lossFlag && lossVolume > 0 && "sell".equals(direction)) {
            if (upStratgy == PubConst.UPSTRATGY.FBNQ) {
                //如果倍投次数小于最大倍投次数就继续倍投，反之止损回到最初
                if (openVolumeLen <= PubConst.MAX_OPEN_NUM) {
                    redisService.lpush(openVolumeKey + contractCode,String.valueOf(lossVolume));
                    logStr = "SWAP-止损后倍投，止损张数：" + lossVolume;
                } else {
                    //修剪列表 只留2个元素
                    redisService.listTrim(openVolumeKey + contractCode,-2,-1);
                    logStr = "SWAP-最大止损回到开始的地方，止损张数：" + lossVolume;
                }
            }
            if (upStratgy == PubConst.UPSTRATGY.PLL) {
                //修剪列表 只留2个元素
                redisService.listTrim(openVolumeKey + contractCode,-2,-1);
                logStr = "SWAP-止损回到开始的地方，止损张数：" + lossVolume;
            }
            log.info(logStr);
            mailService.sendMail("SWAP-订单止损拆分",logStr,"");
        } else if(winVolume > 0 && "sell".equals(direction)){
            if (upStratgy == PubConst.UPSTRATGY.FBNQ) {
                //如果止盈了并且倍投队列长度大于3回退两步,等于3回退一步
                int backNum = openVolumeLen > 3 ? 2 : (openVolumeLen == 3 ? 1 : 0);
                for (int i = 0; i < backNum; i++) {
                    redisService.leftPop(openVolumeKey + contractCode);
                }
                logStr = "SWAP-盈利后撤步数" + backNum;
            }
            if (upStratgy == PubConst.UPSTRATGY.PLL) {
                if (openVolumeLen + 1 >= PubConst.PLLNUM) {
                    //修剪列表 只留2个元素
                    redisService.listTrim(openVolumeKey + contractCode,-2,-1);
                    logStr = "SWAP-止赢回到开始的地方，止赢张数：" + winVolume;
                } else {
                    redisService.lpush(openVolumeKey + contractCode,String.valueOf(winVolume));
                    logStr = "SWAP-止赢后进阶，止盈张数：" + winVolume;
                }
            }
            log.info(logStr);
            mailService.sendMail("SWAP-订单止盈拆分",logStr,"");
        }
    }
}
