package com.mgmresorts.booking.room.reservation.search.dao;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.SqlQuerySpec;
import com.google.inject.Inject;
import com.mgmresorts.booking.common.error.exception.DataNotFoundException;
import com.mgmresorts.booking.common.error.exception.SystemException;
import com.mgmresorts.booking.room.oxi.models.GuestCount;
import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.oxi.models.RoomStay;
import com.mgmresorts.booking.room.oxi.models.extensions.AdditionalGuest;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.dao.helper.ResponseHelper;
import com.mgmresorts.booking.room.reservation.search.dao.helper.SearchHelper;
import com.mgmresorts.booking.room.reservation.search.response.ResponseWithHeaders;
import com.mgmresorts.booking.room.reservation.search.transformer.BasicReservationResponseTransformer;
import com.mgmresorts.booking.room.reservation.search.transformer.BulkFetchResponseTransformer;
import com.mgmresorts.booking.room.reservation.search.transformer.ReservationResponseTransformer;
import com.mgmresorts.booking.room.reservation.search.util.CommonUtil;
import com.mgmresorts.booking.room.reservation.search.util.DateTimeUtil;
import com.mgmresorts.booking.room.reservation.search.util.ServiceConstants;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;

/**
 * Implementation class which connects to cosmos DB instance to query
 * reservation documents.
 */
@Log4j2
@Data
public class DocumentDaoImpl implements DocumentDao {

	private CosmosAsyncClient documentClient;

	private CosmosAsyncContainer container;

	private AppProperties appProps;

	@Inject
	public DocumentDaoImpl(AppProperties appProps) {

		log.info("Creating instance: DocumentDao");
		// disable netty's logging
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig loggerCfg = config.getLoggerConfig("io.netty");
		loggerCfg.setLevel(Level.OFF);
		ctx.updateLoggers();

		this.appProps = appProps;
		CosmosClientBuilder clientBuilder = new CosmosClientBuilder().endpoint(appProps.getCosmosHost())
				.key(System.getenv(ServiceConstants.COSMOS_DB_ACCESS_KEY)).consistencyLevel(ConsistencyLevel.SESSION);
		// Using default gateway mode for local as VPN is allowing all IPs required for direct
		if (StringUtils.isEmpty(System.getenv(ServiceConstants.APP_PROFILE))
				|| System.getenv(ServiceConstants.APP_PROFILE).equals(ServiceConstants.LOCAL)) {
			clientBuilder = clientBuilder.gatewayMode();
			log.debug("Using gateway mode for cosmos connection");
		} else {
			log.debug("Using direct mode for cosmos connection");
		}
		documentClient = clientBuilder.buildAsyncClient();
		container = documentClient.getDatabase(ServiceConstants.DATABASE_ID)
				.getContainer(ServiceConstants.COLLECTION_ID);
	}

	@Override
	public List<Reservation> searchReservations(Map<String, String> params) {

		Map<String, String> paramsCopy = new HashMap<>(params);
		paramsCopy.remove(ServiceConstants.JWT_EXISTS);
		paramsCopy.remove(ServiceConstants.AGGREGATED);
		boolean exclude = !MapUtils.getBoolean(paramsCopy, ServiceConstants.INCLUDE_ALL_ROOM_TYPES, false);
		paramsCopy.remove(ServiceConstants.INCLUDE_ALL_ROOM_TYPES);
		SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(paramsCopy, appProps);

		if (querySpec.getQueryText().endsWith(" AND")) {
			querySpec.setQueryText(querySpec.getQueryText().substring(0, querySpec.getQueryText().length() - 4));
		}
		querySpec.setQueryText(querySpec.getQueryText().concat(" order by r.stayDateRange.startTime"));
		List<Reservation> results = executeQuery(querySpec, paramsCopy, exclude);

		if ((paramsCopy.containsKey(ServiceConstants.CONF_NUMBER)
				|| paramsCopy.containsKey(ServiceConstants.OPERA_CONF_NUMBER)) && results.isEmpty()) {
            log.info("Query with confirmation number yielded no results");
        }
        return results;
    }
    
	@Override
	public Reservation searchInHouseReservations(Map<String, String> params) {

		SqlQuerySpec sqlQuerySpec = SearchHelper.createInHouseSearchQuerySpec(params);
		return executeInHouseQuery(sqlQuerySpec);
	}

