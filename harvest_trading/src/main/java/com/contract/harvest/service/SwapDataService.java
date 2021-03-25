package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.contract.harvest.common.Depth;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.common.Topic;
import com.contract.harvest.entity.HuobiSwapEntity;
import com.contract.harvest.service.inter.SwapServiceInter;
import com.contract.harvest.tools.Arith;
import com.huobi.api.request.trade.ContractOrderRequest;
import com.huobi.api.response.account.ContractPositionInfoResponse;
import com.huobi.api.response.trade.ContractOrderInfoResponse;
import com.huobi.api.response.trade.ContractOrderResponse;
import com.huobiswap.api.enums.DirectionEnum;
import com.huobiswap.api.enums.OffsetEnum;
import com.huobi.api.exception.ApiException;
import com.huobiswap.api.request.trade.SwapOrderRequest;
import com.huobiswap.api.response.account.SwapPositionInfoResponse;
import com.huobiswap.api.response.market.SwapContractInfoResponse;
import com.huobiswap.api.response.trade.SwapOrderInfoResponse;
import com.huobiswap.api.response.trade.SwapOrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
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
//        SwapContractInfoResponse.DataBean(symbol=BCH, contractCode=BCH-USDT, contractSize=0, priceTick=0.010000000000000000, settlementDate=1616572800000, createDate=20201021, contractStatus=1, supportMarginMode=all, deliveryTime=null)
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
            stopPrice = BigDecimal.valueOf(Arith.mul(price,Arith.sub(1,stopPercent))).setScale(3,BigDecimal.ROUND_HALF_UP);
            limitPrice = BigDecimal.valueOf(Arith.mul(price,Arith.add(1,limitPercent))).setScale(3,BigDecimal.ROUND_HALF_UP);
        }
        //空仓止盈损
        if (direction == DirectionEnum.SELL) {
            stopPrice = BigDecimal.valueOf(Arith.mul(price,Arith.add(1,stopPercent))).setScale(3,BigDecimal.ROUND_HALF_UP);
            limitPrice = BigDecimal.valueOf(Arith.mul(price,Arith.sub(1,limitPercent))).setScale(3,BigDecimal.ROUND_HALF_UP);
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
        System.out.println(order.toString());
        return order;
//        SwapOrderRequest(contractCode=BCH-USDT, clientOrderId=5436357548, price=513.89, volume=10, direction=BUY, offset=OPEN, leverRate=3, orderPriceType=post_only, tpTriggerPrice=529.307, tpOrderPrice=529.307, tpOrderPriceType=optimal_10, slTriggerPrice=498.473, slOrderPrice=498.473, slOrderPriceType=optimal_10)
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
            ContractOrderResponse contractOrderResponse = JSON.parseObject(contractOrder,ContractOrderResponse.class);
            orderIdStr = contractOrderResponse.getData().getOrderIdStr();
        } else {
            orderIdStr = JSONObject.parseObject(orderInfoStr).getString("orderIdStr");
        }
        if ("".equals(orderIdStr) || null == orderIdStr) {
            log.info("swap订单id为空" + contractOrder);
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
            log.info("swap订单信息" + JSON.toJSONString(orderInfo) + "重试次数："+ i);
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
            log.info("swap全部成交:"+orderInfo);
        } else if (Arrays.binarySearch(partSuccessArr,orderStatus) >= 0) {
            //撤单
            String contractCancel = huobiSwapEntity.swapCancelRequest(orderInfo.getOrderIdStr(),"",contractCode);
            log.info("swap部分成交:"+orderInfo+"撤单信息:"+contractCancel);
        } else {
            //撤单
            String contractCancel = huobiSwapEntity.swapCancelRequest(orderInfo.getOrderIdStr(),"",contractCode);
            log.info("swap撤单:"+orderInfo+"撤单信息:"+contractCancel);
        }
        int tradeVolume = orderInfo.getTradeVolume().intValue();
        if (tradeVolume > 0) {
            mailService.sendMail("swap成功下单-成交量"+tradeVolume,"订单信息:"+JSON.toJSONString(orderInfo),"");
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
}
