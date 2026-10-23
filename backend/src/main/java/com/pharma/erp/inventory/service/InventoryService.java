package com.pharma.erp.inventory.service;

import static com.pharma.erp.inventory.repository.InventorySpecifications.active;
import static com.pharma.erp.inventory.repository.InventorySpecifications.all;
import static com.pharma.erp.inventory.repository.InventorySpecifications.attributeEquals;
import static com.pharma.erp.inventory.repository.InventorySpecifications.expiringBetween;
import static com.pharma.erp.inventory.repository.InventorySpecifications.hasStockStatus;
import static com.pharma.erp.inventory.repository.InventorySpecifications.lowStock;
import static com.pharma.erp.inventory.repository.InventorySpecifications.textMatches;

import com.pharma.erp.common.web.ConflictException;
import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.inventory.dto.InventoryItemCreateRequest;
import com.pharma.erp.inventory.dto.InventoryItemResponse;
import com.pharma.erp.inventory.dto.InventoryItemUpdateRequest;
import com.pharma.erp.inventory.dto.ItemDetails;
import com.pharma.erp.inventory.dto.ItemStatusResponse;
import com.pharma.erp.inventory.dto.StockAdjustmentRequest;
import com.pharma.erp.inventory.dto.StockAdjustmentResponse;
import com.pharma.erp.inventory.dto.StockMovementResponse;
import com.pharma.erp.inventory.entity.InventoryItem;
import com.pharma.erp.inventory.entity.ItemStatus;
import com.pharma.erp.inventory.entity.MovementReason;
import com.pharma.erp.inventory.entity.StockMovement;
import com.pharma.erp.inventory.entity.StockStatus;
import com.pharma.erp.inventory.repository.InventoryItemRepository;
import com.pharma.erp.inventory.repository.ItemStatusRepository;
import com.pharma.erp.inventory.repository.StockMovementRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inventory batches, stock movements (tracking) and derived stock status. */
@Service
public class InventoryService {

    private final InventoryItemRepository items;
    private final StockMovementRepository movements;
    private final ItemStatusRepository statuses;
    private final ReferenceChecker references;
    private final EntityManager entityManager;
    private final Clock clock;
    private final int defaultReorderLevel;

    @Autowired
    public InventoryService(InventoryItemRepository items, StockMovementRepository movements,
            ItemStatusRepository statuses, ReferenceChecker references, EntityManager entityManager,
            ObjectProvider<Clock> clock, @Value("${pharma.inventory.default-reorder-level:10}") int defaultReorderLevel) {
        this(items, movements, statuses, references, entityManager, clock.getIfAvailable(Clock::systemUTC),
                defaultReorderLevel);
    }

    InventoryService(InventoryItemRepository items, StockMovementRepository movements, ItemStatusRepository statuses,
            ReferenceChecker references, EntityManager entityManager, Clock clock, int defaultReorderLevel) {
        this.items = items;
        this.movements = movements;
        this.statuses = statuses;
        this.references = references;
        this.entityManager = entityManager;
        this.clock = clock;
        this.defaultReorderLevel = defaultReorderLevel;
    }

