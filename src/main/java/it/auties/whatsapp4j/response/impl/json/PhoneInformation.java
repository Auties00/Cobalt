package it.auties.whatsapp4j.response.impl.json;

import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record PhoneInformation(String waVersion, String mcc, String mnc,
                           String osVersion, String deviceManufacturer,
                           String deviceModel, String osBuildNumber) {
}