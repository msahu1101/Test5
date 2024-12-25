package com.mgmresorts.booking.room.reservation.search.models;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SearchRequest {

	private String firstName;
	private String lastName;
	private String checkInDate;
	private String checkOutDate;
	private String hotelCode;
	private String email;
	private String mlifeNumber;

	public SearchRequest(Map<String, String> params) {
		this.firstName = params.getOrDefault("firstName", StringUtils.EMPTY);
		this.lastName = params.getOrDefault("lastName", StringUtils.EMPTY);
		this.checkInDate = params.getOrDefault("checkInDate", StringUtils.EMPTY);
		this.checkOutDate = params.getOrDefault("checkOutDate", StringUtils.EMPTY);
		this.hotelCode = params.getOrDefault("hotelCode", StringUtils.EMPTY);
		this.email = params.getOrDefault("email", StringUtils.EMPTY);
		this.mlifeNumber = params.getOrDefault("mlifeNumber", StringUtils.EMPTY);
	}
}
