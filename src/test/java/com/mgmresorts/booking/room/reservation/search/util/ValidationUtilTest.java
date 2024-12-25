package com.mgmresorts.booking.room.reservation.search.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mgmresorts.booking.common.error.Error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ValidationUtilTest {
	
	@Test
	public void testSearchValidationById() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("id", "Test");

		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationByConfNumber() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("confNumber", "Test");

		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationByOperaConfNumber() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("operaConfirmationNumber", "Test");

		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationByMlifeNumber() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("mlifeNumber", "Test");

		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationByGuestMlifeNumber() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("guestMlifeNumber", "Test");

		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationByOperaProfileId() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("operaProfileId", "Test");

		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationByRoomNumber() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("checkInDate", "2020-12-01");
		params.put("hotelCode", "001");
		params.put("roomNumber", "9233");

		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationByNameAndHotel() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("checkInDate", "2020-12-01");
		params.put("hotelCode", "001");
		params.put("firstName", "Test");

		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationByMgmId() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("mgmId", "Test");

		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testBasicSearchValidationByOperaConfNumberAndLastName() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("operaConfirmationNumber", "Test");
		params.put("lastName", "lName");

		assertTrue(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	public void testBasicSearchValidationByIdAndLastName() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("id", "Test");
		params.put("lastName", "lName");

		assertTrue(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	public void testBasicSearchValidationByConfNumberAndLastName() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("confNumber", "Test");
		params.put("lastName", "lName");

		assertTrue(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	public void testBasicSearchValidationByMgmIdAndLastName() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("mgmId", "Test");
		params.put("lastName", "lName");

		assertTrue(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	public void testBasicSearchValidationByMlifeAndLastName() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("guestMlifeNumber", "Test");

		assertTrue(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	public void testBasicSearchValidationByOperaProfileIdAndLastName() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("guestMlifeNumber", "Test");

		assertTrue(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	public void testBasicSearchValidationByGuestMlifeAndLastName() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("operaProfileId", "Test");

		assertTrue(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	public void testBasicSearchValidationByRoomNumber() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("roomNumber", "Test");
		params.put("hotelCode", "Test");
		params.put("checkInDate", "Test");

		assertTrue(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	public void testBasicSearchValidationByFirstName() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("firstName", "Test");
		params.put("hotelCode", "Test");
		params.put("checkInDate", "Test");

		assertTrue(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	public void testBasicSearchValidationByFirstNameAndArrival() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("firstName", "Test");
		params.put("hotelCode", "Test");
		params.put("startDate", "Test");
		params.put("endDate", "Test");

		assertTrue(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	public void testBasicSearchValidationByRoomNumberDateCClast4() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("roomNumber", "Test");
		params.put("checkInDate", "Test");
		params.put("ccLast4Digits", "Test");

		assertTrue(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	public void testSearchValidationByMlife() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("mlifeNumber", "Test");

		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationForMissingInfo() {
		Map<String, String> params = new HashMap<String, String>();

		assertFalse(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationForPartialInfoScenario1() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("checkInDate", "2020-12-01");
		params.put("hotelCode", "001");

		assertFalse(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationForPartialInfoScenario2() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("roomNumber", "9233");
		params.put("checkInDate", "2020-12-01");

		assertFalse(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationForCheckoutParams() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("roomNumber", "9233");
		params.put("checkInDate", "2020-12-01");
		params.put("ccLast4Digits", "444");

		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testSearchValidationForCheckInRangeParams() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastName", "Test");
		params.put("firstName", "First");
		params.put("startDate", "2020-12-01");
		params.put("endDate", "2020-12-01");
		params.put("hotelCode", "444");

		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	public void testIsTrueNumber() {
		String number = "1234567789";
		String number1 = "12b23";
		String number2 = "abc";
		String number3 = "3019156220201018161545";

		assertTrue(ValidationUtil.isTrueNumber(number));
		assertFalse(ValidationUtil.isTrueNumber(number1));
		assertFalse(ValidationUtil.isTrueNumber(number2));
		assertTrue(ValidationUtil.isTrueNumber(number3));

	}

	@Test
	public void testSearchValidationForHotelPartialNameAndCheckIn() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("hotelCode", "001");
		params.put("firstName", "Bro");
		params.put("lastName", "Reb");
		params.put("checkInDate", "2020-01-01");
		params.put("nameSearchOperation", "STARTSWITH");

		// validation should be successful when both first and last name has 3
		// chars or more
		assertTrue(ValidationUtil.isSearchValid(params));

		// validation should be successful when atleast one of first and last
		// name has 3 chars or more
		params.put("firstName", "B");
		params.put("lastName", "Reb");
		assertTrue(ValidationUtil.isSearchValid(params));

		// validation should fail when both first and last name has less than 3
		// chars
		params.put("firstName", "B");
		params.put("lastName", "Re");
		assertFalse(ValidationUtil.isSearchValid(params));

		// Name length validation shouldn't take effect when startswith
		// operation is not used
		params.put("nameSearchOperation", "FULL");
		assertTrue(ValidationUtil.isSearchValid(params));

		// Name length validation shouldn't take effect when name search
		// operation is not specified
		params.remove("nameSearchOperation");
		assertTrue(ValidationUtil.isSearchValid(params));
	}

	@Test
	void isTrueNumberTestNullStringPassedIn() {
		assertFalse(ValidationUtil.isTrueNumber(null));
	}

	@Test
	void isBasicSearchValidTestInvalidCCProvided() {
		Map<String, String> params = new HashMap<>();
		params.put("ccLast4Digits", "55555");
		assertFalse(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	void isBasicSearchValidTestinValidFirstAndLastName() {
		Map<String, String> params = new HashMap<>();
		params.put("nameSearchOperation", "FULL");
		params.put("firstName", "test");
		params.put("lastName", "test");
		assertFalse(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	void isBasicSearchValidTestinValidFirstNameMatch() {
		Map<String, String> params = new HashMap<>();
		params.put("firstNameMatch", "INVALID");
		assertFalse(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	void isBasicSearchValidTestinValidLAstNameMatch() {
		Map<String, String> params = new HashMap<>();
		params.put("lastNameMatch", "INVALID");
		assertFalse(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	void isInHouseBasicSearchValidTestHotelCodeAndRoomNum() {
		Map<String, String> params = new HashMap<>();
		params.put("hotelCode", "123");
		params.put("roomNumber", "1234");
		assertTrue(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	void isInHouseBasicSearchValidNoHotelCode() {
		Map<String, String> params = new HashMap<>();
		params.put("roomNumber", "1234");
		assertFalse(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	void isInHouseBasicSearchValidNoRoomNum() {
		Map<String, String> params = new HashMap<>();
		params.put("hotelCode", "123");
		assertFalse(ValidationUtil.isBasicSearchValid(params));
	}

	@Test
	void testValidateInputDataWithValidParams() {
		Map<String, String> params = new HashMap<>();
		params.put("hotelCode", "001");
		params.put("operaConfirmationNumber", "85727231");
		params.put("roomNumber", "1234");
		params.put("checkInDate", "2022-01-01");
		params.put("checkOutDate", "2022-01-01");
		params.put("ccLast4Digits", "4444");
		params.put("id", "8c923e9c-2516-3516-8237-2017de90543b");
		params.put("mgmId", "00u1d89k4hbUa9zN80h9");
		params.put("mlifeNumber", "123456789");
		params.put("operaConfirmationNumbers", "85727231,85727232");
		params.put("resvNameIds", "81831221,81831221");
		params.put("ids", "8c923e9c-2516-3516-8237-2017de90543b,8c423e9c-2516-3526-8237-2017de90543b");

		Optional<Error> result = ValidationUtil.validateInputData(params);
		assertFalse(result.isPresent());
	}

	@Test
	void testValidateInputDataInvalidNumericValue() {
		Optional<Error> result = ValidationUtil.validateInputData(getParams("operaConfirmationNumber", "82381F192"));
		assertTrue(result.isPresent());
	}

	@Test
	void testValidateInputDataInvalidHotelCode() {
		Optional<Error> result = ValidationUtil.validateInputData(getParams("hotelCode", "1234"));
		assertTrue(result.isPresent());
	}

	@Test
	void testValidateInputDataInvalidDate() {
		Optional<Error> result = ValidationUtil.validateInputData(getParams("checkInDate", "2022-99-99"));
		assertTrue(result.isPresent());
	}

	@Test
	void testValidateInputDataInvalidCC() {
		Optional<Error> result = ValidationUtil.validateInputData(getParams("ccLast4Digits", "12345"));
		assertTrue(result.isPresent());
	}

	@Test
	void testValidateInputDataInvalidMgmId() {
		Optional<Error> result = ValidationUtil.validateInputData(getParams("mgmId", "12345678-"));
		assertTrue(result.isPresent());
	}

	@Test
	void testValidateInputDataInvalidGuid() {
		Optional<Error> result = ValidationUtil.validateInputData(getParams("id", "8c923e9c2516351682372017de90543"));
		assertTrue(result.isPresent());
	}

	@Test
	void testValidateInputDataInvalidAlphanumericValue() {
		Optional<Error> result = ValidationUtil.validateInputData(getParams("mgmId", "41221F12F@"));
		assertTrue(result.isPresent());
	}

	@Test
	void testValidateInputDataInvalidMultipleNumericValues() {
		Optional<Error> result = ValidationUtil.validateInputData(getParams("operaConfirmationNumbers", "857asd231,857asd32"));
		assertTrue(result.isPresent());
	}

	@Test
	void testValidateInputDataInvalidMultipleGuids() {
		Optional<Error> result = ValidationUtil.validateInputData(getParams("ids", "2017de90543c,8c923e9c-2516-3516-8237-2017de90543d89091284"));
		assertTrue(result.isPresent());
	}

	@ParameterizedTest
	@ValueSource(strings = {"guest", "anon"})
	void testEnsureServiceOrEmployee_ExpectFailure(String role) {
		Optional<Error> error = ValidationUtil.ensureServiceOrEmployeeRole(role);
		assertTrue(error.isPresent());
	}

	@ParameterizedTest
	@ValueSource(strings = {"employee", "service"})
	void testEnsureServiceOrEmployeeRole_ExpectSuccess(String role) {
		Optional<Error> error = ValidationUtil.ensureServiceOrEmployeeRole(role);
		assertFalse(error.isPresent());
	}



	@ParameterizedTest
	@ValueSource(strings = {"employee", "service", "guest"})
	void testEnsureNoAnonymousJwtRole_ExpectSuccess(String role) {
		Optional<Error> error = ValidationUtil.ensureNoAnonymousJwtRole(role);
		assertFalse(error.isPresent());
	}

	@Test
	void testEnsureNoAnonymousJwtRole_ExpectFailure() {
		Optional<Error> error = ValidationUtil.ensureNoAnonymousJwtRole("anon");
		assertTrue(error.isPresent());
	}

	@Test
	void testvalidateFolioRequestWithConfNumberAndServiceRole() {
		Map<String, String> params = new HashMap<>();
		params.put("jwtExists", "true");
		params.put("confNumber", "123");
		params.put("mgm_role", "service");
		assertFalse(ValidationUtil.validateFolioRequest(params).isPresent());
	}

	@Test
	void testvalidateFolioRequestWithConfNumberAndAnonRole() {
		Map<String, String> params = new HashMap<>();
		params.put("jwtExists", "true");
		params.put("confNumber", "123");
		params.put("mgm_role", "anon");
		assertTrue(ValidationUtil.validateFolioRequest(params).isPresent());
	}

	@Test
	void testvalidateFolioRequestWithConfNumberAndNoJwt() {
		Map<String, String> params = new HashMap<>();
		params.put("confNumber", "123");
		assertTrue(ValidationUtil.validateFolioRequest(params).isPresent());
	}

	@Test
	void testvalidateFolioRequestWithOtherParams() {
		Map<String, String> params = new HashMap<>();
		params.put("lastName", "lName");
		params.put("hotelCode", "001");
		params.put("ccLast4Digits", "4444");
		assertFalse(ValidationUtil.validateFolioRequest(params).isPresent());
	}

	@Test
	void testvalidateFolioRequestWithOtherParamsWithoutHotelCode() {
		Map<String, String> params = new HashMap<>();
		params.put("lastName", "lName");
		params.put("ccLast4Digits", "4444");
		assertTrue(ValidationUtil.validateFolioRequest(params).isPresent());
	}

	@Test
	void testvalidateFolioRequestWithOtherParamsWithoutLast4() {
		Map<String, String> params = new HashMap<>();
		params.put("lastName", "lName");
		params.put("hotelCode", "001");
		assertTrue(ValidationUtil.validateFolioRequest(params).isPresent());
	}

	@Test
	void testvalidateFolioRequestWithOtherParamsWithoutLastName() {
		Map<String, String> params = new HashMap<>();
		params.put("hotelCode", "001");
		params.put("ccLast4Digits", "4444");
		assertTrue(ValidationUtil.validateFolioRequest(params).isPresent());
	}
	
	@Test
	void testvalidateFolioRequestWithConfNumberHotelCodeLastNameAndCCLast4DigitsWithAnonRole() {
		Map<String, String> params = new HashMap<>();
		params.put("lastName", "lName");
		params.put("jwtExists", "true");
		params.put("hotelCode", "001");
		params.put("ccLast4Digits", "4444");
		params.put("confNumber", "123");
		params.put("mgm_role", "anon");
		assertFalse(ValidationUtil.validateFolioRequest(params).isPresent());
	}


	private Map<String, String> getParams(String key, String value) {
		Map<String, String> params = new HashMap<>();
		params.put(key, value);
		return params;
	}
}
