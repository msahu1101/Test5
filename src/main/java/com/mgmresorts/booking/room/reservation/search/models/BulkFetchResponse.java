package com.mgmresorts.booking.room.reservation.search.models;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import lombok.Data;

@Data
public class BulkFetchResponse {

    private String id;
    private MgmProfile mgmProfile;
	private String hotelCode;
	private String operaConfirmationNumber;
	private String resvNameId;
	private String roomType;
	private String roomNumber;
	private String status;
	private int guests;
	private String inventoryBlockCode;
	private String marketSegmentCode;
	private String sourceCode;
	private XMLGregorianCalendar arrivalTime;
	private XMLGregorianCalendar bookingDate;
	private int stayLength;
	private String guaranteeType;
	private Profile profile;
	private Payment payment;
	private List<Package> packages;
	private List<RatePlan> ratePlans;
	private List<SpecialRequest> specialRequests;
	private Metadata metadata;
	private List<ReservationReference> reservationReferences;
	private GuestCount guestCount = new GuestCount();
}
