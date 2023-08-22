package it.auties.whatsapp.util;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.AsyncCaptchaCodeSupplier;
import it.auties.whatsapp.api.AsyncVerificationCodeSupplier;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.exception.RegistrationException;
import it.auties.whatsapp.model.exchange.Attributes;
import it.auties.whatsapp.model.mobile.VerificationCodeError;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.util.Spec.Whatsapp;
import lombok.experimental.UtilityClass;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@UtilityClass
public class RegistrationHelper {
    public CompletableFuture<Void> registerPhoneNumber(Store store, Keys keys, AsyncVerificationCodeSupplier codeHandler, AsyncCaptchaCodeSupplier captchaHandler, VerificationCodeMethod method) {
        if (method == VerificationCodeMethod.NONE) {
            return sendVerificationCode(store, keys, codeHandler, captchaHandler);
        }

        return requestVerificationCode(store, keys, method, false)
                .thenComposeAsync(ignored -> sendVerificationCode(store, keys, codeHandler, captchaHandler));
    }

    public CompletableFuture<Void> requestVerificationCode(Store store, Keys keys, VerificationCodeMethod method) {
        return requestVerificationCode(store, keys, method, false);
    }

    private CompletableFuture<Void> requestVerificationCode(Store store, Keys keys, VerificationCodeMethod method, boolean badToken) {
        if(method == VerificationCodeMethod.NONE){
            return CompletableFuture.completedFuture(null);
        }

        return requestVerificationCodeOptions(store, keys, method, badToken)
                .thenComposeAsync(attrs -> sendRegistrationRequest(store,"/code", attrs))
                .thenComposeAsync(result -> checkRequestResponse(store, keys, method, result))
                .thenRunAsync(() -> saveRegistrationStatus(store, keys, false));
    }

    private CompletableFuture<Void> checkRequestResponse(Store store, Keys keys, VerificationCodeMethod method, HttpResponse<String> result) {
        Validate.isTrue(result.statusCode() == HttpURLConnection.HTTP_OK,
                "Invalid status code: %s", RegistrationException.class, result.statusCode(), result.body());
        var response = Json.readValue(result.body(), VerificationCodeResponse.class);
        if (response.status().isSuccessful()) {
            return CompletableFuture.completedFuture(null);
        }

        if(response.errorReason() == VerificationCodeError.BAD_TOKEN || response.errorReason() == VerificationCodeError.OLD_VERSION) {
            return requestVerificationCode(store, keys, method, true);
        }

        throw new RegistrationException(response, result.body());
    }

    private CompletableFuture<Map<String, Object>> requestVerificationCodeOptions(Store store, Keys keys, VerificationCodeMethod method, boolean badToken) {
        return getRegistrationOptions(store, keys, badToken,
                Map.entry("mcc", store.phoneNumber().orElseThrow().countryCode().mcc()),
                Map.entry("mnc", store.phoneNumber().orElseThrow().countryCode().mnc()),
                Map.entry("sim_mcc", "000"),
                Map.entry("sim_mnc", "000"),
                Map.entry("method", method.type()),
                Map.entry("reason", ""),
                Map.entry("hasav", "1"));
    }

