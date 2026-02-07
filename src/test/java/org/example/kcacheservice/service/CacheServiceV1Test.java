package org.example.kcacheservice.service;

import org.example.kcacheservice.config.CacheConfig;
import org.example.kcacheservice.dto.ApiResponseEnvelop;
import org.example.kcacheservice.dto.CacheDTO;
import org.example.kcacheservice.entity.CacheEntity;
import org.example.kcacheservice.exception.CacheException;
import org.example.kcacheservice.exception.CacheNotFoundException;
import org.example.kcacheservice.repository.CacheRepository;
import org.example.kcacheservice.service.impl.CacheServiceV1Impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Admin Controller V1 Test")
public class CacheServiceV1Test {

    @Mock
    private CacheRepository cacheRepository;

    @Mock
    private CacheConfig cacheConfig;

    @InjectMocks
    private CacheServiceV1Impl cacheService;

    @BeforeEach
    void setUp() {
        when(cacheConfig.getMaxSize()).thenReturn(3);
        cacheService = new CacheServiceV1Impl(cacheConfig, cacheRepository);
    }

    @Test
    @DisplayName("Should add new record to empty cache successfully")
    void testAdd_NewRecord_EmptyCache() {
        String key = "key1";
        String value = "value1";

        ApiResponseEnvelop<CacheDTO> response = cacheService.add(key, value);

        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getId()).isEqualTo(key);
        assertThat(response.getData().getValue()).isEqualTo(value);
        verify(cacheRepository, times(1)).deleteById(key);
        verify(cacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete existing DB record before adding to cache")
    void testAdd_DeletesExistingDBRecord() {
        String key = "key1";
        String value = "value1";
        doNothing().when(cacheRepository).deleteById(key);

        cacheService.add(key, value);

        verify(cacheRepository, times(1)).deleteById(key);
    }

    @Test
    @DisplayName("Should handle EmptyResultDataAccessException when DB record doesn't exist")
    void testAdd_HandlesEmptyResultDataAccessException() {
        String key = "key1";
        String value = "value1";
        doThrow(new EmptyResultDataAccessException(1)).when(cacheRepository).deleteById(key);

        ApiResponseEnvelop<CacheDTO> response = cacheService.add(key, value);

        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getId()).isEqualTo(key);
    }

    @Test
    @DisplayName("Should evict least read and persist when cache is full")
    void testAdd_EvictsLRU_WhenCacheFull() {
        cacheService.add("key1", "value1");
        cacheService.add("key2", "value2");
        cacheService.add("key3", "value3");
        reset(cacheRepository);

        cacheService.add("key4", "value4");

        verify(cacheRepository, times(1)).save(any(CacheEntity.class));

        assertThatThrownBy(() -> cacheService.fetch("key1"))
                .isInstanceOf(CacheNotFoundException.class);
    }

    @Test
    @DisplayName("Should update existing cache record without eviction")
    void testAdd_UpdatesExistingRecord() {
        String key = "key1";
        cacheService.add(key, "oldValue");

        ApiResponseEnvelop<CacheDTO> response = cacheService.add(key, "newValue");

        assertThat(response.getData().getValue()).isEqualTo("newValue");
        verify(cacheRepository, times(2)).deleteById(key);
    }

    @Test
    @DisplayName("Should throw CacheException when repository throws unexpected exception")
    void testAdd_ThrowsCacheException_OnRepositoryError() {
        String key = "key1";
        doThrow(new RuntimeException("Database error")).when(cacheRepository).deleteById(key);

        assertThatThrownBy(() -> cacheService.add(key, "value1"))
                .isInstanceOf(CacheException.class)
                .hasMessageContaining("Database error");
    }

