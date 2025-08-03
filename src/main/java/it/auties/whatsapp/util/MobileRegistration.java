package it.auties.whatsapp.util;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.WhatsappVerificationHandler;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.exception.RegistrationException;
import it.auties.whatsapp.model.response.RegistrationResponse;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

public final class MobileRegistration {
    public static final String MOBILE_REGISTRATION_ENDPOINT = "https://v.whatsapp.net/v2";
    private static final byte[] REGISTRATION_PUBLIC_KEY = HexFormat.of().parseHex("8e8c0f74c3ebc5d7a6865c6c3c843856b06121cce8ea774d22fb6f122512302d");

    private final HttpClient httpClient;
    private final Store store;
    private final Keys keys;
    private final WhatsappVerificationHandler.Mobile verification;

    public MobileRegistration(Store store, Keys keys, WhatsappVerificationHandler.Mobile verification) {
        this.store = store;
        this.keys = keys;
        this.verification = verification;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void registerPhoneNumber() {
        try {
            requestVerificationCode(false);
            sendVerificationCode();
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException("Cannot register phone number", exception);
        } finally {
            dispose();
        }
    }

    public Optional<RegistrationResponse> requestVerificationCode() {
        try {
            return requestVerificationCode(true);
        }catch (IOException | InterruptedException exception){
            throw new RuntimeException("Cannot request verification code", exception);
        }
    }

    private Optional<RegistrationResponse> requestVerificationCode(boolean closeResources) throws IOException, InterruptedException {
        if (verification.requestMethod().isEmpty()) {
            return Optional.empty();
        }

        try {
            var response = exists(null);
            var result = requestVerificationCode(response, null);
            saveRegistrationStatus(store, keys, false);
            return Optional.of(result);
        } finally {
            if (closeResources) {
                dispose();
            }
        }
    }

    private RegistrationResponse exists(String lastError) throws IOException, InterruptedException {
        var attrs = getRegistrationOptions(store, keys, false);
        var result = sendRequest("/exist", attrs);
        var response = RegistrationResponse.ofJson(result)
                .orElseThrow(() -> new IllegalStateException("Cannot parse response: " + result));

        if (Objects.equals(response.errorReason(), "incorrect")) {
            return response;
        }

        if (lastError == null) {
            return exists(response.errorReason());
        }

        throw new RegistrationException(response, result);
    }

    private RegistrationResponse requestVerificationCode(RegistrationResponse existsResponse, String lastError) throws IOException, InterruptedException {
        var params = getRequestVerificationCodeParameters(existsResponse);
        var attrs = getRegistrationOptions(
                store,
                keys,
                true,
                params
        );
        var result = sendRequest("/code", attrs);
        return onCodeRequestSent(existsResponse, lastError, result);
    }

    private String[] getRequestVerificationCodeParameters(RegistrationResponse existsResponse) {
        var method = verification.requestMethod()
                .orElseThrow();
        var countryCode = store.phoneNumber()
                .orElseThrow()
                .countryCode();
        return switch (store.device().platform()) {
            case IOS, IOS_BUSINESS -> new String[]{
                    "method", method,
                    "sim_mcc", existsResponse.flashType() ? countryCode.mcc() : "000",
                    "sim_mnc", "000",
                    "reason", "",
                    "cellular_strength", "1"
            };
            case ANDROID, ANDROID_BUSINESS -> new String[]{
                    "method", method,
                    "sim_mcc", "000",
                    "sim_mnc", "000",
                    "reason", "",
                    "mcc", "000",
                    "mnc", "000",
                    "feo2_query_status", "error_security_exception",
                    "db", "1",
                    "sim_type", "0",
                    "recaptcha", "%7B%22stage%22%3A%22ABPROP_DISABLED%22%7D",
                    "network_radio_type", "1",
                    "prefer_sms_over_flash", "false",
                    "simnum", "0",
                    "airplane_mode_type", "0",
                    "client_metrics", "%7B%22attempts%22%3A20%2C%22app_campaign_download_source%22%3A%22google-play%7Cunknown%22%7D",
                    "mistyped", "7",
                    "advertising_id", keys.advertisingId().toString(),
                    "hasinrc", "1",
                    "roaming_type", "0",
                    "device_ram", "3.57",
                    "education_screen_displayed", "false",
                    "pid", String.valueOf(ProcessHandle.current().pid()),
                    "gpia", "",
                    "cellular_strength", "5",
                    "_gg", "",
                    "_gi", "",
                    "_gp", "",
                    "backup_token", toUrlHex(keys.backupToken()),
                    "hasav", "2"
            };
            default -> throw new IllegalStateException(store.device().platform() + " doesn't support registration");
        };
    }

    private RegistrationResponse onCodeRequestSent(RegistrationResponse existsResponse, String lastError, String result) throws IOException, InterruptedException {
        var response = RegistrationResponse.ofJson(result)
                .orElseThrow(() -> new IllegalStateException("Cannot parse response: " + result));
        if (isSuccessful(response.status())) {
            return response;
        }

        return switch (response.errorReason()) {
            case "too_recent", "too_many", "too_many_guesses", "too_many_all_methods" ->
                    throw new RegistrationException(response, "Please wait before trying to register this phone number again");
            case "no_routes" ->
                    throw new RegistrationException(response, "You can only register numbers that are already on Whatsapp");
            default -> {
                var newErrorReason = response.errorReason();
                if (newErrorReason.equals(lastError)) {
                    throw new RegistrationException(response, result);
                }
                yield requestVerificationCode(existsResponse, newErrorReason);
            }
        };
    }

    public void sendVerificationCode() throws IOException, InterruptedException {
        var code = verification.verificationCode();
        var attrs = getRegistrationOptions(store, keys, true, "code", normalizeCodeResult(code));
        var result = sendRequest("/register", attrs);
        var response = RegistrationResponse.ofJson(result)
                .orElseThrow(() -> new IllegalStateException("Cannot parse response: " + result));
        if (!isSuccessful(response.status())) {
            throw new RegistrationException(response, result);
        }
        saveRegistrationStatus(store, keys, true);
    }

    private void saveRegistrationStatus(Store store, Keys keys, boolean registered) {
        keys.setRegistered(registered);
        if (registered) {
            var jid = store.phoneNumber().orElseThrow().toJid();
            store.setJid(jid);
            store.addLinkedDevice(jid, 0);
        }
        keys.serialize();
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

    private String sendRequest(String path, String params) throws IOException, InterruptedException {
        try {
            var keypair = SignalKeyPair.random();
            var key = Curve25519.sharedKey(REGISTRATION_PUBLIC_KEY, keypair.privateKey());
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, new byte[12])
            );
            var result = cipher.doFinal(params.getBytes(StandardCharsets.UTF_8));
            var encoder = Base64.getUrlEncoder();
            var cipheredParameters = encoder.encodeToString(keypair.publicKey()) + encoder.encodeToString(result);
            var userAgent = store.device()
                    .toUserAgent(store.version())
                    .orElseThrow(() -> new NoSuchElementException("Missing user agent for registration"));
            var requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("%s%s?ENC=%s".formatted(MOBILE_REGISTRATION_ENDPOINT, path, cipheredParameters)))
                    .GET()
                    .header("User-Agent", userAgent);
            if (store.device().platform().isAndroid()) {
                requestBuilder.header("Accept", "text/json")
                        .header("WaMsysRequest", "1")
                        .header("request_token", UUID.randomUUID().toString())
                        .header("Content-Type", "application/x-www-form-urlencoded");
            }

            var httpResponse = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return httpResponse.body();
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot encrypt request", exception);
        }
    }

    private String getRegistrationOptions(Store store, Keys keys, boolean useToken, String... attributes) {
        var phoneNumber = store.phoneNumber()
                .orElseThrow(() -> new NoSuchElementException("Missing phone number"));
        var token = useToken ? AppMetadata.getToken(phoneNumber.numberWithoutPrefix(), store.device().platform(), store.version()) : null;
        var certificate = store.device().platform().isBusiness() ? AppMetadata.generateBusinessCertificate(keys) : null;
        var fdid = switch (store.device().platform()) {
            case IOS, IOS_BUSINESS -> keys.fdid().toUpperCase(Locale.ROOT);
            case ANDROID, ANDROID_BUSINESS -> keys.fdid().toLowerCase(Locale.ROOT);
            default -> null;
        };
        var registrationParams = toFormParams(
                "cc", phoneNumber.countryCode().prefix(),
                "in", String.valueOf(phoneNumber.numberWithoutPrefix()),
                "rc", String.valueOf(store.releaseChannel().index()),
                "lg", phoneNumber.countryCode().lg(),
                "lc", phoneNumber.countryCode().lc(),
                "authkey", Base64.getUrlEncoder().encodeToString(keys.noiseKeyPair().publicKey()),
                "vname", certificate,
                "e_regid", Base64.getUrlEncoder().encodeToString(keys.encodedRegistrationId()),
                "e_keytype", Base64.getUrlEncoder().encodeToString(SignalConstants.KEY_BUNDLE_TYPE),
                "e_ident", Base64.getUrlEncoder().encodeToString(keys.identityKeyPair().publicKey()),
                "e_skey_id", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().encodedId()),
                "e_skey_val", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().publicKey()),
                "e_skey_sig", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().signature()),
                "fdid", fdid,
                "expid", Base64.getUrlEncoder().encodeToString(keys.deviceId()),
                "id", toUrlHex(keys.identityId()),
                "token", useToken ? token : null
        );
        var additionalParams = toFormParams(attributes);
        if (additionalParams.isEmpty()) {
            return registrationParams;
        } else {
            return registrationParams + additionalParams;
        }
    }

    private String toUrlHex(byte[] buffer) {
        var id = new StringBuilder();
        for (byte x : buffer) {
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
            throw new IllegalArgumentException("Odd form entries");
        }

        var result = new StringJoiner("&");
        for (int i = 0; i < length; i += 2) {
            if (entries[i + 1] == null) {
                continue;
            }
            result.add(entries[i] + "=" + entries[i + 1]);
        }

        return result.toString();
    }

    private void dispose() {
        httpClient.close();
    }
}
