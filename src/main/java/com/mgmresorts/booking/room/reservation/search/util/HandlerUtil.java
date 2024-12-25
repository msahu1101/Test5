package com.mgmresorts.booking.room.reservation.search.util;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HandlerUtil {

	/**
	 * This method reads the mgmId and mlife number claims and returns it if the
	 * authorization token is of type guest
	 * 
	 * @param headers Request headers
	 * @return Guest claims
	 */
	public Map<String, String> getUserClaims(Map<String, String> headers) {
		Map<String, String> claims = new HashMap<>();
		JSONObject jObject = convertJwt(headers);
		if (jObject == null) {
			return claims;
		}
		if (!jObject.has(EMPLOYEE_NUMBER)) {
			if (jObject.has(COM_MGM_ID)) {
				claims.put(MGMID, jObject.getString(COM_MGM_ID));
			}
			if (jObject.has(COM_MGM_MLIFE_NUMBER)) {
				claims.put(MLIFE_NUMBER, jObject.getString(COM_MGM_MLIFE_NUMBER));
			}
			if (jObject.has(SUB)) {
				claims.put(SUB, jObject.getString(SUB));
			}
		}
		claims.put(JWT_EXISTS, "true");
		return claims;
	}

	public String getMgmRole(Map<String, String> headers) {
		JSONObject jObject = convertJwt(headers);
		if (jObject == null) {
			return EMPTY;
		}
		if (jObject.has(MGM_ROLE)) {
			return jObject.getString(MGM_ROLE);
		}
		return EMPTY;
	}

	private JSONObject convertJwt(Map<String, String> headers) {
		if (headers.containsKey(AUTHORIZATION)) {
			String[] jwt = headers.get(AUTHORIZATION).split("\\.");
			if (jwt.length == 3) {
				String json = new String(Base64.getUrlDecoder().decode(jwt[1]));
				return new JSONObject(json);	
			}
		}
		return null;
	}
}