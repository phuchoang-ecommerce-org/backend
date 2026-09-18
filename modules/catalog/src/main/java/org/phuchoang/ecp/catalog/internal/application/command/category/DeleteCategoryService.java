package org.phuchoang.ecp.catalog.internal.application.command.category;

import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.CategoryChanged;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * `UC-ADM-02` — deletes an <em>empty</em> category only (`BR-CAT-03`): products or child categories
 * beneath it reject the command. Idempotent for a category that is already gone.
 */
@Service
public class DeleteCategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public DeleteCategoryService(CategoryRepository categories, ProductRepository products,
            IdentityAuthorization authorization, CatalogEventPublisher events) {
        this.categories = categories;
        this.products = products;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public void delete(CatalogCommandContext context, UUID categoryId) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_CATEGORIES);

        Category before = categories.findById(categoryId).orElse(null);
        if (before == null) {
            return;
        }
        long productCount = products.countByCategoryId(categoryId);
        long childCategoryCount = categories.countByParentId(categoryId);
        if (productCount > 0 || childCategoryCount > 0) {
            throw new DomainException(GenErrorCode.VALIDATION_FAILED, "Category cannot be deleted: productCount="
                + productCount + ", childCategoryCount=" + childCategoryCount);
        }
        categories.deleteById(categoryId);

        events.publish(CategoryChanged.removed(before), context);
    }
}
