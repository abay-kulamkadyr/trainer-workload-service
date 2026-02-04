package com.epam.workload.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import com.epam.workload.infrastructure.persistence.dao.TrainerWorkloadDAO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTrainerWorkloadRepository extends JpaRepository<TrainerWorkloadDAO, Long> {
    Optional<TrainerWorkloadDAO> findByUsernameAndYearAndMonth(String username, int year, int month);

    List<TrainerWorkloadDAO> findByUsername(String username);
}
