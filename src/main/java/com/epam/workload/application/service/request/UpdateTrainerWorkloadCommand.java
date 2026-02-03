package com.epam.workload.application.service.request;

import java.time.LocalDateTime;

import com.epam.workload.application.service.ActionType;

public record UpdateTrainerWorkloadCommand(String trainerUsername, String trainerFirstname, String trainerLastName,
        Boolean isActive, LocalDateTime trainingDate, Integer trainingDurationMin, ActionType actionType) {}
