package org.example.kcacheservice.controller;

import org.example.kcacheservice.dto.ApiResponseEnvelop;
import org.example.kcacheservice.service.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Admin Controller V1 Test")
public class AdminControllerV2Test {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "CacheServiceV2")
    private CacheService cacheService;

    @Test
    @DisplayName("DELETE /v2/admin/cache/clear - Should call clear() and return 200 OK")
    void testClear_Success() throws Exception {
        ApiResponseEnvelop<String> successResponse = ApiResponseEnvelop.success("OK");
        when(cacheService.clear()).thenReturn(successResponse);

        mockMvc.perform(delete("/v2/admin/cache/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value("OK"));

        verify(cacheService, times(1)).clear();
    }

    @Test
    @DisplayName("DELETE /v2/admin/cache/clear - Should call clear() and return 500 Internal Server Exception")
    void testClear_Failure500() throws Exception {
        when(cacheService.clear()).thenThrow(new RuntimeException("Cache clear failed"));

        mockMvc.perform(delete("/v2/admin/cache/clear"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errors[0]").value("Cache clear failed"));

        verify(cacheService, times(1)).clear();
    }

    @Test
    @DisplayName("DELETE /v2/admin/cache/remove/all - Should call removeAll() and return 200 OK")
    void testRemoveAll_Success() throws Exception {
        ApiResponseEnvelop<String> successResponse = ApiResponseEnvelop.success("OK");
        when(cacheService.removeAll()).thenReturn(successResponse);

        mockMvc.perform(delete("/v2/admin/cache/remove/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value("OK"));

        verify(cacheService, times(1)).removeAll();
    }

    @Test
    @DisplayName("DELETE /v2/admin/cache/remove/all - Should call removeAll() and return 500 Internal Server Exception")
    void testRemoveAll_Failure500() throws Exception {
        when(cacheService.removeAll()).thenThrow(new RuntimeException("Cache remove all failed"));

        mockMvc.perform(delete("/v2/admin/cache/remove/all"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errors[0]").value("Cache remove all failed"));

        verify(cacheService, times(1)).removeAll();
    }
}
