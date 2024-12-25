package com.mgmresorts.booking.room.reservation.search.handlers;

import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class HealthHandler {

	@FunctionName("health")
	public HttpResponseMessage health(@HttpTrigger(
			name = "req",
			methods = { HttpMethod.GET },
			route = "v1/health",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context) {

		return request.createResponseBuilder(HttpStatus.OK).build();
	}
}
