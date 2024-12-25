package com.mgmresorts.booking.room.reservation.search.models;

import javax.xml.datatype.XMLGregorianCalendar;

import lombok.Data;

@Data
public class Payment {

	private String cardType;
	private String cardHolderName;
	private String maskedCardNumber;
	private String cardToken;
	private XMLGregorianCalendar cardExpiry;
}
