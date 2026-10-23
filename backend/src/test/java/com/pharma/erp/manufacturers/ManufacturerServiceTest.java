package com.pharma.erp.manufacturers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ManufacturerServiceTest {

    @Test
    void applyTrimsBlanksAndUppercasesIdentifiers() {
        Manufacturer m = new Manufacturer();
        ManufacturerService.apply(m, new ManufacturerRequest(
                "  Cipla Ltd ", ManufacturerType.DISTRIBUTOR, " mh-ws-20b-1 ", "  ", "", "a@b.co", " +91 1234567 ",
                " Mumbai ", null, "Maharashtra", "27aaacc1450b1zb"));

        assertThat(m.getName()).isEqualTo("Cipla Ltd");
        assertThat(m.getType()).isEqualTo(ManufacturerType.DISTRIBUTOR);
        assertThat(m.getLicenceNumber()).isEqualTo("MH-WS-20B-1");
        assertThat(m.getContactPerson()).isNull();
        assertThat(m.getAddress()).isNull();
        assertThat(m.getPhone()).isEqualTo("+91 1234567");
        assertThat(m.getCity()).isEqualTo("Mumbai");
        assertThat(m.getDistrict()).isNull();
        assertThat(m.getGstin()).isEqualTo("27AAACC1450B1ZB");
    }
}
