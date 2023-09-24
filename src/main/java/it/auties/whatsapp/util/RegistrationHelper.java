package it.auties.whatsapp.util;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.AsyncCaptchaCodeSupplier;
import it.auties.whatsapp.api.AsyncVerificationCodeSupplier;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.exception.RegistrationException;
import it.auties.whatsapp.model.mobile.VerificationCodeError;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import it.auties.whatsapp.model.mobile.VerificationCodeStatus;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.util.Spec.Whatsapp;

import java.io.UncheckedIOException;
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

public final class RegistrationHelper {
    public static CompletableFuture<Void> registerPhoneNumber(Store store, Keys keys, AsyncVerificationCodeSupplier codeHandler, AsyncCaptchaCodeSupplier captchaHandler, VerificationCodeMethod method) {
        if (method == VerificationCodeMethod.NONE) {
            return sendVerificationCode(store, keys, codeHandler, captchaHandler);
        }

        return requestVerificationCode(store, keys, method)
                .thenComposeAsync(ignored -> sendVerificationCode(store, keys, codeHandler, captchaHandler));
    }

    public static CompletableFuture<Void> requestVerificationCode(Store store, Keys keys, VerificationCodeMethod method) {
        return requestVerificationCode(store, keys, method, null);
    }

    private static CompletableFuture<Void> requestVerificationCode(Store store, Keys keys, VerificationCodeMethod method, VerificationCodeError lastError) {
        if (method == VerificationCodeMethod.NONE) {
            return CompletableFuture.completedFuture(null);
        }

        return requestVerificationCodeOptions(store, keys, method, lastError)
                .thenComposeAsync(attrs -> sendRegistrationRequest(store, "/code", attrs))
                .thenComposeAsync(result -> checkRequestResponse(store, keys, result.statusCode(), result.body(), lastError, method))
                .thenRunAsync(() -> saveRegistrationStatus(store, keys, false));
    }

    private static CompletableFuture<Void> checkRequestResponse(Store store, Keys keys, int statusCode, String body, VerificationCodeError lastError, VerificationCodeMethod method) {
        try {
            if(statusCode != HttpURLConnection.HTTP_OK) {
                throw new RegistrationException(null, body);
            }

            var response = Json.readValue(body, VerificationCodeResponse.class);
            if (response.status() == VerificationCodeStatus.SUCCESS) {
                return CompletableFuture.completedFuture(null);
            }

            if(response.errorReason() == VerificationCodeError.NO_ROUTES) {
                throw new RegistrationException(response, "VOIPs are not supported by Whatsapp");
            }

            var newErrorReason = response.errorReason();
            if (newErrorReason != lastError) {
                return requestVerificationCode(store, keys, method, newErrorReason);
            }

            throw new RegistrationException(response, body);
        }catch (UncheckedIOException exception) {
            throw new RegistrationException(null, body);
        }
    }

    private static CompletableFuture<Map<String, Object>> requestVerificationCodeOptions(Store store, Keys keys, VerificationCodeMethod method, VerificationCodeError lastError) {
        var countryCode = store.phoneNumber()
                .orElseThrow()
                .countryCode();
        return getRegistrationOptions(store,
                keys,
                lastError == VerificationCodeError.OLD_VERSION || lastError == VerificationCodeError.BAD_TOKEN,
                Map.entry("mcc", countryCode.mcc()),
                Map.entry("mnc", countryCode.mnc()),
                Map.entry("sim_mcc", countryCode.mcc()),
                Map.entry("sim_mnc", countryCode.mnc()),
                Map.entry("method", method.type()),
                Map.entry("reason", lastError != null ? lastError.data() : "")
        );
    }

