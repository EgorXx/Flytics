package ru.kpfu.itis.sorokin.dto;

public record TaskToProcess(
        long id,
        String payload,
        int priority,
        int attempts
) {
}
