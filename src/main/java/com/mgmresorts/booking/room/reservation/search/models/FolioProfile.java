package com.mgmresorts.booking.room.reservation.search.models;

import lombok.Data;

public @Data class FolioProfile {

    private String firstName;
    private String lastName;
    private String email;
    private String mlifeNumber;
    private String operaProfileId;
    private Address address;

}
