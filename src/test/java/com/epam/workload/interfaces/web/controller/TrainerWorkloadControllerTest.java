package com.epam.workload.interfaces.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.List;

import com.epam.workload.application.dto.request.ActionType;
import com.epam.workload.application.dto.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.application.dto.response.MonthSummaryDTO;
import com.epam.workload.application.dto.response.TrainerSummaryResponse;
import com.epam.workload.application.dto.response.YearSummaryDTO;
import com.epam.workload.application.exception.InsufficientDurationException;
import com.epam.workload.application.service.TrainerWorkloadService;
import com.epam.workload.domain.model.TrainerWorkload;
import com.epam.workload.interfaces.web.dto.request.TrainerWorkloadWebRequest;
import com.epam.workload.interfaces.web.mapper.WorkloadRequestMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TrainerWorkloadController.class)
@DisplayName("TrainerWorkloadController Tests")
class TrainerWorkloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TrainerWorkloadService workloadService;

    @MockitoBean
    private WorkloadRequestMapper mapper;

    @Nested
    @DisplayName("POST /api/workload Tests")
    class ProcessWorkloadTests {

        @Test
        @WithMockUser
        @DisplayName("Should process ADD request successfully")
        void shouldProcessAddRequestSuccessfully() throws Exception {
            // Given
            TrainerWorkloadWebRequest request = new TrainerWorkloadWebRequest(
                    "john.smith", "John", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), 120, ActionType.ADD);

            UpdateTrainerWorkloadCommand command = new UpdateTrainerWorkloadCommand(
                    "john.smith", "John", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), 120, ActionType.ADD);

            TrainerWorkload workload = TrainerWorkload.builder()
                    .username("john.smith")
                    .year(Year.of(2025))
                    .month(Month.JANUARY)
                    .trainingDurationMinutes(120)
                    .build();

            when(mapper.toUpdateWorkloadCommand(any())).thenReturn(command);
            when(workloadService.processRequest(command)).thenReturn(workload);

            // When & Then
            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("john.smith"))
                    .andExpect(jsonPath("$.year").value("2025"))
                    .andExpect(jsonPath("$.month").value("JANUARY"))
                    .andExpect(jsonPath("$.trainingDurationMinutes").value(120));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when username is blank")
        void shouldReturn400WhenUsernameIsBlank() throws Exception {
            // Given
            TrainerWorkloadWebRequest request = new TrainerWorkloadWebRequest(
                    "", // Blank username
                    "John",
                    "Smith",
                    true,
                    LocalDateTime.of(2025, 1, 15, 14, 30),
                    120,
                    ActionType.ADD);

            // When & Then
            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trainerUsername").value("Trainer username is required"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when duration is less than 1")
        void shouldReturn400WhenDurationIsLessThan1() throws Exception {
            // Given
            TrainerWorkloadWebRequest request = new TrainerWorkloadWebRequest(
                    "john.smith",
                    "John",
                    "Smith",
                    true,
                    LocalDateTime.of(2025, 1, 15, 14, 30),
                    0, // Invalid duration
                    ActionType.ADD);

            // When & Then
            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.trainingDurationMinutes").value("Training duration must be at least 1 minute"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when required fields are null")
        void shouldReturn400WhenRequiredFieldsAreNull() throws Exception {
            // Given
            TrainerWorkloadWebRequest request = new TrainerWorkloadWebRequest(
                    "john.smith",
                    "John",
                    "Smith",
                    null, // Null active status
                    null, // Null training date
                    120,
                    ActionType.ADD);

            // When & Then
            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 422 when insufficient duration for DELETE")
        void shouldReturn422WhenInsufficientDurationForDelete() throws Exception {
            // Given
            TrainerWorkloadWebRequest request = new TrainerWorkloadWebRequest(
                    "john.smith", "John", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), 120, ActionType.DELETE);

            UpdateTrainerWorkloadCommand command = new UpdateTrainerWorkloadCommand(
                    "john.smith", "John", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), 120, ActionType.DELETE);

            when(mapper.toUpdateWorkloadCommand(any())).thenReturn(command);
            when(workloadService.processRequest(command))
                    .thenThrow(new InsufficientDurationException("Cannot delete 120 minutes"));

            // When & Then
            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("INSUFFICIENT_DURATION"))
                    .andExpect(jsonPath("$.message").value(containsString("Cannot delete 120 minutes")));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Given
            TrainerWorkloadWebRequest request = new TrainerWorkloadWebRequest(
                    "john.smith", "John", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), 120, ActionType.ADD);

            // When & Then
            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/workload/{username} Tests")
    class GetWorkloadSummaryTests {

        @Test
        @WithMockUser
        @DisplayName("Should return trainer summary successfully")
        void shouldReturnTrainerSummarySuccessfully() throws Exception {
            // Given
            String username = "john.smith";

            TrainerSummaryResponse serviceResponse = new TrainerSummaryResponse(
                    username,
                    "John",
                    "Smith",
                    true,
                    List.of(new YearSummaryDTO(
                            Year.of(2025),
                            List.of(
                                    new MonthSummaryDTO(Month.JANUARY, 120),
                                    new MonthSummaryDTO(Month.FEBRUARY, 90)))));

            com.epam.workload.interfaces.web.dto.response.TrainerSummaryResponse webResponse =
                    new com.epam.workload.interfaces.web.dto.response.TrainerSummaryResponse(
                            username,
                            "John",
                            "Smith",
                            true,
                            List.of(new com.epam.workload.interfaces.web.dto.response.YearSummaryDTO(
                                    Year.of(2025),
                                    List.of(
                                            new com.epam.workload.interfaces.web.dto.response.MonthSummaryDTO(
                                                    Month.JANUARY, 120),
                                            new com.epam.workload.interfaces.web.dto.response.MonthSummaryDTO(
                                                    Month.FEBRUARY, 90)))));

            when(workloadService.getTrainerSummary(username)).thenReturn(serviceResponse);
            when(mapper.toTrainerSummaryResponse(serviceResponse)).thenReturn(webResponse);

            // When & Then
            mockMvc.perform(get("/api/workload/{username}", username))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(username))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Smith"))
                    .andExpect(jsonPath("$.status").value(true))
                    .andExpect(jsonPath("$.years").isArray())
                    .andExpect(jsonPath("$.years[0].year").value(2025))
                    .andExpect(jsonPath("$.years[0].months").isArray())
                    .andExpect(jsonPath("$.years[0].months[0].month").value("JANUARY"))
                    .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration")
                            .value(120))
                    .andExpect(jsonPath("$.years[0].months[1].month").value("FEBRUARY"))
                    .andExpect(jsonPath("$.years[0].months[1].trainingSummaryDuration")
                            .value(90));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when trainer not found")
        void shouldReturn404WhenTrainerNotFound() throws Exception {
            // Given
            String username = "nonexistent.user";

            when(workloadService.getTrainerSummary(username))
                    .thenThrow(new EntityNotFoundException("No workload found"));

            // When & Then
            mockMvc.perform(get("/api/workload/{username}", username))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Resource Not Found"));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/workload/{username}", "john.smith")).andExpect(status().isUnauthorized());
        }
    }
}
