package com.pharma.erp.staff.controller;

import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.staff.dto.SalaryRequest;
import com.pharma.erp.staff.dto.SalaryResponse;
import com.pharma.erp.staff.service.SalaryService;
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
import org.springframework.web.bind.annotation.RestController;

/** Payroll history; reads and writes are admin-only. */
@RestController
@RequestMapping("/api/staff/{staffId}/salaries")
public class SalaryController {

    private final SalaryService service;

    public SalaryController(SalaryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public PageResponse<SalaryResponse> list(@PathVariable Integer staffId, Pageable pageable) {
        return service.list(staffId, pageable);
    }

    @GetMapping("/{salaryId}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public SalaryResponse get(@PathVariable Integer staffId, @PathVariable Integer salaryId) {
        return service.get(staffId, salaryId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<SalaryResponse> create(
            @PathVariable Integer staffId, @Valid @RequestBody SalaryRequest request) {
        SalaryResponse created = service.create(staffId, request);
        return ResponseEntity.created(URI.create("/api/staff/" + staffId + "/salaries/" + created.id()))
                .body(created);
    }

    @PutMapping("/{salaryId}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public SalaryResponse update(
            @PathVariable Integer staffId, @PathVariable Integer salaryId, @Valid @RequestBody SalaryRequest request) {
        return service.update(staffId, salaryId, request);
    }

    @DeleteMapping("/{salaryId}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<Void> delete(@PathVariable Integer staffId, @PathVariable Integer salaryId) {
        service.delete(staffId, salaryId);
        return ResponseEntity.noContent().build();
    }
}
