package com.mgmresorts.booking.room.reservation.search.models.opera;

import lombok.Data;

@Data
public class OperaTokenResponse {

    private String token_type;
    private int expires_in;
    private String access_token;
    private String scope;
}
