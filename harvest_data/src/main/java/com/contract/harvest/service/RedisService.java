package com.contract.harvest.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RedisService {
    @Resource
    private SetOperations<String, String> setOperations;
    @Resource
    private HashOperations<String,String,Object> hashOperations;
    /**
     * 获取所有的set值
     */
    public Set<String> getSetMembers(String key) {
        return setOperations.members(key.toUpperCase());
    }
    /**
     * hash set
     */
    public void hashSet(String key,String key1,Object value) {
        hashOperations.put(key.toUpperCase(),key1.toUpperCase(),value);
    }
    /**
     * hash get
     */
    public String hashGet(String key,String key1) {
        Object value = hashOperations.get(key.toUpperCase(),key1.toUpperCase());
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }
}