package org.phuchoang.ecp.identity.internal.application.address;

import org.phuchoang.ecp.sharedkernel.api.address.Address;

/** The write shape for both {@code addOwnAddress} and {@code replaceOwnAddress}. */
public record AddressCommand(Address address, boolean isDefaultShipping, boolean isDefaultBilling) {
}
