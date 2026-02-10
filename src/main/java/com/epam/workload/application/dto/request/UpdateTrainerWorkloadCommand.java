package com.epam.workload.application.dto.request;

import java.time.LocalDateTime;

public record UpdateTrainerWorkloadCommand(
        String trainerUsername,
        String trainerFirstname,
        String trainerLastname,
        Boolean isActive,
        LocalDateTime trainingDate,
        Integer trainingDurationMinutes,
        ActionType actionType) {}
