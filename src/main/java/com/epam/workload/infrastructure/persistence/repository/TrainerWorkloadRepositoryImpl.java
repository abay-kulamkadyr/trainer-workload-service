package com.epam.workload.infrastructure.persistence.repository;

import java.time.Month;
import java.time.Year;
import java.util.Optional;

import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.domain.port.TrainerWorkloadRepository;
import com.epam.workload.infrastructure.persistence.mapper.TrainerWorkloadMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TrainerWorkloadRepositoryImpl implements TrainerWorkloadRepository {
    private final JpaTrainerWorkloadRepository jpaRepository;
    private final TrainerWorkloadMapper mapper;

    @Autowired
    TrainerWorkloadRepositoryImpl(JpaTrainerWorkloadRepository jpaRepository, TrainerWorkloadMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<TrainerWorkload> findByUsernameAndYearAndMonth(String username, Year year, Month month) {
        return jpaRepository
                .findByUsernameAndYearAndMonth(username, year.getValue(), month.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public void save(TrainerWorkload workloadEntry) {
        jpaRepository.save(mapper.toEntity(workloadEntry));
    }
}
