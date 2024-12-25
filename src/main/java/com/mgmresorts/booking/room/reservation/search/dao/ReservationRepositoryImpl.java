package com.mgmresorts.booking.room.reservation.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.dao.helper.OperaDbPoolDataSourceFactory;
import com.mgmresorts.booking.room.reservation.search.util.CommonUtil;
import com.mgmresorts.booking.room.reservation.search.util.ServiceConstants;
import com.mgmresorts.booking.room.reservation.search.util.ValidationUtil;

import lombok.extern.log4j.Log4j2;
import oracle.ucp.jdbc.PoolDataSource;

@Log4j2
public class ReservationRepositoryImpl implements ReservationRepository {

	private PoolDataSource poolDataSource;

	@Inject
	public ReservationRepositoryImpl(AppProperties appProps) {

		log.info("Creating instance: ReservationRepository");

		// todo: only instantiate if any of the properties not fetching from opera cloud
		// if (!Boolean.parseBoolean(System.getenv(ServiceConstants.FETCH_FOLIO_DETAILS_FROM_OPERA_CLOUD))) {
			poolDataSource = OperaDbPoolDataSourceFactory.getPoolDataSource(appProps);
		// }
	}

	@Override
	public Map<String, Object> getFolioDetails(String resvNameId, String resort, String folioViews, Boolean aggregated)
			throws SQLException {

		Map<String, Object> resultMap;
		PreparedStatement preparedStatement = null;
		Connection dbConnection = null;

		try {
			dbConnection = this.poolDataSource.getConnection();

			String sql = "select "
					+ "fintrans.ROOM, fintrans.FOLIO_VIEW, fintrans.TRX_DATE, fintrans.TRANSACTION_DESCRIPTION, "
					+ "fintrans.GUEST_ACCOUNT_DEBIT, fintrans.GUEST_ACCOUNT_CREDIT, fintrans.REMARK, ncc.CREDIT_CARD_NUMBER_4_DIGITS, "
					+ "fintrans.QUANTITY, fintrans.TRX_NO, fintrans.FOLIO_NO, fintrans.REFERENCE, fintrans.UPDATE_DATE "
					+ "from " + "OPERA.FINANCIAL_TRANSACTIONS_VIEW fintrans, OPERA.NAME$_CREDIT_CARD ncc " + "where "
					+ "fintrans.RESV_NAME_ID = ? and fintrans.RESORT = ? and fintrans.FOLIO_VIEW in (?) "
					+ "and not(fintrans.GUEST_ACCOUNT_DEBIT is null and fintrans.GUEST_ACCOUNT_CREDIT is null) "
					+ "and not(fintrans.GUEST_ACCOUNT_DEBIT = 0 and fintrans.GUEST_ACCOUNT_CREDIT = 0) "
					+ "and fintrans.CREDIT_CARD_ID = ncc.CREDIT_CARD_ID(+) " + "order by fintrans.TRX_NO";

			preparedStatement = dbConnection.prepareStatement(sql);
			preparedStatement.setString(1,  resvNameId);
			preparedStatement.setString(2,  resort);
			preparedStatement.setString(3,  folioViews);
			resultMap = buildResultMap(preparedStatement, aggregated);
		} catch (Exception e) {
			log.error("Failed to retrieve folio details from Opera DB", e);
			throw e;
		} finally {
			DbUtils.closeQuietly(preparedStatement);
			DbUtils.closeQuietly(dbConnection);
			dbConnection = null;
		}
		return resultMap;
	}

	private Object[] createLine(ResultSet rs) throws SQLException {

		Object[] createdLine = new Object[13];
		createdLine[0] = rs.getString(1);
		createdLine[1] = rs.getInt(2);
		createdLine[2] = rs.getString(3);
		createdLine[3] = rs.getString(4);
		createdLine[4] = rs.getString(5);
		createdLine[5] = rs.getString(6);
		createdLine[6] = rs.getString(7);
		createdLine[7] = rs.getString(8);
		createdLine[8] = rs.getString(9);
		createdLine[9] = rs.getString(10);
		createdLine[10] = rs.getString(11);
		createdLine[11] = rs.getString(12);
		createdLine[12] = rs.getString(13);

		return createdLine;
	}

