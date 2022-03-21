package com.contract.harvest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class kt {

    static class NameTreadFactory implements ThreadFactory {

        private final AtomicInteger mThreadNum = new AtomicInteger(1);

        private int threadNum;
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "my-thread-" + threadNum++);
            System.out.println(t.getName() + " has been created");
            return t;
        }
    }

    public static void main(String[] args) {
        System.out.println("使用关键字synchronized");
//        SyncThread syncThread = new SyncThread();
//        Thread thread1 = new Thread(syncThread, "SyncThread1");
//        Thread thread2 = new Thread(syncThread, "SyncThread2");
//        Thread thread3 = new Thread(syncThread, "SyncThread3");
//        Thread thread4 = new Thread(syncThread, "SyncThread4");
//        thread1.start();
//        thread2.start();
//        thread3.start();
//        thread4.start();

//        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 20, 200, TimeUnit.MILLISECONDS,
//                new SynchronousQueue<Runnable>());

        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(2);
        ThreadFactory threadFactory = new ThreadTest.NameTreadFactory();
        RejectedExecutionHandler handler = new ThreadTest.MyIgnorePolicy();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 20, 200, TimeUnit.MILLISECONDS,
                workQueue, threadFactory, handler);
        int num = 1;
        while (num < 5) {
            SyncThread myTask = new SyncThread();
            executor.execute(myTask);
//            System.out.println("线程池中线程数目："+executor.getPoolSize()+"，队列中等待执行的任务数目："+
//                    executor.getQueue().size()+"，已执行玩别的任务数目："+executor.getCompletedTaskCount());
            num++;
        }
    }
}



class SyncThread implements Runnable {
    private static int count;
    public SyncThread() {
        count = 1;
    }
    public  void run() {
        synchronized (this){
            for (int i = 1; i <= 10; i++) {
                try {
                    System.out.println("线程名:"+Thread.currentThread().getName() + ":" + (count++));
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public int getCount() {
        return count;
    }
}