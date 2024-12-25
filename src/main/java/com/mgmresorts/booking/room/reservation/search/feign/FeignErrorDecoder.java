package com.mgmresorts.booking.room.reservation.search.feign;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.mgmresorts.booking.common.error.exception.FunctionalException;
import com.mgmresorts.booking.common.error.exception.SystemException;

import feign.Response;
import feign.codec.ErrorDecoder;

public class FeignErrorDecoder implements ErrorDecoder {

	@Override
	public Exception decode(String methodKey, Response response) {

		String responseBody = StringUtils.EMPTY;
		try {
			responseBody = IOUtils.toString(response.body().asReader());
		} catch (IOException e) {
			// No handle
		}
		
		if (response.status() == 400) {
			return new FunctionalException("_invalid_request", responseBody);
		} else {
			return new SystemException("_system_error", responseBody);
		}

	}
}