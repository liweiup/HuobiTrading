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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    @Resource
    private CacheService cacheService;

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
     * ????????????????????????
     */
    public int getDealOpenVol(double price,double contractSize,long dealVolume) {
        long usdtNum = dealVolume * 10;
        double openVol = Arith.div(usdtNum,Arith.mul(price,contractSize));
        return (int) Math.round(openVol);
    }

    /**
     * ??????????????????
     * @param contractCode ????????????
     * @param offset open or close
     * @param direction ????????????
     */
    @Override
    public SwapOrderRequest getPlanOrder(String contractCode, OffsetEnum offset, DirectionEnum direction, long dealVolume, double stopPercent, double limitPercent) throws InterruptedException, NullPointerException,ApiException {
        SwapContractInfoResponse.DataBean contractInfo = getContractInfo(contractCode);
        //?????????????????????
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
        //???????????????
        int decimals = String.valueOf(price).length() - (String.valueOf(price).indexOf(".") + 1);
        contractCode = contractInfo.getContractCode();
        String orderPriceType = offset == OffsetEnum.CLOSE ? "optimal_10" : PubConst.ORDER_PRICE_TYPE;
        SwapOrderRequest order =  SwapOrderRequest.builder()
                .contractCode(contractCode)
                .clientOrderId(dataService.getClientOrderId())
                .price(BigDecimal.valueOf(price))
                .volume(dealVolume)
                .direction(direction)
                .offset(offset)
                .leverRate(PubConst.LEVER_RATE)
                .orderPriceType(orderPriceType)
                .build();
        //???????????????
        BigDecimal stopPrice = null,limitPrice = null;
        if (direction == DirectionEnum.BUY) {
            stopPrice = BigDecimal.valueOf(Arith.mul(price,Arith.sub(1,stopPercent))).setScale(decimals,BigDecimal.ROUND_HALF_UP);
            limitPrice = BigDecimal.valueOf(Arith.mul(price,Arith.add(1,limitPercent))).setScale(decimals,BigDecimal.ROUND_HALF_UP);
        }
        //???????????????
        if (direction == DirectionEnum.SELL) {
            stopPrice = BigDecimal.valueOf(Arith.mul(price,Arith.add(1,stopPercent))).setScale(decimals,BigDecimal.ROUND_HALF_UP);
            limitPrice = BigDecimal.valueOf(Arith.mul(price,Arith.sub(1,limitPercent))).setScale(decimals,BigDecimal.ROUND_HALF_UP);
        }
        //???????????? ??????
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
     * ????????????
     * @param order ????????????
     */
    @Override
    public void handleOrder(SwapOrderRequest order) throws InterruptedException, NullPointerException, ApiException {
        String contractCode = order.getContractCode();
        Long clientOrderId = order.getClientOrderId();
        String orderInfoStr,orderIdStr ,contractOrder = "";
        SwapOrderInfoResponse.DataBean orderInfo = null;
        //??????????????????key
        String orderInfoKey = CacheService.SWAP_ORDER_INFO + contractCode;
        //????????????????????????
        orderInfoStr = redisService.hashGet(orderInfoKey,clientOrderId.toString());
        if ("".equals(orderInfoStr)) {
            //??????-????????????????????????
            contractOrder = huobiSwapEntity.futureContractOrder(order);
            SwapOrderResponse contractOrderResponse = JSON.parseObject(contractOrder, SwapOrderResponse.class);
            orderIdStr = contractOrderResponse.getData().getOrderIdStr();
        } else {
            orderIdStr = JSONObject.parseObject(orderInfoStr).getString("orderIdStr");
        }
        if ("".equals(orderIdStr) || null == orderIdStr) {
            log.info("SWAP-??????id??????" + contractOrder);
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
            //??????????????????
            if (orderInfo.getStatus() == 6 || orderInfo.getStatus() == 7) {
                redisService.hashSet(orderInfoKey,clientOrderId.toString(),JSON.toJSONString(orderInfo));
                break;
            }
            Thread.sleep(3000L);
            i++;
        }
        log.info("SWAP-????????????" + JSON.toJSONString(orderInfo) + "???????????????"+ i);
        int orderStatus = orderInfo.getStatus();
        int[] partSuccessArr = {4,5};
        if (orderStatus == 6) {
            log.info("SWAP-????????????:"+orderInfo);
        } else if (Arrays.binarySearch(partSuccessArr,orderStatus) >= 0) {
            //??????
            String contractCancel = huobiSwapEntity.swapCancelRequest(orderInfo.getOrderIdStr(),"",contractCode);
            log.info("SWAP-????????????:"+orderInfo+"????????????:"+contractCancel);
        } else {
            //??????
            String contractCancel = huobiSwapEntity.swapCancelRequest(orderInfo.getOrderIdStr(),"",contractCode);
            log.info("SWAP-??????:"+orderInfo+"????????????:"+contractCancel);
        }
        int tradeVolume = orderInfo.getTradeVolume().intValue();
        if (tradeVolume > 0) {
            mailService.sendMail("SWAP-????????????-?????????"+tradeVolume,"????????????:"+JSON.toJSONString(orderInfo),"");
        }
    }
    /**
     * ??????????????????
     * @param contractCode String ????????????
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
     * ???????????????????????????
     * @param contractCode String
     * @return int
     */
    @Override
    public int getMaxOpenVolume(String contractCode) {
        SwapContractInfoResponse.DataBean contractInfo = getContractInfo(contractCode);
        //???????????????????????????
        String firstVolume = redisService.getListByIndex(CacheService.SWAP_OPEN_VOLUME + contractCode,Long.parseLong("0"));
        String secondVolume = redisService.getListByIndex(CacheService.SWAP_OPEN_VOLUME + contractCode,Long.parseLong("1"));
        return Integer.parseInt(firstVolume) + Integer.parseInt(secondVolume);
    }
    /**
     * ??????????????????
     * @param contractCode String
     */
    @Override
    public void setContractPositionInfo(String contractCode) throws ApiException, NullPointerException {
        String positionInfo = huobiSwapEntity.getContractPositionInfo(contractCode);
        redisService.hashSet(CacheService.SPACE_INFO,contractCode,positionInfo);
    }

    /**
     * ?????????????????????????????? ???????????????
     */
    @Override
    public void contractLossWinOrder(String contractCode, PubConst.UPSTRATGY upStratgy) {
        //???????????????????????????
        String contractMatchresultsStr = huobiSwapEntity.swapMatchresultsRequest(contractCode,0,10,1,10);
        SwapMatchresultsResponse contractMatchresultsResponse = JSON.parseObject(contractMatchresultsStr,SwapMatchresultsResponse.class);
        List<SwapMatchresultsResponse.DataBean.TradesBean> historyData = contractMatchresultsResponse.getData().getTrades();
        String openVolumeKey = CacheService.SWAP_OPEN_VOLUME,
                orderDealOidKey = CacheService.SWAP_ORDER_DEAL_OID + contractCode,
                lossKey = CacheService.SWAP_ORDER_LOSS + contractCode,
                winKey = CacheService.SWAP_ORDER_WIN + contractCode;
        int winVolume = 0,lossVolume = 0;
        double newestRealProfit = 0;
        if (historyData.size() == 0) {
            return;
        }
        //??????????????????????????????
        boolean lossFlag = historyData.get(0).getRealProfit().doubleValue() < 0,
                onlyNewestLossFlag = true,
                onlyNewestWinFlag = true;
        String direction = "";
        Set<String> orderIdStrSet = new HashSet<>();
        for (SwapMatchresultsResponse.DataBean.TradesBean historyRow : historyData) {
            //??????id
            String orderIdStr = historyRow.getOrderIdStr().toString();
            //id????????????
            if (orderIdStrSet.contains(orderIdStr)) {
                continue;
            }
            //hash??????????????????
            if (redisService.hashExists(lossKey,orderIdStr) || redisService.hashExists(winKey,orderIdStr) || redisService.hashExists(orderDealOidKey,orderIdStr)) {
                continue;
            }
            //???????????????
            String offset = historyRow.getOffset();
            //??????id???????????????????????????
            int tradeVolume = historyData.stream().filter(h-> orderIdStr.equals(h.getOrderIdStr().toString())).mapToInt(h->h.getTradeVolume().intValue()).sum();
            //?????????
            int tradeTurnover = historyData.stream().filter(h-> orderIdStr.equals(h.getOrderIdStr().toString())).mapToInt(h->h.getTradeTurnover().intValue()).sum();
            //?????????
            double tradeFee = historyData.stream().filter(h-> orderIdStr.equals(h.getOrderIdStr().toString())).mapToDouble(h->h.getTradeFee()).sum();
            //??????id???????????????????????????
            double realProfit = historyData.stream().filter(h-> orderIdStr.equals(h.getOrderIdStr().toString())).mapToDouble(h->h.getRealProfit().doubleValue()).sum();
            historyRow.setRealProfit(BigDecimal.valueOf(realProfit));
            historyRow.setTradeVolume(BigDecimal.valueOf(tradeVolume));
            historyRow.setTradeFee(tradeFee);
            historyRow.setTradeTurnover(BigDecimal.valueOf(tradeTurnover));
            //????????????json
            String historyRowJson = JSON.toJSONString(historyRow);
            orderIdStrSet.add(orderIdStr);
            newestRealProfit += realProfit;
            //??????????????????
            if ("close".equals(offset)) {
                direction = historyRow.getDirection();
                //????????????????????????
                if (realProfit < 0) {
                    //?????????????????????
                    if (lossFlag && onlyNewestLossFlag) {
                        lossVolume += tradeVolume;
                    }
                    onlyNewestWinFlag = false;
                    //??????????????????????????????
                    redisService.hashSet(lossKey,orderIdStr,historyRowJson);
                    continue;
                }
                //????????????????????????
                if (realProfit > 0) {
                    //?????????????????????
                    if (!lossFlag && onlyNewestWinFlag) {
                        winVolume += tradeVolume;
                    }
                    onlyNewestLossFlag = false;
                    //??????????????????????????????
                    redisService.hashSet(winKey,orderIdStr,historyRowJson);
                }
            }
            //??????????????????
            if ("open".equals(offset)) {
                redisService.hashSet(orderDealOidKey,orderIdStr,historyRowJson);
            }
        }
        //??????openVolume?????????
        Long openVolumeLen = redisService.getListLen(openVolumeKey + contractCode);
        String logStr = contractCode+"--";
        //???????????????
        if (lossFlag && lossVolume > 0 && "sell".equals(direction)) {
            if (upStratgy == PubConst.UPSTRATGY.FBNQ) {
                //????????????????????????????????????????????????????????????????????????????????????
                if (openVolumeLen <= PubConst.MAX_OPEN_NUM) {
                    redisService.lpush(openVolumeKey + contractCode,String.valueOf(lossVolume));
                    logStr = "SWAP-?????????????????????????????????" + lossVolume;
                } else {
                    //???????????? ??????2?????????
                    redisService.listTrim(openVolumeKey + contractCode,-2,-1);
                    logStr = "SWAP-???????????????????????????????????????????????????" + lossVolume;
                }
            }
            if (upStratgy == PubConst.UPSTRATGY.PLL) {
                //???????????? ??????2?????????
                redisService.listTrim(openVolumeKey + contractCode,-2,-1);
                logStr = "SWAP-?????????????????????????????????????????????" + lossVolume;
            }
            logStr += " ?????????" + newestRealProfit;
            log.info(logStr);
            mailService.sendMail("SWAP-??????????????????",logStr,"");
            //????????????
            cacheService.saveTimeFlag(contractCode);
        } else if(winVolume > 0 && "sell".equals(direction)){
            if (upStratgy == PubConst.UPSTRATGY.FBNQ) {
                //?????????????????????????????????????????????3????????????,??????3????????????
                int backNum = openVolumeLen > 3 ? 2 : (openVolumeLen == 3 ? 1 : 0);
                for (int i = 0; i < backNum; i++) {
                    redisService.leftPop(openVolumeKey + contractCode);
                }
                logStr = "SWAP-??????????????????" + backNum;
            }
            if (upStratgy == PubConst.UPSTRATGY.PLL) {
                if (openVolumeLen + 1 >= PubConst.PLLNUM) {
                    //???????????? ??????2?????????
                    redisService.listTrim(openVolumeKey + contractCode,-2,-1);
                    logStr = "SWAP-?????????????????????????????????????????????" + winVolume;
                } else {
                    //?????????????????????
                    redisService.lpush(openVolumeKey + contractCode,String.valueOf(getMaxOpenVolume(contractCode)));
                    logStr = "SWAP-?????????????????????????????????" + winVolume;
                }
            }
            logStr += " ?????????" + newestRealProfit;
            log.info(logStr);
            mailService.sendMail("SWAP-??????????????????",logStr,"");
        }
    }
}
