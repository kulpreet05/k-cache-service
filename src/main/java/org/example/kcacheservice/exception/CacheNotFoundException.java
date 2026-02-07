package org.example.kcacheservice.exception;

import java.util.ArrayList;

public class CacheNotFoundException extends CacheException {
    public CacheNotFoundException(String message) {
        super(message, new ArrayList<>() {{
            add(message);
        }});
    }
}