	private Reservation executeInHouseQuery(SqlQuerySpec querySpec) {

		log.debug(ServiceConstants.QUERY, querySpec.getQueryText());
		CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
		options.setQueryMetricsEnabled(false);
		options.setMaxDegreeOfParallelism(-1);
		options.setMaxBufferedItemCount(-1);
		List<Reservation> results = new ArrayList<>();

		container.queryItems(querySpec, options, Reservation.class).byPage().flatMap(reservationsFeedResponse -> {
			log.debug(ServiceConstants.COSMOS_DIAGNOSIS, ServiceConstants.EXECUTE_INHOUSE_QUERY,
					reservationsFeedResponse.getCosmosDiagnostics());
			results.addAll(filterReservationsByRestrictedHotels(reservationsFeedResponse.getResults()));
			return Flux.empty();
		}).blockLast();

		return results.stream().findFirst().orElseThrow(
				() -> new DataNotFoundException(ServiceConstants.NO_DATA_FOUND, ServiceConstants.NO_INHOUSE_RES_FOUND));
	}

	/**
	 * Execute query, iterate through the document list, extract the result
	 * portion of each document and convert the list of results to json.
	 * 
	 * @param query
	 *            Query to execute
	 * @param params
	 *            Request params
	 * @param exclude
	 *            Flag indicating if room type based filtering should be applied
	 * @return List of extracted results
	 */
	private List<Reservation> executeQuery(SqlQuerySpec querySpec, Map<String, String> params, boolean exclude) {

		log.debug(ServiceConstants.QUERY, querySpec.getQueryText());
		CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
		options.setQueryMetricsEnabled(false);
		options.setMaxDegreeOfParallelism(-1);
		options.setMaxBufferedItemCount(-1);
		List<Reservation> results = new LinkedList<>();

		container.queryItems(querySpec, options, Reservation.class).byPage().flatMap(reservationsFeedResponse -> {
			log.debug(ServiceConstants.COSMOS_DIAGNOSIS, ServiceConstants.EXECUTE_QUERY,
					reservationsFeedResponse.getCosmosDiagnostics());
			results.addAll(reservationsFeedResponse.getResults());
			return Flux.empty();
		}).blockLast();

		List<Reservation> docsList = new LinkedList<>();
		List<Reservation> filteredDocsList = null;

		results.forEach(resv -> {
			log.debug(resv.getReservationID());
			ReservationResponseTransformer.transform(resv);
			docsList.add(resv);
		});
		log.debug(docsList.size());
		filteredDocsList = filterNameMismatch(docsList, params);
		filteredDocsList = filterExcludedRoomAndShareWithReservations(filteredDocsList, exclude);
		filteredDocsList = ResponseHelper.resolveDuplicates(filteredDocsList);
		filteredDocsList = filterReservationsByRestrictedHotels(filteredDocsList);
		return filteredDocsList;
	}

	@Override
	public ResponseWithHeaders fetchBulkReservations(Map<String, String> params) {

		params.remove(ServiceConstants.JWT_EXISTS);
		String continuationToken = null;
		// If request contains continuationToken, set to feed options to get
		// paginated results
		if (params.containsKey(ServiceConstants.CONTINUATION_TOKEN)) {
			continuationToken = new String(
					Base64.getUrlDecoder().decode(params.get(ServiceConstants.CONTINUATION_TOKEN).replace(" ", "+")));
			log.debug("Request Continuation: {}", continuationToken);
			params.remove(ServiceConstants.CONTINUATION_TOKEN);
		}
		SqlQuerySpec querySpec = SearchHelper.createSearchQuerySpec(params, appProps);

		try {
			ResponseWithHeaders response = executeBulkQuery(querySpec, continuationToken);
			// When params doesn't contain verbose, return compact response
			if (!params.containsKey(ServiceConstants.VERBOSE)) {
				response.setResponse(BulkFetchResponseTransformer.getBulkFetchResponse(response.getResponse()));
			}
			return response;
		} catch (IOException e) {
			throw new SystemException(ServiceConstants.SYSTEM_ERROR, e);
		}
	}

