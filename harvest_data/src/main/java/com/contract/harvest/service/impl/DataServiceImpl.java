package com.contract.harvest.service.impl;

import com.alibaba.fastjson.JSON;
import com.contract.harvest.common.CacheKey;
import com.contract.harvest.common.OpenInfo;
import com.contract.harvest.service.DataService;
import com.contract.harvest.service.RedisService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class DataServiceImpl implements DataService {

    @Resource
    private RedisService redisService;

    @Override
    public OpenInfo getOpenInfo(String symbol) {
        String openInfo = redisService.hashGet(CacheKey.HUOBI_OPEN_INFO,symbol);
        return JSON.parseObject(openInfo, OpenInfo.class);
    }
}
