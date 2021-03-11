package com.contract.harvest.tools;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

import java.util.*;
import java.util.Arrays;

public class IndexCalculation {
    private static final int rsiPeriod = 14;
    private static final int optInFastPeriod = 12;  // 快速移动平均线期间
    private static final int optInSlowPeriod = 26;  // 慢速移动平均线期间
    private static final int optInSignalPeriod = 9;  // Signal期间
    private static final int optInTimePeriod = 30;  // 均线长度
    private static final int bollOptInTimePeriod = 30;  // 布林带长度
    private static final double optInNbDevUp = 2;  // 布林2倍标准差
    private static final double optInNbDevDn = 2;
    private static final double optInAcceleration = 0.02;//SAR抛物线指标
    private static final double optInMaximum = 2;
    private static final int adxInTimePeriod = 14;//ADX平均趋向指数
    private static final int mfiInTimePeriod = 14;//MFI资金流量指标
    private static final int atrInTimePeriod = 14;//atr指标
    private final static double AtrMultiplier = 8.5;//指标突破计算

    /**
     * 获取RSI
     *
     * @param closeArr 一个数组，包含的k线的收盘数据
     * @param period   长度
     */
    public static double[] RSI(double[] closeArr, int period) {
        if (period == 0) {
            period = rsiPeriod;
        }
        Core c = new Core();
        double[] out = new double[closeArr.length];
        int rsiLookback = c.rsiLookback(period);
        RetCode retCode = c.rsi(0, closeArr.length - 1, closeArr, period, new MInteger(), new MInteger(), out);
        if (retCode == RetCode.Success) {
            return Arrays.copyOf(out, out.length - rsiLookback);
        }
        return new double[]{};
    }

    /**
     * 获取MACD
     *
     * @param closeArr 一个数组，包含的k线的收盘数据
     */
    public static Map<String, double[]> MACD(double[] closeArr) {
        Core c = new Core();
        double[] outMACD = new double[closeArr.length];
        double[] outSignal = new double[closeArr.length];
        double[] outMACDHist = new double[closeArr.length];
        int macdLookback = c.macdLookback(optInFastPeriod, optInSlowPeriod, optInSignalPeriod);
        RetCode retCode = c.macd(0, closeArr.length - 1, closeArr, optInFastPeriod, optInSlowPeriod, optInSignalPeriod, new MInteger(), new MInteger(), outMACD, outSignal, outMACDHist);
        Map<String, double[]> macdRes = new HashMap<String, double[]>();
        if (retCode == RetCode.Success) {
            macdRes.put("outMACD", Arrays.copyOf(outMACD, outMACD.length - macdLookback));
            macdRes.put("outSignal", Arrays.copyOf(outSignal, outMACD.length - macdLookback));
            macdRes.put("outMACDHist", Arrays.copyOf(outMACDHist, outMACD.length - macdLookback));
            return macdRes;
        }
        return null;
    }

    /**
     * 均线
     *
     * @param closeArr 一个数组，包含的k线的收盘数据
     * @return
     */
    public static double[] MA(double[] closeArr, int period, String maType) {
        period = period == 0 ? optInTimePeriod : period;
        double[] outMa = new double[closeArr.length];
        Core c = new Core();
        int lookback = 0;
        RetCode retCode = null;
        //简单移动平均
        if ("sma".equals(maType)) {
            lookback = c.smaLookback(period);
            retCode = c.sma(0, closeArr.length - 1, closeArr, period, new MInteger(), new MInteger(), outMa);
        }
        //指数移动平均
        if ("ema".equals(maType)) {
            lookback = c.emaLookback(period);
            retCode = c.ema(0, closeArr.length - 1, closeArr, period, new MInteger(), new MInteger(), outMa);
        }
        //加权移动平均
        if ("wma".equals(maType)) {
            lookback = c.wmaLookback(period);
            retCode = c.wma(0, closeArr.length - 1, closeArr, period, new MInteger(), new MInteger(), outMa);
        }
        //双移动平均
        if ("dema".equals(maType)) {
            lookback = c.demaLookback(period);
            retCode = c.dema(0, closeArr.length - 1, closeArr, period, new MInteger(), new MInteger(), outMa);
        }
        if (retCode == RetCode.Success) {
            return Arrays.copyOf(outMa, outMa.length - lookback);
        }
        return new double[]{};
    }

