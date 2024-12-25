package com.mgmresorts.booking.room.reservation.search.models;

import lombok.Data;

public @Data class FolioResponse {

    private String operaConfirmationNumber;
    private String reservationStatus;
    private String checkInDate;
    private String checkOutDate;
    private String roomNumber;
    private FolioProfile profile;
    private Folio[] folios;
    private Double totalCharges;
    private Double totalCredits;
    private Double currentBalance;
}
