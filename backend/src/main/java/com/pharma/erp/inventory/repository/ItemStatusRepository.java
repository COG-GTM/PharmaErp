package com.pharma.erp.inventory.repository;

import com.pharma.erp.inventory.entity.ItemStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemStatusRepository extends JpaRepository<ItemStatus, Integer> {

    Optional<ItemStatus> findByNameIgnoreCase(String name);
}
