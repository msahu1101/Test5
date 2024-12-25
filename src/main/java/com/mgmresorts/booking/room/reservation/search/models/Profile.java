package com.mgmresorts.booking.room.reservation.search.models;

import lombok.Data;

public @Data class Profile {

    private String firstName;
    private String lastName;
    private String email;
    private String mlifeNumber;
    private String tierStatus;
    private String vipStatus;
    private String operaProfileId;
    private Address address;
    private String mobilePhone;
}
