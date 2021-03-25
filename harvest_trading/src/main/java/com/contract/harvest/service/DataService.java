package com.contract.harvest.service;

import com.alibaba.fastjson.JSON;
import com.contract.harvest.common.Depth;
import com.contract.harvest.common.PubConst;
import com.contract.harvest.common.Topic;
import com.contract.harvest.entity.Candlestick;
import com.contract.harvest.entity.HuobiEntity;
import com.contract.harvest.service.inter.DataServiceInter;
import com.contract.harvest.tools.CodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * @author liwei
 */
@Slf4j
@Service
public class DataService implements DataServiceInter {

    @Resource
    private RedisService redisService;
    @Resource
    private HuobiEntity huobiEntity;
    @Resource
    private MailService mailService;

    /**
     * 获取kline数据
     * @param channel 订阅的标识 如 BSV_CW
     * @param topicIndex k线周期
     */
    @Override
    public List<Candlestick.DataBean> getKlineList(String channel, int topicIndex) throws NullPointerException,IllegalArgumentException{
        //最新的一条k线
        String lineKey = Topic.formatChannel(Topic.KLINE_SUB,channel, topicIndex).toUpperCase();
        String lineData = redisService.hashGet(CacheService.HUOBI_SUB,lineKey);
        if ("".equals(lineData)) {
            throw new NullPointerException(CodeConstant.getMsg(CodeConstant.NONE_KLINE_DATA));
        }
        Candlestick.DataBean tick = JSON.parseObject(lineData,Candlestick.class).getTick();
        //过往的x条k线
        String manyLineStr = redisService.hashGet(CacheService.HUOBI_KLINE,channel+Topic.PERIOD[PubConst.TOPIC_INDEX]);
        if ("".equals(manyLineStr)) {
            throw new NullPointerException(CodeConstant.getMsg(CodeConstant.NONE_KLINE_DATA));
        }
        List<Candlestick.DataBean> tickList = JSON.parseObject(manyLineStr,Candlestick.class).getData();
        if (tick.getId() < tickList.get(tickList.size()-1).getId()) {
            throw new IllegalArgumentException(CodeConstant.getMsg(CodeConstant.KLINE_DATE_ERROR));
        }
        tickList.set(tickList.size()-1,tick);
        return tickList;
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
}
