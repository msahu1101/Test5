package com.mgmresorts.booking.room.reservation.search.dao.helper;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.mgmresorts.booking.room.oxi.models.Reservation;
import com.mgmresorts.booking.room.oxi.models.ReservationStatusType;
import com.mgmresorts.booking.room.oxi.models.RoomStay;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@UtilityClass
@Log4j2
public class ResponseHelper {

	public List<Reservation> resolveDuplicates(List<Reservation> docList) {

		List<Reservation> newDocList = new LinkedList<>();

		Map<String, List<Reservation>> docMap = new LinkedHashMap<>();
		// Iterate through list of reservation objects and group by confirmation
		// numbers
		docList.forEach(doc -> {

			String confNumber = doc.getReservationID();

			if (docMap.containsKey(confNumber)) {
				docMap.get(confNumber).add(doc);
			} else {
				List<Reservation> objList = new LinkedList<>();
				objList.add(doc);
				docMap.put(confNumber, objList);
			}
		});

		for (Map.Entry<String, List<Reservation>> entry : docMap.entrySet()) {
			List<Reservation> resvList = entry.getValue();
			if (entry.getValue().size() > 1) {
				// If there are multiple reservations, remove canceled one
				log.info("More than 1 reservation found for conf number {}", entry.getKey());
				List<Reservation> filteredResvList = new LinkedList<>();

				resvList.forEach(resv -> {
					RoomStay roomStay = resv.getRoomStays().getRoomStay().get(0);
					// remove cancel reservations
					if (!roomStay.getReservationStatusType().equals(ReservationStatusType.CANCELED)) {
						log.info("Removing reservation entry with CANCELLED status for confimation number: {}", entry.getKey());
						filteredResvList.add(resv);
					}
				});

				if (filteredResvList.size() == 1) {
					newDocList.addAll(filteredResvList);
				} else {
					log.info("Still not able to narrow down to 1 resv for conf number {}, so will process for share-with moves",
							entry.getKey());
					newDocList.addAll(resolveDuplicatesForShareWiths(filteredResvList));
				}
			} else {
				newDocList.addAll(entry.getValue());
			}
		}
		return newDocList;
	}
	
	private List<Reservation> resolveDuplicatesForShareWiths(List<Reservation> resvList) {
		// if the reservation is a share-with and got moved, 1 of them should
		// have previousHotelCode. Include the one with value for previousHotelCode
		List<Reservation> filteredResvList = new LinkedList<>();
		for (Reservation doc : resvList) {

			if (StringUtils.isNotEmpty(doc.getPreviousHotelCode())) {
				filteredResvList.add(doc);
				break;
			}
		}
		return filteredResvList;
	}
}
