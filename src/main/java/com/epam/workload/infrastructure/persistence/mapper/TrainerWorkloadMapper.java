package com.epam.workload.infrastructure.persistence.mapper;

import java.time.Month;
import java.time.Year;

import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.infrastructure.persistence.dao.TrainerWorkloadDAO;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class TrainerWorkloadMapper {

    public TrainerWorkload toDomain(@NonNull TrainerWorkloadDAO entity) {
        return TrainerWorkload.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .active(entity.isActive())
                .year(Year.of(entity.getYear()))
                .month(Month.of(entity.getMonth()))
                .trainingDurationMinutes(entity.getDurationMin())
                .build();
    }

    public TrainerWorkloadDAO toEntity(@NonNull TrainerWorkload domain) {
        return TrainerWorkloadDAO.builder()
                .id(domain.getId())
                .username(domain.getUsername())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .isActive(domain.getActive())
                .year(domain.getYear().getValue())
                .month(domain.getMonth().getValue())
                .durationMin(domain.getTrainingDurationMinutes())
                .build();
    }
}
