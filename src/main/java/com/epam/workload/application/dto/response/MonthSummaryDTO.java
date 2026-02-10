package com.epam.workload.application.dto.response;

import java.time.Month;

public record MonthSummaryDTO(Month month, long trainingSummaryDuration) {}
