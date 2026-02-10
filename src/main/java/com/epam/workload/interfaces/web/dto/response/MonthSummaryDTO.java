package com.epam.workload.interfaces.web.dto.response;

import java.time.Month;

public record MonthSummaryDTO(Month month, long trainingSummaryDuration) {}
