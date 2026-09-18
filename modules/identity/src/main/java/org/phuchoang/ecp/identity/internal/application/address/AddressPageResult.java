package org.phuchoang.ecp.identity.internal.application.address;

import java.util.List;

/** {@code nextCursor} is {@code null} when this is the last page. */
public record AddressPageResult(List<AddressSummary> items, String nextCursor) {
}
