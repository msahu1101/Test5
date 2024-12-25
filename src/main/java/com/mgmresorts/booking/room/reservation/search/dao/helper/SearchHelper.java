package com.mgmresorts.booking.room.reservation.search.dao.helper;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.WordUtils;

import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.util.DateTimeUtil;

import lombok.experimental.UtilityClass;

/**
 * Helper class which creates document search query based on the params
 * provided.
 *
 */
@UtilityClass
public class SearchHelper {

    /**
     * Creates select clause for the reservation search query by joining
     * sub-objects required
     * 
     * @param params
     *            Input params
     * @return Returns select clause for the search query
     */
    private String createQuerySelect(Map<String, String> params) {

        StringBuilder querySelect = new StringBuilder("SELECT distinct value r FROM ROOT r ");

        if (params.containsKey(FIRST_NAME) || params.containsKey(LAST_NAME) || params.containsKey(EMAIL)
                || params.containsKey(GUEST_MLIFE) || params.containsKey(OPERA_PROFILE_ID) || params.containsKey(PARTNER_ACCOUNT_NUMBER)) {
            querySelect.append("JOIN p IN r.resProfiles.resProfile ");
        }
        if (params.containsKey(MLIFE_NUMBER) && !params.containsKey(MGMID)) {
            querySelect.append("JOIN m IN r.selectedMemberships.selectedMembership ");
        }
        if (params.containsKey(GUEST_MLIFE) || params.containsKey(PARTNER_ACCOUNT_NUMBER)) {
            querySelect.append("JOIN gm IN p.profile.memberships.membership ");
    }
        if (params.containsKey(CHECKIN_KEY) || params.containsKey(CHECKOUT_KEY) || params.containsKey(START_DATE)
                || params.containsKey(END_DATE) || params.containsKey(RESV_STATUS) || params.containsKey(ROOM_NUMBER)
                || params.containsKey(MARKET_CODES) || params.containsKey(ROOM_TYPE)) {
            querySelect.append("JOIN rs IN r.roomStays.roomStay ");
        }
        if (params.containsKey(CHECKOUT_KEY)) {
            querySelect.append("JOIN rg IN r.resGuests.resGuest ");
        }
        if (params.containsKey(EMAIL)) {
            querySelect.append("JOIN em IN p.profile.electronicAddresses.electronicAddress ");
        }
        if (params.containsKey(CONF_NUMBER) || params.containsKey(OPERA_CONF_NUMBER)) {
            // removing some join clauses if it's already added
            if (params.containsKey(CHECKOUT_KEY)) {
                querySelect.append("JOIN rr IN rg.reservationReferences.reservationReference ");
            } else {
                querySelect.append(
                    "JOIN rg IN r.resGuests.resGuest JOIN rr IN rg.reservationReferences.reservationReference ");
            }
        }
        if (params.containsKey(LAST_4_DIGITS_CC)) {
            querySelect.append("JOIN cc IN r.resCreditCards.resCreditCard ");
        }
        return querySelect.toString();
    }

