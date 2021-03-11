package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.common.Topic;
import com.contract.harvest.entity.HuobiEntity;
import com.contract.harvest.tools.Arith;
import com.huobi.api.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class ScheduledService {

    @Resource
    private RedisService redisService;
    @Resource
    private HuobiEntity huobiEntity;
    @Resource
    private TaskService taskService;
    @Resource
    private DeliveryDataService deliveryDataService;

    @Qualifier("harvestExecutor")
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    //获取所有币名
    public Set<String> getSymbol() {
        return redisService.getSetMembers(CacheService.SYMBLO_FLAG);
    }

    @Scheduled(cron = "*/2 * * * * ?")  //每2秒执行一次
    public void invokeBi() throws Exception {
        for (String symbol : getSymbol()) {
            Thread.sleep(1000);
            Map<String,String> params = new HashMap<>();
            params.put("symbol",symbol);
            taskService.execInvokeBi(params);
        }
    }

    /**
     * 存放最近的kline数据
     */
    @Scheduled(cron = "* 0/2 * * * ?")  //每4分钟执行一次
    public void indexCalculation() {
        try {
            for (String symbol : getSymbol()) {
                String symbolFlag = symbol + PubConst.DEFAULT_CS;
                String strData = huobiEntity.getMarketHistoryKline(symbolFlag,Topic.PERIOD[PubConst.TOPIC_INDEX],PubConst.GET_KLINE_NUM);
                redisService.hashSet(CacheService.HUOBI_KLINE,symbolFlag + Topic.PERIOD[PubConst.TOPIC_INDEX],strData);
    //            String swapStrData = swapEntity.getMarketHistoryKline(symbol+PubConst.SWAP_USDT,Topic.PERIOD[PubConst.TOPIC_INDEX],PubConst.GET_KLINE_NUM);
    //            redisService.hashSet(CacheKey.HUOBI_KLINE,symbol + PubConst.SWAP_USDT + Topic.PERIOD[PubConst.TOPIC_INDEX],swapStrData);
            }
        } catch (ApiException e) {
            log.error(e.getMessage());
        }
    }
    //刷新仓位
    @Scheduled(cron = "* */10 * * * ?")  //每10分钟执行一次
    public void refushPosition() {
        try {
            for (String symbol : getSymbol()) {
                deliveryDataService.setContractPositionInfo(symbol);
            }
        } catch (ApiException e) {
            log.error(e.getMessage());
        }
    }
}
