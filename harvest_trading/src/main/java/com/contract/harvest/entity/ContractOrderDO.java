package com.contract.harvest.entity;

import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * The table 交易订单表
 */
@Repository
public class ContractOrderDO {

    /**
     * id 主键.
     */
    private Integer id;
    /**
     * symbol 币种
     */
    private String symbol;

    /**
     * 合约代码
     */
    private String contract_code;
    /**
     * fee 手续费.
     */
    private Double fee;
    /**
     * zTs 时间戳.
     */
    private Long zTs;
    /**
     * profit 收益.
     */
    private Double profit;
    /**
     * tradeVolume 成交数量.
     */
    private Long tradeVolume;
    /**
     * tradeAvgPrice 成交均价.
     */
    private Double tradeAvgPrice;
    /**
     * tradeTurnover 成交总金额.
     */
    private Double tradeTurnover;
    /**
     * price 价格.
     */
    private Double price;

    /**
     * orderId 订单id.
     */
    private String orderId;
    /**
     * zOffset "open":开 "close":平.
     */
    private String zOffset;
    /**
     * direction "buy":买 "sell":卖.
     */
    private String direction;
    /**
     * contractType 合约类型 ("this_week":当周 "next_week":下周 "quarter":季度).
     */
    private String contractType;
    /**
     * orderPriceType 订单报价类型 订单报价类型 "limit":限价 "opponent":对手价 "post_only":只做maker单,post only下单只受用户持仓数量限制,optimal_5：最优5档、optimal_10：最优10档、optimal_20：最优20档，ioc:IOC订单，fok：FOK订单.
     */
    private String orderPriceType;
    /**
     * orderType 1:报单 、 2:撤单 、 3:强平、4:交割.
     */
    private Integer orderType;
    /**
     * volume 委托数量(张).
     */
    private Integer volume;
    /**
     * leverRate 杠杆倍数.
     */
    private Integer leverRate;
    /**
     * orderStatus (1准备提交 2准备提交 3已提交 4部分成交 5部分成交已撤单 6全部成交 7已撤单 11撤单中).
     */
    private Integer orderStatus;
    /**
     * createTime 创建时间.
     */
    private String createTime;
    /**
     * updateTime 修改时间.
     */
    private String updateTime = "0000-00-00 00:00:00";

