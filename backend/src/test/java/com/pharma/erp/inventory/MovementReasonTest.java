package com.pharma.erp.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharma.erp.inventory.entity.MovementReason;
import org.junit.jupiter.api.Test;

class MovementReasonTest {

    @Test
    void inboundReasonsRequirePositiveChange() {
        for (MovementReason reason : new MovementReason[] {
                MovementReason.RECEIPT, MovementReason.RETURN, MovementReason.TRANSFER_IN}) {
            assertThat(reason.accepts(5)).as(reason.name()).isTrue();
            assertThat(reason.accepts(-5)).as(reason.name()).isFalse();
        }
    }

    @Test
    void outboundReasonsRequireNegativeChange() {
        for (MovementReason reason : new MovementReason[] {
                MovementReason.SALE, MovementReason.TRANSFER_OUT, MovementReason.DAMAGE,
                MovementReason.EXPIRY_WRITE_OFF}) {
            assertThat(reason.accepts(-5)).as(reason.name()).isTrue();
            assertThat(reason.accepts(5)).as(reason.name()).isFalse();
        }
    }

    @Test
    void correctionAcceptsEitherSignButNotZeroAndStatusChangeIsNeverAnAdjustment() {
        assertThat(MovementReason.CORRECTION.accepts(3)).isTrue();
        assertThat(MovementReason.CORRECTION.accepts(-3)).isTrue();
        assertThat(MovementReason.CORRECTION.accepts(0)).isFalse();
        assertThat(MovementReason.STATUS_CHANGE.accepts(1)).isFalse();
        assertThat(MovementReason.STATUS_CHANGE.accepts(-1)).isFalse();
    }
}
