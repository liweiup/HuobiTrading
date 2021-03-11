package com.contract.harvest;

import java.lang.management.ThreadInfo;

public class StringThread implements Runnable {

    private static final String LOCK_PREFIX = "XXX---";

    private String ip;
    private ThreadInfo JdkUtil;

    public StringThread(String ip) {
        this.ip = ip;
    }

    @Override
    public void run() {

        String lock = buildLock();
        synchronized (lock) {
            System.out.println("[" + JdkUtil.getThreadName() + "]开始运行了");
            // 休眠5秒模拟脚本调用
//            JdkUtil.sleep(5000);
            System.out.println("[" + JdkUtil.getThreadName() + "]结束运行了");
        }
    }

    private String buildLock() {
        StringBuilder sb = new StringBuilder();
        sb.append(LOCK_PREFIX);
        sb.append(ip);

        String lock = sb.toString().intern();
        System.out.println("[" + JdkUtil.getThreadName() + "]构建了锁[" + lock + "]");

        return lock;
    }

}