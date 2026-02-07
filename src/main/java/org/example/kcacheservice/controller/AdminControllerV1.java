package org.example.kcacheservice.controller;

import org.example.kcacheservice.dto.ApiResponseEnvelop;
import org.example.kcacheservice.service.CacheService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/cache")
public class AdminControllerV1 {

    private final CacheService cacheService;

    public AdminControllerV1(@Qualifier("CacheServiceV1") CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponseEnvelop<String>> clear() {
        ApiResponseEnvelop<String> response = this.cacheService.clear();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/all")
    public ResponseEntity<ApiResponseEnvelop<String>> removeAll() {
        ApiResponseEnvelop<String> response = this.cacheService.removeAll();
        return ResponseEntity.ok(response);
    }
}
