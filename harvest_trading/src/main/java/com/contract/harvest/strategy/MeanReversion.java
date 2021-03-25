package com.contract.harvest.strategy;

import com.contract.harvest.tools.Arith;
import com.contract.harvest.tools.ValueAccessor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MeanReversion {
    //═════════ MRC Parameter ════════
    /**
     *
     */
    public enum filterType{
        SuperSmoother,
        EhlersEMA,
        Gaussian,
        Ehlers,
        Butterworth,
        BandStop,
        SMA,
        EMA,
        RMA,
    }

    public static void main(String[] args) {
        double[] x = {1};
        sakSmoothing(filterType.EhlersEMA,x,1);
//        System.out.println(filterType.EhlersEMA);
    }
    //Lookback Period
    private final int length = 200;
    //Inner Channel Size Multiplier
    private static final double innermult = 1.0;
    //Outer Channel Size Multiplier
    private static final double outermult = 2.415;
    //═════════ MTF Setting ════════
    //Enable Multiple TimeFrame Analysis
    private final Boolean enableMtf = true;
    //Multiple TimeFrame Type
    private final String mtfType = "Auto"; //"Auto", "Custom"
    private final String mtfLvl1 = "D";
    private final String mtfLvl2 = "W";

    private static final double pi = Math.asin(1) * 2;
    private static final double mult = pi * innermult;
    private static final double mult2 = pi * outermult;
    private static final double gradsize = 0.5;
    private static final double gradtransp = 0.5;

    //Ehler SwissArmyKnife Function
    public static double[] sakSmoothing(filterType _type, double[] _src, int _length) {
        double c0 = 1, c1 = 0, b0 = 1, b1 = 0, b2 = 0, a1 = 0, a2 = 0, alpha = 0, beta = 0, gamma = 0;
        double cycle = 2 * pi / _length;
        switch (_type) {
            case EhlersEMA:
                alpha = (Math.cos(cycle) + Math.sin(cycle) - 1) / Math.cos(cycle);
                b0 = alpha;
                a1 = 1 - alpha;
                break;
            case Gaussian:
                beta    = 2.415 * (1 - Math.cos(cycle));
                alpha   = -beta + Math.sqrt((beta * beta) + (2 * beta));
                c0      = alpha * alpha;
                a1      = 2 * (1 - alpha);
                a2      = -(1 - alpha) * (1 - alpha);
                break;
            case Butterworth:
                beta    = 2.415 * (1 - Math.cos(cycle));
                alpha   = -beta + Math.sqrt((beta * beta) + (2 * beta));
                c0      = alpha * alpha / 4;
                b1      = 2;
                b2      = 1;
                a1      = 2 * (1 - alpha);
                a2      = -(1 - alpha) * (1 - alpha);
                break;
            case BandStop:
                beta    = Math.cos(cycle);
                gamma   = 1 / Math.cos(cycle*2*0.1); // delta default to 0.1. Acceptable delta -- 0.05<d<0.5
                alpha   = gamma - Math.sqrt((gamma * gamma) - 1);
                c0      = (1 + alpha) / 2;
                b1      = -2 * beta;
                b2      = 1;
                a1      = beta * (1 + alpha);
                a2      = -alpha;
                break;
            case SMA:
                c1      = Arith.div(1,_length);
                b0      = Arith.div(1,_length);
                a1      = 1;
                break;
            case EMA:
                alpha   = Arith.div(2,_length + 1);
                b0      = alpha;
                a1      = 1 - alpha;
                break;
            case RMA:
                alpha   = Arith.div(1,_length);
                b0      = alpha;
                a1      = 1 - alpha;
                break;
            default:
                break;
        }
        double[] _Output = new double[_src.length];
        for (int i = _src.length; i > 0; i--) {
            double[] _flagSrc = Arrays.copyOfRange(_src,i-1,_src.length);
            int nowLength = _flagSrc.length > _length ? _length : 0;
            if (_flagSrc.length > 2) {
                _Output[i-1] = (c0 * ((b0 * _flagSrc[0]) + (b1 * _flagSrc[1]) + (b2 * _flagSrc[2]))) + (a1 * _Output[i]) + (a2 * _Output[i+1]) - (c1 * _flagSrc[nowLength]);
            } else {
                _Output[i-1] = (c0 * ((b0 * _flagSrc[0]))) - (c1 * _flagSrc[nowLength]);
            }
        }
        return _Output;
    }

    /**
     * 平滑函数
     * @param _src 数据
     */
    public static double[] superSmoother(double[] _src, int _length) {
        double s_a1 = Math.exp(-Math.sqrt(2) * pi / _length);
        double s_b1    = 2 * s_a1 * Math.cos(Math.sqrt(2) * pi / _length);
        double s_c3    = -Math.pow(s_a1, 2);
        double s_c1    = 1 - s_b1 - s_c3;
        double[] ss = new double[_src.length];
        for (int i = _src.length; i > 0; i--) {
            double[] _flagSrc = Arrays.copyOfRange(_src,i-1,_src.length);
            if (_flagSrc.length > 2) {
                double flagNum = ss[i] == 0 ? _flagSrc[1] : ss[i];
                double flagNum2 = ss[i+1] == 0 ? _flagSrc[2] : ss[i+1];
                ss[i-1] = s_c1 * _flagSrc[0] + s_b1 * flagNum + s_c3 * flagNum2;
            }
        }
        return ss;
    }
    /**
     * 时间周期转化为浮动分钟数
     * @param timeCycle 时间周期
     */
    public static int candlestickTimeConvertMinutes(String timeCycle) {
        int minCount = 0;
        if (timeCycle.endsWith("min")) {
            minCount = Integer.parseInt(timeCycle.replace("min",""));
        }
        if (timeCycle.endsWith("hour")) {
            minCount = 60 * Integer.parseInt(timeCycle.replace("hour",""));
        }
        if (timeCycle.endsWith("day")) {
            minCount = 60 * 24 * Integer.parseInt(timeCycle.replace("day",""));
        }
        if (timeCycle.endsWith("week")) {
            minCount = 60 * 24 * 7 * Integer.parseInt(timeCycle.replace("week",""));
        }
        if (timeCycle.endsWith("mon")) {
            minCount = 60 * 24 * 7 * 30 * Integer.parseInt(timeCycle.replace("mon",""));
        }
        return minCount;
    }
    /**
     * 均值反转
     * @param _src hlc3
     * @param tr 真实波动幅度
     */
    public static void meanReversionChannel(double[] _src, double[] tr, filterType _type, int _length) {
        int v_condition = 0;
        double[] v_meanline = _src;
        double[] v_meanrange = superSmoother(tr, _length);
        //-- Get Line value
        if (_type == filterType.SuperSmoother) {
            v_meanline = superSmoother(_src,_length);
        } else {
            v_meanline = sakSmoothing(_type,_src,_length);
        }
        Map<String,double[]> lineMapArr = new HashMap<>();
        double[] v_upband1 = new double[v_meanline.length],
                 v_loband1 = new double[v_meanline.length],
                 v_upband2 = new double[v_meanline.length],
                 v_loband2 = new double[v_meanline.length];
        for (int i = v_meanline.length - 1; i >= 0; i--) {
            v_upband1[i] = v_meanline[i] + (v_meanrange[i] * mult);
            v_loband1[i] = v_meanline[i] - (v_meanrange[i] * mult);
            v_upband2[i] = v_meanline[i] + (v_meanrange[i] * mult2);
            v_loband2[i] = v_meanline[i] - (v_meanrange[i] * mult2);
            lineMapArr.put("v_upband1",v_upband1);
            lineMapArr.put("v_loband1",v_loband1);
            lineMapArr.put("v_upband2",v_upband2);
            lineMapArr.put("v_loband2",v_loband2);
        }
        System.out.println(Arrays.toString(lineMapArr.get("v_upband2")));
        System.out.println(Arrays.toString(lineMapArr.get("v_loband2")));

    }
}
