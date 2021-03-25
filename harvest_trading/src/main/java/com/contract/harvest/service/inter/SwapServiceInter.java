package com.contract.harvest.service.inter;

import com.contract.harvest.common.Depth;
import com.huobiswap.api.enums.DirectionEnum;
import com.huobiswap.api.enums.OffsetEnum;
import com.huobiswap.api.exception.ApiException;
import com.huobiswap.api.request.trade.SwapOrderRequest;
import com.huobiswap.api.response.account.SwapPositionInfoResponse;
import com.huobiswap.api.response.market.SwapContractInfoResponse;

import java.util.List;

public interface SwapServiceInter {
    /**
     * 获取合约信息
     * @param contractCode 币种
     */
    SwapContractInfoResponse.DataBean getContractInfo(String contractCode) throws ApiException, NullPointerException;
    /**
     * 获取下单实例
     * @param contractCode 合约代码
     * @param offset open or close
     * @param direction 交易方向
     * @param dealVolume 交易数量
     */
    SwapOrderRequest getPlanOrder(String contractCode, OffsetEnum offset, DirectionEnum direction, long dealVolume, double stopPercent, double limitPercent) throws InterruptedException, NullPointerException, ApiException;

    /**
     * 处理订单
     * @param order 订单实体
     */
    void handleOrder(SwapOrderRequest order) throws InterruptedException, NullPointerException, ApiException;

    /**
     * 获取持仓信息
     * @return ContractPositionInfoResponse
     */
    List<SwapPositionInfoResponse.DataBean> getContractPositionInfo(String contractCode) throws ApiException,NullPointerException;
//    /**
//     * 设置持仓信息
//     * @param symbol String
//     */
//    void setContractPositionInfo(String symbol) throws ApiException,NullPointerException;
//
//    /**
//     * 将订单拆分成盈利订单 和亏损订单
//     * @param symbol String
//     */
//    void contractLossWinOrder(String symbol) throws ApiException,NullPointerException;
//
//    /**
//     * 获取最大可开仓张数
//     * @param symbol String
//     * @return int
//     */
//    int getMaxOpenVolume(String symbol) throws NullPointerException;
}
