package com.contract.harvest;


import com.contract.harvest.entity.HuobiEntity;
import com.contract.harvest.service.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest()
public class test {
    @Autowired
    private SuperTrendService superTrendService;


    @Autowired
    private ScheduledService scheduledService;
    @Autowired
    private HuobiEntity huobiEntity;
    @Autowired
    private RedisService redisService;
    @Resource
    private MailService mailService;
    @Resource
    private DeliveryDataService deliveryDataService;

    @Test
    public void TestLoadingCache() throws Exception{
//        Map<String,String> params = new HashMap<>();
//        chaseStrategy.dealIndexV2("BSV");
//        scheduledService.indexCalculation();
//        superTrendService.trading("BSV");
//        superTrendService.hadleQueueOrder("BSV");
//            deliveryDataService.getContractPositionInfo("BSV");
//        mailService.sendMail("成功下单-成交量","订单信息:","");
//        System.out.println(huobiEntity.getContractPositionInfo("BSV"));
//        scheduledService.contractLossWinOrder();
        redisService.hashScan("HB:DELIVERY:CONTRACT:ORDER_LOSS:BSV",1);
    }

    @Test
    public void testSocket() {
//        try {
//            huobiProMainService.start();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Test
    public void restTemplateGetTest(){
        RestTemplate restTemplate = new RestTemplate();
        String notice = restTemplate.getForObject("http://ym.api.com/Index/get_key"
                , String.class);
        System.out.println(notice);
    }

    @Autowired
    private TaskService taskService;


    @Qualifier("harvestExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Test
    @Async
    public void testVoid() throws Exception {
        Integer num = 1;
        while (true) {
            Map<String,String> p = new HashMap<>();
            taskExecutor.execute(new Runnable() {
                public void run() {
                    RestTemplate restTemplate = new RestTemplate();
                    MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                    map.add("token", "fcea956ef580d3cc9dc7624d3419e0cf");
                    map.add("word_text", "滚滚请问法");
                    map.add("word_type", "3");
                    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
                    ResponseEntity<String> response = restTemplate.postForEntity( "http://ym.api.com/v1/detectionText/add_word", request , String.class );
                    System.out.println(response.getBody());
                }
            });
//            taskExecutor.execute();
            System.out.println("线程池中线程数目："+taskExecutor.getThreadPoolExecutor().getPoolSize()+"，队列中等待执行的任务数目："+
                    taskExecutor.getThreadPoolExecutor().getQueue().size()+"，已执行玩别的任务数目："+taskExecutor.getThreadPoolExecutor().getCompletedTaskCount());
            num++;
        }
    }
    @Test
    public void testVoid1() throws Exception {
        System.out.println(fibLoop(22));
    }
    public static long fibLoop(int num) {
        if(num < 1 || num > 92)
            return 0;
        long a = 1;
        long b = 1;
        long temp;
        for(int i = 3; i <= num; i++) {
            temp = a;
            a = b;
            b += temp;
        }
        return b;
    }


}
