package com.contract.harvest.service;

import com.contract.harvest.common.CacheKey;
import com.contract.harvest.socketService.IntertemporalDeal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class ScheduledService {

    @Resource
    private RedisService redisService;

    @Resource
    private IntertemporalDeal intertemporalDeal;

    //获取所有币名
    public Set<String> getSymbol() {
        return redisService.getSetMembers(CacheKey.SYMBLO_FLAG);
    }

    //获取永续合约所有币名
    public Set<String> getSwapSymbol() {
        return redisService.getSetMembers(CacheKey.SWAP_SYMBLO_FLAG);
    }

    @Scheduled(cron = "0 0/30 * * * ?")  //每30分钟检查一次订阅
    public void invokeChaseSubCash() {
        intertemporalDeal.cashClientAddSub();
    }
}
