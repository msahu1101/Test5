package com.mgmresorts.booking.room.reservation.search.transformer;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgmresorts.booking.room.reservation.search.BaseUnitTest;
import com.mgmresorts.booking.room.reservation.search.models.BulkFetchResponse;
import com.mgmresorts.booking.room.reservation.search.models.Profile;

import static org.junit.jupiter.api.Assertions.*;

public class BulkFetchResponseTransformerTest extends BaseUnitTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testBulkFetchResponse() throws IOException {

        String responseJson = getFileContents("/reservations.json");

        String transformedResponse = BulkFetchResponseTransformer.getBulkFetchResponse(responseJson);

        BulkFetchResponse[] respArray = mapper.readValue(transformedResponse.getBytes(), BulkFetchResponse[].class);

        assertTrue(respArray.length == 5);

        BulkFetchResponse resv = respArray[0];
        BulkFetchResponse resvNoEmail = respArray[1];

        assertEquals("788697724", resv.getOperaConfirmationNumber());
        assertEquals("001", resv.getHotelCode());
        assertEquals("CHECKED IN", resv.getGuaranteeType());
        assertEquals(0, resv.getGuests());
        assertEquals("NONR", resv.getMarketSegmentCode());
        assertEquals("777919544", resv.getResvNameId());
        assertEquals("INHOUSE", resv.getStatus());
        assertEquals("AR", resv.getRoomType());
        assertEquals(365, resv.getStayLength());
        assertFalse(resv.getMetadata().getEarlyCheckIn().isExternalPurchase());
        assertFalse(resv.getMetadata().getEarlyCheckIn().isMobilePurchase());
        assertNull(resv.getMetadata().getCheckinReversedDate());

        Profile profile = resv.getProfile();
        assertEquals("Keith  (MGMRI)", profile.getFirstName());
        assertEquals("Meister", profile.getLastName());
        assertEquals("Drew.Crittenden@yahoo.com", profile.getEmail());
        assertEquals("77286842", profile.getMlifeNumber());
        assertEquals("BLUE", profile.getTierStatus());

        // If reservation doesn't have email, other profile attributes should be returned
        Profile noEmailProfile = resvNoEmail.getProfile();
        assertTrue(StringUtils.isEmpty(noEmailProfile.getEmail()));
        assertEquals("Dan", noEmailProfile.getFirstName());
        assertEquals("Taylor", noEmailProfile.getLastName());
        assertFalse(resvNoEmail.getMetadata().getEarlyCheckIn().isExternalPurchase());
        assertFalse(resvNoEmail.getMetadata().getEarlyCheckIn().isMobilePurchase());
        assertNull(resvNoEmail.getMetadata().getCheckinReversedDate());
    }

    @Test
    public void testBulkFetchResponse_resprofiletest() throws IOException {

        BulkFetchResponseTransformer.mockPossibleCellphoneTypes(Arrays.asList("MOBILE", "OTHER", "HOME"));

        String responseJson = getFileContents("/reservation_transformertest.json");

        String transformedResponse = BulkFetchResponseTransformer.getBulkFetchResponse(responseJson);

        BulkFetchResponse[] respArray = mapper.readValue(transformedResponse.getBytes(), BulkFetchResponse[].class);

        assertTrue(respArray.length == 60);

        BulkFetchResponse resv = respArray[0];
        BulkFetchResponse resvNoEmail = respArray[1];

        assertEquals("890093219", resv.getOperaConfirmationNumber());
        assertEquals("275", resv.getHotelCode());
        assertEquals("DD", resv.getGuaranteeType());
        assertEquals(1, resv.getGuests());
        assertEquals("TPKG", resv.getMarketSegmentCode());
        assertEquals("883202739", resv.getResvNameId());
        assertEquals("NOSHOW", resv.getStatus());
        assertEquals("SQST", resv.getRoomType());
        assertEquals(4, resv.getStayLength());
        assertEquals("2021-05-31T23:02:39.000Z", resv.getBookingDate().toString());
        assertFalse(resv.getMetadata().getEarlyCheckIn().isExternalPurchase());
        assertFalse(resv.getMetadata().getEarlyCheckIn().isMobilePurchase());
        assertNull(resv.getMetadata().getCheckinReversedDate());


        Profile profile = resv.getProfile();
        assertEquals("Lawrence", profile.getFirstName());
        assertEquals("Edwards", profile.getLastName());
        assertEquals("wade@testautomation.com", profile.getEmail());

        // should this be null?
        assertNull(profile.getMlifeNumber());
        assertNull(profile.getTierStatus());

        // If reservation doesn't have email, other profile attributes should be returned
        Profile noEmailProfile = resvNoEmail.getProfile();
        assertTrue(StringUtils.isEmpty(noEmailProfile.getEmail()));
        assertEquals("Soham", noEmailProfile.getFirstName());
        assertEquals("Barnett", noEmailProfile.getLastName());
        assertFalse(resvNoEmail.getMetadata().getEarlyCheckIn().isExternalPurchase());
        assertFalse(resvNoEmail.getMetadata().getEarlyCheckIn().isMobilePurchase());
        assertEquals("2021-07-26", resvNoEmail.getMetadata().getCheckinReversedDate());

        testMobilePhoneNumbers(respArray);
    }

    private void testMobilePhoneNumbers(BulkFetchResponse[] respArray) {
        Profile mobilePhoneNumberProfile = respArray[0].getProfile();
        assertEquals("225-333-4455", mobilePhoneNumberProfile.getMobilePhone());
        
        Profile noPhoneNumbersProfile = respArray[1].getProfile();
        assertNull(noPhoneNumbersProfile.getMobilePhone());

        Profile onlyHomePhoneNumberProfile = respArray[2].getProfile();
        assertEquals("702-202-8492", onlyHomePhoneNumberProfile.getMobilePhone());

        Profile multiplePhoneNumbersProfile = respArray[3].getProfile();
        assertEquals("123-452-4332", multiplePhoneNumbersProfile.getMobilePhone());

        Profile multiplePhoneNumbersWithoutMobileProfile = respArray[4].getProfile();
        assertEquals("123-452-5232", multiplePhoneNumbersWithoutMobileProfile.getMobilePhone());

        Profile noValidMobilePhoneNumberProfile = respArray[5].getProfile();
        assertNull(noValidMobilePhoneNumberProfile.getMobilePhone());
    }
}
