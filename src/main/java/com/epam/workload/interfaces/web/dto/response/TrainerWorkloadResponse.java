package com.epam.workload.interfaces.web.dto.response;

import java.time.Month;

public record TrainerWorkloadResponse(String username, String year, Month month, Integer trainingDurationMinutes) {}
