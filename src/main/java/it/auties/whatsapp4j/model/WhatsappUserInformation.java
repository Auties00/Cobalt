// Class generated from JSON model
package it.auties.whatsapp4j.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhatsappUserInformation {
    @JsonProperty("ref")
    private String ref;
    @JsonProperty("wid")
    private String wid;
    @JsonProperty("connected")
    private boolean connected;
    @JsonProperty("isResponse")
    private String isResponse;
    @JsonProperty("serverToken")
    private String serverToken;
    @JsonProperty("browserToken")
    private String browserToken;
    @JsonProperty("clientToken")
    private String clientToken;
    @JsonProperty("lc")
    private String lc;
    @JsonProperty("lg")
    private String lg;
    @JsonProperty("locales")
    private String locales;
    @JsonProperty("protoVersion")
    private List<Integer> protoVersion;
    @JsonProperty("binVersion")
    private int binVersion;
    @JsonProperty("battery")
    private int battery;
    @JsonProperty("plugged")
    private boolean plugged;
    @JsonProperty("platform")
    private String platform;
    @JsonProperty("features")
    private Features features;
    @JsonProperty("phone")
    private Phone phone;
    @JsonProperty("pushname")
    private String pushname;
    @JsonProperty("secret")
    private String secret;
    @JsonProperty("tos")
    private int tos;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Features {
        @JsonProperty("URL")
        private boolean url;
        @JsonProperty("FLAGS")
        private String flags;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Phone {
        @JsonProperty("wa_version")
        private String waVersion;
        @JsonProperty("mcc")
        private String mcc;
        @JsonProperty("mnc")
        private String mnc;
        @JsonProperty("os_version")
        private String osVersion;
        @JsonProperty("device_manufacturer")
        private String deviceManufacturer;
        @JsonProperty("device_model")
        private String deviceModel;
        @JsonProperty("os_build_number")
        private String osBuildNumber;
    }
}