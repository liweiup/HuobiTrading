package com.contract.harvest.service;

import com.contract.harvest.entity.Candlestick;
import lombok.extern.slf4j.Slf4j;
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

    //开仓参数
    public static final String HUOBI_OPEN_INFO = "HB:OPEN_INFO";
    //kline数据
    public static final String HUOBI_KLINE = "HB:KLINE_DATA";
    //订阅数据
    public static final String HUOBI_SUB = "HB:SUB_DATA";
    //持仓信息
    public static final String SPACE_INFO = "HB:SPACE_INFO";

    @Resource
    private DataService dataService;

    //提醒
    @Cacheable(keyGenerator = "universalGenerator",value = "HBCACHE:ENTITYREMIND", cacheManager = "huobiEntityRedisCacheManager")
    public void inform(String flag,String str) {
        log.info(flag+str);
    }
    /**
     * 获取过往的k线
     */
    @Cacheable(key = "'kline'.concat(#symbol+#topicIndex)",value = "kline",cacheManager = "caffeineCacheManger",unless = "#result == null")
    public List<Candlestick.DataBean> getBeforeManyLine(String symbol, int topicIndex) {
        List<Candlestick.DataBean> kline = dataService.getBeforeManyLine(symbol,topicIndex);
        return kline;
    }
    /**
     * k线写入
     */
    @CachePut(key = "'kline'.concat(#symbol+#topicIndex)",value = "kline",cacheManager = "caffeineCacheManger",unless = "#result == null")
    public List<Candlestick.DataBean> setBeforeManyLine(String symbol, int topicIndex) {
        List<Candlestick.DataBean> kline = dataService.getBeforeManyLine(symbol,topicIndex);
        return kline;
    }

    @Cacheable(key = "#key",value = "HBCACHE:TIMEFLAG",cacheManager = "huobiEntityRedisCacheManager",unless = "#result.equals('0')")
    public String getTimeFlag(String key) {
        return "0";
    }
    //锁定
    @CachePut(key = "#key",value = "HBCACHE:TIMEFLAG", cacheManager = "huobiEntityRedisCacheManager")
    public String saveTimeFlag(String key) {
        return "1";
    }

}
