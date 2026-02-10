package com.epam.workload.interfaces.web.controller;

import com.epam.workload.interfaces.web.dto.request.TrainerWorkloadWebRequest;
import com.epam.workload.interfaces.web.dto.response.TrainerSummaryResponse;
import com.epam.workload.interfaces.web.dto.response.TrainerWorkloadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Trainer Workload", description = "Trainer workload management API")
interface TrainerWorkloadControllerApi {
    @Operation(summary = "Update trainer workload", description = "Add or delete training hours for a trainer")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Workload updated successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "422", description = "Insufficient duration for DELETE")
            })
    ResponseEntity<TrainerWorkloadResponse> processTrainerRequest(TrainerWorkloadWebRequest request);

    @Operation(summary = "Get trainer workload summary")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Workload is retrieved successfully"),
                @ApiResponse(responseCode = "404", description = "Trainer is not found")
            })
    ResponseEntity<TrainerSummaryResponse> getTrainerSummary(String username);
}
