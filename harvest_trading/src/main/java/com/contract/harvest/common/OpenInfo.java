package com.contract.harvest.common;

import lombok.Data;

@Data
public class OpenInfo {
    //止损点
    public double stopPercent;
    //止盈点
    public double limitPercent;
    //atr muti
    public double atrMultiplier;
    //atr len
    public int atrLen;

//    public static void main(String[] args) {
//        OpenInfo oinfo = new OpenInfo(0.03,0.06,7,14);
//        System.out.println(JSON.toJSON(oinfo));
//    }
}

