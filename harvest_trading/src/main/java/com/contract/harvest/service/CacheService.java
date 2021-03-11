package com.contract.harvest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
@CacheConfig(cacheNames="HUOBI:CACHE")
public class CacheService {
    @Resource
    private MailService mailService;
    //交割合约的key前缀
    public static final String DELIVERY_CONTRACT = "HB:DELIVERY:CONTRACT:";
    //需要监控的币名称
    public static final String SYMBLO_FLAG = DELIVERY_CONTRACT+"SYMBOL";
    //挂单队列
    public static final String WAIT_ORDER_QUEUE = DELIVERY_CONTRACT+"ORDER_WAIT:";
    //订单成交信息
    public static final String ORDER_INFO = DELIVERY_CONTRACT+"ORDER_CONTRACT_INFO:";
    //已经成交的订单队列key
    public static final String ORDER_DEAL_CLIENTID = DELIVERY_CONTRACT + "ORDER_DEAL_CID:";
    //持仓信息
    public static final String SPACE_INFO = DELIVERY_CONTRACT + "SPACE_INFO";
    //kline数据
    public static final String HUOBI_KLINE = DELIVERY_CONTRACT + "KLINE_DATA";
    //订阅数据
    public static final String HUOBI_SUB = DELIVERY_CONTRACT + "SUB_DATA";


    //永续合约的key前缀
    public static final String SWAP_CONTRACT = "HB:SWAP:CONTRACT:";
    //需要监控的币名称
    public static final String SWAP_SYMBLO_FLAG = SWAP_CONTRACT+"SYMBOL";


    //获取合约信息
    @Cacheable(keyGenerator = "universalGenerator",cacheManager = "huobiEntityHisbasisRedisCacheManager")
    public void inform(String str) {
        log.info("不做处理,"+str);
    }

    @Cacheable(keyGenerator = "universalGenerator",cacheManager = "huobiOrderHandleRedisCacheManager")
    public void remindOrderHandle(String str) {
        log.info("不做处理,"+str);
    }

}
