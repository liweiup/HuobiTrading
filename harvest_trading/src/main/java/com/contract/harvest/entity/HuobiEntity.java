package com.contract.harvest.entity;

import com.alibaba.fastjson.JSON;
import com.huobi.api.exception.ApiException;
import com.huobi.api.request.trade.*;
import com.huobi.api.response.account.ContractAccountInfoResponse;
import com.huobi.api.response.account.ContractAccountPositionInfoResponse;
import com.huobi.api.response.account.ContractPositionInfoResponse;
import com.huobi.api.response.market.*;
import com.huobi.api.response.trade.*;
import com.huobi.api.service.account.AccountAPIServiceImpl;
import com.huobi.api.service.market.MarketAPIServiceImpl;
import com.huobi.api.service.trade.TradeAPIServiceImpl;
import com.huobi.api.service.transfer.TransferApiServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpException;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Slf4j
@Repository
@CacheConfig(cacheNames="HUOBI:API:CACHE")
public class HuobiEntity {

    @Resource
    private MarketAPIServiceImpl marketApi;
    @Resource
    private AccountAPIServiceImpl accountApi;
    @Resource
    private TradeAPIServiceImpl tradeApi;
    @Resource
    private TransferApiServiceImpl transferApi;

    //获取合约信息
    @Cacheable(keyGenerator = "HuobiEntity_keyGenerator",value = "HBCACHE:ENTITY", cacheManager = "huobiEntityRedisCacheManager")
    public String getContractInfo(String symbol, String contractType, String contractCode) throws ApiException {
        ContractContractCodeResponse result =
                marketApi.getContractContractInfo(symbol, contractType, contractCode);
        return JSON.toJSONString(result);
    }

    //获取合约指数信息
    public String getContractIndex(String symbol) throws ApiException {
        ContractIndexResponse result =
                marketApi.getContractIndex(symbol);
        return JSON.toJSONString(result);
    }

    //获取合约最高限价和最低限价
    public String getPriceLimit(String symbol,String contractType, String contractCode) throws ApiException {
        ContractOpenInterestResponse result =
                marketApi.getContractOpenInterest(symbol, contractType, contractCode);
        return JSON.toJSONString(result);
    }
    //获取当前可用合约总持仓量
    public String getOpenInterest(String symbol,String contractType, String contractCode) throws ApiException {
        ContractOpenInterestResponse result =
                marketApi.getContractOpenInterest(symbol, contractType, contractCode);
        return JSON.toJSONString(result);
    }
    /**
     * 获取行情深度数据
     * symbol	string	true	如"BTC_CW"表示BTC当周合约，"BTC_NW"表示BTC次周合约，"BTC_CQ"表示BTC季度合约
     * type	string	true	获得150档深度数据，使用step0, step1, step2, step3, step4, step5（step1至step5是进行了深度合并后的深度），
     * 使用step0时，不合并深度获取150档数据;获得20档深度数据，使用 step6, step7, step8, step9, step10, step11（step7至step11是进行了深度合并后的深度），
     * 使用step6时，不合并深度获取20档数据
     */
    public String getMarketDepth(String symbol,String type) throws ApiException {
        MarketDepthResponse result =
                marketApi.getMarketDepth(symbol,type);
        return JSON.toJSONString(result);
    }

