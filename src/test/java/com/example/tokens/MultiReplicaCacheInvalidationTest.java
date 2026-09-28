package com.example.tokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import com.example.tokens.authorization.AuthorizationResult;
import com.example.tokens.authorization.TokenAuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Reproduces the Task 2 incident: production runs 3 replicas, each with its own in-memory TokenCache.
 * A TOKEN_BLOCKED event must be enforced on every replica within 30s (README "Blocking"), not just the
 * one replica whose Kafka consumer happens to receive it. A shared consumer group id would let Kafka
 * hand the event to only one replica, leaving the others serving a stale cached ALLOWED - which is
 * exactly what happened in production. This test starts two full, independent app instances against one
 * shared Postgres/Kafka to catch that class of bug.
 */
@Testcontainers(disabledWithoutDocker = true)
class MultiReplicaCacheInvalidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    private ConfigurableApplicationContext replicaA;
    private ConfigurableApplicationContext replicaB;

    @AfterEach
    void tearDown() {
        if (replicaA != null) {
            replicaA.close();
        }
        if (replicaB != null) {
            replicaB.close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void blockIsEnforcedOnEveryReplicaWithin30Seconds() {
        replicaA = startReplica();
        replicaB = startReplica();

        String uid = "04B1B2B3B4B5B6"; // seeded ACTIVE, no expiry

        TokenAuthorizationService serviceA = replicaA.getBean(TokenAuthorizationService.class);
        TokenAuthorizationService serviceB = replicaB.getBean(TokenAuthorizationService.class);

        // Warm both replicas' caches with the still-active token, like normal traffic would.
        assertThat(serviceA.authorize(uid, "LOC-BER-HBF").result()).isEqualTo(AuthorizationResult.ALLOWED);
        assertThat(serviceB.authorize(uid, "LOC-BER-HBF").result()).isEqualTo(AuthorizationResult.ALLOWED);

        KafkaTemplate<String, String> kafkaTemplate = replicaA.getBean(KafkaTemplate.class);
        kafkaTemplate.send("tokens.events", uid,
                "{\"type\":\"TOKEN_BLOCKED\",\"uid\":\"" + uid + "\",\"reason\":\"STOLEN\"}").join();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(serviceA.authorize(uid, "LOC-BER-HBF").result()).isEqualTo(AuthorizationResult.BLOCKED));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(serviceB.authorize(uid, "LOC-BER-HBF").result()).isEqualTo(AuthorizationResult.BLOCKED));
    }

    private ConfigurableApplicationContext startReplica() {
        // Command-line-style args (highest precedence) - a "defaultProperties"/properties(Map) override
        // would be outranked by application.yml's own hardcoded localhost:5433/localhost:9092, silently
        // pointing this test at the real local dev stack instead of these ephemeral containers.
        return new SpringApplicationBuilder(TokenAuthorizationServiceApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=" + postgres.getJdbcUrl(),
                        "--spring.datasource.username=" + postgres.getUsername(),
                        "--spring.datasource.password=" + postgres.getPassword(),
                        "--spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers());
    }
}
