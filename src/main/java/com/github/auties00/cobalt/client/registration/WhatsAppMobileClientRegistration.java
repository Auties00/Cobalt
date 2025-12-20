package com.github.auties00.cobalt.client.registration;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.WhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.client.info.WhatsAppMobileClientInfo;
import com.github.auties00.cobalt.exception.MobileRegistrationException;
import com.github.auties00.cobalt.model.business.BusinessVerifiedNameCertificateBuilder;
import com.github.auties00.cobalt.model.business.BusinessVerifiedNameCertificateSpec;
import com.github.auties00.cobalt.model.business.BusinessVerifiedNameDetailsBuilder;
import com.github.auties00.cobalt.model.business.BusinessVerifiedNameDetailsSpec;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.store.WhatsAppStore;
import com.github.auties00.cobalt.util.SecureBytes;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;

public abstract sealed class WhatsAppMobileClientRegistration implements AutoCloseable
        permits WhatsAppAndroidClientRegistration, WhatsAppIosClientRegistration {
    public static final String MOBILE_REGISTRATION_ENDPOINT = "https://v.whatsapp.net/v2";
    private static final byte[] REGISTRATION_PUBLIC_KEY = HexFormat.of().parseHex("8e8c0f74c3ebc5d7a6865c6c3c843856b06121cce8ea774d22fb6f122512302d");
    private static final String SIGNAL_PUBLIC_KEY_TYPE = Base64.getUrlEncoder().encodeToString(new byte[]{SignalIdentityPublicKey.type()});

    protected final HttpClient httpClient;
    protected final WhatsAppStore store;
    protected final WhatsAppClientVerificationHandler.Mobile verification;

    protected WhatsAppMobileClientRegistration(WhatsAppStore store, WhatsAppClientVerificationHandler.Mobile verification) {
        Objects.requireNonNull(store, "store cannot be null");
        Objects.requireNonNull(verification, "verification cannot be null");
        this.store = store;
        this.verification = verification;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public static WhatsAppMobileClientRegistration of(WhatsAppStore store, WhatsAppClientVerificationHandler.Mobile verification) {
        return switch (store.device().platform()) {
            case ANDROID -> new WhatsAppAndroidClientRegistration(store, verification);
            case IOS -> new WhatsAppIosClientRegistration(store, verification);
            default -> throw new IllegalArgumentException("Unsupported platform: " + store.device().platform());
        };
    }

    protected abstract String[] getRequestVerificationCodeParameters(String method);

    protected abstract String generateFdid();

    protected abstract HttpRequest createRequest(String path, String body);

    public void register() {
        try {
            assertRegistrationKeys();
            requestVerificationCodeIfNecessary();
            sendVerificationCode();
        } catch (IOException | InterruptedException exception) {
            throw new MobileRegistrationException(exception);
        }
    }

    // Pretty much this method checks if an account with the exact keys we provide already exists
    // If the api answers "incorrect" it means that no account with those keys exists so we can register
    private void assertRegistrationKeys() throws IOException, InterruptedException {
        // Get request data
        var attrs = getRegistrationOptions(false);

        // First attempt
        var result = sendRequest("/exist", attrs);
        var response = JSON.parseObject(result);
        if (Objects.equals(response.getString("reason"), "incorrect")) {
            return;
        }

        // Second attempt
        result = sendRequest("/exist", attrs);
        response = JSON.parseObject(result);
        if (Objects.equals(response.getString("reason"), "incorrect")) {
            return;
        }

        // Error
        throw new MobileRegistrationException("Cannot get account data", new String(result));
    }


    private void requestVerificationCodeIfNecessary() throws IOException, InterruptedException {
        var codeResult = verification.requestMethod();
        if (codeResult.isEmpty()) {
            return;
        }

        requestVerificationCode(codeResult.get());
        saveRegistrationStatus(false);
    }

    private void requestVerificationCode(String method) throws IOException, InterruptedException {
        String lastError = null;
        while (true) {
            var params = getRequestVerificationCodeParameters(method);
            var attrs = getRegistrationOptions(true, params);
            var result = sendRequest("/code", attrs);
            var response = JSON.parseObject(result);
            var status = response.getString("status");
            if (isSuccessful(status)) {
                return;
            }

            var reason = response.getString("reason");
            if(isTooRecent(reason)) {
                throw new MobileRegistrationException("Please wait before trying to register this phone value again. Don't spam!", new String(result));
            }

            if(isRegistrationBlocked(reason)) {
                var resultJson = new String(result);
                if(method.equals("wa_old")) {
                    throw new MobileRegistrationException("The registration attempt was blocked by Whatsapp: you might want to change platform(iOS/Android) or try using a residential proxy (don't spam)", resultJson);
                }else {
                    throw new MobileRegistrationException("The registration attempt was blocked by Whatsapp: please try using a Whatsapp OTP as a verification method", resultJson);
                }
            }

            if (Objects.equals(reason, lastError)) {
                throw new MobileRegistrationException("An error occurred while registering: " + reason, new String(result));
            }

            lastError = reason;
        }
    }

    private boolean isTooRecent(String reason) {
        return reason.equalsIgnoreCase("too_recent")
               || reason.equalsIgnoreCase("too_many")
               || reason.equalsIgnoreCase("too_many_guesses")
               || reason.equalsIgnoreCase("too_many_all_methods");
    }

    private boolean isRegistrationBlocked(String reason) {
        return reason.equalsIgnoreCase("no_routes");
    }


    public void sendVerificationCode() throws IOException, InterruptedException {
        var code = verification.verificationCode();
        var attrs = getRegistrationOptions(true, "code", normalizeCodeResult(code));
        var result = sendRequest("/register", attrs);
        var response = JSON.parseObject(result);
        var status = response.getString("status");
        if (isSuccessful(status)) {
            saveRegistrationStatus(true);
            return;
        }
        throw new MobileRegistrationException("Cannot confirm registration", new String(result));
    }

    private void saveRegistrationStatus(boolean registered) {
        store.setRegistered(registered);
        if (registered) {
            var phoneNumber = store.phoneNumber()
                    .orElseThrow(() -> new MobileRegistrationException("Phone number wasn't set"));
            var jid = Jid.of(phoneNumber);
            store.setJid(jid);
        }
        store.serialize();
    }

    private String normalizeCodeResult(String code) {
        return code.replaceAll("-", "")
                .trim();
    }

    private boolean isSuccessful(String status) {
        return status.equalsIgnoreCase("ok")
               || status.equalsIgnoreCase("sent")
               || status.equalsIgnoreCase("verified");
    }

    private byte[] sendRequest(String path, String params) throws IOException, InterruptedException {
        try {
            var keypair = SignalIdentityKeyPair.random();
            var key = Curve25519.sharedKey(keypair.privateKey().toEncodedPoint(), REGISTRATION_PUBLIC_KEY);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, new byte[12])
            );
            var result = cipher.doFinal(params.getBytes(StandardCharsets.UTF_8));
            var cipheredParameters = Base64.getUrlEncoder().encodeToString(SecureBytes.concat(keypair.publicKey().toEncodedPoint(), result));
            var requestBuilder = createRequest(path, cipheredParameters);
            var response = httpClient.send(requestBuilder, HttpResponse.BodyHandlers.ofByteArray());
            if(response.statusCode() != 200) {
                throw new RuntimeException("Cannot send request to " + path + ": status code" + response.statusCode());
            }
            return response.body();
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot encrypt request", exception);
        }
    }

    private String getRegistrationOptions(boolean useToken, String... attributes) {
        var phoneNumber = getPhoneNumber(store);
        var token = getToken(phoneNumber, useToken);
        var certificate = generateBusinessCertificate();
        var fdid = generateFdid();
        var registrationParams = toFormParams(
                "cc", String.valueOf(phoneNumber.getCountryCode()),
                "in", String.valueOf(phoneNumber.getNationalNumber()),
                "rc", String.valueOf(store.releaseChannel().index()),
                "lg", "en",
                "lc", "US",
                "authkey", Base64.getUrlEncoder().encodeToString(store.noiseKeyPair().publicKey().toEncodedPoint()),
                "vname", certificate,
                "e_regid", Base64.getUrlEncoder().encodeToString(SecureBytes.intToBytes(store.registrationId(), 4)),
                "e_keytype", SIGNAL_PUBLIC_KEY_TYPE,
                "e_ident", Base64.getUrlEncoder().encodeToString(store.identityKeyPair().publicKey().toEncodedPoint()),
                "e_skey_id", Base64.getUrlEncoder().encodeToString(SecureBytes.intToBytes(store.signedKeyPair().id(), 3)),
                "e_skey_val", Base64.getUrlEncoder().encodeToString(store.signedKeyPair().publicKey().toEncodedPoint()),
                "e_skey_sig", Base64.getUrlEncoder().encodeToString(store.signedKeyPair().signature()),
                "fdid", fdid,
                "expid", Base64.getUrlEncoder().encodeToString(store.deviceId()),
                "id", toUrlHex(store.identityId()),
                "token", useToken ? token : null
        );
        var additionalParams = toFormParams(attributes);
        if (additionalParams.isEmpty()) {
            return registrationParams;
        } else if(registrationParams.isEmpty()) {
            return additionalParams;
        } else {
            return registrationParams + "&" + additionalParams;
        }
    }

    private String getToken(PhoneNumber phoneNumber, boolean useToken) {
        if (!useToken) {
            return null;
        }

        var info = WhatsAppMobileClientInfo.of(store.device().platform());
        return info.computeRegistrationToken(phoneNumber.getNationalNumber());
    }

    protected String generateBusinessCertificate() {
        if(!store.device().platform().isBusiness()) {
            return null;
        }

        var details = new BusinessVerifiedNameDetailsBuilder()
                .name("")
                .issuer("smb:wa")
                .serial(Math.abs(new SecureRandom().nextLong()))
                .build();
        var encodedDetails = BusinessVerifiedNameDetailsSpec.encode(details);
        var certificate = new BusinessVerifiedNameCertificateBuilder()
                .encodedDetails(encodedDetails)
                .signature(Curve25519.sign(store.identityKeyPair().privateKey().toEncodedPoint(), encodedDetails))
                .build();
        return Base64.getUrlEncoder().encodeToString(BusinessVerifiedNameCertificateSpec.encode(certificate));
    }

    protected static PhoneNumber getPhoneNumber(WhatsAppStore store) {
        var phoneNumber = store.phoneNumber()
                .orElseThrow(() -> new MobileRegistrationException("Phone number wasn't set"));
        try {
            return PhoneNumberUtil.getInstance()
                    .parse("+" + phoneNumber, null);
        }catch (NumberParseException exception) {
            throw new MobileRegistrationException("Malformed phone number: " + phoneNumber);
        }
    }

    protected String toUrlHex(byte[] buffer) {
        var id = new StringBuilder();
        for (var x : buffer) {
            id.append(String.format("%%%02x", x));
        }
        return id.toString().toUpperCase(Locale.ROOT);
    }

    private String toFormParams(String... entries) {
        if (entries == null) {
            return "";
        }

        var length = entries.length;
        if ((length & 1) != 0) {
            throw new IllegalArgumentException("Odd form patches");
        }

        var result = new StringJoiner("&");
        for (var i = 0; i < length; i += 2) {
            if (entries[i + 1] == null) {
                continue;
            }
            result.add(entries[i] + "=" + entries[i + 1]);
        }

        return result.toString();
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
