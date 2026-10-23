package com.pharma.erp.staff.controller;

import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.staff.dto.StaffRequest;
import com.pharma.erp.staff.dto.StaffResponse;
import com.pharma.erp.staff.entity.StaffType;
import com.pharma.erp.staff.service.StaffService;
import com.pharma.erp.staff.service.StaffService.StaffFilter;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<StaffResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer loginTypeId,
            @RequestParam(required = false) Integer storeId,
            @RequestParam(required = false) StaffType staffType,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return service.list(new StaffFilter(q, loginTypeId, storeId, staffType, active), pageable);
    }

    @GetMapping("/{id}")
    public StaffResponse get(@PathVariable Integer id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody StaffRequest request) {
        StaffResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/staff/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public StaffResponse update(@PathVariable Integer id, @Valid @RequestBody StaffRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
