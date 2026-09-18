package com.neovision.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configura el caché en memoria con Caffeine.
 * Hoy solo espacios y salones están cacheados (10 minutos, máx. 500 entradas) —
 * son catálogos pequeños de solo-admin que cambian poco. Docentes, estudiantes
 * y sesiones activas se consultan directo a BD sin caché.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_ESPACIOS = "espacios";
    public static final String CACHE_SALONES = "salones";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                CACHE_ESPACIOS,
                CACHE_SALONES
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats());
        return manager;
    }
}
