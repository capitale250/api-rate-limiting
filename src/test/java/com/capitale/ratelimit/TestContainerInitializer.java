package com.capitale.ratelimit;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class TestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    PostgreSQLContainer<?> postgreSQLContainer  = new PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    GenericContainer<?> redis = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        postgreSQLContainer.start();
        redis.start();
        TestPropertyValues.of(
                "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                "spring.datasource.password=" + postgreSQLContainer.getPassword(),
                "capitale.ratelimit.redis="+ "redis://" + redis.getHost() + ":" + redis.getFirstMappedPort()
        ).applyTo(ctx.getEnvironment());
    }
}
