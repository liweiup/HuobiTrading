package com.contract.harvest.common;

import java.util.*;

public class PubConst {
    //k线默认{"1min" , "5min", "15min", "30min", "60min", "4hour","1day", "1week", "1mon", "1year" };
    public static final int TOPIC_INDEX = 1;
    public static final int TOPIC_FLAG_INDEX = 5;
    //时间周期
    public static final int[] DATE_INDEX = {1,5,15,30,60,240,1440};
    //获得150档深度数据，使用step0, step1, step2, step3, step4, step5, step14, step15 （step1至step15是进行了深度合并后的深度），使用step0时，不合并深度获取150档数据;获得20档深度数据，使用 step6, step7, step8, step9, step10, step11, step12, step13（step7至step13是进行了深度合并后的深度），使用step6时，不合并深度获取20档数据
    public static final int DEPTH_SUB_INDEX = 6;
    //20:表示20档不合并的深度，150:表示150档不合并的深度
    public static final int DEPET_HIGH_FREQ_INDEX = 150;
    //默认的交易周期
    public static final String DEFAULT_CS = "_NW";
    //获取的k线数量
    public static final int GET_KLINE_NUM = 800;
    //次季度
    public static final String NEXT_QUARTER_FLAG = "_NQ";
    //季度
    public static final String QUARTER_FLAG = "_CQ";
    //下周
    public static final String NW_FLAG = "_NW";
    //本周
    public static final String CW_FLAG = "_CW";
    //杠杆倍数
    public static final int LEVER_RATE = 3;
    //开仓张数
    public static final int VOLUME = 5;
    //订单获取成交信息次数
    public static final int ORDER_TRY_NUM = 6;
    /*订单报价类型 "limit":限价 "opponent":对手价 "post_only":只做maker单,
    #post only下单只受用户持仓数量限制,optimal_5：最优5档、optimal_10：最优10档、optimal_20：最优20档，ioc:IOC订单，
    #fok：FOK订单, "opponent_ioc"： 对手价-IOC下单，"optimal_5_ioc"：最优5档-IOC下单，"optimal_10_ioc"：最优10档-IOC下单，
    #"optimal_20_ioc"：最优20档-IOC下单,"opponent_fok"： 对手价-FOK下单，"optimal_5_fok"：最优5档-FOK下单，"optimal_10_fok"：
    #最优10档-FOK下单，"optimal_20_fok"：最优20档-FOK下单*/
    public static final String ORDER_PRICE_TYPE = "post_only";
    //止盈｜损下单类型
    public static final String ORDER_STOPLIMIT_TYPE = "optimal_10";
    //币本位合约类型
    public static final Map<String,String> CONTRACT_TYPE = new HashMap<String,String>(){
        {
            put(NEXT_QUARTER_FLAG,"next_quarter");
            put(QUARTER_FLAG,"quarter");
            put(NW_FLAG,"next_week");
            put(CW_FLAG,"this_week");
        }
    };
    //最大倍投次数
    public static final int MAX_OPEN_NUM = 5;
    //usdt永续合约标识
    public static final String SWAP_USDT = "-USDT";
    //止盈止损的策略
    public enum UPSTRATGY {
        /**
         * 斐波那契数列输一进一，赢一退二
         * 0,1,1,2,3,5,8,13,21,34,55,89,144,233,377,610,987
         */
        FBNQ,
        /**
         * 帕罗利进阶下注
         */
        PLL,
    };
    //帕罗利进阶下注终止阶次
    public static final int PLLNUM = 5;
    //止损点
    public static final double STOP_PERCENT = 0.06;
    //止盈点
    public static final double LIMIT_PERCENT = 0.04;
    //信号k线结束的前PRE_SECOND秒,后LATER_SECOND秒之内交易
    public static final int PRE_SECOND = 20;
    public static final int LATER_SECOND = 50;
    //信号k线结束的900秒之内，当前价格小于k线价格交易
    public static final int LAST_SECOND = 900;
    //时间暂停的缓存key
    public static final String TIME_FLAG = "timeFlag";
}
