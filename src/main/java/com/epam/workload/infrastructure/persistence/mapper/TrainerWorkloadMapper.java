package com.epam.workload.infrastructure.persistence.mapper;

import java.time.Month;
import java.time.Year;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.infrastructure.persistence.dao.TrainerWorkloadDAO;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class TrainerWorkloadMapper {

    public TrainerWorkload toDomain(@NonNull TrainerWorkloadDAO entity) {
        Map<Year, Map<Month, Integer>> yearMonthDuration = new HashMap<>();
        for (TrainerWorkloadDAO.YearSummaryDAO yearDAO : entity.getYears()) {
            Map<Month, Integer> months = new EnumMap<>(Month.class);
            for (TrainerWorkloadDAO.MonthSummaryDAO monthDAO : yearDAO.getMonths()) {
                months.put(Month.of(monthDAO.getMonth()), monthDAO.getDurationMin());
            }
            yearMonthDuration.put(Year.of(yearDAO.getYear()), months);
        }

        return TrainerWorkload.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .active(entity.isActive())
                .yearMonthDuration(yearMonthDuration)
                .build();
    }

    public TrainerWorkloadDAO toEntity(@NonNull TrainerWorkload domain) {
        List<TrainerWorkloadDAO.YearSummaryDAO> years = (domain.getYearMonthDuration() == null)
                ? List.of()
                : domain.getYearMonthDuration().entrySet().stream()
                        .map(ye -> TrainerWorkloadDAO.YearSummaryDAO.builder()
                                .year(ye.getKey().getValue())
                                .months(ye.getValue().entrySet().stream()
                                        .map(me -> TrainerWorkloadDAO.MonthSummaryDAO.builder()
                                                .month(me.getKey().getValue())
                                                .durationMin(me.getValue())
                                                .build())
                                        .toList())
                                .build())
                        .toList();

        return TrainerWorkloadDAO.builder()
                .id(domain.getId())
                .username(domain.getUsername())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .isActive(domain.getActive())
                .years(years)
                .build();
    }
}
