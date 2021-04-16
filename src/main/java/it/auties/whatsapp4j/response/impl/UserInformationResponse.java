package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;

import java.util.List;
import java.util.Objects;

/**
 * A json model that contains information only about the user linked to the session
 */
public final class UserInformationResponse implements JsonResponseModel<UserInformationResponse> {
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

    /**
     */
    public UserInformationResponse(String ref, String wid, boolean connected,
                                   String isResponse, String serverToken,
                                   String browserToken, String clientToken, String lc,
                                   String lg, String locales,
                                   List<Integer> protoVersion, int binVersion, int battery,
                                   boolean plugged, String platform,
                                   FeaturesInformation features,
                                   PhoneInformation phone,
                                   String pushname, String secret,
                                   int tos) {
        this.ref = ref;
        this.wid = wid;
        this.connected = connected;
        this.isResponse = isResponse;
        this.serverToken = serverToken;
        this.browserToken = browserToken;
        this.clientToken = clientToken;
        this.lc = lc;
        this.lg = lg;
        this.locales = locales;
        this.protoVersion = protoVersion;
        this.binVersion = binVersion;
        this.battery = battery;
        this.plugged = plugged;
        this.platform = platform;
        this.features = features;
        this.phone = phone;
        this.pushname = pushname;
        this.secret = secret;
        this.tos = tos;
    }

    public String ref() {
        return ref;
    }

    public String wid() {
        return wid;
    }

    public boolean connected() {
        return connected;
    }

    public String isResponse() {
        return isResponse;
    }

    public String serverToken() {
        return serverToken;
    }

    public String browserToken() {
        return browserToken;
    }

    public String clientToken() {
        return clientToken;
    }

    public String lc() {
        return lc;
    }

    public String lg() {
        return lg;
    }

    public String locales() {
        return locales;
    }

    public List<Integer> protoVersion() {
        return protoVersion;
    }

    public int binVersion() {
        return binVersion;
    }

    public int battery() {
        return battery;
    }

    public boolean plugged() {
        return plugged;
    }

    public String platform() {
        return platform;
    }

    public FeaturesInformation features() {
        return features;
    }

    public PhoneInformation phone() {
        return phone;
    }

    public String pushname() {
        return pushname;
    }

    public String secret() {
        return secret;
    }

    public int tos() {
        return tos;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (UserInformationResponse) obj;
        return Objects.equals(this.ref, that.ref) &&
                Objects.equals(this.wid, that.wid) &&
                this.connected == that.connected &&
                Objects.equals(this.isResponse, that.isResponse) &&
                Objects.equals(this.serverToken, that.serverToken) &&
                Objects.equals(this.browserToken, that.browserToken) &&
                Objects.equals(this.clientToken, that.clientToken) &&
                Objects.equals(this.lc, that.lc) &&
                Objects.equals(this.lg, that.lg) &&
                Objects.equals(this.locales, that.locales) &&
                Objects.equals(this.protoVersion, that.protoVersion) &&
                this.binVersion == that.binVersion &&
                this.battery == that.battery &&
                this.plugged == that.plugged &&
                Objects.equals(this.platform, that.platform) &&
                Objects.equals(this.features, that.features) &&
                Objects.equals(this.phone, that.phone) &&
                Objects.equals(this.pushname, that.pushname) &&
                Objects.equals(this.secret, that.secret) &&
                this.tos == that.tos;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, wid, connected, isResponse, serverToken, browserToken, clientToken, lc, lg, locales, protoVersion, binVersion, battery, plugged, platform, features, phone, pushname, secret, tos);
    }

    @Override
    public String toString() {
        return "UserInformationResponse[" +
                "ref=" + ref + ", " +
                "wid=" + wid + ", " +
                "connected=" + connected + ", " +
                "isResponse=" + isResponse + ", " +
                "serverToken=" + serverToken + ", " +
                "browserToken=" + browserToken + ", " +
                "clientToken=" + clientToken + ", " +
                "lc=" + lc + ", " +
                "lg=" + lg + ", " +
                "locales=" + locales + ", " +
                "protoVersion=" + protoVersion + ", " +
                "binVersion=" + binVersion + ", " +
                "battery=" + battery + ", " +
                "plugged=" + plugged + ", " +
                "platform=" + platform + ", " +
                "features=" + features + ", " +
                "phone=" + phone + ", " +
                "pushname=" + pushname + ", " +
                "secret=" + secret + ", " +
                "tos=" + tos + ']';
    }

}