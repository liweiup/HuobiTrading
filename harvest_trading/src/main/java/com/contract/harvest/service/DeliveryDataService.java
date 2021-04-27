package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.contract.harvest.common.Depth;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.common.Topic;
import com.contract.harvest.entity.HuobiEntity;
import com.contract.harvest.service.inter.DeliveryServiceInter;
import com.contract.harvest.tools.Arith;

import com.huobi.api.enums.DirectionEnum;
import com.huobi.api.enums.OffsetEnum;
import com.huobi.api.exception.ApiException;
import com.huobi.api.request.trade.ContractOrderRequest;
import com.huobi.api.response.account.ContractPositionInfoResponse;
import com.huobi.api.response.market.ContractContractCodeResponse;
import com.huobi.api.response.trade.ContractMatchresultsResponse;
import com.huobi.api.response.trade.ContractOrderInfoResponse;
import com.huobi.api.response.trade.ContractOrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liwei
 */
@Slf4j
@Service
public class DeliveryDataService implements DeliveryServiceInter {

    @Resource
    private RedisService redisService;
    @Resource
    private CacheService cacheService;
    @Resource
    private DataService dataService;
    @Resource
    private HuobiEntity huobiEntity;
    @Resource
    private MailService mailService;

    /**
     * 获取合约信息
     * @param symbol 币种
     * @param contractType 合约类型: （this_week:当周 next_week:下周 quarter:当季 next_quarter:次季）
     * @param contractCode 合约代码
     */
    @Override
    public ContractContractCodeResponse.DataBean getContractInfo(String symbol, String contractType, String contractCode) throws NullPointerException, ApiException {
        String contractInfo = huobiEntity.getContractInfo("","","");
        ContractContractCodeResponse response = JSON.parseObject(contractInfo,ContractContractCodeResponse.class);
        List<ContractContractCodeResponse.DataBean> contractCodeData = response.getData();
        if (!"".equals(contractType) && !"".equals(symbol)) {
            contractCodeData = contractCodeData.stream().filter( f -> f.getSymbol().equals(symbol) && f.getContractType().equals(contractType)).collect(Collectors.toList());
        }
        if (!"".equals(contractCode)) {
            contractCodeData = contractCodeData.stream().filter( f -> f.getContractCode().equals(contractCode)).collect(Collectors.toList());
        }
        return contractCodeData.size() > 0 ? contractCodeData.get(0) : null;
    }
    /**
     * 获取下单实例
     * @param symbol 币种
     * @param contractFlag 交易合约类型
     * @param contractCode 合约代码
     * @param offset open or close
     * @param direction 交易方向
     */
    @Override
    public ContractOrderRequest getPlanOrder(String symbol, String contractFlag, String contractCode, OffsetEnum offset, DirectionEnum direction, long dealVolume, double stopPercent, double limitPercent) throws InterruptedException, NullPointerException,ApiException {
        String contractType = PubConst.CONTRACT_TYPE.get(contractFlag);
        ContractContractCodeResponse.DataBean contractInfo = getContractInfo(symbol,contractType,contractCode);
        //获取最新的价格
        String depthSubKey = Topic.formatChannel(Topic.DEPTH_SUB,symbol+contractFlag,PubConst.DEPTH_SUB_INDEX);
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
        ContractOrderRequest order =  ContractOrderRequest.builder()
                .symbol(symbol)
                .contractType(contractType)
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
            stopPrice = BigDecimal.valueOf(Arith.mul(price,Arith.sub(1,stopPercent))).setScale(3,BigDecimal.ROUND_HALF_UP);
            limitPrice = BigDecimal.valueOf(Arith.mul(price,Arith.add(1,limitPercent))).setScale(3,BigDecimal.ROUND_HALF_UP);
        }
        //空仓止盈损
        if (direction == DirectionEnum.SELL) {
            stopPrice = BigDecimal.valueOf(Arith.mul(price,Arith.add(1,limitPercent))).setScale(3,BigDecimal.ROUND_HALF_UP);
            limitPrice = BigDecimal.valueOf(Arith.mul(price,Arith.sub(1,stopPercent))).setScale(3,BigDecimal.ROUND_HALF_UP);
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
    public void handleOrder(ContractOrderRequest order) throws InterruptedException, NullPointerException, ApiException {
        String symbol = order.getSymbol();
        Long clientOrderId = order.getClientOrderId();
        String orderInfoStr,orderIdStr ,contractOrder = "";
        ContractOrderInfoResponse.DataBean orderInfo = null;
        //订单成交信息key
        String orderInfoKey = CacheService.ORDER_INFO + symbol;
        //获取订单成交信息
        orderInfoStr = redisService.hashGet(orderInfoKey,clientOrderId.toString());
        if ("".equals(orderInfoStr)) {
            //下单-存放订单成交信息
            contractOrder = huobiEntity.futureContractOrder(order);
            ContractOrderResponse contractOrderResponse = JSON.parseObject(contractOrder,ContractOrderResponse.class);
            orderIdStr = contractOrderResponse.getData().getOrderIdStr();
        } else {
            orderIdStr = JSONObject.parseObject(orderInfoStr).getString("orderIdStr");
        }
        if ("".equals(orderIdStr) || null == orderIdStr) {
            log.info("订单id为空" + contractOrder);
            return;
        }
        int i = 0;
        while (i < PubConst.ORDER_TRY_NUM) {
            orderInfoStr = huobiEntity.getcontractOrderInfo(orderIdStr,"",symbol);
            ContractOrderInfoResponse orderInfoResponse = JSON.parseObject(orderInfoStr,ContractOrderInfoResponse.class);
            orderInfo = orderInfoResponse.getData().get(0);
            if (null == orderInfo) {
                continue;
            }
            log.info("订单信息" + JSON.toJSONString(orderInfo) + "重试次数："+ i);
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
            log.info("全部成交:"+orderInfo);
        } else if (Arrays.binarySearch(partSuccessArr,orderStatus) >= 0) {
            //撤单
            String contractCancel = huobiEntity.futureContractCancel(orderInfo.getOrderIdStr(),"",symbol);
            log.info("部分成交:"+orderInfo+"撤单信息:"+contractCancel);
        } else {
            //撤单
            String contractCancel = huobiEntity.futureContractCancel(orderInfo.getOrderIdStr(),"",symbol);
            log.info("撤单:"+orderInfo+"撤单信息:"+contractCancel);
        }
        int tradeVolume = orderInfo.getTradeVolume().intValue();
        if (tradeVolume > 0) {
            mailService.sendMail("成功下单-成交量"+tradeVolume,"订单信息:"+JSON.toJSONString(orderInfo),"");
        }
    }
    /**
     * 获取持仓信息
     * @param symbol String
     * @param contractType String 合约类型: （this_week:当周 next_week:下周 quarter:当季 next_quarter:次季）
     * @param contractCode String 合约代码
     */
    @Override
    public List<ContractPositionInfoResponse.DataBean> getContractPositionInfo(String symbol,String contractType,String contractCode) throws ApiException, NullPointerException {
        String positionInfo = redisService.hashGet(CacheService.SPACE_INFO,symbol);
        if ("".equals(contractCode)) {
            contractCode = getContractInfo(symbol,PubConst.CONTRACT_TYPE.get(contractType),"").getContractCode();
        }
        ContractPositionInfoResponse contractPositionInfo = JSON.parseObject(positionInfo, ContractPositionInfoResponse.class);
        if (contractPositionInfo == null) {
            return null;
        }
        String finalContractCode = contractCode;
        return contractPositionInfo.getData().stream().filter( (f) -> f.getContractCode().equals(finalContractCode)).collect(Collectors.toList());
    }
    /**
     * 设置持仓信息
     * @param symbol String
     */
    @Override
    public void setContractPositionInfo(String symbol) throws ApiException, NullPointerException {
        String positionInfo = huobiEntity.getContractPositionInfo(symbol);
        redisService.hashSet(CacheService.SPACE_INFO,symbol,positionInfo);
    }

    /**
     * 将订单拆分成盈利订单 和亏损订单
     */
    @Override
    public void contractLossWinOrder(String symbol, PubConst.UPSTRATGY upStratgy) {
        //取到所有的成交订单
        String contractMatchresultsStr = huobiEntity.contractMatchresultsRequest(symbol,1,0,10);
        ContractMatchresultsResponse contractMatchresultsResponse = JSON.parseObject(contractMatchresultsStr,ContractMatchresultsResponse.class);
        List<ContractMatchresultsResponse.DataBean.TradesBean> historyData = contractMatchresultsResponse.getData().getTrades();
        String openVolumeKey = CacheService.OPEN_VOLUME,
                orderDealOidKey = CacheService.ORDER_DEAL_OID + symbol,
                lossKey = CacheService.ORDER_LOSS + symbol,
                winKey = CacheService.ORDER_WIN + symbol;
        int winVolume = 0,lossVolume = 0;
        //最新的订单是否止损了
        boolean lossFlag = historyData.get(0).getRealProfit().doubleValue() < 0,
                onlyNewestLossFlag = true,
                onlyNewestWinFlag = true;
        String direction = "";
        for (ContractMatchresultsResponse.DataBean.TradesBean historyRow : historyData) {
            //订单id
            String orderIdStr = historyRow.getOrderIdStr().toString();
            //订单id相同的订单成交张数
            int tradeVolume = historyData.stream().filter(h-> orderIdStr.equals(h.getOrderIdStr().toString())).mapToInt(h->h.getTradeVolume().intValue()).sum();
            //订单id相同的订单真实收益
            double realProfit = historyData.stream().filter(h-> orderIdStr.equals(h.getOrderIdStr().toString())).mapToDouble(h->h.getRealProfit().doubleValue()).sum();
            //开仓或平仓
            String offset = historyRow.getOffset();
            direction = historyRow.getDirection();
            historyRow.setRealProfit(BigDecimal.valueOf(realProfit));
            historyRow.setTradeVolume(BigDecimal.valueOf(tradeVolume));
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
        Long openVolumeLen = redisService.getListLen(openVolumeKey + symbol);
        String logStr = "";
        //如果止损了
        if (lossFlag && lossVolume > 0 && "buy".equals(direction)) {
            if (upStratgy == PubConst.UPSTRATGY.FBNQ) {
                //如果倍投次数小于最大倍投次数就继续倍投，反之止损回到最初
                if (openVolumeLen <= PubConst.MAX_OPEN_NUM) {
                    redisService.lpush(openVolumeKey + symbol,String.valueOf(lossVolume));
                    logStr = "止损后倍投，止损张数：" + lossVolume;
                } else {
                    //修剪列表 只留2个元素
                    redisService.listTrim(openVolumeKey + symbol,-2,-1);
                    logStr = "最大止损回到开始的地方，止损张数：" + lossVolume;
                }
            }
            if (upStratgy == PubConst.UPSTRATGY.PLL) {
                //修剪列表 只留2个元素
                redisService.listTrim(openVolumeKey + symbol,-2,-1);
                logStr = "止损回到开始的地方，止损张数：" + lossVolume;
            }
            log.info(logStr);
            mailService.sendMail("订单止损拆分",logStr,"");
            //停用一会
            cacheService.saveTimeFlag(PubConst.TIME_FLAG);

        } else if(winVolume > 0 && "buy".equals(direction)){
            if (upStratgy == PubConst.UPSTRATGY.FBNQ) {
                //如果止盈了并且倍投队列长度大于3回退两步,等于3回退一步
                int backNum = openVolumeLen > 3 ? 2 : (openVolumeLen == 3 ? 1 : 0);
                for (int i = 0; i < backNum; i++) {
                    redisService.leftPop(openVolumeKey + symbol);
                }
                logStr = "盈利后撤步数" + backNum;
            }
            if (upStratgy == PubConst.UPSTRATGY.PLL) {
                if (openVolumeLen + 1 >= PubConst.PLLNUM) {
                    //修剪列表 只留2个元素
                    redisService.listTrim(openVolumeKey + symbol,-2,-1);
                    logStr = "止赢回到开始的地方，止赢张数：" + winVolume;
                } else {
                    redisService.lpush(openVolumeKey + symbol,String.valueOf(winVolume));
                    logStr = "止赢后进阶，止盈张数：" + winVolume;
                }
            }
            log.info(logStr);
            mailService.sendMail("订单止盈拆分",logStr,"");
        }
    }

    /**
     * 获取最大可开仓张数
     * @param symbol String
     * @return int
     */
    @Override
    public int getMaxOpenVolume(String symbol) {
        //获取最新的开仓张数
        String firstVolume = redisService.getListByIndex(CacheService.OPEN_VOLUME + symbol,Long.parseLong("0"));
        String secondVolume = redisService.getListByIndex(CacheService.OPEN_VOLUME + symbol,Long.parseLong("1"));
        return Integer.parseInt(firstVolume) + Integer.parseInt(secondVolume);
    }
}
