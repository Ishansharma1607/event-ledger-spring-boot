package com.mphasis.eventledger.account.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        jdbcTemplate.queryForObject("select 1", Integer.class);
        return Map.of(
                "service", "account-service",
                "status", "UP",
                "database", "UP",
                "timestamp", Instant.now().toString()
        );
    }
}
