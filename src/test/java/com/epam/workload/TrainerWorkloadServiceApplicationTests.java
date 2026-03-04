package com.epam.workload;

import com.epam.workload.infrastructure.persistence.repository.TrainerWorkloadMongoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class TrainerWorkloadServiceApplicationTests {
    @MockitoBean
    private TrainerWorkloadMongoRepository mongoRepository;

    @Test
    void contextLoads() {}
}
