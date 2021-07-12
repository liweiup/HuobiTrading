package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.contract.harvest.common.Depth;
import com.contract.harvest.common.OpenInfo;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.common.Topic;
import com.contract.harvest.entity.Candlestick;
import com.contract.harvest.entity.CandlestickData;
import com.contract.harvest.entity.HuobiEntity;
import com.contract.harvest.entity.HuobiSwapEntity;
import com.contract.harvest.service.inter.DataServiceInter;
import com.contract.harvest.tools.CodeConstant;
import com.contract.harvest.tools.IndexCalculation;
import com.huobi.api.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liwei
 */
@Slf4j
@Service
@CacheConfig(cacheNames="HUOBI:CACHE",cacheManager = "caffeineCacheManger")
public class DataService implements DataServiceInter {

    @Resource
    private RedisService redisService;
    @Resource
    private HuobiEntity huobiEntity;
    @Resource
    private HuobiSwapEntity huobiSwapEntity;
    @Resource
    private ScheduledService scheduledService;
    @Resource
    private CacheService cacheService;

    /**
     * 获取kline数据
     * @param channel 订阅的标识 如 BSV_CW
     * @param topicIndex k线周期
     */
    @Override
    public List<Candlestick.DataBean> getKlineList(String channel, int topicIndex,int tryNum) throws NullPointerException, IllegalArgumentException, InterruptedException {
        //最新的一条k线
        String lineKey = Topic.formatChannel(Topic.KLINE_SUB,channel, topicIndex).toUpperCase();
        String lineData = redisService.hashGet(CacheService.HUOBI_SUB,lineKey);
        if ("".equals(lineData)) {
            scheduledService.indexCalculation();
            throw new NullPointerException(CodeConstant.getMsg(CodeConstant.NONE_KLINE_DATA));
        }
        Candlestick.DataBean tick = JSON.parseObject(lineData,Candlestick.class).getTick();
        //过往的x条k线
        List<Candlestick.DataBean> tickList = cacheService.getBeforeManyLine(channel,topicIndex);
        if (tick.getId().equals(tickList.get(tickList.size()-1).getId())) {
            tickList.set(tickList.size()-1,tick);
        } else if (tick.getId() - 60 * PubConst.DATE_INDEX[topicIndex] == tickList.get(tickList.size()-1).getId()) {
            tickList.add(tick);
        } else {
            scheduledService.indexCalculation();
            throw new IllegalArgumentException(CodeConstant.getMsg(CodeConstant.KLINE_DATE_ERROR));
        }
        tickList = tickList.stream().filter(t->t.getVol().intValue() != 0).collect(Collectors.toList());
        return tickList;
    }

    /**
     * 获取过往的k线
     */
    @Override
    public List<Candlestick.DataBean> getBeforeManyLine(String symbol, int topicIndex) {
        String manyLineStr = redisService.hashGet(CacheService.HUOBI_KLINE,symbol + Topic.PERIOD[topicIndex]);
        if ("".equals(manyLineStr)) {
            throw new NullPointerException(CodeConstant.getMsg(CodeConstant.NONE_KLINE_DATA));
        }
        return JSON.parseObject(manyLineStr,Candlestick.class).getData();
    }

    @Override
    public OpenInfo getOpenInfo(String symbol) {
        String openInfo = redisService.hashGet(CacheService.HUOBI_OPEN_INFO,symbol);
        return JSON.parseObject(openInfo,OpenInfo.class);
    }

    /**
     * 生成随机订单id
     */
    @Override
    public Long getClientOrderId() {
        Random random = new Random();
        // 随机数的量 自由定制，这是9位随机数
        int r = random.nextInt(900) + 100;
        // 返回  17位时间
        DateFormat sdf = new SimpleDateFormat("mmssSSS");
        String timeStr = sdf.format(new Date());
        // 17位时间+9位随机数
        return  Long.valueOf(timeStr + r);
    }
    /**
     * 获取购买价格，与卖出价格
     * @param depthSubKey 成交帐簿的key
     */
    @Override
    public Depth getBidAskPrice(String depthSubKey) throws NullPointerException, InterruptedException {
        String depthStr = redisService.hashGet(CacheService.HUOBI_SUB,depthSubKey);
        if ("".equals(depthStr)) {
            Thread.sleep(1000);
            return getBidAskPrice(depthSubKey);
        }
        return JSON.parseObject(depthStr, Depth.class);
    }

    /**
     * 存放k线数据
     * @param topicIndex 时间周期
     */
    @Override
    public void saveIndexCalculation(int topicIndex) throws ApiException {
        //交割合约
        for (String symbol : scheduledService.getSymbol(0)) {
            String symbolFlag = symbol + PubConst.DEFAULT_CS;
            //开仓参数
            OpenInfo openInfo = getOpenInfo(symbolFlag);
            topicIndex = openInfo.getTopicIndex();
            String strData = huobiEntity.getMarketHistoryKline(symbolFlag,Topic.PERIOD[topicIndex],PubConst.GET_KLINE_NUM);
            redisService.hashSet(CacheService.HUOBI_KLINE,symbolFlag + Topic.PERIOD[openInfo.getTopicIndex()],strData);
            cacheService.setBeforeManyLine(symbolFlag,topicIndex);
        }
        //永续合约
        for (String symbol : scheduledService.getSymbol(1)) {
            String symbolFlag = symbol+PubConst.SWAP_USDT;
            OpenInfo openInfo = getOpenInfo(symbolFlag);
            topicIndex = openInfo.getTopicIndex();
            String swapStrData = huobiSwapEntity.getSwapMarketHistoryKline(symbolFlag,Topic.PERIOD[topicIndex],PubConst.GET_KLINE_NUM);
            redisService.hashSet(CacheService.HUOBI_KLINE,symbolFlag + Topic.PERIOD[topicIndex],swapStrData);
            cacheService.setBeforeManyLine(symbol+PubConst.SWAP_USDT,topicIndex);
        }
    }

    /**
     * 获取止损点
     */
    public double getStopPercent(String symbol,double percent,String type) {
        String key = "DELI".equals(type) ? CacheService.OPEN_VOLUME : CacheService.SWAP_OPEN_VOLUME;
        int len = redisService.getListLen(key + symbol).intValue();
        percent = len == 4 ? percent - 0.04 : percent;
        return percent;
    }

    /**
     * 判断Atr转向
     */
    public int judgeTrendVeer(String symbol,int topicIndex,int atrMultiplier) {
        List<Candlestick.DataBean> candlestickList = getBeforeManyLine(symbol,topicIndex);
        //kline的列值
        CandlestickData tickColumnData = new CandlestickData(candlestickList);
        //计算atr
        double[] atr = IndexCalculation.volatilityIndicators(tickColumnData.open,tickColumnData.high,tickColumnData.low,tickColumnData.close,tickColumnData.id,14,"atr");
        //计算trend
        List<Long> trendList = IndexCalculation.superTrend(tickColumnData.hl2,atr,tickColumnData.close,tickColumnData.id,atrMultiplier,-1);
        return trendList.get(trendList.size() - 1).intValue();
    }
}
