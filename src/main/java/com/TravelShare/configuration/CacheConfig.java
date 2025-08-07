package com.TravelShare.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)                          // Tối đa 1000 entries
                .expireAfterWrite(24, TimeUnit.HOURS)       // Hết hạn sau 24 giờ
                .recordStats());                            // Ghi lại thống kê
        
        // Đặt tên cache
        cacheManager.setCacheNames(java.util.Arrays.asList("ocr-cache", "general-cache", "exchange-rates"));
        
        return cacheManager;
    }
    
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(500)                           // Cache cho OCR
                .expireAfterWrite(12, TimeUnit.HOURS)       // Hết hạn sau 12 giờ
                .recordStats();
    }
}
