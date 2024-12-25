package com.mgmresorts.booking.room.reservation.search.response;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.mgmresorts.booking.common.error.Error;
import com.mgmresorts.booking.common.error.exception.DataNotFoundException;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpResponseMessage.Builder;
import com.microsoft.azure.functions.HttpStatus;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ResponseAdaptor {

	public static HttpResponseMessage getResponse(HttpRequestMessage<?> request, Object o, boolean addSecurityHeaders) {

		if (o instanceof String && StringUtils.isEmpty(o.toString())) {
			Builder responseBuilder = request.createResponseBuilder(HttpStatus.NO_CONTENT);
			setCommonHeaders(responseBuilder, addSecurityHeaders);
			return responseBuilder.header(CONTENT_TYPE, APP_JSON).build();
		} else if (o instanceof ResponseWithHeaders) {
			ResponseWithHeaders response = (ResponseWithHeaders) o;
			Builder responseBuilder = request.createResponseBuilder(HttpStatus.OK);
			setCommonHeaders(responseBuilder, addSecurityHeaders);
			Set<String> keys = response.getHeaders().keySet();
			for (String key : keys) {
				responseBuilder = responseBuilder.header(key, response.getHeaders().getOrDefault(key, ""));
			}
			return responseBuilder.body(response.getResponse()).build();
		} else {
			Builder responseBuilder = request.createResponseBuilder(HttpStatus.OK);
			setCommonHeaders(responseBuilder, addSecurityHeaders);
			return responseBuilder.body(o).build();
		}

	}

	public static HttpResponseMessage getErrorResponse(HttpRequestMessage<?> request, Throwable t, boolean addSecurityHeaders) {
		Error error = new Error(t.getMessage(), t.getClass().getSimpleName());
		Builder responseBuilder = request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR);
		setCommonHeaders(responseBuilder, addSecurityHeaders);
		return responseBuilder.body(error).build();
	}

	public static HttpResponseMessage getErrorResponse(HttpRequestMessage<?> request, Error error, boolean addSecurityHeaders) {
		if (StringUtils.isEmpty(error.getCode())) {
			error.setCode(INVALID_REQUEST);
		}
		Builder responseBuilder = request.createResponseBuilder(HttpStatus.BAD_REQUEST);
		setCommonHeaders(responseBuilder, addSecurityHeaders);
		return responseBuilder.body(error).build();
	}

    private void setCommonHeaders(Builder builder, boolean addSecurityHeaders) {
        builder.header(CONTENT_TYPE, APP_JSON);
        
        if (addSecurityHeaders) {
            // Security headers
            builder.header(HEADER_STRICT_TRANSPORT_SECURITY, STRICT_TRANSPORT_SECURITY_DEFAULT);
            builder.header(HEADER_CONTENT_SECURITY_POLICY, CONTENT_SECURITY_POLICY_DEFAULT);
            builder.header(HEADER_X_CONTENT_TYPE_OPTIONS, X_CONTENT_TYPE_OPTIONS_DEFAULT);
            builder.header(HEADER_X_XSS_PROTECTION, X_XSS_PROTECTION_DEFAULT);
        }

    }

	public static HttpResponseMessage getErrorResponse(HttpRequestMessage<?> request, DataNotFoundException ex) {
		Error error = new Error(ex.getMessage(), ex.getCode());
		return request.createResponseBuilder(HttpStatus.NOT_FOUND).header(CONTENT_TYPE, APP_JSON).body(error).build();
	}
}
