package com.epam.workload.infrastructure.persistence.repository;

import java.util.Optional;

import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.domain.port.TrainerWorkloadRepository;
import com.epam.workload.infrastructure.persistence.mapper.TrainerWorkloadMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TrainerWorkloadRepositoryImpl implements TrainerWorkloadRepository {
    private final TrainerWorkloadMongoRepository mongoRepository;
    private final TrainerWorkloadMapper mapper;

    @Autowired
    TrainerWorkloadRepositoryImpl(TrainerWorkloadMongoRepository mongoRepository, TrainerWorkloadMapper mapper) {
        this.mongoRepository = mongoRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<TrainerWorkload> findByUsername(String username) {
        return mongoRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public void save(TrainerWorkload workloadEntry) {
        mongoRepository.save(mapper.toEntity(workloadEntry));
    }
}
