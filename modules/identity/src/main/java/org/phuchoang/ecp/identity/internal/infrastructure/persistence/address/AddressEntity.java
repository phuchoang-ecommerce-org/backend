package org.phuchoang.ecp.identity.internal.infrastructure.persistence.address;

import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** JPA mapping of {@code identity_address} (Database.md §4.1) — persistence shape only. */
@Entity
@Table(name = "identity_address")
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
    private Instant updatedAt;

    protected AddressEntity() {
        // JPA
    }

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

    UUID getId() {
        return id;
    }
    public void setId(UUID id) { this.id = id; }

    UUID getAccountId() {
        return accountId;
    }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    String getLabel() {
        return label;
    }
    public void setLabel(String label) { this.label = label; }

    String getRecipientName() {
        return recipientName;
    }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    String getLine1() {
        return line1;
    }
    public void setLine1(String line1) { this.line1 = line1; }

    String getLine2() {
        return line2;
    }
    public void setLine2(String line2) { this.line2 = line2; }

    String getCity() {
        return city;
    }
    public void setCity(String city) { this.city = city; }

    String getRegion() {
        return region;
    }
    public void setRegion(String region) { this.region = region; }

    String getPostalCode() {
        return postalCode;
    }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    String getCountryCode() {
        return countryCode;
    }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    String getPhone() {
        return phone;
    }
    public void setPhone(String phone) { this.phone = phone; }

    boolean isDefaultShipping() {
        return defaultShipping;
    }
    public void setDefaultShipping(boolean defaultShipping) { this.defaultShipping = defaultShipping; }

    boolean isDefaultBilling() {
        return defaultBilling;
    }
    public void setDefaultBilling(boolean defaultBilling) { this.defaultBilling = defaultBilling; }

    Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    void initializeAuditTimestamps(Instant now) { this.createdAt = now; this.updatedAt = now; }
}
