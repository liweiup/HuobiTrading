package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.contract.harvest.common.Topic;
import com.contract.harvest.config.CaffeineConfig;
import com.contract.harvest.entity.Candlestick;
import com.contract.harvest.tools.CodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
@CacheConfig(cacheNames="HUOBI:CACHE")
public class CacheService {
    @Resource
    private MailService mailService;
    @Resource
    private RedisService redisService;
    //交割合约的key前缀
    public static final String DELIVERY_CONTRACT = "HB:DELIVERY:CONTRACT:";
    //需要监控的币名称
    public static final String SYMBLO_FLAG = DELIVERY_CONTRACT+"SYMBOL";
    //挂单队列
    public static final String WAIT_ORDER_QUEUE = DELIVERY_CONTRACT+"ORDER_WAIT:";
    //订单成交信息
    public static final String ORDER_INFO = DELIVERY_CONTRACT+"ORDER_CONTRACT_INFO:";
    //已经成交的订单(客户端id)
    public static final String ORDER_DEAL_CLIENTID = DELIVERY_CONTRACT + "ORDER_DEAL_CID:";
    //成交的订单(orderid)
    public static final String ORDER_DEAL_OID = DELIVERY_CONTRACT + "ORDER_DEAL_OID:";
    //亏损订单
    public static final String ORDER_LOSS = DELIVERY_CONTRACT + "ORDER_LOSS:";
    //盈利订单
    public static final String ORDER_WIN = DELIVERY_CONTRACT + "ORDER_WIN:";
    //开仓张数
    public static final String OPEN_VOLUME = DELIVERY_CONTRACT + "OPEN_VOLUME:";


    //永续合约的key前缀
    public static final String SWAP_CONTRACT = "HB:SWAP:CONTRACT:";
    //需要监控的币名称
    public static final String SWAP_SYMBLO_FLAG = SWAP_CONTRACT+"SYMBOL";
    //挂单队列
    public static final String SWAP_WAIT_ORDER_QUEUE = SWAP_CONTRACT+"ORDER_WAIT:";
    //订单成交信息
    public static final String SWAP_ORDER_INFO = SWAP_CONTRACT+"ORDER_CONTRACT_INFO:";
    //已经成交的订单(客户端id)
    public static final String SWAP_ORDER_DEAL_CLIENTID = SWAP_CONTRACT + "ORDER_DEAL_CID:";
    //成交的订单(orderid)
    public static final String SWAP_ORDER_DEAL_OID = SWAP_CONTRACT + "ORDER_DEAL_OID:";
    //亏损订单
    public static final String SWAP_ORDER_LOSS = SWAP_CONTRACT + "ORDER_LOSS:";
    //盈利订单
    public static final String SWAP_ORDER_WIN = SWAP_CONTRACT + "ORDER_WIN:";
    //开仓张数
    public static final String SWAP_OPEN_VOLUME = SWAP_CONTRACT + "OPEN_VOLUME:";



    //kline数据
    public static final String HUOBI_KLINE = "HB:KLINE_DATA";
    //订阅数据
    public static final String HUOBI_SUB = "HB:SUB_DATA";
    //持仓信息
    public static final String SPACE_INFO = "HB:SPACE_INFO";

    @Resource
    private DataService dataService;
    @Resource
    private CacheManager caffeineCacheManger;

    //提醒
    @Cacheable(keyGenerator = "universalGenerator",value = "HBCACHE:ENTITYREMIND", cacheManager = "huobiEntityRedisCacheManager")
    public void inform(String flag,String str) {
        log.info(flag+str);
    }
    /**
     * 获取过往的k线
     */
    @Cacheable(key = "'kline'.concat(#symbol+#topicIndex)",value = "kline",cacheManager = "caffeineCacheManger")
    public List<Candlestick.DataBean> getBeforeManyLine(String symbol, int topicIndex) {
        return dataService.getBeforeManyLine(symbol,topicIndex);
    }

    @Cacheable(key = "'info'.concat(#key)",value = "info",cacheManager = "caffeineCacheManger",unless = "#result == 0")
    public int getTimeFlag(String key) {
        return 0;
    }
    //锁定
    @CachePut(value = "info", key = "'info'.concat(#key)",cacheManager = "caffeineCacheManger")
    public int saveTimeFlag(String key) {
        return 1;
    }

}