    public static CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, AsyncVerificationCodeSupplier handler, AsyncCaptchaCodeSupplier captchaHandler) {
        return handler.get()
                .thenComposeAsync(result -> sendVerificationCode(store, keys, result, captchaHandler, false))
                .thenRunAsync(() -> saveRegistrationStatus(store, keys, true));
    }

    private static void saveRegistrationStatus(Store store, Keys keys, boolean registered) {
        keys.setRegistered(registered);
        if (registered) {
            var jid = store.phoneNumber().orElseThrow().toJid();
            store.setJid(jid);
            store.addLinkedDevice(jid, 0);
        }
        keys.serialize(true);
        store.serialize(true);
    }

    private static CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, String code, AsyncCaptchaCodeSupplier captchaHandler, boolean badToken) {
        return getRegistrationOptions(store, keys, badToken, Map.entry("code", normalizeCodeResult(code)))
                .thenComposeAsync(attrs -> sendRegistrationRequest(store, "/register", attrs))
                .thenComposeAsync(result -> checkVerificationResponse(store, keys, code, result, captchaHandler));
    }

    private static CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, String code, String captcha) {
        return getRegistrationOptions(store, keys, false, Map.entry("code", normalizeCodeResult(code)), Map.entry("fraud_checkpoint_code", normalizeCodeResult(captcha)))
                .thenComposeAsync(attrs -> sendRegistrationRequest(store, "/register", attrs))
                .thenComposeAsync(result -> checkVerificationResponse(store, keys, code, result, null));
    }

    private static CompletableFuture<Void> checkVerificationResponse(Store store, Keys keys, String code, HttpResponse<String> result, AsyncCaptchaCodeSupplier captchaHandler) {
       try {
           if(result.statusCode() != HttpURLConnection.HTTP_OK) {
               throw new RegistrationException(null, result.body());
           }

           var response = Json.readValue(result.body(), VerificationCodeResponse.class);
           if (response.errorReason() == VerificationCodeError.BAD_TOKEN || response.errorReason() == VerificationCodeError.OLD_VERSION) {
               return sendVerificationCode(store, keys, code, captchaHandler, true);
           }

           if (response.errorReason() == VerificationCodeError.CAPTCHA) {
               Objects.requireNonNull(captchaHandler, "Received captcha, but no handler was specified in the options");
               return captchaHandler.apply(response)
                       .thenComposeAsync(captcha -> sendVerificationCode(store, keys, code, captcha));
           }

           if (response.status() == VerificationCodeStatus.SUCCESS) {
               return CompletableFuture.completedFuture(null);
           }

           throw new RegistrationException(response, result.body());
       }catch (UncheckedIOException exception) {
           throw new RegistrationException(null, result.body());
       }
    }

    private static String normalizeCodeResult(String captcha) {
        return captcha.replaceAll("-", "").trim();
    }

    private static CompletableFuture<HttpResponse<String>> sendRegistrationRequest(Store store, String path, Map<String, Object> params) {
        try(var client = createClient(store)) {
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
                    .header("request_token", UUID.randomUUID().toString())
                    .build();
            return client.sendAsync(request, BodyHandlers.ofString());
        }
    }

    private static String getUserAgent(Store store) {
        var osName = getMobileOsName(store);
        var device = store.device().orElseThrow();
        var osVersion = device.osVersion();
        var manufacturer = device.manufacturer();
        var model = device.model().replaceAll(" ", "_");
        return "WhatsApp/%s %s/%s Device/%s-%s".formatted(store.version(), osName, osVersion, manufacturer, model);
    }

    private static String getMobileOsName(Store store) {
        var device = store.device().orElseThrow();
        return switch (device.platform()) {
            case ANDROID -> "Android";
            case SMB_ANDROID -> "SMBA";
            case IOS -> "iOS";
            case SMB_IOS -> "SMBI";
            default -> throw new IllegalStateException("Unsupported mobile os: " + device.platform());
        };
    }

    private static HttpClient createClient(Store store) {
        var clientBuilder = HttpClient.newBuilder();
        store.proxy().ifPresent(proxy -> {
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())));
            clientBuilder.authenticator(new ProxyAuthenticator());
        });
        return clientBuilder.build();
    }

    @SafeVarargs
    private static CompletableFuture<Map<String, Object>> getRegistrationOptions(Store store, Keys keys, boolean isRetry, Entry<String, Object>... attributes) {
        var device = store.device().orElseThrow();
        return MetadataHelper.getToken(store.phoneNumber().orElseThrow().numberWithoutPrefix(), store.business() ? device.businessPlatform() : device.platform(), !isRetry)
                .thenApplyAsync(token -> getRegistrationOptions(store, keys, token, attributes));
    }

    // TODO: Add backup token, locale and language and expid
    private static Map<String, Object> getRegistrationOptions(Store store, Keys keys, String token, Entry<String, Object>[] attributes) {
        return Attributes.of(attributes)
                .put("cc", store.phoneNumber().orElseThrow().countryCode().prefix())
                .put("in", store.phoneNumber().orElseThrow().numberWithoutPrefix())
                .put("rc", store.releaseChannel().index())
                .put("authkey", Base64.getUrlEncoder().encodeToString(keys.noiseKeyPair().publicKey()))
                .put("e_regid", Base64.getUrlEncoder().encodeToString(keys.encodedRegistrationId()))
                .put("e_keytype", "BQ")
                .put("e_ident", Base64.getUrlEncoder().encodeToString(keys.identityKeyPair().publicKey()))
                .put("e_skey_id", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().encodedId()))
                .put("e_skey_val", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().publicKey()))
                .put("e_skey_sig", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().signature()))
                .put("id", keys.recoveryToken())
                .put("token", token)
                .toMap();
    }

    private static String toFormParams(Map<String, Object> values) {
        return values.entrySet()
                .stream()
                .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("&"));
    }
}
