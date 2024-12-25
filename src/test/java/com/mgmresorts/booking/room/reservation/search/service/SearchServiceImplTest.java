package com.mgmresorts.booking.room.reservation.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mgmresorts.booking.common.error.exception.DataNotFoundException;
import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.oxi.models.ReservationStatusType;
import com.mgmresorts.booking.room.reservation.search.BaseUnitTest;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.dao.DocumentDao;
import com.mgmresorts.booking.room.reservation.search.dao.DocumentDaoImpl;
import com.mgmresorts.booking.room.reservation.search.models.BasicReservationProfile;
import com.mgmresorts.booking.room.reservation.search.models.BlockPartnerAccountSettings;
import com.mgmresorts.booking.room.reservation.search.models.BlockProfileIdSettings;
import com.mgmresorts.booking.room.reservation.search.response.ResponseWithHeaders;
import com.mgmresorts.booking.room.reservation.search.util.CommonUtil;
import com.mgmresorts.booking.room.reservation.search.util.ServiceConstants;
import com.mgmresorts.booking.room.reservation.search.util.ZonedDateTimeProvider;

class SearchServiceImplTest extends BaseUnitTest{

	private DocumentDao documentDao;
	private SearchServiceImpl searchService;
	private AppProperties appProperties;
	private ZonedDateTimeProvider zonedDateTimeProvider;

	@BeforeEach
	void setup() {

		documentDao = mock(DocumentDaoImpl.class);
		appProperties = mock(AppProperties.class);
		zonedDateTimeProvider = mock(ZonedDateTimeProvider.class);
		searchService = new SearchServiceImpl();
		searchService.setDocumentDao(documentDao);
		searchService.setAppProperties(appProperties);
		searchService.setZonedDateTimeProvider(zonedDateTimeProvider);
		when(appProperties.getBlockProfileIdSettings()).thenReturn(getBlockProfileIdSettings());
		when(appProperties.getBlockPartnerAccountSettings()).thenReturn(getBlockPartnerAccountSettings());
		when(appProperties.getPropertyTimezones()).thenReturn(getPropertyTimezones());
		when(appProperties.getPropertyDateRollShiftFromMidnightInMinutes()).thenReturn(getPropertyDateRollShiftFromMidnightInMinutes());
		when(appProperties.getTcolvChannelWhitelist()).thenReturn(Arrays.asList("ice", "web", "unknown"));
		ZoneId zoneId = ZoneId.of(ServiceConstants.PACIFIC_TIME_ZONE);
		when(zonedDateTimeProvider.now(zoneId)).thenReturn(ZonedDateTime.now(zoneId));
	}

	@Test
	void searchReservationsTest() throws JsonProcessingException {

		Reservation reservation = convert("/reservation.json", Reservation.class);
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);

        String resultWithBasicInfo = searchService.searchReservations(Collections.emptyMap(), true);

		assertNotNull(resultWithBasicInfo);

        String resultWithoutBasicInfo = searchService.searchReservations(Collections.emptyMap(), false);

