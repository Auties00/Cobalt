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
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.response.AbPropsResponse;
import it.auties.whatsapp.model.response.RegistrationResponse;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

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

        return switch (store.device().platform()) {
            case IOS, IOS_BUSINESS -> onboard("1", 2155550000L, null)
                    .thenComposeAsync(response -> onboard(null, null, response.abHash()))
                    .thenComposeAsync(ignored -> exists(null))
                    .thenComposeAsync(result -> clientLog(result, Map.entry("current_screen", "verify_sms"), Map.entry("previous_screen", "enter_number"), Map.entry("action_taken", "continue"))
                            .thenComposeAsync(response -> requestVerificationCode(response, null)))
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
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
    }

    private CompletableFuture<AbPropsResponse> onboard(String cc, Long in, String abHash) {
        var phoneNumber = store.phoneNumber()
                .orElseThrow();
        var attributes = Attributes.of()
                .put("cc", Objects.requireNonNullElse(cc, phoneNumber.countryCode().prefix()))
                .put("in", Objects.requireNonNullElse(in, phoneNumber.numberWithoutPrefix()))
                .put("rc", store.releaseChannel().index())
                .put("ab_hash", abHash, abHash != null)
                .toMap();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(MOBILE_REGISTRATION_ENDPOINT + "/reg_onboard_abprop?" + toFormParams(attributes)))
                .GET()
                .header("User-Agent", userAgent())
                .header("Content-Type","application/x-www-form-urlencoded")
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> Json.readValue(response.body(), AbPropsResponse.class));
    }

    private String userAgent() {
        return store.device()
                .toUserAgent(store.version())
                .orElseThrow(() -> new NoSuchElementException("Missing user agent for registration"));
    }

    private CompletableFuture<RegistrationResponse> exists(VerificationCodeError lastError) {
        var ios = store.device().platform().isIOS();
        var options = getRegistrationOptions(
                store,
                keys,
                false,
                ios ? Map.entry("recovery_token_error", "-25300") : null
        );
        return options.thenComposeAsync(attrs -> sendRequest("/exist", attrs)).thenComposeAsync(result -> {
            var response = Json.readValue(result, RegistrationResponse.class);
            if (response.errorReason() == VerificationCodeError.INCORRECT) {
                return CompletableFuture.completedFuture(response);
            }

            if (lastError == null) {
                return exists(response.errorReason());
            }

            throw new RegistrationException(response, result);
        });
    }

    private String convertBufferToUrlHex(byte[] buffer) {
        var id = new StringBuilder();
        for (byte x : buffer) {
            id.append(String.format("%%%02x", x));
        }
        return id.toString().toUpperCase(Locale.ROOT);
    }

    @SafeVarargs
    private <T> CompletableFuture<T> clientLog(T data, Entry<String, Object>... attributes) {
        var options = getRegistrationOptions(
                store,
                keys,
                false,
                attributes
        );
        return options.thenCompose(attrs -> sendRequest("/client_log", attrs))
                .thenApply(result -> data);
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

    private Entry<String, Object>[] getRequestVerificationCodeParameters(RegistrationResponse existsResponse) {
        var countryCode = store.phoneNumber()
                .orElseThrow()
                .countryCode();
        return switch(store.device().platform()) {
            case UNKNOWN -> new Entry[]{};
            case IOS, IOS_BUSINESS -> new Entry[]{
                    Map.entry("method", method.data()),
                    Map.entry("sim_mcc", existsResponse.flashType() ? countryCode.mcc() : "000"),
                    Map.entry("sim_mnc", "000"),
                    Map.entry("reason", ""),
                    Map.entry("cellular_strength", 1)
            };
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
    }

    private CompletionStage<RegistrationResponse> onCodeRequestSent(RegistrationResponse existsResponse, VerificationCodeError lastError, String result) {
        var response = Json.readValue(result, RegistrationResponse.class);
        if (response.status() == VerificationCodeStatus.SUCCESS) {
            return CompletableFuture.completedFuture(response);
        }

        return switch (response.errorReason()) {
            case TOO_RECENT, TOO_MANY, TOO_MANY_GUESSES, TOO_MANY_ALL_METHODS -> throw new RegistrationException(response, "Please wait before trying to register this phone number again");
            case NO_ROUTES -> throw new RegistrationException(response, "You can only register numbers that are already on Whatsapp, if you need to register any numbers please contact me on Telegram @Auties00");
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
                .thenComposeAsync(code -> getRegistrationOptions(store, keys, true, Map.entry("code", normalizeCodeResult(code))))
                .thenComposeAsync(attrs -> sendRequest("/register", attrs))
                .thenComposeAsync(result -> {
                    var response = Json.readValue(result, RegistrationResponse.class);
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

    private CompletableFuture<String> sendRequest(String path, Map<String, Object> params) {
        var encodedParams = toFormParams(params);
        var keypair = SignalKeyPair.random();
        var key = Curve25519.sharedKey(REGISTRATION_PUBLIC_KEY, keypair.privateKey());
        var buffer = AesGcm.encrypt(new byte[12], encodedParams.getBytes(StandardCharsets.UTF_8), key);
        var cipheredParameters = Base64.getUrlEncoder().encodeToString(Bytes.concat(keypair.publicKey(), buffer));
        var request = HttpRequest.newBuilder()
                .uri(URI.create("%s%s?ENC=%s".formatted(MOBILE_REGISTRATION_ENDPOINT, path, cipheredParameters)))
                .GET()
                .header("User-Agent", userAgent());
        if(store.device().platform().isAndroid()) {
            request.header("Accept", "text/json")
                    .header("WaMsysRequest", "1")
                    .header("request_token", UUID.randomUUID().toString())
                    .header("Content-Type", "application/x-www-form-urlencoded");
        }
        return httpClient.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }

    @SafeVarargs
    private CompletableFuture<Map<String, Object>> getRegistrationOptions(Store store, Keys keys, boolean useToken, Entry<String, Object>... attributes) {
        var phoneNumber = store.phoneNumber()
                .orElseThrow(() -> new NoSuchElementException("Missing phone number"));
        var tokenFuture = !useToken ? CompletableFuture.completedFuture(null) : AppMetadata.getToken(phoneNumber.numberWithoutPrefix(), store.device().platform(), store.version());
        return tokenFuture.thenApplyAsync(token -> {
            var certificate = store.device().platform().isBusiness() ? AppMetadata.generateBusinessCertificate(keys) : null;
            var requiredAttributes = Arrays.stream(attributes)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (first, second) -> first, LinkedHashMap::new));
            return Attributes.of()
                    .put("cc", phoneNumber.countryCode().prefix())
                    .put("in", phoneNumber.numberWithoutPrefix())
                    .put("rc", store.releaseChannel().index())
                    .put("lg", phoneNumber.countryCode().lg())
                    .put("lc", phoneNumber.countryCode().lc())
                    .put("authkey", Base64.getUrlEncoder().encodeToString(keys.noiseKeyPair().publicKey()))
                    .put("vname", certificate, certificate != null)
                    .put("e_regid", Base64.getUrlEncoder().encodeToString(keys.encodedRegistrationId()))
                    .put("e_keytype", Base64.getUrlEncoder().encodeToString(SignalConstants.KEY_BUNDLE_TYPE))
                    .put("e_ident", Base64.getUrlEncoder().encodeToString(keys.identityKeyPair().publicKey()))
                    .put("e_skey_id", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().encodedId()))
                    .put("e_skey_val", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().publicKey()))
                    .put("e_skey_sig", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().signature()))
                    .put("fdid", keys.fdid().toLowerCase(Locale.ROOT), store.device().platform().isAndroid())
                    .put("fdid", keys.fdid().toUpperCase(Locale.ROOT), store.device().platform().isIOS())
                    .put("expid", Base64.getUrlEncoder().encodeToString(keys.deviceId()))
                    .put("id", convertBufferToUrlHex(keys.identityId()))
                    .put("token", token, useToken)
                    .putAll(requiredAttributes)
                    .toMap();
        });
    }

    private String toFormParams(Map<String, ?> values) {
        return values.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private void dispose() {
        httpClient.close();
    }
}