package service.impl;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.kcacheservice.config.CacheConfig;
import org.example.kcacheservice.dto.ApiResponseEnvelop;
import org.example.kcacheservice.dto.CacheDTO;
import org.example.kcacheservice.entity.CacheEntity;
import org.example.kcacheservice.repository.CacheRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import service.CacheService;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@Slf4j
public class CacheServiceImpl implements CacheService {

    private final Map<String, CacheDTO> cache;
    private final ReentrantReadWriteLock lock;
    private final CacheRepository cacheRepository;
    private final CacheConfig cacheConfig;

    public CacheServiceImpl(CacheConfig cacheConfig, CacheRepository cacheRepository) {
        this.cache = new LinkedHashMap<>(cacheConfig.getMaxSize(), 0.75f, true);
        this.lock = new ReentrantReadWriteLock();
        this.cacheRepository = cacheRepository;
        this.cacheConfig = cacheConfig;
    }

    @Override
    @Transactional
    public ApiResponseEnvelop<CacheDTO> add(String key, String value) {
        log.debug("Adding record to cache");
        /*
         * Cache Logic - sync behavior, might have impact on performance
         * 1. Lock
         * 2. Delete new record from db if exists
         * 3. Check size of cache(map)
         * 4. if max, then remove last node from cache(linked HashMap)
         * 5. Save removed last record to db
         * 6. Add new record to cache
         * 7. Release lock
         * 8. return success response
         */
        try {
            //Step 1
            lock.writeLock().lock();
            log.trace("Acquired write lock");

            //Step 2
            try {
                log.trace("Attempting to delete record with key {} from DB", key);
                this.cacheRepository.deleteById(key);
                log.debug("Record with key {} deleted from DB", key);
            } catch(EmptyResultDataAccessException e) {
                log.trace("Record with key {} not found in DB, proceeding to add", key);
            }

            //Step 4 & 5
            if(this.cache.size() >= this.cacheConfig.getMaxSize()) {
                log.debug("Cache size {} has reached max limit {}", this.cache.size(), this.cacheConfig.getMaxSize());
                evictLeastUsedAndPersist();
            }

            //Step 6
            CacheDTO record = CacheDTO.builder()
                    .id(key)
                    .value(value)
                    .build();
            this.cache.put(key, record);
            return ApiResponseEnvelop.success(record);
        } catch(Exception e) {
            log.error("Error while acquiring write lock", e);
            return ApiResponseEnvelop.error(new ArrayList<>() {{
                add("Error while adding record to cache: " + e.getMessage());
            }});
        } finally {
            lock.writeLock().unlock();
            log.trace("Released write lock");
        }
    }

    @Override
    @Transactional
    public ApiResponseEnvelop<CacheDTO> fetch(String key) {
        log.debug("Fetching record from cache");
        /*
            * Fetch Cache Logic - sync behavior, might have impact on performance
            * 1. Lock
            * 2. Get record from cache(map) => this moves record to top of map
            * 3. If no, check in DB, return error if not in DB
            * 4. If in DB, remove from DB, add to cache(map) at top of map, evictLeastUsed and return
            * 5. Release lock
         */

        try {
            //Step 1
            lock.writeLock().lock();
            log.trace("Acquired write lock");

            //Step 2
            if(this.cache.containsKey(key)) {
                log.debug("Record with key {} found in cache", key);
                return ApiResponseEnvelop.success(this.cache.get(key));
            }

            //Step 3
            Optional<CacheEntity> cacheEntity = this.cacheRepository.findById(key);
            if(cacheEntity.isEmpty()) {
                log.debug("Record with key {} not found in cache or DB", key);
                return ApiResponseEnvelop.error(new ArrayList<>() {{
                    add("Record with key " + key + " not found in cache or DB");
                }});
            }
            cacheRepository.deleteById(key);
            CacheDTO record = CacheDTO.builder()
                    .id(cacheEntity.get().getId())
                    .value(cacheEntity.get().getValue())
                    .build();
            if(this.cache.size() >= this.cacheConfig.getMaxSize()) {
                log.debug("Cache size {} has reached max limit {}", this.cache.size(), this.cacheConfig.getMaxSize());
                evictLeastUsedAndPersist();
            }
            this.cache.put(key, record);
            return ApiResponseEnvelop.success(record);
        } catch(Exception e) {
            log.error("Error while acquiring read lock", e);
            return ApiResponseEnvelop.error(new ArrayList<>() {{
                add("Error while fetching record from cache: " + e.getMessage());
            }});
        } finally {
            lock.readLock().unlock();
            log.trace("Released read lock");
        }
    }

    @Override
    @Transactional
    public ApiResponseEnvelop<String> remove(String key) {
        log.debug("Deleting record from cache");
        try {
            lock.writeLock().lock();
            log.trace("Acquired write lock");

            CacheDTO record = this.cache.remove(key);
            if(null == record) {
                try {
                    log.trace("Attempting to delete record with key {} from DB", key);
                    this.cacheRepository.deleteById(key);
                    log.debug("Record with key {} deleted from DB", key);
                } catch(EmptyResultDataAccessException e) {
                    log.debug("Record with key {} not found in cache or DB", key);
                    return ApiResponseEnvelop.error(new ArrayList<>() {{
                        add("Record with key " + key + " not found in cache or DB");
                    }});
                }
            }
            return ApiResponseEnvelop.success(null);
        } catch(Exception e) {
            log.error("Error while deleting record from cache", e);
            return ApiResponseEnvelop.error(new ArrayList<>() {{
                add("Error while deleting record from cache: " + e.getMessage());
            }});
        } finally {
            lock.writeLock().unlock();
            log.trace("Released write lock");
        }
    }

    @Override
    @Transactional
    public ApiResponseEnvelop<String> removeAll() {
        log.debug("Deleting all records from cache");
        try {
            lock.writeLock().lock();
            log.trace("Acquired write lock");
            this.cache.clear();
            this.cacheRepository.deleteAllInBatch();
            return ApiResponseEnvelop.success(null);
        } catch(Exception e) {
            log.error("Error while deleting all records from cache", e);
            return ApiResponseEnvelop.error(new ArrayList<>() {{
                add("Error while deleting all records from cache: " + e.getMessage());
            }});
        } finally {
            lock.writeLock().unlock();
            log.trace("Released write lock");
        }
    }

    @Override
    @Transactional
    public ApiResponseEnvelop<String> clear() {
        log.debug("Clearing cache");
        try {
            lock.writeLock().lock();
            log.trace("Acquired write lock");
            this.cache.clear();
            return ApiResponseEnvelop.success(null);
        } catch(Exception e) {
            return ApiResponseEnvelop.error(new ArrayList<>() {{
                add("Error while clearing cache: " + e.getMessage());
            }});
        } finally {
            lock.writeLock().unlock();
            log.trace("Released write lock");
        }
    }

    private void evictLeastUsedAndPersist() {
        if(this.cache.isEmpty()) return;

        Iterator<Map.Entry<String, CacheDTO>> it = this.cache.entrySet().iterator();
        if(it.hasNext()) {
            CacheDTO record = it.next().getValue();
            it.remove();
            CacheEntity entity = CacheEntity.builder()
                    .id(record.getId())
                    .value(record.getValue())
                    .build();
            this.cacheRepository.save(entity);
        }
    }
}
