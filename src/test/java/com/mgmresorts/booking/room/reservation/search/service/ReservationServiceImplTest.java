package com.mgmresorts.booking.room.reservation.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.mgmresorts.booking.common.error.exception.FunctionalException;
import com.mgmresorts.booking.common.error.exception.SystemException;
import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.reservation.search.BaseUnitTest;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.dao.OperaDao;
import com.mgmresorts.booking.room.reservation.search.dao.ReservationRepository;
import com.mgmresorts.booking.room.reservation.search.dao.ReservationRepositoryImpl;
import com.mgmresorts.booking.room.reservation.search.models.opera.GetFolioDetailsResponse;
import com.mgmresorts.booking.room.reservation.search.util.CommonUtil;

class ReservationServiceImplTest extends BaseUnitTest {

	private SearchService searchService;
	private ReservationRepository reservationRepository;
	private ReservationServiceImpl reservationService;
	private AppProperties appProperties;
	private OperaDao operaDao;
	private OperaTokenService tokenService;

	@BeforeEach
	void setup() {
		reservationRepository = mock(ReservationRepositoryImpl.class);
		searchService = mock(SearchServiceImpl.class);
		appProperties = mock(AppProperties.class);
		operaDao = mock(OperaDao.class);
		tokenService = mock(OperaTokenServiceImpl.class);
		when(appProperties.getOperaCloudUrl()).thenReturn("/");
		reservationService = new ReservationServiceImpl();
		reservationService.setAppProperties(appProperties);
		reservationService.setOperaDao(operaDao);
		reservationService.setReservationRepository(reservationRepository);
		reservationService.setSearchService(searchService);
		reservationService.setOperaTokenService(tokenService);
	}

	@Test
	void fetchFolioTest() throws SQLException {
		when(appProperties.getOperaCloudEnabledProperties()).thenReturn(Collections.emptyList());
		Reservation reservation = convert("/reservation.json", Reservation.class);
		List<Reservation> response = Arrays.asList(reservation);
		when(searchService.searchReservations(Mockito.any(), Mockito.anyBoolean()))
				.thenReturn(CommonUtil.convertToJson(response));

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("TRX_DATE", new ArrayList<String>());
		when(reservationRepository.getFolioDetails(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyBoolean())).thenReturn(resultMap);

		Map<String, String> params = new HashMap<>();
		params.put("aggregated", "true");
		params.put("jwtExists", "true");

		String result = reservationService.fetchFolio(params);

		assertNotNull(result);
	}

	@Test
	void testFetchFolioTestThrowsOnSQLException() throws SQLException {
		when(appProperties.getOperaCloudEnabledProperties()).thenReturn(Collections.emptyList());
		Reservation reservation = convert("/reservation.json", Reservation.class);
		List<Reservation> response = Arrays.asList(reservation);
		when(searchService.searchReservations(Mockito.any(), Mockito.anyBoolean()))
				.thenReturn(CommonUtil.convertToJson(response));
		when(reservationRepository.getFolioDetails(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyBoolean())).thenThrow(new SQLException());

		try {
			reservationService.fetchFolio(new HashMap<>());
		} catch (SystemException e) {
			assertEquals("Failed to fetch folio details from Opera Db", e.getMessage());
		}
	}

	@Test
	void testFetchFolioTestThrowsOnjsonException() {
		when(appProperties.getOperaCloudEnabledProperties()).thenReturn(Collections.emptyList());
		when(searchService.searchReservations(Mockito.any(), Mockito.anyBoolean()))
				.thenReturn(CommonUtil.convertToJson("invalid json"));

		try {
			reservationService.fetchFolio(new HashMap<>());
		}
		catch (SystemException e) {
			assertEquals("Failed to convert reservation JSON string to reservation object", e.getMessage());
		}

	}

	@Test
	void fetchFolioFromOperaCloudTest() {
		when(appProperties.getOperaCloudEnabledProperties()).thenReturn(Arrays.asList("285"));
		Reservation reservation = convert("/reservation.json", Reservation.class);
		List<Reservation> response = Arrays.asList(reservation);
		when(searchService.searchReservations(Mockito.any(), Mockito.anyBoolean()))
				.thenReturn(CommonUtil.convertToJson(response));

		GetFolioDetailsResponse operaCloudResponse = convert("/operaCloudFolioResponse.json",
				GetFolioDetailsResponse.class);
		when(tokenService.getServiceToken()).thenReturn(StringUtils.EMPTY);
		when(operaDao.getFolioDetails(Mockito.anyString(), Mockito.any(), Mockito.anyString(),
				Mockito.anyString())).thenReturn(operaCloudResponse);

		Map<String, String> params = new HashMap<>();
		params.put("aggregated", "true");
		params.put("jwtExists", "true");

		String result = reservationService.fetchFolio(params);

		assertNotNull(result);
	}

	@Test
	void fetchFolioFromOperaCloudTestThrowsOn400ErrorCode() {
		when(appProperties.getOperaCloudEnabledProperties()).thenReturn(Arrays.asList("285"));
		Reservation reservation = convert("/reservation.json", Reservation.class);
		List<Reservation> response = Arrays.asList(reservation);
		when(searchService.searchReservations(Mockito.any(), Mockito.anyBoolean()))
				.thenReturn(CommonUtil.convertToJson(response));
		when(tokenService.getServiceToken()).thenReturn(StringUtils.EMPTY);
		FunctionalException exception = new FunctionalException("400Error", "400ErrorMsg");
		when(operaDao.getFolioDetails(Mockito.anyString(), Mockito.any(), Mockito.anyString(),
				Mockito.anyString())).thenThrow(exception);

		Map<String, String> params = new HashMap<>();
		params.put("aggregated", "true");
		params.put("jwtExists", "true");

		try {
		 	reservationService.fetchFolio(params);
		}
		catch(RuntimeException ex) {
			assertEquals(exception, ex);
		}
	}

	@Test
	void fetchFolioFromOperaCloudTestThrowsOn500ErrorCode() {
		when(appProperties.getOperaCloudEnabledProperties()).thenReturn(Arrays.asList("285"));
		Reservation reservation = convert("/reservation.json", Reservation.class);
		List<Reservation> response = Arrays.asList(reservation);
		when(searchService.searchReservations(Mockito.any(), Mockito.anyBoolean()))
				.thenReturn(CommonUtil.convertToJson(response));
		when(tokenService.getServiceToken()).thenReturn(StringUtils.EMPTY);
		SystemException exception = new SystemException("500error", "500ErrorMsg");
		when(operaDao.getFolioDetails(Mockito.anyString(), Mockito.any(), Mockito.anyString(),
				Mockito.anyString())).thenThrow(exception);

		Map<String, String> params = new HashMap<>();
		params.put("aggregated", "true");
		params.put("jwtExists", "true");

		try {
		 	reservationService.fetchFolio(params);
		}
		catch(RuntimeException ex) {
			assertEquals(exception, ex);
		}
	}
}
