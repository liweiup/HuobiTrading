//package com.contract.harvest.service;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.contract.harvest.entity.HuobiEntity;
//import com.contract.harvest.common.Topic;
//import com.contract.harvest.tools.*;
//import com.huobi.common.request.Order;
//import org.apache.commons.lang.StringUtils;
//import org.apache.http.HttpException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
//@Service
//public class VerifyParams {
//
//    @Resource
//    private HuobiEntity huobiEntity;
//
//    @Resource
//    private RedisService redisService;
//    @Resource
//    private ChaseStrategy chaseStrategy;
//
//    @Resource
//    private MailService mailService;
//    @Resource
//    private ScheduledService scheduledService;
//
//    @Value("${space.buy_small_vol}")
//    private int buy_small_vol;
//
//    @Value("${space.default_week_type}")
//    private String default_week_type;
//
//    @Value("${space.default_cs}")
//    private String default_cs;
//
//    @Value("${space.lever_rate}")
//    private String lever_rate;
//
//    @Value("${space.order_price_type}")
//    private String order_price_type;
//    @Value("${space.volume}")
//    private String volume;
//
//    //如"BTC_CW"表示BTC当周合约，"BTC_NW"表示BTC次周合约，"BTC_CQ"表示BTC季度合约
//    private final String next_quarter_flag = "_NQ";
//    private final String quarter_flag = "_CQ";
//    private final String nw_flag = "_NW";
//    private final String cw_flag = "_CW";
//
//    private final Integer flag_ts = 5000;
//
//    private static final Logger logger = LoggerFactory.getLogger(VerifyParams.class);
//
//    /**
//     * 获取基差的百分比与具体基差
//     */
//    public Map<String, String> getBasisFlag(double priceOne, double priceTwo) {
//        Map<String, String> priceMap = new HashMap<>();
//        if (Arith.compareNum(priceOne,priceTwo)) {
//            priceMap.put("percent", String.valueOf(Arith.div(Arith.sub(priceOne,priceTwo),priceOne)));
//        } else {
//            priceMap.put("percent", String.valueOf(Arith.div(Arith.sub(priceTwo,priceOne),priceTwo)));
//        }
//        return priceMap;
//    }
//    /**
//     * 获取合约标识
//     */
//    public String[] getContractCode(String contract_type) {
//        //qn季度和次周 qc季度和本周 nqq次季和本季 nqn次季度和次周 nqc次季和本周
//        switch (contract_type) {
//            case "qn":
//                return new String[]{"quarter","next_week",quarter_flag,nw_flag};
//            case "qc":
//                return new String[]{"quarter","this_week",quarter_flag,cw_flag};
//            case "nqq":
//                return new String[]{"next_quarter","quarter",next_quarter_flag,quarter_flag};
//            case "nqn":
//                return new String[]{"next_quarter","next_week",next_quarter_flag,nw_flag};
//            case "nqc":
//                return new String[]{"next_quarter","this_week",next_quarter_flag,cw_flag};
//            case "nc":
//                return new String[]{"next_week","this_week",nw_flag,cw_flag};
//            default:
//                return new String[]{};
//        }
//    }
//    /**
//     * 获取合约标识
//     */
//    public String getContractFlag(String contract_type) {
//        switch (contract_type) {
//            case "next_quarter":
//                return next_quarter_flag;
//            case "quarter":
//                return quarter_flag;
//            case "next_week":
//                return nw_flag;
//            case "this_week":
//                return cw_flag;
//            default:
//                return "";
//        }
//    }
//    /**
//     * 获取正在交易的合约
//     */
//    public String getContractFlag(String symbol,String contract_code) {
//        //qn季度和次周 qc季度和本周 nqq次季和本季 nqn次季度和次周 nqc次季和本周
//        Map<String,String> ContractFlag = new HashMap<>();
//        ContractFlag.put(symbol+next_quarter_flag+"-"+symbol+quarter_flag,"nqq");
//        ContractFlag.put(symbol+next_quarter_flag+"-"+symbol+nw_flag,"nqn");
//        ContractFlag.put(symbol+next_quarter_flag+"-"+symbol+cw_flag,"nqc");
//        ContractFlag.put(symbol+quarter_flag+"-"+symbol+nw_flag,"qn");
//        ContractFlag.put(symbol+quarter_flag+"-"+symbol+cw_flag,"qc");
//        ContractFlag.put(symbol+nw_flag+"-"+symbol+cw_flag,"nc");
//        return ContractFlag.get(contract_code);
//    }
//
//    /**
//     * 校对合约
//     * 选择季度&次周 or 季度&本周
//     */
////    public Map<String, String> checkContractDeal(String symbol,String buy_type) throws IOException, HttpException {
////        Map<String,String> priceMap = new HashMap<String,String>();
////        //合约信息
////        JSONObject cq_contract_info,nc_contract_info;
////        //有无正在进行的交易 合约代码
////        String deal_flag = cacheService.get_redis_key_deal_flag(symbol);
////        //交易周期
////        String price_flag;
////        //合约标识，用来获取买卖价格
////        String contract_code;
////        if (!buy_type.equals("")) {
////            deal_flag = "";
////            default_week_type = buy_type;
////        }
////        if (deal_flag.equals("")) {
////            String[] contractCodeArr = getContractCode(default_week_type);
////            cq_contract_info = getContractInfo(symbol,contractCodeArr[0],"");
////            nc_contract_info = getContractInfo(symbol,contractCodeArr[1],"");
////            contract_code = symbol+contractCodeArr[2]+"-"+symbol+contractCodeArr[3];
////            price_flag = default_week_type;
////        }else{
////            //交易周期
////            String[] deal_flag_arr = deal_flag.split("-");
////            //合约代码
////            if (deal_flag_arr.length != 2)
////            {
////                logger.error("交易代码出错:["+ Arrays.toString(deal_flag_arr) +"]");
////                mailService.sendMail("交易代码出错",Arrays.toString(deal_flag_arr),"");
////                return null;
////            }
////            int may_q_date = Integer.parseInt(deal_flag_arr[0].substring(deal_flag_arr[0].length()-6));
////            int may_m_date = Integer.parseInt(deal_flag_arr[1].substring(deal_flag_arr[1].length()-6));
////            //合约信息
////            if (may_q_date > may_m_date) {
////                cq_contract_info = getContractInfo(symbol,"",deal_flag_arr[0]);
////                nc_contract_info = getContractInfo(symbol,"",deal_flag_arr[1]);
////            } else {
////                cq_contract_info = getContractInfo(symbol,"",deal_flag_arr[1]);
////                nc_contract_info = getContractInfo(symbol,"",deal_flag_arr[0]);
////            }
////            String cq_contract_type = cq_contract_info.getString("contract_type");
////            String nc_contract_type = nc_contract_info.getString("contract_type");
////            if (cq_contract_type.equals(nc_contract_type)) {
////                logger.error("交易代码出错ci:["+ Arrays.toString(deal_flag_arr)+cq_contract_info+nc_contract_info+"]");
////                mailService.sendMail("交易代码出错ci",Arrays.toString(deal_flag_arr)+cq_contract_info+nc_contract_info,"");
////                return null;
////            }
////            contract_code = symbol+getContractFlag(cq_contract_type)+"-"+symbol+getContractFlag(nc_contract_type);
////            price_flag = getContractFlag(symbol,contract_code);
////        }
////        priceMap.put("quarter_code",String.valueOf(cq_contract_info.get("contract_code")));
////        priceMap.put("week_code",String.valueOf(nc_contract_info.get("contract_code")));
////        priceMap.put("contract_type",contract_code);
////        priceMap.put("price_flag",price_flag);
////        return priceMap;
////    }
//    /**
//     * 获取下单实例
//     * @param volume 张数
//     * @param offset open or close
//     * @param lever_rate 杠杆
//     */
//    public List<Order> getListOrderV2(String symbol,String volume,String offset,String lever_rate,String order_price_type,Map<Integer, Map<String, String>> contractAllPrice,JSONObject spaceInfoObj) {
//        List<Order> orders = new ArrayList<>();
//        if (contractAllPrice.size() != 2) {
//            return orders;
//        }
//        for (int i = 0 ; i < contractAllPrice.size(); i++) {
//            Map<String, String> contractInfo = contractAllPrice.get(i);
//            String now_offset = offset.equals("") ? contractInfo.get("offset") : offset;
//            String price_flag = now_offset + "Price";
//            String direction = contractInfo.get("direction");
//            String contractCode = contractInfo.get("contractCode");
//            if (offset.equals("close") && spaceInfoObj != null) {
//                direction = spaceInfoObj.getString(contractCode+"_close");
//                int flag_volume = spaceInfoObj.getJSONObject(contractCode).getIntValue("volume");
//                if (Integer.parseInt(volume) > flag_volume) {
//                    volume = String.valueOf(flag_volume);
//                }
//            }
//            Order order = new Order(symbol, "", contractCode, getClientOrderId(), contractInfo.get(price_flag), volume, direction , now_offset, lever_rate, order_price_type);
//            orders.add(order);
//        }
//        return orders;
//    }
//    /**
//     * 获取可平仓位与方向
//     */
//    public JSONObject getContractAccountPositionInfo(String symbol) throws IOException, HttpException,NullPointerException {
//        String positionInfo = huobiEntity.getContractAccountPositionInfo(symbol);
//        Map<String,Object> flagDirection = new HashMap<>();
//        JSONObject positionInfoJson = JSONObject.parseObject(positionInfo);
//        if (JSONObject.parseObject(positionInfo) == null || positionInfo.equals("") || positionInfoJson.getString("status").equals("error")) {
//            mailService.sendMail("getContractAccountPositionInfo出错",positionInfo,"");
//            return getContractAccountPositionInfo(symbol);
//        }
//        JSONObject positions = positionInfoJson.getJSONArray("data").getJSONObject(0);
//        JSONArray positionsArr = positions.getJSONArray("positions");
//        double profit = 0;
//        int sumVolume = 0;
//        for (Object position:positionsArr) {
//            JSONObject positionObj = JSONObject.parseObject(String.valueOf(position));
//            String contract_code = positionObj.getString("contract_code");
//            String direction = positionObj.getString("direction");
//            flagDirection.put(contract_code+"_close",direction.equals("sell") ? "buy" : "sell");
//            profit = Arith.add(profit,positionObj.getDoubleValue("profit"));
//            sumVolume += positionObj.getIntValue("volume");
//            flagDirection.put(contract_code,positionObj);
//        }
//        flagDirection.put("sumVolume",String.valueOf(sumVolume));
//        flagDirection.put("profit",String.valueOf(profit));
//        //保存持仓信息
//        redisService.setValue(CacheService.SPACE_INFO+symbol,JSON.toJSONString(flagDirection));
//        return (JSONObject) JSON.toJSON(flagDirection);
//    }
//    /**
//     * 获取购买价格，与卖出价格
//     */
//    public Map<String, Object> getBidAskPrice(String qKey,Integer index) throws NullPointerException, InterruptedException {
//        String flagDepth = redisService.hashGet(CacheService.HUOBI_SUB,qKey);
//        if (flagDepth.equals("")) {
//            Thread.sleep(1000);
//            return getBidAskPrice(qKey,index);
//        }
//        String depth = flagDepth;
//        JSONObject depthObj = JSONObject.parseObject(depth);
//        if (depthObj == null) {
//            Thread.sleep(1000);
//            return getBidAskPrice(qKey,index);
//        }
//        depthObj = depthObj.getJSONObject("tick");
//        JSONArray bidsArr = depthObj.getJSONArray("bids").getJSONArray(index);
//        JSONArray asksArr = depthObj.getJSONArray("asks").getJSONArray(index);
//        Map<String, Object> bidAskPriceMap = new HashMap<>();
//        bidAskPriceMap.put("bids", bidsArr);
//        bidAskPriceMap.put("asks", asksArr);
//        bidAskPriceMap.put("ts",depthObj.getLongValue("ts"));
//        return bidAskPriceMap;
//    }
//    /**
//     * 获取指数价格
//     */
//    public JSONObject getIndexPrice(String symbol) throws NullPointerException, InterruptedException {
//        String key =  Topic.formatChannel(Topic.INDEX_SUB,symbol+"-USD",0);
//        String indexInfo = redisService.hashGet(CacheService.HUOBI_SUB,key);
//        if (JSONObject.parseObject(indexInfo) == null) {
//            Thread.sleep(1000);
//            return getIndexPrice(symbol);
//        }
//        return JSONObject.parseObject(indexInfo).getJSONObject("tick");
//    }
//    /**
//     * 获取开清仓买卖价格
//     */
//    public Map<String,String> getOCPrice(String priceFlag,JSONArray asksArr,JSONArray bidsArr,String symbol,String codeFlag,String contractCode) throws IOException, HttpException {
//        Map<String,String> OCMap = new HashMap<>();
//        //获取price_tick
//        JSONObject contract_info = getContractInfo(symbol,"this_week","");
//        double price_tick = contract_info.getDoubleValue("price_tick");
//
//        double openPrice = priceFlag.equals("ask") ? Arith.sub(asksArr.getDoubleValue(0),price_tick) : Arith.add(bidsArr.getDoubleValue(0),price_tick);
//        double closePrice = priceFlag.equals("ask") ? Arith.sub(bidsArr.getDoubleValue(0),price_tick) : Arith.add(asksArr.getDoubleValue(0),price_tick);
//        OCMap.put("openPrice",Double.toString(openPrice));
//        OCMap.put("closePrice",Double.toString(closePrice));
//        OCMap.put("symbol",symbol+codeFlag);
//        OCMap.put("direction",priceFlag.equals("ask") ? "sell" : "buy");
//        OCMap.put("contractCode",contractCode);
//        return OCMap;
//    }
//    /**
//     * 获取季度，次周，本周合约价格V2
//     * websocket获取
//     */
//    public Map<Integer,Map<String,String>> getContractAllPriceV2(String symbol, Map<String, String> priceMap,String tradeFlag) throws IOException, HttpException, InterruptedException, NullPointerException,IndexOutOfBoundsException {
//        Map<Integer,Map<String,String>> OCMap = new HashMap<>();
//        String price_flag = priceMap.get("price_flag");
//        String[] contractCodeArr = getContractCode(price_flag);
//        String qKey = Topic.formatChannel(Topic.DEPTH_SUB,symbol+contractCodeArr[2],6);
//        String wKey = Topic.formatChannel(Topic.DEPTH_SUB,symbol+contractCodeArr[3],6);
//        Map<String, Object> mayQuarterDepth = getBidAskPrice(qKey,1);
//        Map<String, Object> mayWeekDepth = getBidAskPrice(wKey,1);
//        Long mayQuarterTs = (Long) mayQuarterDepth.get("ts");
//        Long mayWeekTs = (Long) mayWeekDepth.get("ts");
//        if (Math.abs(mayQuarterTs - mayWeekTs) > flag_ts) {
//            logger.error("depth时间出错:[mayQuarterTs，mayWeekTs]"+Math.abs(mayQuarterTs - mayWeekTs));
//            mailService.sendMail("depth时间出错",mayQuarterTs+"--"+mayWeekTs,"");
//            Thread.sleep(1000);
//            return getContractAllPriceV2(symbol,priceMap,tradeFlag);
//        }
//        //买一卖一价格
//        JSONArray mayQuarterBids = (JSONArray) mayQuarterDepth.get("bids");
//        JSONArray mayQuarterAsks = (JSONArray) mayQuarterDepth.get("asks");
//        JSONArray mayWeekBids = (JSONArray) mayWeekDepth.get("bids");
//        JSONArray mayWeekAsks = (JSONArray) mayWeekDepth.get("asks");
//        //指数价格
//        JSONObject indexPrice = getIndexPrice(symbol);
//        String mayQuarterPriceFlag = tradeFlag.equals("2") ? "ask" : "bid";
//        String mayWeekPriceFlag = mayQuarterPriceFlag.equals("ask") ? "bid" : "ask";
//        //比较指数价格和买一价格的差距
//        double mayQuarterDiffPrice = Math.abs(Arith.sub(mayQuarterBids.getDoubleValue(0),indexPrice.getDoubleValue("close")));
//        double mayWeekDiffPrice = Math.abs(Arith.sub(mayWeekBids.getDoubleValue(0),indexPrice.getDoubleValue("close")));
//        OCMap.put(1,getOCPrice(mayWeekPriceFlag,mayWeekAsks,mayWeekBids,symbol,contractCodeArr[3],priceMap.get("week_code")));
//        OCMap.put(0,getOCPrice(mayQuarterPriceFlag,mayQuarterAsks,mayQuarterBids,symbol,contractCodeArr[2],priceMap.get("quarter_code")));
////        System.out.println(OCMap);
////        System.out.println("=============================================================================");
////        System.out.println(mayQuarterPriceFlag);
////        System.out.println(mayWeekPriceFlag);
////        System.out.println("===========");
////        System.out.println("qb"+mayQuarterBids);
////        System.out.println("qa"+mayQuarterAsks);
////        System.out.println("===========");
////        System.out.println("wb"+mayWeekBids);
////        System.out.println("wa"+mayWeekAsks);
////        System.out.println("===========");
////        System.out.println(OCMap);
////        System.out.println("===========");
////        System.out.println(indexPrice);
////        System.out.println(symbol);
////        System.out.println("=============================================================================");
////        System.exit(0);
//        return OCMap;
//    }
////    public void getContractOrderInfo(String symbol,String direction) throws InterruptedException {
////        String depthKey = Topic.formatChannel(Topic.DEPTH_SUB,symbol+default_cs,6);
////        Map<String, Object> depth = getBidAskPrice(depthKey,1);
////        String depthFlag = direction.equals("buy") ? "asks" : "bids";
////        depth.get(depthFlag);
////        System.out.println(mayQuarterDepth);
////    }
//    /**
//     * 获取下单实例
//     * @param symbol 币种
//     * @param offset open or close
//     * @param contractCode 交易周期代码
//     * @param direction 交易方向
//     */
//    public Order getPlaceOrder(String symbol,String offset,String contractCode,String direction,String dealVolume) throws InterruptedException, IOException, HttpException {
//        dealVolume = dealVolume.equals("") ? volume : dealVolume;
//        Order order = new Order(symbol, "", contractCode, getClientOrderId(), "0", dealVolume, direction , offset, lever_rate, order_price_type);
//        order = getNewOrder(order,0);
//        return order;
//    }
//
//    /**
//     * 获取最新价钱的订单
//     */
//    public Order getNewOrder(Order order,Integer try_num) throws IOException, HttpException, InterruptedException {
//        JSONObject contract_info = getContractInfo(order.getSymbol(),"",order.getContractCode());
//        //获取最新的价格
//        String contractFlag = getContractFlag(contract_info.getString("contract_type"));
//        String key = Topic.formatChannel(Topic.DEPTH_SUB,order.getSymbol()+contractFlag,6);
//        Map<String, Object> depth = getBidAskPrice(key,0);
//        String direction = order.getDirection().equals("buy") ? "asks" : "bids";
//        JSONArray depth_arr = (JSONArray) depth.get(direction);
//        double new_price = depth_arr.getDoubleValue(0);
//        if (direction.equals("asks")) {
//            new_price = Arith.sub(new_price,contract_info.getDoubleValue("price_tick"));
//        } else {
//            new_price = Arith.add(new_price,contract_info.getDoubleValue("price_tick"));
//        }
//        if (try_num > 0) {
//            order.setClientOrderId(getClientOrderId());
//        }
//        order.setPrice(String.valueOf(new_price));
////        order.setPrice("180");
//        return order;
//    }
//
//    /**
//     * 获取合约信息
//     */
//    public JSONObject getContractInfo(String symbol,String contract_type,String deal_flag) throws IOException, HttpException {
//        String cq_contract_info = huobiEntity.getContractInfo("","","");
//        JSONArray contract_info_arr = JSONObject.parseObject(cq_contract_info).getJSONArray("data");
//        for (Object contract_info:contract_info_arr) {
//            JSONObject contract_info_obj = (JSONObject) contract_info;
//            if (symbol.equals(contract_info_obj.getString("symbol")) && contract_type.equals(contract_info_obj.getString("contract_type"))) {
//                return contract_info_obj;
//            }
//            if (contract_info_obj.getString("contract_code").equals(deal_flag)) {
//                return contract_info_obj;
//            }
//        }
//        return null;
//    }
//    /**
//     * 获取订单id
//     */
//    public Map<String,String> getOrderIdStr(String order_info_str) throws NullPointerException{
//        Map<String,String> OrderMap = new HashMap<>();
//        JSONObject order_info_obj = JSONObject.parseObject(order_info_str);
//        JSONObject order_data = (JSONObject) order_info_obj.get("data");
//        String symbol = order_info_obj.getString("symbol");
//        String offset = order_info_obj.getString("offset");
//        String err_code = order_info_obj.getString("err_code");
//        if (err_code != null) {
//            return OrderMap;
//        }
//        List<String> order_arr = new ArrayList<>();
//        Object[] success_order = order_data.getJSONArray("success").toArray();
//        for (Object flag_order : success_order) {
//            JSONObject order = (JSONObject)flag_order;
//            order_arr.add(order.getString("order_id_str"));
//        }
//        String order_id_str =  StringUtils.join(order_arr,",");
//        OrderMap.put("symbol",symbol);
//        OrderMap.put("order_id_str",order_id_str);
//        OrderMap.put("offset",offset);
//        return OrderMap;
//    }
//
//    /**
//     * 获取订单id
//     */
//    public String getOrderId(String order_info_str) throws NullPointerException{
//        JSONObject data = JSONObject.parseObject(order_info_str).getJSONObject("data");
//        if (data == null) {
//            mailService.sendMail("订单信息错误",order_info_str,"");
//            logger.info(order_info_str);
//            return null;
//        }
//        JSONArray successOrder = data.getJSONArray("success");
//        if (successOrder.size() == 0) {
//            mailService.sendMail("订单信息错误",order_info_str,"");
//            logger.info(order_info_str);
//            return null;
//        }
//        return successOrder.getJSONObject(0).getString("order_id_str");
//    }
//    /**
//     * 获取订单信息
//     */
//    public String getOrderInfo(String order_id_str, String symbol) throws IOException, HttpException, NullPointerException {
////        String contractOrderInfo = huobiEntity.getcontractOrderInfo(order_id_str, "", symbol);
//        return JSONObject.parseObject("").getJSONArray("data").getJSONObject(0).toJSONString();
//    }
//    /**
//     * 获取订单信息
//     */
//    public JSONObject getOrderInfoByStr(Order order, String symbol,String orderInfoKey,String clientOrderId,int try_num) throws NullPointerException, IOException, HttpException, InterruptedException {
//        String orderInfo = null,orderInfoStr,order_id_str;
//        orderInfoStr = redisService.hashGet(orderInfoKey,clientOrderId);
//        if (orderInfoStr.equals("")) {
//            //下单-存放订单成交信息
//            String contractOrder = huobiEntity.futureContractBatchorder(order);
//            order_id_str = getOrderId(contractOrder);
//        } else {
//            order_id_str = JSONObject.parseObject(orderInfoStr).getString("order_id_str");
//        }
//        if (order_id_str == null) {
//            return null;
//        }
//        int i = 0;
//        while (i < try_num) {
//            orderInfo = getOrderInfo(order_id_str,symbol);
//            if (orderInfo.equals("")) {
//                continue;
//            }
//            redisService.hashSet(orderInfoKey,clientOrderId,orderInfo);
//            JSONObject orderInfoObj = JSONObject.parseObject(orderInfo);
//            //如果全部成交
//            if (orderInfoObj.getIntValue("status") == 6 || orderInfoObj.getIntValue("status") == 7) {
//                break;
//            }
//            Thread.sleep(2000L);
//            i++;
//        }
//        return JSONObject.parseObject(orderInfo);
//    }
//
//    //获取当前时间
//    public String getNowDatetime() {
//        Date d = new Date();
//        SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        return time.format(d);
//    }
//    public String getNowDatetime(String format) {
//        Date d = new Date();
//        SimpleDateFormat time = new SimpleDateFormat(format);
//        return time.format(d);
//    }
//    //获取订单id
//    public String getClientOrderId() {
//        return String.valueOf(UUID.randomUUID().toString().hashCode()).replace("-", "");
//    }
//
//    //获取最新的kline数据
//    public JSONObject getNewestKline(String symbol) {
//        String lineKey = Topic.formatChannel(Topic.KLINE_SUB,symbol+default_cs,1).toUpperCase();
//        String lineData = redisService.hashGet(CacheService.HUOBI_SUB,lineKey);
//        return lineData.equals("") ? null : JSON.parseObject(lineData).getJSONObject("tick");
//    }
//    //获取最新的多个kline数据
//    public JSONArray getManyKline(String symbol) throws InterruptedException, IOException, HttpException {
//        String manyLineStr = redisService.hashGet(CacheService.HUOBI_KLINE,symbol);
//        if (manyLineStr.equals("")) {
//            Thread.sleep(1000L);
//            mailService.sendMail("getManyKline出错",manyLineStr,"");
//            return getManyKline(symbol);
//        }
//        return JSONObject.parseObject(manyLineStr).getJSONArray("data");
//    }
//
//    /**
//     * checkKlineTime
//     */
//    public boolean checkKlineTime(JSONArray manyKline) {
//        JSONObject prevKline = manyKline.getJSONObject(manyKline.size()-2);
//        JSONObject lastKline = manyKline.getJSONObject(manyKline.size()-1);
//        if (lastKline.getLongValue("id") < prevKline.getLongValue("id")) {
//            return false;
//        }
//        //上一个15分钟的时间戳
//        List<Long> ds = TakeDate.getDateList(5);
//        Long lastUnix = ds.get(ds.size()-1);
//        return lastUnix == lastKline.getLongValue("id");
//    }
//    /**
//     * 获取持仓信息
//     */
//    public JSONObject getSpaceInfoObj(String symbol,String contractCode) {
//        JSONObject spaceInfoObj = JSON.parseObject(redisService.getValue(CacheService.SPACE_INFO+symbol));
//        return spaceInfoObj == null ? null : spaceInfoObj.getJSONObject(contractCode);
//    }
//
////    public void handleOrder(String symbol,Order order) throws InterruptedException, IOException, HttpException {
////        String clientOrderId = order.getClientOrderId();
////        //订单成交信息key
////        String orderInfoKey = String.format(CacheService.ORDER_INFO,symbol);
////        //获取订单成交信息
////        JSONObject orderInfoObj;
////        //获取订单成交信息
////        orderInfoObj = getOrderInfoByStr(order,symbol,orderInfoKey,clientOrderId,10);
////        if (orderInfoObj == null) {
////            logger.info("orderInfoObj为空");
////            return;
////        }
////        int orderStatus = orderInfoObj.getIntValue("status");
////        int[] partSuccessArr = {4,5};
////        if (orderStatus == 6) {
////            logger.info("全部成交:"+orderInfoObj);
////        } else if (Arrays.binarySearch(partSuccessArr,orderStatus) >= 0) {
////            //撤单
////            huobiEntity.futureContractCancel(orderInfoObj.getString("order_id"),"",symbol);
////            logger.info("部分成交:"+orderInfoObj);
////        } else {
////            //撤单
////            huobiEntity.futureContractCancel(orderInfoObj.getString("order_id"),"",symbol);
////            logger.info("撤单:"+orderInfoObj);
////        }
////        int trade_volume = orderInfoObj.getIntValue("trade_volume");
////        /**
////         * 挂一个平仓单
////         */
////        if (orderInfoObj.getString("offset").equals("open") && trade_volume >= 1) {
////            //获取最新订单
////            Order closeOrder = getNewOrder(order,1);
////            String closeDirection = orderInfoObj.getString("direction").equals("sell") ? "buy" : "sell";
////            closeOrder.setDirection(closeDirection);
////            closeOrder.setOffset("close");
////            //第一个订单的成交数量
////            closeOrder.setVolume(String.valueOf(trade_volume));
////            double price = orderInfoObj.getDoubleValue("price");
////            double decimalsNum = getDecimalsNum(symbol);
////            if (closeDirection.equals("buy")) {
////                price = Arith.sub(price,Arith.getPercentNum(price,decimalsNum));
////            } else {
////                price = Arith.add(price,Arith.getPercentNum(price,decimalsNum));
////            }
////            closeOrder.setPrice(String.valueOf(price));
////            //挂单
////            JSONObject closeOrderObj = getOrderInfoByStr(closeOrder,symbol,orderInfoKey,closeOrder.getClientOrderId(),1);
////            //放入挂单队列
////            cacheService.pushData(CacheService.PUT_CLOSE_ORDER_QUEUE + symbol, orderInfoObj.toJSONString());
////            mailService.sendMail("平仓挂单信息",closeOrderObj.toJSONString(),"");
////        }
////        mailService.sendMail("订单成交信息",orderInfoObj.toJSONString(),"");
////    }
//    /**
//     * 获取平仓价格
//     */
//    public double getDecimalsNum(String symbol) throws InterruptedException, IOException, HttpException {
//        double decimalsNum = 10;
//        //获取rsi
//        double[] rsi =  chaseStrategy.getRSI(symbol);
//        if (rsi == null) {
//            return decimalsNum;
//        }
//        double rsiValue = rsi[rsi.length - 1];
//        double lrate = Double.parseDouble(lever_rate);
//        if (Arith.compareNum(rsiValue, 78) || Arith.compareNum(20, rsiValue)) {
//            decimalsNum = 10 / lrate;
//        }
//        if (Arith.compareNum(rsiValue, 82) || Arith.compareNum(16, rsiValue)) {
//            decimalsNum = 15 / lrate;
//        }
//        if (Arith.compareNum(rsiValue, 84) || Arith.compareNum(15, rsiValue)) {
//            decimalsNum = 20 / lrate;
//        }
//        if (Arith.compareNum(rsiValue, 90) || Arith.compareNum(11, rsiValue)) {
//            decimalsNum = 25 / lrate;
//        }
//        return decimalsNum / 100;
//    }
//
//    /**
//     * 获取一列值
//     */
//    public double[] getArrColumn(JSONArray arr, String key,Boolean reverseFlag) {
//        if (arr.size() <= 0 ) {
//            return null;
//        }
//        if (reverseFlag) {
//            Collections.reverse(arr);
//        }
//        double[] out = new double[arr.size()];
//        for (int i = 0; i < arr.size(); i++) {
//            JSONObject jobj = arr.getJSONObject(i);
//            double value = jobj.getDoubleValue(key);
//            out[i] = value;
//        }
//        return out;
//    }
//    public Map<String, double[]> getKlineMapColumn(JSONArray klineArr,String[] keyArr,Boolean reverseFlag) {
//        Map<String, double[]> KlineMap = new HashMap<String, double[]>();
//        if (keyArr == null) {
//            keyArr = new String[]{"id", "open", "close", "low", "high", "amount", "vol", "count"};
//        }
//        for (String key : keyArr) {
//            KlineMap.put(key,getArrColumn(klineArr,key,reverseFlag));
//        }
//        return KlineMap;
//    }
//    /**
//     * 撤单
//     */
//    public void backoutOrder(String symbol) throws IOException, HttpException {
//        mailService.sendMail("撤所有挂单",symbol,"");
//        String dan = huobiEntity.futureContractCancelall(symbol);
//        //刷新仓位
//        getContractAccountPositionInfo(symbol);
//    }
//
//
//}