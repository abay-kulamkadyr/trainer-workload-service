package com.epam.workload.application.service;

import com.epam.workload.application.service.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.domain.model.TrainerWorkload;

public interface TrainerWorkloadService {
    TrainerWorkload processRequest(UpdateTrainerWorkloadCommand request);
}
