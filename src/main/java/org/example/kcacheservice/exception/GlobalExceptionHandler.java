package org.example.kcacheservice.exception;

import org.example.kcacheservice.dto.ApiResponseEnvelop;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponseEnvelop<Object> handleException(Exception e) {
        ArrayList<String> errors = new ArrayList<>();
        errors.add(e.getMessage());
        return ApiResponseEnvelop.error(errors);
    }

    @ExceptionHandler(CacheException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponseEnvelop<Object> handleCacheException(CacheException e) {
        return prepareAPIResponse(e);
    }

    @ExceptionHandler(CacheNotFoundException.class)
    @ResponseStatus(HttpStatus.PARTIAL_CONTENT)
    public ApiResponseEnvelop<Object> handleCacheNotFoundException(CacheNotFoundException e) {
        return prepareAPIResponse(e);
    }

    private ApiResponseEnvelop<Object> prepareAPIResponse(CacheException e) {
        return ApiResponseEnvelop.error(e.getDisplayMessages());
    }
}
