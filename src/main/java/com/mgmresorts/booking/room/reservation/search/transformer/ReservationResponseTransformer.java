package com.mgmresorts.booking.room.reservation.search.transformer;

import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.reservation.search.util.CommonUtil;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ReservationResponseTransformer {

	public static void transform(Reservation reservation) {

		if (null != reservation.getResCreditCards()) {
			reservation.getResCreditCards().getResCreditCard().forEach(ccInfo -> ccInfo.getCreditCard()
					.setCreditCardCode(CommonUtil.getStandardCardType(ccInfo.getCreditCard().getCreditCardCode()))

			);
		}
		// In some cases, there are credit cards into at profile level as well
		if (null != reservation.getResProfiles()) {
			reservation.getResProfiles().getResProfile().forEach(profile -> {
				if (null != profile.getProfile() && null != profile.getProfile().getCreditCards()) {
					profile.getProfile().getCreditCards().getCreditCard().forEach(ccInfo -> ccInfo
							.setCreditCardCode(CommonUtil.getStandardCardType(ccInfo.getCreditCardCode()))
					);
				}
			});
		}
	}
}
