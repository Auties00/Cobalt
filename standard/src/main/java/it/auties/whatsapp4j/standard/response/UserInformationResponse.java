package it.auties.whatsapp4j.standard.response;

import it.auties.whatsapp4j.common.response.JsonResponseModel;

import java.util.List;

/**
 * A json model that contains information only about the user linked to the session
 */
public final record UserInformationResponse(String ref, String wid, boolean connected, String isResponse, String serverToken,
                                            String browserToken, String clientToken, String lc, String lg, String locales,
                                            List<Integer> protoVersion, int binVersion, int battery, boolean plugged,
                                            String platform, FeaturesInformation features, PhoneInformation phone,
                                            String pushname, String secret, int tos) implements JsonResponseModel {
}