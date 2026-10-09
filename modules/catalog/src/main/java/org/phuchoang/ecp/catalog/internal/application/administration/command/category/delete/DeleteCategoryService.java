package org.phuchoang.ecp.catalog.internal.application.administration.command.category.delete;

import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.CategoryChanged;
import org.phuchoang.ecp.catalog.internal.domain.service.CategoryCommandService;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.catalog.internal.application.error.ApplicationErrorCode;
import org.phuchoang.ecp.catalog.internal.application.error.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * `UC-ADM-02` — deletes an <em>empty</em> category only (`BR-CAT-03`): products or child categories
 * beneath it reject the command. Idempotent for a category that is already gone.
 */
@Service
public class DeleteCategoryService {

    private final CategoryCommandService categories;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public DeleteCategoryService(CategoryCommandService categories, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.categories = categories;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public void delete(CatalogCommandContext context, UUID categoryId) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_CATEGORIES);

        CategoryCommandService.DeletionDecision decision = categories.delete(categoryId);
        if (decision.missing()) {
            return;
        }
        if (!decision.accepted()) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION_FAILED,
                "Category cannot be deleted: productCount="
                + decision.assignedProductCount() + ", childCategoryCount=" + decision.childCategoryCount());
        }

        events.publish(CategoryChanged.removed(decision.category()), context);
    }
}
