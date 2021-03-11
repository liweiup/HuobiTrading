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

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    /**
     * 默认过期时长，单位：秒
     */
    public static final long DEFAULT_EXPIRE = 60 * 60 * 24;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ListOperations<String, String> listOperations;
    @Resource
    private SetOperations<String, String> setOperations;
    @Resource
    private HashOperations<String,String,String> hashOperations;
    /**
     * 不设置过期时长
     */
    public static final long NOT_EXPIRE = -1;

    /**
     * 获取一个key的值
     */
    public String getValue(String key) {
        return stringRedisTemplate.opsForValue().get(key.toUpperCase());
    }

    /**
     * 设置一个key的值
     */
    public void setValue(String key,String value) {
        stringRedisTemplate.opsForValue().set(key.toUpperCase(), value);
    }

    /**
     * 右边插入
     */
    public Long rightPush(String key,String value) {
        return listOperations.rightPush(key.toUpperCase(),value);
    }

    /**
     * 将一个值放入列表头部
     */
    public void lpush(String key, String value) {
        listOperations.leftPush(key.toUpperCase(),value);
    }
    /**
     * 左边取出
     */
    public String leftPop(String key) {
        return listOperations.leftPop(key.toUpperCase());
    }
    /**
     * 右边取出
     */
    public Object rightPop(String key) {
        return listOperations.rightPop(key.toUpperCase());
    }
    /**
     * 获取一个list的长度
     */
    public Long getListLen(String key) {
        return listOperations.size(key.toUpperCase());
    }
    /**
     * 索引获取列表中的元素
     */
    public String getListByIndex(String key,Long index) {
        return listOperations.index(key.toUpperCase(),index);
    }
    /**
     * 修剪列表
     */
    public void listTrim(String key,long start, long end) {
        listOperations.trim(key.toUpperCase(),start,end);
    }
    /**
     * 按区间获取列表值
     */
    public List<String> lrangeList(String key, long start, long end){
        return listOperations.range(key.toUpperCase(),start,end);
    }
    /**
     * add一个set值
     */
    public void addSet(String key, String value) {
        setOperations.add(key.toUpperCase(), value);
    }

    /**
     * 获取所有的set值
     */
    public Set<String> getSetMembers(String key) {
        return setOperations.members(key.toUpperCase());
    }

    /**
     * hash set
     */
    public void hashSet(String key,String key1,String value) {
        hashOperations.put(key.toUpperCase(),key1.toUpperCase(),value);
    }

    /**
     * hash get
     */
    public String hashGet(String key,String key1) {
        key = key.toUpperCase();
        key1 = key1.toUpperCase();
        Object value = hashOperations.get(key,key1);
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    /**
     * 订阅通知
     * @param channelFlag 频道
     * @param content 通知内容
     */
    public void convertAndSend(String channelFlag, String content) {
        stringRedisTemplate.convertAndSend(channelFlag,content);
    }

    /**
     * 删除key
     */
    public void deleteKey(String key) {
        redisTemplate.delete(key.toUpperCase());
    }

    /**
     * 删除多个key
     */
    public void deleteKey(String... keys) {
        Set<String> kSet = Stream.of(keys).map(k -> k).collect(Collectors.toSet());
        redisTemplate.delete(kSet);
    }

    /**
     * 删除Key的集合
     */
    public void deleteKey(Collection<String> keys) {
        Set<String> kSet = keys.stream().map(k -> k).collect(Collectors.toSet());
        redisTemplate.delete(kSet);
    }

    /**
     * 设置key的生命周期
     */
    public void expireKey(String key, long time, TimeUnit timeUnit) {
        redisTemplate.expire(key, time, timeUnit);
    }

    /**
     * 指定key在指定的日期过期
     */
    public void expireKeyAt(String key, Date date) {
        redisTemplate.expireAt(key, date);
    }

    /**
     * 将key设置为永久有效
     */
    public void persistKey(String key) {
        redisTemplate.persist(key);
    }
}