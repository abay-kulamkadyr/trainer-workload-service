package com.epam.workload.interfaces.messaging.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerWorkloadEvent {

    private String trainerUsername;
    private String trainerFirstname;
    private String trainerLastname;
    private Boolean isActive;
    private LocalDateTime trainingDate;
    private Integer trainingDurationMinutes;
    private ActionType actionType;
    private String transactionId;

    public enum ActionType {
        ADD,
        DELETE
    }
}
