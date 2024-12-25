package com.mgmresorts.booking.room.reservation.search.models.opera;

import java.util.List;

import lombok.Data;

@Data
public class ReservationFolioInformation {

    private ReservationInfo reservationInfo;
    private List<FolioWindow> folioWindows;
}
