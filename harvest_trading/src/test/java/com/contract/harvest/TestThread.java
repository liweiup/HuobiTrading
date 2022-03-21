package com.contract.harvest;

import java.lang.management.ThreadInfo;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestThread {
    static final Object Lock = new Object();
    public static void main(String[] args){
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 20, 200, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(5));
        int num = 1;
        while (num < 100) {
//            StringThread stringThread = new StringThread("123:29"+num);
            MyTask myTask = new MyTask(num);
            synchronized (Lock) {
                executor.execute(myTask);
            }
            System.out.println("线程池中线程数目："+executor.getPoolSize()+"，队列中等待执行的任务数目："+
                    executor.getQueue().size()+"，已执行玩别的任务数目："+executor.getCompletedTaskCount());
            num++;
        }
//        executor.shutdown();
    }
    private /*volatile*/ int count = 100;
}
