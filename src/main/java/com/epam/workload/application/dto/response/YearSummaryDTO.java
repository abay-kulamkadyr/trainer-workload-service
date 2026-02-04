package com.epam.workload.application.dto.response;

import java.util.List;

public record YearSummaryDTO(int year, List<MonthSummaryDTO> months) {

}
