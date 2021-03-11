package com.contract.harvest.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
public class CacheConfig {
    @Bean("universalGenerator")
    public KeyGenerator universalGenerator() {
        return (target, method, params) -> method.getName();
    }
    @Bean("HuobiEntity_keyGenerator")
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            Date d = new Date();
            SimpleDateFormat time = new SimpleDateFormat("yyyyMMdd");
            return method.getName()+time.format(d);
        };
    }

    @Bean("HuobiEntity_hisbasisKeyGenerator")
    public KeyGenerator hisbasiskeyGenerator() {
        return (target, method, params) -> {
            Date d = new Date();
            SimpleDateFormat time = new SimpleDateFormat("yyyyMMdd");
            return method.getName()+time.format(d);
        };
    }
}
