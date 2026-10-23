package com.pharma.erp.payments.controller;

import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.payments.dto.PaymentRequest;
import com.pharma.erp.payments.dto.PaymentResponse;
import com.pharma.erp.payments.service.PaymentService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> record(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse created = service.record(request);
        return ResponseEntity.created(URI.create("/api/payments/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<PaymentResponse> list(
            @RequestParam(required = false) Integer invoiceId,
            @PageableDefault(size = 20, sort = {"paidAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(invoiceId, pageable);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable Integer id) {
        return service.get(id);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handleUnknownSortProperty(PropertyReferenceException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Unknown sort property: " + ex.getPropertyName());
    }
}
