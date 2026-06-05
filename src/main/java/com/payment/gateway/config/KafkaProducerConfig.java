package com.payment.gateway.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    private final Environment environment;
    private final ResourceLoader resourceLoader;

    public KafkaProducerConfig(Environment environment, ResourceLoader resourceLoader) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = kafkaClientSecurityConfig();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getRequiredProperty("spring.kafka.bootstrap-servers"));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> config = kafkaClientSecurityConfig();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getRequiredProperty("spring.kafka.bootstrap-servers"));

        return new KafkaAdmin(config);
    }

    @Bean
    public NewTopic healthTopic() {
        return new NewTopic(environment.getRequiredProperty("kafka.health.topic"), 1, (short) 1);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    private Map<String, Object> kafkaClientSecurityConfig() {
        Map<String, Object> config = new HashMap<>();
        copyKafkaProperty(config, "security.protocol", "security.protocol");
        copyKafkaProperty(config, "sasl.mechanism", "sasl.mechanism");
        copyKafkaProperty(config, "sasl.jaas.config", "sasl.jaas.config");
        copyKafkaProperty(config, "ssl.endpoint.identification.algorithm", "ssl.endpoint.identification.algorithm");
        copyKafkaProperty(config, "ssl.truststore.type", "ssl.truststore.type");
        copyKafkaLocationProperty(config, "ssl.truststore.location", "ssl.truststore.location");
        return config;
    }

    private void copyKafkaProperty(Map<String, Object> config, String kafkaProperty, String springProperty) {
        String value = environment.getProperty("spring.kafka.properties." + springProperty);
        if (value == null || value.isBlank()) {
            return;
        }

        config.put(kafkaProperty, value);
    }

    private void copyKafkaLocationProperty(Map<String, Object> config, String kafkaProperty, String springProperty) {
        String value = environment.getProperty("spring.kafka.properties." + springProperty);
        if (value == null || value.isBlank()) {
            return;
        }

        config.put(kafkaProperty, resolveLocationIfPossible(value));
    }

    private String resolveLocationIfPossible(String value) {
        if (!value.startsWith("classpath:")) {
            return value;
        }

        try {
            Resource resource = resourceLoader.getResource(value);
            if (resource.exists()) {
                if (resource.isFile()) {
                    return resource.getFile().getAbsolutePath();
                }

                Path tempFile = Files.createTempFile("kafka-ca-", ".cert");
                tempFile.toFile().deleteOnExit();
                try (InputStream inputStream = resource.getInputStream()) {
                    Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                return tempFile.toAbsolutePath().toString();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read Kafka SSL resource: " + value, e);
        }

        throw new IllegalStateException("Kafka SSL resource does not exist: " + value);
    }
}
