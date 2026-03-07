package com.epam.workload.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;

import com.epam.workload.application.dto.request.ActionType;
import com.epam.workload.application.dto.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.application.dto.response.TrainerSummaryResponse;
import com.epam.workload.application.exception.EntityNotFoundException;
import com.epam.workload.application.exception.InsufficientDurationException;
import com.epam.workload.application.service.TrainerWorkloadService;
import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.infrastructure.persistence.repository.TrainerWorkloadMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrainerWorkloadServiceImplIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TrainerWorkloadService workloadService;

    @Autowired
    private TrainerWorkloadMongoRepository mongoRepository;

    @BeforeEach
    void setUp() {
        mongoRepository.deleteAll();
    }

    @Test
    void add_createsNewDocument_whenTrainerDoesNotExist() {
        UpdateTrainerWorkloadCommand command = addCommand("john.doe", 60, LocalDateTime.of(2024, 6, 1, 9, 0));

        TrainerWorkload result = workloadService.processRequest(command);

        assertThat(mongoRepository.findByUsername("john.doe")).isPresent();
        assertThat(result.getUsername()).isEqualTo("john.doe");
        assertThat(result.getYearMonthDuration()).containsKey(Year.of(2024));
        assertThat(result.getYearMonthDuration().get(Year.of(2024))).containsEntry(Month.JUNE, 60);
    }

    @Test
    void add_accumulatesDuration_whenEntryAlreadyExists() {
        UpdateTrainerWorkloadCommand first = addCommand("john.doe", 60, LocalDateTime.of(2024, 6, 1, 9, 0));
        UpdateTrainerWorkloadCommand second = addCommand("john.doe", 45, LocalDateTime.of(2024, 6, 15, 10, 0));

        workloadService.processRequest(first);
        workloadService.processRequest(second);

        TrainerWorkload saved =
                workloadService.processRequest(addCommand("john.doe", 0, LocalDateTime.of(2024, 6, 20, 8, 0)));
        TrainerSummaryResponse summary = workloadService.getTrainerSummary("john.doe");

        int juneTotal = summary.years().stream()
                .filter(y -> y.year().equals(Year.of(2024)))
                .flatMap(y -> y.months().stream())
                .filter(m -> m.month() == Month.JUNE)
                .mapToInt(m -> (int) m.trainingSummaryDuration())
                .sum();

        assertThat(juneTotal).isEqualTo(105);
    }

    @Test
    void add_keepsEntriesSeparate_acrossYearsAndMonths() {
        workloadService.processRequest(addCommand("jane.smith", 30, LocalDateTime.of(2023, 11, 5, 8, 0)));
        workloadService.processRequest(addCommand("jane.smith", 90, LocalDateTime.of(2024, 3, 20, 8, 0)));

        TrainerSummaryResponse summary = workloadService.getTrainerSummary("jane.smith");

        assertThat(summary.years()).hasSize(2);

        int nov2023 = durationFor(summary, 2023, Month.NOVEMBER);
        int mar2024 = durationFor(summary, 2024, Month.MARCH);

        assertThat(nov2023).isEqualTo(30);
        assertThat(mar2024).isEqualTo(90);
    }

    // --- processRequest: DELETE ---

    @Test
    void delete_reducesDuration_whenEntryExists() {
        workloadService.processRequest(addCommand("john.doe", 90, LocalDateTime.of(2024, 6, 1, 9, 0)));

        workloadService.processRequest(deleteCommand("john.doe", 30, LocalDateTime.of(2024, 6, 1, 9, 0)));

        TrainerSummaryResponse summary = workloadService.getTrainerSummary("john.doe");
        assertThat(durationFor(summary, 2024, Month.JUNE)).isEqualTo(60);
    }

    @Test
    void delete_throws_whenResultWouldGoBelowZero() {
        workloadService.processRequest(addCommand("john.doe", 30, LocalDateTime.of(2024, 6, 1, 9, 0)));

        UpdateTrainerWorkloadCommand overSubtract = deleteCommand("john.doe", 60, LocalDateTime.of(2024, 6, 1, 9, 0));

        assertThatThrownBy(() -> workloadService.processRequest(overSubtract))
                .isInstanceOf(InsufficientDurationException.class)
                .hasMessageContaining("john.doe");
    }

    @Test
    void delete_throws_whenMonthEntryDoesNotExist() {
        workloadService.processRequest(addCommand("john.doe", 60, LocalDateTime.of(2024, 6, 1, 9, 0)));

        UpdateTrainerWorkloadCommand wrongMonth = deleteCommand("john.doe", 30, LocalDateTime.of(2024, 7, 1, 9, 0));

        assertThatThrownBy(() -> workloadService.processRequest(wrongMonth))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("john.doe");
    }

    @Test
    void delete_throws_whenYearEntryDoesNotExist() {
        workloadService.processRequest(addCommand("john.doe", 60, LocalDateTime.of(2024, 6, 1, 9, 0)));

        UpdateTrainerWorkloadCommand wrongYear = deleteCommand("john.doe", 30, LocalDateTime.of(2025, 6, 1, 9, 0));

        assertThatThrownBy(() -> workloadService.processRequest(wrongYear))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("john.doe");
    }

    @Test
    void delete_doesNotMutateOtherMonths() {
        workloadService.processRequest(addCommand("john.doe", 60, LocalDateTime.of(2024, 5, 1, 9, 0)));
        workloadService.processRequest(addCommand("john.doe", 90, LocalDateTime.of(2024, 6, 1, 9, 0)));

        workloadService.processRequest(deleteCommand("john.doe", 30, LocalDateTime.of(2024, 6, 1, 9, 0)));

        TrainerSummaryResponse summary = workloadService.getTrainerSummary("john.doe");
        assertThat(durationFor(summary, 2024, Month.MAY)).isEqualTo(60); // untouched
        assertThat(durationFor(summary, 2024, Month.JUNE)).isEqualTo(60); // reduced
    }

    @Test
    void getTrainerSummary_throws_whenTrainerDoesNotExist() {
        assertThatThrownBy(() -> workloadService.getTrainerSummary("nobody"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("nobody");
    }

    @Test
    void getTrainerSummary_returnsCorrectPersonalDetails() {
        workloadService.processRequest(addCommand("jane.smith", 60, LocalDateTime.of(2024, 6, 1, 9, 0)));

        TrainerSummaryResponse summary = workloadService.getTrainerSummary("jane.smith");

        assertThat(summary.username()).isEqualTo("jane.smith");
        assertThat(summary.firstName()).isEqualTo("Jane");
        assertThat(summary.lastName()).isEqualTo("Smith");
        assertThat(summary.status()).isTrue();
    }

    private UpdateTrainerWorkloadCommand addCommand(String username, int minutes, LocalDateTime date) {
        return new UpdateTrainerWorkloadCommand(username, "Jane", "Smith", true, date, minutes, ActionType.ADD);
    }

    private UpdateTrainerWorkloadCommand deleteCommand(String username, int minutes, LocalDateTime date) {
        return new UpdateTrainerWorkloadCommand(username, "Jane", "Smith", true, date, minutes, ActionType.DELETE);
    }

    private int durationFor(TrainerSummaryResponse summary, int year, Month month) {
        return summary.years().stream()
                .filter(y -> y.year().equals(Year.of(year)))
                .flatMap(y -> y.months().stream())
                .filter(m -> m.month() == month)
                .mapToInt(m -> (int) m.trainingSummaryDuration())
                .findFirst()
                .orElse(0);
    }
}
