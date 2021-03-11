package com.contract.harvest.socketService.impl;

import com.alibaba.fastjson.JSONObject;
import com.contract.harvest.common.CacheKey;
import com.contract.harvest.service.RedisService;
import com.contract.harvest.socketService.HuoBiProWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


/**
 * 其它服务拉取交易对消息 和调其它服务保存消息
 */
@Service
@Slf4j
public class HuoBiProWebSocketServiceImpl implements HuoBiProWebSocketService {

    @Resource
    private RedisService redisService;

    @Override
    public void onReceive(String msg) {
        // 直接发送消息给中转服务， 中转服务来处理信息
        if (StringUtils.isBlank(msg)) {
            log.error("====onReceive-huobi==msg is null");
            return;
        }
        JSONObject msgObj = JSONObject.parseObject(msg);
        String key = msgObj.getString("ch");
        if (key != null) {
            redisService.hashSet(CacheKey.HUOBI_SUB,key.toUpperCase(),msg);
//            log.info("火币网数据:{}", msg);
        }
    }
}