	/**
	 * Appends new entries to column containers for row representation
	 *
	 * @param room container for column
	 * @param folioView container for column
	 * @param trxDate container for column
	 * @param guestAccountDebit container for column
	 * @param guestAccountCredit container for column
	 * @param remark container for column
	 * @param creditCardNumber4Digits container for column
	 * @param quantity container for column
	 * @param trxNo container for column
	 * @param transactionDescription container for column
	 * @param folioNo container for column
	 * @param updateDate container for date
	 * @param line container for the row
	 */
	private void addLineToArrays(List<String> room, List<Integer> folioView, List<String> trxDate,
			List<String> guestAccountDebit, List<String> guestAccountCredit, List<String> remark,
			List<String> creditCardNumber4Digits, List<String> quantity, List<String> trxNo,
			List<String> transactionDescription, List<String> folioNo, List<String> reference, List<String> updateDate,
			Object[] line) {

		room.add((String) line[0]);
		folioView.add((Integer) line[1]);
		trxDate.add((String) line[2]);
		transactionDescription.add((String) line[3]);
		guestAccountDebit.add((String) line[4]);
		guestAccountCredit.add((String) line[5]);
		remark.add((String) line[6]);
		creditCardNumber4Digits.add((String) line[7]);
		quantity.add((String) line[8]);
		trxNo.add((String) line[9]);
		folioNo.add((String) line[10]);
		reference.add((String) line[11]);
		updateDate.add((String) line[12]);
	}

	private String addCharges(String charge1, String charge2) {

		double value1 = 0.0;
		double value2 = 0.0;

		if (null != charge1 && !charge1.equals(ServiceConstants.NULL)) {
			value1 = Double.parseDouble(charge1);
		}
		if (null != charge2 && !charge2.equals(ServiceConstants.NULL)) {
			value2 = Double.parseDouble(charge2);
		}
		return String.valueOf(value1 + value2);
	}

