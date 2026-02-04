package org.example.kcacheservice.dto;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class ApiResponseEnvelop<T> {
    private final String status;
    private final T data;
    private final List<String> errors;

    private ApiResponseEnvelop(String status, T data, List<String> errors) {
        this.status = status;
        this.data = data;
        this.errors = errors;
    }

    public static <T> ApiResponseEnvelop<T> success(T data) {
        return new ApiResponseEnvelop<T>("SUCCESS", data, null);
    }

    public static <T> ApiResponseEnvelop<T> error(List<String> errors) {
        return new ApiResponseEnvelop<T>("ERROR", null, errors);
    }
}
