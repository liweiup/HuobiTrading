package com.contract.harvest.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OpenInfo {
    //币名
    public String symbol;
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
}

