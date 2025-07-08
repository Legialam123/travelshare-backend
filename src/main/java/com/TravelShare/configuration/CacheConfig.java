package com.TravelShare.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("exchange-rates");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)                              // Tối đa 1000 tỷ giá
                .expireAfterWrite(Duration.ofMinutes(30))       // Cache 30 phút
                .recordStats());                                // Theo dõi hiệu suất
        return cacheManager;
    }
}