    /**
     * Create and return search query based on the params supplied.
     * 
     * @param params
     *            Input params to be used for search criteria
     * @param appProps
     *            Application configuration properties
     * @return Returns search query based on params supplied
     */
    public SqlQuerySpec createSearchQuerySpec(Map<String, String> params, AppProperties appProps) {

        normalizeNameEmail(params);
        StringBuilder query = new StringBuilder(createQuerySelect(params) + "WHERE ");
        List<SqlParameter> queryParams = new ArrayList<>();
        
        updateQueryForConfNumberAndLastName(params, query, queryParams);

        updateQueryForStayDates(params, appProps, query, queryParams);

        updateQueryForOperaConfNumber(params, query, queryParams);

        updateQueryForConfNumber(params, query, queryParams);

        updateQueryForName(params, query, queryParams);

        updateQueryForMember(params, query, queryParams);

        updateQueryForPartnerAccount(params, query, queryParams);
        
        updateQueryForGuestMlife(params, query, queryParams);

        updateQueryForDateRange(params, appProps, query, queryParams);

        updateQueryForReservationStatus(params, query, queryParams);
        
        updateQueryForRoomNumber(params, query, queryParams);

        updateQueryForLast4DigitsCc(params, query, queryParams);
        
        updateQueryForOperaProfileId(params, query, queryParams);

        updateQueryForMarketCodes(params, query, queryParams);

        updateQueryForRoomType(params, query, queryParams);

        updateQueryForBookingDate(params, query, queryParams);
        
        Iterator<String> keySet = params.keySet().iterator();
        Map<String, String> clauseParams = new TreeMap<>();

        // Generic mechanism to capture other params
        while (keySet.hasNext()) {
            String key = keySet.next();
            if (appProps.getKeys().containsKey(key)) {
                clauseParams.put(appProps.getKeys().get(key), StringEscapeUtils.unescapeHtml4(params.get(key)));
            }
        }

        // Add primary flag check when email condition is required
        if (params.containsKey(EMAIL)) {
            clauseParams.put("em.mfPrimaryYN", YES);
        }

        // Generic loop to add other params to where clause
        keySet = clauseParams.keySet().iterator();
        while (keySet.hasNext()) {

            String key = keySet.next();
            String[] keyParts = key.split("\\.");
            String keyParam = "@" + keyParts[keyParts.length - 1];
            query.append(key).append("=").append(keyParam);

            if (queryParams.stream().noneMatch(p -> p.getName().equals(keyParam))) {
                queryParams.add(new SqlParameter(keyParam,  clauseParams.get(key)));
            }

            if (keySet.hasNext()) {
                query.append(CLAUSE_APPEND);
            }

        }

        // trim spaces before returning the query
        String queryText = query.toString().trim().replaceAll(" +", " ");

        return new SqlQuerySpec(queryText, queryParams);
    }
    
	public SqlQuerySpec createInHouseSearchQuerySpec(Map<String, String> params) {

		StringBuilder query = new StringBuilder(
				"SELECT distinct value r FROM ROOT r JOIN rs IN r.roomStays.roomStay WHERE ");
		List<SqlParameter> sqlParameters = new ArrayList<>();

		String hotelCode = params.get(HOTEL_CODE);
		String roomNumber = params.get(ROOM_NUMBER);

		query.append("r.hotelReference.hotelCode = ").append(HOTEL_CODE_PARAM);
		query.append(" AND rs.roomID = ").append(ROOM_NUMBER_PARAM);
		query.append(" AND rs.reservationStatusType = \"INHOUSE\"");

		sqlParameters.add(new SqlParameter(HOTEL_CODE_PARAM, hotelCode));
		sqlParameters.add(new SqlParameter(ROOM_NUMBER_PARAM, roomNumber));

		return new SqlQuerySpec(query.toString(), sqlParameters);
	}

    /**
     * Converts email to all lowercase and converts first and last name to
     * capitalize. Using this normalization helps in performing case insensitive
     * search from saved documents.
     * 
     * @param params
     *            Input params
     */
    private void normalizeNameEmail(Map<String, String> params) {

        // convert email to lowercase
        params.computeIfPresent(EMAIL, (key, val) -> StringUtils.lowerCase(val));
        // convert first name and last name to capitilize
        params.computeIfPresent(FIRST_NAME, (key, val) -> WordUtils.capitalizeFully(val));
        params.computeIfPresent(LAST_NAME, (key, val) -> WordUtils.capitalizeFully(val));
    }
    
