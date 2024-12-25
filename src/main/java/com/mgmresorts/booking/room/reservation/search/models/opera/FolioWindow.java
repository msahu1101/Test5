package com.mgmresorts.booking.room.reservation.search.models.opera;

import java.util.List;

import lombok.Data;

@Data
public class FolioWindow {

    private List<Folio> folios;
    private PaymentMethod paymentMethod;
    private Integer folioWindowNo;
    private Balance balance;

}
