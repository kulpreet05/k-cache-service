package org.example.kcacheservice.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/user/cache")
public class UserCacheController {

    @GetMapping("/id/{id}")
    public ResponseEntity<String> getCacheById(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/key/{key}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> add(@PathVariable String key, @RequestBody String value) {
        return ResponseEntity.ok().build();
    }
}
