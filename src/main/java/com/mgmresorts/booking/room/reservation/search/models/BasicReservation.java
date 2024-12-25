package com.mgmresorts.booking.room.reservation.search.models;

import com.mgmresorts.booking.room.oxi.models.RatePlans;
import com.mgmresorts.booking.room.oxi.models.extensions.CheckinInfo;
import com.mgmresorts.booking.room.oxi.models.extensions.Metadata;
import com.mgmresorts.booking.room.oxi.models.extensions.RoomUpsell;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import lombok.Data;

@Data
public class BasicReservation {

    private String hotelCode;
    private String operaConfirmationNumber;
    private String resvNameId;
    private String roomType;
    private String status;
    private int guests;
    private String firstName;
    private String lastName;
    private String mgmId;
    private String mlifeNumber;
    private XMLGregorianCalendar arrivalTime;
    private int stayLength;
    private boolean arriveTimeSet;
    private boolean sharedReservation;
    private List<AdditionalGuest> additionalGuests;
    private RoomUpsell roomUpsell;
    private boolean upcoming;
    private CheckinInfo checkinInfo;
    private Metadata metadata;
    private RatePlans ratePlans;
    private String id;
}
