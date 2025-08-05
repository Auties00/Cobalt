package it.auties.whatsapp.model.response;

import com.alibaba.fastjson2.JSON;
import it.auties.whatsapp.model.mobile.PhoneNumber;

import java.util.Optional;

/**
 * A model that represents a newsletters from Whatsapp regarding the registration of a phone number
 */
public final class RegistrationResponse {
    private final PhoneNumber number;
    private final long lid;
    private final String status;
    private final String errorReason;
    private final String method;
    private final int codeLength;
    private final int notifyAfter;
    private final long retryAfter;
    private final long voiceLength;
    private final long callWait;
    private final long smsWait;
    private final boolean flashType;
    private final long whatsappWait;
    private final boolean securityCodeSet;
    private final String imageCaptcha;
    private final String audioCaptcha;
    private final String cert;
    private final boolean otpEligible;
    private final boolean smsEligible;
    private final boolean possibleMigration;
    private final boolean autoConfigure;
    private final String wipeToken;

    private RegistrationResponse(PhoneNumber number, long lid, String status, String errorReason, String method, int codeLength, int notifyAfter, long retryAfter, long voiceLength, long callWait, long smsWait, boolean flashType, long whatsappWait, boolean securityCodeSet, String imageCaptcha, String audioCaptcha, String cert, boolean otpEligible, boolean smsEligible, boolean possibleMigration, boolean autoConfigure, String wipeToken) {
        this.number = number;
        this.lid = lid;
        this.status = status;
        this.errorReason = errorReason;
        this.method = method;
        this.codeLength = codeLength;
        this.notifyAfter = notifyAfter;
        this.retryAfter = retryAfter;
        this.voiceLength = voiceLength;
        this.callWait = callWait;
        this.smsWait = smsWait;
        this.flashType = flashType;
        this.whatsappWait = whatsappWait;
        this.securityCodeSet = securityCodeSet;
        this.imageCaptcha = imageCaptcha;
        this.audioCaptcha = audioCaptcha;
        this.cert = cert;
        this.otpEligible = otpEligible;
        this.smsEligible = smsEligible;
        this.possibleMigration = possibleMigration;
        this.autoConfigure = autoConfigure;
        this.wipeToken = wipeToken;
    }

    public static Optional<RegistrationResponse> ofJson(byte[] json) {
        if(json == null) {
            return Optional.empty();
        }

        var jsonObject = JSON.parseObject(json);
        if(jsonObject == null) {
            return Optional.empty();
        }

        var login = jsonObject.getLong("login");
        if(login == null) {
            return Optional.empty();
        }

        var phoneNumber = PhoneNumber.of(login);
        if(phoneNumber.isEmpty()) {
            return Optional.empty();
        }

        var lid = jsonObject.getLongValue("lid", 0);
        var status = jsonObject.getString("status");
        var errorReason = jsonObject.getString("reason");
        var method = jsonObject.getString("method");
        var codeLength = jsonObject.getIntValue("length", 0);
        var notifyAfter = jsonObject.getIntValue("notify_after", 0);
        var retryAfter = jsonObject.getLongValue("retry_after", 0);
        var voiceLength = jsonObject.getLongValue("voice_length", 0);
        var callWait = jsonObject.getLongValue("voice_wait", 0);
        var smsWait = jsonObject.getLongValue("sms_wait", 0);
        var flashType = jsonObject.getBooleanValue("flash_type", false);
        var whatsappWait = jsonObject.getLongValue("wa_old_wait", 0);
        var securityCodeSet = jsonObject.getBooleanValue("security_code_set", false);
        var imageCaptcha = jsonObject.getString("image_blob");
        var audioCaptcha = jsonObject.getString("audio_blob");
        var cert = jsonObject.getString("cert");
        var otpEligible = jsonObject.getBooleanValue("wa_old_eligible", false);
        var smsEligible = jsonObject.getBooleanValue("send_sms_eligible", false);
        var possibleMigration = jsonObject.getBooleanValue("possible_migration", false);
        var autoConfigure = jsonObject.getBooleanValue("autoconf_type", false);
        var wipeToken = jsonObject.getString("wipe_token");
        var result = new RegistrationResponse(phoneNumber.get(), lid, status, errorReason, method, codeLength, notifyAfter, retryAfter, voiceLength, callWait, smsWait, flashType, whatsappWait, securityCodeSet, imageCaptcha, audioCaptcha, cert, otpEligible, smsEligible, possibleMigration, autoConfigure, wipeToken);
        return Optional.of(result);
    }

    public PhoneNumber number() {
        return number;
    }

    public long lid() {
        return lid;
    }

    public String status() {
        return status;
    }

    public String errorReason() {
        return errorReason;
    }

    public String method() {
        return method;
    }

    public int codeLength() {
        return codeLength;
    }

    public int notifyAfter() {
        return notifyAfter;
    }

    public long retryAfter() {
        return retryAfter;
    }

    public long voiceLength() {
        return voiceLength;
    }

    public long callWait() {
        return callWait;
    }

    public long smsWait() {
        return smsWait;
    }

    public boolean flashType() {
        return flashType;
    }

    public long whatsappWait() {
        return whatsappWait;
    }

    public boolean securityCodeSet() {
        return securityCodeSet;
    }

    public String imageCaptcha() {
        return imageCaptcha;
    }

    public String audioCaptcha() {
        return audioCaptcha;
    }

    public String cert() {
        return cert;
    }

    public boolean otpEligible() {
        return otpEligible;
    }

    public boolean smsEligible() {
        return smsEligible;
    }

    public boolean possibleMigration() {
        return possibleMigration;
    }

    public boolean autoConfigure() {
        return autoConfigure;
    }

    public String wipeToken() {
        return wipeToken;
    }
}