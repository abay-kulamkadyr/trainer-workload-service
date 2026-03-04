package com.epam.workload.interfaces.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private TrainerWorkloadWebRequest validAddRequest() {
        return new TrainerWorkloadWebRequest(
                "john.smith", "John", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), 120, ActionType.ADD);
    }

    private UpdateTrainerWorkloadCommand validAddCommand() {
        return new UpdateTrainerWorkloadCommand(
                "john.smith", "John", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), 120, ActionType.ADD);
    }

    private TrainerWorkload minimalWorkload() {
        return TrainerWorkload.builder()
                .username("john.smith")
                .firstName("John")
                .lastName("Smith")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("POST /api/workload")
    class PostWorkloadTests {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 and username on successful ADD")
        void shouldReturn200OnSuccessfulAdd() throws Exception {
            when(mapper.toUpdateWorkloadCommand(any())).thenReturn(validAddCommand());
            when(workloadService.processRequest(any())).thenReturn(minimalWorkload());

            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validAddRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("john.smith"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when username is blank")
        void shouldReturn400WhenUsernameIsBlank() throws Exception {
            TrainerWorkloadWebRequest request = new TrainerWorkloadWebRequest(
                    "", "John", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), 120, ActionType.ADD);

            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trainerUsername").value("Trainer username is required"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when first name is blank")
        void shouldReturn400WhenFirstNameIsBlank() throws Exception {
            TrainerWorkloadWebRequest request = new TrainerWorkloadWebRequest(
                    "john.smith", "", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), 120, ActionType.ADD);

            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trainerFirstname").value("Trainer first name is required"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when duration is zero")
        void shouldReturn400WhenDurationIsZero() throws Exception {
            TrainerWorkloadWebRequest request = new TrainerWorkloadWebRequest(
                    "john.smith", "John", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), 0, ActionType.ADD);

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
        @DisplayName("Should return 400 when training date is null")
        void shouldReturn400WhenTrainingDateIsNull() throws Exception {
            TrainerWorkloadWebRequest request =
                    new TrainerWorkloadWebRequest("john.smith", "John", "Smith", true, null, 120, ActionType.ADD);

            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trainingDate").value("Training date is required"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when active status is null")
        void shouldReturn400WhenActiveStatusIsNull() throws Exception {
            TrainerWorkloadWebRequest request = new TrainerWorkloadWebRequest(
                    "john.smith", "John", "Smith", null, LocalDateTime.of(2025, 1, 15, 14, 30), 120, ActionType.ADD);

            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isActive").value("Active status is required"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when action type is null")
        void shouldReturn400WhenActionTypeIsNull() throws Exception {
            TrainerWorkloadWebRequest request = new TrainerWorkloadWebRequest(
                    "john.smith", "John", "Smith", true, LocalDateTime.of(2025, 1, 15, 14, 30), 120, null);

            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.actionType").value("Action type is required"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 422 when insufficient duration on DELETE")
        void shouldReturn422OnInsufficientDuration() throws Exception {
            when(mapper.toUpdateWorkloadCommand(any())).thenReturn(validAddCommand());
            when(workloadService.processRequest(any()))
                    .thenThrow(new InsufficientDurationException("Cannot subtract 120 min"));

            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validAddRequest())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("INSUFFICIENT_DURATION"))
                    .andExpect(jsonPath("$.message").value(containsString("120")));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/workload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validAddRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/workload/{username}")
    class GetWorkloadTests {

        private com.epam.workload.interfaces.web.dto.response.TrainerSummaryResponse webSummary() {
            return new com.epam.workload.interfaces.web.dto.response.TrainerSummaryResponse(
                    "john.smith",
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
        }

        private TrainerSummaryResponse serviceSummary() {
            return new TrainerSummaryResponse(
                    "john.smith",
                    "John",
                    "Smith",
                    true,
                    List.of(new YearSummaryDTO(
                            Year.of(2025),
                            List.of(
                                    new MonthSummaryDTO(Month.JANUARY, 120),
                                    new MonthSummaryDTO(Month.FEBRUARY, 90)))));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with full summary")
        void shouldReturnFullSummary() throws Exception {
            when(workloadService.getTrainerSummary("john.smith")).thenReturn(serviceSummary());
            when(mapper.toTrainerSummaryResponse(any())).thenReturn(webSummary());

            mockMvc.perform(get("/api/workload/{username}", "john.smith"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("john.smith"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Smith"))
                    .andExpect(jsonPath("$.status").value(true))
                    .andExpect(jsonPath("$.years[0].year").value(2025))
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
            when(workloadService.getTrainerSummary("ghost"))
                    .thenThrow(new EntityNotFoundException("No workload found for trainer: ghost"));

            mockMvc.perform(get("/api/workload/{username}", "ghost"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Resource Not Found"))
                    .andExpect(jsonPath("$.message").value(containsString("ghost")));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/workload/{username}", "john.smith")).andExpect(status().isUnauthorized());
        }
    }
}
