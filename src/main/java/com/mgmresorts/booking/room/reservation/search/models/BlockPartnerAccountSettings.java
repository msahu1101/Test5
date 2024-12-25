package com.mgmresorts.booking.room.reservation.search.models;

import java.util.List;

import lombok.Data;

public @Data class BlockPartnerAccountSettings {

    private boolean shouldBlockPartnerAccount;
    private List<String> blockedProgramCodes;
    private List<String> allowedTokens;
    private List<String> blockedReservationStatuses;
    private List<String> blockedHotelCodes;
    
}
