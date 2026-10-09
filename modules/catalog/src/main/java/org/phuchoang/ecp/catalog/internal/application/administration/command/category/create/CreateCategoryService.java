package org.phuchoang.ecp.catalog.internal.application.administration.command.category.create;

import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogLookups;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.administration.command.category.CategorySnapshot;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.CategoryChanged;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.model.Slugs;
import org.phuchoang.ecp.catalog.internal.domain.service.CategoryCommandService;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** `UC-ADM-02` — creates a category beneath an existing parent (or as a root). */
@Service
public class CreateCategoryService {

    private final CategoryCommandService categories;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public CreateCategoryService(CategoryCommandService categories, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.categories = categories;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public CategorySnapshot create(CatalogCommandContext context, CreateCategory command) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_CATEGORIES);

        Category parent = CatalogLookups.optionalParent(command.parentId(), command.parentId() == null
            ? java.util.Optional.empty() : categories.find(command.parentId()));
        Category category = categories.create(UUID.randomUUID(), command.parentId(), command.name(),
            Slugs.normalize(command.name()), command.imageUrl(), command.sortOrder(), command.featured(), parent);

        events.publish(CategoryChanged.of(category), context);
        return CategorySnapshot.from(category);
    }
}
