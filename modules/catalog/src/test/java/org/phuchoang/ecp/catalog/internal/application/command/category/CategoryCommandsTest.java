package org.phuchoang.ecp.catalog.internal.application.command.category;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.CatalogDomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.event.CategoryChanged;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** `UC-ADM-02`: slug policy, hierarchy invariants (`BR-CAT-03`) and empty-only deletion. */
class CategoryCommandsTest {

    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final IdentityAuthorization authorization = mock(IdentityAuthorization.class);
    private final CatalogEventPublisher events = mock(CatalogEventPublisher.class);
    private final CatalogCommandContext context = new CatalogCommandContext(
        new IdentityActor(UUID.randomUUID(), Set.of("ADMINISTRATOR")), UUID.randomUUID());

    @Test
    void creationDerivesTheSlugFromTheNameAndThePathFromTheParent() {
        Category parent = new Category(UUID.randomUUID(), null, "Home", "home", "/home/", 0, null, 0, false);
        when(categories.findById(parent.id())).thenReturn(Optional.of(parent));
        when(categories.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CategorySnapshot created = new CreateCategoryService(categories, authorization, events)
            .create(context, new CreateCategory(parent.id(), "Living Room & Décor", null, 2, true));

        verify(authorization).assertAuthorized(context.caller(), CatalogPermissions.MANAGE_CATEGORIES);
        assertThat(created.slug()).isEqualTo("living-room-d-cor");
        assertThat(created.path()).isEqualTo("/home/" + created.id() + "/");
        assertThat(created.depth()).isEqualTo(1);
        assertThat(captured().eventType()).isEqualTo("CategoryChanged");
    }

    @Test
    void movingBeneathADescendantIsRefusedByTheAggregateAndReportedAsInvalidParent() {
        Category root = new Category(UUID.randomUUID(), null, "Root", "root", "/root/", 0, null, 0, false);
        Category child = new Category(UUID.randomUUID(), root.id(), "Child", "child", "/root/child/", 1, null, 0, false);
        when(categories.findById(root.id())).thenReturn(Optional.of(root));
        when(categories.findById(child.id())).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> new UpdateCategoryService(categories, authorization, events)
            .update(context, root.id(), new CategoryChange("Root", child.id(), null, 0, false)))
            .isInstanceOf(DomainException.class).hasMessage("Invalid parentId.");
        verify(categories, never()).save(any());
    }

    @Test
    void aNonEmptyCategoryCannotBeDeletedAndAMissingOneIsIgnored() {
        Category category = new Category(UUID.randomUUID(), null, "Root", "root", "/root/", 0, null, 0, false);
        when(categories.findById(category.id())).thenReturn(Optional.of(category));
        when(products.countByCategoryId(category.id())).thenReturn(2L);
        DeleteCategoryService service = new DeleteCategoryService(categories, products, authorization, events);

        assertThatThrownBy(() -> service.delete(context, category.id()))
            .isInstanceOf(DomainException.class).hasMessageContaining("productCount=2");
        verify(categories, never()).deleteById(any());

        UUID missing = UUID.randomUUID();
        when(categories.findById(missing)).thenReturn(Optional.empty());
        service.delete(context, missing);
        verify(events, never()).publish(any(), any());
    }

    @Test
    void deletingAnEmptyCategoryAnnouncesItsRemoval() {
        Category category = new Category(UUID.randomUUID(), null, "Root", "root", "/root/", 0, null, 0, false);
        when(categories.findById(category.id())).thenReturn(Optional.of(category));

        new DeleteCategoryService(categories, products, authorization, events).delete(context, category.id());

        verify(categories).deleteById(category.id());
        CategoryChanged removed = (CategoryChanged) captured();
        assertThat(removed.removed()).isTrue();
        assertThat(removed.category()).isEqualTo(category);
    }

    private CatalogDomainEvent captured() {
        ArgumentCaptor<CatalogDomainEvent> emitted = ArgumentCaptor.forClass(CatalogDomainEvent.class);
        verify(events).publish(emitted.capture(), eq(context));
        return emitted.getValue();
    }
}
