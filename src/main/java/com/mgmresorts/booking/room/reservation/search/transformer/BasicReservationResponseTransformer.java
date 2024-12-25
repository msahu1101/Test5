package com.mgmresorts.booking.room.reservation.search.transformer;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.mgmresorts.booking.room.oxi.models.GuestCount;
import com.mgmresorts.booking.room.oxi.models.ProfileType;
import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.oxi.models.RoomStay;
import com.mgmresorts.booking.room.reservation.search.models.AdditionalGuest;
import com.mgmresorts.booking.room.reservation.search.models.BasicReservation;
import com.mgmresorts.booking.room.reservation.search.models.BasicReservationProfile;
import com.mgmresorts.booking.room.reservation.search.models.InHouseReservation;

import lombok.experimental.UtilityClass;

/**
 * Helper class to create basic reservation object with minimal info.
 *
 */
@UtilityClass
public class BasicReservationResponseTransformer {

    public static BasicReservation getBasicReservation(Reservation reservation) {
        BasicReservation basicResv = new BasicReservation();
        basicResv.setHotelCode(reservation.getHotelReference().getHotelCode());
        basicResv.setOperaConfirmationNumber(reservation.getReservationID());
        basicResv.setResvNameId(reservation.getResvNameId());

        RoomStay roomStay = reservation.getRoomStays().getRoomStay().get(0);
        basicResv.setRoomType(roomStay.getRoomInventoryCode());
        basicResv.setStatus(roomStay.getReservationStatusType().name());

        int guests = 0;
        for (GuestCount count : roomStay.getGuestCounts().getGuestCount()) {
            guests = guests + count.getMfCount();
        }
        basicResv.setGuests(guests);
        basicResv.setArrivalTime(reservation.getStayDateRange().getStartTime());
        basicResv.setStayLength(reservation.getStayDateRange().getNumberOfTimeUnits());
        basicResv.setSharedReservation(isSharedReservation(reservation));
        basicResv.setArriveTimeSet(isArrivalTimeSetByGuest(reservation));
        basicResv.setRoomUpsell(reservation.getRoomUpsell());
        basicResv.setCheckinInfo(reservation.getMetadata().getCheckinInfo());
        basicResv.setMetadata(reservation.getMetadata());

        setGuestDetails(basicResv, reservation);
        setAdditionalGuests(basicResv, reservation);

        if (null != reservation.getMgmProfile()) {
            basicResv.setMgmId(reservation.getMgmProfile().getMgmId());
        }

        return basicResv;
    }
    
    public static InHouseReservation getBasicInhouseReservation(Reservation reservation) {
    	InHouseReservation inHouseReservation = new InHouseReservation();
    	inHouseReservation.setResvNameId(reservation.getResvNameId());
    	RoomStay roomStay = reservation.getRoomStays().getRoomStay().get(0);
    	inHouseReservation.setRoomType(roomStay.getRoomInventoryCode());
    	return inHouseReservation;
    }

    public static BasicReservationProfile getBasicReservationProfile(Reservation reservation) {

        BasicReservation resv = new BasicReservation();
        setGuestDetails(resv, reservation);

        BasicReservationProfile resvProfile = new BasicReservationProfile();
        resvProfile.setFirstName(resv.getFirstName());
        resvProfile.setLastName(resv.getLastName());
        resvProfile.setReservationID(reservation.getReservationID());
        resvProfile.setResvNameId(reservation.getResvNameId());

        return resvProfile;
    }

    /**
     * Reservation is a shared reservation if there are multiple /resGuests
     * object in the response and each of them has mfCRSShareID attribute
     * populated.
     * 
     * @param reservation
     *            Reservation payload
     * @return Returns true if the reservation is a shared reservation
     */
    private boolean isSharedReservation(Reservation reservation) {

        if (null != reservation.getResGuests()) {
            List<String> shareIdList = new ArrayList<>();
            reservation.getResGuests().getResGuest().forEach(guest -> {
                if (null != guest.getMfCRSShareID()) {
                    shareIdList.add(String.valueOf(guest.getMfCRSShareID()));
                }
            });

            if (shareIdList.size() > 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Arrival time to considered to be set by guests if the arrival time is
     * greater than time at start of the day. By default, arrival time is set at
     * 00:00 UTC.
     * 
     * @param reservation
     *            Reservation payload
     * @return Returns true if the arrival time is not at 00:00:00 UTC
     */
    private boolean isArrivalTimeSetByGuest(Reservation reservation) {

        ZonedDateTime dateTime = reservation.getStayDateRange().getStartTime().toGregorianCalendar().toZonedDateTime();

        return dateTime.toEpochSecond() > dateTime.toLocalDate().atStartOfDay(ZoneId.of(UTC)).toEpochSecond();
    }

    /**
     * Extract and set guest's first name, last name and mlife number
     * 
     * @param basicResv
     *            Basic Reservation
     * @param reservation
     *            Reservation object
     */
    private void setGuestDetails(BasicReservation basicResv, Reservation reservation) {

        List<String> rphs = new ArrayList<>();
        reservation.getResGuests().getResGuest().forEach(guest -> {
            String profileRphs = StringUtils.EMPTY;
            if (guest.getReservationID().equals(reservation.getReservationID())) {
                profileRphs = guest.getProfileRPHs();
            }

            if (StringUtils.isNotEmpty(profileRphs)) {
                rphs.addAll(Arrays.asList(profileRphs.replaceAll("\\s", "").split(COMMA)));
            }
        });

        reservation.getResProfiles().getResProfile().forEach(profile -> {
            if (profile.getProfile().getProfileType().equals(ProfileType.GUEST)
                    && rphs.contains(String.valueOf(profile.getResProfileRPH()))) {

                com.mgmresorts.booking.room.oxi.models.Profile resProfile = profile.getProfile();

                basicResv.setFirstName(resProfile.getIndividualName().getNameFirst());
                basicResv.setLastName(resProfile.getIndividualName().getNameSur());

                if (null != resProfile.getMemberships()) {
                    resProfile.getMemberships().getMembership().forEach(membership -> {
                        if (membership.getProgramCode().equals(PC)) {
                            basicResv.setMlifeNumber(membership.getAccountID());
                        }
                    });
                }
            }
        });

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
    private void setAdditionalGuests(BasicReservation basicResv, Reservation reservation) {

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
            additionalGuests.add(basicGuest);
        });
        basicResv.setAdditionalGuests(additionalGuests);
    }

}
