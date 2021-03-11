package com.contract.harvest;

class MyTask implements Runnable {
    private int taskNum;

    public MyTask(int num) {
        this.taskNum = num;
    }

    @Override
    public void run() {
        System.out.println("正在执行task "+taskNum);
        try {
            if (taskNum % 5 == 0) {
                throw new Exception("111");
            }
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("task "+taskNum+"执行异常");
            e.printStackTrace();
        }
        System.out.println("task "+taskNum+"执行完毕");
    }
}