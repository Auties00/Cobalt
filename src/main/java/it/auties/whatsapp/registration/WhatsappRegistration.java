package it.auties.whatsapp.registration;

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
import it.auties.whatsapp.util.*;
import it.auties.whatsapp.util.Specification.Whatsapp;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class WhatsappRegistration {
    static {
        Authenticator.setDefault(new ProxyAuthenticator());
    }

    private final HttpClient httpClient;
    private final Store store;
    private final Keys keys;
    private final AsyncVerificationCodeSupplier codeHandler;
    private final VerificationCodeMethod method;

    public WhatsappRegistration(Store store, Keys keys, AsyncVerificationCodeSupplier codeHandler, VerificationCodeMethod method) {
        this.store = store;
        this.keys = keys;
        this.codeHandler = codeHandler;
        this.method = method;
        this.httpClient = createClient();
    }

    public CompletableFuture<Void> registerPhoneNumber() {
        return requestVerificationCode(false)
                .thenCompose(ignored -> sendVerificationCode())
                .whenComplete((result, exception) -> {
                    dispose();
                    if(exception != null) {
                        Exceptions.rethrow(exception);
                    }
                });
    }

    public CompletableFuture<Void> requestVerificationCode() {
        return requestVerificationCode(true);
    }

    private CompletableFuture<Void> requestVerificationCode(boolean closeResources) {
        if(method == VerificationCodeMethod.NONE) {
            return CompletableFuture.completedFuture(null);
        }

        return switch (store.device().platform()) {
            case IOS, IOS_BUSINESS -> onboard("1", 2155550000L, null)
                    .thenComposeAsync(response -> onboard(null, null, response.abHash()))
                    .thenComposeAsync(ignored -> exists(null))
                    .thenComposeAsync(result -> clientLog(result, Map.entry("current_screen", "verify_sms"), Map.entry("previous_screen", "enter_number"), Map.entry("action_taken", "continue"))
                            .thenComposeAsync(response -> requestVerificationCode(response, null)))
                    .whenComplete((result, exception) -> onRequestVerificationCode(closeResources, exception));
            case ANDROID, ANDROID_BUSINESS -> exists(null)
                    .thenComposeAsync(response -> requestVerificationCode(response, null))
                    .whenComplete((result, exception) -> onRequestVerificationCode(closeResources, exception));
            case KAIOS -> requestVerificationCode(null, null)
                    .whenComplete((result, exception) -> onRequestVerificationCode(closeResources, exception));
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
    }

    private void onRequestVerificationCode(boolean closeResources, Throwable exception) {
        if(closeResources) {
            dispose();
        }

        if (exception != null) {
            Exceptions.rethrow(exception);
        }
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
        System.out.println(Whatsapp.MOBILE_REGISTRATION_ENDPOINT + "/reg_onboard_abprop?" + toFormParams(attributes));
        var request = HttpRequest.newBuilder()
                .uri(URI.create(Whatsapp.MOBILE_REGISTRATION_ENDPOINT + "/reg_onboard_abprop?" + toFormParams(attributes)))
                .GET()
                .header("User-Agent", store.device().toUserAgent(store.version()))
                .header("Content-Type","application/x-www-form-urlencoded")
                .build();
        return httpClient.sendAsync(request, BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                        throw new RegistrationException(null, response.body());
                    }

                    System.out.println(response.body());
                    return Json.readValue(response.body(), AbPropsResponse.class);
                });
    }

    private CompletableFuture<RegistrationResponse> exists(VerificationCodeError lastError) {
        var ios = store.device().platform().isIOS();
        var options = getRegistrationOptions(
                store,
                keys,
                false,
                ios ? Map.entry("offline_ab", convertBufferToUrlHex(createOfflineAb())) : null,
                ios ? Map.entry("recovery_token_error", "-25300") : null
        );
        return options.thenComposeAsync(attrs -> sendRequest("/exist", attrs)).thenComposeAsync(result -> {
            if (result.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new RegistrationException(null, result.body());
            }

            var response = Json.readValue(result.body(), RegistrationResponse.class);
            if (response.errorReason() == VerificationCodeError.INCORRECT) {
                return CompletableFuture.completedFuture(response);
            }

            if (lastError == null) {
                return exists(response.errorReason());
            }

            throw new RegistrationException(response, result.body());
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
        return options.thenCompose(attrs -> sendRequest("/client_log", attrs)).thenApply(result -> {
            System.out.println(result.body());
            return data;
        });
    }

    private CompletableFuture<Void> requestVerificationCode(RegistrationResponse existsResponse, VerificationCodeError lastError) {
        var options = getRegistrationOptions(
                store,
                keys,
                true,
                getRequestVerificationCodeParameters(existsResponse)
        );
        return options.thenCompose(attrs -> sendRequest("/code", attrs))
                .thenCompose(result -> onCodeRequestSent(existsResponse, lastError, result))
                .thenRun(() -> saveRegistrationStatus(store, keys, false));
    }

    private Entry<String, Object>[] getRequestVerificationCodeParameters(RegistrationResponse existsResponse) {
        var countryCode = store.phoneNumber()
                .orElseThrow()
                .countryCode();
        return switch(store.device().platform()) {
            case UNKNOWN -> new Entry[]{};
            case ANDROID, ANDROID_BUSINESS -> {
                var gpiaToken = WhatsappMetadata.generateGpiaToken(keys.advertisingId(), keys.deviceId(), store.device().platform().isBusiness());
                yield new Entry[]{
                        Map.entry("method", method.data()),
                        Map.entry("sim_mcc", countryCode.mcc()),
                        Map.entry("sim_mnc", "001"),
                        Map.entry("reason", ""),
                        Map.entry("mcc", countryCode.mcc()),
                        Map.entry("mnc", "001"),
                        Map.entry("feo2_query_status", "error_security_exception"),
                        Map.entry("sim_type", 1),
                        Map.entry("network_radio_type", 1),
                        Map.entry("prefer_sms_over_flash", true),
                        Map.entry("simnum", 0),
                        Map.entry("sim_state", 3),
                        Map.entry("clicked_education_link", false),
                        Map.entry("airplane_mode_type", 0),
                        Map.entry("mistyped", 7),
                        Map.entry("advertising_id", UUID.randomUUID().toString()),
                        Map.entry("hasinrc", 1),
                        Map.entry("roaming_type", 0),
                        Map.entry("device_ram", 4),
                        Map.entry("client_metrics", URLEncoder.encode("{\"attempts\":1}", StandardCharsets.UTF_8)),
                        Map.entry("education_screen_displayed", true),
                        Map.entry("read_phone_permission_granted", 1),
                        Map.entry("pid", ProcessHandle.current().pid()),
                        Map.entry("cellular_strength", ThreadLocalRandom.current().nextInt(3, 6)),
                        Map.entry("gpia_token", gpiaToken),
                        Map.entry("gpia", "%7B%22token%22%3A%22" + gpiaToken + "%22%2C%22error_code%22%3A0%7D")
                };
            }
            case IOS, IOS_BUSINESS -> new Entry[]{
                    Map.entry("method", method.data()),
                    Map.entry("sim_mcc", existsResponse.flashType() ? countryCode.mcc() : "000"),
                    Map.entry("sim_mnc", "000"),
                    Map.entry("reason", ""),
                    Map.entry("cellular_strength", 1)
            };
            case KAIOS -> new Entry[]{
                    Map.entry("mcc", countryCode.mcc()),
                    Map.entry("mnc", "000"),
                    Map.entry("method", method.data()),
            };
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
    }

    private CompletionStage<Void> onCodeRequestSent(RegistrationResponse existsResponse, VerificationCodeError lastError, HttpResponse<String> result) {
        if (result.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new RegistrationException(null, result.body());
        }

        var response = Json.readValue(result.body(), RegistrationResponse.class);
        if (response.status() == VerificationCodeStatus.SUCCESS) {
            return CompletableFuture.completedFuture(null);
        }

        return switch (response.errorReason()) {
            case TOO_RECENT, TOO_MANY, TOO_MANY_GUESSES, TOO_MANY_ALL_METHODS -> throw new RegistrationException(response, "Please wait before trying to register this phone number again");
            case NO_ROUTES -> throw new RegistrationException(response, "You can only register numbers that are already on Whatsapp, if you need to register any numbers please contact me on Telegram @Auties00");
            default -> {
                var newErrorReason = response.errorReason();
                Validate.isTrue(newErrorReason != lastError, () -> new RegistrationException(response, result.body()));
                yield requestVerificationCode(existsResponse, newErrorReason);
            }
        };
    }

    public CompletableFuture<Void> sendVerificationCode() {
        return codeHandler.get()
                .thenComposeAsync(code -> getRegistrationOptions(store, keys, true, Map.entry("code", normalizeCodeResult(code))))
                .thenComposeAsync(attrs -> sendRequest("/register", attrs))
                .thenComposeAsync(result -> {
                    if (result.statusCode() != HttpURLConnection.HTTP_OK) {
                        throw new RegistrationException(null, result.body());
                    }

                    var response = Json.readValue(result.body(), RegistrationResponse.class);
                    if (response.status() == VerificationCodeStatus.SUCCESS) {
                        saveRegistrationStatus(store, keys, true);
                        return CompletableFuture.completedFuture(null);
                    }

                    throw new RegistrationException(response, result.body());
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

    private CompletableFuture<HttpResponse<String>> sendRequest(String path, Map<String, Object> params) {
        var request = createRequest(path, params);
        return httpClient.sendAsync(request, BodyHandlers.ofString()).thenApply(result -> {
            System.out.println(path + ": " + result.body());
            return result;
        });
    }

    private HttpRequest createRequest(String path, Map<String, Object> params) {
        var encodedParams = toFormParams(params);
        var userAgent = store.device().toUserAgent(store.version());
        if(store.device().platform().isKaiOs()) {
            return HttpRequest.newBuilder()
                    .uri(URI.create("%s%s?%s".formatted(Whatsapp.MOBILE_KAIOS_REGISTRATION_ENDPOINT, path, encodedParams)))
                    .GET()
                    .header("User-Agent", userAgent)
                    .build();
        }

        var keypair = SignalKeyPair.random();
        var key = Curve25519.sharedKey(Whatsapp.REGISTRATION_PUBLIC_KEY, keypair.privateKey());
        var buffer = AesGcm.encrypt(new byte[12], encodedParams.getBytes(StandardCharsets.UTF_8), key);
        var cipheredParameters = Base64.getUrlEncoder().encodeToString(BytesHelper.concat(keypair.publicKey(), buffer));
        var request = HttpRequest.newBuilder()
                .uri(URI.create("%s%s?ENC=%s".formatted(Whatsapp.MOBILE_REGISTRATION_ENDPOINT, path, cipheredParameters)))
                .GET()
                .header("User-Agent", userAgent);
        if(store.device().platform().isAndroid()) {
            request.header("Accept", "text/json");
            request.header("WaMsysRequest", "1");
            request.header("request_token", UUID.randomUUID().toString());
            request.header("Content-Type", "application/x-www-form-urlencoded");
        }

        return request.build();
    }

    private HttpClient createClient() {
        try {
            var clientBuilder = HttpClient.newBuilder();
            store.proxy().ifPresent(proxy -> {
                clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())));
                clientBuilder.authenticator(new ProxyAuthenticator());
            });
            return clientBuilder.build();
        }catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    @SafeVarargs
    private CompletableFuture<Map<String, Object>> getRegistrationOptions(Store store, Keys keys, boolean useToken, Entry<String, Object>... attributes) {
        var phoneNumber = store.phoneNumber()
                .orElseThrow(() -> new NoSuchElementException("Missing phone number"));
        var tokenFuture = !useToken ? CompletableFuture.completedFuture(null) : WhatsappMetadata.getToken(phoneNumber.numberWithoutPrefix(), store.device().platform(), store.version());
        return tokenFuture.thenApplyAsync(token -> {
            var certificate = store.device().platform().isBusiness() ? WhatsappMetadata.generateBusinessCertificate(keys) : null;
            var requiredAttributes = Arrays.stream(attributes)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (first, second) -> first, LinkedHashMap::new));
            var result = Attributes.of()
                    .put("cc", phoneNumber.countryCode().prefix())
                    .put("in", phoneNumber.numberWithoutPrefix())
                    .put("rc", store.releaseChannel().index(), !store.device().platform().isKaiOs())
                    .put("lg", phoneNumber.countryCode().lg())
                    .put("lc", phoneNumber.countryCode().lc())
                    .put("authkey", Base64.getUrlEncoder().encodeToString(keys.noiseKeyPair().publicKey()))
                    .put("vname", certificate, certificate != null)
                    .put("e_regid", Base64.getUrlEncoder().encodeToString(keys.encodedRegistrationId()))
                    .put("e_keytype", Base64.getUrlEncoder().encodeToString(Specification.Signal.KEY_BUNDLE_TYPE))
                    .put("e_ident", Base64.getUrlEncoder().encodeToString(keys.identityKeyPair().publicKey()))
                    .put("e_skey_id", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().encodedId()))
                    .put("e_skey_val", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().publicKey()))
                    .put("e_skey_sig", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().signature()))
                    .put("fdid", keys.fdid().toLowerCase(Locale.ROOT), store.device().platform().isAndroid())
                    .put("fdid", keys.fdid().toUpperCase(Locale.ROOT), store.device().platform().isIOS())
                    .put("expid", Base64.getUrlEncoder().encodeToString(keys.deviceId()), !store.device().platform().isKaiOs())
                    .put("id", convertBufferToUrlHex(keys.identityId()))
                    .put("token", token, useToken)
                    .putAll(requiredAttributes)
                    .toMap();
            System.out.println(Json.writeValueAsString(result, true));
            return result;
        });
    }

    private byte[] createOfflineAb() {
        return "{\"exposure\":[\"dummy_aa_offline_rid_universe_ios|dummy_aa_offline_rid_experiment_ios|control\",\"hide_link_device_button_release_rollout_universe|hide_link_device_button_release_rollout_experiment|control\",\"ios_prod_latam_tos_reg_universe|ios_prod_latam_tos_reg_experiment|control\"],\"metrics\":{\"expid_c\":true,\"fdid_c\":true,\"rc_c\":true,\"expid_md\":1701998247,\"expid_cd\":1701998247}}"
                .getBytes(StandardCharsets.UTF_8);
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
