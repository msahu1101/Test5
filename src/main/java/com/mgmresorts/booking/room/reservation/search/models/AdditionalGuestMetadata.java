package com.mgmresorts.booking.room.reservation.search.models;

import lombok.Data;

public @Data class AdditionalGuestMetadata {
    
    protected boolean paymentUpdated;
    protected boolean etaUpdated;
    protected boolean paymentFailed;

}   
