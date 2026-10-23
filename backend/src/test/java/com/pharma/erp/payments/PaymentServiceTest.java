package com.pharma.erp.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pharma.erp.common.web.ConflictException;
import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.payments.dto.PaymentRequest;
import com.pharma.erp.payments.dto.PaymentResponse;
import com.pharma.erp.payments.entity.Payment;
import com.pharma.erp.payments.entity.PaymentMethod;
import com.pharma.erp.payments.repository.PaymentRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:15:30.789Z");

    PaymentRepository payments;
    PaymentMethodService methods;
    InvoiceLookup invoices;
    PaymentService service;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        methods = mock(PaymentMethodService.class);
        invoices = mock(InvoiceLookup.class);
        service = new PaymentService(payments, methods, invoices, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void receiptNumbersAreZeroPaddedAndFitTheColumn() {
        assertThat(PaymentService.receiptNumber(42)).isEqualTo("RCPT-00000042");
        assertThat(PaymentService.receiptNumber(Integer.MAX_VALUE)).hasSizeLessThanOrEqualTo(32);
    }

    @Test
    void nonPositiveAmountsAreRejectedBeforeAnyLookup() {
        assertThatThrownBy(() -> service.record(new PaymentRequest(1, 1, BigDecimal.ZERO, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.record(new PaymentRequest(1, 1, new BigDecimal("-5.00"), null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(invoices, methods, payments);
    }

    @Test
    void missingInvoiceIsNotFound() {
        when(invoices.find(7)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.record(new PaymentRequest(7, 1, BigDecimal.TEN, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Invoice 7");
        verifyNoInteractions(payments);
    }

    @Test
    void inactiveMethodIsConflict() {
        when(invoices.find(7)).thenReturn(Optional.of(new InvoiceLookup.InvoiceRef(7, 3)));
        when(methods.find(2)).thenReturn(new PaymentMethod("Cheque", false));
        assertThatThrownBy(() -> service.record(new PaymentRequest(7, 2, BigDecimal.TEN, null)))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(payments);
    }

    @Test
    void recordCopiesInvoiceClientMethodNameAndTruncatesTimestamp() {
        when(invoices.find(7)).thenReturn(Optional.of(new InvoiceLookup.InvoiceRef(7, 3)));
        when(methods.find(2)).thenReturn(new PaymentMethod("UPI", true));
        when(payments.saveAndFlush(any(Payment.class))).thenAnswer(inv -> withId(inv.getArgument(0), 99));

        PaymentResponse response = service.record(new PaymentRequest(7, 2, new BigDecimal("12.50"), " ref "));

        assertThat(response.id()).isEqualTo(99);
        assertThat(response.invoiceId()).isEqualTo(7);
        assertThat(response.clientId()).isEqualTo(3);
        assertThat(response.paymentType()).isEqualTo("UPI");
        assertThat(response.amount()).isEqualByComparingTo("12.50");
        assertThat(response.reference()).isEqualTo("ref");
        assertThat(response.receiptNumber()).isEqualTo("RCPT-00000099");
        assertThat(response.paidAt()).isEqualTo(Instant.parse("2026-10-23T10:15:30Z"));
    }

    private static Payment withId(Payment payment, int id) throws ReflectiveOperationException {
        Field field = Payment.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(payment, id);
        return payment;
    }
}
