package org.phuchoang.ecp.ordering.internal.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.phuchoang.ecp.ordering.internal.domain.repository.CheckoutRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The Draft order assembled by checkout. Its state is authoritative, including the confirmation
 * token that Sprint 18 will require before placement.
 */
@AggregateRoot
public record Checkout(@Identity UUID id, UUID customerId, String orderNumber, List<CheckoutLine> lines,
        CheckoutAddress shippingAddress, CheckoutAddress billingAddress, ShippingOption shippingOption, UUID promotionId,
        String promotionCode, Money discount, String confirmationToken, long version, Instant createdAt) {

    public Checkout {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(orderNumber, "orderNumber");
        lines = List.copyOf(lines);
        if (lines.isEmpty()) throw new NotPurchasable("Cart has no purchasable line.");
        String currency = lines.getFirst().unitPrice().currency();
        if (lines.stream().anyMatch(line -> !currency.equals(line.unitPrice().currency()))) {
            throw new IllegalArgumentException("Checkout lines must share a currency.");
        }
        discount = discount == null ? Money.zero(currency) : discount;
        BigDecimal subtotal = lines.stream().map(CheckoutLine::lineTotal).map(Money::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!currency.equals(discount.currency()) || discount.amount().compareTo(subtotal) > 0) {
            throw new DiscountExceedsSubtotal();
        }
    }

    public static Checkout start(UUID id, UUID customerId, String orderNumber, List<CheckoutLine> lines) {
        return new Checkout(id, customerId, orderNumber, lines, null, null, null, null, null,
            Money.zero(lines.isEmpty() ? "USD" : lines.getFirst().unitPrice().currency()), null, 0, null);
    }

    /** Persists the complete Checkout aggregate; child lines never have a standalone write operation. */
    public Checkout save(CheckoutRepository repository) {
        return repository.save(this);
    }

    public Checkout setShippingAddress(CheckoutAddress address, List<ShippingOption> options, CheckoutRepository repository) {
        return setShippingAddress(address, options).save(repository);
    }

    public Checkout setBillingAddress(CheckoutAddress address, CheckoutRepository repository) {
        return setBillingAddress(address).save(repository);
    }

    public Checkout selectShippingOption(ShippingOption option, CheckoutRepository repository) {
        return selectShippingOption(option).save(repository);
    }

    public Checkout applyVoucher(UUID newPromotionId, String code, BigDecimal amount, CheckoutRepository repository) {
        return applyVoucher(newPromotionId, code, amount).save(repository);
    }

    public Checkout removeVoucher(CheckoutRepository repository) {
        return removeVoucher().save(repository);
    }

    public Checkout refresh(List<CheckoutLine> currentLines, String newConfirmationToken, CheckoutRepository repository) {
        return refresh(currentLines, newConfirmationToken).save(repository);
    }

    public Money subtotal() {
        String currency = lines.getFirst().unitPrice().currency();
        return new Money(lines.stream().map(CheckoutLine::lineTotal).map(Money::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add), currency);
    }

    public Money shippingFee() {
        return shippingOption == null ? Money.zero(subtotal().currency()) : shippingOption.fee();
    }

    public Money total() {
        return new Money(subtotal().amount().subtract(discount.amount()).add(shippingFee().amount()), subtotal().currency());
    }

    public Checkout setShippingAddress(CheckoutAddress address, List<ShippingOption> options) {
        return new Checkout(id, customerId, orderNumber, lines, Objects.requireNonNull(address), billingAddress,
            null, promotionId, promotionCode, discount, null, version, createdAt);
    }

    public Checkout setBillingAddress(CheckoutAddress address) {
        return new Checkout(id, customerId, orderNumber, lines, shippingAddress, Objects.requireNonNull(address),
            shippingOption, promotionId, promotionCode, discount, null, version, createdAt);
    }

    public Checkout selectShippingOption(ShippingOption option) {
        return new Checkout(id, customerId, orderNumber, lines, shippingAddress, billingAddress,
            Objects.requireNonNull(option), promotionId, promotionCode, discount, null, version, createdAt);
    }

    public Checkout applyVoucher(UUID newPromotionId, String code, BigDecimal amount) {
        BigDecimal cappedAmount = amount.max(BigDecimal.ZERO).min(subtotal().amount());
        return new Checkout(id, customerId, orderNumber, lines, shippingAddress, billingAddress, shippingOption,
            Objects.requireNonNull(newPromotionId), Objects.requireNonNull(code), new Money(cappedAmount, subtotal().currency()),
            null, version, createdAt);
    }

    public Checkout removeVoucher() {
        return new Checkout(id, customerId, orderNumber, lines, shippingAddress, billingAddress, shippingOption,
            null, null, Money.zero(subtotal().currency()), null, version, createdAt);
    }

    public Checkout refresh(List<CheckoutLine> currentLines, String newConfirmationToken) {
        return new Checkout(id, customerId, orderNumber, currentLines, shippingAddress, billingAddress, shippingOption,
            promotionId, promotionCode, discount, Objects.requireNonNull(newConfirmationToken), version, createdAt);
    }

    public boolean ready() {
        return shippingAddress != null && billingAddress != null && shippingOption != null;
    }

    public static final class NotPurchasable extends RuntimeException {
        public NotPurchasable(String message) { super(message); }
    }

    public static final class DiscountExceedsSubtotal extends RuntimeException {
        public DiscountExceedsSubtotal() { super("Discount exceeds the discountable subtotal."); }
    }
}
