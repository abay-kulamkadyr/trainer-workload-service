package com.epam.workload.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.epam.workload.application.dto.response.TrainerSummaryResponse;
import com.epam.workload.application.service.TrainerWorkloadService;
import com.epam.workload.infrastructure.persistence.repository.TrainerWorkloadMongoRepository;
import com.epam.workload.interfaces.messaging.event.TrainerWorkloadEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Import(TrainingEventListenerIntegrationTest.TestKafkaProducerConfig.class)
class TrainingEventListenerIntegrationTest extends BaseIntegrationTest {

    // Provides a KafkaTemplate that serializes values as JSON.
    // The app only configures a consumer, so there is no producer bean by default.
    @TestConfiguration
    static class TestKafkaProducerConfig {

        @Bean
        public ProducerFactory<String, TrainerWorkloadEvent> testProducerFactory() {
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
            return new DefaultKafkaProducerFactory<>(props);
        }

        @Bean
        public KafkaTemplate<String, TrainerWorkloadEvent> testKafkaTemplate(
                ProducerFactory<String, TrainerWorkloadEvent> testProducerFactory) {
            return new KafkaTemplate<>(testProducerFactory);
        }
    }

    @Autowired
    private KafkaTemplate<String, TrainerWorkloadEvent> kafkaTemplate;

    @Autowired
    private TrainerWorkloadService workloadService;

    @Autowired
    private TrainerWorkloadMongoRepository mongoRepository;

    @Value("${app.kafka.topics.training-created}")
    private String topic;

    @BeforeEach
    void setUp() {
        mongoRepository.deleteAll();
    }

    @Test
    void consumesAddEvent_andPersistsWorkload() {
        TrainerWorkloadEvent event = addEvent("john.doe", 60, LocalDateTime.of(2024, 6, 1, 9, 0), "TXN-001");

        kafkaTemplate.send(topic, event.getTrainerUsername(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(mongoRepository.findByUsername("john.doe")).isPresent();
            TrainerSummaryResponse summary = workloadService.getTrainerSummary("john.doe");
            assertThat(durationFor(summary, 2024, Month.JUNE)).isEqualTo(60);
        });
    }

    @Test
    void consumesMultipleAddEvents_andAccumulatesDuration() {
        kafkaTemplate.send(topic, "john.doe", addEvent("john.doe", 60, LocalDateTime.of(2024, 6, 1, 9, 0), "TXN-001"));
        kafkaTemplate.send(topic, "john.doe", addEvent("john.doe", 45, LocalDateTime.of(2024, 6, 15, 9, 0), "TXN-002"));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            TrainerSummaryResponse summary = workloadService.getTrainerSummary("john.doe");
            assertThat(durationFor(summary, 2024, Month.JUNE)).isEqualTo(105);
        });
    }

    @Test
    void consumesDeleteEvent_andReducesDuration() {
        // Seed via ADD first, then DELETE via Kafka
        kafkaTemplate.send(topic, "john.doe", addEvent("john.doe", 90, LocalDateTime.of(2024, 6, 1, 9, 0), "TXN-001"));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(mongoRepository.findByUsername("john.doe"))
                .isPresent());

        kafkaTemplate.send(
                topic, "john.doe", deleteEvent("john.doe", 30, LocalDateTime.of(2024, 6, 1, 9, 0), "TXN-002"));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            TrainerSummaryResponse summary = workloadService.getTrainerSummary("john.doe");
            assertThat(durationFor(summary, 2024, Month.JUNE)).isEqualTo(60);
        });
    }

    @Test
    void insufficientDurationOnDelete_isSwallowed_andOffsetIsCommitted() {
        // Seed 10 minutes, then try to delete 60 - should not throw or retry
        kafkaTemplate.send(topic, "john.doe", addEvent("john.doe", 10, LocalDateTime.of(2024, 6, 1, 9, 0), "TXN-001"));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(mongoRepository.findByUsername("john.doe"))
                .isPresent());

        kafkaTemplate.send(
                topic, "john.doe", deleteEvent("john.doe", 60, LocalDateTime.of(2024, 6, 1, 9, 0), "TXN-002"));

        // Send a subsequent event for a different trainer to confirm the listener
        // is still alive and processing (i.e. the bad event didn't kill the consumer)
        kafkaTemplate.send(
                topic, "jane.smith", addEvent("jane.smith", 30, LocalDateTime.of(2024, 7, 1, 9, 0), "TXN-003"));

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(mongoRepository.findByUsername("jane.smith")).isPresent());

        // john.doe's duration should be unchanged at 10 - the bad delete had no effect
        TrainerSummaryResponse summary = workloadService.getTrainerSummary("john.doe");
        assertThat(durationFor(summary, 2024, Month.JUNE)).isEqualTo(10);
    }

    @Test
    void eventWithTransactionId_doesNotThrow() {
        // Primarily guards the MDC null-check path and ensures no exception leaks
        TrainerWorkloadEvent event = addEvent("john.doe", 60, LocalDateTime.of(2024, 6, 1, 9, 0), "TXN-abc-123");

        kafkaTemplate.send(topic, event.getTrainerUsername(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(mongoRepository.findByUsername("john.doe"))
                .isPresent());
    }

    @Test
    void eventWithNullTransactionId_isProcessedSuccessfully() {
        TrainerWorkloadEvent event = addEvent("john.doe", 60, LocalDateTime.of(2024, 6, 1, 9, 0), null);

        kafkaTemplate.send(topic, event.getTrainerUsername(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(mongoRepository.findByUsername("john.doe"))
                .isPresent());
    }

    // --- helpers ---

    private TrainerWorkloadEvent addEvent(String username, int minutes, LocalDateTime date, String transactionId) {
        return TrainerWorkloadEvent.builder()
                .trainerUsername(username)
                .trainerFirstname("John")
                .trainerLastname("Doe")
                .isActive(true)
                .trainingDate(date)
                .trainingDurationMinutes(minutes)
                .actionType(TrainerWorkloadEvent.ActionType.ADD)
                .transactionId(transactionId)
                .build();
    }

    private TrainerWorkloadEvent deleteEvent(String username, int minutes, LocalDateTime date, String transactionId) {
        return TrainerWorkloadEvent.builder()
                .trainerUsername(username)
                .trainerFirstname("John")
                .trainerLastname("Doe")
                .isActive(true)
                .trainingDate(date)
                .trainingDurationMinutes(minutes)
                .actionType(TrainerWorkloadEvent.ActionType.DELETE)
                .transactionId(transactionId)
                .build();
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
