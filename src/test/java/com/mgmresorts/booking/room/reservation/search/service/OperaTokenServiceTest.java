package com.mgmresorts.booking.room.reservation.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.mgmresorts.booking.common.error.exception.FunctionalException;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.dao.OperaTokenDao;
import com.mgmresorts.booking.room.reservation.search.models.opera.OperaTokenResponse;

class OperaTokenServiceTest {

	private OperaTokenDao operaTokenDao;
	private OperaTokenServiceImpl operaTokenService;
	private AppProperties appProperties;

	@BeforeEach
	void setup() {
		operaTokenDao = mock(OperaTokenDao.class);
		appProperties = mock(AppProperties.class);
		operaTokenService = new OperaTokenServiceImpl(appProperties);
		operaTokenService.setOperaTokenDao(operaTokenDao);
		when(appProperties.getOperaCloudAppKey()).thenReturn("");
		when(appProperties.getOperaCloudUrl()).thenReturn("");
		when(appProperties.getOperaCloudScope()).thenReturn("");
		when(appProperties.getOperaEnterpriseId()).thenReturn("");
		when(appProperties.getOperaCloudClientId()).thenReturn("");
		when(appProperties.getOperaCloudClientSecret()).thenReturn("");
	}

	@Test
	void testGetServiceToken() {

		operaTokenService.setServiceToken(null);
		OperaTokenResponse response = new OperaTokenResponse();
		response.setAccess_token("testToken");
		when(operaTokenDao.getOperaServiceToken(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response);

		String result = operaTokenService.getServiceToken();

		assertEquals("testToken", result);
	}

	@Test
	void testGetServiceTokenFailure() {

		when(operaTokenDao.getOperaServiceToken(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new FunctionalException("error", "error generating token"));

		String result = operaTokenService.getServiceToken();

		assertNull(result);
	}

	@Test
	void testGetServiceToken_returnCurrentToken() {

		operaTokenService.setServiceToken("testToken");

		String result = operaTokenService.getServiceToken();

		assertEquals("testToken", result);
	}
}
