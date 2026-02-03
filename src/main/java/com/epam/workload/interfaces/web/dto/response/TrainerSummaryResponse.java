package com.epam.workload.interfaces.web.dto.response;

import java.util.List;

public record TrainerSummaryResponse(String username, String firstName, String lastName, boolean status,
        List<YearSummaryDTO> years) {}
