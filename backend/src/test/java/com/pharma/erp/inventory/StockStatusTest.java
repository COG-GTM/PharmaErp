package com.pharma.erp.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharma.erp.inventory.entity.StockStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StockStatusTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);

    @ParameterizedTest(name = "qty={0}, expiryOffset={1}, reorder={2} -> {3}")
    @CsvSource({
            "100, 30, 10, IN_STOCK",
            "11, 30, 10, IN_STOCK",
            "10, 30, 10, LOW",
            "1, 30, 10, LOW",
            "0, 30, 10, OUT_OF_STOCK",
            "100, 0, 10, IN_STOCK",
            "100, -1, 10, EXPIRED",
            "0, -1, 10, EXPIRED",
            "5, 30, 0, IN_STOCK",
            "0, 30, 0, OUT_OF_STOCK"
    })
    void derivesStatusWithExpiryTakingPrecedence(int quantity, int expiryOffsetDays, int reorderLevel,
            StockStatus expected) {
        assertThat(StockStatus.derive(quantity, TODAY.plusDays(expiryOffsetDays), reorderLevel, TODAY))
                .isEqualTo(expected);
    }

    @Test
    void batchWithoutExpiryNeverExpires() {
        assertThat(StockStatus.derive(50, null, 10, TODAY)).isEqualTo(StockStatus.IN_STOCK);
        assertThat(StockStatus.derive(0, null, 10, TODAY)).isEqualTo(StockStatus.OUT_OF_STOCK);
    }
}
