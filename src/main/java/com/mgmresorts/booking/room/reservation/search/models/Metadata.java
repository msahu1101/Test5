package com.mgmresorts.booking.room.reservation.search.models;

import com.mgmresorts.booking.room.oxi.models.extensions.CheckinInfo;
import com.mgmresorts.booking.room.oxi.models.extensions.EarlyCheckIn;
import lombok.Data;

@Data
public class Metadata {

    private boolean paymentUpdated;
    private boolean etaUpdated;
    private boolean paymentFailed;
    private boolean guestsUpdated;
    private int additionalGuestsCount;
    private EarlyCheckIn earlyCheckIn;
    private String checkinReversedDate;
    private CheckinInfo checkinInfo;
}