    /**
     * 获取K线数据
     * symbol	true	string	合约名称		如"BTC_CW"表示BTC当周合约，"BTC_NW"表示BTC次周合约，"BTC_CQ"表示BTC季度合约
     * period	true	string	K线类型		1min, 5min, 15min, 30min, 60min,4hour,1day, 1mon
     * size	true	integer	获取数量	150	[1,2000]
     * from	false	integer	开始时间戳 10位 单位S
     * to	false	integer	结束时间戳 10位 单位S
     */
    public String getMarketHistoryKline(String symbol,String period,int size) throws ApiException {
        MarketHistoryKlineResponse result =
                marketApi.getMarketHistoryKline(symbol,period,size);
        return JSON.toJSONString(result);
    }
    /**
     * 获取聚合行情
     */
    public String getMarketDetailMerged(String symbol) throws ApiException {
        MarketDetailMergedResponse result =
                marketApi.getMarketDetailMerged(symbol);
        return JSON.toJSONString(result);
    }
    /**
     * 批量获取最近的交易记录
     * symbol	true	string	合约名称		如"BTC_CW"表示BTC当周合约，"BTC_NW"表示BTC次周合约，"BTC_CQ"表示BTC季度合约
     * size	true	number	获取交易记录的数量	1	[1, 2000]
     */
    public String getMarketHistoryTrade(String symbol,int size) throws ApiException {
        MarketHistoryTradeResponse result =
                marketApi.getMarketHistoryTrade(symbol, size);
        return JSON.toJSONString(result);
    }


    /**
     * 获取用户账户信息
     * @param symbol string	品种代码 "BTC","ETH"...如果缺省，默认返回所有品种
     */
    public String getContractAccountInfo(String symbol) throws ApiException {
        ContractAccountInfoResponse result = accountApi.getContractAccountInfo(symbol);
        return JSON.toJSONString(result);
    }

    /**
     * 获取用户持仓信息
     * @param symbol string
     */
    public String getContractPositionInfo(String symbol) throws ApiException {
        ContractPositionInfoResponse result = accountApi.getContractPositionInfo(symbol);
        return JSON.toJSONString(result);
    }

    /**
     * 查询用户账户和持仓信息
     * post api/v1/contract_account_position_info
     */
    public String getContractAccountPositionInfo(String symbol) throws ApiException {
        ContractAccountPositionInfoResponse result = accountApi.getContractAccountPositionInfo(symbol);
        return JSON.toJSONString(result);
    }

    /**
     * 合约下单
     * 参数名	参数类型	必填	描述
     * symbol	string	true	"BTC","ETH"...
     * contractType	string	true	合约类型 ("this_week":当周 "next_week":下周 "quarter":季度)
     * contractCode	string	true	BTC180914
     * clientOrderId	long	false	客户自己填写和维护，必须为数字
     * price	decimal	false	价格
     * volume	long	true	委托数量(张)
     * direction	string	true	"buy":买 "sell":卖
     * offset	string	true	"open":开 "close":平
     * leverRate	int	true	杠杆倍数[“开仓”若有10倍多单，就不能再下20倍多单]
     * orderPriceType	string	true    订单报价类型 "limit":限价 "opponent":对手价 "post_only":只做maker单,
       post only下单只受用户持仓数量限制,optimal_5：最优5档、optimal_10：最优10档、optimal_20：最优20档，ioc:IOC订单，
       fok：FOK订单, "opponent_ioc"： 对手价-IOC下单，"optimal_5_ioc"：最优5档-IOC下单，"optimal_10_ioc"：最优10档-IOC下单，
       "optimal_20_ioc"：最优20档-IOC下单,"opponent_fok"： 对手价-FOK下单，"optimal_5_fok"：最优5档-FOK下单，"optimal_10_fok"：
        最优10档-FOK下单，"optimal_20_fok"：最优20档-FOK下单
        备注：
        如果contract_code填了值，那就按照contract_code去下单，如果contract_code没有填值，则按照symbol+contract_type去下单。
        对手价下单price价格参数不用传，对手价下单价格是买一和卖一价,optimal_5：最优5档、optimal_10：
        最优10档、optimal_20：最优20档下单price价格参数不用传，"limit":限价，"post_only":只做maker单 需要传价格，"fok"：全部成交或立即取消，"ioc":立即成交并取消剩余。
        Post only(也叫maker only订单，只下maker单)每个周期合约的开仓/平仓的下单数量限制为500000，同时也会受到用户持仓数量限制。
        开平方向
        开多：买入开多(direction用buy、offset用open)
        平多：卖出平多(direction用sell、offset用close)
        开空：卖出开空(direction用sell、offset用open)
        平空：买入平空(direction用buy、offset用close)
     */
    public String futureContractOrder(ContractOrderRequest order) throws ApiException {
        ContractOrderResponse result =
                tradeApi.contractOrderRequest(order);
        return JSON.toJSONString(result);
    }
    /**
     * 批量下单
     * List<Order> orders = new ArrayList();
     * Order order1 = new Order("BTC", "this_week", "BTC181110", "10", "6400", "1", "buy", "open", "10", "limit");
     */
    public String futureContractBatchorder(List<ContractOrderRequest> orders) throws IOException, HttpException {
        String contractBatchorder = "";
//        contractBatchorder = futurePostV1.futureContractBatchorder(orders);
        return contractBatchorder;
    }

