package com.mgmresorts.booking.room.reservation.search.models;

import com.microsoft.azure.functions.HttpResponseMessage;

import lombok.Data;

@Data
public class BaseResponse {

    private HttpResponseMessage responseMsg;
}
