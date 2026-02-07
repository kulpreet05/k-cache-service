package org.example.kcacheservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.kcacheservice.dto.CacheDTO;
import org.example.kcacheservice.entity.CacheEntity;
import org.example.kcacheservice.exception.CacheException;
import org.example.kcacheservice.repository.CacheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@Slf4j
public class CachePersistenceServiceImpl implements org.example.kcacheservice.service.CachePersistenceService {
    private final Map<String, String> persistentStore;
    private final ReentrantReadWriteLock lock;
    private final BlockingQueue<String> evictionQueue;
    private boolean isInit;
    private final CacheRepository cacheRepository;
    private final ScheduledExecutorService dbSyncExecutor;

    public CachePersistenceServiceImpl(CacheRepository cacheRepository) {
        this.persistentStore = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.evictionQueue = new LinkedBlockingDeque<>();
        this.isInit = false;
        this.cacheRepository = cacheRepository;
        this.dbSyncExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("db-sync-thread");
            t.setDaemon(false);
            return t;
        });
    }

    @Override
    @Transactional
    public void initPersistenceStore() {
        log.debug("Initializing persistent store from database");
        if(this.isInit) return;
        try {
            this.loadStoreFromDB();
            this.dbSyncExecutor.scheduleAtFixedRate(this::flushEvictingQueue, 0, 10, TimeUnit.SECONDS);
            this.isInit = true;
        } catch(Exception e) {
            log.error("Error while initializing persistent store from database", e);
            throw new CacheException("Error initializing persistent store");
        } finally {
            if(this.lock.writeLock().isHeldByCurrentThread()) {
                this.lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void persistToStore(CacheDTO cacheDTO) {
        log.debug("Persisting record to persistent store - Key: {}, Value: {}", cacheDTO.getId(), cacheDTO.getValue());
        if(!this.isInit) throw new CacheException("Persistent store not initialized");
        try {
            this.persistentStore.put(cacheDTO.getId(), cacheDTO.getValue());
            this.evictionQueue.offer(cacheDTO.getId());
        } catch(CacheException e) {
            throw e;
        } catch(Exception e) {
            log.error("Error while persisting record to database", e);
            throw new CacheException("Error persisting record to database");
        }
    }

    @Override
    public Optional<CacheDTO> getFromStore(String key) {
        log.debug("Getting record from persistent store - Key: {}", key);
        if(!this.isInit) throw new CacheException("Persistent store not initialized");
        try {
            if(this.persistentStore.containsKey(key)) {
                String value = this.persistentStore.get(key);
                this.evictionQueue.offer(key);
                return Optional.of(CacheDTO.builder()
                        .id(key)
                        .value(value)
                        .build());
            } else {
                return Optional.empty();
            }
        } catch(CacheException e) {
           throw e;
        } catch(Exception e) {
            log.error("Error while fetching record from database", e);
            throw new CacheException("Error fetching record from database");
        }
    }

    @Override
    public void removeFromStore(String key) {
        log.debug("Removing record from persistent store - Key: {}", key);
        try {
            if(!this.isInit) throw new CacheException("Persistent store not initialized");
            if(this.persistentStore.containsKey(key)) {
                this.persistentStore.remove(key);
                this.evictionQueue.offer(key);
            }
        } catch(CacheException e) {
            throw e;
        } catch(Exception e) {
            log.error("Error while removing record from database", e);
            throw new CacheException("Error removing record from database");
        }
    }

    @Override
    @Transactional
    public void removeAll() {
        log.debug("Removing all records from persistent store");
        try {
            if(!this.isInit) throw new CacheException("Persistent store not initialized");
            this.persistentStore.clear();
            this.evictionQueue.clear();
            this.cacheRepository.deleteAll();
        } catch(CacheException e) {
            throw e;
        } catch(Exception e) {
            log.error("Error while clearing persistent store from database", e);
            throw new CacheException("Error clearing persistent store from database");
        }
    }

    private void loadStoreFromDB() {
        log.debug("Loading persistent store from DB");
        try {
            List<CacheEntity> records = this.cacheRepository.findAll();
            Map<String, String> tempStore = records.stream()
                    .collect(HashMap::new, (m, v) -> m.put(v.getId(), v.getValue()), HashMap::putAll);
            this.lock.writeLock().lock();
            this.persistentStore.putAll(tempStore);
        } catch(Exception e) {
            log.error("Error while loading persistent store from database", e);
            throw new CacheException("Error loading persistent store from database");
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void flushEvictingQueue() {
        log.debug("Flushing evicting queue to DB");
        if(this.evictionQueue.isEmpty()) return;

        List<String> keys = new ArrayList<>();
        this.evictionQueue.drainTo(keys);
        List<CacheEntity> entitiesToSave = new ArrayList<>();
        List<String> idsToDelete = new ArrayList<>();
        try {
            lock.readLock().lock();
            for(String key : keys) {
                String value = this.persistentStore.get(key);
                if(null != value) {
                    CacheEntity entity = CacheEntity.builder()
                            .id(key)
                            .value(value)
                            .build();
                    entitiesToSave.add(entity);
                } else {
                    idsToDelete.add(key);
                }
            }
            log.debug("Prepared {} entities to save and {} ids to delete from DB", entitiesToSave.size(), idsToDelete.size());
        } catch(Exception e) {
            log.error("Error flushing eviction queue to DB", e);
            this.evictionQueue.addAll(keys);
        } finally {
            lock.readLock().unlock();
        }

        try {
            if(!entitiesToSave.isEmpty()) {
                this.cacheRepository.saveAll(entitiesToSave);
            }
            if(!idsToDelete.isEmpty()) {
                this.cacheRepository.deleteAllById(idsToDelete);
            }
        } catch(Exception e) {
            log.error("Error persisting evicted entries to DB", e);
            this.evictionQueue.addAll(keys);
        }
    }
}
