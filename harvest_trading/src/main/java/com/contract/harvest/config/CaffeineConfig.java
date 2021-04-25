package com.contract.harvest.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CaffeineConfig {

    public enum CacheEnum {
        KLINE_HOUR_CACHE(1, 20000, 50),
        KLINE_MIN_CACHE(1, 20000, 50);
        private final int minute;
        private final long maxSize;
        private final int initSize;
        CacheEnum(int minute, long maxSize, int initSize) {
            this.minute = minute;
            this.maxSize = maxSize;
            this.initSize = initSize;
        }
    }

    @Bean("caffeineCacheManager")
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        ArrayList<CaffeineCache> caffeineCaches = new ArrayList<>();
        for (CacheEnum cacheEnum : CacheEnum.values()) {
            caffeineCaches.add(new CaffeineCache(cacheEnum.name(),
                    Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(cacheEnum.minute))
                            .initialCapacity(cacheEnum.initSize)
                            .maximumSize(cacheEnum.maxSize).build()));
        }
        cacheManager.setCaches(caffeineCaches);
        return cacheManager;
    }
}
