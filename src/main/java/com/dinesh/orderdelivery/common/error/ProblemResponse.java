package com.dinesh.orderdelivery.common.error;

import java.time.Instant;
import java.util.List;

public record ProblemResponse(
        String message,
        int status,
        String path,
        Instant timestamp,
        List<String> errors
) {
    public static ProblemResponse of(String message, int status, String path, List<String> errors) {
        return new ProblemResponse(message, status, path, Instant.now(), errors);
    }
}

