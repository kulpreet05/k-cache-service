package org.example.kcacheservice.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "cache")
@Validated
@Data
public class CacheConfig {

    /*
        * Maximum size of the cache
        * Default is 100
     */
    @Min(value = 1, message = "maxSize must be at least 1")
    private int maxSize = 100;
}
