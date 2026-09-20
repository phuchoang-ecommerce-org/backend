package org.phuchoang.ecp.messaging.revalidation;

import org.phuchoang.ecp.messaging.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * {@link WebRevalidationGateway} over the JDK {@link HttpClient}: request construction, headers,
 * signature, status interpretation and transport failures all live here and nowhere else.
 */
@Component
class HttpWebRevalidationGateway implements WebRevalidationGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpWebRevalidationGateway.class);

    static final String SIGNATURE_HEADER = "X-ECP-Signature";
    static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final HttpClient http;
    private final PayloadSigner signer;
    private final RevalidationProperties properties;

    HttpWebRevalidationGateway(HttpClient http, PayloadSigner signer, RevalidationProperties properties) {
        this.http = http;
        this.signer = signer;
        this.properties = properties;
    }

    @Override
    public void revalidate(EventEnvelope event, String originalBody) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Catalog revalidation callback is not configured.");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.url()))
            .header("Content-Type", "application/json")
            .header(SIGNATURE_HEADER, signer.sign(originalBody))
            .header(CORRELATION_HEADER, event.correlationId().toString())
            .POST(HttpRequest.BodyPublishers.ofString(originalBody, StandardCharsets.UTF_8))
            .build();

        int status = send(request);
        if (status == 204) {
            return;
        }
        if (status == 401) {
            log.error("callback.signature_rejected eventId={} correlationId={}", event.eventId(), event.correlationId());
            throw new RevalidationSignatureRejectedException(event.eventId());
        }
        throw new IllegalStateException("Web revalidation returned HTTP " + status + " for eventId=" + event.eventId());
    }

    private int send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (IOException exception) {
            throw new UncheckedIOException("Web revalidation callback failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Web revalidation callback interrupted", exception);
        }
    }
}