    /**
     * 布林带
     * 布林带(Bollinger Band)，由压力线、支撑线价格平均线组成，一般情况价格线在压力线和支撑线组成的上下区间中游走，
     * 区间位置会随着价格的变化而自动调整。布林线的理论使用原则是：当股价穿越最外面的压力线（支撑线）时，表示卖点（买点）出现。
     * 当股价延着压力线（支撑线）上升（下降）运行，虽然股价并未穿越，但若回头突破第二条线即是卖点或买点。
     * 在实际应用中，布林线有其滞后性，相对于其他技术指标在判断行情反转时参考价值较低，但在判断盘整行情终结节点上成功率较高
     */
    public static Map<String, double[]> bbands(double[] closeArr, int period) {
        period = period == 0 ? bollOptInTimePeriod : period;
        Core c = new Core();
        double[] outRealUpperBand = new double[closeArr.length];
        double[] outRealMiddleBand = new double[closeArr.length];
        double[] outRealLowerBand = new double[closeArr.length];
        RetCode retCode = c.bbands(0, closeArr.length - 1, closeArr, period, optInNbDevUp, optInNbDevDn, MAType.Sma, new MInteger(), new MInteger(), outRealUpperBand, outRealMiddleBand, outRealLowerBand);
        int bbandsLookback = c.bbandsLookback(period, optInNbDevUp, optInNbDevDn, MAType.Sma);
        Map<String, double[]> bbandsRes = new HashMap<String, double[]>();
        if (retCode == RetCode.Success) {
            bbandsRes.put("outRealUpperBand", Arrays.copyOf(outRealUpperBand, outRealUpperBand.length - bbandsLookback));
            bbandsRes.put("outRealMiddleBand", Arrays.copyOf(outRealMiddleBand, outRealMiddleBand.length - bbandsLookback));
            bbandsRes.put("outRealLowerBand", Arrays.copyOf(outRealLowerBand, outRealLowerBand.length - bbandsLookback));
            return bbandsRes;
        }
        return null;
    }

    /**
     * SAR抛物线指标
     * 停损点转向，利用抛物线方式，随时调整停损点位置以观察买卖点。 由于停损点（又称转向点SAR）以弧形的方式移动，故称之为抛物线转向指标 。
     */
    public static double[] sar(double[] inHigh, double[] inLow, double[] idArr) {
        Core c = new Core();
        double[] outReal = new double[idArr.length];
        int sarLookback = c.sarLookback(optInAcceleration, optInMaximum);
        RetCode retCode = c.sar(0, idArr.length - 1, inHigh, inLow, optInAcceleration, optInMaximum, new MInteger(), new MInteger(), outReal);
        if (retCode == RetCode.Success) {
            return Arrays.copyOf(outReal, outReal.length - sarLookback);
        }
        return new double[]{};
    }

    /**
     * ADX平均趋向指数
     */
    public static double[] adx(double[] inHigh, double[] inLow, double[] inClose, double[] idArr, int period, String adxType) {
        period = period == 0 ? adxInTimePeriod : period;
        Core c = new Core();
        double[] outReal = new double[idArr.length];
        int lookback = 0;
        RetCode retCode = null;
        //ADX平均趋向指数
        if ("adx".equals(adxType)) {
            lookback = c.adxLookback(period);
            retCode = c.adx(0, idArr.length - 1, inHigh, inLow, inClose, period, new MInteger(), new MInteger(), outReal);
        }
        //ADXR平均趋向指数的趋向指数 判断ADX趋势
        if ("adxr".equals(adxType)) {
            lookback = c.adxrLookback(period);
            retCode = c.adxr(0, idArr.length - 1, inHigh, inLow, inClose, period, new MInteger(), new MInteger(), outReal);
        }
        if (retCode == RetCode.Success) {
            return Arrays.copyOf(outReal, outReal.length - lookback);
        }
        return new double[]{};
    }

    /**
     * MFI资金流量指标
     */
    public static double[] mfi(double[] inHigh, double[] inLow, double[] inClose, double[] inVolume, double[] idArr, int period) {
        period = period == 0 ? mfiInTimePeriod : period;
        Core c = new Core();
        double[] outReal = new double[idArr.length];
        int lookback = 0;
        RetCode retCode = null;
        lookback = c.mfiLookback(period);
        retCode = c.mfi(0, idArr.length - 1, inHigh, inLow, inClose, inVolume, period, new MInteger(), new MInteger(), outReal);
        if (retCode == RetCode.Success) {
            return Arrays.copyOf(outReal, outReal.length - lookback);
        }
        return new double[]{};
    }

