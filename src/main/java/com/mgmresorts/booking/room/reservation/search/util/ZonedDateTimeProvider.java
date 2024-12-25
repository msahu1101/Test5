package com.mgmresorts.booking.room.reservation.search.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ZonedDateTimeProvider {

	public ZonedDateTime now(ZoneId zoneId) {

		return ZonedDateTime.now(zoneId);
    }
}