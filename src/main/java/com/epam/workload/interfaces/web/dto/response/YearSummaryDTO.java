package com.epam.workload.interfaces.web.dto.response;

import java.time.Year;
import java.util.List;

public record YearSummaryDTO(Year year, List<MonthSummaryDTO> months) {}
