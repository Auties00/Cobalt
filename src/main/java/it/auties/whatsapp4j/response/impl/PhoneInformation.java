package it.auties.whatsapp4j.response.impl;

import lombok.Builder;
import org.jetbrains.annotations.Nullable;

/**
 * A json model that contains information about the phone of the user associated with this session
 * This record should only be used by {@link UserInformationResponse}
 *
 * @param waVersion a nullable String that represents the version of Whatsapp installed on the phone
 * @param mcc a nullable String that represents the three digits country code for the SIM used for Whatsapp on the phone
 * @param mnc a nullable String that represents a two ord three digits network code linked to the SIM used for Whatsapp on the phone
 * @param osVersion a nullable String that represents the version used by the operating system installed on the phone
 * @param osBuildNumber a nullable String that represents the build number for the operating system installed on the phone
 * @param deviceManufacturer a nullable String that represents the brand of the phone
 * @param deviceModel a nullable String that represents the model of the phone
 */
public record PhoneInformation(@Nullable String waVersion, @Nullable String mcc, @Nullable String mnc,
                        @Nullable String osVersion, @Nullable String osBuildNumber,
                        @Nullable String deviceManufacturer, @Nullable String deviceModel) {
}