package com.mgmresorts.booking.room.reservation.search.logging;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import lombok.experimental.UtilityClass;

/**
 * Utility class to mask JSON data based on keys marked as sensitive
 *
 */
@UtilityClass
public class LogMask {
	// Json keys which are expected to contain sensitive data
	private String[] jsonKeys = { "firstName", "lastName", "cardNumber", "creditCardNumber", "creditCardHolderName", "number", "cvv", "expiry", "cardHolder",
			"userName", "password", "phoneNumbers", "number", "emailAddress1", "street1", "street2", "emailAddress", "email", "street",
			"address1", "address2", "cardHolderName", "billingAddress1", "billingAddress2", "creditCardNumber",
			"creditCardExpireMonth", "creditCardExpireYear", "phone", "paymentToken", "name", "token", "jwtToken", "x-api-key",
			"expirationMonth", "expirationYear", "nameOnCard" , "ocp-apim-subscription-key", "authorization", "genericName", "nameFirst", "nameSur", "eaddress",
			"nameOnCard", "phoneNumber" };
	private static final Pattern jsonPattern = Pattern
			.compile(String.format(PATTERN_REGEX, String.join(REGEX_DELIMITER, jsonKeys)));

	/**
	 * Redact the sensitive information from attributes marked to sensitive in
	 * nature
	 * 
	 * @param message Message to be masked
	 * @return Returns masked message
	 */
	public String mask(String message) {
		StringBuffer buffer = new StringBuffer();
		Matcher matcher = jsonPattern.matcher(message);
		while (matcher.find()) {
			String maskedValue = getMaskedValue(matcher.group(1), matcher.group(2));
			String jsonReplacementRegex = String.format("\"$1\":\"%s\"", maskedValue);
			matcher.appendReplacement(buffer, jsonReplacementRegex);

		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	/**
	 * Redact the sensitive information from map values
	 * 
	 * @param params Map of params
	 * @return Returns update map with redacted values
	 */
	public Map<String, String> mask(Map<String, String> params) {
		Map<String, String> redactedParams = new HashMap<>();
		redactedParams.putAll(params);
		redactedParams.entrySet().forEach(entry -> redactedParams.put(entry.getKey(), getMaskedValue(entry.getKey(), entry.getValue())));

		return redactedParams;
	}

	public String getMaskedValue(String key, String value) {
		if (key.equals("paymentToken") && value.length() > 6) {
			return value.substring(0, 6).concat(StringUtils.repeat('*', value.length() - 6));
		} else if (Arrays.stream(jsonKeys).anyMatch(key::equals)) {
			return StringUtils.repeat('*', value.length());
		} 
		return value;
	}
}
