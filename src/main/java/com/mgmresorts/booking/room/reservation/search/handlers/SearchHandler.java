package com.mgmresorts.booking.room.reservation.search.handlers;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.ThreadContext;

import com.mgmresorts.booking.common.error.Error;
import com.mgmresorts.booking.room.reservation.search.service.SearchService;
import com.mgmresorts.booking.room.reservation.search.util.ValidationUtil;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

/**
 * Http handler class for search reservations API i.e,
 * booking/v1/reservation/room/search
 *
 */
public class SearchHandler extends BaseHandler {

	private final SearchService searchService = injectionContext.instanceOf(SearchService.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mgmresorts.common.handler.AzureSpringBootHttpQueryHandler#validate(
	 * java.util.Map)
	 */
	@Override
	public Optional<Error> validate(Map<String, String> params, HttpRequestMessage<Optional<?>> request) {

		if (!ValidationUtil.isSearchValid(params)) {
			return Optional.of(new Error("One or more required params are missing"));
		}

		Map<String, String> queryParams = request.getQueryParameters();
		if ((queryParams.size() == 1 && queryParams.containsKey(CONF_NUMBER)
				|| queryParams.containsKey(INCLUDE_ALL_ROOM_TYPES))
				&& Boolean.parseBoolean(System.getenv(SEARCH_BY_CONF_NUMBER_RESTRICT_MGM_ROLE))) {
			return ValidationUtil.ensureServiceOrEmployeeRole(params.get(MGM_ROLE));
		}
		return ValidationUtil.ensureNoAnonymousJwtRole(params.getOrDefault(MGM_ROLE, EMPTY));
	}

	/**
	 * Function handler for http request for search reservation API i.e.,
	 * booking/v1/reservation/room/search
	 * 
	 * @param request
	 *            Http request message
	 * @param context
	 *            Execution context
	 * @return Returns search reservation response
	 */
	@FunctionName("search")
	public HttpResponseMessage search(@HttpTrigger(
			name = "req",
			methods = { HttpMethod.GET },
			route = "booking/v1/reservation/room/search",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<?>> request,
			final ExecutionContext context) {

		return this.handleRequest(request, context, true, true);
	}

	@FunctionName("search-apigee")
	public HttpResponseMessage searchApigee(@HttpTrigger(
			name = "req",
			methods = { HttpMethod.GET },
			route = "v1/reservations",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<?>> request,
									  final ExecutionContext context) {
		return this.handleRequest(request, context, true, false);
	}

	@Override
	public Object handleRequest(Map<String, String> params, ExecutionContext context) {

		return searchService.searchReservations(params, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mgmresorts.booking.spring.cloud.handler.
	 * AzureSpringBootHttpQueryHandler#addToLogContext(java.util.Map)
	 */
	@Override
	public void addToLogContext(Map<String, String> headers, Map<String, String> params) {

		Optional.ofNullable(params.get(MLIFE_NUMBER))
				.ifPresent(mlifeNumber -> ThreadContext.put(MLIFE_NUMBER, mlifeNumber));
		Optional.ofNullable(params.get(MGMID)).ifPresent(mgmId -> ThreadContext.put(MGMID, mgmId));
		Optional.ofNullable(params.get(CONF_NUMBER))
				.ifPresent(confNumber -> ThreadContext.put(CONF_NUMBER, confNumber));
		Optional.ofNullable(params.get(OPERA_CONF_NUMBER))
				.ifPresent(operaConfNumber -> ThreadContext.put(OPERA_CONF_NUMBER, operaConfNumber));
		Optional.ofNullable(params.get(ID)).ifPresent(id -> ThreadContext.put(ID, id));
		Optional.ofNullable(params.get(PARTNER_ACCOUNT_NUMBER))
				.ifPresent(partnerAccountNumber -> ThreadContext.put(PARTNER_ACCOUNT_NUMBER, partnerAccountNumber));
		Optional.ofNullable(params.get(GUEST_MLIFE_NUMBER))
				.ifPresent(guestMlifeNumber -> ThreadContext.put(GUEST_MLIFE_NUMBER, guestMlifeNumber));
		Optional.ofNullable(params.get(OPERA_PROFILE_ID))
				.ifPresent(operaProfileId -> ThreadContext.put(OPERA_PROFILE_ID, operaProfileId));
	}
}
