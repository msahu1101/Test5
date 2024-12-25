package com.mgmresorts.booking.room.reservation.search.models.opera;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperaTokenRequest {
    
    private String client_id;
    private String client_secret;
    private String grant_type;
    private String scope;
    private String username;
    private String password;
}
