package com.contract.harvest;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class ThreadTest {

    public static void main(String[] args) throws InterruptedException, IOException, IOException {
        int corePoolSize = 2;
        int maximumPoolSize = 4;
        long keepAliveTime = 10;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(2);
        ThreadFactory threadFactory = new NameTreadFactory();
        RejectedExecutionHandler handler = new MyIgnorePolicy();
//        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit,
//                workQueue, threadFactory, handler);
//
//        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 20, 200, TimeUnit.MILLISECONDS,
//                new SynchronousQueue<Runnable>());
        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 20, 200, unit,
                workQueue, threadFactory, handler);
        executor.prestartAllCoreThreads(); // 预启动所有核心线程
        for (int i = 1; i <= 10; i++) {
            MyTask task = new MyTask(String.valueOf(i));
            executor.execute(task);
        }
        System.in.read(); //阻塞主线程
    }

    static class NameTreadFactory implements ThreadFactory {

        private AtomicInteger mThreadNum = new AtomicInteger(1);
//        private int mThreadNum;
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "my-thread-" + mThreadNum.getAndIncrement());
//            Thread t = new Thread(r, "my-thread-" + mThreadNum++);
            System.out.println(t.getName() + " has been created");
            return t;
        }
    }

    public static class MyIgnorePolicy implements RejectedExecutionHandler {

        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            doLog(r, e);
        }

        private void doLog(Runnable r, ThreadPoolExecutor e) {
            // 可做日志记录等
            System.err.println( r.toString() + " rejected");
//          System.out.println("completedTaskCount: " + e.getCompletedTaskCount());
        }
    }

    static class MyTask implements Runnable {
        private String name;

        public MyTask(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            synchronized (new Object()) {
                try {
                    System.out.println(this.toString() + " is running!");
                    Thread.sleep(3000); //让任务执行慢点
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(this.toString() + " is end!");
            }
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "MyTask [name=" + name + "]";
        }
    }
}

//1，由于线程预启动，首先创建了1，2号线程，然后task1，task2被执行；
//2，但任务提交没有结束，此时任务task3，task6到达发现核心线程已经满了，进入等待队列；
//3，等待队列满后创建任务线程3，4执行任务task3，task6，同时task4，task5进入队列；
//4，此时创建线程数（4）等于最大线程数，且队列已满，所以7，8，9，10任务被拒绝；
//5，任务执行完毕后回头来执行task4，task5，队列清空。