package com.mgmresorts.booking.room.reservation.search.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.azure.cosmos.implementation.Document;

import lombok.Data;

public class CommonUtilTest {

    @Test
    public void testMaskCCNumber() {
        assertEquals("XXXXXXXXXXXX4444", CommonUtil.maskCardNumber("5555555555554444", 'X'));
    }

    @Test
    public void testMaskCCNumberInvalid() {
        assertEquals("44", CommonUtil.maskCardNumber("44", 'X'));
    }

    @Test
    public void testConvertToJson() {
        List<Object> objList = new ArrayList<>();

        Document doc = new Document();
        doc.setId(UUID.randomUUID().toString());
        objList.add(doc);

        assertTrue(StringUtils.isNotEmpty(CommonUtil.convertToJson(objList)));
    }

    @Test
    public void testConvertToObj() {
  
        String json = "{\"val\":\"123\"}";
  
        TestObj result = CommonUtil.convertToObj(json, TestObj.class);
        
        assertNotNull(result);
        assertEquals("123", result.getVal());
    }

    @Test
    public void testMaskQuery() {
        String queryWithName = "SELECT distinct(r) AS result FROM ROOT r JOIN p IN r.resProfiles.resProfile "
                + "WHERE p.profile.individualName.nameSur=\"Rebecca\" AND p.profile.individualName.nameFirst=\"Brown\" AND p.profile.profileType=\"GUEST\" AND r.reservationID=\"796566664\"";

        String maskedString = CommonUtil.maskQuery(queryWithName);

        assertNotEquals(queryWithName, maskedString);
        assertEquals("SELECT distinct(r) AS result FROM ROOT r JOIN p IN r.resProfiles.resProfile "
                + "WHERE p.profile.individualName.nameSur=\"--REDACTED--\" AND p.profile.individualName.nameFirst=\"--REDACTED--\" AND p.profile.profileType=\"GUEST\" AND r.reservationID=\"796566664\"",
                maskedString);

    }

    @Test
    public void testGetCardLast4() {
        assertEquals("4444", CommonUtil.getCardLast4("123454444"));
        assertEquals("4444", CommonUtil.getCardLast4("4444"));
        assertEquals("4444", CommonUtil.getCardLast4("3x454444"));
        assertEquals("4444", CommonUtil.getCardLast4("5555550A00102E3UJZ27YC954444"));
    }

    @Test
    public void testFindLongestCommonPrefix() {
        String[] test = {"test"};
        String[] test1 = {"test", "testing", "testinggg"};
        String[] test2 = {"no", "common"};
        String[] test3 = {};
        String[] test4 = {""};
        String[] test5 = {"vda market cafe tax", "vda market cafe tip", "vda market cafe special"};
        String[] test6 = {" vda market cafe tax", " vda market cafe tip", " vda market cafe special"};
        String[] test7 = {"vda tax", "vda tip"};

        assertEquals("test", CommonUtil.findLongestCommonPrefix(test));
        assertEquals("test", CommonUtil.findLongestCommonPrefix(test1));
        assertEquals("", CommonUtil.findLongestCommonPrefix(test2));
        assertEquals("", CommonUtil.findLongestCommonPrefix(test3));
        assertEquals("", CommonUtil.findLongestCommonPrefix(test4));
        assertEquals("vda market cafe ", CommonUtil.findLongestCommonPrefix(test5));
        assertEquals("vda market cafe ", CommonUtil.findLongestCommonPrefix(test6));
        assertEquals("vda ", CommonUtil.findLongestCommonPrefix(test7));
    }

    @Test
    public void testRemoveCommonPrefixAndAppend() {
        String[] testTokens = {"vda market cafe tax", "vda market cafe tip", "vda market cafe special"};
        assertEquals("vda market cafe tax, tip, special", CommonUtil.removeCommonPrefixAndAppend(testTokens, "vda market cafe "));

        String[] testTokens1 = {"vda1", "vda2", "vda3"};
        assertEquals("vda 1, 2, 3", CommonUtil.removeCommonPrefixAndAppend(testTokens1, "vda"));

        String[] testTokens2 = {"1vda market cafe tax", "2vda market cafe tip", "3vda market cafe special"};
        assertEquals("1vda market cafe tax, 2vda market cafe tip, 3vda market cafe special", CommonUtil.removeCommonPrefixAndAppend(testTokens2, ""));


    }

    @Test
    public void testisBasicSearchFunction() {
        assertTrue(CommonUtil.isBasicSearchFunction("searchUnprotected"));
    }

    @Test
    public void testisBasicSearchFunctionEmpty() {
        assertFalse(CommonUtil.isBasicSearchFunction(""));
    }

    @Test
    public void testisBasicSearchFunctionFalse() {
        assertFalse(CommonUtil.isBasicSearchFunction("search"));
    }

    @Test
    public void testTruncateText() {
        String testStr = "Transaction Descrip 1, Transaction Descrip 2";
        assertEquals("Transaction Descrip 1, 2", CommonUtil.truncateText(testStr));
    }

    @Test
    public void testTruncateTextWithSingleDescription() {
        String testStr = "Transaction Descrip 1";
        assertEquals("Transaction Descrip 1", CommonUtil.truncateText(testStr));
    }

    @Data
    private static class TestObj {
        private String val;
    }
}
