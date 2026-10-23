package com.pharma.erp.finance.controller;

import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.finance.dto.CreateInvoiceRequest;
import com.pharma.erp.finance.dto.InvoiceResponse;
import com.pharma.erp.finance.dto.InvoiceSummaryResponse;
import com.pharma.erp.finance.dto.ReceiptResponse;
import com.pharma.erp.finance.service.InvoiceService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/invoices/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<InvoiceSummaryResponse> list(
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer storeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = {"invoiceDate", "id"}, direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return PageResponse.from(service.list(clientId, storeId, status, from, to, FinanceSorts.check(pageable,
                FinanceSorts.INVOICE_PROPERTIES)));
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable int id) {
        return service.get(id);
    }

    @GetMapping("/{id}/receipt")
    public ReceiptResponse receipt(@PathVariable int id) {
        return service.receipt(id);
    }
}
