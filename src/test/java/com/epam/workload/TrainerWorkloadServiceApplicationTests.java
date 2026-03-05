package com.epam.workload;

import com.epam.workload.infrastructure.persistence.repository.TrainerWorkloadMongoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = "spring.main.banner-mode=off")
class TrainerWorkloadServiceApplicationTests {
    @MockitoBean
    private TrainerWorkloadMongoRepository mongoRepository;

    @Test
    void contextLoads() {}
}
