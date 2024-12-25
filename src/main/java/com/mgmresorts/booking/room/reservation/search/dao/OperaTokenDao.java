package com.mgmresorts.booking.room.reservation.search.dao;

import com.mgmresorts.booking.room.reservation.search.models.opera.OperaTokenRequest;
import com.mgmresorts.booking.room.reservation.search.models.opera.OperaTokenResponse;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface OperaTokenDao {

	@RequestLine("POST /oauth/v1/tokens")
	@Headers({ "Content-Type: application/x-www-form-urlencoded", "x-app-key: {appKey}",
	        "enterpriseId: {enterpriseId}" })
	OperaTokenResponse getOperaServiceToken(OperaTokenRequest request, @Param("appKey") String appKey,
	        @Param("enterpriseId") String enterpriseId);
}