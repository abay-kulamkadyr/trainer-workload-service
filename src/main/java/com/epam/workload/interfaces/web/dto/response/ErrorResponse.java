package com.epam.workload.interfaces.web.dto.response;

import java.time.Instant;

public record ErrorResponse(String error, String message, String path, Instant timestamp, String transactionId) {}
