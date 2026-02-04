package service;

import org.example.kcacheservice.dto.ApiResponseEnvelop;
import org.example.kcacheservice.dto.CacheDTO;

public interface CacheService {
    ApiResponseEnvelop<CacheDTO> add(String key, String value);

    ApiResponseEnvelop<CacheDTO> fetch(String key);

    ApiResponseEnvelop<String> remove(String key);

    ApiResponseEnvelop<String> removeAll();

    ApiResponseEnvelop<String> clear();
}
