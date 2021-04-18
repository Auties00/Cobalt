package it.auties.whatsapp4j.response.impl;


import java.util.Objects;

/**
 * A json model that contains information about the phone of the user associated with this session
 * This record should only be used by {@link UserInformationResponse}
 *
 */
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

    public String waVersion() {
        return waVersion;
    }

    public String mcc() {
        return mcc;
    }

    public String mnc() {
        return mnc;
    }

    public String osVersion() {
        return osVersion;
    }

    public String osBuildNumber() {
        return osBuildNumber;
    }

    public String deviceManufacturer() {
        return deviceManufacturer;
    }

    public String deviceModel() {
        return deviceModel;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PhoneInformation) obj;
        return Objects.equals(this.waVersion, that.waVersion) &&
                Objects.equals(this.mcc, that.mcc) &&
                Objects.equals(this.mnc, that.mnc) &&
                Objects.equals(this.osVersion, that.osVersion) &&
                Objects.equals(this.osBuildNumber, that.osBuildNumber) &&
                Objects.equals(this.deviceManufacturer, that.deviceManufacturer) &&
                Objects.equals(this.deviceModel, that.deviceModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(waVersion, mcc, mnc, osVersion, osBuildNumber, deviceManufacturer, deviceModel);
    }

    @Override
    public String toString() {
        return "PhoneInformation[" +
                "waVersion=" + waVersion + ", " +
                "mcc=" + mcc + ", " +
                "mnc=" + mnc + ", " +
                "osVersion=" + osVersion + ", " +
                "osBuildNumber=" + osBuildNumber + ", " +
                "deviceManufacturer=" + deviceManufacturer + ", " +
                "deviceModel=" + deviceModel + ']';
    }

}