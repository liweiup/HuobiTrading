package com.contract.harvest.common;


public class CacheKey {
    //交割合约的key前缀
    public static final String DELIVERY_CONTRACT = "HB:DELIVERY:CONTRACT:";
    //需要监控的币名称
    public static final String SYMBLO_FLAG = DELIVERY_CONTRACT+"SYMBOL";

    //永续合约的key前缀
    public static final String SWAP_CONTRACT = "HB:SWAP:CONTRACT:";
    //需要监控的币名称
    public static final String SWAP_SYMBLO_FLAG = SWAP_CONTRACT+"SYMBOL";

    //订阅数据
    public static final String HUOBI_SUB = "HB:SUB_DATA";
}
