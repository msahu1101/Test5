package com.mgmresorts.booking.room.reservation.search.models.opera;

import java.util.List;

import lombok.Data;

@Data
public class GetFolioDetailsResponse {

    private ReservationFolioInformation reservationFolioInformation;
    private List<TrxCodeInfo> trxCodesInfo;
}
