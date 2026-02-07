package org.example.kcacheservice.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.kcacheservice.config.CacheConfig;
import org.example.kcacheservice.dto.ApiResponseEnvelop;
import org.example.kcacheservice.dto.CacheDTO;
import org.example.kcacheservice.exception.CacheException;
import org.example.kcacheservice.service.CachePersistenceService;
import org.example.kcacheservice.service.CacheService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service("CacheServiceV2")
@Slf4j
public class CacheServiceV2Impl implements CacheService {

    private final Map<String, CacheDTO> cache;
    private final CachePersistenceService cachePersistenceService;
    private final CacheConfig cacheConfig;
    private final ReentrantReadWriteLock lock;

    public CacheServiceV2Impl(CachePersistenceService cachePersistenceService,
                              CacheConfig cacheConfig) {
        this.cache = new LinkedHashMap<>(cacheConfig.getMaxSize(), 0.75f, true);
        this.cachePersistenceService = cachePersistenceService;
        this.cacheConfig = cacheConfig;
        this.lock = new ReentrantReadWriteLock();
    }

    @PostConstruct
    public void init() {
        log.debug("Initializing cache service");
        try {
            this.cachePersistenceService.initPersistenceStore();
        } catch(Exception e) {
            log.error("Error during cache service initialization", e);
            throw new CacheException("Failed to initialize cache service");
        }
    }

    @Override
    public ApiResponseEnvelop<CacheDTO> add(String key, String value) {
        log.debug("Adding cache entry - Key: {}, Value: {}", key, value);
        /*
         * Cache Logic - mirror behavior, high read/write but high on memory usage
         * 1. Lock
         * 2. Delete new record from persistent store if exists, enqueue key for db operation
         * 3. Check size of cache(map)
         * 4. if max, then remove last node from cache(linked HashMap)
         * 5. Save removed last record to persistent store, enqueue key for db operation
         * 6. Add new record to cache
         * 7. Release lock
         * 8. return success response
         */
        try {
            CacheDTO record = CacheDTO.builder()
                    .id(key)
                    .value(value)
                    .build();
            lock.writeLock().lock();
            log.trace("Acquired write lock");
            this.cachePersistenceService.removeFromStore(key);
            if(this.cache.size() >= this.cacheConfig.getMaxSize()) {
                this.evictLeastUsedAndPersist();
            }
            this.cache.put(key, record);
            return ApiResponseEnvelop.success(CacheDTO.builder().id(key).value(value).build());
        } catch(CacheException e) {
            throw e;
        } catch(Exception e) {
            log.error("Error adding cache entry", e);
            throw new CacheException("Failed to add cache entry");
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ApiResponseEnvelop<CacheDTO> fetch(String key) {
        log.debug("Fetching cache entry - Key: {}", key);
        /*
            * Cache Logic - mirror behavior, high read/write but high on memory usage
            * 1. Lock
            * 2. Check record in cache, if found return
            * 3. Check record in persistent store, if found add to cache, enqueue key for db operation and return
            * 4. Release lock
            * 5. return not found response
         */
        try {
            lock.writeLock().lock();
            if(this.cache.containsKey(key)) {
                CacheDTO record = this.cache.get(key);
                return ApiResponseEnvelop.success(record);
            } else {
                Optional<CacheDTO> optRecord = this.cachePersistenceService.getFromStore(key);
                if(optRecord.isPresent()) {
                    CacheDTO record = optRecord.get();
                    if(this.cache.size() >= this.cacheConfig.getMaxSize()) {
                        this.evictLeastUsedAndPersist();
                    }
                    this.cache.put(key, record);
                    this.cachePersistenceService.removeFromStore(key);
                    return ApiResponseEnvelop.success(record);
                } else {
                    return ApiResponseEnvelop.error(new ArrayList<>(){{
                        add("Record not found for key: " + key);
                    }});
                }
            }
        } catch(CacheException e) {
            throw e;
        } catch(Exception e) {
            log.error("Error fetching cache entry", e);
            throw new CacheException("Failed to fetch cache entry");
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ApiResponseEnvelop<String> remove(String key) {
        log.debug("Removing cache entry - Key: {}", key);
        /*
         * Cache Logic - mirror behavior, high read/write but high on memory usage
         * 1. Lock
         * 2. Remove record from cache if exists
         * 3. Remove record from persistent store, enqueue key for db operation
         * 4. Release lock
         * 5. return success response
         */
        try {
            lock.writeLock().lock();
            if(this.cache.containsKey(key)) {
                this.cache.remove(key);
            } else {
                this.cachePersistenceService.removeFromStore(key);
            }
            return ApiResponseEnvelop.success("OK");
        } catch(CacheException e) {
            throw e;
        } catch(Exception e) {
            log.error("Error removing cache entry", e);
            throw new CacheException("Failed to remove cache entry");
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ApiResponseEnvelop<String> removeAll() {
        log.debug("Removing all cache entries");
        /*
         * Cache Logic - mirror behavior, high read/write but high on memory usage
         * 1. Lock
         * 2. Clear cache
         * 3. Clear persistent store, enqueue all keys for db operation
         * 4. Release lock
         * 5. return success response
         */
        try {
            lock.writeLock().lock();
            this.cache.clear();
            this.cachePersistenceService.removeAll();
            return ApiResponseEnvelop.success("OK");
        } catch(CacheException e) {
            throw e;
        } catch(Exception e) {
            log.error("Error removing all cache entries", e);
            throw new CacheException("Failed to remove all cache entries");
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ApiResponseEnvelop<String> clear() {
        log.debug("Clearing cache and persistent store");
        /*
         * Cache Logic - mirror behavior, high read/write but high on memory usage
         * 1. Lock
         * 2. Clear cache
         * 4. Release lock
         * 5. return success response
         */
        try {
            lock.writeLock().lock();
            this.cache.clear();
            return ApiResponseEnvelop.success("OK");
        } catch(CacheException e) {
            throw e;
        } catch(Exception e) {
            log.error("Error clearing cache and persistent store", e);
            throw new CacheException("Failed to clear cache and persistent store");
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void evictLeastUsedAndPersist() {
        log.debug("Evicting least used cache entry and persisting to DB");
        if(this.cache.isEmpty()) return;

        Iterator<Map.Entry<String, CacheDTO>> it = this.cache.entrySet().iterator();
        if(it.hasNext()) {
            CacheDTO record = it.next().getValue();
            it.remove();
            this.cachePersistenceService.persistToStore(record);
        }
    }
}
