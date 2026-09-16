package org.phuchoang.ecp.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.phuchoang.ecp.inventory.api.InventoryErrorCode;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Test-only controller (EN-WIRE-1) exercising every wire-format shape this sprint decides, so L3
 * tests assert the envelope and the problem shape without waiting for a real domain controller
 * (sprint-02-wire-format-and-shell.md). Lives under {@code src/test} — absent from {@code bootJar}.
 */
@RestController
class WireFormatStubController {

    @GetMapping("/test/wire-format/ok")
    String ok() {
        return "OK";
    }

    @GetMapping("/test/wire-format/orders/{id}")
    String notFound() {
        throw new DomainException(GenErrorCode.NOT_FOUND, "No stub order with that id.");
    }

    @GetMapping("/test/wire-format/domain-error")
    String domainError() {
        throw new DomainException(InventoryErrorCode.INSUFFICIENT_STOCK,
            "SKU TS-BLU-M has 2 units available; 5 were requested.");
    }

    @GetMapping("/test/wire-format/boom")
    String boom() {
        throw new IllegalStateException("unmapped failure");
    }

    @PostMapping("/test/wire-format/validate")
    String validate(@Valid @RequestBody StubRequest request) {
        return request.name();
    }

    @GetMapping("/test/wire-format/items")
    PageEnvelope<String> items(@RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort, HttpServletRequest request) {
        QueryParams.rejectUnknown(request, Set.of("size", "sort", "cursor"));
        if (sort != null) {
            SortSpec.parse(sort, Set.of("name"));
        }
        int clampedSize = Pagination.clampSize(size);
        List<String> items = List.of("alpha", "bravo", "charlie").stream().limit(clampedSize).toList();
        return new PageEnvelope<>(items, new Page(clampedSize, null, (long) items.size()));
    }

    record StubRequest(@NotBlank String name) {
    }
}
