package com.pharma.erp.staff.service;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Restricts client-supplied sort properties to a whitelist so unknown fields are a 400, not a 500. */
final class Pageables {

    private Pageables() {
    }

    static Pageable withAllowedSort(Pageable pageable, Set<String> allowed, Sort defaultSort) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Unsupported sort property '" + order.getProperty() + "'; allowed: " + allowed);
            }
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort().and(Sort.by("id")) : defaultSort;
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
