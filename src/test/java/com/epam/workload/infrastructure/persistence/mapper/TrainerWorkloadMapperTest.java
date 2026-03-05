package com.epam.workload.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Month;
import java.time.Year;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.infrastructure.persistence.dao.TrainerWorkloadDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrainerWorkloadMapperTest {

    private TrainerWorkloadMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TrainerWorkloadMapper();
    }

    @Test
    void toDomain_mapsCorrectly() {

        TrainerWorkloadDAO.MonthSummaryDAO jan = TrainerWorkloadDAO.MonthSummaryDAO.builder()
                .month(1)
                .durationMin(120)
                .build();

        TrainerWorkloadDAO.MonthSummaryDAO feb = TrainerWorkloadDAO.MonthSummaryDAO.builder()
                .month(2)
                .durationMin(200)
                .build();

        TrainerWorkloadDAO.YearSummaryDAO year = TrainerWorkloadDAO.YearSummaryDAO.builder()
                .year(2024)
                .months(List.of(jan, feb))
                .build();

        TrainerWorkloadDAO dao = TrainerWorkloadDAO.builder()
                .id("1")
                .username("trainer1")
                .firstName("John")
                .lastName("Doe")
                .active(true)
                .years(List.of(year))
                .build();

        TrainerWorkload result = mapper.toDomain(dao);

        assertEquals("1", result.getId());
        assertEquals("trainer1", result.getUsername());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertTrue(result.getActive());

        Map<Month, Integer> months = result.getYearMonthDuration().get(Year.of(2024));

        assertEquals(120, months.get(Month.JANUARY));
        assertEquals(200, months.get(Month.FEBRUARY));
    }

    @Test
    void toEntity_mapsCorrectly() {

        Map<Month, Integer> months = new EnumMap<>(Month.class);
        months.put(Month.JANUARY, 100);
        months.put(Month.MARCH, 300);

        Map<Year, Map<Month, Integer>> map = Map.of(Year.of(2023), months);

        TrainerWorkload domain = TrainerWorkload.builder()
                .id("10")
                .username("trainerX")
                .firstName("Jane")
                .lastName("Smith")
                .active(true)
                .yearMonthDuration(map)
                .build();

        TrainerWorkloadDAO dao = mapper.toEntity(domain);

        assertEquals("10", dao.getId());
        assertEquals("trainerX", dao.getUsername());
        assertEquals("Jane", dao.getFirstName());
        assertEquals("Smith", dao.getLastName());
        assertTrue(dao.isActive());

        TrainerWorkloadDAO.YearSummaryDAO year = dao.getYears().get(0);
        assertEquals(2023, year.getYear());

        Map<Integer, Integer> monthMap = year.getMonths().stream()
                .collect(java.util.stream.Collectors.toMap(
                        TrainerWorkloadDAO.MonthSummaryDAO::getMonth,
                        TrainerWorkloadDAO.MonthSummaryDAO::getDurationMin));

        assertEquals(100, monthMap.get(1));
        assertEquals(300, monthMap.get(3));
    }

    @Test
    void toEntity_whenYearMonthDurationNull_returnsEmptyYears() {

        TrainerWorkload domain = TrainerWorkload.builder()
                .id("5")
                .username("trainer")
                .firstName("A")
                .lastName("B")
                .active(false)
                .yearMonthDuration(null)
                .build();

        TrainerWorkloadDAO dao = mapper.toEntity(domain);

        assertNotNull(dao.getYears());
        assertTrue(dao.getYears().isEmpty());
    }

    @Test
    void roundTrip_domainToEntityToDomain() {

        Map<Month, Integer> months = Map.of(
                Month.JANUARY, 50,
                Month.FEBRUARY, 70);

        TrainerWorkload domain = TrainerWorkload.builder()
                .id("1")
                .username("user")
                .firstName("A")
                .lastName("B")
                .active(true)
                .yearMonthDuration(Map.of(Year.of(2024), months))
                .build();

        TrainerWorkloadDAO entity = mapper.toEntity(domain);
        TrainerWorkload result = mapper.toDomain(entity);

        assertEquals(domain.getUsername(), result.getUsername());
        assertEquals(50, result.getYearMonthDuration().get(Year.of(2024)).get(Month.JANUARY));
    }
}
