package com.mgmresorts.booking.room.reservation.search.models;

import java.math.BigDecimal;

import lombok.Data;

public @Data class Package {

    private String serviceInventoryCode;
    private BigDecimal price;
}
