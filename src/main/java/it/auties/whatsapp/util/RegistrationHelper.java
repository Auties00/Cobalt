package it.auties.whatsapp.util;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import it.auties.whatsapp.model.request.Attributes;
import it.auties.whatsapp.util.Spec.Whatsapp;
import lombok.experimental.UtilityClass;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
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

    public CompletableFuture<VerificationCodeResponse> requestVerificationCode(Store store, Keys keys, VerificationCodeMethod method) {
        if(method == VerificationCodeMethod.NONE){
            return CompletableFuture.completedFuture(null);
        }

        var codeOptions = getRegistrationOptions(store, keys,
                Map.entry("mcc", store.phoneNumber().countryCode().mcc()),
                Map.entry("mnc", store.phoneNumber().countryCode().mnc()),
                Map.entry("sim_mcc", "000"),
                Map.entry("sim_mnc", "000"), Map.entry("method", method.type()),
                Map.entry("reason", ""),
                Map.entry("hasav", "1"));
        return sendRegistrationRequest(store,"/code", codeOptions)
                .thenApplyAsync(RegistrationHelper::readResponse);
    }

    public CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, Supplier<CompletableFuture<String>> handler) {
        return handler.get()
                .thenComposeAsync(result -> sendVerificationCode(store, keys, result))
                .thenRunAsync(() -> saveRegistrationStatus(store, keys));
    }

    private void saveRegistrationStatus(Store store, Keys keys) {
        keys.registered(true);
        keys.serialize(true);
        store.jid(store.phoneNumber().toJid());
        store.serialize(true);
    }

    private CompletableFuture<VerificationCodeResponse> sendVerificationCode(Store store, Keys keys, String code) {
        var registerOptions = getRegistrationOptions(store, keys, Map.entry("code", code.replaceAll("-", "")));
        return sendRegistrationRequest(store, "/register", registerOptions)
                .thenApplyAsync(RegistrationHelper::readResponse);
    }

    private static VerificationCodeResponse readResponse(HttpResponse<String> result) {
        var response = Json.readValue(result.body(), VerificationCodeResponse.class);
        Validate.isTrue(response.status().isSuccessful(), "Invalid response: %s".formatted(result));
        return response;
    }

    private CompletableFuture<HttpResponse<String>> sendRegistrationRequest(Store store, String path, Map<String, Object> params) {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create("%s%s?%s".formatted(Whatsapp.MOBILE_REGISTRATION_ENDPOINT, path, toFormParams(params))))
                .GET()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", Whatsapp.MOBILE_UA.formatted(store.version()))
                .build();
        return client.sendAsync(request, BodyHandlers.ofString());
    }

    @SafeVarargs
    private Map<String, Object> getRegistrationOptions(Store store, Keys keys, Entry<String, Object>... attributes) {
        return Attributes.of(attributes)
                .put("cc", store.phoneNumber().countryCode().prefix())
                .put("in", store.phoneNumber().number())
                .put("lg", "en")
                .put("lc", "GB")
                .put("mistyped", "6")
                .put("authkey", Base64.getUrlEncoder().encodeToString(keys.noiseKeyPair().publicKey()))
                .put("e_regid", Base64.getUrlEncoder().encodeToString(keys.encodedRegistrationId()))
                .put("e_keytype", "BQ")
                .put("e_ident", Base64.getUrlEncoder().encodeToString(keys.identityKeyPair().publicKey()))
                .put("e_skey_id", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().encodedId()))
                .put("e_skey_val", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().publicKey()))
                .put("e_skey_sig", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().signature()))
                .put("fdid", keys.phoneId())
                .put("expid", keys.deviceId())
                .put("network_radio_type", "1")
                .put("simnum", "1")
                .put("hasinrc", "1")
                .put("pid", ThreadLocalRandom.current().nextInt(1000))
                .put("rc", "0")
                .put("id", keys.identityId())
                .put("token", HexFormat.of().formatHex(MD5.calculate(Whatsapp.MOBILE_TOKEN + store.phoneNumber().number())))
                .toMap();
    }

    private String toFormParams(Map<String, Object> values) {
        return values.entrySet()
                .stream()
                .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("&"));
    }
}