    public record SearchCriteria(String q, Integer storeId, Integer manufacturerId, String barcode, String batchNo,
            StockStatus stockStatus) {
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> search(SearchCriteria criteria, Pageable pageable) {
        LocalDate today = today();
        Specification<InventoryItem> spec = all();
        if (hasText(criteria.q())) {
            spec = spec.and(textMatches(criteria.q()));
        }
        if (criteria.storeId() != null) {
            spec = spec.and(attributeEquals("storeId", criteria.storeId()));
        }
        if (criteria.manufacturerId() != null) {
            spec = spec.and(attributeEquals("manufacturerId", criteria.manufacturerId()));
        }
        if (hasText(criteria.barcode())) {
            spec = spec.and(attributeEquals("barcode", criteria.barcode().trim()));
        }
        if (hasText(criteria.batchNo())) {
            spec = spec.and(attributeEquals("batchNo", criteria.batchNo().trim()));
        }
        if (criteria.stockStatus() != null) {
            spec = spec.and(hasStockStatus(criteria.stockStatus(), defaultReorderLevel, today));
        }
        return page(spec, pageable, today);
    }

    /** Batches with stock left that expire within {@code days} days (inclusive), soonest first (FEFO). */
    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> expiring(int days, Pageable pageable) {
        LocalDate today = today();
        return page(expiringBetween(today, today.plusDays(days)), withDefaultSort(pageable, "expiryDate"), today);
    }

    /** Usable batches at or below {@code threshold} (or their own reorder level when null), emptiest first. */
    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> lowStockItems(Integer threshold, Pageable pageable) {
        LocalDate today = today();
        return page(lowStock(threshold, defaultReorderLevel, today), withDefaultSort(pageable, "quantity"), today);
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse get(Integer id) {
        return toResponse(find(id), statusNames(), today());
    }

    @Transactional
    public InventoryItemResponse create(InventoryItemCreateRequest request, String username) {
        InventoryItem item = new InventoryItem();
        apply(item, request, null);
        item.setQuantity(request.quantity());
        items.saveAndFlush(item);
        if (request.quantity() > 0) {
            record(item, request.quantity(), MovementReason.RECEIPT, "Opening stock",
                    locationFor(item, request.location()), username);
        }
        entityManager.refresh(item);
        return toResponse(item, statusNames(), today());
    }

    @Transactional
    public InventoryItemResponse update(Integer id, InventoryItemUpdateRequest request, String username) {
        InventoryItem item = lockActive(id);
        Integer previousStatus = item.getStatusId();
        if (!Objects.equals(item.getStoreId(), request.storeId()) && item.getQuantity() != 0) {
            throw new ConflictException("Cannot move " + describe(item) + " with " + item.getQuantity()
                    + " units on hand to another store; record TRANSFER_OUT/TRANSFER_IN adjustments instead");
        }
        apply(item, request, id);
        items.saveAndFlush(item);
        if (!Objects.equals(previousStatus, item.getStatusId())) {
            Map<Integer, String> names = statusNames();
            record(item, 0, MovementReason.STATUS_CHANGE,
                    "Status " + names.getOrDefault(previousStatus, "none") + " -> "
                            + names.getOrDefault(item.getStatusId(), "none"),
                    locationFor(item, null), username);
        }
        entityManager.refresh(item);
        return toResponse(item, statusNames(), today());
    }

    @Transactional
    public void delete(Integer id, String username) {
        InventoryItem item = lockActive(id);
        if (item.getQuantity() != 0) {
            throw new ConflictException("Cannot delete " + describe(item) + " with " + item.getQuantity()
                    + " units on hand; adjust the stock out first");
        }
        item.setArchivedAt(Instant.now(clock));
        items.saveAndFlush(item);
        record(item, 0, MovementReason.STATUS_CHANGE, "Archived", locationFor(item, null), username);
    }

    @Transactional
    public StockAdjustmentResponse adjust(Integer id, StockAdjustmentRequest request, String username) {
        int change = request.quantityChange();
        MovementReason reason = request.reason();
        if (change == 0) {
            throw new IllegalArgumentException("quantityChange must not be zero");
        }
        if (!reason.accepts(change)) {
            throw new IllegalArgumentException(switch (reason.direction()) {
                case IN -> "Reason " + reason + " requires a positive quantityChange";
                case OUT -> "Reason " + reason + " requires a negative quantityChange";
                default -> "Reason " + reason + " cannot be used for a stock adjustment";
            });
        }
        InventoryItem item = lockActive(id);
        LocalDate today = today();
        if (change > 0 && item.getExpiryDate() != null && item.getExpiryDate().isBefore(today)) {
            throw new ConflictException("Cannot add stock to expired batch " + describe(item));
        }
        long after = (long) item.getQuantity() + change;
        if (after < 0) {
            throw new ConflictException("Insufficient stock for " + describe(item) + ": on hand "
                    + item.getQuantity() + ", requested " + (-change));
        }
        if (after > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("quantityChange would overflow the stock quantity");
        }
        item.setQuantity((int) after);
        items.saveAndFlush(item);
        StockMovement movement = record(item, change, reason, trimToNull(request.note()),
                locationFor(item, request.location()), username);
        entityManager.refresh(item);
        Map<Integer, String> names = statusNames();
        return new StockAdjustmentResponse(toResponse(item, names, today), toResponse(movement, names));
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> movements(Integer itemId, Pageable pageable) {
        if (!items.existsByIdAndArchivedAtIsNull(itemId)) {
            throw NotFoundException.of("Inventory item", itemId);
        }
        Map<Integer, String> names = statusNames();
        Pageable unsorted = Pageable.ofSize(pageable.getPageSize()).withPage(pageable.getPageNumber());
        return PageResponse.from(movements.findByItemIdOrderByTrackedAtDescIdDesc(itemId, unsorted),
                m -> toResponse(m, names));
    }

    @Transactional(readOnly = true)
    public List<ItemStatusResponse> statuses() {
        return statuses.findAll(Sort.by("id")).stream().map(s -> new ItemStatusResponse(s.getId(), s.getName()))
                .toList();
    }

    private PageResponse<InventoryItemResponse> page(Specification<InventoryItem> spec, Pageable pageable,
            LocalDate today) {
        Map<Integer, String> names = statusNames();
        return PageResponse.from(items.findAll(active().and(spec), pageable), item -> toResponse(item, names, today));
    }

    private void apply(InventoryItem item, ItemDetails details, Integer existingId) {
        references.requireExists("manufacturerId", details.manufacturerId());
        references.requireExists("storeId", details.storeId());
        references.requireExists("employeeId", details.employeeId());
        references.requireExists("clientId", details.clientId());
        String barcode = trimToNull(details.barcode());
        String batchNo = trimToNull(details.batchNo());
        if (barcode != null && batchNo != null && items.existsBatch(barcode, batchNo, details.storeId(), existingId)) {
            throw new ConflictException("Batch " + batchNo + " of barcode " + barcode + " already exists in "
                    + (details.storeId() == null ? "the unassigned stock" : "store " + details.storeId()));
        }
        item.setName(details.name().trim());
        item.setDescription(trimToNull(details.description()));
        item.setBatchNo(batchNo);
        item.setExpiryDate(details.expiryDate());
        item.setReorderLevel(details.reorderLevel());
        item.setUnitPrice(details.unitPrice());
        item.setGstRate(details.gstRate());
        item.setHsnCode(trimToNull(details.hsnCode()));
        item.setBarcode(barcode);
        item.setManufacturerId(details.manufacturerId());
        item.setStoreId(details.storeId());
        item.setStatusId(resolveStatus(details.status()));
        item.setEmployeeId(details.employeeId());
        item.setClientId(details.clientId());
        item.setAssignDate(details.assignDate());
        item.setDeliveryDate(details.deliveryDate());
        item.setTotalAmount(details.totalAmount());
        item.setAdvanceAmount(details.advanceAmount());
    }

    private Integer resolveStatus(String name) {
        if (!hasText(name)) {
            return null;
        }
        return statuses.findByNameIgnoreCase(name.trim())
                .map(ItemStatus::getId)
                .orElseThrow(() -> new InvalidReferenceException("status", "Unknown status '" + name.trim()
                        + "'; expected one of " + statuses().stream().map(ItemStatusResponse::name).toList()));
    }

    private StockMovement record(InventoryItem item, int change, MovementReason reason, String note, String location,
            String username) {
        return movements.save(new StockMovement(item.getId(), item.getStatusId(), location, change, reason,
                item.getQuantity(), note, username, Instant.now(clock)));
    }

    private InventoryItem lockActive(Integer id) {
        return items.findActiveByIdForUpdate(id).orElseThrow(() -> NotFoundException.of("Inventory item", id));
    }

    private InventoryItem find(Integer id) {
        return items.findByIdAndArchivedAtIsNull(id).orElseThrow(() -> NotFoundException.of("Inventory item", id));
    }

    private Map<Integer, String> statusNames() {
        return statuses.findAll().stream().collect(Collectors.toMap(ItemStatus::getId, ItemStatus::getName));
    }

    InventoryItemResponse toResponse(InventoryItem item, Map<Integer, String> names, LocalDate today) {
        int reorderLevel = item.getReorderLevel() != null ? item.getReorderLevel() : defaultReorderLevel;
        BigDecimal stockValue = item.getUnitPrice() == null
                ? null
                : item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        Long daysToExpiry = item.getExpiryDate() == null ? null : ChronoUnit.DAYS.between(today, item.getExpiryDate());
        return new InventoryItemResponse(item.getId(), item.getName(), item.getDescription(), item.getBatchNo(),
                item.getExpiryDate(), daysToExpiry, item.getQuantity(), reorderLevel, item.getUnitPrice(), stockValue,
                item.getGstRate(), item.getHsnCode(), item.getBarcode(), item.getManufacturerId(), item.getStoreId(),
                names.get(item.getStatusId()),
                StockStatus.derive(item.getQuantity(), item.getExpiryDate(), reorderLevel, today),
                item.getEmployeeId(), item.getClientId(), item.getAssignDate(), item.getDeliveryDate(),
                item.getTotalAmount(), item.getAdvanceAmount(), item.getCreatedAt(), item.getUpdatedAt());
    }

    private static StockMovementResponse toResponse(StockMovement m, Map<Integer, String> names) {
        return new StockMovementResponse(m.getId(), m.getItemId(), names.get(m.getStatusId()), m.getCurrentLocation(),
                m.getQuantityChange(), m.getQuantityAfter(), m.getReason(), m.getNote(), m.getPerformedBy(),
                m.getTrackedAt());
    }

    private static Pageable withDefaultSort(Pageable pageable, String property) {
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(property).ascending().and(Sort.by("id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private static String locationFor(InventoryItem item, String requested) {
        String location = trimToNull(requested);
        if (location != null) {
            return location;
        }
        return item.getStoreId() == null ? null : "Store " + item.getStoreId();
    }

    private static String describe(InventoryItem item) {
        return "item " + item.getId() + (item.getBatchNo() == null ? "" : " (batch " + item.getBatchNo() + ")");
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
