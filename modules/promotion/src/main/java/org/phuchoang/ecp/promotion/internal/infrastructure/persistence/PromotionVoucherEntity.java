package org.phuchoang.ecp.promotion.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promotion_voucher")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
class PromotionVoucherEntity {
    @Id private UUID id;
    @Column(name = "promotion_id") private UUID promotionId;
    private String code;
    @Column(name = "single_use") private boolean singleUse;
    @Column(name = "redeemed_at") private Instant redeemedAt;
    @Column(name = "created_at") private Instant createdAt;
}
