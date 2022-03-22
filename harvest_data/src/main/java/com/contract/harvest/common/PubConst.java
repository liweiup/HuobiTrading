package com.contract.harvest.common;

public class PubConst {
    //k线默认{"1min" , "5min", "15min", "30min", "60min", "4hour","1day", "1week", "1mon", "1year" };
    public static final int TOPIC_INDEX = 1;
    //时间周期
    public static final int[] DATE_INDEX = {1,5,15,30,60,240,1440};
    //获得150档深度数据，使用step0, step1, step2, step3, step4, step5, step14, step15 （step1至step15是进行了深度合并后的深度），使用step0时，不合并深度获取150档数据;获得20档深度数据，使用 step6, step7, step8, step9, step10, step11, step12, step13（step7至step13是进行了深度合并后的深度），使用step6时，不合并深度获取20档数据
    public static final int DEPTH_SUB_INDEX = 6;
    //20:表示20档不合并的深度，150:表示150档不合并的深度
    public static final int DEPET_HIGH_FREQ_INDEX = 150;
    //默认的交易周期
    public static final String DEFAULT_CS = "_CQ";
    //获取的k线数量
    public static final String GET_KLINE_NUM = "800";
    //标识
    public static final String SWAP_USDT = "-USDT";
}
