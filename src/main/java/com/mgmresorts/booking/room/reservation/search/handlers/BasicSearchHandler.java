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
public class BasicSearchHandler extends BaseHandler {

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

		if (!ValidationUtil.isBasicSearchValid(params)) {
			return Optional.of(new Error("One or more required params are missing"));
		}
		return Optional.empty();
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
	@FunctionName("searchUnprotected")
	public HttpResponseMessage searchUnprotected(@HttpTrigger(
			name = "req",
			methods = { HttpMethod.GET },
			route = "booking/v1/reservation/room/search/basic",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<?>> request,
			final ExecutionContext context) {

		return this.handleRequest(request, context, false, true);
	}
	
	@FunctionName("searchUnprotected-apigee")
	public HttpResponseMessage searchUnprotectedApigee(@HttpTrigger(
			name = "req",
			methods = { HttpMethod.GET },
			route = "v1/reservations/basic",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<?>> request,
												 final ExecutionContext context) {
		return this.handleRequest(request, context, false, false);
	}
	
	@Override
	public Object handleRequest(Map<String, String> params, ExecutionContext context) {

		return searchService.searchReservations(params, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mgmresorts.booking.spring.cloud.handler.
	 * AzureSpringBootHttpQueryHandler#addToLogContext(java.util.Map)
	 */
	@Override
	public void addToLogContext(Map<String, String> headers, Map<String, String> params) {

		Optional.ofNullable(params.get(OPERA_CONF_NUMBER))
				.ifPresent(operaConfNumber -> ThreadContext.put(OPERA_CONF_NUMBER, operaConfNumber));
		Optional.ofNullable(params.get(ID)).ifPresent(id -> ThreadContext.put(ID, id));
		Optional.ofNullable(params.get(CONF_NUMBER))
				.ifPresent(confNumber -> ThreadContext.put(CONF_NUMBER, confNumber));
		Optional.ofNullable(params.get(MGMID)).ifPresent(mgmId -> ThreadContext.put(MGMID, mgmId));
		Optional.ofNullable(params.get(MLIFE_NUMBER))
				.ifPresent(mlifeNumber -> ThreadContext.put(MLIFE_NUMBER, mlifeNumber));
		Optional.ofNullable(params.get(GUEST_MLIFE_NUMBER))
				.ifPresent(guestMlifeNumber -> ThreadContext.put(GUEST_MLIFE_NUMBER, guestMlifeNumber));
		Optional.ofNullable(params.get(OPERA_PROFILE_ID))
				.ifPresent(operaProfileId -> ThreadContext.put(OPERA_PROFILE_ID, operaProfileId));
	}
}
