package org.example.kcacheservice.controller;

import org.example.kcacheservice.dto.ApiResponseEnvelop;
import org.example.kcacheservice.dto.CacheDTO;
import org.example.kcacheservice.exception.CacheException;
import org.example.kcacheservice.exception.CacheNotFoundException;
import org.example.kcacheservice.service.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("User Cache V1 Controller Test")
public class UserCacheControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "CacheServiceV1")
    private CacheService cacheService;

    @Test
    @DisplayName("GET /v1/user/cache/{id} - Should fetch cache successfully")
    void testGetCacheById_Success() throws Exception {
        String cacheId = "test-key-123";
        String cacheValue = "test-value";
        CacheDTO cacheDTO = CacheDTO.builder()
                .id(cacheId)
                .value(cacheValue)
                .build();
        ApiResponseEnvelop<CacheDTO> successResponse = ApiResponseEnvelop.success(cacheDTO);
        when(cacheService.fetch(cacheId)).thenReturn(successResponse);

        mockMvc.perform(get("/v1/user/cache/{id}", cacheId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(cacheId))
                .andExpect(jsonPath("$.data.value").value(cacheValue));

        verify(cacheService, times(1)).fetch(cacheId);
    }

    @Test
    @DisplayName("GET /v1/user/cache/{id} - Should handle cache not found")
    void testGetCacheById_NotFound() throws Exception {
        String cacheId = "non-existent-key";
        CacheNotFoundException exception = new CacheNotFoundException("Cache not found");
        when(cacheService.fetch(cacheId)).thenThrow(exception);

        mockMvc.perform(get("/v1/user/cache/{id}", cacheId))
                .andExpect(status().isPartialContent())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("Cache not found"));

        verify(cacheService, times(1)).fetch(cacheId);
    }

    @Test
    @DisplayName("GET /v1/user/cache/{id} - Should handle cache exception")
    void testGetCacheById_CacheException() throws Exception {
        String cacheId = "error-key";
        CacheException exception = new CacheException("Failed to fetch");
        when(cacheService.fetch(cacheId)).thenThrow(exception);

        mockMvc.perform(get("/v1/user/cache/{id}", cacheId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("Failed to fetch"));

        verify(cacheService, times(1)).fetch(cacheId);
    }

    @Test
    @DisplayName("GET /v1/user/cache/{id} - Should handle UUID format ID")
    void testGetCacheById_UUIDFormat() throws Exception {

        String cacheId = "550e8400-e29b-41d4-a716-446655440000";
        String cacheValue = "uuid-value";
        CacheDTO cacheDTO = CacheDTO.builder()
                .id(cacheId)
                .value(cacheValue)
                .build();
        ApiResponseEnvelop<CacheDTO> successResponse = ApiResponseEnvelop.success(cacheDTO);
        when(cacheService.fetch(cacheId)).thenReturn(successResponse);

        mockMvc.perform(get("/v1/user/cache/{id}", cacheId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(cacheId));

        verify(cacheService, times(1)).fetch(cacheId);
    }

    @Test
    @DisplayName("POST /v1/user/cache/{id} - Should add cache with provided ID")
    void testAdd_WithProvidedId_Success() throws Exception {
        // Arrange
        String cacheId = "custom-key-123";
        String cacheValue = "test-value-content";
        CacheDTO cacheDTO = CacheDTO.builder()
                .id(cacheId)
                .value(cacheValue)
                .build();
        ApiResponseEnvelop<CacheDTO> successResponse = ApiResponseEnvelop.success(cacheDTO);
        when(cacheService.add(cacheId, cacheValue)).thenReturn(successResponse);

        mockMvc.perform(post("/v1/user/cache/{id}", cacheId)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(cacheValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(cacheId))
                .andExpect(jsonPath("$.data.value").value(cacheValue));

        verify(cacheService, times(1)).add(cacheId, cacheValue);
    }

    @Test
    @DisplayName("POST /v1/user/cache - Should add cache with auto-generated UUID")
    void testAdd_WithoutId_Success() throws Exception {
        String cacheValue = "test-value-content";
        CacheDTO cacheDTO = CacheDTO.builder()
                .id("generated-uuid")
                .value(cacheValue)
                .build();
        ApiResponseEnvelop<CacheDTO> successResponse = ApiResponseEnvelop.success(cacheDTO);

        when(cacheService.add(any(String.class), eq(cacheValue))).thenReturn(successResponse);


        mockMvc.perform(post("/v1/user/cache")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(cacheValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.value").value(cacheValue));

        verify(cacheService, times(1)).add(any(String.class), eq(cacheValue));
    }

    @Test
    @DisplayName("POST /v1/user/cache/{id} - Should reject JSON content type")
    void testAdd_WrongContentType_JSON() throws Exception {
        mockMvc.perform(post("/v1/user/cache/test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"test\"}"))
                .andExpect(status().isInternalServerError());

        verify(cacheService, never()).add(any(), any());
    }

    @Test
    @DisplayName("POST /v1/user/cache/{id} - Should handle cache exception during add")
    void testAdd_CacheException() throws Exception {
        String cacheId = "error-key";
        String cacheValue = "test-value";
        CacheException exception = new CacheException("Failed to add");
        when(cacheService.add(cacheId, cacheValue)).thenThrow(exception);

        mockMvc.perform(post("/v1/user/cache/{id}", cacheId)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(cacheValue))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors").value("Failed to add"));

        verify(cacheService, times(1)).add(cacheId, cacheValue);
    }

    @Test
    @DisplayName("DELETE /v1/user/cache/key/{id} - Should remove cache successfully")
    void testRemove_Success() throws Exception {
        String cacheId = "key-to-remove";
        ApiResponseEnvelop<String> successResponse = ApiResponseEnvelop.success("OK");
        when(cacheService.remove(cacheId)).thenReturn(successResponse);

        mockMvc.perform(delete("/v1/user/cache/key/{id}", cacheId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value("OK"));

        verify(cacheService, times(1)).remove(cacheId);
    }

    @Test
    @DisplayName("DELETE /v1/user/cache/key/{id} - Should handle cache not found during remove")
    void testRemove_NotFound() throws Exception {
        String cacheId = "non-existent-key";
        CacheNotFoundException exception = new CacheNotFoundException("Cache not found for removal");
        when(cacheService.remove(cacheId)).thenThrow(exception);

        mockMvc.perform(delete("/v1/user/cache/key/{id}", cacheId))
                .andExpect(status().isPartialContent())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("Cache not found for removal"));

        verify(cacheService, times(1)).remove(cacheId);
    }

    @Test
    @DisplayName("Should verify service interactions are isolated per endpoint")
    void testServiceInteractions_Isolated() throws Exception {
        String getKey = "get-key";
        String addKey = "add-key";
        String removeKey = "remove-key";

        CacheDTO getDTO = CacheDTO.builder()
                .id(getKey)
                .value("value1")
                .build();
        CacheDTO addDTO = CacheDTO.builder()
                .id(addKey)
                .value("value2")
                .build();

        when(cacheService.fetch(getKey)).thenReturn(ApiResponseEnvelop.success(getDTO));
        when(cacheService.add(eq(addKey), any())).thenReturn(ApiResponseEnvelop.success(addDTO));
        when(cacheService.remove(removeKey)).thenReturn(ApiResponseEnvelop.success("Removed"));

        mockMvc.perform(get("/v1/user/cache/{id}", getKey));
        mockMvc.perform(post("/v1/user/cache/{id}", addKey)
                .contentType(MediaType.TEXT_PLAIN)
                .content("value2"));
        mockMvc.perform(delete("/v1/user/cache/key/{id}", removeKey));

        verify(cacheService, times(1)).fetch(getKey);
        verify(cacheService, times(1)).add(eq(addKey), any());
        verify(cacheService, times(1)).remove(removeKey);
        verifyNoMoreInteractions(cacheService);
    }
}