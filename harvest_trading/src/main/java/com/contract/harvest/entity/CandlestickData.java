package com.contract.harvest.entity;

import com.contract.harvest.tools.IndexCalculation;
import com.contract.harvest.tools.ValueAccessor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CandlestickData {

    public final List<Candlestick.DataBean> candlestick;

    public double[] hl2;
    public double[] open;
    public double[] high;
    public double[] low;
    public double[] close;
    public Long[] id;

    public CandlestickData(List<Candlestick.DataBean> candlestick) {
        this.candlestick = candlestick;
        this.hl2 = ValueAccessor.hl2(candlestick);
        this.open = getCandlestickListColumn(candlestick.stream().map(Candlestick.DataBean::getOpen).collect(Collectors.toList()));
        this.high = getCandlestickListColumn(candlestick.stream().map(Candlestick.DataBean::getHigh).collect(Collectors.toList()));
        this.low = getCandlestickListColumn(candlestick.stream().map(Candlestick.DataBean::getLow).collect(Collectors.toList()));
        this.close = getCandlestickListColumn(candlestick.stream().map(Candlestick.DataBean::getClose).collect(Collectors.toList()));
        this.id = candlestick.stream().map(Candlestick.DataBean::getId).toArray(Long[]::new);
    }
    /**
     * 获取double数组
     */
    public double[] getCandlestickListColumn(List<BigDecimal> numList) {
        double[] arr = new double[numList.size()];
        for (int i = 0; i < numList.size(); i++) {
            arr[i] = numList.get(i).doubleValue();
        }
        return arr;
    }

}
