package com.epam.workload.infrastructure.persistence.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.domain.port.TrainerWorkloadRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("no-integrations")
public class TrainerWorkloadRepositoryInMemoryImpl implements TrainerWorkloadRepository {

    private final Map<String, TrainerWorkload> store = new ConcurrentHashMap<>();

    @Override
    public Optional<TrainerWorkload> findByUsername(String username) {
        return Optional.ofNullable(store.get(username));
    }

    @Override
    public void save(TrainerWorkload workload) {
        store.put(workload.getUsername(), workload);
    }
}
