package com.shopscale.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
        name = "Standard API Response Wrapper", 
        description = "Global standard enterprise wrapper encompassing all backend JSON responses."
)
public record StandardResponse<T>(

        @Schema(description = "Execution status metric of the API request.", example = "SUCCESS")
        ResponseStatus status,

        @Schema(description = "The HTTP status code mapped intrinsically to the response payload.", example = "200")
        int code,

        @Schema(description = "Human-readable execution message or error detail.", example = "Request successful")
        String message,

        @Schema(description = "The dynamic DTO payload containing requested data.")
        T data,

        @Schema(description = "ISO-8601 UTC timestamp of when the response was generated.", example = "2026-04-01T12:00:00Z")
        String timestamp
) {

    public static <T> StandardResponse<T> success(T data) {
        return new StandardResponse<>(
                ResponseStatus.SUCCESS,
                200,
                "Request successful",
                data,
                Instant.now().toString()
        );
    }


    public static <T> StandardResponse<T> success(String message, T data) {
         return new StandardResponse<>(
                 ResponseStatus.SUCCESS,
                 200,
                 message,
                 data,
                 Instant.now().toString()
         );
    }

    public static <T> StandardResponse<T> failure(String message, int code) {
        return new StandardResponse<>(
                ResponseStatus.FAILURE,
                code,
                message,
                null,
                Instant.now().toString()
        );
    }
}
