package com.pharma.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pharma.erp.finance.service.AmountInWords;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AmountInWordsTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "0       | Rupees Zero Only",
        "1       | Rupees One Only",
        "15      | Rupees Fifteen Only",
        "20      | Rupees Twenty Only",
        "99      | Rupees Ninety Nine Only",
        "100     | Rupees One Hundred Only",
        "903.23  | Rupees Nine Hundred Three and Twenty Three Paise Only",
        "1234.50 | Rupees One Thousand Two Hundred Thirty Four and Fifty Paise Only",
        "100000  | Rupees One Lakh Only",
        "14667.06 | Rupees Fourteen Thousand Six Hundred Sixty Seven and Six Paise Only",
        "2500000 | Rupees Twenty Five Lakh Only",
        "10000000 | Rupees One Crore Only",
        "123456789.99 | Rupees Twelve Crore Thirty Four Lakh Fifty Six Thousand Seven Hundred Eighty Nine and Ninety Nine Paise Only",
        "0.005   | Rupees Zero and One Paise Only"
    })
    void spellsAmountsInTheIndianSystem(String amount, String expected) {
        assertThat(AmountInWords.rupees(new BigDecimal(amount))).isEqualTo(expected);
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThatThrownBy(() -> AmountInWords.rupees(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
