package com.contract.harvest;

import lombok.SneakyThrows;

class MyTask implements Runnable {
    private static int taskNum;
    public MyTask(int num) {
        System.out.println(num);
       taskNum = num;
    }

    @Override
    public void run() {
            System.out.println("正在执行task " + Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("task "+Thread.currentThread().getName() +taskNum+"执行异常");
                e.printStackTrace();
            }
                System.out.println("task "+Thread.currentThread().getName()+taskNum+"执行完毕");
    }
//    public synchronized void run() {
//        System.out.println("正在执行task " + Thread.currentThread().getName() + taskNum);
//        try {
//
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            System.out.println("task "+Thread.currentThread().getName() +taskNum+"执行异常");
//            e.printStackTrace();
//        }
//        System.out.println("task "+Thread.currentThread().getName()+taskNum+"执行完毕");
//    }
}