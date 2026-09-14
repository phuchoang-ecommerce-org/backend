package org.phuchoang.ecp.identity.api;

import java.util.List;

/** `components/schemas/identity.yaml#/CustomerAddressPage`. {@code nextCursor} is {@code null} on the last page. */
public record AddressPageView(List<AddressView> items, String nextCursor) {
}
