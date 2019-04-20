package com.cz.gameplat.live.work.config;

import org.springframework.beans.factory.annotation.*;
import org.springframework.cache.interceptor.*;
import org.springframework.cache.*;
import java.util.*;

public class RedisCacheResolver implements CacheResolver
{
    @Autowired
    private CacheManager cacheManager;
    
    public Collection<? extends Cache> resolveCaches(final CacheOperationInvocationContext<?> context) {
        final List<Cache> caches = new ArrayList<Cache>();
        for (final String cacheName : context.getOperation().getCacheNames()) {
            final Cache cache = this.cacheManager.getCache(cacheName);
            caches.add(cache);
        }
        return caches;
    }
}
