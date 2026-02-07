package org.example.kcacheservice.exception;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CacheException extends RuntimeException {
    private final List<String> displayMessages;

    public CacheException(String message) {
        super(message);
        this.displayMessages = new ArrayList<>() {{
            add(message);
        }};
    }

    public CacheException(String message, List<String> displayMessages) {
        super(message);
        this.displayMessages = displayMessages;
    }
}
