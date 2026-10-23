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

    @Test
    void requestTreatsBlankOptionalStringsAsAbsent() {
        ManufacturerRequest r = new ManufacturerRequest(
                " X ", ManufacturerType.SUPPLIER, " ", null, "\t", "  ", "", null, null, null, "   ");
        assertThat(r.name()).isEqualTo("X");
        assertThat(r.licenceNumber()).isNull();
        assertThat(r.address()).isNull();
        assertThat(r.email()).isNull();
        assertThat(r.phone()).isNull();
        assertThat(r.gstin()).isNull();
    }

    @Test
    void escapeLikeQuotesWildcardsAndEscapeChar() {
        assertThat(ManufacturerService.escapeLike("a_b%c!d")).isEqualTo("a!_b!%c!!d");
        assertThat(ManufacturerService.escapeLike("plain")).isEqualTo("plain");
    }
}
