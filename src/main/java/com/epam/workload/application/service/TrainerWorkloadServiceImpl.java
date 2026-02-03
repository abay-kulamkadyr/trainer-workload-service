package com.epam.workload.application.service;

import java.time.Year;

import com.epam.workload.application.service.exception.InsufficientDurationException;
import com.epam.workload.application.service.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.domain.port.TrainerWorkloadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TrainerWorkloadServiceImpl implements TrainerWorkloadService {
    private final TrainerWorkloadRepository repository;

    @Autowired
    TrainerWorkloadServiceImpl(TrainerWorkloadRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public TrainerWorkload processRequest(UpdateTrainerWorkloadCommand request) {
        TrainerWorkload workload = repository
                .findByUsernameAndYearAndMonth(
                    request.trainerUsername(),
                    Year.of(request.trainingDate().getYear()),
                    request.trainingDate().getMonth())
                .orElseGet(() -> createNewWorkload(request));

        Integer duration = workload.getTrainingDurationMinutes();
        switch (request.actionType()) {
            case ADD -> workload = workload.withTrainingDurationMinutes(duration + request.trainingDurationMin());
            case DELETE -> {
                if (duration - request.trainingDurationMin() < 0) {
                    throw new InsufficientDurationException(String
                            .format(
                                "Cannot delete %d minutes from %s/%d/%s. Current duration: %d minutes",
                                request.trainingDurationMin(),
                                request.trainerUsername(),
                                request.trainingDate().getYear(),
                                request.trainingDate().getMonth(),
                                duration));
                }
                workload = workload.withTrainingDurationMinutes(duration - request.trainingDurationMin());
            }
        }

        repository.save(workload);
        return workload;
    }

    private TrainerWorkload createNewWorkload(UpdateTrainerWorkloadCommand request) {
        return TrainerWorkload
                .builder()
                .username(request.trainerUsername())
                .firstName(request.trainerFirstname())
                .lastName(request.trainerLastName())
                .active(request.isActive())
                .year(Year.of(request.trainingDate().getYear()))
                .month(request.trainingDate().getMonth())
                .trainingDurationMinutes(0)
                .build();
    }
}
