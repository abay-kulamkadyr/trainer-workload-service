package com.epam.workload.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.epam.workload.application.dto.request.ActionType;
import com.epam.workload.application.dto.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.application.dto.response.TrainerSummaryResponse;
import com.epam.workload.application.exception.InsufficientDurationException;
import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.domain.port.TrainerWorkloadRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainerWorkloadServiceImpl Tests")
class TrainerWorkloadServiceImplTest {

    @Mock
    private TrainerWorkloadRepository repository;

    @InjectMocks
    private TrainerWorkloadServiceImpl service;

    private static UpdateTrainerWorkloadCommand command(ActionType actionType, int duration, int year, int month) {
        return new UpdateTrainerWorkloadCommand(
                "john.smith", "John", "Smith", true, LocalDateTime.of(year, month, 15, 10, 0), duration, actionType);
    }

    private static TrainerWorkload workloadWith(Year year, Month month, int duration) {
        Map<Month, Integer> months = new EnumMap<>(Month.class);
        months.put(month, duration);
        Map<Year, Map<Month, Integer>> yearMap = new HashMap<>();
        yearMap.put(year, months);
        return TrainerWorkload.builder()
                .id("1")
                .username("john.smith")
                .firstName("John")
                .lastName("Smith")
                .active(true)
                .yearMonthDuration(yearMap)
                .build();
    }

    private int getDuration(TrainerWorkload workload, Year year, Month month) {
        return workload.getYearMonthDuration().get(year).get(month);
    }

    @Nested
    @DisplayName("ADD operation")
    class AddTests {

