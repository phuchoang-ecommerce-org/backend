package org.phuchoang.ecp.identity.internal.infrastructure.persistence.address;

import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** JPA mapping of {@code identity_address} (Database.md §4.1) — persistence shape only. */
@Entity
@Table(name = "identity_address")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AddressEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column
    private String label;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String line1;

    @Column
    private String line2;

    @Column(nullable = false)
    private String city;

    @Column
    private String region;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column
    private String phone;

    @Column(name = "is_default_shipping", nullable = false)
    private boolean defaultShipping;

    @Column(name = "is_default_billing", nullable = false)
    private boolean defaultBilling;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    AddressEntity(UUID id, UUID accountId, String label, String recipientName, String line1, String line2,
            String city, String region, String postalCode, String countryCode, String phone,
            boolean defaultShipping, boolean defaultBilling, Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.label = label;
        this.recipientName = recipientName;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.region = region;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
        this.phone = phone;
        this.defaultShipping = defaultShipping;
        this.defaultBilling = defaultBilling;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    void initializeAuditTimestamps(Instant now) { this.createdAt = now; this.updatedAt = now; }
}
