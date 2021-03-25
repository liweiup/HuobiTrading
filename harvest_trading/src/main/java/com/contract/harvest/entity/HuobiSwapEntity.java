package com.contract.harvest.entity;

import com.alibaba.fastjson.JSON;
import com.huobiswap.api.exception.ApiException;
import com.huobiswap.api.request.account.SwapMarketHistoryKlineRequest;
import com.huobiswap.api.request.trade.SwapCancelRequest;
import com.huobiswap.api.request.trade.SwapCancelallRequest;
import com.huobiswap.api.request.trade.SwapOrderInfoRequest;
import com.huobiswap.api.request.trade.SwapOrderRequest;
import com.huobiswap.api.response.account.SwapAccountInfoResponse;
import com.huobiswap.api.response.account.SwapPositionInfoResponse;
import com.huobiswap.api.response.market.SwapContractInfoResponse;
import com.huobiswap.api.response.market.SwapMarketHistoryKlineResponse;
import com.huobiswap.api.response.trade.SwapCancelResponse;
import com.huobiswap.api.response.trade.SwapCancelallResponse;
import com.huobiswap.api.response.trade.SwapOrderInfoResponse;
import com.huobiswap.api.response.trade.SwapOrderResponse;
import com.huobiswap.api.service.account.AccountAPIServiceImpl;
import com.huobiswap.api.service.market.MarketAPIServiceImpl;
import com.huobiswap.api.service.trade.TradeAPIServiceImpl;
import com.huobiswap.api.service.transfer.TransferApiServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpException;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Repository
@CacheConfig(cacheNames="HUOBI:API:CACHE")
public class HuobiSwapEntity {

    @Resource
    private MarketAPIServiceImpl swapMarketApi;
    @Resource
    private AccountAPIServiceImpl swapAccountApi;
    @Resource
    private TradeAPIServiceImpl swapTradeApi;
    @Resource
    private TransferApiServiceImpl swapTransferApi;

    /**
     * 获取合约信息
     * @param contractCode false	string	合约代码，不填查询所有合约	BTC-USDT
     * @param supportMarginMode false	string	合约支持的保证金模式	cross：仅支持全仓模式；isolated：仅支持逐仓模式；all：全逐仓都支持
     */
    @Cacheable(keyGenerator = "HuobiEntity_keyGenerator",cacheManager = "huobiEntityRedisCacheManager")
    public String getSwapContractInfo(String contractCode, String supportMarginMode) throws ApiException {
        SwapContractInfoResponse result =
                swapMarketApi.getSwapContractInfo(contractCode,supportMarginMode);
        return JSON.toJSONString(result);
    }

    /**
     * 获取K线数据
     * period	true	string	K线类型		1min, 5min, 15min, 30min, 60min,4hour,1day, 1mon
     * size	true	integer	获取数量	150	[1,2000]
     * from	false	integer	开始时间戳 10位 单位S
     * to	false	integer	结束时间戳 10位 单位S
     */
    public String getSwapMarketHistoryKline(String contractCode,String period,int size) throws ApiException {
        SwapMarketHistoryKlineRequest result = SwapMarketHistoryKlineRequest.builder()
                .contractCode(contractCode)
                .period(period)
                .size(size)
                .build();
        SwapMarketHistoryKlineResponse response = swapMarketApi.getSwapMarketHistoryKline(result);
        return JSON.toJSONString(response);
    }

    /**
     * 获取用户账户信息
     * @param contractCode string	品种代码 "BTC","ETH"...如果缺省，默认返回所有品种
     */
    public String getContractAccountInfo(String contractCode) throws ApiException {
        SwapAccountInfoResponse response = swapAccountApi.getSwapAccountInfo(contractCode);
        return JSON.toJSONString(response);
    }

    /**
     * 获取用户持仓信息
     * @param contractCode string
     */
    public String getContractPositionInfo(String contractCode) throws ApiException {
        SwapPositionInfoResponse response = swapAccountApi.getSwapPositionInfo(contractCode);
        return JSON.toJSONString(response);
    }

    /*************************************合约下单************************************************************/

    /**
     * 合约下单
     */
    public String futureContractOrder(SwapOrderRequest order) {
        SwapOrderResponse response =
                swapTradeApi.swapOrderRequest(order);
        return JSON.toJSONString(response);
    }

    /**
     * 合约取消订单
     * POST api/v1/contract_cancel
     * 参数名称	是否必须	类型	描述
     * order_id	false	string	订单ID(多个订单ID中间以","分隔,一次最多允许撤消10个订单)
     * clientOrderId	false	string	客户订单ID(多个订单ID中间以","分隔,一次最多允许撤消10个订单)
     */
    public String swapCancelRequest(String orderId, String clientOrderId, String contractCode) throws ApiException {
        SwapCancelRequest request = SwapCancelRequest.builder()
                .contractCode(contractCode)
                .orderId(orderId)
                .clientOrderId(clientOrderId)
                .build();
        SwapCancelResponse response =
                swapTradeApi.swapCancelRequest(request);
        return JSON.toJSONString(response);
    }
    /**
     * 合约全部撤单
     */
    public String swapCancelallRequest(String contractCode) throws IOException, HttpException {
        SwapCancelallRequest request = SwapCancelallRequest.builder()
                .contractCode(contractCode)
                .build();
        SwapCancelallResponse response =
                swapTradeApi.swapCancelallRequest(request);
        return JSON.toJSONString(response);
    }

    /**
     * 获取合约订单信息
     * 参数名称 是否必须    类型	描述
     * order_id	请看备注	string	订单ID(多个订单ID中间以","分隔,一次最多允许查询50个订单)
     * client_order_id	请看备注	string	客户订单ID(多个订单ID中间以","分隔,一次最多允许查询50个订单)
     * symbol	true	string	"BTC","ETH"...
     */
    public String getcontractOrderInfo(String orderId, String clientOrderId, String contractCode) throws ApiException {
        SwapOrderInfoRequest request = SwapOrderInfoRequest.builder()
                .contractCode(contractCode)
                .clientOrderId(clientOrderId)
                .orderId(orderId)
                .build();
        SwapOrderInfoResponse response =
                swapTradeApi.swapOrderInfoRequest(request);
        return JSON.toJSONString(response);
    }
}
