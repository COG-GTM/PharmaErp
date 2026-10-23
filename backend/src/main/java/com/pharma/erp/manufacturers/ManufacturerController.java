package com.pharma.erp.manufacturers;

import com.pharma.erp.common.web.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Manufacturers, suppliers and distributors (spec sheet "Manufacturer"). */
@RestController
@RequestMapping("/api/manufacturers")
public class ManufacturerController {

    private final ManufacturerService service;

    public ManufacturerController(ManufacturerService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ManufacturerResponse> list(
            @RequestParam(required = false) ManufacturerType type,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(service.search(type, q, pageable));
    }

    @GetMapping("/{id}")
    public ManufacturerResponse get(@PathVariable int id) {
        return service.get(id);
    }

    @GetMapping("/{id}/stock")
    public ManufacturerStockSummary stock(@PathVariable int id) {
        return service.stock(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<ManufacturerResponse> create(@Valid @RequestBody ManufacturerRequest request) {
        ManufacturerResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/manufacturers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ManufacturerResponse update(@PathVariable int id, @Valid @RequestBody ManufacturerRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