    /**
     * OBV能量潮
     */
    public static double[] obv(double[] inClose, double[] inVolume, double[] idArr) {
        Core c = new Core();
        double[] outReal = new double[idArr.length];
        int lookback = 0;
        RetCode retCode = null;
        lookback = c.obvLookback();
        retCode = c.obv(0, idArr.length - 1, inClose, inVolume, new MInteger(), new MInteger(), outReal);
        if (retCode == RetCode.Success) {
            return Arrays.copyOf(outReal, outReal.length - lookback);
        }
        return new double[]{};
    }

    /**
     * Price Transform(价格变换)
     */
    public static double[] transForm(double[] inOpen, double[] inHigh, double[] inLow, double[] inClose, double[] idArr, String priceType) {
        Core c = new Core();
        double[] outReal = new double[idArr.length];
        int lookback = 0;
        RetCode retCode = null;
        //AVGPRICE平均价格函数
        if ("avgPrice".equals(priceType)) {
            lookback = c.avgPriceLookback();
            retCode = c.avgPrice(0, idArr.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //中位数价格
        if ("medPrice".equals(priceType)) {
            lookback = c.medPriceLookback();
            retCode = c.medPrice(0, idArr.length - 1, inHigh, inLow, new MInteger(), new MInteger(), outReal);
        }
        //TYPPRICE代表性价格
        if ("typPrice".equals(priceType)) {
            lookback = c.typPriceLookback();
            retCode = c.typPrice(0, idArr.length - 1, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //WCLPRICE加权收盘价
        if ("wclPrice".equals(priceType)) {
            lookback = c.wclPriceLookback();
            retCode = c.wclPrice(0, idArr.length - 1, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        if (retCode == RetCode.Success) {
            return Arrays.copyOf(outReal, outReal.length - lookback);
        }
        return new double[]{};
    }

    /**
     * Volatility Indicators(波动率指标)
     * 当前交易日最高价与最低价差值，前一交易日收盘价与当前交易日最高价间的差值，前一交易日收盘价与当前交易日最低价的差值，
     * 这三者中的最大值为真实波幅。即真实波动幅度 = max(最大值,昨日收盘价) − min(最小值,昨日收盘价)，
     * 平均真实波动幅度等于真实波动幅度的N日指数移动平均数。波动幅度可以显示出交易者的期望和热情。
     * 波动幅度的急剧增加表示交易者在当天可能准备持续买进或卖出股票，波动幅度的减少则表示交易者对股市没有太大的兴趣。
     * 波动率指标可用于衡量价格的波动情况，辅助判断趋势改变的可能性，市场的交易氛围，也可以利用波动性指标来帮助止损止盈。
     */
    public static double[] volatilityIndicators(double[] inOpen, double[] inHigh, double[] inLow, double[] inClose, Long[] idArr, int period, String atrType) {
        period = period == 0 ? atrInTimePeriod : period;
        Core c = new Core();
        double[] outReal = new double[idArr.length];
        int lookback = 0;
        RetCode retCode = null;
        //ATR真实波动幅度均值
        if ("atr".equals(atrType)) {
            lookback = c.atrLookback(period);
            retCode = c.atr(0, idArr.length - 1, inHigh, inLow, inClose, period, new MInteger(), new MInteger(), outReal);
        }
        //NATR归一化波动幅度均值
        if ("natr".equals(atrType)) {
            lookback = c.natrLookback(period);
            retCode = c.natr(0, idArr.length - 1, inHigh, inLow, inClose, period, new MInteger(), new MInteger(), outReal);
        }
        if (retCode == RetCode.Success) {
            return Arrays.copyOf(outReal, outReal.length - lookback);
        }
        return new double[]{};
    }

    /**
     * Pattern Recognition(模式识别)
     */
    public static Map<String,ArrayList<Long>> patternRecognition(double[] inOpen, double[] inHigh, double[] inLow, double[] inClose, double[] idArr, String patternType) {
        Core c = new Core();
        int[] outReal = new int[idArr.length];
        int lookback = 0;
        RetCode retCode = null;
        //CDL2CROWS两只乌鸦 三日K线模式，第一天长阳，第二天高开收阴，第三天再次高开继续收阴，收盘比前一日收盘价低，预示股价下跌。
        if ("cdl2Crows".equals(patternType)) {
            lookback = c.cdl2CrowsLookback();
            retCode = c.cdl2Crows(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDL3BLACKCROWS三只乌鸦 三日K线模式，连续三根阴线，每日收盘价都下跌且接近最低价，每日开盘价都在上根K线实体内，预示股价下跌。
        if ("cdl3BlackCrows".equals(patternType)) {
            lookback = c.cdl3BlackCrowsLookback();
            retCode = c.cdl3BlackCrows(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDL3INSIDE三内部上涨和下跌 三日K线模式，母子信号+长K线，以三内部上涨为例，K线为阴阳阳，第三天收盘价高于第一天开盘价，第二天K线在第一天K线内部，预示着股价上涨。
        if ("cdl3BlackCrows".equals(patternType)) {
            lookback = c.cdl3InsideLookback();
            retCode = c.cdl3Inside(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDL3LINESTRIKE三线打击 四日K线模式，前三根阳线，每日收盘价都比前一日高，开盘价在前一日实体内，第四日市场高开，收盘价低于第一日开盘价，预示股价下跌。
        if ("cdl3LineStrike".equals(patternType)) {
            lookback = c.cdl3LineStrikeLookback();
            retCode = c.cdl3LineStrike(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDL3OUTSIDE三外部上涨和下跌 三日K线模式，与三内部上涨和下跌类似，K线为阴阳阳，但第一日与第二日的K线形态相反，以三外部上涨为例，第一日K线在第二日K线内部，预示着股价上涨。
        if ("cdl3Outside".equals(patternType)) {
            lookback = c.cdl3OutsideLookback();
            retCode = c.cdl3Outside(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        /**
         * CDL3STARSINSOUTH南方三星
         * 三日K线模式，与大敌当前相反，三日K线皆阴，第一日有长下影线，第二日与第一日类似，K线整体小于第一日，第三日无下影线实体信号，成交价格都在第一日振幅之内，预示下跌趋势反转，股价上升
         */
        if ("cdl3StarsInSouth".equals(patternType)) {
            lookback = c.cdl3StarsInSouthLookback();
            retCode = c.cdl3StarsInSouth(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDL3WHITESOLDIERS三个白兵 三日K线模式，三日K线皆阳，每日收盘价变高且接近最高价，开盘价在前一日实体上半部，预示股价上升。
        if ("cdl3WhiteSoldiers".equals(patternType)) {
            lookback = c.cdl3WhiteSoldiersLookback();
            retCode = c.cdl3WhiteSoldiers(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDLABANDONEDBABY弃婴 三日K线模式，第二日价格跳空且收十字星（开盘价与收盘价接近，最高价最低价相差不大），预示趋势反转，发生在顶部下跌，底部上涨。
        if ("cdlAbandonedBaby".equals(patternType)) {
            lookback = c.cdlAbandonedBabyLookback(0);
            retCode = c.cdlAbandonedBaby(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, 0,new MInteger(), new MInteger(), outReal);
        }
        //CDLADVANCEBLOCK 大敌当前 三日K线模式，三日都收阳，每日收盘价都比前一日高，开盘价都在前一日实体以内，实体变短，上影线变长。
        if ("cdlAdvanceBlock".equals(patternType)) {
            lookback = c.cdlAdvanceBlockLookback();
            retCode = c.cdlAdvanceBlock(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDLBELTHOLD 捉腰带线 两日K线模式，下跌趋势中，第一日阴线，第二日开盘价为最低价，阳线，收盘价接近最高价，预示价格上涨。
        if ("cdlBeltHold".equals(patternType)) {
            lookback = c.cdlBeltHoldLookback();
            retCode = c.cdlBeltHold(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDLBREAKAWAY 脱离 五日K线模式，以看涨脱离为例，下跌趋势中，第一日长阴线，第二日跳空阴线，延续趋势开始震荡，第五日长阳线，收盘价在第一天收盘价与第二天开盘价之间，预示价格上涨
        if ("cdlBreakaway".equals(patternType)) {
            lookback = c.cdlBreakawayLookback();
            retCode = c.cdlBreakaway(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDLCLOSINGMARUBOZU 收盘缺影线 一日K线模式，以阳线为例，最低价低于开盘价，收盘价等于最高价，预示着趋势持续
        if ("cdlClosingMarubozu".equals(patternType)) {
            lookback = c.cdlClosingMarubozuLookback();
            retCode = c.cdlClosingMarubozu(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDLCONCEALBABYSWALL藏婴吞没 四日K线模式，下跌趋势中，前两日阴线无影线，第二日开盘、收盘价皆低于第二日，第三日倒锤头，第四日开盘价高于前一日最高价，收盘价低于前一日最低价，预示着底部反转。
        if ("cdlConcealBabysWall".equals(patternType)) {
            lookback = c.cdlConcealBabysWallLookback();
            retCode = c.cdlConcealBabysWall(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDLDARKCLOUDCOVER 乌云压顶 二日K线模式，第一日长阳，第二日开盘价高于前一日最高价，收盘价处于前一日实体中部以下，预示着股价下跌。
        if ("cdlDarkCloudCover".equals(patternType)) {
            lookback = c.cdlDarkCloudCoverLookback(0);
            retCode = c.cdlDarkCloudCover(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, 0,new MInteger(), new MInteger(), outReal);
        }
        //CDLDOJI十字 一日K线模式，开盘价与收盘价基本相同。
        if ("cdlDoji".equals(patternType)) {
            lookback = c.cdlDojiLookback();
            retCode = c.cdlDoji(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDLDOJISTAR 十字星 一日K线模式，开盘价与收盘价基本相同，上下影线不会很长，预示着当前趋势反转
        if ("cdlDojiStar".equals(patternType)) {
            lookback = c.cdlDojiStarLookback();
            retCode = c.cdlDojiStar(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDLDRAGONFLYDOJI 蜻蜓十字/T形十字 一日K线模式，开盘后价格一路走低，之后收复，收盘价与开盘价相同，预示趋势反转
        if ("cdlDragonflyDoji".equals(patternType)) {
            lookback = c.cdlDragonflyDojiLookback();
            retCode = c.cdlDragonflyDoji(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDLENGULFING 吞噬模式 两日K线模式，分多头吞噬和空头吞噬，以多头吞噬为例，第一日为阴线，第二日阳线，第一日的开盘价和收盘价在第二日开盘价收盘价之内，但不能完全相同。
        if ("cdlEngulfing".equals(patternType)) {
            lookback = c.cdlEngulfingLookback();
            retCode = c.cdlEngulfing(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDLEVENINGDOJISTAR 十字暮星 三日K线模式，基本模式为暮星，第二日收盘价和开盘价相同，预示顶部反转
        if ("cdlEveningDojiStar".equals(patternType)) {
            lookback = c.cdlEveningDojiStarLookback(0);
            retCode = c.cdlEveningDojiStar(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, 0,new MInteger(), new MInteger(), outReal);
        }
        /*******/
        //CDLEVENINGSTAR 暮星 三日K线模式，与晨星相反，上升趋势中,第一日阳线，第二日价格振幅较小，第三日阴线，预示顶部反转。
        if ("cdlEveningStar".equals(patternType)) {
            lookback = c.cdlEveningStarLookback(0);
            retCode = c.cdlEveningStar(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, 0,new MInteger(), new MInteger(), outReal);
        }
        //CDLGAPSIDESIDEWHITE 向上/下跳空并列阳线 二日K线模式，上升趋势向上跳空，下跌趋势向下跳空,第一日与第二日有相同开盘价，实体长度差不多，则趋势持续。
        if ("cdlGapSideSideWhite".equals(patternType)) {
            lookback = c.cdlGapSideSideWhiteLookback();
            retCode = c.cdlGapSideSideWhite(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLGRAVESTONEDOJI 墓碑十字/倒T十字 一日K线模式，开盘价与收盘价相同，上影线长，无下影线，预示底部反转
        if ("cdlGravestoneDoji".equals(patternType)) {
            lookback = c.cdlGravestoneDojiLookback();
            retCode = c.cdlGravestoneDoji(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //cdlHangingMan 上吊线 一日K线模式，形状与锤子类似，处于上升趋势的顶部，预示着趋势反转
        if ("cdlHangingMan".equals(patternType)) {
            lookback = c.cdlHangingManLookback();
            retCode = c.cdlHangingMan(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLHARAMI 母子线 二日K线模式，分多头母子与空头母子，两者相反，以多头母子为例，在下跌趋势中，第一日K线长阴，第二日开盘价收盘价在第一日价格振幅之内，为阳线，预示趋势反转，股价上升。
        if ("cdlHarami".equals(patternType)) {
            lookback = c.cdlHaramiLookback();
            retCode = c.cdlHarami(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLHARAMICROSS 十字孕线 二日K线模式，与母子县类似，若第二日K线是十字线，便称为十字孕线，预示着趋势反转。
        if ("cdlHaramiCross".equals(patternType)) {
            lookback = c.cdlHaramiCrossLookback();
            retCode = c.cdlHaramiCross(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLPIERCING 刺透形态 两日K线模式，下跌趋势中，第一日阴线，第二日收盘价低于前一日最低价，收盘价处在第一日实体上部，预示着底部反转。
        if ("cdlPiercing".equals(patternType)) {
            lookback = c.cdlPiercingLookback();
            retCode = c.cdlPiercing(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLRISEFALL3METHODS 上升/下降三法 五日K线模式，以上升三法为例，上涨趋势中，第一日长阳线，中间三日价格在第一日范围内小幅震荡，第五日长阳线，收盘价高于第一日收盘价，预示股价上升
        if ("cdlRiseFall3Methods".equals(patternType)) {
            lookback = c.cdlRiseFall3MethodsLookback();
            retCode = c.cdlRiseFall3Methods(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLSHORTLINE 短蜡烛 一日K线模式，实体短，无上下影线
        if ("cdlShortLine".equals(patternType)) {
            lookback = c.cdlShortLineLookback();
            retCode = c.cdlShortLine(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLSPINNINGTOP 纺锤 一日K线，实体小。
        if ("cdlSpinningTop".equals(patternType)) {
            lookback = c.cdlSpinningTopLookback();
            retCode = c.cdlSpinningTop(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLSTALLEDPATTERN 停顿形态 三日K线模式，上涨趋势中，第二日长阳线，第三日开盘于前一日收盘价附近，短阳线，预示着上涨结束
        if ("cdlStalledPattern".equals(patternType)) {
            lookback = c.cdlStalledPatternLookback();
            retCode = c.cdlStalledPattern(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLTASUKIGAP 跳空并列阴阳线 三日K线模式，分上涨和下跌，以上升为例，前两日阳线，第二日跳空，第三日阴线，收盘价于缺口中，上升趋势持续。
        if ("cdlTasukiGap".equals(patternType)) {
            lookback = c.cdlTasukiGapLookback();
            retCode = c.cdlTasukiGap(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLTHRUSTING 插入 二日K线模式，与颈上线类似，下跌趋势中，第一日长阴线，第二日开盘价跳空，收盘价略低于前一日实体中部，与颈上线相比实体较长，预示着趋势持续。
        if ("cdlThrusting".equals(patternType)) {
            lookback = c.cdlThrustingLookback();
            retCode = c.cdlThrusting(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLTRISTAR三星 三日K线模式，由三个十字组成，第二日十字必须高于或者低于第一日和第三日，预示着反转。
        if ("cdlThrusting".equals(patternType)) {
            lookback = c.cdlTristarLookback();
            retCode = c.cdlTristar(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLUNIQUE3RIVER 奇特三河床 三日K线模式，下跌趋势中，第一日长阴线，第二日为锤头，最低价创新低，第三日开盘价低于第二日收盘价，收阳线，收盘价不高于第二日收盘价，预示着反转，第二日下影线越长可能性越大。
        if ("cdlUnique3River".equals(patternType)) {
            lookback = c.cdlUnique3RiverLookback();
            retCode = c.cdlUnique3River(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLUPSIDEGAP2CROWS 向上跳空的两只乌鸦 三日K线模式，第一日阳线，第二日跳空以高于第一日最高价开盘，收阴线，第三日开盘价高于第二日，收阴线，与第一日比仍有缺口。
        if ("cdlUpsideGap2Crows".equals(patternType)) {
            lookback = c.cdlUpsideGap2CrowsLookback();
            retCode = c.cdlUpsideGap2Crows(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //CDLXSIDEGAP3METHODS 上升/下降跳空三法 五日K线模式，以上升跳空三法为例，上涨趋势中，第一日长阳线，第二日短阳线，第三日跳空阳线，第四日阴线，开盘价与收盘价于前两日实体内，第五日长阳线，收盘价高于第一日收盘价，预示股价上升。
        if ("cdlUpsideGap2Crows".equals(patternType)) {
            lookback = c.cdlXSideGap3MethodsLookback();
            retCode = c.cdlXSideGap3Methods(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose,new MInteger(), new MInteger(), outReal);
        }
        //cdlTakuri探水竿 一日K线模式，大致与蜻蜓十字相同，下影线长度长。
        if ("cdlTakuri".equals(patternType)) {
            lookback = c.cdlTakuriLookback();
            retCode = c.cdlTakuri(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        // cdlInvertedHammer倒锤头  一日K线模式，上影线较长，长度为实体2倍以上，无下影线，在下跌趋势底部，预示着趋势反转。
        if ("cdlInvertedHammer".equals(patternType)) {
            lookback = c.cdlInvertedHammerLookback();
            retCode = c.cdlInvertedHammer(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        //CDLHAMMER 锤头 一日K线模式，实体较短，无上影线，下影线大于实体长度两倍，处于下跌趋势底部，预示反转。
        if ("cdlHammer".equals(patternType)) {
            lookback = c.cdlHammerLookback();
            retCode = c.cdlHammer(0, idArr.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        // CDLSHOOTINGSTAR 射击之星 一日K线模式，上影线至少为实体长度两倍，没有下影线，预示着股价下跌
        if ("cdlShootingStar".equals(patternType)) {
            lookback = c.cdlShootingStarLookback();
            retCode = c.cdlShootingStar(0, inOpen.length - 1, inOpen, inHigh, inLow, inClose, new MInteger(), new MInteger(), outReal);
        }
        ArrayList<Long> tids = new ArrayList<>();
        ArrayList<Long> fids = new ArrayList<>();
        for (int i = 0; i < outReal.length; i++) {
            if (outReal[i] == 100) {
                tids.add(new Double(idArr[i + lookback]).longValue());
            }
            if (outReal[i] == -100) {
                fids.add(new Double(idArr[i + lookback]).longValue());
            }
        }
        Map<String,ArrayList<Long>> idsMap = new HashMap<>();
        idsMap.put("tids",tids);
        idsMap.put("fids",fids);
        if (retCode == RetCode.Success) {
            return idsMap;
        }
        return idsMap;
    }

    /**
     * 突破策略
     */
    public static List<Long> superTrend(double[] _src,double[] _atr,double[] _close,Long[] _id) {
        if (_src.length != _atr.length) {
            _src = Arrays.copyOfRange(_src,_src.length-_atr.length,_src.length);
            _close = Arrays.copyOfRange(_close,_close.length-_atr.length,_close.length);
            _id = Arrays.copyOfRange(_id,_id.length-_atr.length,_id.length);
        }
        int length = _src.length;
        double[] up = new double[length];
        double[] dn = new double[length];
        Integer[] trend = new Integer[length];
        List<Long> klineIdList =  new ArrayList<>();
        for (int i = 0; i < length; i++) {
            up[i] = Arith.sub(_src[i],Arith.mul(AtrMultiplier,_atr[i]));
            double up_pre = i == 0 ? up[0] : up[i-1];
            up[i] = i == 0 ? up[i] : (_close[i-1] > up_pre ? Math.max(up[i], up_pre) : up[i]);

            dn[i] = Arith.add(_src[i],Arith.mul(AtrMultiplier,_atr[i]));
            double dn_pre = i == 0 ? dn[0] : dn[i-1];
            dn[i] = i == 0 ? dn[i] : (_close[i-1] < dn_pre ? Math.min(dn[i], dn_pre) : dn[i]);

            trend[i] = i == 0 ? 1 : trend[i-1];
            trend[i] = (trend[i] == -1) && _close[i] > dn_pre ? 1 : (trend[i] == 1) && (_close[i] < up_pre) ? -1 : trend[i];
            if (i != 0 && trend[i] == 1 && trend[i-1] == -1) {
                klineIdList.add(new Double(_id[i]).longValue());
            }
        }
        return klineIdList;
    }
}
