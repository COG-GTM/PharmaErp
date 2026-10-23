package com.pharma.erp.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Intra-state GST arithmetic (spec use case "GST Calculation (State, Central)"). The GST rate is split
 * equally into Central GST and State GST; each component is computed on the taxable value of the line
 * and rounded to 2 decimals with {@link RoundingMode#HALF_UP}, so {@code gst = cgst + sgst} always holds.
 */
public final class GstCalculator {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final List<BigDecimal> ALLOWED_RATES = List.of(
            new BigDecimal("0"), new BigDecimal("5"), new BigDecimal("12"), new BigDecimal("18"), new BigDecimal("28"));

    /** Largest value a {@code DECIMAL(12,2)} money column can hold. */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private GstCalculator() {}

    public static boolean isAllowedRate(BigDecimal rate) {
        return rate != null && ALLOWED_RATES.stream().anyMatch(allowed -> allowed.compareTo(rate) == 0);
    }

    /** Normalised rate (scale 2) or {@link IllegalArgumentException} when not one of 0/5/12/18/28. */
    public static BigDecimal requireAllowedRate(BigDecimal rate) {
        if (!isAllowedRate(rate)) {
            throw new IllegalArgumentException("GST rate must be one of 0, 5, 12, 18, 28 but was " + rate);
        }
        return rate.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    /** Splits the GST on a taxable value into equal CGST and SGST halves. */
    public static GstSplit split(BigDecimal taxableAmount, BigDecimal gstRate) {
        if (taxableAmount == null || taxableAmount.signum() < 0) {
            throw new IllegalArgumentException("Taxable amount must be zero or positive");
        }
        return derive(taxableAmount, requireAllowedRate(gstRate));
    }

    /**
     * Same arithmetic as {@link #split} without the slab check; used to re-derive CGST/SGST for rows that
     * were stored by other writers.
     */
    public static GstSplit derive(BigDecimal taxableAmount, BigDecimal gstRate) {
        BigDecimal rate = money(gstRate);
        BigDecimal taxable = money(taxableAmount);
        BigDecimal halfRate = rate.divide(TWO, SCALE, ROUNDING);
        BigDecimal component = taxable.multiply(halfRate).divide(HUNDRED, SCALE, ROUNDING);
        return new GstSplit(taxable, rate, halfRate, component, halfRate, component, component.add(component));
    }

    /** Amounts for one invoice line: {@code taxable = quantity * unitPrice}, GST split on top. */
    public static LineAmounts line(int quantity, BigDecimal unitPrice, BigDecimal gstRate) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price must be zero or positive");
        }
        BigDecimal price = money(unitPrice);
        GstSplit split = split(price.multiply(BigDecimal.valueOf(quantity)), gstRate);
        return new LineAmounts(quantity, price, split, split.taxableAmount().add(split.gstAmount()));
    }

    public static InvoiceTotals totals(Collection<LineAmounts> lines) {
        BigDecimal subtotal = zero();
        BigDecimal cgst = zero();
        BigDecimal sgst = zero();
        for (LineAmounts line : lines) {
            subtotal = subtotal.add(line.gst().taxableAmount());
            cgst = cgst.add(line.gst().cgstAmount());
            sgst = sgst.add(line.gst().sgstAmount());
        }
        BigDecimal gst = cgst.add(sgst);
        return new InvoiceTotals(subtotal, cgst, sgst, gst, subtotal.add(gst));
    }

    /** Groups line-level GST by rate (ascending) for tax-invoice / e-receipt summaries. */
    public static List<GstSplit> breakdownByRate(Collection<GstSplit> splits) {
        Map<BigDecimal, GstSplit> byRate = new TreeMap<>();
        for (GstSplit s : splits) {
            byRate.merge(s.gstRate(), s, (a, b) -> new GstSplit(
                    a.taxableAmount().add(b.taxableAmount()), a.gstRate(),
                    a.cgstRate(), a.cgstAmount().add(b.cgstAmount()),
                    a.sgstRate(), a.sgstAmount().add(b.sgstAmount()),
                    a.gstAmount().add(b.gstAmount())));
        }
        return List.copyOf(byRate.values());
    }

    public static boolean exceedsStorage(BigDecimal amount) {
        return amount.compareTo(MAX_AMOUNT) > 0;
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }

    public record GstSplit(
            BigDecimal taxableAmount,
            BigDecimal gstRate,
            BigDecimal cgstRate,
            BigDecimal cgstAmount,
            BigDecimal sgstRate,
            BigDecimal sgstAmount,
            BigDecimal gstAmount) {}

    public record LineAmounts(int quantity, BigDecimal unitPrice, GstSplit gst, BigDecimal lineTotal) {}

    public record InvoiceTotals(
            BigDecimal subtotal, BigDecimal cgstAmount, BigDecimal sgstAmount, BigDecimal gstAmount, BigDecimal total) {}
}
