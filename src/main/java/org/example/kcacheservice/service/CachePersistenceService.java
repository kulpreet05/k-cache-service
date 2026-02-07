package org.example.kcacheservice.service;

import org.example.kcacheservice.dto.CacheDTO;

import java.util.Optional;

public interface CachePersistenceService {
    public void initPersistenceStore();

    public void persistToStore(CacheDTO cacheDTO);

    public Optional<CacheDTO> getFromStore(String key);

    public void removeFromStore(String key);

    public void removeAll();
}
