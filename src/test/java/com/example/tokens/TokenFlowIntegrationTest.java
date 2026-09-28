package com.example.tokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import com.example.tokens.authorization.AuthorizationResult;
import com.example.tokens.authorization.TokenAuthorizationService;
import com.example.tokens.token.TokenRepository;
import com.example.tokens.token.TokenStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** End-to-end on one app instance, against the Liquibase seed data. Requires Docker. */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TokenFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TokenAuthorizationService service;

    @Autowired
    private TokenRepository tokens;

    @Test
    void blockedCardIsRefused() {
        String uid = "04B1B2B3B4B5B6"; // seeded ACTIVE, no expiry
        assertThat(service.authorize(uid, "LOC-BER-HBF").result()).isEqualTo(AuthorizationResult.ALLOWED);

        kafkaTemplate.send("tokens.events", uid,
                "{\"type\":\"TOKEN_BLOCKED\",\"uid\":\"" + uid + "\",\"reason\":\"STOLEN\"}").join();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(tokens.findById(uid).orElseThrow().getStatus()).isEqualTo(TokenStatus.BLOCKED));
        assertThat(service.authorize(uid, "LOC-BER-HBF").result()).isEqualTo(AuthorizationResult.BLOCKED);
    }

    @Test
    void newlyIssuedCardIsAllowed() {
        kafkaTemplate.send("tokens.events", "04E1E2E3E4E5E6", """
                {"type":"TOKEN_ISSUED","uid":"04E1E2E3E4E5E6","contractId":"DE-DCS-C55556666-V",
                 "validUntil":"2028-06-30"}""").join();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(service.authorize("04E1E2E3E4E5E6", "LOC-BER-HBF").result())
                        .isEqualTo(AuthorizationResult.ALLOWED));
    }
}
