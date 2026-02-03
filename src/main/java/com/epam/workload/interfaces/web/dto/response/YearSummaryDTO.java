package com.epam.workload.interfaces.web.dto.response;

import java.util.List;

public record YearSummaryDTO(int year, List<MonthSummaryDTO> months) {

}
