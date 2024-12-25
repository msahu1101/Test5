package com.mgmresorts.booking.room.reservation.search.response;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

public @Data class ResponseWithHeaders {

    private String response;
    private Map<String, String> headers = new HashMap<>();
}
