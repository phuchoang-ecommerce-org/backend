package org.phuchoang.ecp.shipping.internal.application.tracking.command;

/** Verifies a carrier callback before it is allowed to touch shipment state. */
public interface CarrierEventAuthenticator {
  boolean authentic(ReceiveCarrierEventCommand event, String signature);
}
