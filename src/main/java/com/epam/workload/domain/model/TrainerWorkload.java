package com.epam.workload.domain.model;

import java.time.Month;
import java.time.Year;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@With
public class TrainerWorkload {
    @EqualsAndHashCode.Include
    Long id;
    @EqualsAndHashCode.Include
    String username;
    String firstName;
    String lastName;
    Boolean active;
    @EqualsAndHashCode.Include
    Year year;
    @EqualsAndHashCode.Include
    Month month;
    Integer trainingDurationMinutes;
}
