package com.mgmresorts.booking.room.reservation.search.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.ThreadContext;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgmresorts.booking.room.reservation.search.util.ServiceConstants;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;

import lombok.Data;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@UtilityClass
@Log4j2
public @Data class LiveSiteLogger {

	private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	private final ObjectMapper mapper = new ObjectMapper();
	private final EventHubProducerClient ehClient = new EventHubClientBuilder()
			.connectionString(System.getenv("livesiteEventsEventhubConnection")).buildProducerClient();
	private final ExecutorService liveSiteExecutor = Executors.newCachedThreadPool();

	public <T> Map<String, Object> getRequestData(HttpRequestMessage<T> request) {

		Map<String, Object> requestData = new HashMap<>();
		requestData.put("path", request.getUri().getPath());
		requestData.put("httpMethod", request.getHttpMethod().name());
		requestData.put("query", LogMask.mask(request.getQueryParameters()));

		return requestData;
	}

	public <T> void sendEvent(HttpRequestMessage<T> request, HttpResponseMessage response, int duration,
			ExecutionContext context) {

		final Map<String, String> values = ThreadContext.getImmutableContext();

		liveSiteExecutor.submit(() -> {
			try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.putAll(values)) {
				sendEventToEventHub(request, response, duration, context);
			}
		});
	}

	private <T> void sendEventToEventHub(HttpRequestMessage<T> request, HttpResponseMessage response, int duration,
			ExecutionContext context) {

		log.debug("Creating live site event hub message");
		TimeZone tz = TimeZone.getTimeZone("UTC");
		df.setTimeZone(tz);
		String ts = df.format(new Date());

		int status = response.getStatusCode();
		final String domain = System.getenv("domain");
		final String env = System.getenv(ServiceConstants.APP_PROFILE);
		final String source = System.getenv("source");
		final String logLevel = status >= 500 ? "ERROR" : "INFO";
		final String containerId = System.getenv("COMPUTERNAME");
		final String host = System.getenv("WEBSITE_HOSTNAME");
		final String version = "1.0";
		final Map<String, String> queryParams = cleanParams(request.getQueryParameters());

		LiveSiteEvent event = LiveSiteEvent.builder().level(logLevel).logTimestamp(ts).headers(getRequestHeaders(request))
				.requestData(getRequestData(request)).responseData(getResponseData(response))
				.path(request.getUri().getPath()).status(status).duration(duration)
				.correlationId(context.getInvocationId()).message("NA")
				.logId(domain + "-" + UUID.randomUUID() + "_" + System.currentTimeMillis()).domain(domain)
				.source(source).env(env).containerId(containerId).host(host).version(version)
				.customer(getCustomerData(queryParams)).property(getProperty(queryParams)).financialImpact(new HashMap<>())
				.build();

		sendToEventHub(event);
	}

	private void sendToEventHub(LiveSiteEvent event) {

		String eventString = convertToString(event);
		log.debug("Sending to live site event hub: {}", LogMask.mask(eventString));

		try {
			EventData eventData = new EventData(eventString);
			EventDataBatch eventDataBatch = ehClient.createBatch();

			if (!eventDataBatch.tryAdd(eventData)) {
				log.warn("Event is too large for an empty batch. Max size: {}", eventDataBatch.getMaxSizeInBytes());
			}
			if (eventDataBatch.getCount() > 0) {
				ehClient.send(eventDataBatch);
				log.debug("Send to live site EH complete");
			}
		} catch (Throwable t) {
			log.error("Error occurred while sending batch", t);
		}
	}

	private <T> Map<String, String> getRequestHeaders(HttpRequestMessage<T> request) {

		Map<String, String> headers = new HashMap<>(request.getHeaders());
		headers.remove("authorization");
		headers.remove("ocp-apim-subscription-key");
		headers.remove("x-api-key");

		return headers;
	}

	private Map<String, Object> getResponseData(HttpResponseMessage response) {

		Map<String, Object> requestData = new HashMap<>();
		int status = response.getStatusCode();
		requestData.put("status", String.valueOf(status));

		if (status >= 400) {
			requestData.put("error", response.getBody());
		}
		return requestData;
	}

	private String convertToString(Object event) {

		try {
			return mapper.writeValueAsString(event);
		} catch (JsonProcessingException e) {
			log.error("Exception in live site event convertToString", e);
		}
		return null;
	}

	private Map<String, String> cleanParams(Map<String, String> queryParams) {

		Map<String, String> cleanParams = new HashMap<>();
		queryParams.keySet().forEach(key -> cleanParams.put(key, StringEscapeUtils.escapeHtml4(queryParams.get(key))));

		return cleanParams;
	}

	private Map<String, String> getCustomerData(Map<String, String> queryParams) {

		Map<String, String> customer = new HashMap<>();
		if (queryParams.containsKey("mlifeNumber")) {
			customer.put("mlifeNumber", queryParams.get("mlifeNumber"));
		}

		if (queryParams.containsKey("mgmId")) {
			customer.put("mgmId", queryParams.get("mgmId"));
		}

		return customer;
	}

	private String getProperty(Map<String, String> queryParams) {

		return queryParams.getOrDefault("hotelCode", "NA");
	}
}
