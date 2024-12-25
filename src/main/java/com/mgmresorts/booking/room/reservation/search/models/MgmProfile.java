package com.mgmresorts.booking.room.reservation.search.models;

import java.util.List;

import lombok.Data;

public @Data class MgmProfile {

    private String mgmId;
    private String mlifeNumber;
    private List<AdditionalGuest> additionalGuests;
}
