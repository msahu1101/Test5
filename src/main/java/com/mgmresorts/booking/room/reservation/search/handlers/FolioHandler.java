package com.mgmresorts.booking.room.reservation.search.handlers;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.ThreadContext;

import com.mgmresorts.booking.common.error.Error;
import com.mgmresorts.booking.room.reservation.search.service.ReservationService;
import com.mgmresorts.booking.room.reservation.search.util.ValidationUtil;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

/**
 * Http handler class for retrieve folio API i.e,
 * booking/v1/reservation/room/folio
 *
 */
public class FolioHandler extends BaseHandler {

	private final ReservationService reservationService = injectionContext.instanceOf(ReservationService.class);

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.mgmresorts.common.handler.AzureSpringBootHttpQueryHandler#validate(
	 * java.util.Map)
	 */
	@Override
	public Optional<Error> validate(Map<String, String> params, HttpRequestMessage<Optional<?>> request) {

		return ValidationUtil.validateFolioRequest(params);
	}

	/**
	 * Function handler for http request for folio retrieval API i.e.,
	 * booking/v1/reservation/room/folio
	 *
	 * @param request
	 *            Http request message
	 * @param context
	 *            Execution context
	 * @return Returns folio response
	 */
	@FunctionName("folio")
	public HttpResponseMessage folio(@HttpTrigger(
			name = "req",
			methods = { HttpMethod.GET },
			route = "v1/reservation/folio",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<?>> request,
									  final ExecutionContext context) {
		return this.handleRequest(request, context, true, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mgmresorts.booking.room.reservation.search.BaseHandler#handleRequest(
	 * java.util.Map, com.microsoft.azure.functions.ExecutionContext)
	 */
	@Override
	public Object handleRequest(Map<String, String> params, ExecutionContext context) {

		return reservationService.fetchFolio(params);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.mgmresorts.booking.spring.cloud.handler.
	 * AzureSpringBootHttpQueryHandler#addToLogContext(java.util.Map)
	 */
	@Override
	public void addToLogContext(Map<String, String> headers, Map<String, String> params) {

		Optional.ofNullable(params.get(CONF_NUMBER))
				.ifPresent(confNumber -> ThreadContext.put(CONF_NUMBER, confNumber));
	}
}
