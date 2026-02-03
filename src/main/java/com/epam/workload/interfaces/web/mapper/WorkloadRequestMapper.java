package com.epam.workload.interfaces.web.mapper;

import com.epam.workload.application.service.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.interfaces.web.dto.request.TrainerWorkloadWebRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkloadRequestMapper {

    UpdateTrainerWorkloadCommand toServiceRequest(TrainerWorkloadWebRequest webRequest);
}
