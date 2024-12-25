package com.mgmresorts.booking.room.reservation.search.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;

import com.google.inject.Inject;
import com.mgmresorts.booking.common.error.exception.DataNotFoundException;
import com.mgmresorts.booking.room.oxi.models.Profile;
import com.mgmresorts.booking.room.oxi.models.ResProfile;
import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.oxi.models.SelectedMembership;
import com.mgmresorts.booking.room.reservation.search.config.AppProperties;
import com.mgmresorts.booking.room.reservation.search.dao.DocumentDao;
import com.mgmresorts.booking.room.reservation.search.models.BasicReservation;
import com.mgmresorts.booking.room.reservation.search.models.BlockPartnerAccountSettings;
import com.mgmresorts.booking.room.reservation.search.models.BlockProfileIdSettings;
import com.mgmresorts.booking.room.reservation.search.response.ResponseWithHeaders;
import com.mgmresorts.booking.room.reservation.search.transformer.BasicReservationResponseTransformer;
import com.mgmresorts.booking.room.reservation.search.util.CommonUtil;
import com.mgmresorts.booking.room.reservation.search.util.ServiceConstants;
import com.mgmresorts.booking.room.reservation.search.util.ZonedDateTimeProvider;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class SearchServiceImpl implements SearchService {

	@Inject
	private DocumentDao documentDao;

	@Inject
	private AppProperties appProperties;

	@Inject
	private ZonedDateTimeProvider zonedDateTimeProvider;

	public SearchServiceImpl() {

		log.info("Creating instance: SearchService");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mgmresorts.booking.room.reservation.search.service.SearchService#
	 * searchReservations(java.util.Map, boolean)
	 */
	@Override
	public String searchReservations(Map<String, String> params, boolean basicInfo) {

		List<Reservation> filteredDocsList = documentDao.searchReservations(params);
		filteredDocsList = handleTCOLVsearch(filteredDocsList, params);
		handleSameDayBooking(filteredDocsList);
		handleBlockedPartnerAccount(filteredDocsList);

		List<Object> finalDocsList = new ArrayList<>();
		List<String> confNumbers = new ArrayList<>();

		if (basicInfo) {
			// convert to basic reservation object
			filteredDocsList.forEach(document -> {
				BasicReservation basicReservation = BasicReservationResponseTransformer.getBasicReservation(document);
				basicReservation.setUpcoming(getMetadataUpcoming(document));
				if (appProperties.getKioskClientId() != null && appProperties.getKioskClientId()
				        .equalsIgnoreCase(params.get(ServiceConstants.KIOSK_CLIENT_ID))) {
					basicReservation.setRatePlans(document.getRoomStays().getRoomStay().get(0).getRatePlans());
					basicReservation.setId(document.getId());
				}
				finalDocsList.add(basicReservation);
			});

			finalDocsList.forEach(doc -> {
				BasicReservation resv = (BasicReservation) doc;
				confNumbers.add(resv.getOperaConfirmationNumber());
			});

		} else {
			finalDocsList.addAll(filteredDocsList);
			finalDocsList.forEach(doc -> {
				Reservation resv = (Reservation) doc;
				handleAddOnsInResponse(resv);
				resv.getMetadata().setIsUpcoming(getMetadataUpcoming(resv));
				confNumbers.add(resv.getReservationID());
			});
		}

		// Print the confirmation numbers for debugging or log analytics
		log.debug("Returning reservations with following opera confirmation numbers: {}",
				StringUtils.join(confNumbers, ServiceConstants.COMMA));

		return CommonUtil.convertToJson(finalDocsList);
	}

	private List<Reservation> handleTCOLVsearch(List<Reservation> filteredDocsList, Map<String, String> params) {
		String role = params.getOrDefault(ServiceConstants.MGM_ROLE, ServiceConstants.EMPTY);
		String channel = params.getOrDefault(ServiceConstants.X_MGM_CHANNEL, ServiceConstants.EMPTY);

		if (role.equals(ServiceConstants.SERVICE_ROLE) && appProperties.getTcolvChannelWhitelist().contains(channel)) {
			return filteredDocsList;
		}

		List<Reservation> resvList = new ArrayList<>(filteredDocsList);
		Iterator<Reservation> iterator = resvList.iterator();
		while (iterator.hasNext()) {
			Reservation resv = iterator.next();
			if (resv.getHotelReference().getHotelCode().equals(ServiceConstants.TCOLV_HOTEL_CODE)) {
				iterator.remove();
				log.info("Removed TCOLV reservation from search results with confirmation #: {}",
						resv.getReservationID());
			}
		}
		return resvList;
	}

	@Override
	public String searchInHouseReservations(Map<String, String> params) {

		Reservation reservation = documentDao.searchInHouseReservations(params);
		return CommonUtil.convertToJson(BasicReservationResponseTransformer.getBasicInhouseReservation(reservation));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mgmresorts.booking.room.reservation.search.service.SearchService#
	 * fetchBulkReservations(java.util.Map)
	 */
	@Override
	public ResponseWithHeaders fetchBulkReservations(Map<String, String> params) {

		return documentDao.fetchBulkReservations(params);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mgmresorts.booking.room.reservation.search.service.SearchService#
	 * searchReservationProfiles(java.util.Map)
	 */
	@Override
	public String searchReservationProfiles(Map<String, String> params) {

		return documentDao.searchReservationProfiles(params);
	}

	private void handleAddOnsInResponse(Reservation reservation) {

		if (!CommonUtil.isNull(reservation.getMgmProfile())
				&& !CommonUtil.isNull(reservation.getMgmProfile().getAssociatedInvClasses())
				&& !reservation.getMgmProfile().getAssociatedInvClasses().isEmpty()) {
			reservation.getMgmProfile().setAssociatedInvClasses(null);
		}
	}

	private boolean getMetadataUpcoming(Reservation reservation) {

		String zoneName = Optional.ofNullable(appProperties.getPropertyTimezones())
				.map(map -> map.get(reservation.getHotelReference().getHotelCode()))
				.orElse(ServiceConstants.PACIFIC_TIME_ZONE);
		ZoneId zoneId = ZoneId.of(zoneName);
		int dateRollShift = Integer
				.parseInt(Optional.ofNullable(appProperties.getPropertyDateRollShiftFromMidnightInMinutes())
						.map(map -> map.get(reservation.getHotelReference().getHotelCode())).orElse("0"));
		ZonedDateTime checkInStart = reservation.getStayDateRange().getStartTime().toGregorianCalendar()
				.toZonedDateTime().toLocalDate().atStartOfDay(zoneId);

		ZonedDateTime upcomingStartAt = checkInStart.minusDays(ServiceConstants.UPCOMING_DAYS_WINDOW);
		ZonedDateTime upcomingEndAt = checkInStart.plusDays(1).plusMinutes(dateRollShift);
		ZonedDateTime now = zonedDateTimeProvider.now(zoneId);

		return upcomingStartAt.compareTo(now) <= 0 && upcomingEndAt.compareTo(now) >= 0;
	}

	private Optional<String> getBlockedProfileId(List<Reservation> resvList) {

		if (resvList.isEmpty()) {
			return Optional.empty();
		}
		List<String> blockedProfileIdsList = Arrays.asList(Optional
				.ofNullable(appProperties.getBlockProfileIdSettings()).map(BlockProfileIdSettings::getBlockedProfileIds)
				.orElse(ServiceConstants.EMPTY).split(ServiceConstants.COMMA));
		return resvList.get(0).getResProfiles().getResProfile().stream()
				.filter(profile -> blockedProfileIdsList.contains(profile.getProfile().getMfResortProfileID()))
				.findFirst().map(ResProfile::getProfile).map(Profile::getMfResortProfileID);
	}

	private boolean isBlockedToken() {

		String role = ThreadContext.get(ServiceConstants.MGM_ROLE);
		List<String> blockedProfileIdAllowedTokensList = Arrays.asList(Optional
				.ofNullable(appProperties.getBlockProfileIdSettings()).map(BlockProfileIdSettings::getAllowedTokens)
				.orElse(ServiceConstants.EMPTY).split(ServiceConstants.COMMA));
		return !blockedProfileIdAllowedTokensList.contains(role);
	}

	private boolean isBookedWithin24Hours(Reservation reservation, Optional<String> blockedProfileId) {

		LocalDate checkinDate = reservation.getStayDateRange().getStartTime().toGregorianCalendar().toZonedDateTime()
				.toLocalDate();
		LocalDate bookingDatePlus24Hours = reservation.getOriginalBookingDate().toGregorianCalendar().toZonedDateTime()
				.toLocalDate().plusDays(1);
		boolean sameOrNextDayBooking = checkinDate.compareTo(bookingDatePlus24Hours) <= 0;

		if (sameOrNextDayBooking) {
			log.info("Found same day reservation: hotel code = {}, confirmation number = {}, booking date = {}, arrival date = {}, profile id = {}, reservation status = {}",
					reservation.getHotelReference().getHotelCode(), reservation.getReservationID(),
					reservation.getOriginalBookingDate(), reservation.getStayDateRange().getStartTime(),
					blockedProfileId.get(),
					reservation.getRoomStays().getRoomStay().get(0).getReservationStatusType().value());
		}
		return sameOrNextDayBooking;
	}

	private boolean isBlockedReservationStatus(Reservation reservation) {

		String status = reservation.getRoomStays().getRoomStay().get(0).getReservationStatusType().value();
		List<String> blockedReservationStatusList = Arrays
				.asList(Optional.ofNullable(appProperties.getBlockProfileIdSettings())
						.map(BlockProfileIdSettings::getBlockedReservationStatuses).orElse(ServiceConstants.EMPTY)
						.split(ServiceConstants.COMMA));
		return blockedReservationStatusList.contains(status);
	}

	private void handleSameDayBooking(List<Reservation> filteredDocsList) {

		Optional<String> blockedProfileId = getBlockedProfileId(filteredDocsList);

		if (blockedProfileId.isPresent() && isBlockedToken()
				&& isBookedWithin24Hours(filteredDocsList.get(0), blockedProfileId)
				&& isBlockedReservationStatus(filteredDocsList.get(0))) {
			Reservation resv = filteredDocsList.get(0);
			log.info("Blocked same day reservation: hotel code = {}, confirmation number = {}, booking date = {}, arrival date = {}, blocked profile id = {}, blocked reservation status = {}",
					resv.getHotelReference().getHotelCode(), resv.getReservationID(), resv.getOriginalBookingDate(),
					resv.getStayDateRange().getStartTime(), blockedProfileId.get(),
					resv.getRoomStays().getRoomStay().get(0).getReservationStatusType().value());
			throw new DataNotFoundException(ServiceConstants.NO_DATA_FOUND, "Requested resource not found");
		}
	}

	private void handleBlockedPartnerAccount(List<Reservation> filteredDocsList) {

		BlockPartnerAccountSettings settings = appProperties.getBlockPartnerAccountSettings();

		if (filteredDocsList.isEmpty() || settings == null ||!settings.isShouldBlockPartnerAccount()) {
			return;
		}

		Reservation resv = filteredDocsList.get(0);

		List<SelectedMembership> selectedMemberships = resv.getSelectedMemberships() != null
				? filteredDocsList.get(0).getSelectedMemberships().getSelectedMembership()
				: Collections.emptyList();

		// Search selected memberships for blocked program code
		Optional<String> blockedMembership = Optional.ofNullable(selectedMemberships.stream()
				.filter(membership -> membership.getMfInactiveDate() == null && membership.getProgramCode() != null
						&& settings.getBlockedProgramCodes().contains(membership.getProgramCode()))
				.findFirst().map(SelectedMembership::getProgramCode).orElse(null));

		if (blockedMembership.isPresent()
				&& (settings.getBlockedHotelCodes().contains(resv.getHotelReference().getHotelCode())
						|| settings.getBlockedHotelCodes().isEmpty())
				&& !settings.getAllowedTokens().contains(ThreadContext.get(ServiceConstants.MGM_ROLE))) {
			log.info("Found Hyatt Membership reservation: hotel code = {}, confirmation number = {}, booking date = {}, arrival date = {}, program code = {}, reservation status = {}",
					resv.getHotelReference().getHotelCode(), resv.getReservationID(), resv.getOriginalBookingDate(),
					resv.getStayDateRange().getStartTime(), blockedMembership.get(),
					resv.getRoomStays().getRoomStay().get(0).getReservationStatusType().value());

			if (settings.getBlockedReservationStatuses()
					.contains(resv.getRoomStays().getRoomStay().get(0).getReservationStatusType().value())) {
				log.info("Blocked Hyatt Membership reservation: hotel code = {}, confirmation number = {}, booking date = {}, arrival date = {}, blocked program code = {}, blocked reservation status = {}",
						resv.getHotelReference().getHotelCode(), resv.getReservationID(), resv.getOriginalBookingDate(),
						resv.getStayDateRange().getStartTime(), blockedMembership.get(),
						resv.getRoomStays().getRoomStay().get(0).getReservationStatusType().value());
				throw new DataNotFoundException(ServiceConstants.NO_DATA_FOUND, "Requested resource not found");
			}
		}
	}
}