    /**
     * 合约取消订单
     * POST api/v1/contract_cancel
     * 参数名称	是否必须	类型	描述
     * order_id	false	string	订单ID(多个订单ID中间以","分隔,一次最多允许撤消10个订单)
     * clientOrderId	false	string	客户订单ID(多个订单ID中间以","分隔,一次最多允许撤消10个订单)
     * symbol	true	string	"BTC","ETH"...
     */
    public String futureContractCancel(String orderId, String clientOrderId, String symbol) throws ApiException {
        ContractCancelRequest request = ContractCancelRequest.builder()
                .symbol(symbol)
                .orderId(orderId)
                .clientOrderId(clientOrderId)
                .build();
        ContractCancelResponse result =
                tradeApi.contractCancelRequest(request);
        return JSON.toJSONString(result);
    }
    /**
     * 合约全部撤单
     * POST api/v1/contract_cancelall
     * 请求参数
     * 参数名称	是否必须	类型	描述
     * symbol	true	string	品种代码，如"BTC","ETH"...
     * contract_code	false	string	合约code
     * contract_type	false	string	合约类型
     */
    public String futureContractCancelall(String symbol) throws IOException, HttpException {
        ContractCancelallRequest request = ContractCancelallRequest.builder()
                .symbol(symbol)
                .build();
        ContractCancelallResponse result =
                tradeApi.contractCancelallRequest(request);
        return JSON.toJSONString(result);
    }

    /**
     * 获取合约订单信息
     * 参数名称 是否必须    类型	描述
     * order_id	请看备注	string	订单ID(多个订单ID中间以","分隔,一次最多允许查询50个订单)
     * client_order_id	请看备注	string	客户订单ID(多个订单ID中间以","分隔,一次最多允许查询50个订单)
     * symbol	true	string	"BTC","ETH"...
     */
    public String getcontractOrderInfo(String orderId, String clientOrderId, String symbol) throws ApiException {
        ContractOrderInfoRequest request = ContractOrderInfoRequest.builder()
                .symbol(symbol)
                .orderId(orderId)
                .clientOrderId(clientOrderId)
                .build();
        ContractOrderInfoResponse result =
                tradeApi.contractOrderInfoRequest(request);
        return JSON.toJSONString(result);
    }

    /**
     * 获取历史成交记录
     * symbol	true	string	品种代码		支持大小写,"BTC","ETH"...
     * type 1:所有订单,2:结束状态的订单
     * trade_type	true	int	交易类型		0:全部,1:买入开多,2: 卖出开空,3: 买入平空,4: 卖出平多,5: 卖出强平,6: 买入强平
     * create_date	true	int	日期		可随意输入正整数，如果参数超过90则默认查询90天的数据
     * contract_code	false	string	合约code
     * page_index	false	int	页码，不填默认第1页	1
     * page_size	false	int	不填默认20，不得多于50	20	[1-50]
     */
    public String contractMatchresultsRequest(String symbol,int type,int tradeType, int createDate) {
        ContractMatchresultsRequest request = ContractMatchresultsRequest.builder()
                .symbol(symbol)
                .type(type)
                .tradeType(tradeType)
                .createDate(createDate)
                .pageSize(50)
                .build();
        ContractMatchresultsResponse result =
                tradeApi.contractMatchresultsRequest(request);
        return JSON.toJSONString(result);
    }
}
