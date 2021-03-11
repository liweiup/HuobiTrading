package com.contract.harvest.socketService;

import com.contract.harvest.common.HuoBiProWebSocketClient;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.service.ScheduledService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

@PropertySource(value = {"classpath:exchange.properties"})
@Service
public class IntertemporalDeal {

    @Resource
    private HuoBiProWebSocketService huoBiProWebSocketService;
    @Resource
    private ScheduledService scheduledService;


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
            if (!SYMBOL_SET_CHANNEL.contains(symbol)) {
                //交割
                cashClient.addSub(Topic.formatChannel(Topic.KLINE_SUB,symbol + PubConst.DEFAULT_CS,PubConst.TOPIC_INDEX));
                cashClient.addSub(Topic.formatChannel(Topic.DEPTH_SUB,symbol + PubConst.DEFAULT_CS,PubConst.DEPTH_SUB_INDEX));
            }
            SYMBOL_SET_CHANNEL.add(symbol);
        }
    }

    public void swapClientAddSub() {
        Set<String> symbolList = scheduledService.getSwapSymbol();
        for (String symbol : symbolList) {
            if (!SWAP_SYMBOL_SET_CHANNEL.contains(symbol)) {
                //永续
                swapClient.addSub(Topic.formatChannel(Topic.KLINE_SUB,symbol + "-USDT",PubConst.TOPIC_INDEX));
                swapClient.addSub(Topic.formatChannel(Topic.DEPTH_SUB,symbol + "-USDT",PubConst.DEPTH_SUB_INDEX));
            }
            SWAP_SYMBOL_SET_CHANNEL.add(symbol);
        }
    }
}
