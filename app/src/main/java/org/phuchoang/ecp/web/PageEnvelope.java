package org.phuchoang.ecp.web;

import java.util.List;

/**
 * The envelope every collection endpoint returns (Integration Contract.md §3, {@code common.yaml#/PageEnvelope}).
 * There is no unpaginated list endpoint anywhere in this API.
 */
public record PageEnvelope<T>(List<T> items, Page page) {
}
