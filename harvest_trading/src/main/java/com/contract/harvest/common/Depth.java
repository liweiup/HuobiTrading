package com.contract.harvest.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class Depth {
    //消息生成时间，单位：毫秒
    private long ts;
    //数据所属的 channel
    private String ch;

    private TickBean tick;
    @Data
    @AllArgsConstructor
    public static class TickBean {
        //买盘,[price(挂单价), vol(此价格挂单张数)], //按price降序
        private List<List<BigDecimal>> bids;
        //卖盘,[price(挂单价), vol(此价格挂单张数)]  //按price升序
        private List<List<BigDecimal>> asks;
        //mrid 订单ID
        private long mrid;
        private long ts;
        private Integer version;
    }
}
