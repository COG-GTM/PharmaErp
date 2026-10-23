package com.pharma.erp.manufacturers;

import com.pharma.erp.common.web.ConflictException;
import com.pharma.erp.common.web.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManufacturerService {

    private static final String STOCK_SQL = """
            SELECT i.item_id, i.item_name, i.batch_no, i.expiry_date, i.quantity, i.unit_price, i.store_id,
                   s.status_name
            FROM inventory_item i
            LEFT JOIN item_status s ON s.status_id = i.status_id
            WHERE i.manufacturer_id = ?
            ORDER BY i.expiry_date IS NULL, i.expiry_date, i.item_id
            """;

    private final ManufacturerRepository repository;
    private final JdbcTemplate jdbc;

    public ManufacturerService(ManufacturerRepository repository, JdbcTemplate jdbc) {
        this.repository = repository;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Page<ManufacturerResponse> search(ManufacturerType type, String query, Pageable pageable) {
        String q = blankToNull(query);
        String pattern = q == null ? null : "%" + q.toLowerCase(Locale.ROOT) + "%";
        return repository.search(type, pattern, pageable).map(ManufacturerResponse::from);
    }

    @Transactional(readOnly = true)
    public ManufacturerResponse get(int id) {
        return ManufacturerResponse.from(find(id));
    }

    @Transactional
    public ManufacturerResponse create(ManufacturerRequest request) {
        Manufacturer manufacturer = new Manufacturer();
        apply(manufacturer, request);
        if (repository.existsByNameIgnoreCase(manufacturer.getName())) {
            throw duplicateName(manufacturer.getName());
        }
        if (manufacturer.getLicenceNumber() != null
                && repository.existsByLicenceNumberIgnoreCase(manufacturer.getLicenceNumber())) {
            throw duplicateLicence(manufacturer.getLicenceNumber());
        }
        return ManufacturerResponse.from(repository.saveAndFlush(manufacturer));
    }

    @Transactional
    public ManufacturerResponse update(int id, ManufacturerRequest request) {
        Manufacturer manufacturer = find(id);
        apply(manufacturer, request);
        if (repository.existsByNameIgnoreCaseAndIdNot(manufacturer.getName(), id)) {
            throw duplicateName(manufacturer.getName());
        }
        if (manufacturer.getLicenceNumber() != null
                && repository.existsByLicenceNumberIgnoreCaseAndIdNot(manufacturer.getLicenceNumber(), id)) {
            throw duplicateLicence(manufacturer.getLicenceNumber());
        }
        return ManufacturerResponse.from(repository.saveAndFlush(manufacturer));
    }

    @Transactional
    public void delete(int id) {
        Manufacturer manufacturer = find(id);
        Integer items = jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_item WHERE manufacturer_id = ?", Integer.class, id);
        if (items != null && items > 0) {
            throw new ConflictException(
                    "Manufacturer " + id + " is referenced by " + items + " inventory item(s) and cannot be deleted");
        }
        repository.delete(manufacturer);
    }

    @Transactional(readOnly = true)
    public ManufacturerStockSummary stock(int id) {
        Manufacturer manufacturer = find(id);
        List<ManufacturerStockItem> items = jdbc.query(STOCK_SQL, (rs, n) -> new ManufacturerStockItem(
                rs.getInt("item_id"),
                rs.getString("item_name"),
                rs.getString("batch_no"),
                rs.getObject("expiry_date", java.time.LocalDate.class),
                rs.getInt("quantity"),
                rs.getBigDecimal("unit_price"),
                (Integer) rs.getObject("store_id", Integer.class),
                rs.getString("status_name")), id);
        long totalQuantity = items.stream().mapToLong(ManufacturerStockItem::quantity).sum();
        BigDecimal totalValue = items.stream()
                .filter(i -> i.unitPrice() != null)
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ManufacturerStockSummary(
                manufacturer.getId(), manufacturer.getName(), manufacturer.getType(), items.size(), totalQuantity,
                totalValue, items);
    }

    private Manufacturer find(int id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("Manufacturer", id));
    }

    static void apply(Manufacturer m, ManufacturerRequest r) {
        m.setName(r.name().trim());
        m.setType(r.type());
        m.setLicenceNumber(upper(r.licenceNumber()));
        m.setContactPerson(blankToNull(r.contactPerson()));
        m.setAddress(blankToNull(r.address()));
        m.setEmail(blankToNull(r.email()));
        m.setPhone(blankToNull(r.phone()));
        m.setCity(blankToNull(r.city()));
        m.setDistrict(blankToNull(r.district()));
        m.setState(blankToNull(r.state()));
        m.setGstin(upper(r.gstin()));
    }

    private static String upper(String value) {
        String v = blankToNull(value);
        return v == null ? null : v.toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ConflictException duplicateName(String name) {
        return new ConflictException("A manufacturer named '" + name + "' already exists");
    }

    private static ConflictException duplicateLicence(String licence) {
        return new ConflictException("Licence number '" + licence + "' is already registered");
    }
}
