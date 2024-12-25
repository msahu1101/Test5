package com.mgmresorts.booking.room.reservation.search.service;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.dao.OperaTokenDao;
import com.mgmresorts.booking.room.reservation.search.feign.FeignEncoder;
import com.mgmresorts.booking.room.reservation.search.feign.FeignErrorDecoder;
import com.mgmresorts.booking.room.reservation.search.models.opera.OperaTokenRequest;
import com.mgmresorts.booking.room.reservation.search.models.opera.OperaTokenResponse;
import com.mgmresorts.booking.room.reservation.search.util.ServiceConstants;

import feign.Feign;
import feign.Logger.Level;
import feign.auth.BasicAuthRequestInterceptor;
import feign.gson.GsonDecoder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class OperaTokenServiceImpl implements OperaTokenService {

	private OperaTokenDao operaTokenDao;
	private AppProperties appProperties;
	private String serviceToken;

	@Inject
	public OperaTokenServiceImpl(AppProperties appProperties) {
		this.appProperties = appProperties;
		if (StringUtils.isNotBlank(System.getenv(ServiceConstants.OPERA_CLOUD_ENABLED_PROPERTIES))) {
			operaTokenDao = Feign.builder().logLevel(Level.FULL).encoder(new FeignEncoder()).decoder(new GsonDecoder())
					.requestInterceptor(new BasicAuthRequestInterceptor(appProperties.getOperaCloudClientId(),
							appProperties.getOperaCloudClientSecret()))
					.errorDecoder(new FeignErrorDecoder())
					.target(OperaTokenDao.class, appProperties.getOperaCloudUrl());
			generateServiceToken();
			// Refresh token again every 50 mins
			new Timer().scheduleAtFixedRate(new TokenRefreshTask(), 0, 3000000);
		}
	}

	private void generateServiceToken() {

		log.debug("Start opera service token refresh");

		try {
			OperaTokenResponse authTokenResponse = operaTokenDao.getOperaServiceToken(
					OperaTokenRequest.builder().grant_type(ServiceConstants.GRANT_TYPE)
							.scope(appProperties.getOperaCloudScope()).build(),
					appProperties.getOperaCloudAppKey(), appProperties.getOperaEnterpriseId());

			serviceToken = authTokenResponse.getAccess_token();
		} catch (Exception e) {
			log.error("Error generating opera service token", e);
			// in case of an error, empty the token
			serviceToken = null;
		}
		log.info("Complete opera service token refresh");
	}

	/**
	 * Timer task to refresh service token periodically
	 *
	 */
	public class TokenRefreshTask extends TimerTask {

		@Override
		public void run() {

			generateServiceToken();
		}
	}

	@Override
	public synchronized String getServiceToken() {

		if (StringUtils.isEmpty(serviceToken)) {
			generateServiceToken();
		}
		return serviceToken;
	}
}
