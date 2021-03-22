package com.contract.harvest.view;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.contract.harvest.service.CacheService;
import com.contract.harvest.service.RedisService;
import com.contract.harvest.service.ScheduledService;
import com.huobi.api.response.trade.ContractMatchresultsResponse;
import org.springframework.data.redis.core.Cursor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class ContractInfo {

    @Resource
    private RedisService redisService;
    @Resource
    private ScheduledService scheduledService;

    public List<List<ContractMatchresultsResponse.DataBean.TradesBean>> getContractList(String[] orderKeyArr) {
        List<List<ContractMatchresultsResponse.DataBean.TradesBean>> contractList = new ArrayList<>();
        int count = 1000;
        for (String key : orderKeyArr) {
            Cursor<Map.Entry<String, String>> cursor = redisService.hashScan(key,count);
            List<ContractMatchresultsResponse.DataBean.TradesBean> rList = new ArrayList<>();
            cursor.forEachRemaining(v -> rList.add(JSON.parseObject(v.getValue(),ContractMatchresultsResponse.DataBean.TradesBean.class)));
            contractList.add(rList);
        }
        return contractList;
    }

    public Map<String,JSONObject> getSpaceInfo(String[] keys) {
        Map<String,JSONObject> spaceInfo = new HashMap<>();
        for (String key : keys) {
            String sInfo = redisService.hashGet(CacheService.SPACE_INFO, key);
            spaceInfo.put(key,JSON.parseObject(sInfo));
        }
        return spaceInfo;
    }
    public Set<String> getSymbols() {
        return scheduledService.getSymbol();
    }
}
