package com.mgmresorts.booking.room.reservation.search.transformer;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;

import com.mgmresorts.booking.room.oxi.models.*;
import com.mgmresorts.booking.room.reservation.search.models.*;
import com.mgmresorts.booking.room.reservation.search.models.opera.CreditAmount;
import com.mgmresorts.booking.room.reservation.search.models.opera.DebitAmount;
import com.mgmresorts.booking.room.reservation.search.models.opera.FolioWindow;
import com.mgmresorts.booking.room.reservation.search.models.opera.GetFolioDetailsResponse;
import com.mgmresorts.booking.room.reservation.search.models.opera.PaymentCard;
import com.mgmresorts.booking.room.reservation.search.models.opera.PaymentMethod;
import com.mgmresorts.booking.room.reservation.search.models.opera.Posting;
import com.mgmresorts.booking.room.reservation.search.models.opera.TrxCodeInfo;
import com.mgmresorts.booking.room.reservation.search.util.CommonUtil;
import com.mgmresorts.booking.room.reservation.search.util.ServiceConstants;
import com.mgmresorts.booking.room.reservation.search.util.ValidationUtil;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
@Log4j2
public class FolioResponseTransformer {

    private DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);

    /**
     * Sets baseResponse for folio service with expected response parameters
     * provided from reservation object
     *
     * @param baseResponse
     *                     Blank folio response object
     * @param reservation
     *                     Reservation to extract parameters from
     * @param jwtExists
     *                     Flag to provide profile information in base response
     * @return
     */
    public static FolioResponse setBaseResponse(FolioResponse baseResponse, Reservation reservation, String jwtExists,
            boolean shouldHideProfilePII) {

        final RoomStay roomStay = reservation.getRoomStays().getRoomStay().get(0);
        baseResponse.setOperaConfirmationNumber(reservation.getReservationID());
        baseResponse.setReservationStatus(roomStay.getReservationStatusType().name());
        String[] arrivalDates = getCheckInAndCheckOut(reservation);
        baseResponse.setCheckInDate(arrivalDates[0]);
        baseResponse.setCheckOutDate(arrivalDates[1]);
        if (null != jwtExists) {
            baseResponse.setProfile(getFolioProfile(reservation, shouldHideProfilePII));
        }
        baseResponse.setTotalCharges(null);
        baseResponse.setTotalCredits(null);
        baseResponse.setCurrentBalance(null);
        baseResponse.setRoomNumber(roomStay.getRoomID());

        return baseResponse;
    }

    public static void addUpdateDates(List<Date> updateDates, String date) {
        Date updateDate;
        try {
            updateDate = dateFormat.parse(date);
            updateDates.add(updateDate);
        } catch (Exception e) {
            log.error("Date parsing failed for date {} and hence not added to updateDates list", date);
        }
    }

    /**
     * Enhances folioResponse to include array of folios from resultMap. Each
     * distinct window in resultmap will have folio created for it with array of
     * billItems representing row from opera db call.
     *
     * @param folioResponse
     *                      Base response from folioHandler to be enhanced
     *
     * @param resultMap
     *                      ResultMap from opera DB call
     */
    @SuppressWarnings("unchecked")
    public static void transform(FolioResponse folioResponse, Map<String, Object> resultMap) {

        final ArrayList<String> dates = (ArrayList<String>) resultMap.get(TRX_DATE);
        final ArrayList<String> descriptions = (ArrayList<String>) resultMap.get(TRANSACTION_DESCRIPTION);
        final ArrayList<String> supplements = (ArrayList<String>) resultMap.get(REMARK);
        final ArrayList<String> debits = (ArrayList<String>) resultMap.get(GUEST_ACCOUNT_DEBIT);
        final ArrayList<String> credits = (ArrayList<String>) resultMap.get(GUEST_ACCOUNT_CREDIT);
        final ArrayList<String> ccLast4Digits = (ArrayList<String>) resultMap.get(CREDIT_CARD_NUMBER_4_DIGITS);
        final ArrayList<Integer> windows = (ArrayList<Integer>) resultMap.get(FOLIO_VIEW);
        final ArrayList<String> references = (ArrayList<String>) resultMap.get(REFERENCE);
        final ArrayList<String> updatedDates = (ArrayList<String>) resultMap.get(UPDATE_DATE);

        // Establish distinct windows and create folios for each.
        final List<Integer> distinctWindows = windows.stream().distinct().collect(Collectors.toList());
        final Folio[] folios = new Folio[distinctWindows.size()];
        distinctWindows.forEach(window -> {
            Folio folio = new Folio();
            folio.setWindowNo(window);
            folios[window - 1] = folio;
        });

        // Create array of bill items for each distinct window while
        // accumulating total charges and credits across each window
        double totalCharges = 0;
        double totalCredits = 0;
        final BillItem[][] billItems = new BillItem[distinctWindows.size()][ccLast4Digits.size()];
        for (int w = 0; w < distinctWindows.size(); w++) {
            double windowCharges = 0;
            double windowCredits = 0;
            double rounded = 0;
            List<Date> updateDates = new ArrayList<>();
            for (int i = 0; i < billItems[w].length; i++) {
                addUpdateDates(updateDates, updatedDates.get(i));
                final BillItem item = new BillItem();
                item.setChargeAmount(null);
                item.setCreditAmount(null);
                int window = windows.get(i);
                billItems[window - 1][i] = item;
                item.setDate(transformDate(dates.get(i)));
                item.setDescription(descriptions.get(i));
                item.setSupplement(supplements.get(i));
                item.setReference(references.get(i));
                item.setCcLast4Digits(ccLast4Digits.get(i));
                if (null != debits.get(i) && !debits.get(i).equals("null")) {
                    double debit = Double.parseDouble(debits.get(i));
                    rounded = (double) Math.round(debit * 100.0) / 100.0;
                    item.setChargeAmount(rounded);
                    totalCharges = (double) Math.round(totalCharges * 100.00) / 100.0 + rounded;
                    windowCharges = (double) Math.round(windowCharges * 100.00) / 100.0 + rounded;
                }
                if (null != credits.get(i) && !credits.get(i).equals("null")) {
                    double credit = Double.parseDouble(credits.get(i));
                    rounded = (double) Math.round(credit * 100.0) / 100.0;
                    item.setCreditAmount(rounded);
                    totalCredits = (double) Math.round(totalCredits * 100.00) / 100.0 + rounded;
                    windowCredits = (double) Math.round(windowCredits * 100.00) / 100.0 + rounded;
                }
            }
            if (updateDates.size() > 0) {
                Optional<Date> dateOpt = updateDates.stream().max(Date::compareTo);
                if (dateOpt.isPresent()) {
                    folios[w].setWindowLastUpdated(dateFormat.format(dateOpt.get()));
                }
            }
            folios[w].setWindowCharges((double) Math.round(windowCharges * 100.0) / 100.0);
            folios[w].setWindowBalance((double) Math.round((windowCharges - windowCredits) * 100.0) / 100.0);
            folios[w].setWindowCredits((double) Math.round(windowCredits * 100.0) / 100.0);
        }
        // for each folio, assign correct billItems array
        for (Folio folio : folios) {
            folio.setBillItems(billItems[folio.getWindowNo() - 1]);
        }

        // set summation of charges
        if (totalCharges != 0) {
            folioResponse.setTotalCharges((double) Math.round(totalCharges * 100.0) / 100.0);
        }
        folioResponse.setTotalCredits((double) Math.round(totalCredits * 100.0) / 100.0);
        folioResponse.setCurrentBalance((double) Math.round((totalCharges - totalCredits) * 100.0) / 100.0);

        // finalize response by setting folios array
        folioResponse.setFolios(folios);
    }

    /**
     * Extracts profile object from passed in reservation
     *
     * @param reservation
     *                    reservation to extract profile from
     * @return Folio Profile objecct
     */
    public static FolioProfile getFolioProfile(Reservation reservation, boolean shouldHideProfilePII) {
        List<String> rphs = new ArrayList<>();
        FolioProfile prof = new FolioProfile();

        reservation.getResGuests().getResGuest().forEach(guest -> {
            String profileRphs = StringUtils.EMPTY;
            if (guest.getReservationID().equals(reservation.getReservationID())) {
                profileRphs = guest.getProfileRPHs();// primary guest
            }
            if (StringUtils.isNotEmpty(profileRphs)) {
                rphs.addAll(Arrays.asList(profileRphs.replaceAll("\\s", "").split(COMMA)));
            }
        });

        reservation.getResProfiles().getResProfile().forEach(profile -> {
            if (profile.getProfile().getProfileType().equals(ProfileType.GUEST)
                    && rphs.contains(String.valueOf(profile.getResProfileRPH()))) {
                com.mgmresorts.booking.room.oxi.models.Profile resProfile = profile.getProfile();

                prof.setFirstName(resProfile.getIndividualName().getNameFirst());
                prof.setLastName(resProfile.getIndividualName().getNameSur());

                if (shouldHideProfilePII) {
                    return;
                }

                if (null != resProfile.getPostalAddresses()) {
                    resProfile.getPostalAddresses().getPostalAddress().forEach(address -> {
                        if (YES.equalsIgnoreCase(address.getMfPrimaryYN())) {
                            Address add = new Address();
                            add.setStreet(address.getAddress1());
                            add.setCity(address.getCity());
                            add.setState(address.getStateCode());
                            add.setCountry(address.getCountryCode());
                            add.setZip(address.getPostalCode());
                            prof.setAddress(add);
                        }
                    });
                }

                if (null != resProfile.getElectronicAddresses()) {
                    resProfile.getElectronicAddresses().getElectronicAddress().forEach(email -> {
                        if (YES.equalsIgnoreCase(email.getMfPrimaryYN())) {
                            prof.setEmail(email.getEAddress());
                        }
                    });
                }

                Memberships memberships = resProfile.getMemberships();
                if (null != memberships) {
                    Optional<Membership> membership = memberships.getMembership().stream()
                            .filter(mem -> mem.getProgramCode().equals(PC)).findFirst();
                    membership.ifPresent(value -> prof.setMlifeNumber(value.getAccountID()));
                }
                prof.setOperaProfileId(resProfile.getMfResortProfileID());
            }
        });
        return prof;
    }

    /**
     * Returns string pair representing checkin and checkout dates transformed
     * to proper format from passed in reservation object
     * 
     * @param reservation
     * @return String pair 0:checkin, 1:checkout
     */
    public static String[] getCheckInAndCheckOut(Reservation reservation) {
        DateFormat dateFormat = new SimpleDateFormat(ServiceConstants.DATE_FORMAT_STRING);

        Calendar cal = reservation.getStayDateRange().getStartTime().toGregorianCalendar();
        String checkIn = dateFormat.format(cal.getTime());

        int days = reservation.getStayDateRange().getNumberOfTimeUnits();
        cal.add(Calendar.DAY_OF_MONTH, days);
        String checkOut = dateFormat.format(cal.getTime());

        String[] returnPair = new String[2];
        returnPair[0] = checkIn;
        returnPair[1] = checkOut;

        return returnPair;
    }

    /**
     * Returns string representation of a date that is presented as a string in
     * a format other than yyyy-mm-dd
     * 
     * @param dateToTransform
     * @return string representation of date in format yyyy-mm-dd
     */
    public static String transformDate(String dateToTransform) {
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);
        try {
            Date date = dateFormat.parse(dateToTransform);
            return dateFormat.format(date);
        } catch (Exception e) {
            log.error("Failed to parse string {} to date format {}", dateToTransform, DATE_FORMAT_STRING);
            return null;
        }
    }

    /**
     * Enhances folioResponse to include array of folios from opera cloud response.
     * Each
     * distinct window in the response will have a folio created for it with an
     * array of
     * billItems representing a single, or aggregated transaction
     *
     * @param folioResponse
     *                           Base response from folioHandler to be enhanced
     * @param operaCloudresponse
     *                           Response from opera cloud call
     * @param aggregated
     *                           Aggregation flag
     */
    public static void transformOperaCloudResponse(FolioResponse folioResponse,
            GetFolioDetailsResponse operaCloudResponse, boolean aggregated) {

        List<FolioWindow> windows = operaCloudResponse.getReservationFolioInformation().getFolioWindows();
        if (windows == null || windows.isEmpty()) {
            log.debug("No folio windows available.");
            return;
        }
        double totalCharges = 0;
        double totalCredits = 0;
        double currentBalance = 0;
        List<Folio> folios = new ArrayList<>();
        List<TrxCodeInfo> trxCodesInfo = operaCloudResponse.getTrxCodesInfo();
        for (FolioWindow window : windows) {
            if (!DEFAULT_FOLIO_WINDOWS.contains(window.getFolioWindowNo().toString()) || window.getFolios() == null
                    || window.getFolios().isEmpty()) {
                log.debug("No folios available.");
                continue;
            }
            List<Date> postingDates = new ArrayList<>();
            // Create and populate a response folio for each window
            Folio responseFolio = new Folio();
            responseFolio.setWindowNo(window.getFolioWindowNo());
            List<BillItem> billItems = new ArrayList<>();
            double windowCharges = 0;
            double windowCredits = 0;
            double roundedWindowBalance = roundToHundredths(window.getBalance().getAmount());
            currentBalance = roundToHundredths(currentBalance) + roundedWindowBalance;
            responseFolio.setWindowBalance(roundedWindowBalance);
            for (com.mgmresorts.booking.room.reservation.search.models.opera.Folio folio : window.getFolios()) {
                if (folio.getPostings() == null || folio.getPostings().isEmpty()) {
                    log.debug("No postings available.");
                    continue;
                }
                List<Posting> postings = folio.getPostings();
                // If aggregated, group together itemized checks into one bill item
                if (aggregated) {
                    postings = aggregatePostings(postings, trxCodesInfo);
                }
                // Arrange postings by transaction number
                Collections.sort(postings, (p1, p2) -> Long.compare(p2.getTransactionNo(), p1.getTransactionNo()));
                for (Posting posting : postings) {
                    BillItem billItem = initBillItem(posting, window, trxCodesInfo);
                    CreditAmount creditAmount = posting.getCreditAmount();
                    DebitAmount chargeAmount = posting.getDebitAmount();
                    if (creditAmount != null) {
                        double roundedCreditAmount = roundToHundredths(creditAmount.getAmount());
                        billItem.setCreditAmount(roundedCreditAmount);
                        windowCredits = roundToHundredths(windowCredits) + roundedCreditAmount;
                        totalCredits = roundToHundredths(totalCredits) + roundedCreditAmount;
                    }
                    if (chargeAmount != null) {
                        double roundedChargeAmount = roundToHundredths(chargeAmount.getAmount());
                        billItem.setChargeAmount(roundedChargeAmount);
                        windowCharges = roundToHundredths(windowCharges) + roundedChargeAmount;
                        totalCharges = roundToHundredths(totalCharges) + roundedChargeAmount;
                    }
                    billItems.add(billItem);
                    addUpdateDates(postingDates, posting.getPostingDate());
                }
            }
            if (!billItems.isEmpty()) {
                responseFolio.setBillItems(billItems.toArray(new BillItem[billItems.size()]));
            }
            responseFolio.setWindowCharges(roundToHundredths(windowCharges));
            responseFolio.setWindowCredits(roundToHundredths(windowCredits));
            // Set windowLastUpdated to the latest posting date
            postingDates.stream().max(Date::compareTo)
                    .ifPresent(date -> responseFolio.setWindowLastUpdated(dateFormat.format(date)));
            folios.add(responseFolio);
        }
        if (!folios.isEmpty()) {
            folioResponse.setFolios(folios.toArray(new Folio[folios.size()]));
            folioResponse.setCurrentBalance(roundToHundredths(currentBalance));
            folioResponse.setTotalCharges(roundToHundredths(totalCharges));
            folioResponse.setTotalCredits(roundToHundredths(totalCredits));
        }
    }

    /**
     * Takes a list of individual postings from a single folio
     * and aggregates postings that share a check number,
     * all credits/debits are accumulated and descriptions
     * are combined and truncated
     * 
     * @param postings
     *                 The list of postings from a single folio
     * @return
     *                 A list of aggregated postings
     */
    private List<Posting> aggregatePostings(List<Posting> postings, List<TrxCodeInfo> trxCodesInfo) {

        List<Posting> aggregatedPostings = new ArrayList<>();
        // Group postings by their check number
        Map<String, List<Posting>> groupedPostings = postings.stream()
                .collect(Collectors.groupingBy(Posting::getReference));
        // Start with first item and accumulate charges/credits in each grouping
        for (Map.Entry<String, List<Posting>> entry : groupedPostings.entrySet()) {
            String ref = entry.getKey();
            List<Posting> checkItems = entry.getValue();
            if (checkItems.size() == 1 || !ValidationUtil.isTrueNumber(ref)) {
                aggregatedPostings.addAll(checkItems);
                continue;
            }
            Posting aggregatedItem = checkItems.get(0);
            List<Date> postedDates = new ArrayList<>();
            List<Date> transactionDates = new ArrayList<>();
            addUpdateDates(postedDates, aggregatedItem.getPostingDate());
            addUpdateDates(transactionDates, aggregatedItem.getTransactionDate());
            String aggregatedDescription = findTrxDescrip(aggregatedItem, trxCodesInfo);
            for (int i = 1; i < checkItems.size(); i++) {
                Posting item = checkItems.get(i);
                aggregateAmounts(aggregatedItem, item);
                String itemDescrip = findTrxDescrip(item, trxCodesInfo);
                if (StringUtils.isNotBlank(aggregatedDescription) && StringUtils.isNotBlank(itemDescrip)) {
                    aggregatedDescription = aggregatedDescription.concat(COMMA + itemDescrip);
                }
                addUpdateDates(transactionDates, item.getTransactionDate());
                addUpdateDates(postedDates, item.getPostingDate());
            }
            //Set transaction and posted dates to the latest of the aggregate
            postedDates.stream().max(Date::compareTo)
                    .ifPresent(date -> aggregatedItem.setPostingDate(dateFormat.format(date)));
            transactionDates.stream().max(Date::compareTo)
                    .ifPresent(date -> aggregatedItem.setTransactionDate(dateFormat.format(date)));
            if (StringUtils.isNotBlank(aggregatedDescription)) {
                aggregatedItem.setAggregatedDescription(CommonUtil.truncateText(aggregatedDescription));
            }
            aggregatedPostings.add(aggregatedItem);
        }
        return aggregatedPostings;
    }

    private void aggregateAmounts(Posting aggregatedItem, Posting item) {
        if (aggregatedItem.getCreditAmount() != null && item.getCreditAmount() != null) {
            aggregatedItem.getCreditAmount().setAmount(
                    aggregatedItem.getCreditAmount().getAmount() + item.getCreditAmount().getAmount());
        }
        if (aggregatedItem.getDebitAmount() != null && item.getDebitAmount() != null) {
            aggregatedItem.getDebitAmount().setAmount(
                    aggregatedItem.getDebitAmount().getAmount() + item.getDebitAmount().getAmount());
        }
    }

    private BillItem initBillItem(Posting posting, FolioWindow window, List<TrxCodeInfo> trxCodesInfo) {
        BillItem billItem = new BillItem();
        billItem.setDate(posting.getTransactionDate());
        if (StringUtils.isNotBlank(posting.getAggregatedDescription())) {
            billItem.setDescription(posting.getAggregatedDescription());
        }
        else {
            billItem.setDescription(findTrxDescrip(posting, trxCodesInfo));
        }
        if (StringUtils.isNotBlank(posting.getReference())) {
            billItem.setReference(posting.getReference());
        }
        billItem.setSupplement(posting.getRemark());
        Optional.ofNullable(window.getPaymentMethod()).map(PaymentMethod::getPaymentCard)
                .map(PaymentCard::getCardNumberMasked)
                .ifPresent(cardNo -> {
                    if (ServiceConstants.PAYMENT.equalsIgnoreCase(posting.getTransactionType()) && cardNo.length() >= 4) {
                        billItem.setCcLast4Digits(cardNo.substring(cardNo.length() - 4));
                    }
                });
        return billItem;
    }

    private double roundToHundredths(double amount) {
        return (double) Math.round(amount * 100) / 100;
    }

    private String findTrxDescrip(Posting posting, List<TrxCodeInfo> trxCodesInfo) {
        return trxCodesInfo.stream().filter(trx -> StringUtils.equals(trx.getTransactionCode(), posting.getTransactionCode()))
                .map(TrxCodeInfo::getDescription).findFirst().orElse(null);
    }
}