	/**
	 * Executes query for bulk fetch, sets continuation header for pagination,
	 * convert the result to json.
	 * 
	 * @param query
	 *            Query to execute
	 * @param options
	 *            Feed options to use
	 * @return Returns response with headers
	 * @throws DocumentClientException
	 */
	private ResponseWithHeaders executeBulkQuery(SqlQuerySpec querySpec, String continuationToken) {
		// Bulk search doesn't require canceled reservations
		querySpec.setQueryText(querySpec.getQueryText().concat(" AND rs.reservationStatusType!=\"CANCELED\""));
		// Bulk query doesn't require distinct.
		// Continuation token with distinct query is not supported from SDK
		querySpec.setQueryText(querySpec.getQueryText().replace("distinct ", StringUtils.EMPTY));
		log.debug(ServiceConstants.QUERY, querySpec.getQueryText());
		final CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
		options.setResponseContinuationTokenLimitInKb(
				Integer.parseInt(System.getenv(ServiceConstants.CONTINUATION_TOKEN_LIMIT_IN_KB)));
		options.setQueryMetricsEnabled(false);
		options.setMaxDegreeOfParallelism(-1);
		options.setMaxBufferedItemCount(-1);

		Iterable<FeedResponse<Reservation>> feedResponseIterator = container
				.queryItems(querySpec, options, Reservation.class)
				.byPage(continuationToken, Integer.parseInt(System.getenv(ServiceConstants.MAX_ITEM_COUNT)))
				.toIterable();

		List<Reservation> docsList = new ArrayList<>();
		ResponseWithHeaders result = new ResponseWithHeaders();
		FeedResponse<Reservation> page = feedResponseIterator.iterator().next();
		log.debug(ServiceConstants.COSMOS_DIAGNOSIS, ServiceConstants.EXECUTE_BULK_QUERY, page.getCosmosDiagnostics());
		String nextToken = page.getContinuationToken();
		docsList.addAll(page.getResults());

		if (StringUtils.isNotEmpty(nextToken)) {
			log.debug("Response Continuation: {}", nextToken);
			result.getHeaders().put(ServiceConstants.CONTINUATION_TOKEN,
					Base64.getEncoder().encodeToString(nextToken.getBytes()));
		}
		List<Reservation> filteredDocsList = null;
		filteredDocsList = filterExcludedRoomAndShareWithReservations(docsList, true);
		filteredDocsList = filterReservationsByRestrictedHotels(filteredDocsList);
		result.setResponse(CommonUtil.convertToJson(filteredDocsList));
		return result;
	}

	private List<Reservation> filterExcludedRoomAndShareWithReservations(List<Reservation> docList, boolean exclude) {

		if (!exclude) {
			log.info("Room type based filtering is disabled");
			return docList;
		}
		List<Reservation> newDocList = new LinkedList<>();

		docList.forEach(resv -> {
			String hotelCode = resv.getHotelReference().getHotelCode();
			RoomStay roomStay = resv.getRoomStays().getRoomStay().get(0);
			String roomCode = Optional.ofNullable(roomStay.getRoomInventoryCode()).orElse(StringUtils.EMPTY);
			String guaranteeType = Optional.ofNullable(roomStay.getGuaranteeInfo().getMfGuaranteeType())
					.orElse(StringUtils.EMPTY);
			int guests = 0;

			for (GuestCount count : roomStay.getGuestCounts().getGuestCount()) {
				guests = guests + count.getMfCount();
			}
			String excludedRoomCodes = MapUtils.getString(appProps.getExcludedRoomTypes(), hotelCode,
					StringUtils.EMPTY);

			if (!excludedRoomCodes.contains(roomCode) && !guaranteeType.equalsIgnoreCase(ServiceConstants.SH)
					&& guests > 0) {
				newDocList.add(resv);
			} else {
				log.info("Reservation is excluded. Either room type is in exclude list or it's a share-with reservation: room code = {}, hotel code = {}, guarantee type = {}, guests = {}",
						roomCode, hotelCode, guaranteeType, guests);
			}
		});
		return newDocList;
	}
	
