package com.mgmresorts.booking.room.reservation.search.models.opera;

import lombok.Data;

@Data
public class Posting {
    
    private String transactionDate;
    private String aggregatedDescription;
    private String transactionCode;
    private String transactionType;
    private String remark;
    private String reference = "";
    private DebitAmount debitAmount;
    private CreditAmount creditAmount;
    private String postingDate;
    private Long transactionNo;
}
