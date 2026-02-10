package com.epam.workload.interfaces.web.mapper;

import com.epam.workload.application.dto.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.application.dto.response.TrainerSummaryResponse;
import com.epam.workload.interfaces.web.dto.request.TrainerWorkloadWebRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkloadRequestMapper {

    UpdateTrainerWorkloadCommand toUpdateWorkloadCommand(TrainerWorkloadWebRequest webRequest);

    com.epam.workload.interfaces.web.dto.response.TrainerSummaryResponse toTrainerSummaryResponse(
            TrainerSummaryResponse serviceResponse);
}