		assertNotNull(resultWithoutBasicInfo);
		assertEquals(1, CommonUtil.convertToReservationList(resultWithoutBasicInfo).length);
		assertTrue(CommonUtil.convertToReservationList(resultWithoutBasicInfo)[0] instanceof Reservation);
	}
	
	@Test
	void searchReservationUpcoming() throws DatatypeConfigurationException {

		Reservation reservation = convert("/reservation.json", Reservation.class);
		ZoneId zoneId = ZoneId.of(ServiceConstants.PACIFIC_TIME_ZONE);
		XMLGregorianCalendar startTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(
				ZonedDateTime.now(zoneId).plusDays(ServiceConstants.UPCOMING_DAYS_WINDOW - 1).toLocalDate().toString());
		reservation.getStayDateRange().setStartTime(startTime);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));
		when(zonedDateTimeProvider.now(zoneId)).thenReturn(ZonedDateTime.now(zoneId));

		String resultWithBasicInfo = searchService.searchReservations(Collections.emptyMap(), true);
		
		assertNotNull(resultWithBasicInfo);
		assertTrue(resultWithBasicInfo.contains("\"upcoming\":true"));
	}

	@Test
	void searchReservationUpcomingThreeDaysPriorAndBeforeDateRoll() throws DatatypeConfigurationException {

		Reservation reservation = convert("/reservation.json", Reservation.class);
		XMLGregorianCalendar startTime = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(LocalDate.of(2020, 06, 06).toString());
		reservation.getStayDateRange().setStartTime(startTime);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));
		ZoneId zoneId = ZoneId.of(ServiceConstants.PACIFIC_TIME_ZONE);
		when(zonedDateTimeProvider.now(zoneId)).thenReturn(ZonedDateTime.of(2020, 06, 03, 00, 30, 0, 0, zoneId));

        String resultWithBasicInfo = searchService.searchReservations(Collections.emptyMap(), true);
		
		assertNotNull(resultWithBasicInfo);
		assertTrue(resultWithBasicInfo.contains("\"upcoming\":true"));
	}

	@Test
	void searchReservationUpcomingLastDayAndBeforeDateRoll() throws DatatypeConfigurationException {

		Reservation reservation = convert("/reservation.json", Reservation.class);
		XMLGregorianCalendar startTime = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(LocalDate.of(2020, 06, 06).toString());
		reservation.getStayDateRange().setStartTime(startTime);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));
		ZoneId zoneId = ZoneId.of(ServiceConstants.PACIFIC_TIME_ZONE);
		when(zonedDateTimeProvider.now(zoneId)).thenReturn(ZonedDateTime.of(2020, 06, 07, 02, 45, 0, 0, zoneId));

        String resultWithBasicInfo = searchService.searchReservations(Collections.emptyMap(), true);
		
		assertNotNull(resultWithBasicInfo);
		assertTrue(resultWithBasicInfo.contains("\"upcoming\":true"));
	}

	@Test
	void searchReservationUpcomingLastDayAndAfterDateRoll() throws DatatypeConfigurationException {

		Reservation reservation = convert("/reservation.json", Reservation.class);
		XMLGregorianCalendar startTime = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(LocalDate.of(2020, 06, 06).toString());
		reservation.getStayDateRange().setStartTime(startTime);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));
		ZoneId zoneId = ZoneId.of(ServiceConstants.PACIFIC_TIME_ZONE);
		when(zonedDateTimeProvider.now(zoneId)).thenReturn(ZonedDateTime.of(2020, 06, 07, 03, 15, 0, 0, zoneId));

		String resultWithBasicInfo = searchService.searchReservations(Collections.emptyMap(), true);
		
		assertNotNull(resultWithBasicInfo);
		assertTrue(resultWithBasicInfo.contains("\"upcoming\":false"));
	}

	@Test
	void searchReservationUpcomingNYTimeZone() throws DatatypeConfigurationException {

		Reservation reservation = convert("/reservation.json", Reservation.class);
		ZoneId zoneId = ZoneId.of("America/New_York");
		XMLGregorianCalendar startTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(
				ZonedDateTime.now(zoneId).plusDays(ServiceConstants.UPCOMING_DAYS_WINDOW - 1).toLocalDate().toString());
		reservation.getStayDateRange().setStartTime(startTime);
		reservation.getHotelReference().setHotelCode("304");
		when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));
		when(zonedDateTimeProvider.now(zoneId)).thenReturn(ZonedDateTime.now(zoneId));

		String resultWithBasicInfo = searchService.searchReservations(Collections.emptyMap(), true);
		
		assertNotNull(resultWithBasicInfo);
		assertTrue(resultWithBasicInfo.contains("\"upcoming\":true"));
	}

	@Test
	void searchReservationUpcomingNYTimeZoneThreeDaysPriorAndBeforeDateRoll() throws DatatypeConfigurationException {

		Reservation reservation = convert("/reservation.json", Reservation.class);
		XMLGregorianCalendar startTime = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(LocalDate.of(2020, 06, 06).toString());
		reservation.getStayDateRange().setStartTime(startTime);
		reservation.getHotelReference().setHotelCode("304");
		ZoneId zoneId = ZoneId.of("America/New_York");
		when(zonedDateTimeProvider.now(zoneId)).thenReturn(ZonedDateTime.of(2020, 06, 03, 00, 30, 0, 0, zoneId));
		when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));

		String resultWithBasicInfo = searchService.searchReservations(Collections.emptyMap(), true);
		
		assertNotNull(resultWithBasicInfo);
		assertTrue(resultWithBasicInfo.contains("\"upcoming\":true"));
	}

	@Test
	void searchReservationUpcomingNYTimeZoneLastDayAndBeforeDateRoll() throws DatatypeConfigurationException {

		Reservation reservation = convert("/reservation.json", Reservation.class);
		XMLGregorianCalendar startTime = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(LocalDate.of(2020, 06, 06).toString());
		reservation.getStayDateRange().setStartTime(startTime);
		reservation.getHotelReference().setHotelCode("304");
		ZoneId zoneId = ZoneId.of("America/New_York");
		when(zonedDateTimeProvider.now(zoneId)).thenReturn(ZonedDateTime.of(2020, 06, 07, 03, 45, 0, 0, zoneId));
		when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));

		String resultWithBasicInfo = searchService.searchReservations(Collections.emptyMap(), true);
		
		assertNotNull(resultWithBasicInfo);
		assertTrue(resultWithBasicInfo.contains("\"upcoming\":true"));
	}

	@Test
	void searchReservationUpcomingNYTimeZoneLastDayAndAfterDateRoll() throws DatatypeConfigurationException {

		Reservation reservation = convert("/reservation.json", Reservation.class);
		XMLGregorianCalendar startTime = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(LocalDate.of(2020, 06, 06).toString());
		reservation.getStayDateRange().setStartTime(startTime);
		reservation.getHotelReference().setHotelCode("304");
		ZoneId zoneId = ZoneId.of("America/New_York");
		when(zonedDateTimeProvider.now(zoneId)).thenReturn(ZonedDateTime.of(2020, 06, 07, 04, 15, 0, 0, zoneId));
		when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));

		String resultWithBasicInfo = searchService.searchReservations(Collections.emptyMap(), true);
		
		assertNotNull(resultWithBasicInfo);
		assertTrue(resultWithBasicInfo.contains("\"upcoming\":false"));
	}

	@Test
	void searchInHouseReservationTest() {

		when(documentDao.searchInHouseReservations(anyMap()))
				.thenReturn(convert("/reservation.json", Reservation.class));

		String result = searchService.searchInHouseReservations(new HashMap<>());

		assertNotNull(result);
		assertTrue(result.contains("\"resvNameId\":\"786195318\""));
		assertTrue(result.contains("\"roomType\":\"DWTK\""));

		verify(documentDao, times(1)).searchInHouseReservations(anyMap());
	}

	@Test
	void fetchBulkReservationsTest() {

		ResponseWithHeaders response = new ResponseWithHeaders();
		response.setHeaders(null);
		response.setResponse(CommonUtil.convertToJson(convert("/reservation.json", Reservation.class)));
		when(documentDao.fetchBulkReservations(Mockito.any())).thenReturn(response);

        ResponseWithHeaders result = searchService.fetchBulkReservations(Collections.emptyMap());

		assertNotNull(result);
	}

	@Test
	void searchReservationProfilesTest() {

		BasicReservationProfile profile = new BasicReservationProfile();
		List<BasicReservationProfile> response = Arrays.asList(profile);
		when(documentDao.searchReservationProfiles(Mockito.any())).thenReturn(CommonUtil.convertToJson(response));

        String result = searchService.searchReservationProfiles(Collections.emptyMap());

		assertNotNull(result);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/bookingDotcomReservation-12-01-AM-bookingTime-sameDayArrival.json",
			"/bookingDotcomReservation-12-01-AM-bookingTime-nextDayArrival.json",
			"/bookingDotcomReservation-11-59-PM-bookingTime-sameDayArrival.json",
			"/bookingDotcomReservation-11-59-PM-bookingTime-nextDayArrival.json" })
	void testBlockedProfileId_bookedWithin24Hours(String fileName) {

		Reservation reservation = convert(fileName, Reservation.class);
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);
		ThreadContext.put(ServiceConstants.MGM_ROLE, "guest");

        assertThrows(DataNotFoundException.class, () -> searchService.searchReservations(Collections.emptyMap(), false));
        assertThrows(DataNotFoundException.class, () -> searchService.searchReservations(Collections.emptyMap(), true));
	}

   
	@Test
	void testBlockedProfileId_noBlockedProfileIdSettingsFound() {

		Reservation reservation = convert("/bookingDotcomReservation-12-01-AM-bookingTime-sameDayArrival.json", Reservation.class);
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);
		when(appProperties.getBlockProfileIdSettings()).thenReturn(null);
		ThreadContext.put(ServiceConstants.MGM_ROLE, "guest");

        String fullSearchResult = searchService.searchReservations(Collections.emptyMap(), false);
        String basicSearchResult = searchService.searchReservations(Collections.emptyMap(), true);

		assertNotNull(fullSearchResult);
		assertNotNull(basicSearchResult);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/bookingDotcomReservation-12-01-AM-bookingTime-sameDayArrival.json",
			"/bookingDotcomReservation-12-01-AM-bookingTime-nextDayArrival.json",
			"/bookingDotcomReservation-11-59-PM-bookingTime-sameDayArrival.json",
			"/bookingDotcomReservation-11-59-PM-bookingTime-nextDayArrival.json" })
	void testBlockedProfileId_bookedWithin24Hours_allowedReservationStatus(String fileName) {

		Reservation reservation = convert(fileName, Reservation.class);
		reservation.getRoomStays().getRoomStay().get(0).setReservationStatusType(ReservationStatusType.INHOUSE);
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);
		ThreadContext.put(ServiceConstants.MGM_ROLE, "guest");

        String fullSearchResult = searchService.searchReservations(Collections.emptyMap(), false);
        String basicSearchResult = searchService.searchReservations(Collections.emptyMap(), true);

		assertNotNull(fullSearchResult);
		assertNotNull(basicSearchResult);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/bookingDotcomReservation-12-01-AM-bookingTime-sameDayArrival.json",
			"/bookingDotcomReservation-12-01-AM-bookingTime-nextDayArrival.json",
			"/bookingDotcomReservation-11-59-PM-bookingTime-sameDayArrival.json",
			"/bookingDotcomReservation-11-59-PM-bookingTime-nextDayArrival.json" })
	void testUnblockedProfileId_bookedWithin24Hours(String fileName) {

		Reservation reservation = convert(fileName, Reservation.class);
		reservation.getResProfiles().getResProfile().get(1).getProfile().setMfResortProfileID("123456789");
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);
		ThreadContext.put(ServiceConstants.MGM_ROLE, "guest");

        String fullSearchResult = searchService.searchReservations(Collections.emptyMap(), false);
        String basicSearchResult = searchService.searchReservations(Collections.emptyMap(), true);

		assertNotNull(fullSearchResult);
		assertNotNull(basicSearchResult);
	}

	@Test
	void testBlockedProfileId_sameDayBooking_allowedToken() {

		Reservation reservation = convert("/bookingDotcomReservation-12-01-AM-bookingTime-sameDayArrival.json",
				Reservation.class);
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);
		ThreadContext.put(ServiceConstants.MGM_ROLE, "service");

        String fullSearchResult = searchService.searchReservations(Collections.emptyMap(), false);
        String basicSearchResult = searchService.searchReservations(Collections.emptyMap(), true);

		assertNotNull(fullSearchResult);
		assertNotNull(basicSearchResult);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/bookingDotcomReservation-11-59-PM-bookingTime-twoDaysFromArrival.json",
			"/bookingDotcomReservation-12-01-AM-bookingTime-twoDaysFromArrival.json" })
	void testBlockedProfileId_outside24HourWindow(String fileName) {

		Reservation reservation = convert(fileName, Reservation.class);
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);
		ThreadContext.put(ServiceConstants.MGM_ROLE, "guest");

        String fullSearchResult = searchService.searchReservations(Collections.emptyMap(), false);
        String basicSearchResult = searchService.searchReservations(Collections.emptyMap(), true);

		assertNotNull(fullSearchResult);
		assertNotNull(basicSearchResult);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/reservation-partner-account-selectedmemberships.json" })
	void testBlockedPartnerAccount(String fileName) {

		Reservation reservation = convert(fileName, Reservation.class);
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);
		ThreadContext.put(ServiceConstants.MGM_ROLE, "guest");

        assertThrows(DataNotFoundException.class, () -> searchService.searchReservations(Collections.emptyMap(), false));
	}

	@Test
	void testBlockedPartnerAccount_blockAllProperties() {

		Reservation reservation = convert("/reservation-partner-account-selectedmemberships.json", Reservation.class);
		BlockPartnerAccountSettings blockPartnerAccountSettings = getBlockPartnerAccountSettings();
		blockPartnerAccountSettings.setBlockedHotelCodes(Collections.emptyList());
		when(appProperties.getBlockPartnerAccountSettings()).thenReturn(blockPartnerAccountSettings);
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);
		ThreadContext.put(ServiceConstants.MGM_ROLE, "guest");
	   
        assertThrows(DataNotFoundException.class, () -> searchService.searchReservations(Collections.emptyMap(), false));
	}

	@Test
	void testBlockedPartnerAccount_validHotelCode() {

		Reservation reservation = convert("/reservation-partner-account-memberships.json", Reservation.class);
		reservation.getHotelReference().setHotelCode("930");
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);
		ThreadContext.put(ServiceConstants.MGM_ROLE, "guest");

        String fullSearchResult = searchService.searchReservations(Collections.emptyMap(), false);
        String basicSearchResult = searchService.searchReservations(Collections.emptyMap(), true);

		assertNotNull(fullSearchResult);
		assertNotNull(basicSearchResult);
	}

	@Test
	void testBlockedPartnerAccount_validToken() {

		Reservation reservation = convert("/reservation-partner-account-memberships.json", Reservation.class);
		reservation.getHotelReference().setHotelCode("285");
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);
		ThreadContext.put(ServiceConstants.MGM_ROLE, "employee");

        String fullSearchResult = searchService.searchReservations(Collections.emptyMap(), false);
        String basicSearchResult = searchService.searchReservations(Collections.emptyMap(), true);

		assertNotNull(fullSearchResult);
		assertNotNull(basicSearchResult);
	}

	@Test
	void testBlockedPartnerAccount_inHouse() {

		Reservation reservation = convert("/reservation-partner-account-memberships.json", Reservation.class);
		reservation.getRoomStays().getRoomStay().get(0).setReservationStatusType(ReservationStatusType.INHOUSE);
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);
		ThreadContext.put(ServiceConstants.MGM_ROLE, "employee");

        String fullSearchResult = searchService.searchReservations(Collections.emptyMap(), false);
        String basicSearchResult = searchService.searchReservations(Collections.emptyMap(), true);

		assertNotNull(fullSearchResult);
		assertNotNull(basicSearchResult);
	}

	@Test
	void testBlockedPartnerAccount_filterInActiveMemberships() {

		Reservation reservation = convert("/reservation-partner-account-inactive-memberships.json", Reservation.class);
		List<Reservation> response = Arrays.asList(reservation);
		when(documentDao.searchReservations(Mockito.any())).thenReturn(response);
		ThreadContext.put(ServiceConstants.MGM_ROLE, "employee");

		String fullSearchResult = searchService.searchReservations(Collections.emptyMap(), false);
		String basicSearchResult = searchService.searchReservations(Collections.emptyMap(), true);

		assertNotNull(fullSearchResult);
		assertNotNull(basicSearchResult);
	}

    @ParameterizedTest
    @ValueSource(strings = { "ice", "web", "unknown" })
    void testTCOLVSearchWithServiceTokenWithValidChannels(String channel) {
        Reservation reservation = convert("/reservation.json", Reservation.class);
        reservation.getHotelReference().setHotelCode("195");
        when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));
        Map<String, String> params = new HashMap<>();
        params.put(ServiceConstants.MGM_ROLE, "service");
		params.put(ServiceConstants.X_MGM_CHANNEL, channel);

        String searchResult = searchService.searchReservations(params, false);
   
        assertNotNull(searchResult);
		assertTrue(searchResult.contains("195"));
    }

	@Test
    void testTCOLVSearchWithServiceTokenWithInvalidChannelAndSingleSearchResult() {
        Reservation reservation = convert("/reservation.json", Reservation.class);
        reservation.getHotelReference().setHotelCode("195");
        when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));
        Map<String, String> params = new HashMap<>();
        params.put(ServiceConstants.MGM_ROLE, "service");
        params.put(ServiceConstants.X_MGM_CHANNEL, "mobile");

        String result = searchService.searchReservations(params, true);

        assertEquals("[]", result);
    }

	@Test
    void testTCOLVSearchWithServiceTokenWithInvalidChannelAndMultipleSearchResults() {
        Reservation reservation1 = convert("/reservation.json", Reservation.class);
        Reservation reservation2 = convert("/reservation.json", Reservation.class);
        reservation2.getHotelReference().setHotelCode("195");
        when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation1, reservation2));
        Map<String, String> params = new HashMap<>();
        params.put(ServiceConstants.MGM_ROLE, "service");
		params.put(ServiceConstants.X_MGM_CHANNEL, "mobile");

		String searchResult = searchService.searchReservations(params, true);

        assertNotNull(searchResult);
		assertFalse(searchResult.contains("\"hotelCode\":\"195\""));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testTCOLVSearchWithGuestToken(boolean basicInfo) {
        Reservation reservation = convert("/reservation.json", Reservation.class);
        reservation.getHotelReference().setHotelCode("195");
        when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));
        Map<String, String> params = new HashMap<>();
        params.put(ServiceConstants.MGM_ROLE, "guest");

        String result = searchService.searchReservations(params, basicInfo);

        assertEquals("[]", result);
    }

    @Test
    void testBasicSearchForKiosk() {
        Reservation reservation = convert("/reservation.json", Reservation.class);
        when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));
        when(appProperties.getKioskClientId()).thenReturn("kiosk_app_service");
        Map<String, String> params = new HashMap<>();
        params.put(ServiceConstants.KIOSK_CLIENT_ID, "kiosk_app_service");

        String searchResult = searchService.searchReservations(params, true);

        assertNotNull(searchResult);
        assertTrue(searchResult.contains("ratePlans"));
		assertTrue(searchResult.contains("\"id\":\"46cb10a2-a39f-332d-a269-63bdca907556\""));
    }

    @Test
    void testBasicSearchForKioskNullproperty() {
        Reservation reservation = convert("/reservation.json", Reservation.class);
        when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));
        when(appProperties.getKioskClientId()).thenReturn(null);
        Map<String, String> params = new HashMap<>();
        params.put(ServiceConstants.KIOSK_CLIENT_ID, "kiosk_app_service");

        String searchResult = searchService.searchReservations(params, true);

        assertNotNull(searchResult);
        assertFalse(searchResult.contains("ratePlans"));
		assertFalse(searchResult.contains("\"id\":\"46cb10a2-a39f-332d-a269-63bdca907556\""));
    }

    @Test
    void testBasicSearchForKiosk_NoKioskParam() {
        Reservation reservation = convert("/reservation.json", Reservation.class);
        when(documentDao.searchReservations(Mockito.any())).thenReturn(Arrays.asList(reservation));
        when(appProperties.getKioskClientId()).thenReturn(null);
        when(appProperties.getKioskClientId()).thenReturn("kiosk_app_service");
        Map<String, String> params = new HashMap<>();
        params.put(ServiceConstants.MGM_ROLE, "guest");

        String searchResult = searchService.searchReservations(params, true);

        assertNotNull(searchResult);
        assertFalse(searchResult.contains("ratePlans"));
		assertFalse(searchResult.contains("\"id\":\"46cb10a2-a39f-332d-a269-63bdca907556\""));
    }

	private BlockProfileIdSettings getBlockProfileIdSettings() {

		BlockProfileIdSettings blockProfileIdSettings = new BlockProfileIdSettings();
		blockProfileIdSettings.setBlockedProfileIds("82492537");
		blockProfileIdSettings.setBlockedReservationStatuses("RESERVED");
		blockProfileIdSettings.setAllowedTokens("service,employee");
		return blockProfileIdSettings;
	}

	private BlockPartnerAccountSettings getBlockPartnerAccountSettings() {

		BlockPartnerAccountSettings blockPartnerAccountSettings = new BlockPartnerAccountSettings();
		blockPartnerAccountSettings.setShouldBlockPartnerAccount(true);
		blockPartnerAccountSettings.setBlockedHotelCodes(Arrays.asList("280","285"));
		blockPartnerAccountSettings.setBlockedReservationStatuses(Arrays.asList("RESERVED"));
		blockPartnerAccountSettings.setAllowedTokens(Arrays.asList("service","employee"));
		blockPartnerAccountSettings.setBlockedProgramCodes(Arrays.asList("GP"));
		return blockPartnerAccountSettings;
	}

	private Map<String, String> getPropertyTimezones() {

		Map<String, String> propertyTimezones = new HashMap<>();
		propertyTimezones.put("285", ServiceConstants.PACIFIC_TIME_ZONE);
		propertyTimezones.put("304", "America/New_York");
		return propertyTimezones;
	}

	private Map<String, String> getPropertyDateRollShiftFromMidnightInMinutes() {

		Map<String, String> propertyDateRollShiftFromMidnightInMinutes = new HashMap<>();
		propertyDateRollShiftFromMidnightInMinutes.put("285", "180");
		propertyDateRollShiftFromMidnightInMinutes.put("304", "240");
		return propertyDateRollShiftFromMidnightInMinutes;
	}
}
