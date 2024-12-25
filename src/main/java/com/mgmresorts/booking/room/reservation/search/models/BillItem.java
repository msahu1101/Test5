package com.mgmresorts.booking.room.reservation.search.models;

import lombok.Data;

public @Data class BillItem {

    private String date;
    private String description;
    private Double creditAmount;
    private Double chargeAmount;
    private String supplement;
    private String reference;
    private String ccLast4Digits;
}
