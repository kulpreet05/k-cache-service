package org.example.kcacheservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/admin/cache")
public class AdminController {

    @DeleteMapping("/clear")
    public ResponseEntity<String> clear() {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/remove/all")
    public ResponseEntity<String> removeAll() {
        return ResponseEntity.ok().build();
    }
}
