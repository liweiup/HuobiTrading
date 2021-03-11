package com.contract.harvest.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;

@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableCaching
public class RedisConfig {

    @Bean
    public Jackson2JsonRedisSerializer<Object> jackson2JsonSerializer() {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(
                Object.class);
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build();
        // 初始化objectmapper
        ObjectMapper mapper = JsonMapper.builder()
                .serializationInclusion(JsonInclude.Include.NON_EMPTY)
                .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL)
                .build();
        jackson2JsonRedisSerializer.setObjectMapper(mapper);
        return jackson2JsonRedisSerializer;
    }

    @Bean
    public RedisTemplate<String,Object> redisTemplate(LettuceConnectionFactory redisConnectionFactory,Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置key/hashkey序列化
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        // 设置值序列化
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.setConnectionFactory(redisConnectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public SetOperations<String, String> opsForSetString(RedisTemplate<String, String> redisTemplate) {
        return redisTemplate.opsForSet();
    }

    @Bean
    public HashOperations<String,String,Object> hashOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForHash();
    }

    /**
     * 缓存配置
     * @return
     */
    @Bean
    public RedisCacheConfiguration publicRedisCacheConfiguration(Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(jackson2JsonRedisSerializer));
    }

    @Primary
    @Bean
    public RedisCacheManager huobiEntityRedisCacheManager(RedisCacheConfiguration publicRedisCacheConfiguration,LettuceConnectionFactory factory) {
        publicRedisCacheConfiguration = publicRedisCacheConfiguration.entryTtl(Duration.ofHours(1));
        return RedisCacheManager.builder(factory).cacheDefaults(publicRedisCacheConfiguration).build();
    }

    @Bean
    public RedisCacheManager huobiEntityHisbasisRedisCacheManager(RedisCacheConfiguration publicRedisCacheConfiguration,LettuceConnectionFactory factory) {
        publicRedisCacheConfiguration = publicRedisCacheConfiguration.entryTtl(Duration.ofHours(2));
        return RedisCacheManager.builder(factory).cacheDefaults(publicRedisCacheConfiguration).build();
    }

    @Bean
    public RedisCacheManager huobiOrderHandleRedisCacheManager(RedisCacheConfiguration publicRedisCacheConfiguration,LettuceConnectionFactory factory) {
        publicRedisCacheConfiguration = publicRedisCacheConfiguration.entryTtl(Duration.ofMinutes(3));
        return RedisCacheManager.builder(factory).cacheDefaults(publicRedisCacheConfiguration).build();
    }

}
