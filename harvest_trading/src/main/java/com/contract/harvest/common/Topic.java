package com.contract.harvest.common;

/**
 * 订阅主题, 火币网
 */
public final class Topic {
    //K线订阅
    public static String KLINE_SUB = "market.%s.kline.%s";
    //交易深度
    public static String MARKET_DEPTH_SUB = "market.%s.depth.step0";
    //交易行情
    public static String MARKET_TRADE_SUB = "market.%s.trade.detail";
    //行情
    public static String MARKET_DETAIL_SUB = "market.%s.detail";
    //K线交易周期
    public static String[] PERIOD = {"1min" , "5min", "15min", "30min", "60min", "4hour","1day", "1week", "1mon", "1year" };
    /*********************交割合约***********************************/
    //合约买一卖一行情数据
    public static String BBO_SUB = "market.%s.bbo";
    //market depth 数据
    public static String DEPTH_SUB = "market.%s.depth.step%s";
    //指数数据
    public static String INDEX_SUB = "market.%s.index.%s";
    //订阅 Market Depth增量推送数据
    public static String DEPET_HIGH_FREQ = "market.%s.depth.size_%s.high_freq";

    //拼接订阅主题
    public static String formatChannel(String topic, String channel ,Integer index) {
        if (topic.equalsIgnoreCase(KLINE_SUB) || topic.equalsIgnoreCase(INDEX_SUB) ) {
            return String.format(topic, channel, PERIOD[index]);
        }
        if (topic.equalsIgnoreCase(DEPTH_SUB) || topic.equalsIgnoreCase(DEPET_HIGH_FREQ)) {
            return String.format(topic, channel, index);
        }
        return String.format(topic, channel);
    }
}
