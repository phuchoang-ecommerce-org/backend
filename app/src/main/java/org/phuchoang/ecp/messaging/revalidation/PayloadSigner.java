package org.phuchoang.ecp.messaging.revalidation;

/** Produces the {@code X-ECP-Signature} value the storefront verifies before acting on a callback. */
public interface PayloadSigner {

    String sign(String payload);
}
