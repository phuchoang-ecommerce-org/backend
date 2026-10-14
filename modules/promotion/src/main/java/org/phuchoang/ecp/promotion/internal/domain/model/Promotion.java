package org.phuchoang.ecp.promotion.internal.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.phuchoang.ecp.promotion.internal.domain.repository.PromotionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Authoritative promotion aggregate. A preview never changes this object;
 * {@link #claim(UUID, UUID, BigDecimal, Instant)} is the only usage-counter transition.
 */
@AggregateRoot
public record Promotion(
        @Identity UUID id,
        String code,
        String name,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        String currency,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderAmount,
        Instant validFrom,
        Instant validUntil,
        int usageLimit,
        int usageCount,
        int perCustomerLimit,
        int priority,
        boolean stackable,
        PromotionStatus status,
        long version) {

    public Promotion {
        Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Promotion name is required.");
        Objects.requireNonNull(discountType, "discountType");
        if (discountValue == null || discountValue.signum() <= 0) {
            throw new IllegalArgumentException("Promotion discount must be positive.");
        }
        if (discountType == DiscountType.PERCENTAGE && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount must not exceed 100.");
        }
        if (discountType != DiscountType.PERCENTAGE && (currency == null || !currency.matches("[A-Z]{3}"))) {
            throw new IllegalArgumentException("A monetary promotion requires an ISO currency.");
        }
        Objects.requireNonNull(validFrom, "validFrom");
        Objects.requireNonNull(validUntil, "validUntil");
        if (!validUntil.isAfter(validFrom)) throw new IllegalArgumentException("Promotion end must be after its start.");
        if (usageLimit <= 0) throw new IllegalArgumentException("Promotion usage limit is required and must be positive.");
        if (usageCount < 0 || usageCount > usageLimit) throw new IllegalArgumentException("Promotion usage count is invalid.");
        if (perCustomerLimit <= 0) throw new IllegalArgumentException("Per-customer usage limit is required and must be positive.");
        if (minOrderAmount != null && minOrderAmount.signum() < 0) {
            throw new IllegalArgumentException("Minimum order amount must not be negative.");
        }
        if (maxDiscountAmount != null && maxDiscountAmount.signum() <= 0) {
            throw new IllegalArgumentException("Maximum discount amount must be positive.");
        }
        Objects.requireNonNull(status, "status");
    }

    public static Promotion draft(UUID id, String name, DiscountType type, BigDecimal value, String currency,
            BigDecimal maxDiscountAmount, BigDecimal minOrderAmount, Instant validFrom, Instant validUntil,
            Integer usageLimit, Integer perCustomerLimit, int priority, boolean stackable) {
        return draft(id, null, name, null, type, value, currency, maxDiscountAmount, minOrderAmount, validFrom,
            validUntil, usageLimit, perCustomerLimit, priority, stackable);
    }

    public static Promotion draft(UUID id, String code, String name, String description, DiscountType type,
            BigDecimal value, String currency, BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
            Instant validFrom, Instant validUntil, Integer usageLimit, Integer perCustomerLimit, int priority,
            boolean stackable) {
        return new Promotion(id, code, name, description, type, value, currency, maxDiscountAmount, minOrderAmount,
            validFrom, validUntil, usageLimit == null ? 0 : usageLimit, 0,
            perCustomerLimit == null ? 0 : perCustomerLimit, priority, stackable, PromotionStatus.DRAFT, 0);
    }

    /** Creates and persists a new root through the aggregate's domain repository. */
    public static Promotion create(CreatePromotion request, PromotionRepository repository) {
        if (request.validUntil() == null || !request.validUntil().isAfter(request.now())) {
            throw new IllegalArgumentException("Promotion validity period has already elapsed.");
        }
        Promotion promotion = draft(UUID.randomUUID(), request.code(), request.name(), request.description(),
            request.discountType(), request.discountValue(), request.currency(), request.maxDiscountAmount(),
            request.minOrderAmount(), request.validFrom(), request.validUntil(), request.usageLimit(),
            request.perCustomerLimit(), request.priority(), request.stackable());
        repository.save(promotion);
        return promotion;
    }

    public Promotion activate(Instant now) {
        if (now.isAfter(validUntil)) throw new IllegalArgumentException("An elapsed promotion cannot be activated.");
        return copy(status == PromotionStatus.EXPIRED ? PromotionStatus.EXPIRED : PromotionStatus.ACTIVE, usageCount);
    }

    public Evaluation evaluate(UUID customerId, BigDecimal discountableAmount, Instant now) {
        if (status != PromotionStatus.ACTIVE || now.isBefore(validFrom) || !now.isBefore(validUntil)
                || usageCount >= usageLimit) return Evaluation.ineligible();
        if (discountableAmount == null || discountableAmount.signum() < 0) {
            throw new IllegalArgumentException("Discountable amount must not be negative.");
        }
        if (minOrderAmount != null && discountableAmount.compareTo(minOrderAmount) < 0) return Evaluation.ineligible();
        return new Evaluation(true, calculateDiscount(discountableAmount));
    }

    public Promotion claim(UUID orderId, UUID customerId, BigDecimal discountableAmount, Instant now) {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(customerId, "customerId");
        if (!evaluate(customerId, discountableAmount, now).eligible()) throw new PromotionUsageExhaustedException();
        return copy(status, usageCount + 1);
    }

    /** Creates voucher children under this promotion aggregate. */
    public List<PromotionVoucher> generateVouchers(List<VoucherDefinition> definitions) {
        return definitions.stream()
            .map(definition -> new PromotionVoucher(definition.id(), id, definition.code(), definition.singleUse(), null))
            .toList();
    }

    /** Loads the root, generates its voucher children, and persists them through its repository. */
    public static List<PromotionVoucher> generateVouchers(GenerateVouchers request, SecureRandom random,
            PromotionRepository repository) {
        if (request.count() < 1 || request.count() > 10_000) {
            throw new IllegalArgumentException("Voucher batch size must be between 1 and 10000.");
        }
        Promotion promotion = repository.findById(request.promotionId())
            .orElseThrow(() -> new IllegalArgumentException("Promotion was not found."));
        List<VoucherDefinition> definitions = java.util.stream.IntStream.range(0, request.count())
            .mapToObj(ignored -> new VoucherDefinition(UUID.randomUUID(), normalizedPrefix(request.prefix()) + randomCode(random),
                request.singleUse()))
            .toList();
        List<PromotionVoucher> vouchers = promotion.generateVouchers(definitions);
        repository.appendVouchers(promotion, vouchers);
        return vouchers;
    }

    /** Performs the locked redemption transition and atomically records its commercial evidence. */
    public static Redemption redeem(RedeemPromotion request, PromotionRepository repository) {
        Promotion promotion = repository.findForRedemption(request.promotionId())
            .orElseThrow(RedemptionUnavailableException::new);
        try {
            if (repository.redemptionCountForCustomer(request.promotionId(), request.customerId())
                    >= promotion.perCustomerLimit()) {
                throw new PromotionUsageExhaustedException();
            }
            Promotion claimed = promotion.claim(request.orderId(), request.customerId(), request.discountableAmount(), request.now());
            BigDecimal discount = claimed.calculateDiscount(request.discountableAmount());
            PromotionRedemption redemption = new PromotionRedemption(UUID.randomUUID(), request.promotionId(), request.orderId(),
                request.customerId(), discount, claimed.currency(), null, request.now());
            repository.recordRedemption(claimed, redemption);
            return new Redemption(claimed.id(), request.orderId(), request.customerId(), discount, claimed.currency());
        } catch (PromotionUsageExhaustedException exhausted) {
            throw new RedemptionUnavailableException();
        }
    }

    public BigDecimal calculateDiscount(BigDecimal discountableAmount) {
        BigDecimal proposed = switch (discountType) {
            case PERCENTAGE -> discountableAmount.multiply(discountValue).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> discountValue;
            case FREE_SHIPPING -> discountableAmount;
        };
        if (maxDiscountAmount != null) proposed = proposed.min(maxDiscountAmount);
        return proposed.min(discountableAmount).setScale(4, RoundingMode.HALF_UP);
    }

    private Promotion copy(PromotionStatus nextStatus, int nextUsageCount) {
        return new Promotion(id, code, name, description, discountType, discountValue, currency, maxDiscountAmount,
            minOrderAmount, validFrom, validUntil, usageLimit, nextUsageCount, perCustomerLimit, priority,
            stackable, nextStatus, version);
    }

    public record Evaluation(boolean eligible, BigDecimal discountAmount) {
        static Evaluation ineligible() { return new Evaluation(false, null); }
    }

    public record VoucherDefinition(UUID id, String code, boolean singleUse) {
        public VoucherDefinition {
            Objects.requireNonNull(id, "id");
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("Voucher code is required.");
            }
        }
    }

    public record CreatePromotion(String code, String name, String description, DiscountType discountType,
            BigDecimal discountValue, String currency, BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
            Instant validFrom, Instant validUntil, Integer usageLimit, Integer perCustomerLimit, int priority,
            boolean stackable, Instant now) {
    }

    public record GenerateVouchers(UUID promotionId, int count, boolean singleUse, String prefix) {
    }

    public record RedeemPromotion(UUID promotionId, UUID orderId, UUID customerId, BigDecimal discountableAmount,
            Instant now) {
    }

    public record Redemption(UUID promotionId, UUID orderId, UUID customerId, BigDecimal discountAmount,
            String currency) {
    }

    public static final class RedemptionUnavailableException extends RuntimeException {
    }

    private static String normalizedPrefix(String prefix) {
        return prefix == null ? "" : prefix.trim().toUpperCase() + "-";
    }

    private static String randomCode(SecureRandom random) {
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
