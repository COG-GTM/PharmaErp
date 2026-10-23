package com.pharma.erp.inventory.repository;

import com.pharma.erp.inventory.entity.InventoryItem;
import com.pharma.erp.inventory.entity.StockStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Query building blocks; the stock-status predicates mirror {@link StockStatus#derive}. */
public final class InventorySpecifications {

    private InventorySpecifications() {
    }

    public static Specification<InventoryItem> all() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<InventoryItem> textMatches(String text) {
        String like = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), like, '\\'),
                cb.like(cb.lower(root.get("batchNo")), like, '\\'),
                cb.like(cb.lower(root.get("barcode")), like, '\\'));
    }

    public static Specification<InventoryItem> attributeEquals(String attribute, Object value) {
        return (root, query, cb) -> cb.equal(root.get(attribute), value);
    }

    public static Specification<InventoryItem> notExpired(LocalDate today) {
        return (root, query, cb) -> notExpired(root, cb, today);
    }

    /** Usable (not expired) batches with stock left whose expiry falls in {@code [today, until]}. */
    public static Specification<InventoryItem> expiringBetween(LocalDate today, LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.between(root.get("expiryDate"), today, until),
                cb.greaterThan(root.get("quantity"), 0));
    }

    /**
     * Usable batches at or below {@code threshold}, or at or below their own reorder level (falling
     * back to {@code defaultReorderLevel}) when no threshold is given. Includes out-of-stock batches.
     */
    public static Specification<InventoryItem> lowStock(Integer threshold, int defaultReorderLevel, LocalDate today) {
        return (root, query, cb) -> cb.and(
                notExpired(root, cb, today),
                threshold != null
                        ? cb.le(root.get("quantity"), threshold)
                        : cb.le(root.get("quantity"), effectiveReorderLevel(root, cb, defaultReorderLevel)));
    }

    public static Specification<InventoryItem> hasStockStatus(StockStatus status, int defaultReorderLevel,
            LocalDate today) {
        return (root, query, cb) -> {
            Expression<Integer> quantity = root.get("quantity");
            Expression<Integer> reorder = effectiveReorderLevel(root, cb, defaultReorderLevel);
            return switch (status) {
                case EXPIRED -> cb.lessThan(root.get("expiryDate"), today);
                case OUT_OF_STOCK -> cb.and(notExpired(root, cb, today), cb.le(quantity, 0));
                case LOW -> cb.and(notExpired(root, cb, today), cb.gt(quantity, 0), cb.le(quantity, reorder));
                case IN_STOCK -> cb.and(notExpired(root, cb, today), cb.gt(quantity, 0), cb.gt(quantity, reorder));
            };
        };
    }

    private static Predicate notExpired(Root<InventoryItem> root, CriteriaBuilder cb, LocalDate today) {
        return cb.or(cb.isNull(root.get("expiryDate")), cb.greaterThanOrEqualTo(root.get("expiryDate"), today));
    }

    private static Expression<Integer> effectiveReorderLevel(Root<InventoryItem> root, CriteriaBuilder cb,
            int defaultReorderLevel) {
        return cb.coalesce(root.<Integer>get("reorderLevel"), cb.literal(defaultReorderLevel));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
