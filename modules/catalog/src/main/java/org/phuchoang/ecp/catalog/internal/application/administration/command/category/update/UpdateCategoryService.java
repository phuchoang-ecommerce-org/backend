package org.phuchoang.ecp.catalog.internal.application.administration.command.category.update;

import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogLookups;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.administration.command.category.CategorySnapshot;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.CategoryChanged;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.model.CategoryHierarchyViolation;
import org.phuchoang.ecp.catalog.internal.domain.service.CategoryCommandService;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * `UC-ADM-02` — edits a category, including moving it. Whether a move is legal (`BR-CAT-03`, no
 * cycles) is the {@link Category} aggregate's decision; this service only loads the proposed parent
 * and translates a refusal into the caller's validation failure.
 */
@Service
public class UpdateCategoryService {

    private final CategoryCommandService categories;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public UpdateCategoryService(CategoryCommandService categories, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.categories = categories;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public CategorySnapshot update(CatalogCommandContext context, UUID categoryId, CategoryChange change) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_CATEGORIES);

        Category before = CatalogLookups.requireCategory(categories.find(categoryId));
        Category parent = CatalogLookups.optionalParent(change.parentId(), change.parentId() == null
            ? java.util.Optional.empty() : categories.find(change.parentId()));

        Category after;
        try {
            after = categories.change(before, change.parentId(), change.name(), change.imageUrl(), change.sortOrder(),
                change.featured(), parent);
        } catch (CategoryHierarchyViolation violation) {
            throw CatalogLookups.invalid("parentId");
        }

        events.publish(CategoryChanged.of(after), context);
        return CategorySnapshot.from(after);
    }
}
