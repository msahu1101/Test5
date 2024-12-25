package com.mgmresorts.booking.room.reservation.search.util;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.PACIFIC_TIME_ZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

public class DateTimeUtilTest {

    @Test
    public void testGetEpochMillis() {
        assertEquals(1590969600000L, DateTimeUtil.getEpochMillis("2020-06-01"));
    }

    @Test
    public void testGetEpochMillisPlusDays() {
        assertEquals(1591142400000L, DateTimeUtil.getEpochMillis("2020-06-01", 2));
    }

    @Test
	public void testIsLocalDateTimeInThePast() {
		LocalDateTime twoMinsForward = LocalDateTime.now(ZoneId.of(PACIFIC_TIME_ZONE)).plusMinutes(2);
		LocalDateTime twoMinsAgo = LocalDateTime.now(ZoneId.of(PACIFIC_TIME_ZONE)).minusMinutes(2);
		
		assertFalse(DateTimeUtil.isLocalDateTimeInThePast(twoMinsForward));
		assertTrue(DateTimeUtil.isLocalDateTimeInThePast(twoMinsAgo));
	}
}
