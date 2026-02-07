package org.example.kcacheservice.controller;

import org.example.kcacheservice.dto.ApiResponseEnvelop;
import org.example.kcacheservice.dto.CacheDTO;
import org.example.kcacheservice.service.CacheService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v2/user/cache")
public class UserCacheV2Controller {

    private final CacheService cacheService;

    public UserCacheV2Controller(@Qualifier("CacheServiceV2") CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseEnvelop<CacheDTO>> getCacheById(@PathVariable String id) {
        ApiResponseEnvelop<CacheDTO> response = cacheService.fetch(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = {"", "/{id}"}, consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<ApiResponseEnvelop<CacheDTO>> add(@PathVariable Optional<String> id, @RequestBody String value) {
        ApiResponseEnvelop<CacheDTO> response = cacheService.add(id.orElse(UUID.randomUUID().toString()), value);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/key/{id}")
    public ResponseEntity<ApiResponseEnvelop<String>> remove(@PathVariable String id) {
        ApiResponseEnvelop<String> response = cacheService.remove(id);
        return ResponseEntity.ok(response);
    }
}