        @Test
        @DisplayName("Should add duration to existing year and month entry")
        void shouldAddToExistingEntry() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2025), Month.JANUARY, 100)));

            TrainerWorkload result = service.processRequest(command(ActionType.ADD, 60, 2025, 1));

            assertThat(getDuration(result, Year.of(2025), Month.JANUARY)).isEqualTo(160);
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Should create new month entry under existing year")
        void shouldCreateNewMonthUnderExistingYear() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2025), Month.JANUARY, 100)));

            TrainerWorkload result = service.processRequest(command(ActionType.ADD, 50, 2025, 2));

            assertThat(getDuration(result, Year.of(2025), Month.JANUARY)).isEqualTo(100);
            assertThat(getDuration(result, Year.of(2025), Month.FEBRUARY)).isEqualTo(50);
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Should create new year and month entry when year does not exist")
        void shouldCreateNewYearEntry() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2024), Month.DECEMBER, 100)));

            TrainerWorkload result = service.processRequest(command(ActionType.ADD, 75, 2025, 1));

            assertThat(getDuration(result, Year.of(2024), Month.DECEMBER)).isEqualTo(100);
            assertThat(getDuration(result, Year.of(2025), Month.JANUARY)).isEqualTo(75);
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Should create brand new trainer document when none exists")
        void shouldCreateNewDocumentWhenNoneExists() {
            when(repository.findByUsername("john.smith")).thenReturn(Optional.empty());

            TrainerWorkload result = service.processRequest(command(ActionType.ADD, 60, 2025, 1));

            assertThat(result.getUsername()).isEqualTo("john.smith");
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Smith");
            assertThat(result.getActive()).isTrue();
            assertThat(getDuration(result, Year.of(2025), Month.JANUARY)).isEqualTo(60);
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Should add to zero duration")
        void shouldAddToZeroDuration() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2025), Month.JANUARY, 0)));

            TrainerWorkload result = service.processRequest(command(ActionType.ADD, 30, 2025, 1));

            assertThat(getDuration(result, Year.of(2025), Month.JANUARY)).isEqualTo(30);
        }

        @Test
        @DisplayName("Should accumulate multiple ADD calls on same month")
        void shouldAccumulateMultipleAdds() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2025), Month.JANUARY, 0)))
                    .thenReturn(Optional.of(workloadWith(Year.of(2025), Month.JANUARY, 60)));

            service.processRequest(command(ActionType.ADD, 60, 2025, 1));
            TrainerWorkload result = service.processRequest(command(ActionType.ADD, 40, 2025, 1));

            assertThat(getDuration(result, Year.of(2025), Month.JANUARY)).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("DELETE operation")
    class DeleteTests {

        @Test
        @DisplayName("Should subtract duration from existing entry")
        void shouldSubtractFromExistingEntry() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2025), Month.JANUARY, 100)));

            TrainerWorkload result = service.processRequest(command(ActionType.DELETE, 40, 2025, 1));

            assertThat(getDuration(result, Year.of(2025), Month.JANUARY)).isEqualTo(60);
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Should allow deleting exact available duration leaving zero")
        void shouldAllowDeletingExactDuration() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2025), Month.JANUARY, 100)));

            TrainerWorkload result = service.processRequest(command(ActionType.DELETE, 100, 2025, 1));

            assertThat(getDuration(result, Year.of(2025), Month.JANUARY)).isEqualTo(0);
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Should throw InsufficientDurationException when subtracting more than available")
        void shouldThrowWhenSubtractingMoreThanAvailable() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2025), Month.JANUARY, 100)));

            assertThatThrownBy(() -> service.processRequest(command(ActionType.DELETE, 150, 2025, 1)))
                    .isInstanceOf(InsufficientDurationException.class)
                    .hasMessageContaining("150");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InsufficientDurationException when subtracting from zero")
        void shouldThrowWhenSubtractingFromZero() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2025), Month.JANUARY, 0)));

            assertThatThrownBy(() -> service.processRequest(command(ActionType.DELETE, 10, 2025, 1)))
                    .isInstanceOf(InsufficientDurationException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when year entry does not exist")
        void shouldThrowWhenYearNotFound() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2024), Month.JANUARY, 100)));

            assertThatThrownBy(() -> service.processRequest(command(ActionType.DELETE, 50, 2025, 1)))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("2025");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when month entry does not exist")
        void shouldThrowWhenMonthNotFound() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2025), Month.FEBRUARY, 100)));

            assertThatThrownBy(() -> service.processRequest(command(ActionType.DELETE, 50, 2025, 1)))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("JANUARY");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when no trainer document exists")
        void shouldThrowWhenNoDocumentExists() {
            when(repository.findByUsername("john.smith")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processRequest(command(ActionType.DELETE, 50, 2025, 1)))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getTrainerSummary")
    class GetTrainerSummaryTests {

        @Test
        @DisplayName("Should return summary for trainer with single year and month")
        void shouldReturnSummaryForSingleYearAndMonth() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2025), Month.JANUARY, 120)));

            TrainerSummaryResponse response = service.getTrainerSummary("john.smith");

            assertThat(response.username()).isEqualTo("john.smith");
            assertThat(response.firstName()).isEqualTo("John");
            assertThat(response.lastName()).isEqualTo("Smith");
            assertThat(response.status()).isTrue();
            assertThat(response.years()).hasSize(1);
            assertThat(response.years().getFirst().year()).isEqualTo(Year.of(2025));
            assertThat(response.years().getFirst().months()).hasSize(1);
            assertThat(response.years().getFirst().months().getFirst().month()).isEqualTo(Month.JANUARY);
            assertThat(response.years().getFirst().months().getFirst().trainingSummaryDuration())
                    .isEqualTo(120);
        }

        @Test
        @DisplayName("Should return summary with multiple months in same year")
        void shouldReturnSummaryWithMultipleMonths() {
            Map<Month, Integer> months = new EnumMap<>(Month.class);
            months.put(Month.JANUARY, 120);
            months.put(Month.FEBRUARY, 90);
            months.put(Month.MARCH, 150);
            Map<Year, Map<Month, Integer>> yearMap = new HashMap<>();
            yearMap.put(Year.of(2025), months);

            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(TrainerWorkload.builder()
                            .username("john.smith")
                            .firstName("John")
                            .lastName("Smith")
                            .active(true)
                            .yearMonthDuration(yearMap)
                            .build()));

            TrainerSummaryResponse response = service.getTrainerSummary("john.smith");

            assertThat(response.years()).hasSize(1);
            assertThat(response.years().getFirst().months()).hasSize(3);
        }

        @Test
        @DisplayName("Should return summary with multiple years")
        void shouldReturnSummaryWithMultipleYears() {
            Map<Month, Integer> months2024 = new EnumMap<>(Month.class);
            months2024.put(Month.DECEMBER, 100);
            Map<Month, Integer> months2025 = new EnumMap<>(Month.class);
            months2025.put(Month.JANUARY, 120);
            Map<Year, Map<Month, Integer>> yearMap = new HashMap<>();
            yearMap.put(Year.of(2024), months2024);
            yearMap.put(Year.of(2025), months2025);

            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(TrainerWorkload.builder()
                            .username("john.smith")
                            .firstName("John")
                            .lastName("Smith")
                            .active(true)
                            .yearMonthDuration(yearMap)
                            .build()));

            TrainerSummaryResponse response = service.getTrainerSummary("john.smith");

            assertThat(response.years()).hasSize(2);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when trainer has no document")
        void shouldThrowWhenTrainerNotFound() {
            when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTrainerSummary("ghost"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("ghost");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should not mutate the original workload map on ADD")
        void shouldNotMutateOriginalOnAdd() {
            TrainerWorkload original = workloadWith(Year.of(2025), Month.JANUARY, 100);
            when(repository.findByUsername("john.smith")).thenReturn(Optional.of(original));

            service.processRequest(command(ActionType.ADD, 50, 2025, 1));

            // original map should be untouched
            assertThat(original.getYearMonthDuration().get(Year.of(2025)).get(Month.JANUARY))
                    .isEqualTo(100);
        }

        @Test
        @DisplayName("Should not mutate the original workload map on DELETE")
        void shouldNotMutateOriginalOnDelete() {
            TrainerWorkload original = workloadWith(Year.of(2025), Month.JANUARY, 100);
            when(repository.findByUsername("john.smith")).thenReturn(Optional.of(original));

            service.processRequest(command(ActionType.DELETE, 40, 2025, 1));

            assertThat(original.getYearMonthDuration().get(Year.of(2025)).get(Month.JANUARY))
                    .isEqualTo(100);
        }

        @Test
        @DisplayName("Should handle large duration values without overflow")
        void shouldHandleLargeDurationValues() {
            when(repository.findByUsername("john.smith"))
                    .thenReturn(Optional.of(workloadWith(Year.of(2025), Month.JANUARY, 50)));

            UpdateTrainerWorkloadCommand cmd = new UpdateTrainerWorkloadCommand(
                    "john.smith",
                    "John",
                    "Smith",
                    true,
                    LocalDateTime.of(2025, 1, 15, 10, 0),
                    Integer.MAX_VALUE - 100,
                    ActionType.ADD);

            TrainerWorkload result = service.processRequest(cmd);

            assertThat(getDuration(result, Year.of(2025), Month.JANUARY)).isEqualTo(Integer.MAX_VALUE - 50);
        }
    }
}
