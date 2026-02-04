package org.example.kcacheservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CacheDTO {
    private String id;
    private String value;
}
