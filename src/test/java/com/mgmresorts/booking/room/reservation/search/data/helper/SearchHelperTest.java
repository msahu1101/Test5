package com.mgmresorts.booking.room.reservation.search.data.helper;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.dao.helper.SearchHelper;
import com.mgmresorts.booking.room.reservation.search.inject.ApplicationInjector;
import com.mgmresorts.booking.room.reservation.search.util.DateTimeUtil;
import com.mgmresorts.booking.room.reservation.search.util.ServiceConstants;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class SearchHelperTest {

    static AppProperties appProps;

    public SearchHelperTest() throws IOException {

        appProps = new AppProperties();

        final String propertyFilename = "/application-"
                + (StringUtils.isNotBlank(System.getenv(ServiceConstants.APP_PROFILE))
                        ? System.getenv(ServiceConstants.APP_PROFILE)
                        : "local")
                + ".properties";
        final Properties props = new Properties();

        try {
            props.load(ApplicationInjector.class.getResourceAsStream("/application-common.properties"));
            props.load(ApplicationInjector.class.getResourceAsStream(propertyFilename));
        } catch (IOException e) {
            log.debug("Could not load applicaton properties configuration: {}", e);
        }

        props.keySet().forEach(key -> {
            if (key.toString().startsWith("keys")) {
                appProps.getKeys().put(key.toString().replaceFirst("keys.", ""), props.getProperty(key.toString()));
            }
            if (key.toString().startsWith("excludedRoomTypes")) {
                appProps.getExcludedRoomTypes().put(key.toString().replaceFirst("excludedRoomTypes.", ""),
                        props.getProperty(key.toString()));
            }
            if (key.equals("cosmosHost")) {
                appProps.setCosmosHost(props.getProperty(key.toString()));
            }
        });

    }

    @Test
    public void testQueryByOperaConfNumber() {
        String operaConfNumber = "796566664";
        Map<String, String> params = new HashMap<>();
        params.put(OPERA_CONF_NUMBER, operaConfNumber);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN rg IN r.resGuests.resGuest JOIN rr IN rg.reservationReferences.reservationReference"
            + " WHERE (r.reservationID=" + OPERA_CONF_NUMBER_PARAM
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.reservationID=" + OPERA_CONF_NUMBER_PARAM + "))",
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(OPERA_CONF_NUMBER_PARAM, operaConfNumber);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByConfNumber() {
        String confNumberLower = "m03ccc151";
        String confNumberUpper = "M03CCC151";
        Map<String, String> params = new HashMap<>();
        params.put(CONF_NUMBER, confNumberLower);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN rg IN r.resGuests.resGuest JOIN rr IN rg.reservationReferences.reservationReference"
            + " WHERE (r.reservationID=" + CONF_NUMBER_UPPER_PARAM + " or r.reservationID=" + CONF_NUMBER_LOWER_PARAM
            + " or (rr.referenceNumber=" + CONF_NUMBER_UPPER_PARAM + " or rr.referenceNumber=" + CONF_NUMBER_LOWER_PARAM + ")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.reservationID=" + CONF_NUMBER_UPPER_PARAM + "))",
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(CONF_NUMBER_UPPER_PARAM, confNumberUpper);
        expectedParams.put(CONF_NUMBER_LOWER_PARAM, confNumberLower);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByMlifeNumber() {
        String mLife = "7934791";
        Map<String, String> params = new HashMap<>();
        params.put(MLIFE_NUMBER, mLife);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);
        
        assertEquals("SELECT distinct value r FROM ROOT r JOIN m IN r.selectedMemberships.selectedMembership"
            + " WHERE (m.accountID=" + MLIFE_NUMBER_PARAM + " AND m.programCode=\"PC\")",
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(MLIFE_NUMBER_PARAM, mLife);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }
    
    @Test
    public void testQueryByGuestMlifeNumber() {
        String guestMLife = "7934791";
        Map<String, String> params = new HashMap<>();
        params.put(GUEST_MLIFE, guestMLife);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);
        
        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN gm IN p.profile.memberships.membership"
            + " WHERE (gm.accountID=" + GUEST_MLIFE_PARAM + " AND gm.programCode=\"PC\" AND NOT IS_DEFINED(gm.mfInactiveDate))",
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(GUEST_MLIFE_PARAM, guestMLife);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByConfNumberAndHotel() {
        String confNumberUpper = "M03CCC151";
        String confNumberLower = "m03ccc151";
        String hotelCode = "001";
        Map<String, String> params = new HashMap<>();
        params.put(CONF_NUMBER, confNumberUpper);
        params.put(HOTEL_CODE, hotelCode);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN rg IN r.resGuests.resGuest JOIN rr IN rg.reservationReferences.reservationReference"
            + " WHERE (r.reservationID=" + CONF_NUMBER_UPPER_PARAM + " or r.reservationID=" + CONF_NUMBER_LOWER_PARAM
            + " or (rr.referenceNumber=" + CONF_NUMBER_UPPER_PARAM + " or rr.referenceNumber=" + CONF_NUMBER_LOWER_PARAM + ")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.reservationID=" + CONF_NUMBER_UPPER_PARAM + "))"
            + " AND r.hotelReference.hotelCode=" + HOTEL_CODE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(CONF_NUMBER_UPPER_PARAM, confNumberUpper);
        expectedParams.put(CONF_NUMBER_LOWER_PARAM, confNumberLower);
        expectedParams.put(HOTEL_CODE_PARAM, hotelCode);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByOperaConfNumberAndLastName() {
        String operaConfNumber = "796566664";
        String lastName = "Rebecca";
        Map<String, String> params = new HashMap<>();
        params.put(OPERA_CONF_NUMBER, operaConfNumber);
        params.put(LAST_NAME, lastName);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile"
            + " JOIN rg IN r.resGuests.resGuest JOIN rr IN rg.reservationReferences.reservationReference"
            + " WHERE (r.reservationID=" + OPERA_CONF_NUMBER_PARAM
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.reservationID=" + OPERA_CONF_NUMBER_PARAM + ")) AND"
            + " ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=" + LAST_NAME_PARAM + "))",
            querySpec.getQueryText());
        
        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(OPERA_CONF_NUMBER_PARAM, operaConfNumber);
        expectedParams.put(LAST_NAME_PARAM, lastName);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByOperaConfNumberAndHotel() {
        String operaConfNumber = "796566664";
        String hotelCode = "001";
        Map<String, String> params = new HashMap<>();
        params.put(OPERA_CONF_NUMBER, operaConfNumber);
        params.put(HOTEL_CODE, hotelCode);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN rg IN r.resGuests.resGuest JOIN rr IN rg.reservationReferences.reservationReference"
            + " WHERE (r.reservationID=" + OPERA_CONF_NUMBER_PARAM
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.reservationID=" + OPERA_CONF_NUMBER_PARAM + "))"
            + " AND r.hotelReference.hotelCode=" + HOTEL_CODE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(OPERA_CONF_NUMBER_PARAM, operaConfNumber);
        expectedParams.put(HOTEL_CODE_PARAM, hotelCode);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByMlifeAndLastName() {
        String mLife = "7934791";
        String lastName = "Rebecca";
        Map<String, String> params = new HashMap<>();
        params.put(MLIFE_NUMBER, mLife);
        params.put(LAST_NAME, lastName);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN m IN r.selectedMemberships.selectedMembership"
            + " WHERE ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=" + LAST_NAME_PARAM + "))"
            + " AND (m.accountID=" + MLIFE_NUMBER_PARAM + " AND m.programCode=\"PC\")",
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(MLIFE_NUMBER_PARAM, mLife);
        expectedParams.put(LAST_NAME_PARAM, lastName);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByHotelNameAndCheckIn() {
        String hotelCode = "001";
        String firstName = "Brown";
        String lastName = "Rebecca";
        String checkInDate = "2020-01-01";
        Map<String, String> params = new HashMap<>();
        params.put(HOTEL_CODE, hotelCode);
        params.put(FIRST_NAME, firstName);
        params.put(LAST_NAME, lastName);
        params.put(CHECKIN_KEY, checkInDate);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN rs IN r.roomStays.roomStay"
            + " WHERE rs.timeSpan.startTime>=" + CHECKIN_MIN_PARAM + " AND rs.timeSpan.startTime<" + CHECKIN_MAX_PARAM
            + " AND ((p.profile.individualName.nameFirst=" + FIRST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.firstName=" + FIRST_NAME_PARAM + "))"
            + " AND ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=" + LAST_NAME_PARAM + "))"
            + " AND r.hotelReference.hotelCode=" + HOTEL_CODE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(HOTEL_CODE_PARAM, hotelCode);
        expectedParams.put(FIRST_NAME_PARAM, firstName);
        expectedParams.put(LAST_NAME_PARAM, lastName);
        long checkInMin = DateTimeUtil.getEpochMillis(checkInDate);
        long checkInMax = DateTimeUtil.getEpochMillis(checkInDate, 1);
        expectedParams.put(CHECKIN_MIN_PARAM, checkInMin);
        expectedParams.put(CHECKIN_MAX_PARAM, checkInMax);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }
    
    @Test
    public void testQueryByHotelPartialNameAndCheckIn() {
        String hotelCode = "001";
        String firstName = "Bro";
        String lastName = "Reb";
        String checkInDate = "2020-01-01";
        Map<String, String> params = new HashMap<>();
        params.put(HOTEL_CODE, hotelCode);
        params.put(FIRST_NAME, firstName);
        params.put(LAST_NAME, lastName);
        params.put(CHECKIN_KEY, checkInDate);
        params.put(NAME_SEARCH_OPERATION, "STARTSWITH");

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN rs IN r.roomStays.roomStay"
            + " WHERE rs.timeSpan.startTime>=" + CHECKIN_MIN_PARAM + " AND rs.timeSpan.startTime<" + CHECKIN_MAX_PARAM
            + " AND ((STARTSWITH(p.profile.individualName.nameFirst," + FIRST_NAME_PARAM + ",true) AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where STARTSWITH(ag.firstName," + FIRST_NAME_PARAM + ",true)))"
            + " AND ((STARTSWITH(p.profile.individualName.nameSur," + LAST_NAME_PARAM + ",true) AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where STARTSWITH(ag.lastName," + LAST_NAME_PARAM + ",true))) "
            + "AND r.hotelReference.hotelCode=" + HOTEL_CODE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(HOTEL_CODE_PARAM, hotelCode);
        expectedParams.put(FIRST_NAME_PARAM, firstName);
        expectedParams.put(LAST_NAME_PARAM, lastName);
        long checkInMin = DateTimeUtil.getEpochMillis(checkInDate);
        long checkInMax = DateTimeUtil.getEpochMillis(checkInDate, 1);
        expectedParams.put(CHECKIN_MIN_PARAM, checkInMin);
        expectedParams.put(CHECKIN_MAX_PARAM, checkInMax);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByHotelNameAndStay() {
        String hotelCode = "001";
        String firstName = "Brown";
        String lastName = "Rebecca";
        String checkInDate = "2020-01-01";
        String checkOutDate = "2020-01-03";
        Map<String, String> params = new HashMap<>();
        params.put(HOTEL_CODE, hotelCode);
        params.put(FIRST_NAME, firstName);
        params.put(LAST_NAME, lastName);
        params.put(CHECKIN_KEY, checkInDate);
        params.put(CHECKOUT_KEY, checkOutDate);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN rs IN r.roomStays.roomStay JOIN rg IN r.resGuests.resGuest"
            + " WHERE rs.timeSpan.startTime>=" + CHECKIN_MIN_PARAM + " AND rs.timeSpan.startTime<" + CHECKIN_MAX_PARAM
            + " AND rg.reservationID=r.reservationID AND rg.departureTime>=" + CHECKOUT_MIN_PARAM + " AND rg.departureTime<" + CHECKOUT_MAX_PARAM
            + " AND ((p.profile.individualName.nameFirst=" + FIRST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.firstName=" + FIRST_NAME_PARAM + "))"
            + " AND ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=" + LAST_NAME_PARAM + "))"
            + " AND r.hotelReference.hotelCode=" + HOTEL_CODE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(HOTEL_CODE_PARAM, hotelCode);
        expectedParams.put(FIRST_NAME_PARAM, firstName);
        expectedParams.put(LAST_NAME_PARAM, lastName);
        long checkInMin = DateTimeUtil.getEpochMillis(checkInDate);
        long checkInMax = DateTimeUtil.getEpochMillis(checkInDate, 1);
        expectedParams.put(CHECKIN_MIN_PARAM, checkInMin);
        expectedParams.put(CHECKIN_MAX_PARAM, checkInMax);
        long checkOutMin = DateTimeUtil.getEpochMillis(checkOutDate);
        long checkOutMax = DateTimeUtil.getEpochMillis(checkOutDate, 1);
        expectedParams.put(CHECKOUT_MIN_PARAM, checkOutMin);
        expectedParams.put(CHECKOUT_MAX_PARAM, checkOutMax);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByAllParams() {
        String hotelCode = "001";
        String firstName = "Brown";
        String lastName = "Rebecca";
        String checkInDate = "2020-01-01";
        String checkOutDate = "2020-01-03";
        String mLife = "7934791";
        String mgmId = "sdfkkjasdfkhjk";
        String email = "test@random.com";
        String startDate = "2020-01-01";
        String endDate = "2020-12-01";
        Map<String, String> params = new HashMap<>();
        params.put(HOTEL_CODE, hotelCode);
        params.put(FIRST_NAME, firstName);
        params.put(LAST_NAME, lastName);
        params.put(CHECKIN_KEY, checkInDate);
        params.put(CHECKOUT_KEY, checkOutDate);
        params.put(MLIFE_NUMBER, mLife);
        params.put(MGMID, mgmId);
        params.put(EMAIL, email);
        params.put(START_DATE, startDate);
        params.put(END_DATE, endDate);
        params.put(RESV_STATUS, "RESERVED, INHOUSE");

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN rs IN r.roomStays.roomStay"
            + " JOIN rg IN r.resGuests.resGuest JOIN em IN p.profile.electronicAddresses.electronicAddress"
            + " WHERE rs.timeSpan.startTime>=" + CHECKIN_MIN_PARAM + " AND rs.timeSpan.startTime<" + CHECKIN_MAX_PARAM
            + " AND rg.reservationID=r.reservationID AND rg.departureTime>=" + CHECKOUT_MIN_PARAM + " AND rg.departureTime<" + CHECKOUT_MAX_PARAM
            + " AND ((p.profile.individualName.nameFirst=" + FIRST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.firstName=" + FIRST_NAME_PARAM + "))"
            + " AND ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=" + LAST_NAME_PARAM + "))"
            + " AND (r.mgmProfile.mgmId=" + MGMID_PARAM
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.mgmId=" + MGMID_PARAM + ")"
            + " or EXISTS(select value m from m in r.selectedMemberships.selectedMembership"
            + " where m.accountID=" + MLIFE_NUMBER_PARAM + " AND m.programCode=\"PC\"))"
            + " AND rs.timeSpan.startTime>=" + START_DATE_PARAM + " AND rs.timeSpan.startTime<=" + END_DATE_PARAM
            + " AND ARRAY_CONTAINS(" + RESV_STATUS_PARAM + ", rs.reservationStatusType)"
            + " AND em.eaddress=@eaddress AND em.mfPrimaryYN=@mfPrimaryYN"
            + " AND r.hotelReference.hotelCode=" + HOTEL_CODE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(HOTEL_CODE_PARAM, hotelCode);
        expectedParams.put(FIRST_NAME_PARAM, firstName);
        expectedParams.put(LAST_NAME_PARAM, lastName);
        long checkInMin = DateTimeUtil.getEpochMillis(checkInDate);
        long checkInMax = DateTimeUtil.getEpochMillis(checkInDate, 1);
        expectedParams.put(CHECKIN_MIN_PARAM, checkInMin);
        expectedParams.put(CHECKIN_MAX_PARAM, checkInMax);
        long checkOutMin = DateTimeUtil.getEpochMillis(checkOutDate);
        long checkOutMax = DateTimeUtil.getEpochMillis(checkOutDate, 1);
        expectedParams.put(CHECKOUT_MIN_PARAM, checkOutMin);
        expectedParams.put(CHECKOUT_MAX_PARAM, checkOutMax);
        expectedParams.put(MLIFE_NUMBER_PARAM, mLife);
        expectedParams.put(MGMID_PARAM, mgmId);
        expectedParams.put("@eaddress", email);
        expectedParams.put("@mfPrimaryYN", "Y");
        long startDateTicks = DateTimeUtil.getEpochMillis(startDate);
        expectedParams.put(START_DATE_PARAM, startDateTicks);
        long endDateTicks = DateTimeUtil.getEpochMillis(endDate, 1) - 1;
        expectedParams.put(END_DATE_PARAM, endDateTicks);
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode resvStatusArr = mapper.createArrayNode();
        resvStatusArr.add("RESERVED");
        resvStatusArr.add("INHOUSE");
        expectedParams.put(RESV_STATUS_PARAM, resvStatusArr);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByName() {
        String firstName = "Brown";
        Map<String, String> params = new HashMap<>();
        params.put(FIRST_NAME, firstName);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile"
            + " WHERE ((p.profile.individualName.nameFirst=" + FIRST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.firstName=" + FIRST_NAME_PARAM + "))",
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(FIRST_NAME_PARAM, firstName);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByMgmId() {
        String mgmId = "sdfkkjasdfkhjk";
        Map<String, String> params = new HashMap<>();
        params.put(MGMID, mgmId);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r WHERE (r.mgmProfile.mgmId=" + MGMID_PARAM
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.mgmId=" + MGMID_PARAM + "))",
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(MGMID_PARAM, mgmId);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByMgmIdOrMlife() {
        String mgmId = "sdfkkjasdfkhjk";
        String mLife = "7934791";
        Map<String, String> params = new HashMap<>();
        params.put(MGMID, mgmId);
        params.put(MLIFE_NUMBER, mLife);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r WHERE (r.mgmProfile.mgmId=" + MGMID_PARAM
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.mgmId=" + MGMID_PARAM + ")"
            + " or EXISTS(select value m from m in r.selectedMemberships.selectedMembership"
            + " where m.accountID=" + MLIFE_NUMBER_PARAM + " AND m.programCode=\"PC\"))",
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(MGMID_PARAM, mgmId);
        expectedParams.put(MLIFE_NUMBER_PARAM, mLife);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByMgmIdOrMlifeWithDateRange() {
        String mgmId = "sdfkkjasdfkhjk";
        String mLife = "7934791";
        String startDate = "2020-01-01";
        String endDate = "2020-12-01";
        Map<String, String> params = new HashMap<>();
        params.put(MGMID, mgmId);
        params.put(MLIFE_NUMBER, mLife);
        params.put(START_DATE, startDate);
        params.put(END_DATE, endDate);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN rs IN r.roomStays.roomStay WHERE (r.mgmProfile.mgmId=" + MGMID_PARAM
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.mgmId=" + MGMID_PARAM + ")"
            + " or EXISTS(select value m from m in r.selectedMemberships.selectedMembership"
            + " where m.accountID=" + MLIFE_NUMBER_PARAM + " AND m.programCode=\"PC\"))"
            + " AND rs.timeSpan.startTime>=" + START_DATE_PARAM + " AND rs.timeSpan.startTime<=" + END_DATE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(MGMID_PARAM, mgmId);
        expectedParams.put(MLIFE_NUMBER_PARAM, mLife);
        long startDateTicks = DateTimeUtil.getEpochMillis(startDate);
        expectedParams.put(START_DATE_PARAM, startDateTicks);
        long endDateTicks = DateTimeUtil.getEpochMillis(endDate, 1) - 1;
        expectedParams.put(END_DATE_PARAM, endDateTicks);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByMgmIdOrMlifeWithStartDate() {
        String mgmId = "sdfkkjasdfkhjk";
        String mLife = "7934791";
        String startDate = "2020-01-01";
        Map<String, String> params = new HashMap<>();
        params.put(MGMID, mgmId);
        params.put(MLIFE_NUMBER, mLife);
        params.put(START_DATE, startDate);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN rs IN r.roomStays.roomStay WHERE (r.mgmProfile.mgmId=" + MGMID_PARAM
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.mgmId=" + MGMID_PARAM + ")"
            + " or EXISTS(select value m from m in r.selectedMemberships.selectedMembership"
            + " where m.accountID=" + MLIFE_NUMBER_PARAM + " AND m.programCode=\"PC\"))"
            + " AND rs.timeSpan.startTime>=" + START_DATE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(MGMID_PARAM, mgmId);
        expectedParams.put(MLIFE_NUMBER_PARAM, mLife);
        long startDateTicks = DateTimeUtil.getEpochMillis(startDate);
        expectedParams.put(START_DATE_PARAM, startDateTicks);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByMgmIdOrMlifeWithEndRange() {
        String mgmId = "sdfkkjasdfkhjk";
        String mLife = "7934791";
        String endDate = "2020-12-01";
        Map<String, String> params = new HashMap<>();
        params.put(MGMID, mgmId);
        params.put(MLIFE_NUMBER, mLife);
        params.put(END_DATE, endDate);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r " + "JOIN rs IN r.roomStays.roomStay WHERE (r.mgmProfile.mgmId=" + MGMID_PARAM
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.mgmId=" + MGMID_PARAM + ")"
            + " or EXISTS(select value m from m in r.selectedMemberships.selectedMembership"
            + " where m.accountID=" + MLIFE_NUMBER_PARAM + " AND m.programCode=\"PC\"))"
            + " AND rs.timeSpan.startTime<=" + END_DATE_PARAM,
             querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(MGMID_PARAM, mgmId);
        expectedParams.put(MLIFE_NUMBER_PARAM, mLife);
        long endDateTicks = DateTimeUtil.getEpochMillis(endDate, 1) - 1;
        expectedParams.put(END_DATE_PARAM, endDateTicks);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByMgmIdOrMlifeWithResvStatus() {
        String mgmId = "sdfkkjasdfkhjk";
        String mLife = "7934791";
        Map<String, String> params = new HashMap<>();
        params.put(MGMID, mgmId);
        params.put(MLIFE_NUMBER, mLife);
        params.put(RESV_STATUS, "reserved");

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN rs IN r.roomStays.roomStay WHERE (r.mgmProfile.mgmId=" + MGMID_PARAM
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.mgmId=" + MGMID_PARAM + ")"
            + " or EXISTS(select value m from m in r.selectedMemberships.selectedMembership"
            + " where m.accountID=" + MLIFE_NUMBER_PARAM + " AND m.programCode=\"PC\"))"
            + " AND ARRAY_CONTAINS(" + RESV_STATUS_PARAM + ", rs.reservationStatusType)",
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(MGMID_PARAM, mgmId);
        expectedParams.put(MLIFE_NUMBER_PARAM, mLife);
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode resvStatusArr = mapper.createArrayNode();
        resvStatusArr.add("RESERVED");
        expectedParams.put(RESV_STATUS_PARAM, resvStatusArr);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryByMgmIdOrMlifeWithResvStatusMultiple() {
        String mgmId = "sdfkkjasdfkhjk";
        String mLife = "7934791";
        Map<String, String> params = new HashMap<>();
        params.put(MGMID, mgmId);
        params.put(MLIFE_NUMBER, mLife);
        params.put(RESV_STATUS, "reserved, inhouse");

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN rs IN r.roomStays.roomStay WHERE (r.mgmProfile.mgmId=" + MGMID_PARAM
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.mgmId=" + MGMID_PARAM + ")"
            + " or EXISTS(select value m from m in r.selectedMemberships.selectedMembership"
            + " where m.accountID=" + MLIFE_NUMBER_PARAM + " AND m.programCode=\"PC\"))"
            + " AND ARRAY_CONTAINS(" + RESV_STATUS_PARAM + ", rs.reservationStatusType)",
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(MGMID_PARAM, mgmId);
        expectedParams.put(MLIFE_NUMBER_PARAM, mLife);
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode resvStatusArr = mapper.createArrayNode();
        resvStatusArr.add("RESERVED");
        resvStatusArr.add("INHOUSE");
        expectedParams.put(RESV_STATUS_PARAM, resvStatusArr);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryById() {
        Map<String, String> params = new HashMap<>();
        params.put("operaConfirmationNumbers", "123,456,789");

        SqlQuerySpec querySpec = SearchHelper.createSearchByIdsQuerySpec(params);

        assertEquals("SELECT value r FROM ROOT r WHERE ARRAY_CONTAINS(@idsArr, r.reservationID)", querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode resvStatusArr = mapper.createArrayNode();
        resvStatusArr.add("123");
        resvStatusArr.add("456");
        resvStatusArr.add("789");
        expectedParams.put("@idsArr", resvStatusArr);

        assertQueryParams(expectedParams, querySpec.getParameters());

        params = new HashMap<>();
        params.put("resvNameIds", "123,456,789");

        querySpec = SearchHelper.createSearchByIdsQuerySpec(params);

        assertEquals("SELECT value r FROM ROOT r WHERE ARRAY_CONTAINS(@idsArr, r.resvNameId)", querySpec.getQueryText());

        assertQueryParams(expectedParams, querySpec.getParameters());

        params = new HashMap<>();
        params.put("ids", "123,456,789");

        querySpec = SearchHelper.createSearchByIdsQuerySpec(params);

        assertEquals("SELECT value r FROM ROOT r WHERE ARRAY_CONTAINS(@idsArr, r.id)", querySpec.getQueryText());

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryForCheckOutLookup() {
        String lastName = "Test";
        String checkInDate = "2020-12-01";
        String hotelCode = "001";
        String roomNumber = "9233";
        Map<String, String> params = new HashMap<>();
        params.put(LAST_NAME, lastName);
        params.put(CHECKIN_KEY, checkInDate);
        params.put(HOTEL_CODE, hotelCode);
        params.put(ROOM_NUMBER, roomNumber);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN rs IN r.roomStays.roomStay"
            + " WHERE rs.timeSpan.startTime>=" + CHECKIN_MIN_PARAM + " AND rs.timeSpan.startTime<" + CHECKIN_MAX_PARAM
            + " AND ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=" + LAST_NAME_PARAM + "))"
            + " AND rs.roomID=" + ROOM_NUMBER_PARAM
            + " AND r.hotelReference.hotelCode=" + HOTEL_CODE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(LAST_NAME_PARAM, lastName);
        long checkInMin = DateTimeUtil.getEpochMillis(checkInDate);
        long checkInMax = DateTimeUtil.getEpochMillis(checkInDate, 1);
        expectedParams.put(CHECKIN_MIN_PARAM, checkInMin);
        expectedParams.put(CHECKIN_MAX_PARAM, checkInMax);
        expectedParams.put(HOTEL_CODE_PARAM, hotelCode);
        expectedParams.put(ROOM_NUMBER_PARAM, roomNumber);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }
    
    @Test
    public void testQueryForCheckOutLookupWithLast4DigitsCc() {
        String lastName = "Test";
        String checkInDate = "2020-12-01";
        String roomNumber = "9233";
        String ccLast4Digits = "4444";
        Map<String, String> params = new HashMap<>();
        params.put(LAST_NAME, lastName);
        params.put(CHECKIN_KEY, checkInDate);
        params.put(ROOM_NUMBER, roomNumber);
        params.put(LAST_4_DIGITS_CC, ccLast4Digits);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN rs IN r.roomStays.roomStay JOIN cc IN r.resCreditCards.resCreditCard"
            + " WHERE rs.timeSpan.startTime>=" + CHECKIN_MIN_PARAM + " AND rs.timeSpan.startTime<" + CHECKIN_MAX_PARAM
            + " AND ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=" + LAST_NAME_PARAM + "))"
            + " AND rs.roomID=" + ROOM_NUMBER_PARAM
            + " AND cc.creditCard.mfPrimaryYN=\"Y\" AND ENDSWITH(cc.creditCard.creditCardNumber, " + LAST_4_DIGITS_CC_PARAM + ")",
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(LAST_NAME_PARAM, lastName);
        long checkInMin = DateTimeUtil.getEpochMillis(checkInDate);
        long checkInMax = DateTimeUtil.getEpochMillis(checkInDate, 1);
        expectedParams.put(CHECKIN_MIN_PARAM, checkInMin);
        expectedParams.put(CHECKIN_MAX_PARAM, checkInMax);
        expectedParams.put(ROOM_NUMBER_PARAM, roomNumber);
        expectedParams.put(LAST_4_DIGITS_CC_PARAM, ccLast4Digits);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }
    
    @Test
    public void testQueryForCheckInDateRangeLookup() {
        String hotelCode = "001";
        String firstName = "Brown";
        String lastName = "Rebecca";
        String startDate = "2020-01-01";
        String endDate = "2020-01-04";
        Map<String, String> params = new HashMap<>();
        params.put(HOTEL_CODE, hotelCode);
        params.put(FIRST_NAME, firstName);
        params.put(LAST_NAME, lastName);
        params.put(START_DATE, startDate);
        params.put(END_DATE, endDate);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN rs IN r.roomStays.roomStay"
            + " WHERE ((p.profile.individualName.nameFirst=" + FIRST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.firstName=" + FIRST_NAME_PARAM + "))"
            + " AND ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=" + LAST_NAME_PARAM + "))"
            + " AND rs.timeSpan.startTime>=" + START_DATE_PARAM + " AND rs.timeSpan.startTime<=" + END_DATE_PARAM
            + " AND r.hotelReference.hotelCode=" + HOTEL_CODE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(HOTEL_CODE_PARAM, hotelCode);
        expectedParams.put(FIRST_NAME_PARAM, firstName);
        expectedParams.put(LAST_NAME_PARAM, lastName);
        long startDateTicks = DateTimeUtil.getEpochMillis(startDate);
        expectedParams.put(START_DATE_PARAM, startDateTicks);
        long endDateTicks = DateTimeUtil.getEpochMillis(endDate, 1) - 1;
        expectedParams.put(END_DATE_PARAM, endDateTicks);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }
    
    @Test
    public void testQueryForOperaProfileId() {
        String operaProfileId = "123456";
        Map<String, String> params = new HashMap<>();
        params.put(OPERA_PROFILE_ID, operaProfileId);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile WHERE"
            + " p.profile.mfResortProfileID=" + OPERA_PROFILE_ID_PARAM + " AND p.profile.profileType=\"GUEST\"",
            querySpec.getQueryText());
        
        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(OPERA_PROFILE_ID_PARAM, operaProfileId);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryForNameMatch() {
        String hotelCode = "001";
        String firstName = "Brown";
        String lastName = "Rebecca";
        String startDate = "2021-08-16";
        String endDate = "2023-02-16";
        Map<String, String> params = new HashMap<>();
        params.put(HOTEL_CODE, hotelCode);
        params.put(FIRST_NAME, firstName);
        params.put(LAST_NAME, lastName);
        params.put(START_DATE, startDate);
        params.put(END_DATE, endDate);
        params.put(FIRST_NAME_MATCH, "LIKE");
        params.put(LAST_NAME_MATCH, "FULL");

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN rs IN r.roomStays.roomStay"
            + " WHERE ((CONTAINS(p.profile.individualName.nameFirst," + FIRST_NAME_PARAM + ",true) AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where CONTAINS(ag.firstName," + FIRST_NAME_PARAM + ",true)))"
            + " AND ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=" + LAST_NAME_PARAM + "))"
            + " AND rs.timeSpan.startTime>=" + START_DATE_PARAM + " AND rs.timeSpan.startTime<=" + END_DATE_PARAM
            + " AND r.hotelReference.hotelCode=" + HOTEL_CODE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(HOTEL_CODE_PARAM, hotelCode);
        expectedParams.put(FIRST_NAME_PARAM, firstName);
        expectedParams.put(LAST_NAME_PARAM, lastName);
        long startDateTicks = DateTimeUtil.getEpochMillis(startDate);
        expectedParams.put(START_DATE_PARAM, startDateTicks);
        long endDateTicks = DateTimeUtil.getEpochMillis(endDate, 1) - 1;
        expectedParams.put(END_DATE_PARAM, endDateTicks);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryForMarketCodes() {
        String hotelCode = "001";
        String firstName = "Brown";
        String lastName = "Rebecca";
        String startDate = "2021-08-16";
        String endDate = "2023-02-16";
        Map<String, String> params = new HashMap<>();
        params.put(HOTEL_CODE, hotelCode);
        params.put(FIRST_NAME, firstName);
        params.put(LAST_NAME, lastName);
        params.put(START_DATE, startDate);
        params.put(END_DATE, endDate);
        params.put(MARKET_CODES, "TFIT,TGDS,TPKG,TENT,TEXE,TCOR,TPAR,TSMG");

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN rs IN r.roomStays.roomStay"
            + " WHERE ((p.profile.individualName.nameFirst=" + FIRST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.firstName=" + FIRST_NAME_PARAM + "))"
            + " AND ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=" + LAST_NAME_PARAM + "))"
            + " AND rs.timeSpan.startTime>=" + START_DATE_PARAM + " AND rs.timeSpan.startTime<=" + END_DATE_PARAM
            + " AND ARRAY_CONTAINS(" + MARKET_CODES_PARAM + ", rs.marketSegmentCode)"
            + " AND r.hotelReference.hotelCode=" + HOTEL_CODE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(HOTEL_CODE_PARAM, hotelCode);
        expectedParams.put(FIRST_NAME_PARAM, firstName);
        expectedParams.put(LAST_NAME_PARAM, lastName);
        long startDateTicks = DateTimeUtil.getEpochMillis(startDate);
        expectedParams.put(START_DATE_PARAM, startDateTicks);
        long endDateTicks = DateTimeUtil.getEpochMillis(endDate, 1) - 1;
        expectedParams.put(END_DATE_PARAM, endDateTicks);
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode marketCodesArr = mapper.createArrayNode();
        marketCodesArr.add("TFIT");
        marketCodesArr.add("TGDS");
        marketCodesArr.add("TPKG");
        marketCodesArr.add("TENT");
        marketCodesArr.add("TEXE");
        marketCodesArr.add("TCOR");
        marketCodesArr.add("TPAR");
        marketCodesArr.add("TSMG");
        expectedParams.put(MARKET_CODES_PARAM, marketCodesArr);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryForRoomType() {
        String hotelCode = "001";
        String firstName = "Brown";
        String lastName = "Rebecca";
        String startDate = "2021-08-16";
        String endDate = "2023-02-16";
        String roomType = "DGQQ";
        Map<String, String> params = new HashMap<>();
        params.put(HOTEL_CODE, hotelCode);
        params.put(FIRST_NAME, firstName);
        params.put(LAST_NAME, lastName);
        params.put(START_DATE, startDate);
        params.put(END_DATE, endDate);
        params.put(ROOM_TYPE, roomType);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN rs IN r.roomStays.roomStay"
            + " WHERE ((p.profile.individualName.nameFirst=" + FIRST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.firstName=" + FIRST_NAME_PARAM + "))"
            + " AND ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=" + LAST_NAME_PARAM + "))"
            + " AND rs.timeSpan.startTime>=" + START_DATE_PARAM + " AND rs.timeSpan.startTime<=" + END_DATE_PARAM
            + " AND rs.roomInventoryCode=" + ROOM_TYPE_PARAM
            + " AND r.hotelReference.hotelCode=" + HOTEL_CODE_PARAM,
            querySpec.getQueryText());

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(HOTEL_CODE_PARAM, hotelCode);
        expectedParams.put(FIRST_NAME_PARAM, firstName);
        expectedParams.put(LAST_NAME_PARAM, lastName);
        long startDateTicks = DateTimeUtil.getEpochMillis(startDate);
        expectedParams.put(START_DATE_PARAM, startDateTicks);
        long endDateTicks = DateTimeUtil.getEpochMillis(endDate, 1) - 1;
        expectedParams.put(END_DATE_PARAM, endDateTicks);
        expectedParams.put(ROOM_TYPE_PARAM, roomType);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }

    @Test
    public void testQueryForBookingDate() {
        String hotelCode = "001";
        String firstName = "Brown";
        String lastName = "Rebecca";
        String startDate = "2021-08-16";
        String endDate = "2023-02-16";
        String bookingDate = "2022-03-30";
        Map<String, String> params = new HashMap<>();
        params.put(HOTEL_CODE, hotelCode);
        params.put(FIRST_NAME, firstName);
        params.put(LAST_NAME, lastName);
        params.put(START_DATE, startDate);
        params.put(END_DATE, endDate);
        params.put(BOOKING_DATE, bookingDate);

        SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

        assertEquals("SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN rs IN r.roomStays.roomStay"
            + " WHERE ((p.profile.individualName.nameFirst=" + FIRST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.firstName=" + FIRST_NAME_PARAM + "))"
            + " AND ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM + " AND p.profile.profileType=\"GUEST\")"
            + " or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=" + LAST_NAME_PARAM + "))"
            + " AND rs.timeSpan.startTime>=" + START_DATE_PARAM + " AND rs.timeSpan.startTime<=" + END_DATE_PARAM
            + " AND r.originalBookingDate>=" + BOOKING_DATE_START_PARAM + " AND r.originalBookingDate<=" + BOOKING_DATE_END_PARAM
            + " AND r.hotelReference.hotelCode=" + HOTEL_CODE_PARAM,
            querySpec.getQueryText());


        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put(HOTEL_CODE_PARAM, hotelCode);
        expectedParams.put(FIRST_NAME_PARAM, firstName);
        expectedParams.put(LAST_NAME_PARAM, lastName);
        long startDateTicks = DateTimeUtil.getEpochMillis(startDate);
        expectedParams.put(START_DATE_PARAM, startDateTicks);
        long endDateTicks = DateTimeUtil.getEpochMillis(endDate, 1) - 1;
        expectedParams.put(END_DATE_PARAM, endDateTicks);
        long bookingDateStartTicks = DateTimeUtil.getEpochMillis(bookingDate);
        expectedParams.put(BOOKING_DATE_START_PARAM, bookingDateStartTicks);
        long bookingDateEndTicks = DateTimeUtil.getEpochMillis(bookingDate, 1);
        expectedParams.put(BOOKING_DATE_END_PARAM, bookingDateEndTicks);

        assertQueryParams(expectedParams, querySpec.getParameters());
    }
    
	@Test
	public void testCreateInHouseQuerySpec() {
		String hotelCode = "001";
		String roomNumber = "1234";
		Map<String, String> params = new HashMap<>();
		params.put(HOTEL_CODE, hotelCode);
		params.put(ROOM_NUMBER, roomNumber);

		SqlQuerySpec sqlQuerySpec = SearchHelper.createInHouseSearchQuerySpec(params);

		assertEquals(
				"SELECT distinct value r FROM ROOT r JOIN rs IN r.roomStays.roomStay WHERE r.hotelReference.hotelCode = "
						+ HOTEL_CODE_PARAM + " AND rs.roomID = " + ROOM_NUMBER_PARAM
						+ " AND rs.reservationStatusType = \"INHOUSE\"",
				sqlQuerySpec.getQueryText());

		Map<String, Object> expectedParams = new HashMap<>();
		expectedParams.put(HOTEL_CODE_PARAM, hotelCode);
		expectedParams.put(ROOM_NUMBER_PARAM, roomNumber);

		assertQueryParams(expectedParams, sqlQuerySpec.getParameters());
	}
	
	@Test
	public void testQueryForConfNumberAndLastName() {
		String confNumber = "9876543120";
		String lastName = "Rebecca";
		Map<String, String> params = new HashMap<>();
		params.put(CONF_NUMBER, confNumber);
		params.put(LAST_NAME, lastName);

		SqlQuerySpec sqlQuerySpec = SearchHelper.createSearchQuerySpec(params, appProps);

		assertEquals(
				"SELECT distinct value r FROM ROOT r JOIN p IN r.resProfiles.resProfile JOIN rg IN r.resGuests.resGuest JOIN rr IN rg.reservationReferences.reservationReference WHERE (r.reservationID="
						+ CONF_NUMBER_UPPER_PARAM + " or r.reservationID=" + CONF_NUMBER_LOWER_PARAM
						+ " or (rr.referenceNumber=" + CONF_NUMBER_UPPER_PARAM + " or rr.referenceNumber="
						+ CONF_NUMBER_LOWER_PARAM
						+ ") or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.reservationID="
						+ CONF_NUMBER_UPPER_PARAM + " AND ag.lastName=" + LAST_NAME_PARAM
						+ ")) AND ((p.profile.individualName.nameSur=" + LAST_NAME_PARAM
						+ " AND p.profile.profileType=\"GUEST\") or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.reservationID="
						+ CONF_NUMBER_UPPER_PARAM + " AND ag.lastName=" + LAST_NAME_PARAM + "))",
				sqlQuerySpec.getQueryText());

		Map<String, Object> expectedParams = new HashMap<>();
		expectedParams.put(CONF_NUMBER_LOWER_PARAM, confNumber);
		expectedParams.put(CONF_NUMBER_UPPER_PARAM, confNumber);
		expectedParams.put(LAST_NAME_PARAM, lastName);

		assertQueryParams(expectedParams, sqlQuerySpec.getParameters());
	}

    private void assertQueryParams(Map<String, Object> expectedParams, List<SqlParameter> actualParams) {

        actualParams.forEach(param -> {
            String paramName = param.getName();
            assertTrue(expectedParams.containsKey(paramName));
            Object expectedValue = expectedParams.get(paramName);
            assertEquals(expectedValue, param.getValue(Object.class));
        });
    }
}
