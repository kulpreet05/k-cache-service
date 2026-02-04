package org.example.kcacheservice.repository;

import org.example.kcacheservice.entity.CacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CacheRepository extends JpaRepository<CacheEntity, String> { }
