package com.contract.harvest.tools;

import java.util.HashMap;
import java.util.Map;

public class CodeConstant {
    public static final Map<String, String> messageMap = new HashMap<>();

    public static final String NO_CLEAN_SPACE = "1001";
    public static final String LOSS_NO_CAN_CLEAN_SPACE = "1002";
    public static final String NO_REDUCE_SPACE = "1003";
    public static final String LOSS_NO_CAN_REDUCE_SPACE = "1004";
    public static final String DEAL_ORDER_QUEUE_ERROR = "1005";

    public static final String CLEAN_SPACE_AN_ORDER = "2001";
    public static final String ADD_SPACE_AN_ORDER = "2002";
    public static final String REDUCE_SPACE_AN_ORDER = "2003";
    public static final String OPEN_SPACE_AN_ORDER = "2004";

    public static final String NONE_KLINE_DATA = "3001";
    public static final String KLINE_DATE_ERROR = "3002";
    public static final String NONE_CONTRACT = "3003";

    static {
        messageMap.put(CodeConstant.NO_CLEAN_SPACE,"没有可清仓位");
        messageMap.put(CodeConstant.LOSS_NO_CAN_CLEAN_SPACE,"亏损无法清仓");
        messageMap.put(CodeConstant.NO_REDUCE_SPACE,"没有可减仓位");
        messageMap.put(CodeConstant.LOSS_NO_CAN_REDUCE_SPACE,"亏损无法减仓");
        messageMap.put(CodeConstant.CLEAN_SPACE_AN_ORDER,"清仓下单");
        messageMap.put(CodeConstant.ADD_SPACE_AN_ORDER,"加仓下单");
        messageMap.put(CodeConstant.REDUCE_SPACE_AN_ORDER,"减仓下单");
        messageMap.put(CodeConstant.OPEN_SPACE_AN_ORDER,"开仓下单");
        messageMap.put(CodeConstant.DEAL_ORDER_QUEUE_ERROR,"订单成交队列数量错误");
        messageMap.put(CodeConstant.NONE_KLINE_DATA,"没有k线数据");
        messageMap.put(CodeConstant.KLINE_DATE_ERROR,"k线数据时间周期错误");
        messageMap.put(CodeConstant.NONE_CONTRACT,"获取合约信息出错");
    }

    public static String getMsg(String code) {
        return messageMap.get(code);
    }
}
