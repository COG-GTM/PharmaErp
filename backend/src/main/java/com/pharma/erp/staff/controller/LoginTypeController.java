package com.pharma.erp.staff.controller;

import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.staff.dto.LoginTypeRequest;
import com.pharma.erp.staff.dto.LoginTypeResponse;
import com.pharma.erp.staff.service.LoginTypeService;
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

@RestController
@RequestMapping("/api/login-types")
public class LoginTypeController {

    private final LoginTypeService service;

    public LoginTypeController(LoginTypeService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<LoginTypeResponse> list(Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public LoginTypeResponse get(@PathVariable Integer id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<LoginTypeResponse> create(@Valid @RequestBody LoginTypeRequest request) {
        LoginTypeResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/login-types/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public LoginTypeResponse update(@PathVariable Integer id, @Valid @RequestBody LoginTypeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
