package org.phuchoang.ecp.cart.internal.application.cart.expiry;

import org.phuchoang.ecp.cart.internal.domain.service.CartCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** UC-CRT-06 application workflow. It owns no inventory operation because carts reserve no stock. */
@Service
public class CartExpiryService {
    private static final int BATCH_SIZE = 100;

    private final CartCommandService commands;
    private final CartExpiryDeferral deferral;
    private final Clock clock;

    public CartExpiryService(CartCommandService commands, CartExpiryDeferral deferral, Clock clock) {
        this.commands = commands;
        this.deferral = deferral;
        this.clock = clock;
    }

    /** A failed invocation rolls back, leaves carts active, and lets the next scheduled run retry. */
    @Transactional
    public void expireDueCarts() {
        commands.expireDueCarts(clock.instant(), BATCH_SIZE, deferral::hasActiveCheckoutOrDraftOrder);
    }
}
