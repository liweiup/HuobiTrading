package com.contract.harvest.service;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class SubscriptionService {

    @Resource SuperTrendService superTrendService;
    @Resource SwapSuperTrendService swapSuperTrendService;
    /**
     * 订阅分发
     * @param message string
     */
    public void handleMessage(String message) throws Exception {
        //处理订单
        if (message.startsWith("hadleQueueOrder:")) {
            String[] strArr = message.split(":");
            superTrendService.hadleQueueOrder(strArr[1]);
        }
        if (message.startsWith("swapHadleQueueOrder:")) {
            String[] strArr = message.split(":");
            swapSuperTrendService.hadleQueueOrder(strArr[1]);
        }
    }
}