	private void updateQueryForConfNumberAndLastName(Map<String, String> params, StringBuilder query,
			List<SqlParameter> queryParams) {

		if (params.containsKey(CONF_NUMBER) && params.containsKey(LAST_NAME) && params.size() == 2) {
			// convert confNumber to uppercase
			String confNumberUpper = StringUtils.upperCase(params.get(CONF_NUMBER), Locale.ENGLISH);
			String confNumberLower = StringUtils.lowerCase(params.get(CONF_NUMBER), Locale.ENGLISH);

			String lastName = StringEscapeUtils.unescapeHtml4(params.get(LAST_NAME));

			// Look for either of opera or internal confirmation number
			query.append("(r.reservationID=").append(CONF_NUMBER_UPPER_PARAM).append(" or r.reservationID=")
					.append(CONF_NUMBER_LOWER_PARAM).append(" or (rr.referenceNumber=").append(CONF_NUMBER_UPPER_PARAM)
					.append(" or rr.referenceNumber=").append(CONF_NUMBER_LOWER_PARAM).append(")")
					.append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.reservationID=")
					.append(CONF_NUMBER_UPPER_PARAM).append(" AND ag.lastName=").append(LAST_NAME_PARAM).append("))");
			queryParams.add(new SqlParameter(CONF_NUMBER_UPPER_PARAM, confNumberUpper));
			queryParams.add(new SqlParameter(CONF_NUMBER_LOWER_PARAM, confNumberLower));
			params.remove(CONF_NUMBER);

			query.append(" AND ((p.profile.individualName.nameSur=").append(LAST_NAME_PARAM)
					.append(" AND p.profile.profileType=\"GUEST\")")
					.append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.reservationID=")
					.append(CONF_NUMBER_UPPER_PARAM).append(" AND ag.lastName=").append(LAST_NAME_PARAM).append("))");
			queryParams.add(new SqlParameter(LAST_NAME_PARAM, lastName));
			params.remove(LAST_NAME);

			if (!params.isEmpty()) {
				query.append(CLAUSE_APPEND);
			}
		}
	}

    private void updateQueryForStayDates(Map<String, String> params, AppProperties appProps, StringBuilder query,
            List<SqlParameter> queryParams) {

        if (params.containsKey(CHECKIN_KEY)) {
            // If check-in date is available, add date check condition
            long checkInMin = DateTimeUtil.getEpochMillis(params.get(CHECKIN_KEY));
            long checkInMax = DateTimeUtil.getEpochMillis(params.get(CHECKIN_KEY), 1);

            query.append(appProps.getKeys().get(CHECKIN_KEY)).append(">=").append(CHECKIN_MIN_PARAM).append(CLAUSE_APPEND);
            query.append(appProps.getKeys().get(CHECKIN_KEY)).append("<").append(CHECKIN_MAX_PARAM).append(CLAUSE_APPEND);
            queryParams.add(new SqlParameter(CHECKIN_MIN_PARAM, checkInMin));
            queryParams.add(new SqlParameter(CHECKIN_MAX_PARAM, checkInMax));

            params.remove(CHECKIN_KEY);
        }

        if (params.containsKey(CHECKOUT_KEY)) {

            // if check-out date is available, construct the date range check by
            // using time unit attribute in document
            long checkOutMin = DateTimeUtil.getEpochMillis(params.get(CHECKOUT_KEY));
            long checkOutMax = DateTimeUtil.getEpochMillis(params.get(CHECKOUT_KEY), 1);

            query.append("rg.reservationID=r.reservationID AND ");
            query.append(appProps.getKeys().get(CHECKOUT_KEY)).append(">=").append(CHECKOUT_MIN_PARAM).append(CLAUSE_APPEND);
            query.append(appProps.getKeys().get(CHECKOUT_KEY)).append("<").append(CHECKOUT_MAX_PARAM).append(CLAUSE_APPEND);
            queryParams.add(new SqlParameter(CHECKOUT_MIN_PARAM, checkOutMin));
            queryParams.add(new SqlParameter(CHECKOUT_MAX_PARAM, checkOutMax));

            params.remove(CHECKOUT_KEY);
        }
    }

