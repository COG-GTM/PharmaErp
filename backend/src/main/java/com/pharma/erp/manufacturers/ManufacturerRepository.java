package com.pharma.erp.manufacturers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManufacturerRepository extends JpaRepository<Manufacturer, Integer> {

    @Query("""
            SELECT m FROM Manufacturer m
            WHERE (:type IS NULL OR m.type = :type)
              AND (:q IS NULL
                   OR LOWER(m.name) LIKE :q
                   OR LOWER(m.licenceNumber) LIKE :q
                   OR LOWER(m.city) LIKE :q
                   OR LOWER(m.contactPerson) LIKE :q)
            """)
    Page<Manufacturer> search(@Param("type") ManufacturerType type, @Param("q") String q, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    boolean existsByLicenceNumberIgnoreCase(String licenceNumber);

    boolean existsByLicenceNumberIgnoreCaseAndIdNot(String licenceNumber, Integer id);
}