    /**
     * Set id 主键.
     */
    public void setId(Integer id){
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getContract_code() {
        return contract_code;
    }

    public void setContract_code(String contract_code) {
        this.contract_code = contract_code;
    }

    /**
     * Get id 主键.
     *
     * @return the string
     */
    public Integer getId(){
        return id;
    }

    /**
     * Set fee 手续费.
     */
    public void setFee(Double fee){
        this.fee = fee;
    }

    /**
     * Get fee 手续费.
     *
     * @return the string
     */
    public Double getFee(){
        return fee;
    }

    /**
     * Set zTs 时间戳.
     */
    public void setZTs(Long zTs){
        this.zTs = zTs;
    }

    /**
     * Get zTs 时间戳.
     *
     * @return the string
     */
    public Long getZTs(){
        return zTs;
    }

    /**
     * Set profit 收益.
     */
    public void setProfit(Double profit){
        this.profit = profit;
    }

    /**
     * Get profit 收益.
     *
     * @return the string
     */
    public Double getProfit(){
        return profit;
    }

    /**
     * Set tradeVolume 成交数量.
     */
    public void setTradeVolume(Long tradeVolume){
        this.tradeVolume = tradeVolume;
    }

    /**
     * Get tradeVolume 成交数量.
     *
     * @return the string
     */
    public Long getTradeVolume(){
        return tradeVolume;
    }

    /**
     * Set tradeAvgPrice 成交均价.
     */
    public void setTradeAvgPrice(Double tradeAvgPrice){
        this.tradeAvgPrice = tradeAvgPrice;
    }

    /**
     * Get tradeAvgPrice 成交均价.
     *
     * @return the string
     */
    public Double getTradeAvgPrice(){
        return tradeAvgPrice;
    }

    /**
     * Set tradeTurnover 成交总金额.
     */
    public void setTradeTurnover(Double tradeTurnover){
        this.tradeTurnover = tradeTurnover;
    }

    /**
     * Get tradeTurnover 成交总金额.
     *
     * @return the string
     */
    public Double getTradeTurnover(){
        return tradeTurnover;
    }

    /**
     * Set price 价格.
     */
    public void setPrice(Double price){
        this.price = price;
    }

    /**
     * Get price 价格.
     *
     * @return the string
     */
    public Double getPrice(){
        return price;
    }

    /**
     * Set orderId 订单id.
     */
    public void setOrderId(String orderId){
        this.orderId = orderId;
    }

    /**
     * Get orderId 订单id.
     *
     * @return the string
     */
    public String getOrderId(){
        return orderId;
    }

    /**
     * Set zOffset "open":开 "close":平.
     */
    public void setZOffset(String zOffset){
        this.zOffset = zOffset;
    }

    /**
     * Get zOffset "open":开 "close":平.
     *
     * @return the string
     */
    public String getZOffset(){
        return zOffset;
    }

    /**
     * Set direction "buy":买 "sell":卖.
     */
    public void setDirection(String direction){
        this.direction = direction;
    }

    /**
     * Get direction "buy":买 "sell":卖.
     *
     * @return the string
     */
    public String getDirection(){
        return direction;
    }

    /**
     * Set contractType 合约类型 ("this_week":当周 "next_week":下周 "quarter":季度).
     */
    public void setContractType(String contractType){
        this.contractType = contractType;
    }

    /**
     * Get contractType 合约类型 ("this_week":当周 "next_week":下周 "quarter":季度).
     *
     * @return the string
     */
    public String getContractType(){
        return contractType;
    }

    /**
     * Set orderPriceType 订单报价类型 订单报价类型 "limit":限价 "opponent":对手价 "post_only":只做maker单,post only下单只受用户持仓数量限制,optimal_5：最优5档、optimal_10：最优10档、optimal_20：最优20档，ioc:IOC订单，fok：FOK订单.
     */
    public void setOrderPriceType(String orderPriceType){
        this.orderPriceType = orderPriceType;
    }

    /**
     * Get orderPriceType 订单报价类型 订单报价类型 "limit":限价 "opponent":对手价 "post_only":只做maker单,post only下单只受用户持仓数量限制,optimal_5：最优5档、optimal_10：最优10档、optimal_20：最优20档，ioc:IOC订单，fok：FOK订单.
     *
     * @return the string
     */
    public String getOrderPriceType(){
        return orderPriceType;
    }

    /**
     * Set orderType 1:报单 、 2:撤单 、 3:强平、4:交割.
     */
    public void setOrderType(Integer orderType){
        this.orderType = orderType;
    }

    /**
     * Get orderType 1:报单 、 2:撤单 、 3:强平、4:交割.
     *
     * @return the string
     */
    public Integer getOrderType(){
        return orderType;
    }

    /**
     * Set volume 委托数量(张).
     */
    public void setVolume(Integer volume){
        this.volume = volume;
    }

    /**
     * Get volume 委托数量(张).
     *
     * @return the string
     */
    public Integer getVolume(){
        return volume;
    }

    /**
     * Set leverRate 杠杆倍数.
     */
    public void setLeverRate(Integer leverRate){
        this.leverRate = leverRate;
    }

    /**
     * Get leverRate 杠杆倍数.
     *
     * @return the string
     */
    public Integer getLeverRate(){
        return leverRate;
    }

    /**
     * Set orderStatus (1准备提交 2准备提交 3已提交 4部分成交 5部分成交已撤单 6全部成交 7已撤单 11撤单中).
     */
    public void setOrderStatus(Integer orderStatus){
        this.orderStatus = orderStatus;
    }

    /**
     * Get orderStatus (1准备提交 2准备提交 3已提交 4部分成交 5部分成交已撤单 6全部成交 7已撤单 11撤单中).
     *
     * @return the string
     */
    public Integer getOrderStatus(){
        return orderStatus;
    }

    /**
     * Set createTime 创建时间.
     */
    public void setCreateTime(String createTime){
        this.createTime = createTime;
    }

    /**
     * Get createTime 创建时间.
     *
     * @return the string
     */
    public String getCreateTime(){
        return createTime;
    }

    /**
     * Set updateTime 修改时间.
     */
    public void setUpdateTime(String updateTime){
        this.updateTime = updateTime;
    }

    /**
     * Get updateTime 修改时间.
     *
     * @return the string
     */
    public String getUpdateTime(){
        return updateTime;
    }

    @Override
    public String toString() {
        return "ContractOrderDO{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", contract_code='" + contract_code + '\'' +
                ", fee=" + fee +
                ", zTs=" + zTs +
                ", profit=" + profit +
                ", tradeVolume=" + tradeVolume +
                ", tradeAvgPrice=" + tradeAvgPrice +
                ", tradeTurnover=" + tradeTurnover +
                ", price=" + price +
                ", orderId='" + orderId + '\'' +
                ", zOffset='" + zOffset + '\'' +
                ", direction='" + direction + '\'' +
                ", contractType='" + contractType + '\'' +
                ", orderPriceType='" + orderPriceType + '\'' +
                ", orderType=" + orderType +
                ", volume=" + volume +
                ", leverRate=" + leverRate +
                ", orderStatus=" + orderStatus +
                ", createTime='" + createTime + '\'' +
                ", updateTime='" + updateTime + '\'' +
                '}';
    }
}
