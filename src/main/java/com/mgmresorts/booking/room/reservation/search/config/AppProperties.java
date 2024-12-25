package com.mgmresorts.booking.room.reservation.search.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Splitter;
import com.mgmresorts.booking.room.reservation.search.inject.ApplicationInjector;
import com.mgmresorts.booking.room.reservation.search.models.BlockPartnerAccountSettings;
import com.mgmresorts.booking.room.reservation.search.models.BlockProfileIdSettings;
import com.mgmresorts.booking.room.reservation.search.util.CommonUtil;
import com.mgmresorts.booking.room.reservation.search.util.ServiceConstants;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
public @Data class AppProperties {

	public AppProperties() {

		log.info("Creating instance: AppProperties");
		loadProperties();
	}

	/***
	 * Loads the Environment specific properties file.
	 * 
	 * @return Properties Properties Object
	 */
	private void loadProperties() {

		final String propertyFilename = "/application-"
				+ (StringUtils.isNotBlank(System.getenv(ServiceConstants.APP_PROFILE))
						? System.getenv(ServiceConstants.APP_PROFILE)
						: ServiceConstants.LOCAL)
				+ ".properties";
		final Properties props = new Properties();

		try {
            props.load(ApplicationInjector.class.getResourceAsStream("/application-common.properties"));
            props.load(ApplicationInjector.class.getResourceAsStream(propertyFilename));
		} catch (IOException e) {
			log.info("Could not load applicaton properties configuration", e);
		}
		props.keySet().forEach(key -> {
			if (key.toString().startsWith(ServiceConstants.KEYS)) {
				keys.put(key.toString().replaceFirst("keys.", ""), props.getProperty(key.toString()));
			}
			if (key.toString().startsWith(ServiceConstants.EXCLUDED_ROOM_TYPES)) {
				excludedRoomTypes.put(key.toString().replaceFirst("excludedRoomTypes.", ""),
						props.getProperty(key.toString()));
			}
			if (key.equals(ServiceConstants.OPERA_CLOUD_URL)) {
				operaCloudUrl = props.getProperty(key.toString());
			}
			if (key.equals(ServiceConstants.COSMOS_HOST)) {
				cosmosHost = props.getProperty(key.toString());
			}
			if (key.equals(ServiceConstants.ALLOWED_PARAMS)) {
				allowedParams = props.getProperty(key.toString());
			}
			if (key.equals("dbMinPoolSize")) {
				dbMinPoolSize = Integer.parseInt(props.getProperty(key.toString()));
			}
			if (key.equals("dbMaxPoolSize")) {
				dbMaxPoolSize = Integer.parseInt(props.getProperty(key.toString()));
			}
			if (key.equals("dbInitialPoolSize")) {
				dbInitialPoolSize = Integer.parseInt(props.getProperty(key.toString()));
			}
			if (key.equals("dbMaxConnectionReuseCount")) {
				dbMaxConnectionReuseCount = Integer.parseInt(props.getProperty(key.toString()));
			}
			if (key.equals("dbAbandonedConnectionTimeoutInSec")) {
				dbAbandonedConnectionTimeoutInSec = Integer.parseInt(props.getProperty(key.toString()));
			}
			if (key.equals("dbTimeToLiveConnectionTimeoutInSec")) {
				dbTimeToLiveConnectionTimeoutInSec = Integer.parseInt(props.getProperty(key.toString()));
			}
			if (key.equals("dbConnectionWaitTimeoutInSec")) {
				dbConnectionWaitTimeoutInSec = Integer.parseInt(props.getProperty(key.toString()));
			}
			if (key.equals("dbInactiveConnectionTimeoutInSec")) {
				dbInactiveConnectionTimeoutInSec = Integer.parseInt(props.getProperty(key.toString()));
			}
			if (key.equals("dbQueryTimeoutInSec")) {
				dbQueryTimeoutInSec = Integer.parseInt(props.getProperty(key.toString()));
			}
			if (key.equals("dbSecondsToTrustIdleConnection")) {
				dbSecondsToTrustIdleConnection = Integer.parseInt(props.getProperty(key.toString()));
			}
			if (key.equals("dbTimeoutCheckInterval")) {
				dbTimeoutCheckInterval = Integer.parseInt(props.getProperty(key.toString()));
			}
		});
		// Load settings for blocked partner accounts
		Optional.ofNullable(System.getenv(ServiceConstants.BLOCK_PARTNER_ACCOUNT_SETTINGS))
				.ifPresent(settings -> blockPartnerAccountSettings = CommonUtil
						.convertToObj(settings, BlockPartnerAccountSettings.class));
		// Load settings for blocked profile ids
		Optional.ofNullable(System.getenv(ServiceConstants.BLOCK_PROFILE_ID_SETTINGS))
				.ifPresent(settings -> blockProfileIdSettings = CommonUtil
						.convertToObj(settings, BlockProfileIdSettings.class));
		// Load property timezones
		Optional.ofNullable(System.getenv(ServiceConstants.PROPERTY_TIME_ZONES))
				.ifPresent(timezones -> propertyTimezones = Splitter.on(ServiceConstants.COMMA)
						.withKeyValueSeparator(ServiceConstants.REGEX_DELIMITER).split(timezones));
		Optional.ofNullable(System.getenv(ServiceConstants.PROPERTY_DATE_ROLL_SHIFT_FROM_MIDNIGHT_IN_MINUTES))
				.ifPresent(dateRollShift -> propertyDateRollShiftFromMidnightInMinutes = Splitter
						.on(ServiceConstants.COMMA).withKeyValueSeparator(ServiceConstants.REGEX_DELIMITER)
						.split(dateRollShift));
		// Load restricted hotels for search
		Optional.ofNullable(System.getenv(ServiceConstants.SEARCH_DISABLE_DATES)).ifPresent(dates -> {
			Map<String, String> stringMap = Splitter.on(ServiceConstants.COMMA)
					.withKeyValueSeparator(ServiceConstants.REGEX_DELIMITER).split(dates);

			stringMap.forEach((hotelCode, date) -> {
				try {
					LocalDateTime parsedDate = LocalDateTime.parse(date);
					searchDisableDates.put(hotelCode, parsedDate);
					log.info("Loaded search disable config value for hotel code {}: {}. Current dateTime is {}",
							hotelCode, parsedDate, LocalDateTime.now());
				} catch (Exception ex) {
					log.error("Error parsing {} config value for hotel code {}: {}",
							ServiceConstants.SEARCH_DISABLE_DATES, hotelCode, date);
				}
			});
		});

		operaCloudClientId = Optional.ofNullable(System.getenv(ServiceConstants.OPERA_CLOUD_CLIENT_ID))
				.orElse(StringUtils.EMPTY);
		operaCloudClientSecret = Optional.ofNullable(System.getenv(ServiceConstants.OPERA_CLOUD_CLIENT_SECRET))
				.orElse(StringUtils.EMPTY);
		operaCloudAppKey = Optional.ofNullable(System.getenv(ServiceConstants.OPERA_CLOUD_APP_KEY))
				.orElse(StringUtils.EMPTY);
		operaEnterpriseId = Optional.ofNullable(System.getenv(ServiceConstants.OPERA_ENTERPRISEID))
				.orElse(StringUtils.EMPTY);
		operaCloudScope = Optional.ofNullable(System.getenv(ServiceConstants.OPERA_CLOUD_SCOPE))
				.orElse(StringUtils.EMPTY);
		operaCloudEnabledProperties = Splitter.on(",").splitToList(Optional
			.ofNullable(System.getenv(ServiceConstants.OPERA_CLOUD_ENABLED_PROPERTIES)).orElse(StringUtils.EMPTY));
		kioskClientId = Optional.ofNullable(System.getenv(ServiceConstants.KIOSK_CLIENT_ID)).orElse(null);
		tcolvChannelWhitelist = Splitter.on(",").splitToList(Optional
				.ofNullable(System.getenv(ServiceConstants.TCOLV_CHANNEL_WHITELIST)).orElse(StringUtils.EMPTY));
	}

	private String operaEnterpriseId;
	private String operaCloudScope;
	private String operaCloudClientId;
	private String operaCloudClientSecret;
	private String operaCloudAppKey;
	private List<String> operaCloudEnabledProperties;
	private String operaCloudUrl;
	private List<String> tcolvChannelWhitelist;
	private Map<String, String> keys = new HashMap<>();
	private String cosmosHost;
	private Map<String, String> excludedRoomTypes = new HashMap<>();
	private String allowedParams;
	private Map<String, String> propertyTimezones = new HashMap<>();
	private Map<String, String> propertyDateRollShiftFromMidnightInMinutes = new HashMap<>();
	private Map<String, LocalDateTime> searchDisableDates = new HashMap<>();
	private BlockProfileIdSettings blockProfileIdSettings;
	private BlockPartnerAccountSettings blockPartnerAccountSettings;
	private int dbMinPoolSize;
	private int dbMaxPoolSize;
	private int dbInitialPoolSize;
	private int dbMaxConnectionReuseCount;
	private int dbAbandonedConnectionTimeoutInSec;
	private int dbTimeToLiveConnectionTimeoutInSec;
	private int dbConnectionWaitTimeoutInSec;
	private int dbInactiveConnectionTimeoutInSec;
	private int dbQueryTimeoutInSec;
	private int dbSecondsToTrustIdleConnection;
	private int dbTimeoutCheckInterval;
	private String kioskClientId;
}
