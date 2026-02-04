package com.epam.workload.application.service;

import com.epam.workload.application.dto.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.application.dto.response.TrainerSummaryResponse;
import com.epam.workload.domain.model.TrainerWorkload;

public interface TrainerWorkloadService {
    TrainerWorkload processRequest(UpdateTrainerWorkloadCommand request);

    TrainerSummaryResponse getTrainerSummary(String username);
}