	private List<Reservation> filterReservationsByRestrictedHotels(List<Reservation> reservations) {

		Map<String, LocalDateTime> searchDisableDates = appProps.getSearchDisableDates();

		if (searchDisableDates == null || searchDisableDates.isEmpty()) {
			return reservations;
		}
		List<Reservation> filteredReservations = new LinkedList<>();

		reservations.forEach(reservation -> {
			String hotelCode = reservation.getHotelReference().getHotelCode();

			if (searchDisableDates.containsKey(hotelCode)
					&& DateTimeUtil.isLocalDateTimeInThePast(searchDisableDates.get(hotelCode))) {
				log.info("Reservation {} is excluded by {} config value for hotelCode {}: {}. Current dateTime is {}",
						reservation.getReservationID(), ServiceConstants.SEARCH_DISABLE_DATES, hotelCode,
						searchDisableDates.get(hotelCode), LocalDateTime.now());
				return;
			}
			filteredReservations.add(reservation);
		});
		return filteredReservations;
	}

	/**
	 * If the search is for secondary reservation conf number, name should match
	 * against respective secondary guest. Since it's very difficult enforce
	 * such match via cosmos query, doing it as filtering logic in this method.
	 * 
	 * @param docList
	 *            List of reservations returned by cosmos
	 * @param params
	 *            Query params received
	 * @return List of reservations filtered
	 */
	private List<Reservation> filterNameMismatch(List<Reservation> docList, Map<String, String> params) {
		// if query params didn't contain confNumber or didn't contain name
		// attribute, no filtering required
		if (!params.containsKey(ServiceConstants.CONF_NUMBER) || (!params.containsKey(ServiceConstants.FIRST_NAME)
				&& !params.containsKey(ServiceConstants.LAST_NAME))) {
			log.debug("Neither confNumber nor name attributes available, no filerting required for name mismatch");
			return docList;
		}
		List<Reservation> newDocList = new LinkedList<>();

		docList.forEach(resv -> {
			log.info("ConfNumber or name attributes are available, verifying if its secondary reservation and name matches");
			// if the confNumber is for secondary reservation, check name
			// matches on the secondary reservation
			Optional<AdditionalGuest> secGuestOpt = resv.getMgmProfile().getAdditionalGuests().stream()
					.filter(guest -> StringUtils.isNotEmpty(guest.getReservationID())
							&& guest.getReservationID().equals(params.get(ServiceConstants.CONF_NUMBER)))
					.findFirst();

			if (secGuestOpt.isPresent()) {
				AdditionalGuest secGuest = secGuestOpt.get();
				String firstName = params.getOrDefault(ServiceConstants.FIRST_NAME, secGuest.getFirstName());
				String lastName = params.getOrDefault(ServiceConstants.LAST_NAME, secGuest.getLastName());

				if (secGuest.getFirstName().equalsIgnoreCase(firstName)
						&& secGuest.getLastName().equalsIgnoreCase(lastName)) {
					newDocList.add(resv);
				} else {
					log.info("Look up was for secondary confirmation number and didn't match with secondary guest name");
				}
			} else {
				newDocList.add(resv);
			}
		});
		return newDocList;
	}

	@Override
	public String searchReservationProfiles(Map<String, String> params) {
		
		params.remove(ServiceConstants.JWT_EXISTS);
		SqlQuerySpec querySpec = SearchHelper.createSearchByIdsQuerySpec(params);
		log.debug(ServiceConstants.QUERY, querySpec.getQueryText());
		CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
		options.setQueryMetricsEnabled(false);
		options.setMaxDegreeOfParallelism(-1);
		options.setMaxBufferedItemCount(-1);
		List<Reservation> results = new LinkedList<>();

		container.queryItems(querySpec, options, Reservation.class).byPage().flatMap(reservationsFeedResponse -> {
			log.debug(ServiceConstants.COSMOS_DIAGNOSIS, ServiceConstants.SEARCH_RESERVATION_PROFILES,
					reservationsFeedResponse.getCosmosDiagnostics());
			results.addAll(reservationsFeedResponse.getResults());
			return Flux.empty();
		}).blockLast();

		List<Reservation> filteredResults = filterReservationsByRestrictedHotels(results);
		List<Object> docsList = new ArrayList<>();
		// Extract the result portion of each document in the list
		filteredResults.forEach(resv -> docsList.add(BasicReservationResponseTransformer.getBasicReservationProfile(resv)));
		return CommonUtil.convertToJson(docsList);
	}
}
