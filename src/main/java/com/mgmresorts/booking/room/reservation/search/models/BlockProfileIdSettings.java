package com.mgmresorts.booking.room.reservation.search.models;

import lombok.Data;

@Data
public class BlockProfileIdSettings {
    
    private String blockedProfileIds;
    private String allowedTokens;
    private String blockedReservationStatuses;
}


