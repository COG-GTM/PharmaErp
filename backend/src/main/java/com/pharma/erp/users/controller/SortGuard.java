package com.pharma.erp.users.controller;

import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Rejects sort properties outside an allow-list (400) instead of failing in the persistence layer. */
final class SortGuard {

    private SortGuard() {}

    static Pageable check(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new IllegalArgumentException("Cannot sort by '" + order.getProperty() + "'; allowed: " + allowed);
            }
        }
        return pageable;
    }
}
