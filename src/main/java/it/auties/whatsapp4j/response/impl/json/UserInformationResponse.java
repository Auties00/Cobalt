package it.auties.whatsapp4j.response.impl.json;

import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Jacksonized
public record UserInformationResponse(String ref, String wid, boolean connected,
                                      String isResponse, String serverToken,
                                      String browserToken, String clientToken, String lc,
                                      String lg, String locales,
                                      List<Integer> protoVersion, int binVersion, int battery,
                                      boolean plugged, String platform,
                                      FeaturesInformation features,
                                      PhoneInformation phone,
                                      String pushname, String secret,
                                      int tos) implements JsonResponseModel {
}