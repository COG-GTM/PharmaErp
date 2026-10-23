package com.pharma.erp.finance.controller;

import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.finance.dto.ExpenseRequest;
import com.pharma.erp.finance.dto.ExpenseResponse;
import com.pharma.erp.finance.service.ExpenseService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/expenses/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<ExpenseResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = {"expenseDate", "id"}, direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return PageResponse.from(service.list(category, status, storeId, from, to,
                FinanceSorts.check(pageable, FinanceSorts.EXPENSE_PROPERTIES)));
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(@PathVariable int id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable int id, @Valid @RequestBody ExpenseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
