package com.contract.harvest.service;

import com.contract.harvest.common.OpenInfo;

public interface DataService {
    /**
     * 获取开仓信息
     */
    OpenInfo getOpenInfo(String symbol);
}
