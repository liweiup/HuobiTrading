package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.contract.harvest.common.Depth;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.entity.Candlestick;
import com.contract.harvest.common.Topic;
import com.contract.harvest.entity.HuobiEntity;
import com.contract.harvest.service.inter.DataServiceInter;
import com.contract.harvest.tools.Arith;
import com.contract.harvest.tools.CodeConstant;
import com.contract.harvest.tools.FormatParam;

import com.google.gson.Gson;
import com.huobi.api.enums.DirectionEnum;
import com.huobi.api.enums.OffsetEnum;
import com.huobi.api.exception.ApiException;
import com.huobi.api.request.trade.ContractOrderRequest;
import com.huobi.api.response.account.ContractPositionInfoResponse;
import com.huobi.api.response.market.ContractContractCodeResponse;
import com.huobi.api.response.trade.ContractOrderInfoResponse;
import com.huobi.api.response.trade.ContractOrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeliveryDataService implements DataServiceInter {

    @Resource
    private RedisService redisService;
    @Resource
    private HuobiEntity huobiEntity;
    @Resource
    private MailService mailService;

    /**
     * 获取kline数据
     * @param channel 订阅的标识 如 BSV_CW
     * @param topicIndex k线周期
     */
    @Override
    public List<Candlestick.DataBean> getKlineList(String channel, int topicIndex) throws NullPointerException,IllegalArgumentException{
        //最新的一条k线
        String lineKey = Topic.formatChannel(Topic.KLINE_SUB,channel, topicIndex).toUpperCase();
        String lineData = redisService.hashGet(CacheService.HUOBI_SUB,lineKey);
        if ("".equals(lineData)) {
            throw new NullPointerException(CodeConstant.getMsg(CodeConstant.NONE_KLINE_DATA));
        }
        Candlestick.DataBean tick = JSON.parseObject(lineData,Candlestick.class).getTick();
        //过往的x条k线
        String manyLineStr = redisService.hashGet(CacheService.HUOBI_KLINE,channel+Topic.PERIOD[PubConst.TOPIC_INDEX]);
        if ("".equals(manyLineStr)) {
            throw new NullPointerException(CodeConstant.getMsg(CodeConstant.NONE_KLINE_DATA));
        }
        List<Candlestick.DataBean> tickList = JSON.parseObject(manyLineStr,Candlestick.class).getData();
        if (tick.getId() < tickList.get(tickList.size()-1).getId()) {
            throw new IllegalArgumentException(CodeConstant.getMsg(CodeConstant.KLINE_DATE_ERROR));
        }
        tickList.set(tickList.size()-1,tick);
        return tickList;
    }
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
        Depth.TickBean depth = getBidAskPrice(depthSubKey).getTick();
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
                .clientOrderId(getClientOrderId())
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
        return order;
    }
    /**
     * 获取购买价格，与卖出价格
     * @param depthSubKey 成交帐簿的key
     */
    @Override
    public Depth getBidAskPrice(String depthSubKey) throws NullPointerException, InterruptedException {
        String depthStr = redisService.hashGet(CacheService.HUOBI_SUB,depthSubKey);
        if ("".equals(depthStr)) {
            Thread.sleep(1000);
            return getBidAskPrice(depthSubKey);
        }
        return JSON.parseObject(depthStr, Depth.class);
    }
    /**
     * 生成随机订单id
     */
    @Override
    public Long getClientOrderId() {
        Random random = new Random();
        // 随机数的量 自由定制，这是9位随机数
        int r = random.nextInt(900) + 100;
        // 返回  17位时间
        DateFormat sdf = new SimpleDateFormat("mmssSSS");
        String timeStr = sdf.format(new Date());
        // 17位时间+9位随机数
        return  Long.valueOf(timeStr + r);
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
    public void splitOrder(String symbol) {
        //取到前一天所有的成交订单
        String contractMatchresultsStr = huobiEntity.contractMatchresultsRequest(symbol,0,1);
        System.out.println(contractMatchresultsStr);
//        String key = CacheService.ORDER_DEAL_CLIENTID + symbol;
//        Set<String> dealClientIdSet =  redisService.getSetMembers(key);
//        if (null == dealClientIdSet) {
//            return;
//        }
//        for (String clientId : dealClientIdSet) {
//            //获取订单信息
//            String orderInfo = redisService.hashGet(CacheService.ORDER_INFO + symbol,clientId);
//            ContractOrderInfoResponse.DataBean orderInfoResponse = JSON.parseObject(orderInfo,ContractOrderInfoResponse.DataBean.class);
//            System.out.println(orderInfoResponse);
//        }


    }

}
