package com.epam.workload.application.exception;

public class InsufficientDurationException extends RuntimeException {
    public InsufficientDurationException(String message) {
        super(message);
    }
}
