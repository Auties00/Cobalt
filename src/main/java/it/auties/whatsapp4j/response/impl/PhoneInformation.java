package it.auties.whatsapp4j.response.impl;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * A json model that contains information about the phone of the user associated with this session
 * This record should only be used by {@link UserInformationResponse}
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class PhoneInformation {
    private final String waVersion;
    private final String mcc;
    private final String mnc;
    private final String osVersion;
    private final String osBuildNumber;
    private final String deviceManufacturer;
    private final String deviceModel;

    /**
     * @param waVersion a nullable String that represents the version of Whatsapp installed on the phone
     * @param mcc a nullable String that represents the three digits country code for the SIM used for Whatsapp on the phone
     * @param mnc a nullable String that represents a two ord three digits network code linked to the SIM used for Whatsapp on the phone
     * @param osVersion a nullable String that represents the version used by the operating system installed on the phone
     * @param osBuildNumber a nullable String that represents the build number for the operating system installed on the phone
     * @param deviceManufacturer a nullable String that represents the brand of the phone
     * @param deviceModel a nullable String that represents the model of the phone
     */
    public PhoneInformation(String waVersion, String mcc, String mnc,
                            String osVersion, String osBuildNumber,
                            String deviceManufacturer, String deviceModel) {
        this.waVersion = waVersion;
        this.mcc = mcc;
        this.mnc = mnc;
        this.osVersion = osVersion;
        this.osBuildNumber = osBuildNumber;
        this.deviceManufacturer = deviceManufacturer;
        this.deviceModel = deviceModel;
    }
}