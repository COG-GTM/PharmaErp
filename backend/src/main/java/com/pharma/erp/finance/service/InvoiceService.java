package com.pharma.erp.finance.service;

import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.finance.dto.CreateInvoiceRequest;
import com.pharma.erp.finance.dto.InvoiceLineRequest;
import com.pharma.erp.finance.dto.InvoiceResponse;
import com.pharma.erp.finance.dto.InvoiceSummaryResponse;
import com.pharma.erp.finance.dto.ReceiptResponse;
import com.pharma.erp.finance.entity.Invoice;
import com.pharma.erp.finance.entity.InvoiceLine;
import com.pharma.erp.finance.repository.FinanceReferenceRepository;
import com.pharma.erp.finance.repository.InvoiceRepository;
import com.pharma.erp.finance.service.GstCalculator.GstSplit;
import com.pharma.erp.finance.service.GstCalculator.InvoiceTotals;
import com.pharma.erp.finance.service.GstCalculator.LineAmounts;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    static final String CURRENCY = "INR";

    private final InvoiceRepository invoices;
    private final FinanceReferenceRepository references;
    private final Clock clock;

    @Autowired
    public InvoiceService(InvoiceRepository invoices, FinanceReferenceRepository references) {
        this(invoices, references, Clock.systemUTC());
    }

    InvoiceService(InvoiceRepository invoices, FinanceReferenceRepository references, Clock clock) {
        this.invoices = invoices;
        this.references = references;
        this.clock = clock;
    }

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        checkReference(errors, "clientId", "Client", request.clientId(), references::clientExists);
        checkReference(errors, "storeId", "Store", request.storeId(), references::storeExists);
        checkReference(errors, "prescriptionId", "Prescription", request.prescriptionId(),
                references::prescriptionExists);

        Invoice invoice = new Invoice();
        List<LineAmounts> amounts = new ArrayList<>();
        for (int i = 0; i < request.lines().size(); i++) {
            InvoiceLineRequest lineRequest = request.lines().get(i);
            String field = "lines[" + i + "]";
            String description = trimToNull(lineRequest.description());
            if (lineRequest.itemId() != null) {
                Optional<String> itemName = references.itemName(lineRequest.itemId());
                if (itemName.isEmpty()) {
                    addError(errors, field + ".itemId", "Inventory item " + lineRequest.itemId() + " does not exist");
                } else if (description == null) {
                    description = truncate(itemName.get(), 500);
                }
            } else if (description == null) {
                addError(errors, field + ".description", "must not be blank when itemId is not given");
            }
            LineAmounts line = GstCalculator.line(lineRequest.quantity(), lineRequest.unitPrice(), lineRequest.gstRate());
            amounts.add(line);
            if (GstCalculator.exceedsStorage(line.lineTotal())) {
                addError(errors, field, "line total " + line.lineTotal().toPlainString() + " exceeds the maximum of "
                        + GstCalculator.MAX_AMOUNT.toPlainString());
            }

            InvoiceLine entity = new InvoiceLine();
            entity.setItemId(lineRequest.itemId());
            entity.setDescription(description);
            entity.setQuantity(line.quantity());
            entity.setUnitPrice(line.unitPrice());
            entity.setGstRate(line.gst().gstRate());
            entity.setGstAmount(line.gst().gstAmount());
            entity.setLineTotal(line.lineTotal());
            invoice.addLine(entity);
        }
        InvoiceTotals totals = GstCalculator.totals(amounts);
        if (GstCalculator.exceedsStorage(totals.total())) {
            addError(errors, "lines", "invoice total " + totals.total().toPlainString() + " exceeds the maximum of "
                    + GstCalculator.MAX_AMOUNT.toPlainString());
        }
        if (!errors.isEmpty()) {
            throw new InvalidReferenceException(errors);
        }

        invoice.setInvoiceNumber(UUID.randomUUID().toString().replace("-", ""));
        invoice.setClientId(request.clientId());
        invoice.setStoreId(request.storeId());
        invoice.setPrescriptionId(request.prescriptionId());
        invoice.setDescription(trimToNull(request.description()));
        invoice.setStatus(Invoice.STATUS_ISSUED);
        invoice.setInvoiceDate(request.invoiceDate() != null ? request.invoiceDate() : LocalDate.now(clock));
        invoice.setSubtotal(totals.subtotal());
        invoice.setGstAmount(totals.gstAmount());
        invoice.setTotalAmount(totals.total());
        invoice.setPaidAmount(GstCalculator.zero());
        Invoice saved = invoices.saveAndFlush(invoice);
        saved.setInvoiceNumber(invoiceNumber(saved.getId()));
        return FinanceMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceSummaryResponse> list(
            Integer clientId, Integer storeId, String status, LocalDate from, LocalDate to, Pageable pageable) {
        Specification<Invoice> spec = Specification.<Invoice>where(null)
                .and(equal("clientId", clientId))
                .and(equal("storeId", storeId))
                .and(equal("status", status))
                .and(from == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("invoiceDate"), from))
                .and(to == null ? null : (root, q, cb) -> cb.lessThanOrEqualTo(root.get("invoiceDate"), to));
        return invoices.findAll(spec, pageable).map(FinanceMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(int id) {
        return FinanceMapper.toResponse(load(id));
    }

    @Transactional(readOnly = true)
    public ReceiptResponse receipt(int id) {
        Invoice invoice = load(id);
        InvoiceResponse response = FinanceMapper.toResponse(invoice);
        List<GstSplit> splits = invoice.getLines().stream().map(FinanceMapper::lineSplit).toList();
        return new ReceiptResponse(
                "ER-" + invoice.getInvoiceNumber(),
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getStatus(),
                invoice.getClientId(),
                invoice.getStoreId(),
                CURRENCY,
                response.lines(),
                GstCalculator.breakdownByRate(splits).stream().map(FinanceMapper::toBreakdown).toList(),
                response.subtotal(),
                response.cgstAmount(),
                response.sgstAmount(),
                response.gstAmount(),
                response.totalAmount(),
                response.paidAmount(),
                response.balanceDue(),
                AmountInWords.rupees(response.totalAmount()),
                Instant.now(clock));
    }

    static String invoiceNumber(int id) {
        return String.format("INV-%06d", id);
    }

    private Invoice load(int id) {
        return invoices.findWithLinesById(id).orElseThrow(() -> NotFoundException.of("Invoice", id));
    }

    private static void checkReference(
            Map<String, List<String>> errors, String field, String label, Integer id,
            java.util.function.IntPredicate exists) {
        if (id != null && !exists.test(id)) {
            addError(errors, field, label + " " + id + " does not exist");
        }
    }

    static void addError(Map<String, List<String>> errors, String field, String message) {
        errors.computeIfAbsent(field, k -> new ArrayList<>()).add(message);
    }

    static <T> Specification<T> equal(String attribute, Object value) {
        return value == null ? null : (root, q, cb) -> cb.equal(root.get(attribute), value);
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
