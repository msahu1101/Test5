package com.mgmresorts.booking.room.reservation.search.logging;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiveSiteEvent {

	private String level;
	private String logTimestamp;
	private String message;
	private Map<String, String> headers;
	private Map<String, Object> requestData;
	private Map<String, Object> responseData;
	private Map<String, String> customer;
	private Map<String, String> ext;
	private int status;
	private String correlationId;
	private String logId;
	private String containerId;
	private String host;
	private String env;
	private int duration;
	private String domain;
	private String source;
	private String version;
	private String path;
	private String property;
	private Map<String, String> financialImpact;
}
