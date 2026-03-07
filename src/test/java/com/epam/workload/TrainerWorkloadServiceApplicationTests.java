package com.epam.workload;

import com.epam.workload.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.main.banner-mode=off")
class TrainerWorkloadServiceApplicationTests extends BaseIntegrationTest {
    @Test
    void contextLoads() {}
}
