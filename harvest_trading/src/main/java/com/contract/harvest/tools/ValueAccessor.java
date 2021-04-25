package com.contract.harvest.tools;

import com.contract.harvest.entity.Candlestick;

import java.util.List;

public class ValueAccessor {
    // (最高价+最低价+收盘价) / 3
    public static double hlc3(Candlestick.DataBean candlestick) {
        double sum = Arith.add(candlestick.getHigh().doubleValue(),candlestick.getLow().doubleValue(),candlestick.getClose().doubleValue());
        return Arith.div(sum,3);
    }
    public static double[] hlc3(List<Candlestick.DataBean> candlestick) {
        double[] hlc3Arr = new double[candlestick.size()];
        int i = 0;
        for (Candlestick.DataBean tick : candlestick) {
            double hlc3 = hlc3(tick);
            hlc3Arr[i] = hlc3;
            i++;
        }
        return hlc3Arr;
    }
    // (最高价+最低价) / 2
    public static double hl2(Candlestick.DataBean candlestick) {
        double sum = Arith.add(candlestick.getHigh().doubleValue(),candlestick.getLow().doubleValue());
        return Arith.div(sum,2);
    }
    public static double[] hl2(List<Candlestick.DataBean> candlestick) {
        double[] hlc2Arr = new double[candlestick.size()];
        int i = 0;
        for (Candlestick.DataBean tick : candlestick) {
            double hl2 = hl2(tick);
            hlc2Arr[i] = hl2;
            i++;
        }
        return hlc2Arr;
    }

    //真实波动幅度，是max(high - low, abs(high - close[1]), abs(low - close[1]))
    public static double[] tr(List<Candlestick.DataBean> candlestick) {
        double[] tr = new double[candlestick.size()];
        int count = 0;
        for (int i = candlestick.size() - 1; i >= 0; i--) {
            if (count > 0) {
                Candlestick.DataBean tick = candlestick.get(i);
                Candlestick.DataBean prevtick = candlestick.get(i+1);
                double maxNum = Math.max(Arith.sub(tick.getHigh().doubleValue(),tick.getLow().doubleValue()),Math.abs(Arith.sub(tick.getHigh().doubleValue(),prevtick.getClose().doubleValue())));
                maxNum = Math.max(maxNum,Math.abs(Arith.sub(tick.getLow().doubleValue(),prevtick.getClose().doubleValue())));
                tr[i] = maxNum;
            }
            count ++;
        }
        return tr;
    }

    /**
     * 偏移到最高K线
     */
    public int highestBars(List<Candlestick.DataBean> candlestick, int len)
    {
        double high = 1;
        int highIndex = 0;
        int startindex = candlestick.size() - len + 1;
        int endIndex = candlestick.size();
        for (int i = startindex; i < endIndex; i++)
        {
//            double value = values[i];
            Candlestick.DataBean value = candlestick.get(i);
//            if (value > high)
//            {
//                highIndex = i;
//                high = value;
//            }
        }
        return highIndex;
    }
}