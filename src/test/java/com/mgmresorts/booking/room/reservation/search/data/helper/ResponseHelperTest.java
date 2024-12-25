package com.mgmresorts.booking.room.reservation.search.data.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgmresorts.booking.room.oxi.models.GuestCount;
import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.oxi.models.ReservationStatusType;
import com.mgmresorts.booking.room.oxi.models.RoomStay;
import com.mgmresorts.booking.room.reservation.search.BaseUnitTest;
import com.mgmresorts.booking.room.reservation.search.dao.helper.ResponseHelper;
import com.mgmresorts.booking.room.reservation.search.models.BasicReservation;
import com.mgmresorts.booking.room.reservation.search.transformer.BasicReservationResponseTransformer;

public class ResponseHelperTest extends BaseUnitTest {

    @Test
    public void testGetBasicReservation() {

        Reservation reservation = convert("/reservation.json", Reservation.class);
        
        BasicReservation basicResv = BasicReservationResponseTransformer.getBasicReservation(reservation);
        
        commonAssertions(reservation, basicResv);
        assertEquals(false, basicResv.isArriveTimeSet());
        assertEquals(false, basicResv.isSharedReservation());
        
    }
    
    @Test
    public void testGetBasicReservationShared() {

        Reservation reservation = convert("/reservation-shared.json", Reservation.class);
        
        BasicReservation basicResv = BasicReservationResponseTransformer.getBasicReservation(reservation);
        
        commonAssertions(reservation, basicResv);
        assertEquals(true, basicResv.isArriveTimeSet());
        assertEquals(true, basicResv.isSharedReservation());
        
    }
    
    @Test
    public void testGetBasicReservationETASet() {

        Reservation reservation = convert("/reservation-etaset.json", Reservation.class);
        
        BasicReservation basicResv = BasicReservationResponseTransformer.getBasicReservation(reservation);
        
        commonAssertions(reservation, basicResv);
        assertEquals(true, basicResv.isArriveTimeSet());
        assertEquals(false, basicResv.isSharedReservation());
        
    }
    
    private void commonAssertions(Reservation reservation, BasicReservation basicResv) {
        
        RoomStay roomStay = reservation.getRoomStays().getRoomStay().get(0);
        int guests = 0;
        for (GuestCount count : roomStay.getGuestCounts().getGuestCount()) {
            guests = guests + count.getMfCount();
        }
        
        assertEquals(reservation.getHotelReference().getHotelCode(), basicResv.getHotelCode());
        assertEquals(reservation.getReservationID(), basicResv.getOperaConfirmationNumber());
        assertEquals(reservation.getResvNameId(), basicResv.getResvNameId());
        assertEquals(reservation.getStayDateRange().getStartTime(), basicResv.getArrivalTime());
        assertEquals(reservation.getStayDateRange().getNumberOfTimeUnits().intValue(), basicResv.getStayLength());
        assertEquals(roomStay.getRoomInventoryCode(), basicResv.getRoomType());
        assertEquals(roomStay.getReservationStatusType().name(), basicResv.getStatus());
        assertEquals(guests, basicResv.getGuests());
    }
    
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(Include.NON_NULL).setSerializationInclusion(Include.NON_EMPTY);

    @Test
    public void testResolveDuplicates() throws IOException {
        
        // Test Data Scenarios:
        // 788697724 - 1 CANCELED, 1 INHOUSE -> 1 INHOUSE should be returned
        // 788697725 - 1 INHOUSE -> 1 INHOUSE should be returned
        // 788697726 - 2 CANCELED -> None should be returned
        // 788697727 - 2 RESERVED -> None should be returned
        // 788697728 - 1 CANCELED, 2 RESERVED -> None should be returned
        // 788697729 - 1 CANCELED -> 1 CANCELED should be returned
        // 797987153 - 2 RESERVED (share-with move scenario) - 1 with previousHotelCode  should be returned

        String responseJson = getFileContents("/reservations-duplicates.json");
        Reservation[] reservations = MAPPER.readValue(responseJson, Reservation[].class);

        List<Reservation> docsList = new ArrayList<>();
        for (Reservation resv : reservations) {
            docsList.add(resv);
        }

        docsList = ResponseHelper.resolveDuplicates(docsList);

        // 4 docs should be returned
        assertEquals(4, docsList.size());

        String expectedConfNumbers = "788697724,788697725,788697729,797987153";

        docsList.forEach(doc -> {
            Reservation resv = (Reservation) doc;

            assertTrue(expectedConfNumbers.contains(resv.getReservationID()));

            RoomStay roomStay = resv.getRoomStays().getRoomStay().get(0);
            ReservationStatusType status = roomStay.getReservationStatusType();

            if (resv.getReservationID().equals("788697724")) {
                assertEquals(ReservationStatusType.INHOUSE, status);
            } else if (resv.getReservationID().equals("788697725")) {
                assertEquals(ReservationStatusType.INHOUSE, status);
            } else if (resv.getReservationID().equals("788697729")) {
                assertEquals(ReservationStatusType.CANCELED, status);
            } else if (resv.getReservationID().equals("797987153")) {
                assertEquals(ReservationStatusType.RESERVED, status);
                assertEquals("001", resv.getHotelReference().getHotelCode());
                assertEquals("290", resv.getPreviousHotelCode());
            }

        });
    }
}
