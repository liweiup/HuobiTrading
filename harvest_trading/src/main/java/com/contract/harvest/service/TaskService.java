package com.contract.harvest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Service

public class TaskService  {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    @Resource SuperTrendService superTrendService;

    @Qualifier("harvestExecutor")
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    @Async
    public void execInvokeBi(Map<String,String> params) {
        taskExecutor.execute(() -> {
            int quePoolNum = taskExecutor.getThreadPoolExecutor().getQueue().size();
            if (quePoolNum > 1) {
                logger.info("线程等待：线程池中线程数目："+taskExecutor.getThreadPoolExecutor().getPoolSize()+"，队列中等待执行的任务数目："+
                taskExecutor.getThreadPoolExecutor().getQueue().size()+"，已执行玩别的任务数目："+taskExecutor.getThreadPoolExecutor().getCompletedTaskCount());
                return;
            }
            synchronized(getBuildLock(params.get("symbol"))) {
                try {
                    superTrendService.trading(params.get("symbol"));
                } catch (InterruptedException e) {
                    logger.error("线程异常"+e.getMessage());
                } catch (Exception e) {
                    logger.error("线程外异常"+e.getMessage());
                }
            }
        });
    }
    /**
     * 获取锁
     */
    private String getBuildLock(String lockStr) {
        lockStr = lockStr.intern();
        return lockStr;
    }
}