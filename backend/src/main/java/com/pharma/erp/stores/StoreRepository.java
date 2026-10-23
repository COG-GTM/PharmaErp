package com.pharma.erp.stores;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, Integer> {

    boolean existsByCode(String code);

    boolean existsByName(String name);

    boolean existsByCodeAndIdNot(String code, Integer id);

    boolean existsByNameAndIdNot(String name, Integer id);

    @Query("""
            SELECT s FROM Store s
            WHERE LOWER(s.code) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '!'
               OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '!'
               OR LOWER(s.address) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '!'
            """)
    Page<Store> search(@Param("q") String q, Pageable pageable);

    /** Escapes LIKE metacharacters so {@link #search} matches {@code text} literally. */
    static String likeLiteral(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
