package com.mgmresorts.booking.room.reservation.search.service;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.mgmresorts.booking.common.error.exception.FunctionalException;
import com.mgmresorts.booking.common.error.exception.SystemException;
import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.dao.OperaDao;
import com.mgmresorts.booking.room.reservation.search.dao.ReservationRepository;
import com.mgmresorts.booking.room.reservation.search.models.FolioResponse;
import com.mgmresorts.booking.room.reservation.search.models.opera.GetFolioDetailsResponse;
import com.mgmresorts.booking.room.reservation.search.transformer.FolioResponseTransformer;
import com.mgmresorts.booking.room.reservation.search.util.CommonUtil;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class ReservationServiceImpl implements ReservationService {

	@Inject
	private SearchService searchService;

	@Inject
	private ReservationRepository reservationRepository;

	@Inject
	private OperaDao operaDao;

	@Inject
	private AppProperties appProperties;

	@Inject
	private OperaTokenService operaTokenService;

	public ReservationServiceImpl() {
		log.info("Creating instance: ReservationService");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mgmresorts.booking.room.reservation.search.service.ReservationService
	 * #fetchFolio(java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String fetchFolio(Map<String, String> params) {

		final Map<String, Object> operaResultMap;
		final Reservation[] cosmosReservation;
		final FolioResponse folioResponse = new FolioResponse();
		final String jwtExists = params.getOrDefault(JWT_EXISTS, null);
		final Boolean aggregatedOperaResponse = Boolean.parseBoolean(params.getOrDefault(AGGREGATED, "false"));
		final boolean shouldHideProfilePII = !params.containsKey(CC_LAST_4)
				&& !params.getOrDefault(X_MGM_CHANNEL, EMPTY).equalsIgnoreCase(MOBILE);

		try {
			// fetch reservation from cosmos by all parameters provided
			final String reservation = searchService.searchReservations(params, false);
			if (reservation.equals(EMPTY_JSON)) {
				throw new FunctionalException(NO_DATA_FOUND, "No reservation found in cosmos for provided params");
			}
			cosmosReservation = CommonUtil.convertToReservationList(reservation);
			// create base response with reservation from cosmos
			FolioResponseTransformer.setBaseResponse(folioResponse, cosmosReservation[0], jwtExists,
					shouldHideProfilePII);
		} catch (JsonProcessingException ex) {
			throw new SystemException(RUNTIME_ERROR, "Failed to convert reservation JSON string to reservation object");
		}
		if (appProperties.getOperaCloudEnabledProperties()
				.contains(cosmosReservation[0].getHotelReference().getHotelCode())) {
			GetFolioDetailsResponse operaCloudResponse = null;
			try {
				operaCloudResponse = operaDao.getFolioDetails(operaTokenService.getServiceToken(),
						appProperties.getOperaCloudAppKey(), cosmosReservation[0].getHotelReference().getHotelCode(),
						cosmosReservation[0].getResvNameId());
				log.info("Fetched folio details from opera cloud: {}", CommonUtil.convertToJson(operaCloudResponse));
			} catch (RuntimeException ex) {
				log.warn("Error fetching folio details from opera cloud", ex);
				throw ex;
			}
			FolioResponseTransformer.transformOperaCloudResponse(folioResponse, operaCloudResponse,
					aggregatedOperaResponse);
		} else {
			try {
				// fetch folio details from opera with the reservation from cosmos
				operaResultMap = reservationRepository.getFolioDetails(cosmosReservation[0].getResvNameId(),
						cosmosReservation[0].getHotelReference().getHotelCode(), DEFAULT_FOLIO_WINDOWS,
						aggregatedOperaResponse);
			} catch (SQLException e) {
				throw new SystemException(RUNTIME_ERROR, "Failed to fetch folio details from Opera Db");
			}
			// If the resultMap from opera has data, validate credit card and
			// transform base response.

			// each row of operaResultMap contains a transaction date field, which
			// is mapped to the TRX_DATE column,
			// though all fields in the rows are returned as null if non-existent.

			if (!((ArrayList<String>) operaResultMap.get(TRX_DATE)).isEmpty()) {
				// enhance base response object with bill items returned from opera
				FolioResponseTransformer.transform(folioResponse, operaResultMap);
			}
		}
		return CommonUtil.convertToJson(folioResponse);
	}
}
