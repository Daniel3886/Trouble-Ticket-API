package org.springboot.troubleticketapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(
        properties = {
            "spring.flyway.locations=classpath:db/migration,classpath:db/seed",
            "app.security.jwt-secret=" + TroubleTicketApiIntegrationTest.JWT_SECRET
        })
class TroubleTicketApiIntegrationTest {

    static final String JWT_SECRET = "integration-test-secret-at-least-32-bytes";

    // from db/seed/R__demo_services.sql
    static final long ACTIVE_SERVICE = 987654321L;
    static final long INACTIVE_SERVICE = 987654323L;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @LocalServerPort
    int port;

    RestTestClient client;

    record NoteJson(String id, String text, String date) {}

    record TicketJson(
            String id, String externalId, Long serviceId, String description, String status, List<NoteJson> notes) {}

    record ErrorJson(String code, String message, String requestId) {}

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + "/api/v1/troubleTicket")
                .build();
    }

    @Test
    void createIsIdempotentForSameTenantAndExternalId() {
        // given
        String body = createJson(uniqueExternalId(), ACTIVE_SERVICE);

        // when
        TicketJson first = postTicket(body, 201);
        TicketJson repeated = postTicket(body, 200);

        // then
        assertThat(repeated.id()).isEqualTo(first.id());
        assertThat(repeated.notes()).hasSize(1);
    }

    @Test
    void ticketGoesThroughCreateNoteAndClose() {
        // given
        String id =
                postTicket(createJson(uniqueExternalId(), ACTIVE_SERVICE), 201).id();

        // when
        client.post()
                .uri("/{id}/note", id)
                .header("Authorization", bearer("tenant-a"))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"text\":\"Second note\"}")
                .exchange()
                .expectStatus()
                .isCreated();

        TicketJson closed = client.patch()
                .uri("/{id}", id)
                .header("Authorization", bearer("tenant-a"))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"closed\"}")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TicketJson.class)
                .returnResult()
                .getResponseBody();

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(closed.id()).isEqualTo(id);
            softly.assertThat(closed.status()).isEqualTo("closed");
            softly.assertThat(closed.notes()).extracting(NoteJson::text).containsExactly("Initial note", "Second note");
        });
    }

    @Test
    void ticketOfAnotherTenantIsNotVisible() {
        // given
        String id =
                postTicket(createJson(uniqueExternalId(), ACTIVE_SERVICE), 201).id();

        // when
        ErrorJson error = client.get()
                .uri("/{id}", id)
                .header("Authorization", bearer("tenant-b"))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorJson.class)
                .returnResult()
                .getResponseBody();

        // then
        assertThat(error.code()).isEqualTo("TROUBLE_TICKET_NOT_FOUND");
    }

    @Test
    void createWithInactiveServiceReturns404() {
        ErrorJson error = client.post()
                .header("Authorization", bearer("tenant-a"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createJson(uniqueExternalId(), INACTIVE_SERVICE))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorJson.class)
                .returnResult()
                .getResponseBody();

        assertThat(error.code()).isEqualTo("SERVICE_NOT_FOUND");
    }

    @Test
    void tokenWithoutTenantReturns403() {
        ErrorJson error = client.get()
                .header("Authorization", bearer(null))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorJson.class)
                .returnResult()
                .getResponseBody();

        assertThat(error.code()).isEqualTo("MISSING_TENANT");
    }

    @Test
    void concurrentCreatesWithSameExternalIdCreateExactlyOneTicket() throws Exception {
        // given: 8 requests with the same externalId, released at the same moment
        String body = createJson(uniqueExternalId(), ACTIVE_SERVICE);
        int requests = 8;
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> responses = new ArrayList<>();

        // when
        for (int i = 0; i < requests; i++) {
            responses.add(pool.submit(() -> {
                start.await();
                return client.post()
                        .header("Authorization", bearer("tenant-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .exchange()
                        .returnResult(String.class)
                        .getStatus()
                        .value();
            }));
        }
        start.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> response : responses) {
            statuses.add(response.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(statuses).containsOnly(201, 200);
        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
    }

    private TicketJson postTicket(String body, int expectedStatus) {
        return client.post()
                .header("Authorization", bearer("tenant-a"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatusCode.valueOf(expectedStatus))
                .expectBody(TicketJson.class)
                .returnResult()
                .getResponseBody();
    }

    private static String createJson(String externalId, long serviceId) {
        return """
                {
                "externalId":"%s",
                "serviceId":%d,
                "description":"Router down",
                "status":"new",
                "note":"Initial note"
                }
                """.formatted(externalId, serviceId);
    }

    private static String uniqueExternalId() {
        return "it-" + UUID.randomUUID();
    }

    private static String bearer(String tenant) {
        try {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .subject("integration-test")
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)));
            if (tenant != null) {
                claims.claim("tenant_id", tenant);
            }
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
            jwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
            return "Bearer " + jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
