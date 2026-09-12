package org.phuchoang.ecp.web.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.phuchoang.ecp.identity.api.AddressPageView;
import org.phuchoang.ecp.identity.api.AddressView;
import org.phuchoang.ecp.identity.api.AddressWriteRequest;
import org.phuchoang.ecp.identity.api.IdentityFacade;
import org.phuchoang.ecp.sharedkernel.api.RateLimiter;
import org.phuchoang.ecp.security.JwtKeysConfig;
import org.phuchoang.ecp.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.phuchoang.ecp.sharedkernel.api.GenErrorCode.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L3 — `listOwnAddresses`/`addOwnAddress`/`getOwnAddress` (`UC-CUS-09`). */
@WebMvcTest(controllers = AddressController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class})
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdentityFacade identityFacade;

    @MockitoBean
    private RateLimiter rateLimiter;

    @BeforeEach
    void allowEveryRequest() {
        BDDMockito.given(rateLimiter.tryConsume(anyString(), anyString()))
            .willReturn(new RateLimiter.Decision(true, 0));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor customerJwt() {
        return jwt().jwt(builder -> builder.subject("018f3c2a-0000-7000-8000-000000000000")
            .claim("roles", Set.of("CUSTOMER")));
    }

    @Test
    void listOwnAddressesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me/addresses"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listOwnAddressesReturnsAPageEnvelope() throws Exception {
        AddressView view = new AddressView(UUID.randomUUID().toString(), "Home", "Jane Doe", "1 Main St", null,
            "Springfield", null, "12345", "US", null, true, false);
        BDDMockito.given(identityFacade.listOwnAddresses(any(), any(), org.mockito.ArgumentMatchers.anyInt()))
            .willReturn(new AddressPageView(List.of(view), null));

        mockMvc.perform(get("/api/v1/accounts/me/addresses").with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].recipientName").value("Jane Doe"))
            .andExpect(jsonPath("$.page.next").doesNotExist());
    }

    @Test
    void listOwnAddressesRejectsAnUnknownQueryParameter() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me/addresses?bogus=1").with(customerJwt()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void addOwnAddressReturns201WithLocation() throws Exception {
        AddressView created = new AddressView("018f3c2a-0000-7000-8000-000000000099", "Home", "Jane Doe",
            "1 Main St", null, "Springfield", null, "12345", "US", null, true, false);
        BDDMockito.given(identityFacade.addOwnAddress(any(), any(AddressWriteRequest.class))).willReturn(created);

        mockMvc.perform(post("/api/v1/accounts/me/addresses").with(csrf()).with(customerJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientName\":\"Jane Doe\",\"line1\":\"1 Main St\",\"city\":\"Springfield\","
                    + "\"postalCode\":\"12345\",\"countryCode\":\"US\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("018f3c2a-0000-7000-8000-000000000099"));
    }

    @Test
    void addOwnAddressRejectsAMissingRequiredField() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/me/addresses").with(csrf()).with(customerJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientName\":\"Jane Doe\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void gettingAnotherCustomersAddressIs404NeverForbidden() throws Exception {
        UUID addressId = UUID.randomUUID();
        doThrow(new org.phuchoang.ecp.sharedkernel.api.DomainException(NOT_FOUND, "Address not found."))
            .when(identityFacade).getOwnAddress(any(), org.mockito.ArgumentMatchers.eq(addressId));

        mockMvc.perform(get("/api/v1/accounts/me/addresses/" + addressId).with(customerJwt()))
            .andExpect(status().isNotFound());
    }

    @Test
    void removeOwnAddressReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/accounts/me/addresses/" + UUID.randomUUID()).with(csrf())
                .with(customerJwt()))
            .andExpect(status().isNoContent());
    }
}
