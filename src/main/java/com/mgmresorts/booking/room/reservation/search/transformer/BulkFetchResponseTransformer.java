package com.mgmresorts.booking.room.reservation.search.transformer;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.mgmresorts.booking.room.oxi.models.CreditCard;
import com.mgmresorts.booking.room.oxi.models.GuestCount;
import com.mgmresorts.booking.room.oxi.models.PhoneNumber;
import com.mgmresorts.booking.room.oxi.models.ProfileType;
import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.oxi.models.RoomStay;
import com.mgmresorts.booking.room.reservation.search.models.AdditionalGuest;
import com.mgmresorts.booking.room.reservation.search.models.AdditionalGuestMetadata;
import com.mgmresorts.booking.room.reservation.search.models.BulkFetchResponse;
import com.mgmresorts.booking.room.reservation.search.models.Metadata;
import com.mgmresorts.booking.room.reservation.search.models.MgmProfile;
import com.mgmresorts.booking.room.reservation.search.models.Package;
import com.mgmresorts.booking.room.reservation.search.models.Payment;
import com.mgmresorts.booking.room.reservation.search.models.Profile;
import com.mgmresorts.booking.room.reservation.search.models.RatePlan;
import com.mgmresorts.booking.room.reservation.search.models.ReservationReference;
import com.mgmresorts.booking.room.reservation.search.models.SpecialRequest;
import com.mgmresorts.booking.room.reservation.search.util.CommonUtil;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

/**
 * Utility class to transform reservation documents into customized response as
 * described in the API specification
 *
 */
@UtilityClass
@Log4j2
public class BulkFetchResponseTransformer {

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.setSerializationInclusion(Include.NON_NULL).setSerializationInclusion(Include.NON_EMPTY);
	
	private List<String> possibleCellphoneTypes = Splitter.on(COMMA)
			.splitToList(Optional.ofNullable(System.getenv(POSSIBLE_CELL_PHONE_TYPES)).orElse(EMPTY));
	
	public void mockPossibleCellphoneTypes(List<String> mockCellphoneTypes) {
		possibleCellphoneTypes = mockCellphoneTypes;
	}

	/**
	 * Transforms list of reservation documents into customize response as
	 * described in the API specification.
	 * 
	 * @param responseJson
	 *            Json string of reservation docs
	 * @return Returns json string of customized response
	 * @throws IOException 
	 */
	public static String getBulkFetchResponse(String responseJson) throws IOException {

		Reservation[] reservations = MAPPER.readValue(responseJson, Reservation[].class);
		List<BulkFetchResponse> responseList = new ArrayList<>();

		for (Reservation reservation : reservations) {
			BulkFetchResponse fetchResponse = new BulkFetchResponse();
			fetchResponse.setId(reservation.getId());
			fetchResponse.setHotelCode(reservation.getHotelReference().getHotelCode());
			fetchResponse.setOperaConfirmationNumber(reservation.getReservationID());
			fetchResponse.setResvNameId(reservation.getResvNameId());

			setMgmProfile(fetchResponse, reservation);
			setMetadata(fetchResponse, reservation);

			RoomStay roomStay = reservation.getRoomStays().getRoomStay().get(0);

			fetchResponse.setRoomType(roomStay.getRoomInventoryCode());
			fetchResponse.setRoomNumber(roomStay.getRoomID());
			fetchResponse.setStatus(roomStay.getReservationStatusType().name());
			fetchResponse.setInventoryBlockCode(roomStay.getInventoryBlockCode());
			fetchResponse.setMarketSegmentCode(roomStay.getMarketSegmentCode());
			fetchResponse.setSourceCode(roomStay.getMfsourceCode());
			if (null != roomStay.getGuaranteeInfo()) {
				fetchResponse.setGuaranteeType(roomStay.getGuaranteeInfo().getMfGuaranteeType());
			}

			int guests = 0;
			for (GuestCount count : roomStay.getGuestCounts().getGuestCount()) {
				guests = guests + count.getMfCount();
				if (count.getAgeQualifyingCode().equals(ADULT)) {
					fetchResponse.getGuestCount().setNumAdults(count.getMfCount());
				} else if (count.getAgeQualifyingCode().equals(CHILD)) {
					fetchResponse.getGuestCount().setNumChildren(count.getMfCount());
				}
			}
			fetchResponse.setGuests(guests);
			fetchResponse.setArrivalTime(reservation.getStayDateRange().getStartTime());
			fetchResponse.setStayLength(reservation.getStayDateRange().getNumberOfTimeUnits());
			fetchResponse.setBookingDate(reservation.getOriginalBookingDate());
			
			setRatePlans(fetchResponse, reservation);

			setProfile(fetchResponse, reservation);

			setSpecialRequests(fetchResponse, reservation);

			setPackages(fetchResponse, reservation);

			setPaymentInfo(fetchResponse, reservation);

			responseList.add(fetchResponse);
		}

		return MAPPER.writeValueAsString(responseList);

	}

