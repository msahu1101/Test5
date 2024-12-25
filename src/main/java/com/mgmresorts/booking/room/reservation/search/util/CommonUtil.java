package com.mgmresorts.booking.room.reservation.search.util;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mgmresorts.booking.room.oxi.models.Reservation;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

/**
 * Utility class providing common utility functions required for the project
 *
 */
@UtilityClass
@Log4j2
public final class CommonUtil {

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false).setSerializationInclusion(Include.NON_NULL)
			.setSerializationInclusion(Include.NON_EMPTY);

	/**
	 * Applies the specified mask for all but last 4 digits/chars.
	 * 
	 * @param strText
	 *            Text to be masked
	 * @param maskChar
	 *            Char to use for mask
	 * @return Returns masked number
	 */
	public static String maskCardNumber(String strText, char maskChar) {

		if (StringUtils.isEmpty(strText) || strText.length() < 4) {
			return strText;
		}

		int index = 0;
		StringBuilder maskedNumber = new StringBuilder();
		for (int i = 0; i < strText.length(); i++) {

			if (i < strText.length() - 4) {
				maskedNumber.append(maskChar);
			} else {
				maskedNumber.append(strText.charAt(index));
			}

			index++;
		}

		// return the masked number
		return maskedNumber.toString();
	}

	/**
	 * returns string representing last 4 of provided cardNumber
	 *
	 * @param cardNumber
	 *            card number
	 * @return Returns last 4 digits as string
	 */
	public static String getCardLast4(String cardNumber) {

		if (StringUtils.isEmpty(cardNumber) || cardNumber.length() < 4) {
			return cardNumber;
		}
		return StringUtils.substring(cardNumber, cardNumber.length() - 4, cardNumber.length());
	}

	/**
	 * Converts list of objects to json string.
	 *
	 * @param documentList
	 *            List of objects
	 * @return Returns json string
	 */
	public static String convertToJson(Object documentList) {

		// Convert list of documents into json string
		try {
			return MAPPER.writeValueAsString(documentList);
		} catch (JsonProcessingException e) {
			log.error("Error converting to JSON", e);
		}

		return null;
	}

	/**
	 * Converts a JSON string to requested object
	 * 
	 * @param json
	 *            Json String
	 * @param classType
	 *            Class type to be populated
	 * @return Returns object populated with deserialized json
	 */
	public static <T> T convertToObj(String json, Class<T> classType) {

		try {
			return MAPPER.readValue(json, classType);
		} catch (JsonProcessingException e) {
			log.warn("Error converting to Object", e);
		}

		return null;
	}

	/**
	 * Converts string representation of a list of reservations to an array of Reservation objects
	 *
	 * @param reservation
	 *            string list of reservations
	 * @return Returns Reservation array
	 */
	public static Reservation[] convertToReservationList(String reservation) throws JsonProcessingException {
		try {
			return MAPPER.readValue(reservation, Reservation[].class);
		} catch (JsonProcessingException e) {
			log.error("Error converting from JSON string to reservation object", e);
			throw e;
		}
	}

	/**
	 * Method returns same card type codes as used by OWS by converting OXI
	 * types into OWS types. Currently, visa is the only card which is different
	 * from OXI vs OWS.
	 * 
	 * @param cardType
	 *            Card type code
	 * @return Returns converted card type code
	 */
	public static String getStandardCardType(String cardType) {
		if (StringUtils.isEmpty(cardType)) {
			return StringUtils.EMPTY;
		} else {
			return cardType.equals("VI") ? "VS" : cardType;
		}
	}

	/**
	 * Method redacts the first name and last name from cosmos sql query
	 * 
	 * @param query
	 *            Cosmos SQL query
	 * @return Returns redacted query to be used for logging
	 */
	public static String maskQuery(String query) {
		String updatedQuery = new String(query.getBytes());
		updatedQuery = updatedQuery.replaceAll("individualName.nameSur=\"([^\"]*)\"",
				"individualName.nameSur=\"--REDACTED--\"");
		updatedQuery = updatedQuery.replaceAll("individualName.nameFirst=\"([^\"]*)\"",
				"individualName.nameFirst=\"--REDACTED--\"");

		return updatedQuery;
	}

	/**
	 * Performs algorithm for finding longest common prefix among array of strings.
	 * After sorting a list of strings, a natural common prefix can be found by comparing the first and
	 * last string in array.  This is because sorting of strings is done character wise and similar prefixes
	 * are valued the same, unlike integers which are sorted by total value.
	 *
	 * @param descriptions array of strings to be processed
	 * @return common prefix shared among descriptions
	 */
	public static String findLongestCommonPrefix(String[] descriptions) {
		String[] arr = descriptions;
		int size = arr.length;

		// The longest common prefix of an empty array is "".
		if (size == 0) {
			return "";

		// The longest common prefix of an array containing
		// only one element is that element itself.
		} else if (size == 1) {
			return arr[0];
		} else {
			// We only need to compare first and last string in a sorted list to identify common prefix
			Arrays.sort(arr);
			int length = arr[0].length();
			StringBuilder responseBuilder = new StringBuilder();
			StringBuilder wordBuilder = new StringBuilder();

			// Compare the first and the last strings character
			// by character.
			for (int i = 0; i < length; i++) {

				// If the characters match, append the character to the result.
				if (arr[0].charAt(i) == arr[size - 1].charAt(i)) {

					// if we've reached a space, this is potentially the end of a word.
					if (arr[0].charAt(i) == ' ') {
						// if the description starts with a space, don't include it
						if (i != 0) {
							wordBuilder.append(arr[0].charAt(i));
							responseBuilder.append(wordBuilder.toString());
							wordBuilder.setLength(0);
						}
					} else {
						// add letter to the word
						wordBuilder.append(arr[0].charAt(i));
					}
					// Else, stop the comparison.
				} else {
					// only return what's been added as a full word so far
					return responseBuilder.toString();
				}
			}
			// we traversed entire string properly, so add the last word seen
			return responseBuilder.append(wordBuilder.toString()).toString();
		}
	}

	/**
	 * Removes common prefix from each token and returns string representation of the aggregated
	 * string containing unique suffixes appended to prefix by delimiter
	 *
	 * @param tokens string tokens representing fields to be aggregated
	 * @param prefix common prefix among tokens
	 * @return
	 */
	public static String removeCommonPrefixAndAppend(String[] tokens, String prefix) {
		String finalValue = prefix.trim();
		String delimiter = " ";
		String delimiter1 = "";

		for (String token : tokens) {
			if (prefix.equals("")) {
				finalValue = finalValue.concat(delimiter1 + token);
				delimiter1 = ", ";
			} else {
				finalValue = finalValue.concat(delimiter + token.substring(prefix.length()).trim());
				delimiter = ", ";
			}
		}

		return finalValue;
	}

	/**
	 * Checks whether an object is null or not
	 *
	 * @param objs
	 * 			Object to be checked
	 * @return Returns boolean value
	 */
	public static boolean isNull(Object... objs) {
		for (Object t : objs) {
			if (t == null) {
				return true;
			}
		}
		return false;
	}

	public static boolean isBasicSearchFunction(String functionName) {
		return functionName.startsWith(ServiceConstants.BASIC_SEARCH_FUNCTION_NAME);
	}

	/** Truncates a string of comma delimited text.
     *
     * If text represents a singleton string, that string is returned.
     *
     * If text has multiple entries separated by comma, return truncated string by reducing field to
     * longest common prefix with each unique suffix appended by delimiter.
     *
     * @param text
     * @return truncated string
     */
    public static String truncateText(String text) {
        String[] tokens = StringUtils.split(text, ServiceConstants.COMMA);
        if (tokens.length > 1) {
            String[] trimmedTokens = Arrays.stream(tokens).map(String::trim).toArray(String[]::new);
            String longestCommonPrefix = findLongestCommonPrefix(trimmedTokens);
            return removeCommonPrefixAndAppend(trimmedTokens, longestCommonPrefix);
        } else {
            return tokens[0];
        }
    }
}
