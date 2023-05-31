package it.auties.whatsapp.util;

import it.auties.bytes.Bytes;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGmc;
import it.auties.whatsapp.exception.RegistrationException;
import it.auties.whatsapp.model.mobile.RegistrationStatus;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import it.auties.whatsapp.model.request.Attributes;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@UtilityClass
public class RegistrationHelper {
    public CompletableFuture<Void> registerPhoneNumber(Store store, Keys keys, Supplier<CompletableFuture<String>> handler, VerificationCodeMethod method) {
        if (method == VerificationCodeMethod.NONE) {
            return sendVerificationCode(store, keys, handler);
        }

        return requestVerificationCode(store, keys, method)
                .thenComposeAsync(ignored -> sendVerificationCode(store, keys, handler));
    }

    public CompletableFuture<Void> requestVerificationCode(Store store, Keys keys, VerificationCodeMethod method) {
        if(method == VerificationCodeMethod.NONE){
            return CompletableFuture.completedFuture(null);
        }

        return requestVerificationCodeOptions(store, keys, method)
                .thenComposeAsync(attrs -> sendRegistrationRequest(store,"/code", attrs))
                .thenAcceptAsync(RegistrationHelper::checkResponse)
                .thenRunAsync(() -> saveRegistrationStatus(store, keys, false));
    }

    private CompletableFuture<Map<String, Object>> requestVerificationCodeOptions(Store store, Keys keys, VerificationCodeMethod method) {
        return getRegistrationOptions(store, keys,
                Map.entry("mcc", store.phoneNumber().get().countryCode().mcc()),
                Map.entry("mnc", store.phoneNumber().get().countryCode().mnc()),
                Map.entry("sim_mcc", "000"),
                Map.entry("sim_mnc", "000"),
                Map.entry("method", method.type()),
                Map.entry("reason", ""),
                Map.entry("hasav", "1"));
    }

    public CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, Supplier<CompletableFuture<String>> handler) {
        return handler.get()
                .thenComposeAsync(result -> sendVerificationCode(store, keys, result))
                .thenRunAsync(() -> saveRegistrationStatus(store, keys, true));
    }

    private void saveRegistrationStatus(Store store, Keys keys, boolean registered) {
        keys.registrationStatus(RegistrationStatus.REGISTERED);
        if(registered){
            store.jid(store.phoneNumber().get().toJid());
            store.addLinkedDevice(store.jid(), 0);
        }
        keys.serialize(true);
        store.serialize(true);
    }

    private CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, String code) {
        return getRegistrationOptions(store, keys, Map.entry("code", code.replaceAll("-", "")))
                .thenComposeAsync(attrs -> sendRegistrationRequest(store, "/register", attrs))
                .thenAcceptAsync(RegistrationHelper::checkResponse);
    }

    private void checkResponse(HttpResponse<String> result) {
        Validate.isTrue(result.statusCode() == HttpURLConnection.HTTP_OK,
                "Invalid status code: %s", RegistrationException.class, result.statusCode(), result.body());
        var response = Json.readValue(result.body(), VerificationCodeResponse.class);
        if(response.status().isSuccessful()){
            return;
        }

        throw new RegistrationException("Invalid response: %s".formatted(result.body()));
    }

    private CompletableFuture<HttpResponse<String>> sendRegistrationRequest(Store store, String path, Map<String, Object> params) {
        var client = createClient(store);
        var encodedParams = toFormParams(params);
        var keypair = SignalKeyPair.random();
        var key = Curve25519.sharedKey(Whatsapp.REGISTRATION_PUBLIC_KEY, keypair.privateKey());
        var buffer = AesGmc.encrypt(new byte[12], encodedParams.getBytes(StandardCharsets.UTF_8), key);
        var enc = Base64.getUrlEncoder().encodeToString(Bytes.of(keypair.publicKey(), buffer).toByteArray());
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
        var osVersion = store.osVersion();
        var manufacturer = store.manufacturer();
        var model = store.model().replaceAll(" ", "_");
        return "WhatsApp/%s %s/%s Device/%s-%s".formatted(store.version(), osName, osVersion, manufacturer, model);
    }

    private String getMobileOsName(Store store) {
        return switch (store.os()) {
            case ANDROID -> store.business() ? "SMBA" : "Android";
            case IOS -> store.business() ? "SMBI" : "iOS";
            default -> throw new IllegalStateException("Unsupported mobile os: " + store.os());
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
    private CompletableFuture<Map<String, Object>> getRegistrationOptions(Store store, Keys keys, Entry<String, Object>... attributes) {
        return MetadataHelper.getToken(store.phoneNumber().get().numberWithoutPrefix(), store.os(), store.business())
                .thenApplyAsync(token -> getRegistrationOptions(store, keys, token, attributes));
    }

    // TODO: Add backup token, locale and language and expid
    private Map<String, Object> getRegistrationOptions(Store store, Keys keys, String token, Entry<String, Object>[] attributes) {
        return Attributes.of(attributes)
                .put("cc", store.phoneNumber().get().countryCode().prefix())
                .put("in", store.phoneNumber().get().numberWithoutPrefix())
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
