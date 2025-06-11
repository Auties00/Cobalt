package it.auties.whatsapp.util;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.AsyncVerificationCodeSupplier;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.exception.RegistrationException;
import it.auties.whatsapp.model.mobile.VerificationCodeError;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeStatus;
import it.auties.whatsapp.model.response.RegistrationResponse;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class MobileRegistration {
    public static final String MOBILE_REGISTRATION_ENDPOINT = "https://v.whatsapp.net/v2";
    private static final byte[] REGISTRATION_PUBLIC_KEY = HexFormat.of().parseHex("8e8c0f74c3ebc5d7a6865c6c3c843856b06121cce8ea774d22fb6f122512302d");

    private final HttpClient httpClient;
    private final Store store;
    private final Keys keys;
    private final AsyncVerificationCodeSupplier codeHandler;
    private final VerificationCodeMethod method;

    public MobileRegistration(Store store, Keys keys, AsyncVerificationCodeSupplier codeHandler, VerificationCodeMethod method) {
        this.store = store;
        this.keys = keys;
        this.codeHandler = codeHandler;
        this.method = method;
        this.httpClient = HttpClient.newHttpClient();
    }

    public CompletableFuture<RegistrationResponse> registerPhoneNumber() {
        return requestVerificationCode(false)
                .thenCompose(ignored -> sendVerificationCode())
                .thenApply(result -> {
                    dispose();
                    return result;
                })
                .exceptionallyCompose(throwable -> {
                    dispose();
                    return CompletableFuture.failedFuture(throwable);
                });
    }

    public CompletableFuture<RegistrationResponse> requestVerificationCode() {
        return requestVerificationCode(true);
    }

    private CompletableFuture<RegistrationResponse> requestVerificationCode(boolean closeResources) {
        if(method == VerificationCodeMethod.NONE) {
            return CompletableFuture.completedFuture(null);
        }

        return exists(null)
                .thenComposeAsync(response -> requestVerificationCode(response, null))
                .thenApply(result -> {
                    if(closeResources) {
                        dispose();
                    }
                    return result;
                })
                .exceptionallyCompose(throwable -> {
                    if(closeResources) {
                        dispose();
                    }
                    return CompletableFuture.failedFuture(throwable);
                });
    }

    private CompletableFuture<RegistrationResponse> exists(VerificationCodeError lastError) {
        var options = getRegistrationOptions(
                store,
                keys,
                false
        );
        return options.thenComposeAsync(attrs -> sendRequest("/exist", attrs)).thenComposeAsync(result -> {
            var response = RegistrationResponse.ofJson(result)
                    .orElseThrow(() -> new IllegalStateException("Cannot parse response: " + result));
            if (response.errorReason() == VerificationCodeError.INCORRECT) {
                return CompletableFuture.completedFuture(response);
            }

            if (lastError == null) {
                return exists(response.errorReason());
            }

            throw new RegistrationException(response, result);
        });
    }

    private CompletableFuture<RegistrationResponse> requestVerificationCode(RegistrationResponse existsResponse, VerificationCodeError lastError) {
        var options = getRegistrationOptions(
                store,
                keys,
                true,
                getRequestVerificationCodeParameters(existsResponse)
        );
        return options.thenComposeAsync(attrs -> sendRequest("/code", attrs))
                .thenComposeAsync(result -> onCodeRequestSent(existsResponse, lastError, result))
                .thenApplyAsync(response -> {
                    saveRegistrationStatus(store, keys, false);
                    return response;
                });
    }

    private String[] getRequestVerificationCodeParameters(RegistrationResponse existsResponse) {
        var countryCode = store.phoneNumber()
                .orElseThrow()
                .countryCode();
        return switch(store.device().platform()) {
            case IOS, IOS_BUSINESS -> new String[]{
                    "method", method.data(),
                    "sim_mcc", existsResponse.flashType() ? countryCode.mcc() : "000",
                    "sim_mnc", "000",
                    "reason", "",
                    "cellular_strength", "1"
            };
            case ANDROID, ANDROID_BUSINESS -> new String[]{
                    "method", method.data(),
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
                    "_gp","",
                    "backup_token", toUrlHex(keys.backupToken()),
                    "hasav", "2"
            };
            default -> throw new IllegalStateException(store.device().platform() + " doesn't support registration");
        };
    }

    private CompletionStage<RegistrationResponse> onCodeRequestSent(RegistrationResponse existsResponse, VerificationCodeError lastError, String result) {
        var response = RegistrationResponse.ofJson(result)
                .orElseThrow(() -> new IllegalStateException("Cannot parse response: " + result));
        if (response.status() == VerificationCodeStatus.SUCCESS) {
            return CompletableFuture.completedFuture(response);
        }

        return switch (response.errorReason()) {
            case TOO_RECENT, TOO_MANY, TOO_MANY_GUESSES, TOO_MANY_ALL_METHODS -> throw new RegistrationException(response, "Please wait before trying to register this phone number again");
            case NO_ROUTES -> throw new RegistrationException(response, "You can only register numbers that are already on Whatsapp (for further support contact me on telegram @Auties00)");
            default -> {
                var newErrorReason = response.errorReason();
                if(newErrorReason == lastError) {
                    throw new RegistrationException(response, result);
                }
                yield requestVerificationCode(existsResponse, newErrorReason);
            }
        };
    }

    public CompletableFuture<RegistrationResponse> sendVerificationCode() {
        return codeHandler.get()
                .thenComposeAsync(code -> getRegistrationOptions(
                        store, keys, true,
                        "code", normalizeCodeResult(code)
                ))
                .thenComposeAsync(attrs -> sendRequest("/register", attrs))
                .thenComposeAsync(result -> {
                    var response = RegistrationResponse.ofJson(result)
                            .orElseThrow(() -> new IllegalStateException("Cannot parse response: " + result));
                    if (response.status() == VerificationCodeStatus.SUCCESS) {
                        saveRegistrationStatus(store, keys, true);
                        return CompletableFuture.completedFuture(response);
                    }

                    throw new RegistrationException(response, result);
                });
    }

    private void saveRegistrationStatus(Store store, Keys keys, boolean registered) {
        keys.setRegistered(registered);
        if (registered) {
            var jid = store.phoneNumber().orElseThrow().toJid();
            store.setJid(jid);
            store.addLinkedDevice(jid, 0);
        }
        keys.serialize(true);
        store.serialize(true);
    }

    private String normalizeCodeResult(String captcha) {
        return captcha.replaceAll("-", "").trim();
    }

    private CompletableFuture<String> sendRequest(String path, String params) {
        var keypair = SignalKeyPair.random();
        var key = Curve25519.sharedKey(REGISTRATION_PUBLIC_KEY, keypair.privateKey());
        var buffer = AesGcm.encrypt(new byte[12], params.getBytes(StandardCharsets.UTF_8), key);
        var encoder = Base64.getUrlEncoder();
        var cipheredParameters = encoder.encodeToString(keypair.publicKey()) + encoder.encodeToString(buffer);
        var userAgent = store.device()
                .toUserAgent(store.version())
                .orElseThrow(() -> new NoSuchElementException("Missing user agent for registration"));
        var request = HttpRequest.newBuilder()
                .uri(URI.create("%s%s?ENC=%s".formatted(MOBILE_REGISTRATION_ENDPOINT, path, cipheredParameters)))
                .GET()
                .header("User-Agent", userAgent);
        if(store.device().platform().isAndroid()) {
            request.header("Accept", "text/json")
                    .header("WaMsysRequest", "1")
                    .header("request_token", UUID.randomUUID().toString())
                    .header("Content-Type", "application/x-www-form-urlencoded");
        }
        return httpClient.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }

    private CompletableFuture<String> getRegistrationOptions(Store store, Keys keys, boolean useToken, String... attributes) {
        var phoneNumber = store.phoneNumber()
                .orElseThrow(() -> new NoSuchElementException("Missing phone number"));
        var tokenFuture = useToken ? AppMetadata.getToken(phoneNumber.numberWithoutPrefix(), store.device().platform(), store.version()) : CompletableFuture.<String>completedFuture(null);
        return tokenFuture.thenApplyAsync(token -> {
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
            if(additionalParams.isEmpty()) {
                return registrationParams;
            }else {
                return registrationParams + additionalParams;
            }
        });
    }

    private String toUrlHex(byte[] buffer) {
        var id = new StringBuilder();
        for (byte x : buffer) {
            id.append(String.format("%%%02x", x));
        }
        return id.toString().toUpperCase(Locale.ROOT);
    }

    // For every pair of entries we need a = to separate them and a & to join the pair to the next one
    // Then we need to factor in the length of the entry itself (either the key or value)
    // Exclude the last &
    // Then write the result
    private String toFormParams(String... entries) {
        var length = entries.length;
        if((length & 1) != 0) {
            throw new IllegalArgumentException("Odd form entries");
        }
        
        var resultLength = entries.length - 1;
        for(var entry : entries) {
            resultLength += entry.length();
        }
        
        var result = new StringBuilder(resultLength);
        var i = 0;
        while (i < entries.length) {
            result.append(entries[i++])
                    .append('=')
                    .append(entries[i++]);
            if(result.length() < result.capacity()) {
                result.append('&');
            }
        }
        return result.toString();
    }

    private void dispose() {
        httpClient.close();
    }
}