package com.epam.workload.application.service;

import java.time.Month;
import java.time.Year;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epam.workload.application.dto.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.application.dto.response.MonthSummaryDTO;
import com.epam.workload.application.dto.response.TrainerSummaryResponse;
import com.epam.workload.application.dto.response.YearSummaryDTO;
import com.epam.workload.application.exception.EntityNotFoundException;
import com.epam.workload.application.exception.InsufficientDurationException;
import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.domain.port.TrainerWorkloadRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TrainerWorkloadServiceImpl implements TrainerWorkloadService {

    private final TrainerWorkloadRepository repository;

    @Autowired
    TrainerWorkloadServiceImpl(TrainerWorkloadRepository repository) {
        this.repository = repository;
    }

    @Override
    public TrainerWorkload processRequest(UpdateTrainerWorkloadCommand request) {
        Year year = Year.of(request.trainingDate().getYear());
        Month month = request.trainingDate().getMonth();

        log.debug(
                "Processing {} request for trainer: {} [{}/{}]",
                request.actionType(),
                request.trainerUsername(),
                year,
                month);

        TrainerWorkload workload = repository
                .findByUsername(request.trainerUsername())
                .orElseGet(() -> {
                    log.debug("No existing document for trainer '{}' - creating new record", request.trainerUsername());
                    return createNewWorkload(request);
                });

        TrainerWorkload updated =
                switch (request.actionType()) {
                    case ADD -> addDuration(workload, year, month, request.trainingDurationMinutes());
                    case DELETE -> subtractDuration(workload, year, month, request.trainingDurationMinutes());
                };

        repository.save(updated);
        log.info(
                "Saved workload for trainer '{}' - action: {}, [{}/{}]",
                request.trainerUsername(),
                request.actionType(),
                year,
                month);
        return updated;
    }

    @Override
    public TrainerSummaryResponse getTrainerSummary(String username) {
        log.debug("Fetching workload summary for trainer: {}", username);

        TrainerWorkload workload = repository
                .findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("No workload found for trainer: " + username));

        return mapToResponse(workload);
    }

    private TrainerWorkload addDuration(TrainerWorkload workload, Year year, Month month, int minutes) {
        Map<Year, Map<Month, Integer>> updated = deepCopy(workload.getYearMonthDuration());
        updated.computeIfAbsent(year, y -> new EnumMap<>(Month.class)).merge(month, minutes, Integer::sum);

        log.debug("Added {} minutes to trainer '{}' [{}/{}]", minutes, workload.getUsername(), year, month);
        return workload.withYearMonthDuration(updated);
    }

    private TrainerWorkload subtractDuration(TrainerWorkload workload, Year year, Month month, int minutes) {
        Map<Year, Map<Month, Integer>> yearMap = workload.getYearMonthDuration();
        Map<Month, Integer> monthMap = yearMap.get(year);

        if (monthMap == null || !monthMap.containsKey(month)) {
            throw new EntityNotFoundException(
                    String.format("No workload entry for trainer '%s' in %s/%s", workload.getUsername(), month, year));
        }

        int current = monthMap.get(month);
        if (current - minutes < 0) {
            throw new InsufficientDurationException(String.format(
                    "Cannot subtract %d minutes from trainer '%s' in %s/%s - current is %d min",
                    minutes, workload.getUsername(), month, year, current));
        }

        Map<Year, Map<Month, Integer>> updated = deepCopy(yearMap);
        updated.get(year).put(month, current - minutes);

        log.debug("Subtracted {} minutes from trainer '{}' [{}/{}]", minutes, workload.getUsername(), year, month);
        return workload.withYearMonthDuration(updated);
    }

    private TrainerSummaryResponse mapToResponse(TrainerWorkload workload) {
        List<YearSummaryDTO> years = workload.getYearMonthDuration().entrySet().stream()
                .map(ye -> new YearSummaryDTO(
                        ye.getKey(),
                        ye.getValue().entrySet().stream()
                                .map(me -> new MonthSummaryDTO(me.getKey(), me.getValue()))
                                .toList()))
                .toList();

        return new TrainerSummaryResponse(
                workload.getUsername(), workload.getFirstName(), workload.getLastName(), workload.getActive(), years);
    }

    private TrainerWorkload createNewWorkload(UpdateTrainerWorkloadCommand request) {
        return TrainerWorkload.builder()
                .username(request.trainerUsername())
                .firstName(request.trainerFirstname())
                .lastName(request.trainerLastname())
                .active(request.isActive())
                .build();
    }

    private Map<Year, Map<Month, Integer>> deepCopy(Map<Year, Map<Month, Integer>> original) {
        Map<Year, Map<Month, Integer>> copy = new HashMap<>();
        original.forEach((year, months) -> copy.put(year, new EnumMap<>(months)));
        return copy;
    }
}
