package org.phuchoang.ecp.identity.application.query;

import org.phuchoang.ecp.identity.application.mapper.AddressSummary;

import java.util.List;

/** {@code nextCursor} is {@code null} when this is the last page. */
public record AddressPageResult(List<AddressSummary> items, String nextCursor) {
}
