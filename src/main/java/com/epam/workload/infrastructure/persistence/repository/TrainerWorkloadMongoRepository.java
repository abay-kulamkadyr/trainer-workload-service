package com.epam.workload.infrastructure.persistence.repository;

import java.util.Optional;

import com.epam.workload.infrastructure.persistence.dao.TrainerWorkloadDAO;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TrainerWorkloadMongoRepository extends MongoRepository<TrainerWorkloadDAO, String> {
    Optional<TrainerWorkloadDAO> findByUsername(String username);
}
