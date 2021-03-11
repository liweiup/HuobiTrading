package com.contract.harvest.service.inter;

import com.alibaba.fastjson.JSONObject;
import com.contract.harvest.common.Depth;
import com.contract.harvest.entity.Candlestick;
import com.huobi.api.enums.DirectionEnum;
import com.huobi.api.enums.OffsetEnum;
import com.huobi.api.exception.ApiException;
import com.huobi.api.request.trade.ContractOrderRequest;
import com.huobi.api.response.account.ContractPositionInfoResponse;
import com.huobi.api.response.market.ContractContractCodeResponse;
import org.apache.http.HttpException;

import java.io.IOException;
import java.util.List;

public interface DataServiceInter {
    /**
     * 获取kline数据
     * @param channel 订阅的标识 如 BSV_CW
     * @param topicIndex k线周期
     */
    List<Candlestick.DataBean> getKlineList(String channel, int topicIndex);
    /**
     * 获取合约信息
     * @param symbol 币种
     * @param contractType 合约类型: （this_week:当周 next_week:下周 quarter:当季 next_quarter:次季）
     * @param contractCode 合约代码
     */
    ContractContractCodeResponse.DataBean getContractInfo(String symbol, String contractType, String contractCode) throws ApiException, NullPointerException;
    /**
     * 获取下单实例
     * @param symbol 币种
     * @param contractFlag 交易合约类型
     * @param contractCode 合约代码
     * @param offset open or close
     * @param direction 交易方向
     * @param dealVolume 交易数量
     */
    ContractOrderRequest getPlanOrder(String symbol, String contractFlag, String contractCode, OffsetEnum offset, DirectionEnum direction, long dealVolume,double stopPercent, double limitPercent) throws InterruptedException, NullPointerException, ApiException;
    /**
     * 获取购买价格，与卖出价格
     * @param depthSubKey 成交帐簿的key
     */
    Depth getBidAskPrice(String depthSubKey) throws InterruptedException, NullPointerException, ApiException;
    /**
     * 生成随机订单id
     * @return Long
     */
    Long getClientOrderId();
    /**
     * 处理订单
     * @param order 订单实体
     */
    void handleOrder(ContractOrderRequest order) throws InterruptedException, NullPointerException, ApiException;

    /**
     * 获取持仓信息
     * @param symbol String
     * @return ContractPositionInfoResponse
     */
    List<ContractPositionInfoResponse.DataBean> getContractPositionInfo(String symbol,String contractType,String contractCode) throws ApiException,NullPointerException;
    /**
     * 设置持仓信息
     * @param symbol String
     */
    void setContractPositionInfo(String symbol) throws ApiException,NullPointerException;

}
