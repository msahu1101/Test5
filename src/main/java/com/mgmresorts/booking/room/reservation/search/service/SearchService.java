package com.mgmresorts.booking.room.reservation.search.service;

import java.util.Map;

import com.mgmresorts.booking.room.reservation.search.response.ResponseWithHeaders;

public interface SearchService {

	/**
	 * Search reservation documents in cosmos DB based on the search params
	 * supplied.
	 * 
	 * @param params
	 *            Search params
	 * @param basicInfo
	 *            Boolean to indicate if basic or full reservation payload to be
	 *            returned
	 * @return Returns json string of reservation documents found
	 */
	String searchReservations(Map<String, String> params, boolean basicInfo);

	/**
	 * Search INHOUSE reservation documents in cosmos DB based on the search params
	 * supplied.
	 * 
	 * @param params    Search params
	 * @param basicInfo Boolean to indicate if basic or full reservation payload to
	 *                  be returned
	 * @return Returns json string of reservation documents found
	 */
	String searchInHouseReservations(Map<String, String> params);

	/**
	 * Search reservation documents in cosmos DB based on the search params
	 * supplied and returns a result object which will include result and
	 * continuation token
	 * 
	 * @param params
	 *            Search params
	 * @return Returns result object which will include result and continuation
	 *         token
	 */
	ResponseWithHeaders fetchBulkReservations(Map<String, String> params);

	/**
	 * Search & return reservation profiles in cosmos DB for all reservations
	 * requested
	 * 
	 * @param params
	 *            Search params
	 * @return Returns json string of reservation profiles found
	 */
	String searchReservationProfiles(Map<String, String> params);
}
