package com.contract.harvest.service.inter;

import com.contract.harvest.common.Depth;
import com.contract.harvest.common.OpenInfo;
import com.contract.harvest.entity.Candlestick;
import com.huobi.api.exception.ApiException;

import java.util.List;

public interface DataServiceInter {
    /**
     * 获取kline数据
     * @param channel 订阅的标识 如 BSV_CW
     * @param topicIndex k线周期
     */
    List<Candlestick.DataBean> getKlineList(String channel, int topicIndex);
    /**
     * 生成随机订单id
     * @return Long
     */
    Long getClientOrderId();
    /**
     * 获取购买价格，与卖出价格
     * @param depthSubKey 成交帐簿的key
     */
    Depth getBidAskPrice(String depthSubKey) throws InterruptedException, NullPointerException, ApiException;

    /**
     * 获取过往的K线
     */
    List<Candlestick.DataBean> getBeforeManyLine(String symbol, int topicIndex);

    /**
     * 存放k线数据
     */
    void saveIndexCalculation(int topicIndex);
    /**
     * 获取开仓参数
     */
    OpenInfo getOpenInfo(String symbol);
}
