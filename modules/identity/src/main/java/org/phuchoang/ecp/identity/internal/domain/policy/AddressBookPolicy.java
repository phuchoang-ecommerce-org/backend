package org.phuchoang.ecp.identity.internal.domain.policy;

import java.util.Objects;
import java.util.UUID;

/** Decides shipping-default transitions without performing address persistence. */
public final class AddressBookPolicy {

    public AddressBookPlan plan(AddressBookContext context, AddressBookChange change) {
        Objects.requireNonNull(context, "context"); Objects.requireNonNull(change, "change");
        if (change.type() == ChangeType.REMOVE && context.targetIsDefaultShipping() && context.addressCount() > 1) {
            return AddressBookPlan.rejected(Reason.REPLACEMENT_DEFAULT_REQUIRED);
        }
        boolean targetDefault = switch (change.type()) {
            case ADD -> context.addressCount() == 0 || change.requestedDefaultShipping();
            case REPLACE -> change.requestedDefaultShipping();
            case REMOVE -> false;
        };
        boolean clearExisting = targetDefault && context.currentDefaultShippingId() != null
            && !context.currentDefaultShippingId().equals(change.targetAddressId());
        return AddressBookPlan.accepted(targetDefault, clearExisting);
    }

    public record AddressBookContext(long addressCount, UUID currentDefaultShippingId, boolean targetIsDefaultShipping) {
        public AddressBookContext { if (addressCount < 0) throw new IllegalArgumentException("address count cannot be negative."); }
    }
    public record AddressBookChange(ChangeType type, UUID targetAddressId, boolean requestedDefaultShipping) {
        public AddressBookChange { Objects.requireNonNull(type, "type"); }
        public static AddressBookChange add(boolean requestedDefaultShipping) { return new AddressBookChange(ChangeType.ADD, null, requestedDefaultShipping); }
    }
    public record AddressBookPlan(boolean accepted, boolean targetDefaultShipping, boolean clearExistingDefaultShipping, Reason reason) {
        static AddressBookPlan accepted(boolean targetDefault, boolean clearExisting) { return new AddressBookPlan(true, targetDefault, clearExisting, null); }
        static AddressBookPlan rejected(Reason reason) { return new AddressBookPlan(false, false, false, reason); }
    }
    public enum ChangeType { ADD, REPLACE, REMOVE }
    public enum Reason { REPLACEMENT_DEFAULT_REQUIRED }
}
