package com.epam.workload.domain.port;

import java.time.Month;
import java.time.Year;
import java.util.Optional;

import com.epam.workload.domain.model.TrainerWorkload;

public interface TrainerWorkloadRepository {
    Optional<TrainerWorkload> findByUsernameAndYearAndMonth(String username, Year year, Month month);

    void save(TrainerWorkload workloadEntry);
}
