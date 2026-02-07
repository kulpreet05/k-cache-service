package org.example.kcacheservice.service;

import org.example.kcacheservice.dto.ApiResponseEnvelop;
import org.example.kcacheservice.dto.CacheDTO;

public interface CacheService {
    public ApiResponseEnvelop<CacheDTO> add(String key, String value);

    public ApiResponseEnvelop<CacheDTO> fetch(String key);

    public ApiResponseEnvelop<String> remove(String key);

    public ApiResponseEnvelop<String> removeAll();

    public ApiResponseEnvelop<String> clear();
}
