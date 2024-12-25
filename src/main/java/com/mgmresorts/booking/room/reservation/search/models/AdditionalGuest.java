package com.mgmresorts.booking.room.reservation.search.models;

import lombok.Data;

public @Data class AdditionalGuest {

    private String profileId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String mgmId;
    private String reservationStatus;
    private String reservationID;
    private String resvNameId;
    private boolean precreated;
    private String shareType;
    private AdditionalGuestMetadata metadata;
    private Payment payment;
}
