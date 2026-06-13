package com.mphasis.eventledger.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AccountConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    void accountServiceRuntimeConfigurationIsLoaded() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("account-service");
        assertThat(environment.getProperty("server.port")).isEqualTo("8081");
        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }
}