    @Test
    @DisplayName("Should fetch record from cache successfully")
    void testFetch_FromCache_Success() {
        String key = "key1";
        String value = "value1";
        cacheService.add(key, value);

        ApiResponseEnvelop<CacheDTO> response = cacheService.fetch(key);

        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getId()).isEqualTo(key);
        assertThat(response.getData().getValue()).isEqualTo(value);
    }

    @Test
    @DisplayName("Should fetch record from DB when not in cache")
    void testFetch_FromDB_WhenNotInCache() {
        String key = "key1";
        String value = "value1";
        CacheEntity entity = CacheEntity.builder()
                .id(key)
                .value(value)
                .build();
        when(cacheRepository.findById(key)).thenReturn(Optional.of(entity));

        ApiResponseEnvelop<CacheDTO> response = cacheService.fetch(key);

        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getId()).isEqualTo(key);
        assertThat(response.getData().getValue()).isEqualTo(value);
        verify(cacheRepository, times(1)).findById(key);
        verify(cacheRepository, times(1)).deleteById(key);
    }

    @Test
    @DisplayName("Should throw CacheNotFoundException when record not found")
    void testFetch_ThrowsNotFoundException_WhenRecordNotFound() {
        String key = "key1";
        when(cacheRepository.findById(key)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cacheService.fetch(key))
                .isInstanceOf(CacheNotFoundException.class)
                .hasMessageContaining("not found in cache or DB");
    }

    @Test
    @DisplayName("Should evict least read when fetching from DB and cache is full")
    void testFetch_EvictsLRU_WhenCacheFull() {
        cacheService.add("key1", "value1");
        cacheService.add("key2", "value2");
        cacheService.add("key3", "value3");

        CacheEntity entity = CacheEntity.builder()
                .id("key4")
                .value("value4")
                .build();
        when(cacheRepository.findById("key4")).thenReturn(Optional.of(entity));
        reset(cacheRepository);
        when(cacheRepository.findById("key4")).thenReturn(Optional.of(entity));

        cacheService.fetch("key4");

        verify(cacheRepository, times(1)).save(any(CacheEntity.class)); // LRU evicted and saved
    }

    @Test
    @DisplayName("Should remove record from cache successfully")
    void testRemove_FromCache_Success() {
        String key = "key1";
        cacheService.add(key, "value1");

        ApiResponseEnvelop<String> response = cacheService.remove(key);

        assertThat(response).isNotNull();
        assertThat(response.getData()).isNull();

        assertThatThrownBy(() -> cacheService.fetch(key))
                .isInstanceOf(CacheNotFoundException.class);
    }

    @Test
    @DisplayName("Should remove record from DB when not in cache")
    void testRemove_FromDB_WhenNotInCache() {
        String key = "key1";
        doNothing().when(cacheRepository).deleteById(key);

        ApiResponseEnvelop<String> response = cacheService.remove(key);

        assertThat(response).isNotNull();
        verify(cacheRepository, times(1)).deleteById(key);
    }

    @Test
    @DisplayName("Should handle repository exception during remove")
    void testRemove_HandlesRepositoryException() {
        String key = "key1";
        doThrow(new RuntimeException("Database error")).when(cacheRepository).deleteById(key);

        ApiResponseEnvelop<String> response = cacheService.remove(key);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should remove all records from cache and DB")
    void testRemoveAll_Success() {
        cacheService.add("key1", "value1");
        cacheService.add("key2", "value2");
        doNothing().when(cacheRepository).deleteAllInBatch();

        ApiResponseEnvelop<String> response = cacheService.removeAll();

        assertThat(response).isNotNull();
        verify(cacheRepository, times(1)).deleteAllInBatch();

        assertThatThrownBy(() -> cacheService.fetch("key1"))
                .isInstanceOf(CacheNotFoundException.class);
    }

    @Test
    @DisplayName("Should handle exception during removeAll")
    void testRemoveAll_HandlesException() {
        doThrow(new RuntimeException("Database error")).when(cacheRepository).deleteAllInBatch();

        ApiResponseEnvelop<String> response = cacheService.removeAll();

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should clear cache without touching DB")
    void testClear_Success() {
        cacheService.add("key1", "value1");
        cacheService.add("key2", "value2");

        reset(cacheRepository);

        ApiResponseEnvelop<String> response = cacheService.clear();

        assertThat(response).isNotNull();
        verifyNoInteractions(cacheRepository);

        assertThatThrownBy(() -> cacheService.fetch("key1"))
                .isInstanceOf(CacheNotFoundException.class);
    }

    @Test
    @DisplayName("Should handle exception during clear")
    void testClear_HandlesException() {
        ApiResponseEnvelop<String> response = cacheService.clear();

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should handle multiple adds to same key")
    void testAdd_MultipleTimesToSameKey() {
        cacheService.add("key1", "value1");
        cacheService.add("key1", "value2");
        cacheService.add("key1", "value3");

        ApiResponseEnvelop<CacheDTO> response = cacheService.fetch("key1");
        assertThat(response.getData().getValue()).isEqualTo("value3");
    }

    @Test
    @DisplayName("Should handle cache with max size of 1")
    void testAdd_WithMaxSizeOne() {
        when(cacheConfig.getMaxSize()).thenReturn(1);
        cacheService = new CacheServiceV1Impl(cacheConfig, cacheRepository);

        cacheService.add("key1", "value1");
        cacheService.add("key2", "value2");

        verify(cacheRepository, times(1)).save(any(CacheEntity.class));
    }
}