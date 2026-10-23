package com.pharma.erp.stores;

import com.pharma.erp.common.web.ConflictException;
import com.pharma.erp.common.web.NotFoundException;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StoreService {

    static final Set<String> SORTABLE =
            Set.of("id", "code", "name", "address", "gstin", "managerStaffId", "createdAt", "updatedAt");

    private final StoreRepository stores;
    private final StaffDirectory staff;

    public StoreService(StoreRepository stores, StaffDirectory staff) {
        this.stores = stores;
        this.staff = staff;
    }

    @Transactional(readOnly = true)
    public Page<StoreResponse> list(String query, Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new InvalidReferenceException("sort", "unknown property '" + order.getProperty()
                        + "'; sortable: " + String.join(", ", SORTABLE.stream().sorted().toList()));
            }
        });
        Page<Store> page = StringUtils.hasText(query)
                ? stores.search(StoreRepository.likeLiteral(query.trim()), pageable)
                : stores.findAll(pageable);
        return page.map(StoreResponse::from);
    }

    @Transactional(readOnly = true)
    public StoreResponse get(int id) {
        return StoreResponse.from(find(id));
    }

    @Transactional
    public StoreResponse create(StoreRequest request) {
        Store store = new Store();
        apply(store, request);
        if (stores.existsByCode(store.getCode())) {
            throw duplicate("code", store.getCode());
        }
        if (stores.existsByName(store.getName())) {
            throw duplicate("name", store.getName());
        }
        return StoreResponse.from(stores.saveAndFlush(store));
    }

    @Transactional
    public StoreResponse update(int id, StoreRequest request) {
        Store store = find(id);
        apply(store, request);
        if (stores.existsByCodeAndIdNot(store.getCode(), id)) {
            throw duplicate("code", store.getCode());
        }
        if (stores.existsByNameAndIdNot(store.getName(), id)) {
            throw duplicate("name", store.getName());
        }
        return StoreResponse.from(stores.saveAndFlush(store));
    }

    @Transactional
    public void delete(int id) {
        Store store = find(id);
        try {
            stores.delete(store);
            stores.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(
                    "Store " + id + " is still referenced by inventory items or invoices and cannot be deleted");
        }
    }

    private Store find(int id) {
        return stores.findById(id).orElseThrow(() -> NotFoundException.of("Store", id));
    }

    private void apply(Store store, StoreRequest request) {
        if (request.managerStaffId() != null && !staff.exists(request.managerStaffId())) {
            throw new InvalidReferenceException(
                    "managerStaffId", "staff member " + request.managerStaffId() + " does not exist");
        }
        store.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        store.setName(request.name().trim());
        store.setAddress(blankToNull(request.address()));
        String gstin = blankToNull(request.gstin());
        store.setGstin(gstin == null ? null : gstin.toUpperCase(Locale.ROOT));
        store.setManagerStaffId(request.managerStaffId());
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static ConflictException duplicate(String field, String value) {
        return new ConflictException("A store with " + field + " '" + value + "' already exists");
    }
}
