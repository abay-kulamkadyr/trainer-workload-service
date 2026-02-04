package com.epam.workload.application.service;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.workload.application.dto.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.application.dto.response.MonthSummaryDTO;
import com.epam.workload.application.dto.response.TrainerSummaryResponse;
import com.epam.workload.application.dto.response.YearSummaryDTO;
import com.epam.workload.application.exception.InsufficientDurationException;
import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.domain.port.TrainerWorkloadRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
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

    @Override
    public TrainerSummaryResponse getTrainerSummary(String username) {
        List<TrainerWorkload> workloads = repository.getTrainerWorkloads(username);
        if (workloads == null || workloads.isEmpty()) {
            throw new EntityNotFoundException("No workload found for trainer: " + workloads);
        }
        return mapToResponse(workloads);
    }

    private TrainerSummaryResponse mapToResponse(@NonNull List<TrainerWorkload> workloads) {
        // Group by Year, then map to MonthSummaryDTO
        var yearsList = workloads
                .stream()
                .collect(
                    Collectors
                            .groupingBy(
                                TrainerWorkload::getYear,
                                Collectors
                                        .mapping(
                                            w -> new MonthSummaryDTO(w.getMonth().name(),
                                                    w.getTrainingDurationMinutes()),
                                            Collectors.toList())))
                .entrySet()
                .stream()
                .map(entry -> new YearSummaryDTO(entry.getKey().getValue(), entry.getValue()))
                .toList();

        TrainerWorkload info = workloads.getFirst();
        return new TrainerSummaryResponse(info
                .getUsername(), info.getFirstName(), info.getLastName(), info.getActive(), yearsList);
    }

    private TrainerWorkload createNewWorkload(UpdateTrainerWorkloadCommand request) {
        return TrainerWorkload
                .builder()
                .username(request.trainerUsername())
                .firstName(request.trainerFirstname())
                .lastName(request.trainerLastname())
                .active(request.isActive())
                .year(Year.of(request.trainingDate().getYear()))
                .month(request.trainingDate().getMonth())
                .trainingDurationMinutes(0)
                .build();
    }
}
