package org.phuchoang.ecp.messaging.revalidation;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.messaging.EventEnvelope;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The HTTP boundary against a real (local, ephemeral) server — no Kafka, no Redis. */
class HttpWebRevalidationGatewayTest {

    private HttpServer server;
    private final AtomicInteger status = new AtomicInteger(204);
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicReference<String> receivedSignature = new AtomicReference<>();
    private final AtomicReference<String> receivedCorrelation = new AtomicReference<>();
    private final AtomicReference<String> receivedMethod = new AtomicReference<>();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/revalidate", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            receivedSignature.set(exchange.getRequestHeaders().getFirst(HttpWebRevalidationGateway.SIGNATURE_HEADER));
            receivedCorrelation.set(exchange.getRequestHeaders().getFirst(HttpWebRevalidationGateway.CORRELATION_HEADER));
            exchange.sendResponseHeaders(status.get(), -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void postsTheOriginalBodyWithSignatureAndCorrelationHeadersAndAccepts204() {
        EventEnvelope event = event();
        gateway("secret").revalidate(event, "{\"raw\":true}");

        assertThat(receivedMethod.get()).isEqualTo("POST");
        assertThat(receivedBody.get()).isEqualTo("{\"raw\":true}");
        assertThat(receivedSignature.get()).isEqualTo(HmacSha256PayloadSigner.sign("secret", "{\"raw\":true}"));
        assertThat(receivedCorrelation.get()).isEqualTo(event.correlationId().toString());
    }

    @Test
    void aRejectedSignatureAndAnyOtherStatusAreDistinctFailures() {
        EventEnvelope event = event();
        status.set(401);
        assertThatThrownBy(() -> gateway("secret").revalidate(event, "b"))
            .isInstanceOf(RevalidationSignatureRejectedException.class).hasMessageContaining("signature rejected");
        status.set(500);
        assertThatThrownBy(() -> gateway("secret").revalidate(event, "b"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTP 500");
    }

    @Test
    void anUnconfiguredCallbackFailsAtCallTimeWithoutSendingAnything() {
        RevalidationProperties unconfigured = new RevalidationProperties("", "", "g");
        WebRevalidationGateway gateway = new HttpWebRevalidationGateway(HttpClient.newHttpClient(),
            new HmacSha256PayloadSigner(unconfigured), unconfigured);

        assertThatThrownBy(() -> gateway.revalidate(event(), "b"))
            .isInstanceOf(IllegalStateException.class).hasMessage("Catalog revalidation callback is not configured.");
        assertThat(receivedBody.get()).isNull();
    }

    private WebRevalidationGateway gateway(String secret) {
        RevalidationProperties properties = new RevalidationProperties(
            "http://127.0.0.1:" + server.getAddress().getPort() + "/revalidate", secret, "g");
        return new HttpWebRevalidationGateway(HttpClient.newHttpClient(), new HmacSha256PayloadSigner(properties),
            properties);
    }

    private static EventEnvelope event() {
        return new EventEnvelope(UUID.randomUUID(), "ProductUpdated", 1, Instant.now(), "Product", UUID.randomUUID(),
            UUID.randomUUID(), null, JsonMapper.builder().build().readTree("{\"variantIds\":[]}"));
    }

}
