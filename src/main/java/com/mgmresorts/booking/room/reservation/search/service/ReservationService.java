package com.mgmresorts.booking.room.reservation.search.service;

import java.util.Map;

public interface ReservationService {

    /**
     * Service to find reservation based on search params and fetch folio items
     * for the found reservation.
     * 
     * @param params
     *            Maps of params to be used for search
     * @return Returns complete folio information for the reservation
     */
    String fetchFolio(Map<String, String> params);
}
