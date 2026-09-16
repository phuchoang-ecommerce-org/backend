package org.phuchoang.ecp.web.identity;

import jakarta.servlet.http.HttpServletRequest;
import org.phuchoang.ecp.identity.api.authorization.Actor;
import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.identity.api.request.AddressWriteRequest;
import org.phuchoang.ecp.identity.api.view.AddressPageView;
import org.phuchoang.ecp.identity.api.view.AddressView;
import org.phuchoang.ecp.sharedkernel.api.error.FieldErrorCodes;
import org.phuchoang.ecp.web.error.FieldError;
import org.phuchoang.ecp.web.error.ValidationException;
import org.phuchoang.ecp.web.pagination.Page;
import org.phuchoang.ecp.web.pagination.PageEnvelope;
import org.phuchoang.ecp.web.pagination.Pagination;
import org.phuchoang.ecp.web.request.QueryParams;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** `listOwnAddresses`, `addOwnAddress`, `getOwnAddress`, `replaceOwnAddress`, `removeOwnAddress` (`UC-CUS-09`). */
@RestController
class AddressController {

    private static final Set<String> LIST_QUERY_PARAMS = Set.of("cursor", "size");

    private final IdentityFacade identityFacade;

    AddressController(IdentityFacade identityFacade) {
        this.identityFacade = identityFacade;
    }

    @GetMapping("/api/v1/accounts/me/addresses")
    PageEnvelope<AddressView> listOwnAddresses(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size) {
        QueryParams.rejectUnknown(request, LIST_QUERY_PARAMS);
        int pageSize = Pagination.clampSize(size);
        AddressPageView page = identityFacade.listOwnAddresses(actorOf(jwt), cursor, pageSize);
        return new PageEnvelope<>(page.items(), new Page(page.items().size(), page.nextCursor(), null));
    }

    @PostMapping("/api/v1/accounts/me/addresses")
    ResponseEntity<AddressView> addOwnAddress(@AuthenticationPrincipal Jwt jwt,
            @RequestBody AddressWriteRequest request) {
        requireAddressFields(request);
        AddressView created = identityFacade.addOwnAddress(actorOf(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/v1/accounts/me/addresses/" + created.id()))
            .body(created);
    }

    @GetMapping("/api/v1/accounts/me/addresses/{addressId}")
    AddressView getOwnAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID addressId) {
        return identityFacade.getOwnAddress(actorOf(jwt), addressId);
    }

    @PutMapping("/api/v1/accounts/me/addresses/{addressId}")
    AddressView replaceOwnAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID addressId,
            @RequestBody AddressWriteRequest request) {
        requireAddressFields(request);
        return identityFacade.replaceOwnAddress(actorOf(jwt), addressId, request);
    }

    @DeleteMapping("/api/v1/accounts/me/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeOwnAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID addressId) {
        identityFacade.removeOwnAddress(actorOf(jwt), addressId);
    }

    private static void requireAddressFields(AddressWriteRequest request) {
        List<FieldError> errors = new ArrayList<>();
        requireNonBlank(request.recipientName(), "recipientName", errors);
        requireNonBlank(request.line1(), "line1", errors);
        requireNonBlank(request.city(), "city", errors);
        requireNonBlank(request.postalCode(), "postalCode", errors);
        requireNonBlank(request.countryCode(), "countryCode", errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static void requireNonBlank(String value, String field, List<FieldError> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new FieldError(field, FieldErrorCodes.REQUIRED, field + " is required."));
        }
    }

    private Actor actorOf(Jwt jwt) {
        Set<String> roles = Set.copyOf(jwt.getClaimAsStringList("roles"));
        return new Actor(UUID.fromString(jwt.getSubject()), roles);
    }
}
