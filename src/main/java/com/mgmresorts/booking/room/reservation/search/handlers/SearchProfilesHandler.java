package com.mgmresorts.booking.room.reservation.search.handlers;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;

import com.mgmresorts.booking.common.error.Error;
import com.mgmresorts.booking.room.reservation.search.service.SearchService;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

/**
 * Http handler class for search reservations API i.e,
 * booking/v1/reservation/room/profiles
 *
 */
public class SearchProfilesHandler extends BaseHandler {

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

		if (StringUtils.isEmpty(params.getOrDefault(OPERA_CONF_NUMBERS, EMPTY))
				&& StringUtils.isEmpty(params.getOrDefault(IDS, EMPTY))
				&& StringUtils.isEmpty(params.getOrDefault(RESV_NAME_IDS, EMPTY))) {
			return Optional.of(new Error("One or more required params are missing"));
		}

		if (StringUtils.isNotEmpty(params.get(MLIFE_NUMBER)) || StringUtils.isNotEmpty(params.get(MGMID))) {
			return Optional.of(new Error("Request is not supported for guest acccess"));
		}
		
		for (Map.Entry<String, String> entry : params.entrySet()) {
			String value = entry.getValue();
			if (value.split(COMMA).length > 100) {
				return Optional.of(new Error("Search is limited to max of 100"));
			}
		}
		return Optional.empty();
	}

	/**
	 * Function handler for http request for search reservation API i.e.,
	 * booking/v1/reservation/room/profiles
	 * 
	 * @param request
	 *            Http request message
	 * @param context
	 *            Execution context
	 * @return Returns search reservation response
	 */
	@FunctionName("searchProfiles")
	public HttpResponseMessage searchProfiles(@HttpTrigger(
			name = "req",
			methods = { HttpMethod.GET },
			route = "booking/v1/reservation/room/profiles",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<?>> request,
			final ExecutionContext context) {

		return this.handleRequest(request, context, true, true);
	}

	@FunctionName("searchProfiles-apigee")
	public HttpResponseMessage searchProfilesApigee(@HttpTrigger(
			name = "req",
			methods = { HttpMethod.GET },
			route = "v1/reservation/profiles",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<?>> request,
			final ExecutionContext context) {

		return this.handleRequest(request, context, true, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mgmresorts.booking.spring.cloud.handler.
	 * AzureSpringBootHttpQueryHandler#addToLogContext(java.util.Map)
	 */
	@Override
	public void addToLogContext(Map<String, String> headers, Map<String, String> params) {

		Optional.ofNullable(params.get(OPERA_CONF_NUMBERS))
				.ifPresent(operaConfNumbers -> ThreadContext.put(OPERA_CONF_NUMBERS, operaConfNumbers));
		Optional.ofNullable(params.get(IDS)).ifPresent(ids -> ThreadContext.put(IDS, ids));
		Optional.ofNullable(params.get(RESV_NAME_IDS))
				.ifPresent(resvNameIds -> ThreadContext.put(RESV_NAME_IDS, resvNameIds));
	}

	@Override
	public Object handleRequest(Map<String, String> params, ExecutionContext context) {

		return searchService.searchReservationProfiles(params);
	}
}
