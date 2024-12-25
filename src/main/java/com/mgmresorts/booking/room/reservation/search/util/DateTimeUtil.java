package com.mgmresorts.booking.room.reservation.search.util;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import lombok.experimental.UtilityClass;

/**
 * Utility class providing common utility functions required for the project
 *
 */
@UtilityClass
public final class DateTimeUtil {

    /**
     * Returns epoch millis by parsing the date string
     * 
     * @param dateStr
     *            Date string
     * @return Returns epoch millis by parsing the date string
     */
    public static long getEpochMillis(String dateStr) {
        return LocalDate.parse(dateStr).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * Returns epoch millis by adding the days passed to the date represented as
     * string
     * 
     * @param dateStr
     *            Date String
     * @param plusDays
     *            Number of days to add
     * @return Returns epoch millis by adding the days passed to the date
     *         represented as string
     */
    public static long getEpochMillis(String dateStr, int plusDays) {
        return LocalDate.parse(dateStr).plusDays(plusDays).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * Tests if passed DateTime string is in the past
     * 
     * @param dateTimeStr
     *            DateTime String
     * @return Returns true if passed DateTime string is in the past
     */
    public boolean isLocalDateTimeInThePast(LocalDateTime localDateTime){

		ZonedDateTime nowZoned = ZonedDateTime.now(ZoneId.of(PACIFIC_TIME_ZONE));
		LocalDateTime nowLocal = nowZoned.toLocalDateTime();

		return nowLocal.isAfter(localDateTime);
	}
}
