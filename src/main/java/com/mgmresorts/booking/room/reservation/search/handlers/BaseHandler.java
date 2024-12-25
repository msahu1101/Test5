package com.mgmresorts.booking.room.reservation.search.handlers;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.ThreadContext;

import com.mgmresorts.booking.common.error.Error;
import com.mgmresorts.booking.common.error.exception.DataNotFoundException;
import com.mgmresorts.booking.common.error.exception.FunctionalException;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.inject.InjectionContext;
import com.mgmresorts.booking.room.reservation.search.logging.LiveSiteLogger;
import com.mgmresorts.booking.room.reservation.search.logging.LogMask;
import com.mgmresorts.booking.room.reservation.search.models.BaseResponse;
import com.mgmresorts.booking.room.reservation.search.response.ResponseAdaptor;
import com.mgmresorts.booking.room.reservation.search.util.CommonUtil;
import com.mgmresorts.booking.room.reservation.search.util.HandlerUtil;
import com.mgmresorts.booking.room.reservation.search.util.ServiceConstants;
import com.mgmresorts.booking.room.reservation.search.util.ValidationUtil;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class BaseHandler {

	private TelemetryClient telemetryClient = new TelemetryClient();

	/**
	 * Injector Context.
	 */
	protected final InjectionContext injectionContext = InjectionContext.get();

	private final AppProperties appProps = injectionContext.instanceOf(AppProperties.class);

	public abstract Optional<Error> validate(Map<String, String> input, HttpRequestMessage<Optional<?>> request);

	public abstract void addToLogContext(Map<String, String> headers, Map<String, String> input);

	public abstract Object handleRequest(Map<String, String> params, ExecutionContext context);

	public HttpResponseMessage handleRequest(HttpRequestMessage<Optional<?>> request, ExecutionContext context,
			boolean addUserClaims, boolean addSecurityHeaders) {

		ThreadContext.put(ServiceConstants.INVOCATION_ID, context.getInvocationId());
		ThreadContext.put(ServiceConstants.FUNCTION_NAME, context.getFunctionName());

		Map<String, String> headers = request.getHeaders();
		ThreadContext.put(ServiceConstants.X_MGM_CORRELATION_ID,
				Optional.ofNullable(headers.get(ServiceConstants.X_MGM_CORRELATION_ID))
						.orElse(headers.get(ServiceConstants.X_CORRELATION_ID)));
		ThreadContext.put(ServiceConstants.X_MGM_JOURNEY_ID, Optional
				.ofNullable(headers.get(ServiceConstants.X_MGM_JOURNEY_ID)).orElse(UUID.randomUUID().toString()));
		String channel = headers.getOrDefault(ServiceConstants.X_MGM_CHANNEL, ServiceConstants.UNKNOWN_CHANNEL_NAME);
		ThreadContext.put(ServiceConstants.X_MGM_CHANNEL, channel);
		String mgmRole = HandlerUtil.getMgmRole(headers);
		ThreadContext.put(ServiceConstants.MGM_ROLE, mgmRole);

		final BaseResponse baseResponse = new BaseResponse();
		HttpResponseMessage httpResponseMessage = null;
		StopWatch watch = new StopWatch();
		watch.start();

		Map<String, String> reqParams = cleanParams(request.getQueryParameters());
		reqParams.put(ServiceConstants.MGM_ROLE, mgmRole);
		reqParams.put(ServiceConstants.X_MGM_CHANNEL, channel);

		boolean isBasicSearch = CommonUtil
		        .isBasicSearchFunction(Optional.ofNullable(context.getFunctionName()).orElse(EMPTY));
		if (addUserClaims || isBasicSearch) {
			setUserClaims(headers, reqParams, isBasicSearch);
		}

		Optional<Error> validationError = validate(reqParams, request);
		Optional<Error> inputDataValidationError = ValidationUtil.validateInputData(reqParams);
		addToLogContext(headers, reqParams);

		log.info("Request received by function {}: {}", context.getFunctionName(),
				LiveSiteLogger.getRequestData(request));

		if (validationError.isPresent()) {
			httpResponseMessage = ResponseAdaptor.getErrorResponse(request, validationError.get(), addSecurityHeaders);
		} else if (inputDataValidationError.isPresent()) {
			httpResponseMessage = ResponseAdaptor.getErrorResponse(request, inputDataValidationError.get(),
					addSecurityHeaders);
		} else {
			try {
				Object response = handleRequest(reqParams, context);
				httpResponseMessage = ResponseAdaptor.getResponse(request, response, addSecurityHeaders);
			} catch (FunctionalException ex) {
				log.warn(ex.getMessage(), ex);
				httpResponseMessage = ResponseAdaptor.getErrorResponse(request,
						new Error(ex.getMessage(), ex.getCode()), addSecurityHeaders);
			} catch (DataNotFoundException ex) {
				log.warn(ex.getMessage(), ex);
				httpResponseMessage = ResponseAdaptor.getErrorResponse(request, ex);
			} catch (Exception ex) {
				telemetryClient.trackException(ex);
				log.error(ex.getMessage(), ex);
				httpResponseMessage = ResponseAdaptor.getErrorResponse(request, ex, addSecurityHeaders);
			}
		}

		// todo: make overrideable to truncate bulk response text - it is truncated by logging sdk currently
		Object body = httpResponseMessage.getBody();
		Optional.ofNullable(System.getenv(ServiceConstants.LOG_RESPONSE)).filter(Boolean::parseBoolean)
				.ifPresent(logResponse -> log.info("Response sent from function {}: {}", context.getFunctionName(),
						LogMask.mask(StringEscapeUtils.unescapeJson(CommonUtil.convertToJson(body)))));

		baseResponse.setResponseMsg(httpResponseMessage);

		LiveSiteLogger.sendEvent(request, baseResponse.getResponseMsg(), (int) watch.getTime(), context);

		ThreadContext.clearAll();

		return baseResponse.getResponseMsg();
	}

	private Map<String, String> cleanParams(Map<String, String> queryParams) {

		Map<String, String> cleanParams = new HashMap<>();
		queryParams.keySet().forEach(key -> {
			if (appProps.getAllowedParams().contains(key)) {
				cleanParams.put(key, StringEscapeUtils.escapeHtml4(queryParams.get(key)));
			}
		});

		return cleanParams;
	}

	private void setUserClaims(Map<String, String> headers, Map<String, String> reqParams, boolean isBasicSearch) {

		Map<String, String> userClaims = HandlerUtil.getUserClaims(headers);
		if (isBasicSearch) {
			reqParams.put(KIOSK_CLIENT_ID, userClaims.getOrDefault(SUB, EMPTY));
		} else {
			reqParams.putAll(userClaims);

			// Handling pagination header
			if (headers.containsKey(ServiceConstants.CONTINUATION_TOKEN)) {
				reqParams.put(ServiceConstants.CONTINUATION_TOKEN, headers.get(ServiceConstants.CONTINUATION_TOKEN));
			} else if (headers.containsKey(ServiceConstants.CONTINUATION_TOKEN_LOWER)) {
				reqParams.put(ServiceConstants.CONTINUATION_TOKEN,
				        headers.get(ServiceConstants.CONTINUATION_TOKEN_LOWER));
			}
		}
	}
}
