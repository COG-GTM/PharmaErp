package com.pharma.erp.inventory.repository;

import com.pharma.erp.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryItemRepository
        extends JpaRepository<InventoryItem, Integer>, JpaSpecificationExecutor<InventoryItem> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.id = :id and i.archivedAt is null")
    Optional<InventoryItem> findActiveByIdForUpdate(@Param("id") Integer id);

    Optional<InventoryItem> findByIdAndArchivedAtIsNull(Integer id);

    @Query("""
            select count(i) > 0 from InventoryItem i
            where i.barcode = :barcode and i.batchNo = :batchNo
              and ((:storeId is null and i.storeId is null) or i.storeId = :storeId)
              and i.archivedAt is null
              and (:excludeId is null or i.id <> :excludeId)
            """)
    boolean existsBatch(@Param("barcode") String barcode, @Param("batchNo") String batchNo,
            @Param("storeId") Integer storeId, @Param("excludeId") Integer excludeId);
}
