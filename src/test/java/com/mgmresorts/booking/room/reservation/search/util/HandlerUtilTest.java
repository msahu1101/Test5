package com.mgmresorts.booking.room.reservation.search.util;

import static com.mgmresorts.booking.room.reservation.search.util.ServiceConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class HandlerUtilTest {

    @Test
    void testGetUserClaims() {
        String testToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2ODIzMTcyMDgsImV4cCI6MTcxMzg1MzIwOCwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsImNvbS5tZ20ubWxpZmVfbnVtYmVyIjoiMTIzIiwiY29tLm1nbS5pZCI6IjQ1NiJ9.cFNFouqCYSgsqXGVz9yOAciv4_zFAOopLaCijnWtrR4";
        Map<String, String> params = new HashMap<>();
        params.put(AUTHORIZATION, testToken);

        Map<String, String> result = HandlerUtil.getUserClaims(params);
        
        assertEquals("123", result.get("mlifeNumber"));
        assertEquals("456", result.get("mgmId"));
        assertEquals("true", result.get(JWT_EXISTS));
    }

    @Test
    void testGetUserClaims_employeeToken() {
        String testToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2ODIzMTcyMDgsImV4cCI6MTcxMzg1MzIwOCwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsImNvbS5tZ20ubWxpZmVfbnVtYmVyIjoiMTIzIiwiY29tLm1nbS5pZCI6IjQ1NiIsImVtcGxveWVlX251bWJlciI6IjEyMyJ9.kEM20qF8tFgJK24WMV3ZDs9OYoJMgAPQ7OAajqsNZIw";
        Map<String, String> params = new HashMap<>();
        params.put(AUTHORIZATION, testToken);

        Map<String, String> result = HandlerUtil.getUserClaims(params);

        assertEquals(1, result.size());
        assertEquals("true", result.get(JWT_EXISTS));
    }

    @Test
    void testGetUserClaims_noAuthInHeaders() {
        Map<String, String> params = new HashMap<>();

        Map<String, String> result = HandlerUtil.getUserClaims(params);

        assertEquals(0, result.size());
    }

    @Test
    void testGetMgmRole() {
        String testToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2ODIzMTcyMDgsImV4cCI6MTcxMzg1MzIwOCwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIm1nbV9yb2xlIjoic2VydmljZSJ9.TdAEf47ZVSRHGT-LbHyWCayCU-0Kw1RDooV_SZCiO5M";
        Map<String, String> headers = new HashMap<>();
        headers.put(AUTHORIZATION, testToken);

        String result = HandlerUtil.getMgmRole(headers);

        assertEquals("service", result);
    }

    @Test
    void testGetMgmRole_noAuthInHeaders() {
        Map<String, String> headers = new HashMap<>();

        String result = HandlerUtil.getMgmRole(headers);

        assertEquals(EMPTY, result);
    }
    
}
