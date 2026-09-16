package org.phuchoang.ecp.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.sharedkernel.api.ratelimit.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L3 web-slice tests (EN-WIRE-1) asserting the problem+json shape, the pagination envelope, and
 * correlation propagation against {@link WireFormatStubController} — a container start-up to assert
 * a JSON field name is the waste L3 exists to avoid (sprint-02-wire-format-and-shell.md).
 */
@WebMvcTest(controllers = WireFormatStubController.class)
class WireFormatWebTest {

    @Autowired
    private MockMvc mockMvc;

    // RateLimitFilter (Sprint 03, EN-WIRE-2; per-caller keying added Sprint 04, US-AUD-04) is a
    // servlet Filter bean, which @WebMvcTest always includes regardless of controller scope — it
    // needs a RateLimiter and a JwtDecoder, which this narrow slice otherwise has no bean for.
    @MockitoBean
    private RateLimiter rateLimiter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void allowEveryRequest() {
        org.mockito.BDDMockito.given(rateLimiter.tryConsume(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString())).willReturn(new RateLimiter.Decision(true, 0));
    }

    @Test
    void successResponseIsUnaffected() throws Exception {
        mockMvc.perform(get("/test/wire-format/ok"))
            .andExpect(status().isOk())
            .andExpect(content().string("OK"));
    }

    @Test
    void notFoundRendersProblemJsonWithCorrelationIdAndNoStackOrRawBody() throws Exception {
        mockMvc.perform(get("/test/wire-format/orders/018f3c2a-7b41-7c9e-9f10-2a4b6c8d0e12")
                .header("X-Correlation-Id", "0f9c2b3a-4d61-4e2f-9c77-1a2b3c4d5e6f"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(header().string("X-Correlation-Id", "0f9c2b3a-4d61-4e2f-9c77-1a2b3c4d5e6f"))
            .andExpect(jsonPath("$.code").value("ECP-GEN-4040"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.correlationId").value("0f9c2b3a-4d61-4e2f-9c77-1a2b3c4d5e6f"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors").isEmpty())
            // no stack frame or exception class name leaks into the body
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("DomainException"))));
    }

    @Test
    void correlationIdIsMintedWhenAbsent() throws Exception {
        mockMvc.perform(get("/test/wire-format/ok"))
            .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void domainErrorMapsItsOwnErrorCode() throws Exception {
        mockMvc.perform(get("/test/wire-format/domain-error"))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("ECP-INV-4091"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.type").value("https://ecp.example/errors/ECP-INV-4091"));
    }

    @Test
    void unmappedExceptionFallsBackToGen5000WithNoLeakage() throws Exception {
        mockMvc.perform(get("/test/wire-format/boom"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("ECP-GEN-5000"))
            .andExpect(jsonPath("$.detail").doesNotExist())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("IllegalStateException"))));
    }

    @Test
    void beanValidationReportsEveryFailingFieldAtOnce() throws Exception {
        mockMvc.perform(post("/test/wire-format/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("ECP-GEN-4000"))
            .andExpect(jsonPath("$.errors[0].field").value("name"))
            .andExpect(jsonPath("$.errors[0].code").value("ECP-GEN-4003"));
    }

    @Test
    void paginationEnvelopeShapeIsCorrect() throws Exception {
        mockMvc.perform(get("/test/wire-format/items").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.page.size").value(2))
            .andExpect(jsonPath("$.page.next").doesNotExist());
    }

    @Test
    void oversizedPageIsClampedNotRejected() throws Exception {
        mockMvc.perform(get("/test/wire-format/items").param("size", "500"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    void unknownQueryParameterIsRejected() throws Exception {
        mockMvc.perform(get("/test/wire-format/items").param("bogus", "1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ECP-GEN-4000"))
            .andExpect(jsonPath("$.errors[0].field").value("bogus"));
    }
}
