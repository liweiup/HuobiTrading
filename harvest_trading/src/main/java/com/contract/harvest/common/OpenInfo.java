package com.contract.harvest.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OpenInfo {
    //止损点
    public double stopPercent;
    //止盈点
    public double limitPercent;
    //atr muti
    public double atrMultiplier;
    //atr len
    public int atrLen;
    //k线周期
    public int topicIndex;

    public static void main(String[] args) {
        OpenInfo bsvDeliOInfo = new OpenInfo(0.03,0.06,7,14,1);
        OpenInfo bsvSwapOInfo = new OpenInfo(0.03,0.06,6,14,1);
        OpenInfo bchSwapOInfo = new OpenInfo(0.03,0.06,6,14,1);
        OpenInfo dogeSwapOInfo = new OpenInfo(0.03,0.06,6,14,3);
    }
}

