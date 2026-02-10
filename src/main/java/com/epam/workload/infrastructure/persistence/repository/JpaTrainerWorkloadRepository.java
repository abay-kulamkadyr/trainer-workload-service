package com.epam.workload.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import com.epam.workload.infrastructure.persistence.dao.TrainerWorkloadDAO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaTrainerWorkloadRepository extends JpaRepository<TrainerWorkloadDAO, Long> {
    Optional<TrainerWorkloadDAO> findByUsernameAndYearAndMonth(String username, int year, int month);

    @Query("SELECT t FROM TrainerWorkloadDAO t WHERE t.username = :username ORDER BY t.year, t.month")
    List<TrainerWorkloadDAO> findByUsernameOrderedByYearAndMonth(String username);
}