	private Map<String, Object> buildResultMap(PreparedStatement preparedStatement, Boolean aggregated) throws SQLException {

		List<String> room = new ArrayList<>();
		List<Integer> folioView = new ArrayList<>();
		List<String> trxDate = new ArrayList<>();
		List<String> guestAccountDebit = new ArrayList<>();
		List<String> guestAccountCredit = new ArrayList<>();
		List<String> remark = new ArrayList<>();
		List<String> creditCardNumber4Digits = new ArrayList<>();
		List<String> quantity = new ArrayList<>();
		List<String> trxNo = new ArrayList<>();
		List<String> transactionDescription = new ArrayList<>();
		List<String> folioNo = new ArrayList<>();
		List<String> reference = new ArrayList<>();
		List<String> updateDate = new ArrayList<>();

		// fetch result set
		try (ResultSet rs = preparedStatement.executeQuery()) {

			Object[] previousLineAggregate = new Object[13];
			Object[] line;
			boolean trueNumberReference;

			while (rs.next()) {
				line = createLine(rs);
				trueNumberReference = ValidationUtil.isTrueNumber((String) line[11]);
				String referenceStr = (String) line[11];
				String date = (String) line[2];

				// if aggregated flag is false, return rows from opera without aggregating
				if (!aggregated) {

					addLineToArrays(room, folioView, trxDate, guestAccountDebit, guestAccountCredit, remark,
							creditCardNumber4Digits, quantity, trxNo, transactionDescription, folioNo, reference, updateDate, line);

				// valid reference number and it's a continuation of the aggregate
				} else if (trueNumberReference && referenceStr.equals(previousLineAggregate[11])
						&& date.equals(previousLineAggregate[2])) {

					// concat descriptions for post processing
					previousLineAggregate[3] = ((String) previousLineAggregate[3]).concat(ServiceConstants.COMMA + line[3]);
					previousLineAggregate[12] = ((String) previousLineAggregate[12]).concat(ServiceConstants.COMMA + line[12]);

					// add costs and add debits to previousLineAggregate
					if (((String) previousLineAggregate[4]).isEmpty()) {
						previousLineAggregate[5] = addCharges((String) previousLineAggregate[5], (String) line[5]);
					} else {
						previousLineAggregate[4] = addCharges((String) previousLineAggregate[4], (String) line[4]);
					}

				// valid reference number that marks the start of a new aggregation
				} else if (trueNumberReference) {
					if (previousLineAggregate[3] != null) {
						previousLineAggregate[3] = CommonUtil.truncateText((String) previousLineAggregate[3]);
						previousLineAggregate[12] = truncateUpdateDates(previousLineAggregate);

						// add previousLineAggregate to arrays
						addLineToArrays(room, folioView, trxDate, guestAccountDebit, guestAccountCredit, remark,
								creditCardNumber4Digits, quantity, trxNo, transactionDescription, folioNo, reference, updateDate,
								previousLineAggregate);
					}
					// current line becomes previous aggregate line
					previousLineAggregate = line;

				// invalid reference number
				} else {
					if (previousLineAggregate[3] != null) {
						previousLineAggregate[3] = CommonUtil.truncateText((String) previousLineAggregate[3]);
						previousLineAggregate[12] = truncateUpdateDates(previousLineAggregate);
						addLineToArrays(room, folioView, trxDate, guestAccountDebit, guestAccountCredit, remark,
								creditCardNumber4Digits, quantity, trxNo, transactionDescription, folioNo, reference, updateDate,
								previousLineAggregate);
					}
					addLineToArrays(room, folioView, trxDate, guestAccountDebit, guestAccountCredit, remark,
							creditCardNumber4Digits, quantity, trxNo, transactionDescription, folioNo, reference, updateDate, line);
					previousLineAggregate = new Object[13];
				}
			}

			// add tail-case
			if (previousLineAggregate[0] != null) {
				previousLineAggregate[3] = CommonUtil.truncateText((String) previousLineAggregate[3]);
				previousLineAggregate[12] = truncateUpdateDates(previousLineAggregate);
				addLineToArrays(room, folioView, trxDate, guestAccountDebit, guestAccountCredit, remark,
						creditCardNumber4Digits, quantity, trxNo, transactionDescription, folioNo, reference, updateDate,
						previousLineAggregate);
			}
		}

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put(ServiceConstants.ROOM, room);
		resultMap.put(ServiceConstants.FOLIO_VIEW, folioView);
		resultMap.put(ServiceConstants.TRX_DATE, trxDate);
		resultMap.put(ServiceConstants.TRANSACTION_DESCRIPTION, transactionDescription);
		resultMap.put(ServiceConstants.GUEST_ACCOUNT_DEBIT, guestAccountDebit);
		resultMap.put(ServiceConstants.GUEST_ACCOUNT_CREDIT, guestAccountCredit);
		resultMap.put(ServiceConstants.REMARK, remark);
		resultMap.put(ServiceConstants.CREDIT_CARD_NUMBER_4_DIGITS, creditCardNumber4Digits);
		resultMap.put(ServiceConstants.QUANTITY, quantity);
		resultMap.put(ServiceConstants.TRX_NO, trxNo);
		resultMap.put(ServiceConstants.FOLIO_NO, folioNo);
		resultMap.put(ServiceConstants.REFERENCE, reference);
		resultMap.put(ServiceConstants.UPDATE_DATE, updateDate);

		return resultMap;
	}

	/**
	 * Similar to above method except with transaction update_date.
	 *
	 * In this method, a string of dates is tokenized and then the latest date is extracted through iterative
	 * conversion to Date object.
	 *
	 * @param line line array of values representing a row from opera resultSet
	 * @return truncated description string
	 */
	private String truncateUpdateDates(Object[] line) {

		String dates = (String) line[12];
		String[] tokens = StringUtils.split(dates, ServiceConstants.COMMA);

		if (tokens.length > 1) {
			Date latestDate = new Date();
			String[] trimmedTokens = Arrays.stream(tokens).map(String::trim).toArray(String[]::new);
			DateFormat dateFormat = new SimpleDateFormat(ServiceConstants.DATE_FORMAT_STRING);

			for (int i = 0; i < trimmedTokens.length; i++) {
				try {
					Date date = dateFormat.parse(trimmedTokens[i]);

					if (i == 0) {
						latestDate = date;
					} else if (date.after(latestDate)) {
						latestDate = date;
					}
				} catch (ParseException e) {
					log.error("Update Date Parsing error");
				}
			}

			return dateFormat.format(latestDate);
		} else {
			return tokens[0];
		}
	}
}
