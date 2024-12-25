package com.mgmresorts.booking.room.reservation.search.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.reservation.search.BaseUnitTest;
import com.mgmresorts.booking.room.reservation.search.models.BasicReservation;
import com.mgmresorts.booking.room.reservation.search.models.BasicReservationProfile;
import com.mgmresorts.booking.room.reservation.search.models.InHouseReservation;

public class BasicReservationResponseTransformerTest extends BaseUnitTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testBasicResvProfileResponse() throws IOException {

        Reservation resv = convert("/reservation.json", Reservation.class);

        BasicReservationProfile resvProfile = BasicReservationResponseTransformer.getBasicReservationProfile(resv);

        assertEquals("Test", resvProfile.getFirstName());
        assertEquals("Test", resvProfile.getLastName());
        assertEquals("797907620", resvProfile.getReservationID());
        assertEquals("786195318", resvProfile.getResvNameId());
    }

    @Test
    public void testBasicResvResponse() throws IOException {
        Reservation resv = convert("/reservation.json", Reservation.class);

        BasicReservation resvBasic = BasicReservationResponseTransformer.getBasicReservation(resv);

        assertEquals("285", resvBasic.getHotelCode());
        assertEquals("797907620", resvBasic.getOperaConfirmationNumber());
        assertEquals("786195318", resvBasic.getResvNameId());
        
        assertEquals("DWTK", resvBasic.getRoomType());
        assertEquals("RESERVED", resvBasic.getStatus());

        assertEquals(2, resvBasic.getGuests());
        assertEquals(1, resvBasic.getStayLength());

        assertEquals("Test", resvBasic.getFirstName());
        assertEquals("Test", resvBasic.getLastName());

        assertEquals("AWARDED", resvBasic.getRoomUpsell().getStatus().name());
        assertEquals("CONFIRMED", resvBasic.getRoomUpsell().getType().name());
    }
    
	@Test
	public void testBasicInHouseReservationResponse() {
		Reservation reservation = convert("/reservation.json", Reservation.class);

		InHouseReservation inHouseReservation = BasicReservationResponseTransformer
				.getBasicInhouseReservation(reservation);

		assertNotNull(inHouseReservation);
		assertEquals("786195318", inHouseReservation.getResvNameId());
		assertEquals("DWTK", inHouseReservation.getRoomType());
	}
}