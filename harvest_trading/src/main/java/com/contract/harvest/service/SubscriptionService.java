package com.contract.harvest.service;

import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;

@Service
public class SubscriptionService {

    @Resource SuperTrendService superTrendService;
    /**
     * 订阅分发
     * @param message string
     */
    public void handleMessage(String message) throws Exception {
        //处理订单
        if (message.startsWith("hadleQueueOrder:")) {
            String[] strArr = message.split(":");
            System.out.println(Arrays.toString(strArr));
            superTrendService.hadleQueueOrder(strArr[1]);
        }
//        switch (message) {
//            case "ChaseStrategy-hadleQueueOrder":
//                chaseStrategy.hadleQueueOrder();
//                break;
//            default:
//                break;
//        }
    }
}
