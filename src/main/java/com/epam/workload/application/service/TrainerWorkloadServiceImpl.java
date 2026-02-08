package com.epam.workload.application.service;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import com.epam.workload.application.dto.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.application.dto.response.MonthSummaryDTO;
import com.epam.workload.application.dto.response.TrainerSummaryResponse;
import com.epam.workload.application.dto.response.YearSummaryDTO;
import com.epam.workload.application.exception.InsufficientDurationException;
import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.domain.port.TrainerWorkloadRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TrainerWorkloadServiceImpl implements TrainerWorkloadService {
    private final TrainerWorkloadRepository repository;

    @Autowired
    TrainerWorkloadServiceImpl(TrainerWorkloadRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public TrainerWorkload processRequest(UpdateTrainerWorkloadCommand request) {
        log.debug("Processing {} request for trainer: {}", request.actionType(), request.trainerUsername());

        TrainerWorkload workload = repository
                .findByUsernameAndYearAndMonth(
                        request.trainerUsername(),
                        Year.of(request.trainingDate().getYear()),
                        request.trainingDate().getMonth())
                .orElseGet(() -> createNewWorkload(request));

        switch (request.actionType()) {
            case ADD -> workload = workload.withTrainingDurationMinutes(
                    workload.getTrainingDurationMinutes() + request.trainingDurationMinutes());
            case DELETE -> workload = decreaseWorkloadDuration(workload, request.trainingDurationMinutes());
        }

        repository.save(workload);
        return workload;
    }

    @Override
    @Transactional(readOnly = true)
    public TrainerSummaryResponse getTrainerSummary(String username) {
        List<TrainerWorkload> workloads = repository.getTrainerWorkloadsOrderedByYearAndMonth(username);

        if (workloads == null || workloads.isEmpty()) {
            throw new EntityNotFoundException("No workload found for trainer: " + username);
        }

        return mapToResponse(workloads);
    }

    /**
     * Transforms workload data in single-pass. Since data is already ordered by year/month from the database, we avoid
     * multiple streaming passes
     */
    private TrainerSummaryResponse mapToResponse(List<TrainerWorkload> workloads) {
        TrainerWorkload firstWorkload = workloads.getFirst();
        List<YearSummaryDTO> yearsSummaries = new ArrayList<>();

        Year currentYear = null;
        List<MonthSummaryDTO> currentMonths = null;

        for (var workload : workloads) {
            if (!workload.getYear().equals(currentYear)) {
                // Save previous years data if it exists
                if (currentYear != null) {
                    yearsSummaries.add(new YearSummaryDTO(currentYear, currentMonths));
                }
                currentYear = workload.getYear();
                currentMonths = new ArrayList<>();
            }

            currentMonths.add(new MonthSummaryDTO(workload.getMonth(), workload.getTrainingDurationMinutes()));
        }

        // Add the last year
        if (currentYear != null) {
            yearsSummaries.add(new YearSummaryDTO(currentYear, currentMonths));
        }

        return new TrainerSummaryResponse(
                firstWorkload.getUsername(),
                firstWorkload.getFirstName(),
                firstWorkload.getLastName(),
                firstWorkload.getActive(),
                yearsSummaries);
    }

    private TrainerWorkload decreaseWorkloadDuration(TrainerWorkload workload, Integer trainingDurationMinutes) {
        if (workload.getTrainingDurationMinutes() - trainingDurationMinutes < 0) {
            throw new InsufficientDurationException(String.format(
                    "Cannot decrease workload by %d minutes from Trainer %s, year %s, month %s. Current workload duration: %d minutes",
                    trainingDurationMinutes,
                    workload.getUsername(),
                    workload.getYear(),
                    workload.getMonth(),
                    workload.getTrainingDurationMinutes()));
        }
        return workload.withTrainingDurationMinutes(workload.getTrainingDurationMinutes() - trainingDurationMinutes);
    }

    private TrainerWorkload createNewWorkload(UpdateTrainerWorkloadCommand request) {
        return TrainerWorkload.builder()
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