	/**
	 * Extract and set rate plans information day-wise from the reservation
	 * object
	 * 
	 * @param fetchResponse
	 *            Bulk fetch response
	 * @param reservation
	 *            Reservation object
	 * @param context
	 *            Execution context
	 */
	private void setRatePlans(BulkFetchResponse fetchResponse, Reservation reservation) {

		List<RatePlan> ratePlanList = new ArrayList<>();
		reservation.getRoomStays().getRoomStay().get(0).getRatePlans().getRatePlan().forEach(ratePlan -> {
			RatePlan plan = new RatePlan();
			plan.setStartDate(ratePlan.getTimeSpan().getStartTime());
			GregorianCalendar cal = ratePlan.getTimeSpan().getStartTime().toGregorianCalendar();
			cal.add(Calendar.DAY_OF_MONTH, ratePlan.getTimeSpan().getNumberOfTimeUnits());
			try {
				plan.setEndDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));
			} catch (DatatypeConfigurationException e) {
				log.error("DatatypeConfiguration exception", e);
			}
			plan.setRatePlanCode(ratePlan.getRatePlanCode());
			ratePlanList.add(plan);
		});
		fetchResponse.setRatePlans(ratePlanList);
	}

	/**
	 * Extract and set all the guest related info like name, email, mlife, tier
	 * info.
	 * 
	 * @param fetchResponse
	 *            Bulk fetch response
	 * @param reservation
	 *            Reservation object
	 */
	private void setProfile(BulkFetchResponse fetchResponse, Reservation reservation) {

		Profile guestProfile = new Profile();

		List<String> rphs = new ArrayList<>();
		reservation.getResGuests().getResGuest().forEach(guest -> {
			String profileRphs = StringUtils.EMPTY;
			if (guest.getReservationID().equals(reservation.getReservationID())) {
				profileRphs = guest.getProfileRPHs();//primary guest

				List<ReservationReference> references = new ArrayList<>();
				// Populate reservation references
				if (guest.getReservationReferences() != null) {
					guest.getReservationReferences().getReservationReference().forEach(resRef -> {
						ReservationReference ref = new ReservationReference();
						ref.setReferenceNumber(resRef.getReferenceNumber());
						ref.setType(resRef.getType());
						references.add(ref);
					});
				}
				fetchResponse.setReservationReferences(references);
			}

			if (StringUtils.isNotEmpty(profileRphs)) {
				rphs.addAll(Arrays.asList(profileRphs.replaceAll("\\s", "").split(COMMA)));
			}
		});

		reservation.getResProfiles().getResProfile().forEach(profile -> {
			if (profile.getProfile().getProfileType().equals(ProfileType.GUEST)
					&& rphs.contains(String.valueOf(profile.getResProfileRPH()))) {

				com.mgmresorts.booking.room.oxi.models.Profile resProfile = profile.getProfile();

				if (null != resProfile.getElectronicAddresses()) {
					resProfile.getElectronicAddresses().getElectronicAddress().forEach(email -> {
						if (email.getMfPrimaryYN().equalsIgnoreCase(YES)) {
							guestProfile.setEmail(email.getEAddress());
						}
					});
				}

				guestProfile.setFirstName(resProfile.getIndividualName().getNameFirst());
				guestProfile.setLastName(resProfile.getIndividualName().getNameSur());
				guestProfile.setVipStatus(resProfile.getMfVipStatus());
				guestProfile.setOperaProfileId(resProfile.getMfResortProfileID());

				if (null != resProfile.getMemberships()) {
					resProfile.getMemberships().getMembership().forEach(membership -> {
						if (membership.getProgramCode().equals(PC)) {
							guestProfile.setMlifeNumber(membership.getAccountID());
						}
					});
				}

				guestProfile.setMobilePhone(getMobilePhone(resProfile));
			}
		});

		guestProfile.setTierStatus(reservation.getTierStatus());

		fetchResponse.setProfile(guestProfile);
	}

	/**
	 * Extract and set special requests information from the reservation object
	 * 
	 * @param fetchResponse
	 *            Bulk fetch response
	 * @param reservation
	 *            Reservation object
	 */
	private void setSpecialRequests(BulkFetchResponse fetchResponse, Reservation reservation) {

		if (null != reservation.getSpecialRequests()) {
			List<SpecialRequest> sRequestList = new ArrayList<>();
			reservation.getSpecialRequests().getSpecialRequest().forEach(request -> {
				SpecialRequest specialRequest = new SpecialRequest();
				specialRequest.setRequestCode(request.getRequestCode());
				specialRequest.setRequestComments(request.getRequestComments());
				sRequestList.add(specialRequest);
			});
			fetchResponse.setSpecialRequests(sRequestList);
		}
	}

	/**
	 * Extract and set packages or otherwise called services information from
	 * the reservation object
	 * 
	 * @param fetchResponse
	 *            Bulk fetch response
	 * @param reservation
	 *            Reservation object
	 */
	private void setPackages(BulkFetchResponse fetchResponse, Reservation reservation) {

		if (null != reservation.getServices()) {
			List<Package> packageList = new ArrayList<>();
			reservation.getServices().getService().forEach(service -> {
				Package servicePackage = new Package();
				servicePackage.setServiceInventoryCode(service.getServiceInventoryCode());
				servicePackage.setPrice(service.getPrice().getValueNum());
				packageList.add(servicePackage);
			});
			fetchResponse.setPackages(packageList);
		}
	}

	/**
	 * Extract and set payment attribute related information
	 * 
	 * @param fetchResponse
	 *            Bulk fetch response
	 * @param reservation
	 *            Reservation object
	 */
	private void setPaymentInfo(BulkFetchResponse fetchResponse, Reservation reservation) {

		if (null != reservation.getResCreditCards()) {
			reservation.getResCreditCards().getResCreditCard().forEach(ccInfo -> {
				if (ccInfo.getCreditCard().getMfPrimaryYN().equalsIgnoreCase(YES)) {
					Payment payment = new Payment();
					payment.setCardType(CommonUtil.getStandardCardType(ccInfo.getCreditCard().getCreditCardCode()));
					payment.setCardHolderName(ccInfo.getCreditCard().getCreditCardHolderName());
					String maskedCardNum = CommonUtil.maskCardNumber(ccInfo.getCreditCard().getCreditCardNumber(), 'X');
					payment.setMaskedCardNumber(maskedCardNum);
					payment.setCardToken(ccInfo.getCreditCard().getCreditCardNumber());
					payment.setCardExpiry(ccInfo.getCreditCard().getCreditCardExpire());
					fetchResponse.setPayment(payment);
				}
			});
		}
	}
	
	/**
	 * Extract and set all the additional guests that may be added to the
	 * reservation.
	 * 
	 * @param basicResv
	 *            Basic Reservation response object
	 * @param reservation
	 *            Reservation object retrieved from the cloud store
	 */
	private void setMgmProfile(BulkFetchResponse bulkResv, Reservation reservation) {

		if (null == reservation.getMgmProfile()) {
			return;
		}

		MgmProfile mgmProfile = new MgmProfile();
		mgmProfile.setMgmId(reservation.getMgmProfile().getMgmId());

		List<AdditionalGuest> additionalGuests = new ArrayList<>();
		reservation.getMgmProfile().getAdditionalGuests().forEach(guest -> {
			AdditionalGuest basicGuest = new AdditionalGuest();
			basicGuest.setFirstName(guest.getFirstName());
			basicGuest.setLastName(guest.getLastName());
			basicGuest.setReservationID(guest.getReservationID());
			basicGuest.setMgmId(guest.getMgmId());
			basicGuest.setPrecreated(guest.isPrecreated());
			basicGuest.setReservationStatus(guest.getReservationStatus());
			basicGuest.setResvNameId(guest.getResvNameId());
			basicGuest.setShareType(guest.getShareType());
			
			AdditionalGuestMetadata guestMeta = new AdditionalGuestMetadata();
			if (null != guest.getMetadata()) {
				guestMeta.setEtaUpdated(guest.getMetadata().isEtaUpdated());
				guestMeta.setPaymentFailed(guest.getMetadata().isPaymentFailed());
				guestMeta.setPaymentUpdated(guest.getMetadata().isPaymentUpdated());
				basicGuest.setMetadata(guestMeta);
			}
			
			if (null != guest.getCreditCard()) {
				Payment guestPayment = new Payment();
				CreditCard card = guest.getCreditCard();

				guestPayment.setCardType(CommonUtil.getStandardCardType(card.getCreditCardCode()));
				guestPayment.setCardHolderName(card.getCreditCardHolderName());
				String maskedCardNum = CommonUtil.maskCardNumber(card.getCreditCardNumber(), 'X');
				guestPayment.setMaskedCardNumber(maskedCardNum);
				guestPayment.setCardToken(card.getCreditCardNumber());
				guestPayment.setCardExpiry(card.getCreditCardExpire());
				basicGuest.setPayment(guestPayment);
			}
			
			additionalGuests.add(basicGuest);
		});
		mgmProfile.setAdditionalGuests(additionalGuests);
		bulkResv.setMgmProfile(mgmProfile);
	}

	/**
	 * Extracts and sets all the metadata flags maintained in the cloud store
	 * 
	 * @param bulkResv
	 *            Bulk Reservation response object
	 * @param reservation
	 *            Reservation object retrieved from the cloud store
	 */
	private void setMetadata(BulkFetchResponse bulkResv, Reservation reservation) {
		com.mgmresorts.booking.room.oxi.models.extensions.Metadata resvMetaData = reservation.getMetadata();

		Metadata metadata = new Metadata();
		metadata.setPaymentUpdated(resvMetaData.isPaymentUpdated());
		metadata.setPaymentFailed(resvMetaData.isPaymentFailed());
		metadata.setEtaUpdated(resvMetaData.isEtaUpdated());
		metadata.setGuestsUpdated(resvMetaData.isGuestsUpdated());
		metadata.setAdditionalGuestsCount(resvMetaData.getAdditionalGuestsCount());
		metadata.setEarlyCheckIn(resvMetaData.getEarlyCheckIn());
		metadata.setCheckinReversedDate(CommonUtil.isNull(resvMetaData.getCheckinReversedDate()) ? null : resvMetaData.getCheckinReversedDate());
		metadata.setCheckinInfo(resvMetaData.getCheckinInfo());
		bulkResv.setMetadata(metadata);
	}

	private String getMobilePhone(com.mgmresorts.booking.room.oxi.models.Profile resProfile) {
		if (resProfile.getPhoneNumbers() == null) {
			return null;
		}

		// Find most likely cell phone number in the guest profile
		return resProfile.getPhoneNumbers().getPhoneNumber().stream()
				.filter(phoneNumber -> phoneNumber.getPhoneNumberType() != null
						&& possibleCellphoneTypes.contains(phoneNumber.getPhoneNumberType().value()))
				.min(Comparator.comparingInt(
						phoneNumber -> possibleCellphoneTypes.indexOf(phoneNumber.getPhoneNumberType().value())))
				.map(PhoneNumber::getPhoneNumber).orElse(null);
	}

}
