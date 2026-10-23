package com.pharma.erp.inventory.controller;

import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.inventory.dto.InventoryItemCreateRequest;
import com.pharma.erp.inventory.dto.InventoryItemResponse;
import com.pharma.erp.inventory.dto.InventoryItemUpdateRequest;
import com.pharma.erp.inventory.dto.ItemStatusResponse;
import com.pharma.erp.inventory.dto.StockAdjustmentRequest;
import com.pharma.erp.inventory.dto.StockAdjustmentResponse;
import com.pharma.erp.inventory.dto.StockMovementResponse;
import com.pharma.erp.inventory.entity.StockStatus;
import com.pharma.erp.inventory.service.InventoryService;
import com.pharma.erp.inventory.service.InventoryService.SearchCriteria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Inventory batches, stock adjustments, tracking history and expiry / low-stock views. Writes need SCOPE_admin. */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Set<String> SORTABLE = Set.of(
            "id", "name", "batchNo", "expiryDate", "quantity", "unitPrice", "barcode", "storeId", "manufacturerId",
            "deliveryDate", "createdAt", "updatedAt");

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<InventoryItemResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer storeId,
            @RequestParam(required = false) Integer manufacturerId,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) StockStatus stockStatus,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.search(new SearchCriteria(q, storeId, manufacturerId, barcode, batchNo, stockStatus),
                checkedSort(pageable));
    }

    @GetMapping("/expiring")
    public PageResponse<InventoryItemResponse> expiring(
            @RequestParam(defaultValue = "30") @Min(0) @Max(3650) int days,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.expiring(days, checkedSort(pageable));
    }

    @GetMapping("/low-stock")
    public PageResponse<InventoryItemResponse> lowStock(
            @RequestParam(required = false) @Min(0) Integer threshold,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.lowStockItems(threshold, checkedSort(pageable));
    }

    @GetMapping("/statuses")
    public List<ItemStatusResponse> statuses() {
        return service.statuses();
    }

    @GetMapping("/{id:\\d+}")
    public InventoryItemResponse get(@PathVariable Integer id) {
        return service.get(id);
    }

    @GetMapping("/{id:\\d+}/movements")
    public PageResponse<StockMovementResponse> movements(@PathVariable Integer id,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.movements(id, pageable);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody InventoryItemCreateRequest request,
            Authentication authentication) {
        InventoryItemResponse created = service.create(request, authentication.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public InventoryItemResponse update(@PathVariable Integer id,
            @Valid @RequestBody InventoryItemUpdateRequest request, Authentication authentication) {
        return service.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id:\\d+}/adjust")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public StockAdjustmentResponse adjust(@PathVariable Integer id, @Valid @RequestBody StockAdjustmentRequest request,
            Authentication authentication) {
        return service.adjust(id, request, authentication.getName());
    }

    private static Pageable checkedSort(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new IllegalArgumentException("Cannot sort by '" + order.getProperty() + "'; sortable fields: "
                        + SORTABLE.stream().sorted().toList());
            }
        }
        return pageable.getSort().isSorted()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        pageable.getSort().and(Sort.by("id")))
                : pageable;
    }
}
