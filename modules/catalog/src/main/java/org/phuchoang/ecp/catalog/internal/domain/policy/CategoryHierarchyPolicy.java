package org.phuchoang.ecp.catalog.internal.domain.policy;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Pure multi-node category hierarchy decision for {@code BR-CAT-03}. */
public final class CategoryHierarchyPolicy {

    public CategoryMoveDecision planMove(CategoryMoveContext context, CategorySubtreeSnapshot subtree) {
        Objects.requireNonNull(context, "context"); Objects.requireNonNull(subtree, "subtree");
        if (context.proposedParentId() != null && context.proposedParentId().equals(context.categoryId())) {
            return CategoryMoveDecision.rejected(Reason.SELF_PARENT);
        }
        if (context.proposedParentPath() != null && context.proposedParentPath().startsWith(context.currentPath())) {
            return CategoryMoveDecision.rejected(Reason.DESCENDANT_PARENT);
        }
        String path = context.proposedParentPath() == null ? "/" + context.categoryId() + "/"
            : context.proposedParentPath() + context.categoryId() + "/";
        int depth = context.proposedParentDepth() == null ? 0 : context.proposedParentDepth() + 1;
        return CategoryMoveDecision.accepted(path, depth, subtree.descendantIds());
    }

    public CategoryChangeDecision validateDeletion(CategoryOccupancy occupancy) {
        Objects.requireNonNull(occupancy, "occupancy");
        if (occupancy.childCategoryCount() > 0) return CategoryChangeDecision.rejected(Reason.HAS_CHILDREN);
        if (occupancy.assignedProductCount() > 0) return CategoryChangeDecision.rejected(Reason.HAS_PRODUCTS);
        return CategoryChangeDecision.allowed();
    }

    public record CategoryMoveContext(UUID categoryId, String currentPath, UUID proposedParentId,
                                      String proposedParentPath, Integer proposedParentDepth) {
        public CategoryMoveContext {
            Objects.requireNonNull(categoryId, "categoryId");
            if (currentPath == null || !currentPath.startsWith("/") || !currentPath.endsWith("/")) throw new IllegalArgumentException("current path is invalid.");
        }
    }
    public record CategorySubtreeSnapshot(List<UUID> descendantIds) {
        public CategorySubtreeSnapshot { descendantIds = List.copyOf(Objects.requireNonNull(descendantIds, "descendantIds")); }
    }
    public record CategoryMoveDecision(boolean accepted, String rootPath, int rootDepth, List<UUID> descendants, Reason reason) {
        static CategoryMoveDecision accepted(String path, int depth, List<UUID> descendants) { return new CategoryMoveDecision(true, path, depth, descendants, null); }
        static CategoryMoveDecision rejected(Reason reason) { return new CategoryMoveDecision(false, null, 0, List.of(), reason); }
    }
    public record CategoryOccupancy(long childCategoryCount, long assignedProductCount) {
        public CategoryOccupancy { if (childCategoryCount < 0 || assignedProductCount < 0) throw new IllegalArgumentException("counts cannot be negative."); }
    }
    public record CategoryChangeDecision(boolean accepted, Reason reason) {
        static CategoryChangeDecision allowed() { return new CategoryChangeDecision(true, null); }
        static CategoryChangeDecision rejected(Reason reason) { return new CategoryChangeDecision(false, reason); }
    }
    public enum Reason { SELF_PARENT, DESCENDANT_PARENT, HAS_CHILDREN, HAS_PRODUCTS }
}
