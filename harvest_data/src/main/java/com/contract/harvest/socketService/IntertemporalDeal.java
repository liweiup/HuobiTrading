package com.contract.harvest.socketService;

import com.contract.harvest.common.CacheKey;
import com.contract.harvest.common.HuoBiProWebSocketClient;
import com.contract.harvest.common.OpenInfo;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.service.DataService;
import com.contract.harvest.service.RedisService;
import com.contract.harvest.service.ScheduledService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;
@Slf4j
@PropertySource(value = {"classpath:exchange.properties"})
@Service
public class IntertemporalDeal {

    @Resource
    private HuoBiProWebSocketService huoBiProWebSocketService;
    @Resource
    private ScheduledService scheduledService;
    @Resource
    private DataService dataService;


    @Value("${huobi.sk_url_ws}")
    private String skUrlWs;

    @Value("${huobi.sk_url_notification}")
    private String skUrlNotification;

    @Value("${huobi.sk_url_index}")
    private String skUrlIndex;

    @Value("${huobi.swap_url_ws}")
    private String swapUrlWs;

    //交割合约
    private HuoBiProWebSocketClient cashClient;
    //永续合约
    private HuoBiProWebSocketClient swapClient;


    private static final Set<String> SYMBOL_SET_CHANNEL = new HashSet<>();
    private static final Set<String> SWAP_SYMBOL_SET_CHANNEL = new HashSet<>();

    @PostConstruct
    public void invokeChaseSubCash() {
        cashClient = new HuoBiProWebSocketClient(huoBiProWebSocketService, skUrlWs);
        swapClient = new HuoBiProWebSocketClient(huoBiProWebSocketService, swapUrlWs);
        cashClient.start();
        swapClient.start();
        cashClientAddSub();
        swapClientAddSub();
    }

    public void cashClientAddSub() {
        Set<String> symbolList = scheduledService.getSymbol();
        for (String symbol : symbolList) {
            OpenInfo openInfo = dataService.getOpenInfo(symbol + PubConst.DEFAULT_CS);
            String klineSub = Topic.formatChannel(Topic.KLINE_SUB,symbol + PubConst.DEFAULT_CS,openInfo.getTopicIndex());
            String depthSub = Topic.formatChannel(Topic.DEPTH_SUB,symbol + PubConst.DEFAULT_CS,PubConst.DEPTH_SUB_INDEX);
            if (!SYMBOL_SET_CHANNEL.contains(symbol)) {
                log.info("新增订阅"+symbol);
                //交割
                cashClient.addSub(klineSub);
                cashClient.addSub(depthSub);
            }
            SYMBOL_SET_CHANNEL.add(symbol);
        }
        //取消订阅
        for (String unSymbol : SYMBOL_SET_CHANNEL) {
            OpenInfo openInfo = dataService.getOpenInfo(unSymbol + PubConst.DEFAULT_CS);
            String klineSub = Topic.formatChannel(Topic.KLINE_SUB,unSymbol + PubConst.DEFAULT_CS,openInfo.getTopicIndex());
            String depthSub = Topic.formatChannel(Topic.DEPTH_SUB,unSymbol + PubConst.DEFAULT_CS,PubConst.DEPTH_SUB_INDEX);
            if (!symbolList.contains(unSymbol)) {
                log.info("取消订阅" + unSymbol);
                swapClient.unAddSub(klineSub);
                swapClient.unAddSub(depthSub);
            }
        }
    }
    public void swapClientAddSub() {
        Set<String> symbolList = scheduledService.getSwapSymbol();
        for (String symbol : symbolList) {
            OpenInfo openInfo = dataService.getOpenInfo(symbol + "-USDT");
            String klineSub = Topic.formatChannel(Topic.KLINE_SUB,symbol + "-USDT",openInfo.getTopicIndex());
            String depthSub = Topic.formatChannel(Topic.DEPTH_SUB,symbol + "-USDT",PubConst.DEPTH_SUB_INDEX);
            if (!SWAP_SYMBOL_SET_CHANNEL.contains(symbol)) {
                log.info("新增订阅"+symbol);
                //永续
                swapClient.addSub(klineSub);
                swapClient.addSub(depthSub);
            }
            SWAP_SYMBOL_SET_CHANNEL.add(symbol);
        }
        //取消订阅
        for (String unSymbol : SWAP_SYMBOL_SET_CHANNEL) {
            OpenInfo openInfo = dataService.getOpenInfo(unSymbol + "-USDT");
            String klineSub = Topic.formatChannel(Topic.KLINE_SUB,unSymbol + "-USDT",openInfo.getTopicIndex());
            String depthSub = Topic.formatChannel(Topic.DEPTH_SUB,unSymbol + "-USDT",PubConst.DEPTH_SUB_INDEX);
            if (!symbolList.contains(unSymbol)) {
                log.info("取消订阅" + unSymbol);
                swapClient.unAddSub(klineSub);
                swapClient.unAddSub(depthSub);
            }
        }
    }
}
