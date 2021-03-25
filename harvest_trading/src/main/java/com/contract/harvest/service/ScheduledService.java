package com.contract.harvest.service;

import com.contract.harvest.common.PubConst;
import com.contract.harvest.common.Topic;
import com.contract.harvest.entity.HuobiEntity;
import com.contract.harvest.entity.HuobiSwapEntity;
import com.huobi.api.exception.ApiException;
import com.huobi.api.response.account.ContractPositionInfoResponse;
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
    private HuobiSwapEntity huobiSwapEntity;
    @Resource
    private TaskService taskService;
    @Resource
    private DeliveryDataService deliveryDataService;

    @Qualifier("harvestExecutor")
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    /**
     * 获取所有币名 默认获取交割合约
     */
    public Set<String> getSymbol(int type) {
        String symbolKey = type == 1 ? CacheService.SWAP_SYMBLO_FLAG : CacheService.SYMBLO_FLAG;
        return redisService.getSetMembers(symbolKey);
    }

    @Scheduled(cron = "0/2 * * * * ?")  //每2秒执行一次
    public void invokeBi() throws Exception {
        for (String symbol : getSymbol(0)) {
            Map<String,String> params = new HashMap<>();
            params.put("symbol",symbol);
            taskService.execInvokeBi(params);
        }
    }

    /**
     * 存放最近的kline数据
     */
    @Scheduled(cron = "0 0/2 * * * ?")  //每4分钟执行一次
    public void indexCalculation() {
        try {
            //交割合约
            for (String symbol : getSymbol(0)) {
                String symbolFlag = symbol + PubConst.DEFAULT_CS;
                String strData = huobiEntity.getMarketHistoryKline(symbolFlag,Topic.PERIOD[PubConst.TOPIC_INDEX],PubConst.GET_KLINE_NUM);
                redisService.hashSet(CacheService.HUOBI_KLINE,symbolFlag + Topic.PERIOD[PubConst.TOPIC_INDEX],strData);
            }
            //永续合约
            for (String symbol : getSymbol(1)) {
                String swapStrData = huobiSwapEntity.getSwapMarketHistoryKline(symbol+PubConst.SWAP_USDT,Topic.PERIOD[PubConst.TOPIC_INDEX],PubConst.GET_KLINE_NUM);
                redisService.hashSet(CacheService.HUOBI_KLINE,symbol + PubConst.SWAP_USDT + Topic.PERIOD[PubConst.TOPIC_INDEX],swapStrData);
            }
        } catch (ApiException e) {
            log.error("存放最近的kline数据"+e.getMessage());
        }
    }
    //刷新仓位
    @Scheduled(cron = "0 0/10 * * * ?")  //每10分钟执行一次
    public void refushPosition() {
        try {
            for (String symbol : getSymbol(0)) {
                deliveryDataService.setContractPositionInfo(symbol);
            }
        } catch (ApiException e) {
            log.error("刷新仓位"+e.getMessage());
        }
    }

    //拆分订单 15分钟一次
    @Scheduled(cron = "0 0/15 * * * ?")  //每15分钟执行一次
    public void contractLossWinOrder() {
        try {
            for (String symbol : getSymbol(0)) {
                //持仓量
                List<ContractPositionInfoResponse.DataBean> contractPositionInfo = deliveryDataService.getContractPositionInfo(symbol, PubConst.DEFAULT_CS,"");
                int takeVolume = contractPositionInfo != null && contractPositionInfo.size() > 0 ? contractPositionInfo.get(0).getVolume().intValue() : 0;
                //没有持仓的情况下再进行订单拆分
                if (takeVolume != 0) {
                    continue;
                }
                deliveryDataService.contractLossWinOrder(symbol, PubConst.UPSTRATGY.PLL);
            }
        } catch (ApiException e) {
            log.error("拆分订单"+e.getMessage());
        }
    }
}
