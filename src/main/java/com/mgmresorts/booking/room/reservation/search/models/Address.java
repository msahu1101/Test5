package com.mgmresorts.booking.room.reservation.search.models;

import lombok.Data;

public @Data class Address {

    private String street;
    private String city;
    private String state;
    private String country;
    private String zip;
}
