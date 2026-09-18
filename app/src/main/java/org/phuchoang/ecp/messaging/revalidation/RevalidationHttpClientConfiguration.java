package org.phuchoang.ecp.messaging.revalidation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/** One shared JDK HTTP client for outbound storefront callbacks. */
@Configuration
class RevalidationHttpClientConfiguration {

    @Bean
    HttpClient revalidationHttpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }
}
