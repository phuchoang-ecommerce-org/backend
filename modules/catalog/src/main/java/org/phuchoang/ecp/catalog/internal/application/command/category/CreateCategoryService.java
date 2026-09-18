package org.phuchoang.ecp.catalog.internal.application.command.category;

import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogLookups;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.CategoryChanged;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.model.Slugs;
import org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** `UC-ADM-02` — creates a category beneath an existing parent (or as a root). */
@Service
public class CreateCategoryService {

    private final CategoryRepository categories;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public CreateCategoryService(CategoryRepository categories, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.categories = categories;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public CategorySnapshot create(CatalogCommandContext context, CreateCategory command) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_CATEGORIES);

        Category parent = CatalogLookups.optionalParent(categories, command.parentId());
        Category category = categories.save(Category.create(UUID.randomUUID(), command.parentId(), command.name(),
            Slugs.normalize(command.name()), command.imageUrl(), command.sortOrder(), command.featured(), parent));

        events.publish(CategoryChanged.of(category), context);
        return CategorySnapshot.from(category);
    }
}
