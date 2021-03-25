package com.contract.harvest.service.inter;

import com.contract.harvest.common.PubConst;
import com.huobi.api.enums.DirectionEnum;
import com.huobi.api.enums.OffsetEnum;
import com.huobi.api.exception.ApiException;
import com.huobi.api.request.trade.ContractOrderRequest;
import com.huobi.api.response.account.ContractPositionInfoResponse;
import com.huobi.api.response.market.ContractContractCodeResponse;

import java.util.List;

public interface DeliveryServiceInter {
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

    /**
     * 将订单拆分成盈利订单 和亏损订单
     * @param symbol String
     */
    void contractLossWinOrder(String symbol, PubConst.UPSTRATGY upStratgy) throws ApiException,NullPointerException;

    /**
     * 获取最大可开仓张数
     * @param symbol String
     * @return int
     */
    int getMaxOpenVolume(String symbol) throws NullPointerException;
}
