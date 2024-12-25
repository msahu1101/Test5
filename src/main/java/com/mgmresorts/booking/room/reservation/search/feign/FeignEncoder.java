package com.mgmresorts.booking.room.reservation.search.feign;

import java.lang.reflect.Type;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.form.FormEncoder;
import feign.gson.GsonEncoder;

public class FeignEncoder implements Encoder {

    private Encoder jsonEncoder = new GsonEncoder();
    private Encoder formEncoder = new FormEncoder();

    @Override
    public void encode(Object obj, Type bodyType, RequestTemplate template) throws EncodeException {
        String contentType = template.headers().get("Content-Type").stream().findFirst().orElse("");
        if (contentType.contains("application/x-www-form-urlencoded")) {
            formEncoder.encode(obj, bodyType, template);
        } else {
            jsonEncoder.encode(obj, bodyType, template);
        }
    }

}
