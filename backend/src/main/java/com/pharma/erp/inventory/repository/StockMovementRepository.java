package com.pharma.erp.inventory.repository;

import com.pharma.erp.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {

    Page<StockMovement> findByItemIdOrderByTrackedAtDescIdDesc(Integer itemId, Pageable pageable);
}
