package org.phuchoang.ecp.identity.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised on successful registration (`UC-CUS-01`). ADR-0012 §4 — in-process transport only, never
 * Kafka. Carries the raw verification token only because this sprint stubs the notification
 * module as an in-process log listener standing in for a real email dispatch (Sprint 03 Review
 * Notes) — a real `NotificationListener` would instead receive an opaque reference and mint the
 * link itself, never the token (`NFR-SEC-07`).
 */
public record AccountRegistered(UUID accountId, String email, String verificationToken, Instant occurredAt) {
}
