package com.epam.workload.interfaces.messaging.listener;

import com.epam.workload.application.dto.request.ActionType;
import com.epam.workload.application.dto.request.UpdateTrainerWorkloadCommand;
import com.epam.workload.application.exception.InsufficientDurationException;
import com.epam.workload.application.service.TrainerWorkloadService;
import com.epam.workload.interfaces.messaging.event.TrainerWorkloadEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TrainingEventListener {
    private final TrainerWorkloadService workloadService;

    @Autowired
    TrainingEventListener(TrainerWorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.training-created}",
            groupId = "${spring.application.name}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onTrainingEvent(
            @Payload TrainerWorkloadEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        if (event.getTransactionId() != null) {
            MDC.put("transactionId", event.getTransactionId());
        }

        log.info(
                "Received training event for trainer: {} from partition: {}, offset: {} [TxnId: {}]",
                event.getTrainerUsername(),
                partition,
                offset,
                event.getTransactionId());

        try {
            UpdateTrainerWorkloadCommand command = mapToCommand(event);
            workloadService.processRequest(command);

            log.info("Successfully processed training event for trainer: {}", event.getTrainerUsername());

        } catch (InsufficientDurationException e) {
            log.error("Business validation failed for trainer: {} - {}", event.getTrainerUsername(), e.getMessage());
            // TODO: Send to dead-letter topic or alert monitoring

        } catch (Exception e) {
            log.error("Failed to process training event for trainer: {}", event.getTrainerUsername(), e);
            throw e; // Trigger Kafka Retry
        } finally {
            MDC.remove("transactionId");
        }
    }

    private UpdateTrainerWorkloadCommand mapToCommand(TrainerWorkloadEvent event) {
        return new UpdateTrainerWorkloadCommand(
                event.getTrainerUsername(),
                event.getTrainerFirstname(),
                event.getTrainerLastname(),
                event.getIsActive(),
                event.getTrainingDate(),
                event.getTrainingDurationMinutes(),
                event.getActionType() == TrainerWorkloadEvent.ActionType.ADD ? ActionType.ADD : ActionType.DELETE);
    }
}
