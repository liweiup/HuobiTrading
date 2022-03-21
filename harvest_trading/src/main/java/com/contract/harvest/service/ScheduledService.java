package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.contract.harvest.common.Depth;
import com.contract.harvest.common.OpenInfo;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.common.Topic;
import com.huobi.api.exception.ApiException;
import com.huobi.api.response.account.ContractPositionInfoResponse;
import com.huobiswap.api.response.account.SwapPositionInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * @author liwei
 */
@Slf4j
@Service
public class ScheduledService {

    @Resource
    private RedisService redisService;
    @Resource
    private CacheService cacheService;
    @Resource
    private DataService dataService;
    @Resource
    private TaskService taskService;
    @Resource
    private DeliveryDataService deliveryDataService;
    @Resource
    private SwapDataService swapDataService;

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
    /**
     * 设置开仓参数
     */
    @PostConstruct
    public void setSymbolOpenInfo() {
        HashMap<String,OpenInfo> openInfoList = new HashMap<String,OpenInfo>(){{
            put("BSV_NW",new OpenInfo("BSV_NW",0.06,0.03,7,14,1));
            put("BSV-USDT",new OpenInfo("BSV-USDT",0.06,0.03,6,14,1));
            put("BCH-USDT",new OpenInfo("BCH-USDT",0.06,0.03,6,14,1));
            put("DOGE-USDT",new OpenInfo("DOGE-USDT",0,0,6,14,3));
        }};
        for (String key : openInfoList.keySet()) {
            if (!redisService.hashExists(CacheService.HUOBI_OPEN_INFO,key)) {
                redisService.hashSet(CacheService.HUOBI_OPEN_INFO,key, JSON.toJSONString(openInfoList.get(key)));
            }
        }
    }
    @Scheduled(cron = "0/2 * * * * ?")  //每2秒执行一次
    public void invokeBi() {
        //交割合约
        for (String symbol : getSymbol(0)) {
            Map<String,String> params = new HashMap<>();
            params.put("symbol",symbol);
            params.put("type","delivery");
            taskService.execInvokeBi(params);
        }
        //永续合约
        for (String symbol : getSymbol(1)) {
            Map<String,String> params = new HashMap<>();
            params.put("symbol",symbol + PubConst.SWAP_USDT);
            params.put("type","swap");
            taskService.execInvokeBi(params);
        }
    }
    /**
     * 存放最近的kline数据
     */
    @Scheduled(cron = "0 0/5 * * * ?")  //每5分钟执行一次
    public void indexCalculation() {
        try {
            dataService.saveIndexCalculation(1);
        } catch (ApiException e) {
            log.error("存放5分钟的kline数据"+e.getMessage());
        }
    }
    //刷新仓位
    @Scheduled(cron = "0 0/5 * * * ?")  //每8分钟执行一次
    public void refushPosition() {
        try {
            for (String symbol : getSymbol(0)) {
                deliveryDataService.setContractPositionInfo(symbol);
            }
            for (String symbol : getSymbol(1)) {
                swapDataService.setContractPositionInfo(symbol + PubConst.SWAP_USDT);
            }
        } catch (ApiException e) {
            log.error("刷新仓位"+e.getMessage());
        } catch (Exception e) {
            log.error("刷新仓位异常"+e.getMessage());
        }
    }
    //拆分订单 15分钟一次
    @Scheduled(cron = "0 0/10 * * * ?")  //每15分钟执行一次
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
            for (String symbol : getSymbol(1)) {
                //持仓量
                List<SwapPositionInfoResponse.DataBean> contractPositionInfo = swapDataService.getContractPositionInfo(symbol+ PubConst.SWAP_USDT);
                int takeVolume = contractPositionInfo != null && contractPositionInfo.size() > 0 ? contractPositionInfo.get(0).getVolume().intValue() : 0;
                //没有持仓的情况下再进行订单拆分
                if (takeVolume != 0) {
                    continue;
                }
                swapDataService.contractLossWinOrder(symbol + PubConst.SWAP_USDT, PubConst.UPSTRATGY.PLL);
            }
        } catch (ApiException e) {
            log.error("拆分订单"+e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("拆分订单异常"+e.getMessage());
        }
    }
//    @Scheduled(cron = "0/2 * * * * ?")  //每15分钟执行一次
    public void getContractVolume() throws InterruptedException {

        String depthSubKey = Topic.formatChannel(Topic.DEPTH_SUB,"BSV_NW",PubConst.DEPTH_SUB_INDEX);
        Depth.TickBean depth = dataService.getBidAskPrice(depthSubKey).getTick();
        System.out.println(depth);
//        redisService.hashGet(CacheService.HUOBI_SUB,"MARKET.BSV_NW.DEPTH.STEP6");
    }
}
