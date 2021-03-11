//package com.contract.harvest.service;
//
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.contract.harvest.entity.HuobiEntity;
//import com.contract.harvest.tools.IndexCalculation;
//import org.apache.http.HttpException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
//@Service
//public class ChaseStrategy {
//
//    @Resource
//    private MailService mailService;
//    @Resource
//    private CacheService cacheService;
//    @Resource
//    private VerifyParams verifyParams;
//    @Resource
//    private ScheduledService scheduledService;
//    @Resource
//    private HuobiEntity huobiEntity;
//    @Value("${space.default_cs}")
//    private String default_cs;
//    @Value("${space.volume}")
//    private String volume;
//
//    private static final Logger logger = LoggerFactory.getLogger(ChaseStrategy.class);
//
//    public double[] getRSI(String symbol) throws InterruptedException, IOException, HttpException {
//        //获取最新的一个kline数据
//        JSONObject newestKline = verifyParams.getNewestKline(symbol);
//        if (newestKline == null) {
//            logger.info("等待数据刷新。。。");
//            return null;
//        }
//        //获取最近的200个kline数据
//        JSONArray manyKline = verifyParams.getManyKline(symbol);
//        manyKline.set(manyKline.size() - 1, newestKline);
//        double[] closeArr = verifyParams.getArrColumn(manyKline,"close",false);
//        double[] rsi = IndexCalculation.RSI(closeArr, 14);
//        //校验最后两根k线的时间
//        boolean timeFlag = verifyParams.checkKlineTime(manyKline);
////        if (!timeFlag) {
////            logger.info("时间错误。。。。");
////            return null;
////        }
//        return rsi;
//    }
//    /**
//     * 指标RSI，合约多空
//     */
//    public void dealIndex(String symbol) throws IOException, HttpException, InterruptedException {
////        JSONObject contractInfo = verifyParams.getContractInfo(symbol, "this_week", "");
////        String contractCode = contractInfo.getString("contract_code");
////        double[] rsi = getRSI(symbol);
////        if (rsi == null) {
////            return;
////        }
////        double rsiValue = rsi[rsi.length - 1];
////        //获取持仓信息
////        JSONObject spaceInfo = verifyParams.getSpaceInfoObj(symbol, contractCode);
////        //成交数量
////        int contractVolume = spaceInfo == null ? 0 : spaceInfo.getIntValue("volume");
////        String queLenKey = CacheService.WAIT_ORDER_QUEUE;
////        //队列是否有订单等待处理
////        Long queLen = cacheService.get_queue_len(queLenKey + symbol);
////        if (queLen > 0) {
////            cacheService.remindOrderHandle("已有订单等待处理,币:" + symbol);
////            Thread.sleep(1000);
////            return;
////        }
////        Order order = null;
////        double flagRsi = 0;
////        //如果rsi波动幅度0.5以上，就睡眠等待
////        Thread.sleep(2000L);
////        rsi = getRSI(symbol);
////        if (rsi == null) {
////            return;
////        }
////        flagRsi = Math.abs(Arith.sub(rsi[rsi.length - 1],rsiValue));
////        rsiValue = rsi[rsi.length - 1];
//////        if (Arith.compareNum(flagRsi,0.3) && (!Arith.compareNum(rsiValue,88) || !Arith.compareNum(11,rsiValue))) {
//////            logger.info("rsi波动幅度："+flagRsi);
//////            Thread.sleep(5000L);
//////            return;
//////        }
////        String log = symbol+"当前rsi"+rsiValue+"rsi波动幅度："+flagRsi;
////        //做空
//////        if (Arith.compareNum(rsiValue, 78) && contractVolume < 5) {
//////            order = verifyParams.getPlaceOrder(symbol, "open", contractCode, "sell","");
//////        }
//////        if (Arith.compareNum(rsiValue, 81) && contractVolume < 8) {
//////            order = verifyParams.getPlaceOrder(symbol, "open", contractCode, "sell","");
//////        }
////        if (Arith.compareNum(rsiValue, 84) && contractVolume < 20) {
////            order = verifyParams.getPlaceOrder(symbol, "open", contractCode, "sell","");
////        }
////        if (Arith.compareNum(rsiValue, 90) && contractVolume < 40) {
////            order = verifyParams.getPlaceOrder(symbol, "open", contractCode, "sell","");
////        }
////
////        //做多
////        if (Arith.compareNum(20, rsiValue) && contractVolume < 5) {
////            order = verifyParams.getPlaceOrder(symbol, "open", contractCode, "buy","");
////        }
////        if (Arith.compareNum(16, rsiValue) && contractVolume < 10) {
////            order = verifyParams.getPlaceOrder(symbol, "open", contractCode, "buy","");
////        }
////        if (Arith.compareNum(15, rsiValue) && contractVolume < 20) {
////            order = verifyParams.getPlaceOrder(symbol, "open", contractCode, "buy","");
////        }
////        if (Arith.compareNum(11, rsiValue) && contractVolume < 40) {
////            order = verifyParams.getPlaceOrder(symbol, "open", contractCode, "buy","");
////        }
////
////        //平仓 成交数量大于0  并且rsi在45～52之间
////        if (contractVolume > 0) {
////            String direction = spaceInfo.getString("direction").equals("buy") ? "sell" : "buy";
//////            String volume = spaceInfo.getString("volume");
////            int available = spaceInfo.getIntValue("available");
////            if (
////                direction.equals("sell") &&
////                Arith.compareNum(rsiValue, 68)
////            ) {
////                if (available < 1) {
////                    verifyParams.backoutOrder(symbol);
////                    return;
////                }
////                order = verifyParams.getPlaceOrder(symbol, "close", contractCode, direction,"");
////            }
////
////            if (
////                    direction.equals("buy") &&
////                    Arith.compareNum(36, rsiValue)
////            ) {
////                if (available < 1) {
////                    verifyParams.backoutOrder(symbol);
////                    return;
////                }
////                order = verifyParams.getPlaceOrder(symbol, "close", contractCode, direction,"");
////            }
////        }
////        if (order != null) {
////            String orderJsonStr = JSON.toJSONString(order);
////            logger.info("下单:"+log+orderJsonStr);
////            cacheService.pushData(queLenKey + symbol, order);
////            //订阅通知
////            cacheService.inform_sub("order_queue", "ChaseStrategy-hadleQueueOrder");
////        }
////        //提醒
////        cacheService.informBasis(log);
//    }
//
//    public void dealIndexV2(String symbol) throws IOException, HttpException, InterruptedException {
//        //获取最近的200个kline数据
//        JSONArray manyKline = verifyParams.getManyKline(symbol);
//        Map<String, double[]> KlineMap = verifyParams.getKlineMapColumn(manyKline,null,false);
//        double[] rsi = IndexCalculation.RSI(KlineMap.get("close"), 14);
//        double[] mfi = IndexCalculation.mfi(KlineMap.get("high"), KlineMap.get("low"),KlineMap.get("close"),KlineMap.get("vol"),KlineMap.get("id"),0);
//        Map<String,ArrayList<Long>> MapIds = IndexCalculation.patternRecognition(KlineMap.get("open"), KlineMap.get("high"), KlineMap.get("low"),KlineMap.get("close"),KlineMap.get("id"),"cdlEngulfing");
//        ArrayList<Long> tids = MapIds.get("tids");
//        assert tids != null;
//        double[] idArr =  Arrays.copyOfRange(KlineMap.get("id"), 14,KlineMap.get("id").length);
//        SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        for (Long id : tids) {
//            int key = Arrays.binarySearch(idArr,id);
//            if (key == -1) {
//                continue;
//            }
//            System.out.println("mfi:"+mfi[key]);
//            System.out.println("rsi:"+rsi[key]);
//            Date date = new Date(new Double(id +"000").longValue());
//            System.out.println(time.format(date));
//            System.out.println("=====================");
//        }
////        Arrays.sort(rsi);
////        int key = Arrays.binarySearch(rsi,57.3316061472447);
////        System.out.println("key:"+key);
////        System.out.println("value"+rsi[387]);
////        double[] nowIdArr = Arrays.copyOfRange(KlineMap.get("id"), 14,KlineMap.get("id").length);
////        for (int i = nowIdArr.length; i > 0; i--) {
////            String mill = String.valueOf(new Double(nowIdArr[i-1]).longValue());
////            Date date = new Date(new Double(mill +"000").longValue());
////            System.out.println(time.format(date));
////            System.out.println(rsi[i-1]);
////            System.out.println("=====================");
////        }
////        System.out.println(cdl2Crows);
////        System.out.println("rsilen:"+mfi.length);
////        System.out.println(Arrays.toString(rsi));
////        System.out.println(Arrays.toString(mfi));
//    }
//
//    public void hadleQueueOrder() throws IOException, HttpException, InterruptedException {
////        Set<String> symbolList = scheduledService.getSymbol();
////        for (String symbol : symbolList) {
////            //队列是否有订单等待处理
////            Long que_len = cacheService.get_queue_len(CacheService.WAIT_ORDER_QUEUE + symbol);
////            if (que_len == 0) {
////                continue;
////            }
////            //获取订单
////            Object orderObj = cacheService.getDataByIndex(CacheService.WAIT_ORDER_QUEUE+symbol);
////            Order order = (Order)orderObj;
////            verifyParams.handleOrder(symbol,order);
////            //删除订单队列
////            cacheService.deleteData(CacheService.WAIT_ORDER_QUEUE+symbol);
////            //刷新仓位
////            JSONObject positionInfo = verifyParams.getContractAccountPositionInfo(symbol);
////            int sumVolume = positionInfo.getIntValue("sumVolume");
////            if (sumVolume > 15) {
////                mailService.sendMail("持仓信息",positionInfo.toJSONString(),"");
////            }
////        }
//    }
//}