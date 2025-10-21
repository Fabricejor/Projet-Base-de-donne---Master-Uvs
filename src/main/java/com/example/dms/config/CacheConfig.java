package com.example.dms.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configuration du gestionnaire de cache en mémoire
     * 
     * Caches configurés:
     * - ventes : Cache pour les résultats de findAllFromAllRegions()
     * - statistiques : Cache pour les statistiques système
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("ventes"),
            new ConcurrentMapCache("statistiques"),
            new ConcurrentMapCache("ventesByRegion")
        ));
        return cacheManager;
    }
}

