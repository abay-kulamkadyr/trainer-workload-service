package com.epam.workload.domain.model;

import java.time.Month;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;

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
    String id;

    @EqualsAndHashCode.Include
    String username;

    String firstName;

    String lastName;

    Boolean active;

    @Builder.Default
    Map<Year, Map<Month, Integer>> yearMonthDuration = new HashMap<>();
}
