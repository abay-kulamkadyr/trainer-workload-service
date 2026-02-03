package com.epam.workload.application.service.exception;

public class InsufficientDurationException extends RuntimeException {
    public InsufficientDurationException(String message) {
        super(message);
    }
}
