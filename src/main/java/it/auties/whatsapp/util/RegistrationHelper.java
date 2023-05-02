package it.auties.whatsapp.util;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.exception.RegistrationException;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import it.auties.whatsapp.model.request.Attributes;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.util.Spec.Whatsapp;
import lombok.experimental.UtilityClass;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.Security;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@UtilityClass
public class RegistrationHelper {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

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

        var codeOptions = getRegistrationOptions(
                store,
                keys,
                Map.entry("mcc", store.phoneNumber().countryCode().mcc()),
                Map.entry("mnc", store.phoneNumber().countryCode().mnc()),
                Map.entry("sim_mcc", "000"),
                Map.entry("sim_mnc", "000"),
                Map.entry("method", method.type()),
                Map.entry("reason", ""),
                Map.entry("hasav", "1")
        );
        return sendRegistrationRequest(store,"/code", codeOptions)
                .thenAcceptAsync(RegistrationHelper::checkResponse)
                .thenRunAsync(() -> saveRegistrationStatus(store, keys, false));
    }

    public CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, Supplier<CompletableFuture<String>> handler) {
        return handler.get()
                .thenComposeAsync(result -> sendVerificationCode(store, keys, result))
                .thenRunAsync(() -> saveRegistrationStatus(store, keys, true));
    }

    private void saveRegistrationStatus(Store store, Keys keys, boolean registered) {
        keys.registered(registered);
        if(registered){
            store.jid(store.phoneNumber().toJid());
            store.addLinkedDevice(store.jid(), 0);
        }
        keys.serialize(true);
        store.serialize(true);
    }

    private CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, String code) {
        var registerOptions = getRegistrationOptions(store, keys, Map.entry("code", code.replaceAll("-", "")));
        return sendRegistrationRequest(store, "/register", registerOptions)
                .thenAcceptAsync(RegistrationHelper::checkResponse);
    }

    private void checkResponse(HttpResponse<String> result) {
        Validate.isTrue(result.statusCode() == HttpURLConnection.HTTP_OK,
                "Invalid status code: %s", RegistrationException.class, result.statusCode(), result.body());
        var response = Json.readValue(result.body(), VerificationCodeResponse.class);
        Validate.isTrue(response.status().isSuccessful(),
                "Invalid response: %s", RegistrationException.class, result.body());
    }

    private CompletableFuture<HttpResponse<String>> sendRegistrationRequest(Store store, String path, Map<String, Object> params) {
        var client = createClient(store);
        var request = HttpRequest.newBuilder()
                .uri(URI.create("%s%s?%s".formatted(Whatsapp.MOBILE_REGISTRATION_ENDPOINT, path, toFormParams(params))))
                .GET()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", getUserAgent(store))
                .build();
        return client.sendAsync(request, BodyHandlers.ofString());
    }

    private String getUserAgent(Store store) {
        return "WhatsApp/%s %s/%s Device/%s-%s".formatted(
                store.version(),
                getMobileOsName(store.osType()),
                store.osVersion(),
                store.manufacturer(),
                store.model()
        );
    }

    private Object getMobileOsName(UserAgentPlatform platform) {
        return switch (platform) {
            case ANDROID -> "Android";
            case IOS -> "iOS";
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
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
    private Map<String, Object> getRegistrationOptions(Store store, Keys keys, Entry<String, Object>... attributes) {
        Objects.requireNonNull(store.phoneNumber(), "Missing phone number: please specify it");
        return Attributes.of(attributes)
                .put("cc", store.phoneNumber().countryCode().prefix())
                .put("in", store.phoneNumber().number())
                .put("lg", "en")
                .put("lc", "GB")
                .put("mistyped", "6")
                .put("authkey", Base64.getUrlEncoder().encodeToString(keys.noiseKeyPair().publicKey()))
                .put("e_regid", Base64.getUrlEncoder().encodeToString(keys.encodedRegistrationId()))
                .put("e_ident", Base64.getUrlEncoder().encodeToString(keys.identityKeyPair().publicKey()))
                .put("e_skey_id", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().encodedId()))
                .put("e_skey_val", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().publicKey()))
                .put("e_skey_sig", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().signature()))
                .put("fdid", keys.phoneId())
                .put("expid", keys.deviceId())
                .put("network_radio_type", "1")
                .put("simnum", "1")
                .put("hasinrc", "1")
                .put("pid", ProcessHandle.current().pid())
                .put("rc", store.releaseChannel().index())
                .put("id", keys.identityId())
                .put("token", TokenHelper.getToken(String.valueOf(store.phoneNumber().number()), store.osType()))
                .toMap();
    }

    private String toFormParams(Map<String, Object> values) {
        return values.entrySet()
                .stream()
                .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("&"));
    }
}
