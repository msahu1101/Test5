package com.mgmresorts.booking.room.reservation.search.dao;

import com.mgmresorts.booking.room.reservation.search.models.opera.GetFolioDetailsResponse;
import com.mgmresorts.booking.room.reservation.search.util.ServiceConstants;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface OperaDao {

	@RequestLine("GET /csh/v1/hotels/{hotelCode}/reservations/{reservationId}/folios?fetchInstructions=Payment&fetchInstructions=Postings&fetchInstructions=Reservation&fetchInstructions=Transactioncodes&limit="
			+ ServiceConstants.FOLIO_FETCH_LIMIT)
	@Headers({ "Content-Type: application/json", "Authorization: Bearer {authorization}", "x-app-key: {appKey}",
			"x-hotelid: {hotelCode}" })
	GetFolioDetailsResponse getFolioDetails(@Param("authorization") String authorization,
			@Param("appKey") String appKey,
			@Param("hotelCode") String hotelCode, @Param("reservationId") String reservationId);
}
