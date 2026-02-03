package com.epam.workload.interfaces.web.dto.request;

import java.time.LocalDateTime;

import com.epam.workload.application.service.ActionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrainerWorkloadWebRequest(@NotBlank(message = "Trainer username is required") String trainerUsername,

        @NotBlank(message = "Trainer first name is required") String trainerFirstname,

        @NotBlank(message = "Trainer last name is required") String trainerLastname,

        @NotNull(message = "Active status is required") Boolean isActive,

        @NotNull(message = "Training date is required") LocalDateTime trainingDate,

        @NotNull(message = "Training duration is required") @Min(
                value = 1,
                message = "Training duration must be at least 1 minute") Integer trainingDurationMin,

        @NotNull(message = "Action type is required") ActionType actionType) {}
