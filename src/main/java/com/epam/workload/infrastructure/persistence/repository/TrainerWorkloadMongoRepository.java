package com.epam.workload.infrastructure.persistence.repository;

import java.util.Optional;

import com.epam.workload.infrastructure.persistence.dao.TrainerWorkloadDAO;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;

@Profile("!no-integrations")
public interface TrainerWorkloadMongoRepository extends MongoRepository<TrainerWorkloadDAO, String> {
    Optional<TrainerWorkloadDAO> findByUsername(String username);
}
