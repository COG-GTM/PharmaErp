package com.pharma.erp.finance.controller;

import com.pharma.erp.finance.dto.FinanceSummaryResponse;
import com.pharma.erp.finance.service.FinanceSummaryService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
public class FinanceSummaryController {

    private final FinanceSummaryService service;

    public FinanceSummaryController(FinanceSummaryService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public FinanceSummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(from, to);
    }
}
