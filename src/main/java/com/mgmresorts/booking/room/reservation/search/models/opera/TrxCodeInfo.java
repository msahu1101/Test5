package com.mgmresorts.booking.room.reservation.search.models.opera;

import lombok.Data;

@Data
public class TrxCodeInfo {
    
    private String description;
    private String transactionCode;
    private String hotelId;
}
