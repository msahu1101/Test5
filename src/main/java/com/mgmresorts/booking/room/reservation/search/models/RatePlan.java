package com.mgmresorts.booking.room.reservation.search.models;

import javax.xml.datatype.XMLGregorianCalendar;

import lombok.Data;

@Data
public class RatePlan {

	private XMLGregorianCalendar startDate;
	private XMLGregorianCalendar endDate;
	private String ratePlanCode;
}
