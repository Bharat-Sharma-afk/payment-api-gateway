package com.payment.gateway.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String healthTopic;

    public SystemHealthController(JdbcTemplate jdbcTemplate, 
                                  StringRedisTemplate redisTemplate, 
                                  KafkaTemplate<String, String> kafkaTemplate,
                                  @Value("${kafka.health.topic}") String healthTopic) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.healthTopic = healthTopic;
    }

    @GetMapping("/status")
    public Map<String, String> checkAllConnections() {
        Map<String, String> statusReport = new LinkedHashMap<>();

        // 1. Test PostgreSQL
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            statusReport.put("PostgreSQL", "🟢 CONNECTED");
        } catch (Exception e) {
            statusReport.put("PostgreSQL", "🔴 FAILED: " + e.getMessage());
        }

        // 2. Test Redis
        try {
            redisTemplate.opsForValue().set("health_ping", "ok", Duration.ofSeconds(5));
            String val = redisTemplate.opsForValue().get("health_ping");
            if ("ok".equals(val)) {
                statusReport.put("Redis", "🟢 CONNECTED");
            } else {
                statusReport.put("Redis", "🔴 FAILED: Unexpected value");
            }
        } catch (Exception e) {
            statusReport.put("Redis", "🔴 FAILED: " + e.getMessage());
        }

        // 3. Test Kafka
        try {
            // We use .get() to force the code to wait for the broker's acknowledgment
            kafkaTemplate.send(healthTopic, "ping-message").get(5, TimeUnit.SECONDS);
            statusReport.put("Kafka", "🟢 CONNECTED");
        } catch (Exception e) {
            statusReport.put("Kafka", "🔴 FAILED: " + e.getMessage());
        }

        return statusReport;
    }
}
