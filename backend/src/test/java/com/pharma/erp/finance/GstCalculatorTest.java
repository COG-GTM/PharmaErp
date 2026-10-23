package com.pharma.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pharma.erp.finance.dto.GstRateValidator;
import com.pharma.erp.finance.service.GstCalculator;
import com.pharma.erp.finance.service.GstCalculator.GstSplit;
import com.pharma.erp.finance.service.GstCalculator.InvoiceTotals;
import com.pharma.erp.finance.service.GstCalculator.LineAmounts;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class GstCalculatorTest {

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }

    @ParameterizedTest(name = "{0}% on 100.00 -> CGST {2} + SGST {2} = {3}")
    @CsvSource({
        "0,  0.00,  0.00,  0.00, 100.00",
        "5,  2.50,  2.50,  5.00, 105.00",
        "12, 6.00,  6.00, 12.00, 112.00",
        "18, 9.00,  9.00, 18.00, 118.00",
        "28, 14.00, 14.00, 28.00, 128.00"
    })
    void eachSlabSplitsEquallyIntoCentralAndState(
            String rate, String halfRate, String component, String gst, String total) {
        LineAmounts line = GstCalculator.line(1, d("100.00"), d(rate));

        assertThat(line.gst().taxableAmount()).isEqualByComparingTo("100.00");
        assertThat(line.gst().cgstRate()).isEqualByComparingTo(halfRate);
        assertThat(line.gst().sgstRate()).isEqualByComparingTo(halfRate);
        assertThat(line.gst().cgstAmount()).isEqualByComparingTo(component);
        assertThat(line.gst().sgstAmount()).isEqualByComparingTo(component);
        assertThat(line.gst().gstAmount()).isEqualByComparingTo(gst);
        assertThat(line.lineTotal()).isEqualByComparingTo(total);
    }

    @ParameterizedTest(name = "qty {0} x {1} @ {2}% -> CGST {3}, total {4}")
    @CsvSource({
        // 97.93 * 2.5% = 2.44825 -> 2.45 (HALF_UP)
        "7,  13.99,   5,  2.45,   102.83",
        // 0.20 * 2.5% = 0.005 -> 0.01 (HALF_UP; HALF_EVEN would give 0.00)
        "1,  0.20,    5,  0.01,   0.22",
        // 0.10 * 2.5% = 0.0025 -> 0.00
        "1,  0.10,    5,  0.00,   0.10",
        // 1199.94 * 14% = 167.9916 -> 167.99
        "6,  199.99,  28, 167.99, 1535.92",
        // 4503.00 * 9% = 405.27
        "4,  1125.75, 18, 405.27, 5313.54",
        // 255.00 * 6% = 15.30
        "10, 25.50,   12, 15.30,  285.60",
        // 0.25 * 6% = 0.015 -> 0.02
        "1,  0.25,    12, 0.02,   0.29",
        // 0.05 * 9% = 0.0045 -> 0.00
        "1,  0.05,    18, 0.00,   0.05",
        // 0.25 * 14% = 0.035 -> 0.04
        "1,  0.25,    28, 0.04,   0.33"
    })
    void roundsEachComponentHalfUpToTwoDecimals(
            int quantity, String unitPrice, String rate, String cgst, String lineTotal) {
        LineAmounts line = GstCalculator.line(quantity, d(unitPrice), d(rate));

        assertThat(line.gst().cgstAmount()).isEqualByComparingTo(cgst);
        assertThat(line.gst().sgstAmount()).isEqualByComparingTo(cgst);
        assertThat(line.lineTotal()).isEqualByComparingTo(lineTotal);
        assertThat(line.lineTotal().scale()).isEqualTo(2);
    }

    @Test
    void unitPriceWithMoreThanTwoDecimalsIsRoundedHalfUpBeforeMultiplying() {
        LineAmounts line = GstCalculator.line(3, d("10.005"), d("12"));

        assertThat(line.unitPrice()).isEqualByComparingTo("10.01");
        assertThat(line.gst().taxableAmount()).isEqualByComparingTo("30.03");
        // 30.03 * 6% = 1.8018 -> 1.80
        assertThat(line.gst().cgstAmount()).isEqualByComparingTo("1.80");
        assertThat(line.lineTotal()).isEqualByComparingTo("33.63");
    }

    @Test
    void rateWithTrailingZerosIsAccepted() {
        assertThat(GstCalculator.line(2, d("50"), d("18.00")).gst().gstAmount()).isEqualByComparingTo("18.00");
        assertThat(GstCalculator.requireAllowedRate(d("5.0"))).isEqualTo(d("5.00"));
    }

    @Test
    void zeroPriceAndZeroRateYieldZeroTax() {
        LineAmounts free = GstCalculator.line(5, d("0"), d("28"));
        assertThat(free.gst().gstAmount()).isEqualByComparingTo("0");
        assertThat(free.lineTotal()).isEqualByComparingTo("0");

        LineAmounts exempt = GstCalculator.line(4, d("99.99"), d("0"));
        assertThat(exempt.gst().gstAmount()).isEqualByComparingTo("0");
        assertThat(exempt.lineTotal()).isEqualByComparingTo("399.96");
    }

    @Test
    void largeAmountsKeepFullPrecision() {
        LineAmounts line = GstCalculator.line(1_000_000, d("9999.99"), d("28"));

        assertThat(line.gst().taxableAmount()).isEqualByComparingTo("9999990000.00");
        assertThat(line.gst().cgstAmount()).isEqualByComparingTo("1399998600.00");
        assertThat(line.lineTotal()).isEqualByComparingTo("12799987200.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "3", "7.5", "12.5", "-5", "100", "18.01", "40"})
    void rejectsNonSlabRates(String rate) {
        assertThat(GstCalculator.isAllowedRate(d(rate))).isFalse();
        assertThat(new GstRateValidator().isValid(d(rate), null)).isFalse();
        assertThatThrownBy(() -> GstCalculator.line(1, d("10"), d(rate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0, 5, 12, 18, 28");
    }

    @ParameterizedTest
    @NullSource
    void nullRateIsRejectedByCalculatorButLeftToNotNullByValidator(BigDecimal rate) {
        assertThat(GstCalculator.isAllowedRate(rate)).isFalse();
        assertThat(new GstRateValidator().isValid(rate, null)).isTrue();
        assertThatThrownBy(() -> GstCalculator.split(d("10"), rate)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveQuantity(int quantity) {
        assertThatThrownBy(() -> GstCalculator.line(quantity, d("10"), d("5")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThatThrownBy(() -> GstCalculator.line(1, d("-0.01"), d("5")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GstCalculator.split(d("-1"), d("5")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invoiceTotalsSumLineLevelComponents() {
        List<LineAmounts> lines = List.of(
                GstCalculator.line(10, d("25.50"), d("12")),
                GstCalculator.line(3, d("120.00"), d("18")),
                GstCalculator.line(7, d("13.99"), d("5")),
                GstCalculator.line(2, d("45.00"), d("0")));

        InvoiceTotals totals = GstCalculator.totals(lines);

        assertThat(totals.subtotal()).isEqualByComparingTo("802.93");
        assertThat(totals.cgstAmount()).isEqualByComparingTo("50.15");
        assertThat(totals.sgstAmount()).isEqualByComparingTo("50.15");
        assertThat(totals.gstAmount()).isEqualByComparingTo("100.30");
        assertThat(totals.total()).isEqualByComparingTo("903.23");
    }

    @Test
    void totalsOfNoLinesAreZero() {
        InvoiceTotals totals = GstCalculator.totals(List.of());
        assertThat(totals.total()).isEqualByComparingTo("0");
        assertThat(totals.total().scale()).isEqualTo(2);
    }

    @Test
    void lineLevelRoundingIsAppliedPerLineNotOnTheSum() {
        // Two lines of 0.20 @ 5%: each CGST 0.005 -> 0.01, so 0.02 in total (0.40 @ 2.5% would also be 0.01).
        InvoiceTotals totals = GstCalculator.totals(List.of(
                GstCalculator.line(1, d("0.20"), d("5")), GstCalculator.line(1, d("0.20"), d("5"))));
        assertThat(totals.cgstAmount()).isEqualByComparingTo("0.02");
        assertThat(totals.gstAmount()).isEqualByComparingTo("0.04");
    }

    @Test
    void breakdownGroupsByRateAscending() {
        List<GstSplit> splits = List.of(
                GstCalculator.split(d("100"), d("18")),
                GstCalculator.split(d("50"), d("5")),
                GstCalculator.split(d("200"), d("18")),
                GstCalculator.split(d("10"), d("0")));

        List<GstSplit> breakdown = GstCalculator.breakdownByRate(splits);

        assertThat(breakdown).extracting(GstSplit::gstRate).containsExactly(d("0.00"), d("5.00"), d("18.00"));
        GstSplit eighteen = breakdown.get(2);
        assertThat(eighteen.taxableAmount()).isEqualByComparingTo("300.00");
        assertThat(eighteen.cgstAmount()).isEqualByComparingTo("27.00");
        assertThat(eighteen.sgstAmount()).isEqualByComparingTo("27.00");
        assertThat(eighteen.gstAmount()).isEqualByComparingTo("54.00");
        assertThat(eighteen.cgstRate()).isEqualByComparingTo("9");
    }

    @Test
    void invariantsHoldForRandomLines() {
        Random random = new Random(42);
        for (int i = 0; i < 2_000; i++) {
            BigDecimal price = BigDecimal.valueOf(random.nextInt(1_000_000), 2);
            int quantity = 1 + random.nextInt(500);
            BigDecimal rate = GstCalculator.ALLOWED_RATES.get(random.nextInt(GstCalculator.ALLOWED_RATES.size()));

            LineAmounts line = GstCalculator.line(quantity, price, rate);
            GstSplit gst = line.gst();

            assertThat(gst.cgstAmount()).isEqualTo(gst.sgstAmount());
            assertThat(gst.gstAmount()).isEqualTo(gst.cgstAmount().add(gst.sgstAmount()));
            assertThat(line.lineTotal()).isEqualTo(gst.taxableAmount().add(gst.gstAmount()));
            BigDecimal exactHalf = gst.taxableAmount().multiply(rate).divide(d("200"));
            assertThat(gst.cgstAmount().subtract(exactHalf).abs()).isLessThanOrEqualTo(d("0.005"));
        }
    }
}