    private void updateQueryForOperaConfNumber(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        if (params.containsKey(OPERA_CONF_NUMBER)) {

            // convert confNumber to uppercase
            String confNumber = params.get(OPERA_CONF_NUMBER);

            // Look for either of primary or secondary opera confirmation number
            query.append("(r.reservationID=").append(OPERA_CONF_NUMBER_PARAM)
                .append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.reservationID=")
                .append(OPERA_CONF_NUMBER_PARAM).append("))");
            queryParams.add(new SqlParameter(OPERA_CONF_NUMBER_PARAM, confNumber));
            params.remove(OPERA_CONF_NUMBER);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForConfNumber(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        if (params.containsKey(CONF_NUMBER)) {

            // convert confNumber to uppercase
            String confNumberUpper = StringUtils.upperCase(params.get(CONF_NUMBER), Locale.ENGLISH);
            String confNumberLower = StringUtils.lowerCase(params.get(CONF_NUMBER), Locale.ENGLISH);

            // Look for either of opera or internal confirmation number
            query.append("(r.reservationID=").append(CONF_NUMBER_UPPER_PARAM).append(" or r.reservationID=")
                .append(CONF_NUMBER_LOWER_PARAM).append(" or (rr.referenceNumber=").append(CONF_NUMBER_UPPER_PARAM)
                .append(" or rr.referenceNumber=").append(CONF_NUMBER_LOWER_PARAM).append(")")
                .append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.reservationID=")
                .append(CONF_NUMBER_UPPER_PARAM).append("))");
            queryParams.add(new SqlParameter(CONF_NUMBER_UPPER_PARAM, confNumberUpper));
            queryParams.add(new SqlParameter(CONF_NUMBER_LOWER_PARAM, confNumberLower));
            params.remove(CONF_NUMBER);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForName(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {

        if (params.containsKey(NAME_SEARCH_OPERATION) && params.get(NAME_SEARCH_OPERATION).equalsIgnoreCase("STARTSWITH")) {
            updateQueryForStartsWithNameSearch(params, query, queryParams);
        } else if (params.containsKey(FIRST_NAME_MATCH) && params.containsKey(LAST_NAME_MATCH)) {
            updateQueryForNameMatchSearch(params, query, queryParams);
        } else {
            updateQueryForFullNameSearch(params, query, queryParams);
        }
        params.remove(NAME_SEARCH_OPERATION);
        params.remove(FIRST_NAME_MATCH);
        params.remove(LAST_NAME_MATCH);
    }

    private void updateQueryForFullNameSearch(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        if (params.containsKey(FIRST_NAME)) {

            String firstName = StringEscapeUtils.unescapeHtml4(params.get(FIRST_NAME));

            query.append("((p.profile.individualName.nameFirst=").append(FIRST_NAME_PARAM)
                .append(" AND p.profile.profileType=\"GUEST\")")
                .append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.firstName=")
                .append(FIRST_NAME_PARAM).append("))");
            queryParams.add(new SqlParameter(FIRST_NAME_PARAM, firstName));
            params.remove(FIRST_NAME);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }

        if (params.containsKey(LAST_NAME)) {

            String lastName = StringEscapeUtils.unescapeHtml4(params.get(LAST_NAME));

            query.append("((p.profile.individualName.nameSur=").append(LAST_NAME_PARAM)
                    .append(" AND p.profile.profileType=\"GUEST\")")
                    .append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=")
                    .append(LAST_NAME_PARAM).append("))");
            queryParams.add(new SqlParameter(LAST_NAME_PARAM, lastName));
            params.remove(LAST_NAME);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForStartsWithNameSearch(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        if (params.containsKey(FIRST_NAME)) {

            String firstName = StringEscapeUtils.unescapeHtml4(params.get(FIRST_NAME));

            query.append("((STARTSWITH(p.profile.individualName.nameFirst,").append(FIRST_NAME_PARAM)
                .append(",true) AND p.profile.profileType=\"GUEST\")")
                .append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where STARTSWITH(ag.firstName,")
                .append(FIRST_NAME_PARAM).append(",true)))");
            queryParams.add(new SqlParameter(FIRST_NAME_PARAM, firstName));
            params.remove(FIRST_NAME);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }

        if (params.containsKey(LAST_NAME)) {

            String lastName = StringEscapeUtils.unescapeHtml4(params.get(LAST_NAME));

            query.append("((STARTSWITH(p.profile.individualName.nameSur,").append(LAST_NAME_PARAM)
                .append(",true) AND p.profile.profileType=\"GUEST\")")
                .append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where STARTSWITH(ag.lastName,")
                .append(LAST_NAME_PARAM).append(",true)))");
            queryParams.add(new SqlParameter(LAST_NAME_PARAM, lastName));
            params.remove(LAST_NAME);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForNameMatchSearch(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        if (params.containsKey(FIRST_NAME) && params.containsKey(LAST_NAME)) {

            String firstName = StringEscapeUtils.unescapeHtml4(params.get(FIRST_NAME));
            String lastName = StringEscapeUtils.unescapeHtml4(params.get(LAST_NAME));

            if(params.get(FIRST_NAME_MATCH).equalsIgnoreCase("LIKE")) {

                query.append("((CONTAINS(p.profile.individualName.nameFirst,").append(FIRST_NAME_PARAM)
                    .append(",true) AND p.profile.profileType=\"GUEST\")")
                    .append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where CONTAINS(ag.firstName,")
                    .append(FIRST_NAME_PARAM).append(",true)))");
            } else {

                query.append("((p.profile.individualName.nameFirst=").append(FIRST_NAME_PARAM)
                    .append(" AND p.profile.profileType=\"GUEST\")")
                    .append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.firstName=")
                    .append(FIRST_NAME_PARAM).append("))");
            }

            query.append (CLAUSE_APPEND);

            if(params.get(LAST_NAME_MATCH).equalsIgnoreCase("LIKE")) {

                query.append("((CONTAINS(p.profile.individualName.nameSur,").append(LAST_NAME_PARAM)
                    .append(",true) AND p.profile.profileType=\"GUEST\")")
                    .append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where CONTAINS(ag.lastName,")
                    .append(LAST_NAME_PARAM).append(",true)))");

            } else {

                query.append("((p.profile.individualName.nameSur=").append(LAST_NAME_PARAM)
                    .append(" AND p.profile.profileType=\"GUEST\")")
                    .append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.lastName=")
                    .append(LAST_NAME_PARAM).append("))");
            }

            queryParams.add(new SqlParameter(FIRST_NAME_PARAM, firstName));
            queryParams.add(new SqlParameter(LAST_NAME_PARAM, lastName));

            params.remove(FIRST_NAME);
            params.remove(LAST_NAME);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }
    
    private void updateQueryForOperaProfileId(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        if (params.containsKey(OPERA_PROFILE_ID)) {

            String operaProfileId = params.get(OPERA_PROFILE_ID);

            query.append("p.profile.mfResortProfileID=" + OPERA_PROFILE_ID_PARAM + " AND p.profile.profileType=\"GUEST\"");
            queryParams.add(new SqlParameter(OPERA_PROFILE_ID_PARAM, operaProfileId));
            params.remove(OPERA_PROFILE_ID);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForMember(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        boolean updateDone = false;
        if (params.containsKey(MLIFE_NUMBER) && params.containsKey(MGMID)) {

            // Look for either of mgmId or mlife number
            query.append("(r.mgmProfile.mgmId=").append(MGMID_PARAM)
                .append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.mgmId=")
                .append(MGMID_PARAM).append(")")
                .append(" or EXISTS(select value m from m in r.selectedMemberships.selectedMembership where m.accountID=")
                .append(MLIFE_NUMBER_PARAM).append(" AND m.programCode=\"PC\"))");
            queryParams.add(new SqlParameter(MGMID_PARAM, params.get(MGMID)));
            queryParams.add(new SqlParameter(MLIFE_NUMBER_PARAM, params.get(MLIFE_NUMBER)));
            updateDone = true;
        } else if (params.containsKey(MLIFE_NUMBER)) {
            query.append("(m.accountID=").append(MLIFE_NUMBER_PARAM).append(" AND m.programCode=\"PC\")");
            queryParams.add(new SqlParameter(MLIFE_NUMBER_PARAM, params.get(MLIFE_NUMBER)));
            updateDone = true;
        } else if (params.containsKey(MGMID)) {
            query.append("(r.mgmProfile.mgmId=").append(MGMID_PARAM)
                .append(" or EXISTS(select value ag from ag in r.mgmProfile.additionalGuests where ag.mgmId=")
                .append(MGMID_PARAM).append("))");
            queryParams.add(new SqlParameter(MGMID_PARAM, params.get(MGMID)));
            updateDone = true;
        }

        params.remove(MLIFE_NUMBER);
        params.remove(MGMID);

        if (updateDone && !params.isEmpty()) {
            query.append(CLAUSE_APPEND);
        }
    }
    
    private void updateQueryForGuestMlife(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {

        if (params.containsKey(GUEST_MLIFE)) {

            query.append("(gm.accountID=").append(GUEST_MLIFE_PARAM)
                .append(" AND gm.programCode=\"PC\" AND NOT IS_DEFINED(gm.mfInactiveDate))");
            queryParams.add(new SqlParameter(GUEST_MLIFE_PARAM, params.get(GUEST_MLIFE)));

            params.remove(GUEST_MLIFE);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForPartnerAccount(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {

        if (params.containsKey(PARTNER_ACCOUNT_NUMBER)) {

            String programCode = params.getOrDefault(PROGRAM_CODE, DEFAULT_PROGRAM_CODE);

            query.append("(gm.accountID=").append(PARTNER_ACCOUNT_NUMBER_PARAM)
                    .append(String.format(" AND gm.programCode=%s AND NOT IS_DEFINED(gm.mfInactiveDate))",
                            PROGRAM_CODE_PARAM));
            queryParams.add(new SqlParameter(PARTNER_ACCOUNT_NUMBER_PARAM, params.get(PARTNER_ACCOUNT_NUMBER)));
            queryParams.add(new SqlParameter(PROGRAM_CODE_PARAM, programCode));

            params.remove(PARTNER_ACCOUNT_NUMBER);

            if (params.containsKey(PROGRAM_CODE)) {
                params.remove(PROGRAM_CODE);
            }

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForDateRange(Map<String, String> params, AppProperties appProps, StringBuilder query,
            List<SqlParameter> queryParams) {

        if (params.containsKey(START_DATE)) {

            long checkIn = DateTimeUtil.getEpochMillis(params.get(START_DATE));
            query.append(appProps.getKeys().get(START_DATE)).append(">=").append(START_DATE_PARAM);
            queryParams.add(new SqlParameter(START_DATE_PARAM, checkIn));
            params.remove(START_DATE);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }

        if (params.containsKey(END_DATE)) {

            // Using minus 1 to exclude next day to be included
            long checkIn = DateTimeUtil.getEpochMillis(params.get(END_DATE), 1) - 1;
            query.append(appProps.getKeys().get(END_DATE)).append("<=").append(END_DATE_PARAM);
            queryParams.add(new SqlParameter(END_DATE_PARAM, checkIn));
            params.remove(END_DATE);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForReservationStatus(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        if (params.containsKey(RESV_STATUS)) {

            String resvStatus = params.get(RESV_STATUS);
            Object[] resvStatusArr = Arrays.stream(resvStatus.split(COMMA)).map(s -> s.trim().toUpperCase(Locale.ENGLISH)).toArray();

            query.append("ARRAY_CONTAINS(").append(RESV_STATUS_PARAM).append(", rs.reservationStatusType)");
            queryParams.add(new SqlParameter(RESV_STATUS_PARAM, resvStatusArr));
            params.remove(RESV_STATUS);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForRoomNumber(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        if (params.containsKey(ROOM_NUMBER)) {

            String roomNumber = params.get(ROOM_NUMBER);

            query.append("rs.roomID=").append(ROOM_NUMBER_PARAM);
            queryParams.add(new SqlParameter(ROOM_NUMBER_PARAM, roomNumber));
            params.remove(ROOM_NUMBER);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForLast4DigitsCc(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        if (params.containsKey(LAST_4_DIGITS_CC)) {

            String last4DigitsCC = params.get(LAST_4_DIGITS_CC);

            query.append("cc.creditCard.mfPrimaryYN=\"Y\" AND ENDSWITH(cc.creditCard.creditCardNumber, ").append(LAST_4_DIGITS_CC_PARAM).append(")");
            queryParams.add(new SqlParameter(LAST_4_DIGITS_CC_PARAM, last4DigitsCC));
            params.remove(LAST_4_DIGITS_CC);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForMarketCodes(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        if (params.containsKey(MARKET_CODES)) {

            String marketCodes = params.get(MARKET_CODES);
            Object[] marketCodesArr = Arrays.stream(marketCodes.split(COMMA)).map(c -> c.trim().toUpperCase(Locale.ENGLISH)).toArray();
            
            query.append("ARRAY_CONTAINS(").append(MARKET_CODES_PARAM).append(", rs.marketSegmentCode)");
            queryParams.add(new SqlParameter(MARKET_CODES_PARAM, marketCodesArr));
            params.remove(MARKET_CODES);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForRoomType(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        if (params.containsKey(ROOM_TYPE)) {

            String roomType = params.get(ROOM_TYPE);

            query.append("rs.roomInventoryCode=").append(ROOM_TYPE_PARAM);
            queryParams.add(new SqlParameter(ROOM_TYPE_PARAM, roomType));

            params.remove(ROOM_TYPE);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    private void updateQueryForBookingDate(Map<String, String> params, StringBuilder query,
            List<SqlParameter> queryParams) {
        if (params.containsKey(BOOKING_DATE)) {

            long bookingDateStart = DateTimeUtil.getEpochMillis(params.get(BOOKING_DATE));
            long bookingDateEnd = DateTimeUtil.getEpochMillis(params.get(BOOKING_DATE), 1);

            query.append("r.originalBookingDate>=").append(BOOKING_DATE_START_PARAM);
            query.append(CLAUSE_APPEND);
            query.append("r.originalBookingDate<=").append(BOOKING_DATE_END_PARAM);
            queryParams.add(new SqlParameter(BOOKING_DATE_START_PARAM, bookingDateStart));
            queryParams.add(new SqlParameter(BOOKING_DATE_END_PARAM, bookingDateEnd));

            params.remove(BOOKING_DATE);

            if (!params.isEmpty()) {
                query.append(CLAUSE_APPEND);
            }
        }
    }

    /**
     * Create and return query to search by reservation identifiers like id,
     * reservationID, resvNameId based on the params supplied.
     * 
     * @param params
     *            Input params to be used for search criteria
     * @param appProps
     *            Application configuration properties
     * @return Returns search query based on params supplied
     */
    public SqlQuerySpec createSearchByIdsQuerySpec(Map<String, String> params) {

        String query = "SELECT value r FROM ROOT r WHERE ARRAY_CONTAINS(" + IDS_PARAM + ", " + ID_ATTR + ")";
        String ids;

        if (params.containsKey(OPERA_CONF_NUMBERS)) {
            query = query.replace(ID_ATTR, "r.reservationID");
            ids = params.get(OPERA_CONF_NUMBERS);

        } else if (params.containsKey(RESV_NAME_IDS)) {
            query = query.replace(ID_ATTR, "r.resvNameId");
            ids = params.get(RESV_NAME_IDS);
        } else {
            query = query.replace(ID_ATTR, "r.id");
            ids = params.get(IDS);
        }

        List<SqlParameter> queryParams = new ArrayList<>();
        Object[] idsArr = Arrays.stream(ids.split(COMMA)).map(String::trim).toArray();
        queryParams.add(new SqlParameter(IDS_PARAM, idsArr));

        return new SqlQuerySpec(query, queryParams);
    }
}
