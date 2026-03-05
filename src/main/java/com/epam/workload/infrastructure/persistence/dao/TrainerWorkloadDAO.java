package com.epam.workload.infrastructure.persistence.dao;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "trainer_workloads")
@CompoundIndex(name = "name_search_idx", def = "{'firstName': 1, 'lastName': 1}")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor // Required by Spring Data MongoDB
@Builder
public class TrainerWorkloadDAO {
    @Id
    private @Nullable String id;

    @Indexed(unique = true)
    private String username;

    private String firstName;

    private String lastName;

    private boolean active;

    @Builder.Default
    private List<YearSummaryDAO> years = new ArrayList<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class YearSummaryDAO {
        private int year;

        @Builder.Default
        private List<MonthSummaryDAO> months = new ArrayList<>();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MonthSummaryDAO {
        private int month;
        private int durationMin;
    }
}
