package com.pharma.erp.clients;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Search filters for {@code GET /api/clients}: name (substring, case-insensitive) and phone (digits only). */
final class ClientSpecifications {

    private ClientSpecifications() {
    }

    static Specification<Client> search(String name, String phone, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(name)) {
                predicates.add(nameLike(root, cb, name));
            }
            if (hasText(phone)) {
                predicates.add(phoneLike(root, cb, phone));
            }
            if (hasText(q)) {
                predicates.add(cb.or(nameLike(root, cb, q), phoneLike(root, cb, q)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private static Predicate nameLike(Root<Client> root, CriteriaBuilder cb, String term) {
        return cb.like(cb.lower(root.get("name")), containsPattern(term.trim().toLowerCase(Locale.ROOT)), '\\');
    }

    private static Predicate phoneLike(Root<Client> root, CriteriaBuilder cb, String term) {
        String digits = digitsOnly(term);
        if (digits.isEmpty()) {
            return cb.disjunction();
        }
        Expression<String> stored = root.get("phone");
        for (String separator : List.of(" ", "-", "+", "(", ")", ".")) {
            stored = cb.function("REPLACE", String.class, stored, cb.literal(separator), cb.literal(""));
        }
        return cb.like(stored, containsPattern(digits), '\\');
    }

    private static String containsPattern(String term) {
        String escaped = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
