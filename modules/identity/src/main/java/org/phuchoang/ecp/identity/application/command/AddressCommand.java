package org.phuchoang.ecp.identity.application.command;

import org.phuchoang.ecp.sharedkernel.api.address.Address;

/** The write shape for both {@code addOwnAddress} and {@code replaceOwnAddress}. */
public record AddressCommand(Address address, boolean isDefaultShipping, boolean isDefaultBilling) {
}
