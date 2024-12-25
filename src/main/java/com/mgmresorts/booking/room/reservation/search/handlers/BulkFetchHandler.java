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
 * Http handler class for bulk search API i.e.,
 * booking/v1/reservation/room/batch
 *
 */
public class BulkFetchHandler extends BaseHandler {

	private final SearchService searchService = injectionContext.instanceOf(SearchService.class);

	@Override
	public Optional<Error> validate(Map<String, String> params, HttpRequestMessage<Optional<?>> request) {

		if (StringUtils.isEmpty(params.getOrDefault(HOTEL_CODE, EMPTY))
				|| (StringUtils.isEmpty(params.getOrDefault(CHECKIN_DATE, EMPTY))
						&& StringUtils.isEmpty(params.getOrDefault(CHECKOUT_DATE, EMPTY)))) {
			return Optional.of(new Error("One or more required params are missing"));
		}

		if (StringUtils.isNotEmpty(params.get(MLIFE_NUMBER)) || StringUtils.isNotEmpty(params.get(MGMID))) {
			return Optional.of(new Error("Request is not supported for guest acccess"));
		}
		return Optional.empty();
	}

	/**
	 * Function handler for http request to bulk search API i.e.,
	 * booking/v1/reservation/room/batch
	 * 
	 * @param request
	 *            Http request message
	 * @param context
	 *            Execution context
	 * @return Returns http response message containing document
	 */
	@FunctionName("bulkFetch")
	public HttpResponseMessage bulkFetch(@HttpTrigger(
			name = "req",
			methods = { HttpMethod.GET },
			route = "booking/v1/reservation/room/batch",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<?>> request,
			final ExecutionContext context) {

		return this.handleRequest(request, context, true, true);
	}

	@FunctionName("bulkFetch-apigee")
	public HttpResponseMessage bulkFetchApigee(@HttpTrigger(
			name = "req",
			methods = { HttpMethod.GET },
			route = "v1/reservations/batch",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<?>> request,
			final ExecutionContext context) {

		return this.handleRequest(request, context, true, false);
	}
	
	@FunctionName("batchFetchVerbose")
	public HttpResponseMessage batchFetch(@HttpTrigger(
			name = "req",
			methods = { HttpMethod.GET },
			route = "v1/reservations/batch/verbose",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<?>> request,
			final ExecutionContext context) {

		return this.handleRequest(request, context, true, false);
	}

	public void addToLogContext(Map<String, String> headers, Map<String, String> params) {

		Optional.ofNullable(params.get(HOTEL_CODE)).ifPresent(hotelCode -> ThreadContext.put(HOTEL_CODE, hotelCode));
		Optional.ofNullable(params.get(CHECKIN_DATE))
				.ifPresent(checkInDate -> ThreadContext.put(CHECKIN_DATE, checkInDate));
		Optional.ofNullable(params.get(CHECKOUT_DATE))
				.ifPresent(checkOutDate -> ThreadContext.put(CHECKOUT_DATE, checkOutDate));
	}

	@Override
	public Object handleRequest(Map<String, String> params, ExecutionContext context) {

		if (context.getFunctionName().equals(BATCH_FETCH_VERBOSE)) {
			params.put(VERBOSE, "true");
		}
		return searchService.fetchBulkReservations(params);
	}
}
