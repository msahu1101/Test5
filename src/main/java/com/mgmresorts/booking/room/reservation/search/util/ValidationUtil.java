package com.mgmresorts.booking.room.reservation.search.util;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.mgmresorts.booking.common.error.exception.FunctionalException;
import com.mgmresorts.booking.common.error.Error;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@Log4j2
@UtilityClass
public class ValidationUtil {

	public boolean isSearchValid(Map<String, String> params) {

		if (!params.getOrDefault(NAME_SEARCH_OP, FULL).equalsIgnoreCase(FULL)
				&& params.getOrDefault(FIRST_NAME, EMPTY).length() < 3
				&& params.getOrDefault(LAST_NAME, EMPTY).length() < 3) {
			log.debug("When using non-full name search, any of first and last name should be more than 3 chars");
			return false;
		}

		return (StringUtils.isNotEmpty(params.getOrDefault(OPERA_CONF_NUMBER, EMPTY))
				|| StringUtils.isNotEmpty(params.getOrDefault(ID, EMPTY))
				|| StringUtils.isNotEmpty(params.getOrDefault(CONF_NUMBER, EMPTY))
				|| StringUtils.isNotEmpty(params.getOrDefault(MGMID, EMPTY))
				|| StringUtils.isNotEmpty(params.getOrDefault(MLIFE_NUMBER, EMPTY))
				|| StringUtils.isNotEmpty(params.getOrDefault(PARTNER_ACCOUNT_NUMBER, EMPTY))
				|| StringUtils.isNotEmpty(params.getOrDefault(GUEST_MLIFE_NUMBER, EMPTY))
				|| StringUtils.isNotEmpty(params.getOrDefault(OPERA_PROFILE_ID, EMPTY))
				|| (StringUtils.isNotEmpty(params.getOrDefault(ROOM_NUMBER, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(CHECKIN_DATE, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(HOTEL_CODE, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(FIRST_NAME, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(CHECKIN_DATE, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(HOTEL_CODE, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(FIRST_NAME, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY))
						&& StringUtils.isNotEmpty(
								params.getOrDefault(ARRIVAL_RANGE_START, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(ARRIVAL_RANGE_END, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(HOTEL_CODE, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(ROOM_NUMBER, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(CHECKIN_DATE, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(CC_LAST_4, EMPTY))));
	}

	public boolean isBasicSearchValid(Map<String, String> params) {

		if (!params.getOrDefault(NAME_SEARCH_OP, FULL).equalsIgnoreCase(FULL)
				&& params.getOrDefault(FIRST_NAME, EMPTY).length() < 3
				&& params.getOrDefault(LAST_NAME, EMPTY).length() < 3) {
			log.info("When using non-full name search, any of first and last name should be more than 3 chars");
			return false;
		}

		if (!params.getOrDefault(FIRST_NAME_MATCH, FULL).equalsIgnoreCase(FULL)
				|| !params.getOrDefault(LAST_NAME_MATCH, "FULL").equalsIgnoreCase(FULL)) {
			log.info("Should not be able to use fuzzy name for basic search");
			return false;
		}

		return ((StringUtils.isNotEmpty(params.getOrDefault(OPERA_CONF_NUMBER, EMPTY))
				&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(ID, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(CONF_NUMBER, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(MGMID, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(MLIFE_NUMBER, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(GUEST_MLIFE_NUMBER, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(OPERA_PROFILE_ID, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(ROOM_NUMBER, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(CHECKIN_DATE, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(HOTEL_CODE, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(FIRST_NAME, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(CHECKIN_DATE, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(HOTEL_CODE, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(FIRST_NAME, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY))
						&& StringUtils.isNotEmpty(
								params.getOrDefault(ARRIVAL_RANGE_START, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(ARRIVAL_RANGE_END, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(HOTEL_CODE, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(ROOM_NUMBER, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(CHECKIN_DATE, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(CC_LAST_4, EMPTY)))
				|| (StringUtils.isNotEmpty(params.getOrDefault(HOTEL_CODE, EMPTY))
						&& StringUtils.isNotEmpty(params.getOrDefault(ROOM_NUMBER, EMPTY))));
	}

	public Optional<Error> validateFolioRequest(Map<String, String> params) {
		boolean isValidSearchByConfNumber = StringUtils.isNotEmpty(params.getOrDefault(JWT_EXISTS, EMPTY))
				&& StringUtils.isNotEmpty(params.getOrDefault(CONF_NUMBER, EMPTY));

		boolean isValidSearchByOtherParams = StringUtils.isNotEmpty(params.getOrDefault(HOTEL_CODE, EMPTY))
				&& StringUtils.isNotEmpty(params.getOrDefault(LAST_NAME, EMPTY))
				&& StringUtils.isNotEmpty(params.getOrDefault(CC_LAST_4, EMPTY));

		if (!isValidSearchByConfNumber && !isValidSearchByOtherParams) {
			return Optional.of(new Error("One or more required params are missing"));
		}

		if (isValidSearchByConfNumber && !isValidSearchByOtherParams) {
			return ensureNoAnonymousJwtRole(params.get(MGM_ROLE));
		}
		return Optional.empty();
	}

	public boolean isTrueNumber(String number) {

		if (StringUtils.isBlank(number)) {
			return false;
		}
		try {
			new BigInteger(number);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public Optional<Error> validateInputData(Map<String, String> params) {

		try {
			if (params.containsKey(OPERA_CONF_NUMBER)) {
				validateNumericValue(params.get(OPERA_CONF_NUMBER), "Opera confirmation number must be numeric and non-empty");
			}
			if (params.containsKey(ID)) {
				validateGuid(params.get(ID), "Id must be non-empty and in valid GUID format");
			}
			if (params.containsKey(HOTEL_CODE)) {
				validateHotelCode(params.get(HOTEL_CODE));
			}
			if (params.containsKey(ROOM_NUMBER)) {
				validateNumericValue(params.get(ROOM_NUMBER), "Room number must be numeric and non-empty");
			}
			if (params.containsKey(CC_LAST_4)) {
				validateCClast4Digits(params.get(CC_LAST_4));
			}
			if (params.containsKey(MGMID)) {
				validateAlphanumericValue(params.get(MGMID), "mgmId must be alphanumeric and non-empty");
			}
			if (params.containsKey(MLIFE_NUMBER)) {
				validateNumericValue(params.get(MLIFE_NUMBER), "MLife number must be numeric and non-empty");
			}
			if (params.containsKey(OPERA_PROFILE_ID)) {
				validateNumericValue(params.get(OPERA_PROFILE_ID), "Opera profile id must be numeric and non-empty");
			}
			if (params.containsKey(CHECKIN_DATE)) {
				validateDate(params.get(CHECKIN_DATE), "Check-in date must be non-empty and in yyyy-MM-dd format with valid digits");
			}
			if (params.containsKey(CHECKOUT_DATE)) {
				validateDate(params.get(CHECKOUT_DATE), "Check-out date must be non-empty and in yyyy-MM-dd format with valid digits");
			}
			if (params.containsKey(OPERA_CONF_NUMBERS)) {
				validateMultipleNumericValues(params.get(OPERA_CONF_NUMBERS), "Opera confirmation numbers must be numeric and non-empty");
			}
			if (params.containsKey(IDS)) {
				validateMultipleGuids(params.get(IDS), "Ids must be non-empty and in valid GUID format");
			}
			if (params.containsKey(RESV_NAME_IDS)) {
				validateMultipleNumericValues(params.get(RESV_NAME_IDS), "ResvNameIds must be numeric and non-empty");
			}
		} catch (FunctionalException e) {
			return Optional.of(new Error(e.getMessage(), e.getCode()));
		}
		return Optional.empty();
	}

	public Optional<Error> ensureServiceOrEmployeeRole(String role) {

		if (!role.equals(SERVICE_ROLE) && !role.equals(EMPLOYEE_ROLE)) {
			return Optional.of(new Error("Forbidden"));
		}
		return Optional.empty();
	}

	public Optional<Error> ensureNoAnonymousJwtRole(String role) {

		if (role.equals(ANONYMOUS_ROLE)) {
			return Optional.of(new Error("Forbidden"));
		}
		return Optional.empty();
	}

	private void validateMultipleNumericValues(String values, String validationMessage) {

		if (StringUtils.isEmpty(values)) {
			throw new FunctionalException(INVALID_FORMAT, validationMessage);
		}
		String[] valueArr = values.split(COMMA);
		for (String value : valueArr) {
			validateNumericValue(value.trim(), validationMessage);
		}
	}

	private void validateMultipleGuids(String guids, String validationMessage) {

		if (StringUtils.isEmpty(guids)) {
			throw new FunctionalException(INVALID_FORMAT, validationMessage);
		}
		String[] guidsArr = guids.split(COMMA);
		for (String guid : guidsArr) {
			validateGuid(guid.trim(), validationMessage);
		}
	}

	private void validateAlphanumericValue(String alphanumericValue, String validationMessage) {

		if (StringUtils.isEmpty(alphanumericValue) || !StringUtils.isAlphanumeric(alphanumericValue)) {
			throw new FunctionalException(INVALID_FORMAT, validationMessage);
		}
	}

	private void validateGuid(String guid, String validationMessage) {

		if (StringUtils.isEmpty(guid) || guid.length() != ID_REQUIRED_LENGTH || (!guid.matches(
				GUID_REGEX))) {
			throw new FunctionalException(INVALID_FORMAT, validationMessage);
		}
	}

	private void validateHotelCode(String hotelCode) {

		if (StringUtils.isEmpty(hotelCode) || hotelCode.length() != ServiceConstants.HOTEL_CODE_REQUIRED_LENGTH
				|| !StringUtils.isNumeric(hotelCode)) {
			throw new FunctionalException(INVALID_FORMAT,
					"Hotel code must have a length of 3 and contain only digits");
		}
	}

	private void validateCClast4Digits(String ccLast4Digits) {

		if (StringUtils.isEmpty(ccLast4Digits)
				|| ccLast4Digits.length() != ServiceConstants.CC_LAST_FOUR_REQUIRED_LENGTH
				|| !StringUtils.isNumeric(ccLast4Digits)) {
			throw new FunctionalException(INVALID_FORMAT,
					"ccLast4Digits must be numeric and contain 4 digits");
		}
	}

	private void validateDate(String date, String validationMessage) {

		if (StringUtils.isEmpty(date) || !date.matches(DATE_REGEX)) {
			throw new FunctionalException(INVALID_FORMAT,
			validationMessage);
		}
	}

	private void validateNumericValue(String numericValue, String validationMessage) {

		if (StringUtils.isEmpty(numericValue) || !StringUtils.isNumeric(numericValue)) {
			throw new FunctionalException(INVALID_FORMAT,
			validationMessage);
		}
	}
}
