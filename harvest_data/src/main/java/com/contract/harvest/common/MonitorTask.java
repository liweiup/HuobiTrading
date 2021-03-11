package com.contract.harvest.common;

import lombok.extern.slf4j.Slf4j;


/**
 * @Description: 防止 因为某种原因与火币网的websokcet连接被断开 所以需要有线程不定时查看查看，
 * 当5秒中没有获取数据火币网的消息,就认为与火币websocket失去连接 ，需要重新连接
 */
@Slf4j
public class MonitorTask implements Runnable {

    private long startTime = System.currentTimeMillis();

    private long checkTime = 5000L;

    private AbstractWebSocketClient client;

    public MonitorTask(AbstractWebSocketClient client) {
        this.client = client;
    }

    /**
     * 每次获取消息都会更新startTime
     */
    public void updateTime() {
        this.startTime = System.currentTimeMillis();
    }

//    @SneakyThrows
    @Override
    public void run() {
        if (System.currentTimeMillis() - startTime > checkTime) {
            client.reConnect();
        }
    }
}
