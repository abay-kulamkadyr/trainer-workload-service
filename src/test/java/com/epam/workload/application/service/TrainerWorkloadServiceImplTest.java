package com.epam.workload.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
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
@DisplayName("TrainerWorkloadService Tests")
class TrainerWorkloadServiceImplTest {

    @Mock
    private TrainerWorkloadRepository repository;

    @InjectMocks
    private TrainerWorkloadServiceImpl service;

    private UpdateTrainerWorkloadCommand createCommand(ActionType actionType, Integer duration) {
        return new UpdateTrainerWorkloadCommand(
                "john.smith", "John", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), duration, actionType);
    }

    @Nested
    @DisplayName("ADD Operation Tests")
    class AddOperationTests {

        @Test
        @DisplayName("Should add duration to existing workload")
        void shouldAddDurationToExistingWorkload() {
            // Given
            UpdateTrainerWorkloadCommand command = createCommand(ActionType.ADD, 60);

            TrainerWorkload existingWorkload = TrainerWorkload.builder()
                    .id(1L)
                    .username("john.smith")
                    .firstName("John")
                    .lastName("Smith")
                    .active(true)
                    .year(Year.of(2025))
                    .month(Month.JANUARY)
                    .trainingDurationMinutes(100)
                    .build();

            when(repository.findByUsernameAndYearAndMonth(eq("john.smith"), eq(Year.of(2025)), eq(Month.JANUARY)))
                    .thenReturn(Optional.of(existingWorkload));

            // When
            TrainerWorkload result = service.processRequest(command);

            // Then
            assertThat(result.getTrainingDurationMinutes()).isEqualTo(160); // 100 + 60
            verify(repository).save(any(TrainerWorkload.class));
        }

        @Test
        @DisplayName("Should create new workload when none exists")
        void shouldCreateNewWorkloadWhenNoneExists() {
            // Given
            UpdateTrainerWorkloadCommand command = createCommand(ActionType.ADD, 60);

            when(repository.findByUsernameAndYearAndMonth(eq("john.smith"), eq(Year.of(2025)), eq(Month.JANUARY)))
                    .thenReturn(Optional.empty());

            // When
            TrainerWorkload result = service.processRequest(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("john.smith");
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Smith");
            assertThat(result.getActive()).isTrue();
            assertThat(result.getYear()).isEqualTo(Year.of(2025));
            assertThat(result.getMonth()).isEqualTo(Month.JANUARY);
            assertThat(result.getTrainingDurationMinutes()).isEqualTo(60); // 0 + 60
            verify(repository).save(any(TrainerWorkload.class));
        }

        @Test
        @DisplayName("Should handle adding to zero duration")
        void shouldHandleAddingToZeroDuration() {
            // Given
            UpdateTrainerWorkloadCommand command = createCommand(ActionType.ADD, 30);

            TrainerWorkload existingWorkload = TrainerWorkload.builder()
                    .username("john.smith")
                    .year(Year.of(2025))
                    .month(Month.JANUARY)
                    .trainingDurationMinutes(0)
                    .build();

            when(repository.findByUsernameAndYearAndMonth(any(), any(), any()))
                    .thenReturn(Optional.of(existingWorkload));

            // When
            TrainerWorkload result = service.processRequest(command);

            // Then
            assertThat(result.getTrainingDurationMinutes()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("DELETE Operation Tests")
    class DeleteOperationTests {

        @Test
        @DisplayName("Should subtract duration from existing workload")
        void shouldSubtractDurationFromExistingWorkload() {
            // Given
            UpdateTrainerWorkloadCommand command = createCommand(ActionType.DELETE, 40);

            TrainerWorkload existingWorkload = TrainerWorkload.builder()
                    .id(1L)
                    .username("john.smith")
                    .firstName("John")
                    .lastName("Smith")
                    .active(true)
                    .year(Year.of(2025))
                    .month(Month.JANUARY)
                    .trainingDurationMinutes(100)
                    .build();

            when(repository.findByUsernameAndYearAndMonth(eq("john.smith"), eq(Year.of(2025)), eq(Month.JANUARY)))
                    .thenReturn(Optional.of(existingWorkload));

            // When
            TrainerWorkload result = service.processRequest(command);

            // Then
            assertThat(result.getTrainingDurationMinutes()).isEqualTo(60); // 100 - 40
            verify(repository).save(any(TrainerWorkload.class));
        }

        @Test
        @DisplayName("Should throw exception when deleting more than available")
        void shouldThrowExceptionWhenDeletingMoreThanAvailable() {
            // Given
            UpdateTrainerWorkloadCommand command = createCommand(ActionType.DELETE, 150);

            TrainerWorkload existingWorkload = TrainerWorkload.builder()
                    .username("john.smith")
                    .year(Year.of(2025))
                    .month(Month.JANUARY)
                    .trainingDurationMinutes(100)
                    .build();

            when(repository.findByUsernameAndYearAndMonth(any(), any(), any()))
                    .thenReturn(Optional.of(existingWorkload));

            // When & Then
            assertThatThrownBy(() -> service.processRequest(command)).isInstanceOf(InsufficientDurationException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when deleting from zero duration")
        void shouldThrowExceptionWhenDeletingFromZeroDuration() {
            // Given
            UpdateTrainerWorkloadCommand command = createCommand(ActionType.DELETE, 10);

            TrainerWorkload existingWorkload = TrainerWorkload.builder()
                    .username("john.smith")
                    .year(Year.of(2025))
                    .month(Month.JANUARY)
                    .trainingDurationMinutes(0)
                    .build();

            when(repository.findByUsernameAndYearAndMonth(any(), any(), any()))
                    .thenReturn(Optional.of(existingWorkload));

            // When & Then
            assertThatThrownBy(() -> service.processRequest(command)).isInstanceOf(InsufficientDurationException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow deleting exact available duration")
        void shouldAllowDeletingExactAvailableDuration() {
            // Given
            UpdateTrainerWorkloadCommand command = createCommand(ActionType.DELETE, 100);

            TrainerWorkload existingWorkload = TrainerWorkload.builder()
                    .username("john.smith")
                    .year(Year.of(2025))
                    .month(Month.JANUARY)
                    .trainingDurationMinutes(100)
                    .build();

            when(repository.findByUsernameAndYearAndMonth(any(), any(), any()))
                    .thenReturn(Optional.of(existingWorkload));

            // When
            TrainerWorkload result = service.processRequest(command);

            // Then
            assertThat(result.getTrainingDurationMinutes()).isEqualTo(0);
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Should throw exception when deleting from non-existent workload")
        void shouldThrowExceptionWhenDeletingFromNonExistentWorkload() {
            // Given
            UpdateTrainerWorkloadCommand command = createCommand(ActionType.DELETE, 50);

            when(repository.findByUsernameAndYearAndMonth(any(), any(), any())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.processRequest(command)).isInstanceOf(InsufficientDurationException.class);
        }
    }

    @Nested
    @DisplayName("Get Trainer Summary Tests")
    class GetTrainerSummaryTests {

        @Test
        @DisplayName("Should return summary with single year and month")
        void shouldReturnSummaryWithSingleYearAndMonth() {
            // Given
            String username = "john.smith";

            TrainerWorkload workload = TrainerWorkload.builder()
                    .username(username)
                    .firstName("John")
                    .lastName("Smith")
                    .active(true)
                    .year(Year.of(2025))
                    .month(Month.JANUARY)
                    .trainingDurationMinutes(120)
                    .build();

            when(repository.getTrainerWorkloadsOrderedByYearAndMonth(username)).thenReturn(List.of(workload));

            // When
            TrainerSummaryResponse response = service.getTrainerSummary(username);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.username()).isEqualTo(username);
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
        void shouldReturnSummaryWithMultipleMonthsInSameYear() {
            // Given
            String username = "john.smith";

            List<TrainerWorkload> workloads = Arrays.asList(
                    TrainerWorkload.builder()
                            .username(username)
                            .firstName("John")
                            .lastName("Smith")
                            .active(true)
                            .year(Year.of(2025))
                            .month(Month.JANUARY)
                            .trainingDurationMinutes(120)
                            .build(),
                    TrainerWorkload.builder()
                            .username(username)
                            .firstName("John")
                            .lastName("Smith")
                            .active(true)
                            .year(Year.of(2025))
                            .month(Month.FEBRUARY)
                            .trainingDurationMinutes(90)
                            .build(),
                    TrainerWorkload.builder()
                            .username(username)
                            .firstName("John")
                            .lastName("Smith")
                            .active(true)
                            .year(Year.of(2025))
                            .month(Month.MARCH)
                            .trainingDurationMinutes(150)
                            .build());

            when(repository.getTrainerWorkloadsOrderedByYearAndMonth(username)).thenReturn(workloads);

            // When
            TrainerSummaryResponse response = service.getTrainerSummary(username);

            // Then
            assertThat(response.years()).hasSize(1);
            assertThat(response.years().getFirst().year()).isEqualTo(Year.of(2025));
            assertThat(response.years().getFirst().months()).hasSize(3);
        }

        @Test
        @DisplayName("Should return summary with multiple years")
        void shouldReturnSummaryWithMultipleYears() {
            // Given
            String username = "john.smith";

            List<TrainerWorkload> workloads = Arrays.asList(
                    TrainerWorkload.builder()
                            .username(username)
                            .firstName("John")
                            .lastName("Smith")
                            .active(true)
                            .year(Year.of(2024))
                            .month(Month.DECEMBER)
                            .trainingDurationMinutes(100)
                            .build(),
                    TrainerWorkload.builder()
                            .username(username)
                            .firstName("John")
                            .lastName("Smith")
                            .active(true)
                            .year(Year.of(2025))
                            .month(Month.JANUARY)
                            .trainingDurationMinutes(120)
                            .build());

            when(repository.getTrainerWorkloadsOrderedByYearAndMonth(username)).thenReturn(workloads);

            // When
            TrainerSummaryResponse response = service.getTrainerSummary(username);

            // Then
            assertThat(response.years()).hasSize(2);
        }

        @Test
        @DisplayName("Should throw exception when no workloads found")
        void shouldThrowExceptionWhenNoWorkloadsFound() {
            // Given
            String username = "nonexistent.user";
            when(repository.getTrainerWorkloadsOrderedByYearAndMonth(username)).thenReturn(List.of());

            // When & Then
            assertThatThrownBy(() -> service.getTrainerSummary(username)).isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when workloads is null")
        void shouldThrowExceptionWhenWorkloadsIsNull() {
            // Given
            String username = "john.smith";
            when(repository.getTrainerWorkloadsOrderedByYearAndMonth(username)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> service.getTrainerSummary(username)).isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very large duration values")
        void shouldHandleVeryLargeDurationValues() {
            // Given
            UpdateTrainerWorkloadCommand command = new UpdateTrainerWorkloadCommand(
                    "john.smith",
                    "John",
                    "Smith",
                    true,
                    LocalDateTime.of(2025, 1, 15, 14, 30),
                    Integer.MAX_VALUE - 100,
                    ActionType.ADD);

            TrainerWorkload existingWorkload = TrainerWorkload.builder()
                    .username("john.smith")
                    .year(Year.of(2025))
                    .month(Month.JANUARY)
                    .trainingDurationMinutes(50)
                    .build();

            when(repository.findByUsernameAndYearAndMonth(any(), any(), any()))
                    .thenReturn(Optional.of(existingWorkload));

            // When
            TrainerWorkload result = service.processRequest(command);

            // Then
            assertThat(result.getTrainingDurationMinutes()).isEqualTo(Integer.MAX_VALUE - 50);
        }

        @Test
        @DisplayName("Should handle workload for different months")
        void shouldHandleWorkloadForDifferentMonths() {
            // Given
            UpdateTrainerWorkloadCommand januaryCommand = new UpdateTrainerWorkloadCommand(
                    "john.smith", "John", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), 100, ActionType.ADD);

            UpdateTrainerWorkloadCommand februaryCommand = new UpdateTrainerWorkloadCommand(
                    "john.smith", "John", "Smith", true, LocalDateTime.of(2025, 2, 15, 14, 30), 150, ActionType.ADD);

            when(repository.findByUsernameAndYearAndMonth(eq("john.smith"), eq(Year.of(2025)), eq(Month.JANUARY)))
                    .thenReturn(Optional.empty());

            when(repository.findByUsernameAndYearAndMonth(eq("john.smith"), eq(Year.of(2025)), eq(Month.FEBRUARY)))
                    .thenReturn(Optional.empty());

            // When
            TrainerWorkload januaryResult = service.processRequest(januaryCommand);
            TrainerWorkload februaryResult = service.processRequest(februaryCommand);

            // Then
            assertThat(januaryResult.getMonth()).isEqualTo(Month.JANUARY);
            assertThat(januaryResult.getTrainingDurationMinutes()).isEqualTo(100);
            assertThat(februaryResult.getMonth()).isEqualTo(Month.FEBRUARY);
            assertThat(februaryResult.getTrainingDurationMinutes()).isEqualTo(150);
            verify(repository, times(2)).save(any());
        }
    }
}
