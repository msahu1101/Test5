package com.mgmresorts.booking.room.reservation.search.dao;

import java.util.List;
import java.util.Map;

import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.reservation.search.response.ResponseWithHeaders;

/**
 * Data access interface to perform search operations against cosmos DB.
 *
 */
public interface DocumentDao {

	/**
	 * Search reservation documents in cosmos DB based on the search params
	 * supplied.
	 * 
	 * @param params
	 *            Search params
	 * @param basicInfo
	 *            Boolean to indicate if basic or full reservation payload to be
	 *            returned
	 * @return Returns list of reservation documents found
	 */
	List<Reservation> searchReservations(Map<String, String> params);
	
	/**
	 * Search INHOUSE reservation documents in cosmos DB based on the search params
	 * supplied.
	 * 
	 * @param params Search params
	 * 
	 * @return Returns list of reservation documents found
	 */
	Reservation searchInHouseReservations(Map<String, String> params);

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
