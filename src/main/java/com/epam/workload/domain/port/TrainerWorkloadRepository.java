package com.epam.workload.domain.port;

import java.util.Optional;

import com.epam.workload.domain.model.TrainerWorkload;

public interface TrainerWorkloadRepository {
    Optional<TrainerWorkload> findByUsername(String username);

    void save(TrainerWorkload workload);
}
