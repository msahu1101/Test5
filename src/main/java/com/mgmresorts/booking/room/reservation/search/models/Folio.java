package com.mgmresorts.booking.room.reservation.search.models;

import lombok.Data;

public @Data class Folio {

    private int windowNo;
    private double windowCharges;
    private double windowCredits;
    private double windowBalance;
    private String windowLastUpdated;
    private BillItem[] billItems;
}
