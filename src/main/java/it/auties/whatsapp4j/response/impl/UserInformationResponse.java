package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

/**
 * A json model that contains information only about the user linked to the session
 */
@AllArgsConstructor
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class UserInformationResponse implements JsonResponseModel {
    private final String ref;
    private final String wid;
    private final boolean connected;
    private final String isResponse;
    private final String serverToken;
    private final String browserToken;
    private final String clientToken;
    private final String lc;
    private final String lg;
    private final String locales;
    private final List<Integer> protoVersion;
    private final int binVersion;
    private final int battery;
    private final boolean plugged;
    private final String platform;
    private final FeaturesInformation features;
    private final PhoneInformation phone;
    private final String pushname;
    private final String secret;
    private final int tos;
}