package com.pharma.erp.inventory.entity;

/** Why a stock movement happened; stored in {@code stock_tracking.reason}. */
public enum MovementReason {
    RECEIPT(Direction.IN),
    RETURN(Direction.IN),
    TRANSFER_IN(Direction.IN),
    SALE(Direction.OUT),
    TRANSFER_OUT(Direction.OUT),
    DAMAGE(Direction.OUT),
    EXPIRY_WRITE_OFF(Direction.OUT),
    CORRECTION(Direction.EITHER),
    /** System-recorded logistics status change (quantity unchanged); not accepted by the adjust endpoint. */
    STATUS_CHANGE(Direction.NONE);

    public enum Direction { IN, OUT, EITHER, NONE }

    private final Direction direction;

    MovementReason(Direction direction) {
        this.direction = direction;
    }

    public Direction direction() {
        return direction;
    }

    /** Whether a non-zero {@code quantityChange} has the sign this reason requires. */
    public boolean accepts(int quantityChange) {
        return switch (direction) {
            case IN -> quantityChange > 0;
            case OUT -> quantityChange < 0;
            case EITHER -> quantityChange != 0;
            case NONE -> false;
        };
    }
}
