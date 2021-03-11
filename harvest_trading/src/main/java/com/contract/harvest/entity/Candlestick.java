package com.contract.harvest.entity;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class Candlestick {

    private String ch;
    private String status;
    private Long ts;
    private DataBean tick;
    private List<DataBean> data;
    @Data
    @AllArgsConstructor
    public static class DataBean {
        private Long id;
        private BigDecimal amount;
        private BigDecimal close;
        private BigDecimal count;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal open;
        private BigDecimal vol;
    }
}
