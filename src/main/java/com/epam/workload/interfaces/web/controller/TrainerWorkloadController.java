package com.epam.workload.interfaces.web.controller;

import com.epam.workload.application.dto.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.application.service.TrainerWorkloadService;
import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.interfaces.web.dto.request.TrainerWorkloadWebRequest;
import com.epam.workload.interfaces.web.dto.response.TrainerSummaryResponse;
import com.epam.workload.interfaces.web.dto.response.TrainerWorkloadResponse;
import com.epam.workload.interfaces.web.mapper.WorkloadRequestMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workload")
public class TrainerWorkloadController implements TrainerWorkloadControllerApi {
    private final TrainerWorkloadService workloadService;
    private final WorkloadRequestMapper mapper;

    @Autowired
    TrainerWorkloadController(TrainerWorkloadService workloadService, WorkloadRequestMapper mapper) {
        this.workloadService = workloadService;
        this.mapper = mapper;
    }

    @Override
    @PostMapping
    public ResponseEntity<TrainerWorkloadResponse> processTrainerRequest(
            @Valid @RequestBody TrainerWorkloadWebRequest request) {
        UpdateTrainerWorkloadCommand serviceRequest = mapper.toUpdateWorkloadCommand(request);
        TrainerWorkload workloadResponse = workloadService.processRequest(serviceRequest);
        return ResponseEntity.ok(toResponse(workloadResponse));
    }

    @Override
    @GetMapping("/{username}")
    public ResponseEntity<TrainerSummaryResponse> getTrainerSummary(@PathVariable String username) {
        return ResponseEntity.ok(mapper.toTrainerSummaryResponse(workloadService.getTrainerSummary(username)));
    }

    private TrainerWorkloadResponse toResponse(TrainerWorkload workload) {
        return new TrainerWorkloadResponse(workload.getUsername(),
                workload.getYear().toString(),
                workload.getMonth().toString(),
                workload.getTrainingDurationMinutes());
    }

}
