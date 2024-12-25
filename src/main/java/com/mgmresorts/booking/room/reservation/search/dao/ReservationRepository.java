package com.mgmresorts.booking.room.reservation.search.dao;

import java.sql.SQLException;
import java.util.Map;

public interface ReservationRepository {

    /**
     * This method queries folio details for a reservation
     *
     *
     * @param resvNameId
     *            resv name id from cosmos reservation
     * @param resort
     *            resort/hotel code
     * @return Returns map of lists representing table response for folios
     * @throws SQLException
     *             Throws SQL Exception in case of any connectivity issues with
     *             Opera DB
     */
    Map<String, Object> getFolioDetails(String resvNameId, String resort, String folioNumber, Boolean aggregated) throws SQLException;
}