    public CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, AsyncVerificationCodeSupplier handler, AsyncCaptchaCodeSupplier captchaHandler) {
        return handler.get()
                .thenComposeAsync(result -> sendVerificationCode(store, keys, result, captchaHandler, false))
                .thenRunAsync(() -> saveRegistrationStatus(store, keys, true));
    }

    private void saveRegistrationStatus(Store store, Keys keys, boolean registered) {
        keys.registered(registered);
        if(registered){
            store.jid(store.phoneNumber().orElseThrow().toJid());
            store.addLinkedDevice(store.jid(), 0);
        }
        keys.serialize(true);
        store.serialize(true);
    }

    private CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, String code, AsyncCaptchaCodeSupplier captchaHandler, boolean badToken) {
        return getRegistrationOptions(store, keys, badToken, Map.entry("code", normalizeCodeResult(code)))
                .thenComposeAsync(attrs -> sendRegistrationRequest(store, "/register", attrs))
                .thenComposeAsync(result -> checkVerificationResponse(store, keys, code, result, captchaHandler));
    }

    private CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, String code, String captcha) {
        return getRegistrationOptions(store, keys, false, Map.entry("code", normalizeCodeResult(code)), Map.entry("fraud_checkpoint_code", normalizeCodeResult(captcha)))
                .thenComposeAsync(attrs -> sendRegistrationRequest(store, "/register", attrs))
                .thenComposeAsync(result -> checkVerificationResponse(store, keys, code, result, null));
    }

    private CompletableFuture<Void> checkVerificationResponse(Store store, Keys keys, String code, HttpResponse<String> result, AsyncCaptchaCodeSupplier captchaHandler) {
        System.out.println(result.body());
        Validate.isTrue(result.statusCode() == HttpURLConnection.HTTP_OK,
                "Invalid status code: %s", RegistrationException.class, result.statusCode(), result.body());
        var response = Json.readValue(result.body(), VerificationCodeResponse.class);
        if(response.errorReason() == VerificationCodeError.BAD_TOKEN || response.errorReason() == VerificationCodeError.OLD_VERSION) {
            return sendVerificationCode(store, keys, code, captchaHandler, true);
        }

        if(response.errorReason() == VerificationCodeError.CAPTCHA) {
            Objects.requireNonNull(captchaHandler, "Received captcha error, but no handler was specified in the options");
            return captchaHandler.apply(response)
                    .thenComposeAsync(captcha -> sendVerificationCode(store, keys, code, captcha));
        }

        if (!response.status().isSuccessful()) {
            throw new RegistrationException(response, result.body());
        }

        return CompletableFuture.completedFuture(null);
    }

    private String normalizeCodeResult(String captcha) {
        return captcha.replaceAll("-", "").trim();
    }

    private CompletableFuture<HttpResponse<String>> sendRegistrationRequest(Store store, String path, Map<String, Object> params) {
        var client = createClient(store);
        var encodedParams = toFormParams(params);
        var keypair = SignalKeyPair.random();
        var key = Curve25519.sharedKey(Whatsapp.REGISTRATION_PUBLIC_KEY, keypair.privateKey());
        var buffer = AesGcm.encrypt(new byte[12], encodedParams.getBytes(StandardCharsets.UTF_8), key);
        var enc = Base64.getUrlEncoder().encodeToString(BytesHelper.concat(keypair.publicKey(), buffer));
        var request = HttpRequest.newBuilder()
                .uri(URI.create("%s%s?ENC=%s".formatted(Whatsapp.MOBILE_REGISTRATION_ENDPOINT, path, enc)))
                .GET()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", getUserAgent(store))
                .header("WaMsysRequest", "1")
                .header("request_token", UUID.randomUUID().toString())
                .build();
        return client.sendAsync(request, BodyHandlers.ofString());
    }

    private String getUserAgent(Store store) {
        var osName = getMobileOsName(store);
        var osVersion = store.device().osVersion();
        var manufacturer = store.device().manufacturer();
        var model = store.device().model().replaceAll(" ", "_");
        return "WhatsApp/%s %s/%s Device/%s-%s".formatted(store.version(), osName, osVersion, manufacturer, model);
    }

    private String getMobileOsName(Store store) {
        return switch (store.device().osType()) {
            case ANDROID -> store.business() ? "SMBA" : "Android";
            case IOS -> store.business() ? "SMBI" : "iOS";
            default -> throw new IllegalStateException("Unsupported mobile os: " + store.device().osType());
        };
    }

    private HttpClient createClient(Store store) {
        var clientBuilder = HttpClient.newBuilder();
        store.proxy().ifPresent(proxy -> {
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())));
            clientBuilder.authenticator(new ProxyAuthenticator());
        });
        return clientBuilder.build();
    }

    @SafeVarargs
    private CompletableFuture<Map<String, Object>> getRegistrationOptions(Store store, Keys keys, boolean isRetry, Entry<String, Object>... attributes) {
        return MetadataHelper.getToken(store.phoneNumber().orElseThrow().numberWithoutPrefix(), store.device().osType(), store.business(), !isRetry)
                .thenApplyAsync(token -> getRegistrationOptions(store, keys, token, attributes));
    }

    // TODO: Add backup token, locale and language and expid
    private Map<String, Object> getRegistrationOptions(Store store, Keys keys, String token, Entry<String, Object>[] attributes) {
        return Attributes.of(attributes)
                .put("cc", store.phoneNumber().orElseThrow().countryCode().prefix())
                .put("in", store.phoneNumber().orElseThrow().numberWithoutPrefix())
                .put("rc", store.releaseChannel().index())
                .put("lg", "en")
                .put("lc", "GB") // Locale
                .put("mistyped", "6")
                .put("authkey", Base64.getUrlEncoder().encodeToString(keys.noiseKeyPair().publicKey()))
                .put("e_regid", Base64.getUrlEncoder().encodeToString(keys.encodedRegistrationId()))
                .put("e_keytype", "BQ")
                .put("e_ident", Base64.getUrlEncoder().encodeToString(keys.identityKeyPair().publicKey()))
                .put("e_skey_id", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().encodedId()))
                .put("e_skey_val", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().publicKey()))
                .put("e_skey_sig", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().signature()))
                .put("fdid", keys.phoneId())
                .put("network_ratio_type", "1")
                .put("simnum", "1")
                .put("hasinrc", "1")
                .put("expid", keys.deviceId())
                .put("pid", ProcessHandle.current().pid())
                .put("id", keys.recoveryToken())
                .put("token", token)
                .toMap();
    }

    private String toFormParams(Map<String, Object> values) {
        return values.entrySet()
                .stream()
                .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("&"));
    }
}
