package org.phuchoang.ecp.catalog.internal.application.command.product;

import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogPermissions;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * `UC-ADM-01` bulk amendment. Transaction semantics are deliberate: <em>each item is its own
 * transaction</em> (a separate bean's {@code @Transactional} method, never self-invocation), so
 * partial success is the contract — a failed item is reported with the code of the
 * {@link DomainException} that rejected it and never rolls back its siblings. This service itself is
 * not transactional. Authorization is checked once up front so an unauthorized caller gets
 * {@code FORBIDDEN} instead of N identical per-item failures.
 */
@Service
public class BulkAmendmentService {

    private static final Logger log = LoggerFactory.getLogger(BulkAmendmentService.class);

    private final UpdateProductService updates;
    private final IdentityAuthorization authorization;

    public BulkAmendmentService(UpdateProductService updates, IdentityAuthorization authorization) {
        this.updates = updates;
        this.authorization = authorization;
    }

    public List<BulkAmendmentOutcome> amend(CatalogCommandContext context, List<BulkAmendment> items) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);
        return items.stream().map(item -> amendOne(context, item)).toList();
    }

    private BulkAmendmentOutcome amendOne(CatalogCommandContext context, BulkAmendment item) {
        try {
            updates.update(context, item.productId(), item.change());
            return BulkAmendmentOutcome.applied(item.productId());
        } catch (DomainException exception) {
            return BulkAmendmentOutcome.failed(item.productId(), exception.errorCode().code(), exception.getMessage());
        } catch (RuntimeException exception) {
            log.warn("Bulk amendment of product {} failed unexpectedly", item.productId(), exception);
            return BulkAmendmentOutcome.failed(item.productId(), GenErrorCode.UNMAPPED_ERROR.code(),
                "The amendment could not be applied.");
        }
    }
}
