package com.mgmresorts.booking.room.reservation.search.transformer;

import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.reservation.search.BaseUnitTest;
import com.mgmresorts.booking.room.reservation.search.models.BillItem;
import com.mgmresorts.booking.room.reservation.search.models.Folio;
import com.mgmresorts.booking.room.reservation.search.models.FolioResponse;
import com.mgmresorts.booking.room.reservation.search.models.opera.GetFolioDetailsResponse;

import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FolioResponseTransformerTest extends BaseUnitTest {
//    FolioResponse folioResponse = convert("/folioResponse.json", FolioResponse.class);

    @Test
    public void testTransform() throws IOException {
        FolioResponse folioResponse = convert("/folioResponse.json", FolioResponse.class);
        FolioResponse folioResponseWithoutProfilePII = convert("/folioResponse-without-pii.json", FolioResponse.class);
        Reservation[] reservations = convert("/reservations.json", Reservation[].class);
        Reservation resv = reservations[3];

        Map<String, Object> resultMap = new HashMap<>();
        ArrayList<String> room = new ArrayList<>();
        room.add("room");
        room.add("room");
        room.add("room");
        resultMap.put("ROOM", room);

        ArrayList<String> dates = new ArrayList<>();
        dates.add("2019-12-02 00:00:00");
        dates.add("2019-12-03 00:00:00");
        dates.add("2019-12-04 00:00:00");
        resultMap.put("TRX_DATE", dates);

        ArrayList<String> descriptions = new ArrayList<>();
        descriptions.add("description");
        descriptions.add("description_1");
        descriptions.add("description_2");
        resultMap.put("TRANSACTION_DESCRIPTION", descriptions);

        ArrayList<String> debits = new ArrayList<>();
        debits.add("10.00");
        debits.add("24.567891200982309");
        debits.add("15.367891200982309");
        resultMap.put("GUEST_ACCOUNT_DEBIT", debits);

        ArrayList<String> credits = new ArrayList<>();
        credits.add(null);
        credits.add("13.467891200982309");
        credits.add(null);
        resultMap.put("GUEST_ACCOUNT_CREDIT", credits);

        ArrayList<String> ccLast4Digits = new ArrayList<>();
        ccLast4Digits.add("4444");
        ccLast4Digits.add(null);
        ccLast4Digits.add(null);
        resultMap.put("CREDIT_CARD_NUMBER_4_DIGITS", ccLast4Digits);

        ArrayList<String> supplements = new ArrayList<>();
        supplements.add("remark");
        supplements.add("remark_1");
        supplements.add("remark_2");
        resultMap.put("REMARK", supplements);

        ArrayList<String> references = new ArrayList<>();
        references.add("reference");
        references.add("reference_1");
        references.add("reference_2");
        resultMap.put("REFERENCE", references);

        ArrayList<Integer> folioView = new ArrayList<>();
        folioView.add(1);
        folioView.add(1);
        folioView.add(1);
        resultMap.put("FOLIO_VIEW", folioView);

        ArrayList<String> updateDate = new ArrayList<>();
        updateDate.add("2020-01-01");
        updateDate.add("2020-01-02");
        updateDate.add("");
        resultMap.put("UPDATE_DATE", updateDate);

        Map<String, String> params = new HashMap<>();
        params.put("jwtExists", "true");
        params.put("checkInDate", "2019-12-18");
        FolioResponse response = new FolioResponse();
        FolioResponse responseWithoutProfilePII = new FolioResponse();

        FolioResponseTransformer.setBaseResponse(response, resv, "true", false);
        FolioResponseTransformer.transform(response, resultMap);
        assertEquals(folioResponse, response);

        FolioResponseTransformer.setBaseResponse(responseWithoutProfilePII, resv, "true", true);
        FolioResponseTransformer.transform(responseWithoutProfilePII, resultMap);
        assertEquals(folioResponseWithoutProfilePII, responseWithoutProfilePII);
    }

    @Test
    public void testTransform_allUpdatedDatesEmpty() throws IOException {
        Reservation[] reservations = convert("/reservations.json", Reservation[].class);
        FolioResponse folioResponse = convert("/folioResponse.json", FolioResponse.class);
        int folioSize = folioResponse.getFolios().length;
        for(int i = 0; i < folioSize; i++){
            folioResponse.getFolios()[i].setWindowLastUpdated(null);
        }
        Reservation resv = reservations[3];

        Map<String, Object> resultMap = new HashMap<>();
        ArrayList<String> room = new ArrayList<>();
        room.add("room");
        room.add("room");
        room.add("room");
        resultMap.put("ROOM", room);

        ArrayList<String> dates = new ArrayList<>();
        dates.add("2019-12-02 00:00:00");
        dates.add("2019-12-03 00:00:00");
        dates.add("2019-12-04 00:00:00");
        resultMap.put("TRX_DATE", dates);

        ArrayList<String> descriptions = new ArrayList<>();
        descriptions.add("description");
        descriptions.add("description_1");
        descriptions.add("description_2");
        resultMap.put("TRANSACTION_DESCRIPTION", descriptions);

        ArrayList<String> debits = new ArrayList<>();
        debits.add("10.00");
        debits.add("24.567891200982309");
        debits.add("15.367891200982309");
        resultMap.put("GUEST_ACCOUNT_DEBIT", debits);

        ArrayList<String> credits = new ArrayList<>();
        credits.add(null);
        credits.add("13.467891200982309");
        credits.add(null);
        resultMap.put("GUEST_ACCOUNT_CREDIT", credits);

        ArrayList<String> ccLast4Digits = new ArrayList<>();
        ccLast4Digits.add("4444");
        ccLast4Digits.add(null);
        ccLast4Digits.add(null);
        resultMap.put("CREDIT_CARD_NUMBER_4_DIGITS", ccLast4Digits);

        ArrayList<String> supplements = new ArrayList<>();
        supplements.add("remark");
        supplements.add("remark_1");
        supplements.add("remark_2");
        resultMap.put("REMARK", supplements);

        ArrayList<String> references = new ArrayList<>();
        references.add("reference");
        references.add("reference_1");
        references.add("reference_2");
        resultMap.put("REFERENCE", references);

        ArrayList<Integer> folioView = new ArrayList<>();
        folioView.add(1);
        folioView.add(1);
        folioView.add(1);
        resultMap.put("FOLIO_VIEW", folioView);

        ArrayList<String> updateDate = new ArrayList<>();
        updateDate.add("");
        updateDate.add("");
        updateDate.add("");
        resultMap.put("UPDATE_DATE", updateDate);

        Map<String, String> params = new HashMap<>();
        params.put("jwtExists", "true");
        params.put("checkInDate", "2019-12-18");
        FolioResponse response = new FolioResponse();

        FolioResponseTransformer.setBaseResponse(response, resv, "true", false);
        FolioResponseTransformer.transform(response, resultMap);
        assertEquals(folioResponse, response);
    }

    @Test
    public void testTransform_only1UpdatedDateIsNotEmpty() throws IOException {
        Reservation[] reservations = convert("/reservations.json", Reservation[].class);
        FolioResponse folioResponse = convert("/folioResponse.json", FolioResponse.class);

        Reservation resv = reservations[3];

        Map<String, Object> resultMap = new HashMap<>();
        ArrayList<String> room = new ArrayList<>();
        room.add("room");
        room.add("room");
        room.add("room");
        resultMap.put("ROOM", room);

        ArrayList<String> dates = new ArrayList<>();
        dates.add("2019-12-02 00:00:00");
        dates.add("2019-12-03 00:00:00");
        dates.add("2019-12-04 00:00:00");
        resultMap.put("TRX_DATE", dates);

        ArrayList<String> descriptions = new ArrayList<>();
        descriptions.add("description");
        descriptions.add("description_1");
        descriptions.add("description_2");
        resultMap.put("TRANSACTION_DESCRIPTION", descriptions);

        ArrayList<String> debits = new ArrayList<>();
        debits.add("10.00");
        debits.add("24.567891200982309");
        debits.add("15.367891200982309");
        resultMap.put("GUEST_ACCOUNT_DEBIT", debits);

        ArrayList<String> credits = new ArrayList<>();
        credits.add(null);
        credits.add("13.467891200982309");
        credits.add(null);
        resultMap.put("GUEST_ACCOUNT_CREDIT", credits);

        ArrayList<String> ccLast4Digits = new ArrayList<>();
        ccLast4Digits.add("4444");
        ccLast4Digits.add(null);
        ccLast4Digits.add(null);
        resultMap.put("CREDIT_CARD_NUMBER_4_DIGITS", ccLast4Digits);

        ArrayList<String> supplements = new ArrayList<>();
        supplements.add("remark");
        supplements.add("remark_1");
        supplements.add("remark_2");
        resultMap.put("REMARK", supplements);

        ArrayList<String> references = new ArrayList<>();
        references.add("reference");
        references.add("reference_1");
        references.add("reference_2");
        resultMap.put("REFERENCE", references);

        ArrayList<Integer> folioView = new ArrayList<>();
        folioView.add(1);
        folioView.add(1);
        folioView.add(1);
        resultMap.put("FOLIO_VIEW", folioView);

        ArrayList<String> updateDate = new ArrayList<>();
        updateDate.add("");
        updateDate.add("2020-01-02");
        updateDate.add("");
        resultMap.put("UPDATE_DATE", updateDate);

        Map<String, String> params = new HashMap<>();
        params.put("jwtExists", "true");
        params.put("checkInDate", "2019-12-18");
        FolioResponse response = new FolioResponse();

        FolioResponseTransformer.setBaseResponse(response, resv, "true", false);
        FolioResponseTransformer.transform(response, resultMap);
        assertEquals(folioResponse, response);
    }

    @Test
    public void testGetFutureDate() throws Exception {
        Reservation[] reservations = convert("/reservations.json", Reservation[].class);
        Reservation resv = reservations[3];
        resv.getStayDateRange().setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2020, 2, 27,
                DatatypeConstants.FIELD_UNDEFINED));
        assertEquals("2020-02-29", FolioResponseTransformer.getCheckInAndCheckOut(resv)[1]);
        resv.getStayDateRange().setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2019, 2, 27,
                DatatypeConstants.FIELD_UNDEFINED));
        assertEquals("2019-03-01", FolioResponseTransformer.getCheckInAndCheckOut(resv)[1]);
    }

    @Test
    public void testTransformOperaCloudResponse() {
        GetFolioDetailsResponse operaCloudResponse = convert("/operaCloudFolioResponse.json", GetFolioDetailsResponse.class);
        FolioResponse folioResponse = new FolioResponse();

        FolioResponseTransformer.transformOperaCloudResponse(folioResponse, operaCloudResponse, false);

        assertEquals(666.35, folioResponse.getCurrentBalance());
        assertEquals(222.1, folioResponse.getTotalCredits());
        assertEquals(888.45, folioResponse.getTotalCharges());
        assertEquals(1, folioResponse.getFolios().length);

        Folio folio = folioResponse.getFolios()[0];
        
        assertEquals("2024-01-28", folio.getWindowLastUpdated());
        assertEquals(4, folio.getBillItems().length);
        assertEquals("4444", folio.getBillItems()[0].getCcLast4Digits());

        BillItem[] billItems = folio.getBillItems();
        assertEquals(444.15, billItems[0].getChargeAmount());
        assertNull(billItems[0].getCreditAmount());
        assertEquals("2024-01-26", billItems[0].getDate());
        assertEquals("King Room Tax", billItems[0].getDescription());
        assertEquals("12345", billItems[0].getReference());
    }

    @Test
    public void testTransformOperaCloudResponseAggregated() {
        GetFolioDetailsResponse operaCloudResponse = convert("/operaCloudFolioResponse.json", GetFolioDetailsResponse.class);
        FolioResponse folioResponse = new FolioResponse();

        FolioResponseTransformer.transformOperaCloudResponse(folioResponse, operaCloudResponse, true);

        assertEquals(666.35, folioResponse.getCurrentBalance());
        assertEquals(222.1, folioResponse.getTotalCredits());
        assertEquals(888.45, folioResponse.getTotalCharges());
        assertEquals(1, folioResponse.getFolios().length);

        Folio folio = folioResponse.getFolios()[0];
        
        assertEquals("2024-01-28", folio.getWindowLastUpdated());

        BillItem[] billItems = folio.getBillItems();

        assertEquals(3, billItems.length);

        assertEquals(777.3, billItems[0].getChargeAmount());
        assertEquals("King Room , Tax", billItems[0].getDescription());
        assertEquals("2024-01-26", billItems[0].getDate());
        assertEquals("12345", billItems[0].getReference());
    }

    @Test
    public void testTransformOperaCloudResponseNoFolios() {
        GetFolioDetailsResponse operaCloudResponse = convert("/operaCloudFolioResponse.json", GetFolioDetailsResponse.class);
        operaCloudResponse.getReservationFolioInformation().setFolioWindows(Collections.emptyList());
        FolioResponse folioResponse = new FolioResponse();

        FolioResponseTransformer.transformOperaCloudResponse(folioResponse, operaCloudResponse, true);

        assertNull(folioResponse.getFolios());
    }

    @Test
    public void testTransformOperaCloudResponseInvalidWindowNo() {
        GetFolioDetailsResponse operaCloudResponse = convert("/operaCloudFolioResponse.json", GetFolioDetailsResponse.class);
        operaCloudResponse.getReservationFolioInformation().getFolioWindows().get(0).setFolioWindowNo(123);
        FolioResponse folioResponse = new FolioResponse();

        FolioResponseTransformer.transformOperaCloudResponse(folioResponse, operaCloudResponse, true);

        assertNull(folioResponse.getFolios());
    }

    @Test
    public void testTransformOperaCloudResponseNoPostings() {
        GetFolioDetailsResponse operaCloudResponse = convert("/operaCloudFolioResponse.json", GetFolioDetailsResponse.class);
        operaCloudResponse.getReservationFolioInformation().getFolioWindows().get(0).getFolios().get(0).setPostings(Collections.emptyList());
        FolioResponse folioResponse = new FolioResponse();

        FolioResponseTransformer.transformOperaCloudResponse(folioResponse, operaCloudResponse, true);

        assertNull(folioResponse.getFolios()[0].getBillItems());
    }

    @Test
    public void testTransformOperaCloudResponseEmptyCC() {
        GetFolioDetailsResponse operaCloudResponse = convert("/operaCloudFolioResponse.json", GetFolioDetailsResponse.class);
        operaCloudResponse.getReservationFolioInformation().getFolioWindows().get(0).getPaymentMethod().getPaymentCard().setCardNumberMasked("");
        FolioResponse folioResponse = new FolioResponse();

        FolioResponseTransformer.transformOperaCloudResponse(folioResponse, operaCloudResponse, true);

        assertNotNull(folioResponse);
    }
}
