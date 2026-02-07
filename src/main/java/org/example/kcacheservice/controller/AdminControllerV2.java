package org.example.kcacheservice.controller;

import org.example.kcacheservice.dto.ApiResponseEnvelop;
import org.example.kcacheservice.service.CacheService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/admin/cache")
public class AdminControllerV2 {

    private final CacheService cacheService;

    public AdminControllerV2(@Qualifier("CacheServiceV2") CacheService cacheService) {
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
