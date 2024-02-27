package it.auties.whatsapp.registration;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.AsyncVerificationCodeSupplier;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.exception.RegistrationException;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.mobile.CountryCode;
import it.auties.whatsapp.model.mobile.VerificationCodeError;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeStatus;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.response.AbPropsResponse;
import it.auties.whatsapp.model.response.RegistrationResponse;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.registration.apns.ApnsClient;
import it.auties.whatsapp.registration.apns.ApnsPacket;
import it.auties.whatsapp.registration.apns.ApnsPayloadTag;
import it.auties.whatsapp.util.*;
import it.auties.whatsapp.util.Specification.Whatsapp;

import javax.net.ssl.SSLContext;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class WhatsappRegistration {
    static {
        ProxyAuthenticator.allowAll();
    }

    private final HttpClient httpClient;
    private final Store store;
    private final Keys keys;
    private final AsyncVerificationCodeSupplier codeHandler;
    private final VerificationCodeMethod method;
    private final ApnsClient apnsClient;
    private final CountryCode countryCode;

    public WhatsappRegistration(Store store, Keys keys, AsyncVerificationCodeSupplier codeHandler, VerificationCodeMethod method) {
        this.store = store;
        this.keys = keys;
        this.codeHandler = codeHandler;
        this.method = method;
        this.httpClient = createClient();
        var platform = store.device().platform();
        this.apnsClient = platform.isIOS() && method != VerificationCodeMethod.NONE ? new ApnsClient(store.proxy().orElse(null)) : null;
        this.countryCode = CountryCode.values()[ThreadLocalRandom.current().nextInt(CountryCode.values().length)];
    }

    public CompletableFuture<RegistrationResponse> registerPhoneNumber() {
        return requestVerificationCode(false)
                .thenCompose(ignored -> sendVerificationCode())
                .whenComplete((result, exception) -> {
                    dispose();
                    if (exception != null) {
                        Exceptions.rethrow(exception);
                    }
                });
    }

    public CompletableFuture<RegistrationResponse> requestVerificationCode() {
        return requestVerificationCode(true);
    }

    private CompletableFuture<RegistrationResponse> requestVerificationCode(boolean closeResources) {
        if (method == VerificationCodeMethod.NONE) {
            return CompletableFuture.completedFuture(null);
        }

        var originalDevice = store.device();
        store.setDevice(originalDevice.toPersonal());
        var future = switch (store.device().platform()) {
            case IOS -> onboard("1", 2155550000L, null)
                    .thenComposeAsync(response -> onboard(null, null, response.abHash()), CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS))
                    .thenComposeAsync(pushToken -> exists(originalDevice, null))
                    .thenComposeAsync(response -> clientLog(response, Map.entry("current_screen", "verify_sms"), Map.entry("previous_screen", "enter_number"), Map.entry("action_taken", "continue")), CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS))
                    .thenComposeAsync(ignored -> getIOSPushCode())
                    .thenComposeAsync(result -> requestVerificationCode(result, null));
            case ANDROID -> exists(null, null)
                    .thenComposeAsync(response -> requestVerificationCode(null, null));
            case KAIOS -> requestVerificationCode(null, null);
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
        return future.whenComplete((result, exception) -> {
            store.setDevice(originalDevice);
            if (closeResources) {
                dispose();
            }

            if (exception != null) {
                Exceptions.rethrow(exception);
            }
        });
    }


    public CompletableFuture<Boolean> exists() {
        return exists(null, null)
                .thenApplyAsync(RegistrationResponse::whatsappOldEligible)
                .whenCompleteAsync((result, exception) -> {
                    dispose();
                    if (exception != null) {
                        Exceptions.rethrow(exception);
                    }
                });
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
                .uri(URI.create(Whatsapp.MOBILE_REGISTRATION_ENDPOINT + "/reg_onboard_abprop?" + toFormParams(attributes)))
                .GET()
                .header("User-Agent", store.device().toUserAgent(store.version()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        System.out.println("Sending request to /reg_onboard_abprop with parameters " + attributes);
        return httpClient.sendAsync(request, BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new RegistrationException(null, "Invalid status code: " + response.statusCode());
            }

            System.out.println("Received respose /reg_onboard_abprop " + response.body());
            return Json.readValue(response.body(), AbPropsResponse.class);
        });
    }

    private CompletableFuture<String> getIOSPushToken() {
        if (apnsClient == null) {
            return CompletableFuture.completedFuture(null);
        }

        return apnsClient.getAppToken(store.device().platform().isBusiness());
    }

    private CompletableFuture<RegistrationResponse> exists(CompanionDevice originalDevice, VerificationCodeError lastError) {
        return getIOSPushToken().thenComposeAsync(pushToken -> {
            var ios = store.device().platform().isIOS();
            var options = getRegistrationOptions(
                    store,
                    keys,
                    false,
                    ios ? Map.entry("offline_ab", Specification.Whatsapp.MOBILE_OFFLINE_AB) : null,
                    pushToken == null ? null : Map.entry("push_token", convertBufferToUrlHex(pushToken.getBytes(StandardCharsets.UTF_8))),
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

                if (lastError != null) {
                    throw new RegistrationException(response, result.body());
                }

                var useOriginalDevice = originalDevice != null && response.errorReason() == VerificationCodeError.FORMAT_WRONG;
                var currentDevice = store.device();
                if(useOriginalDevice) {
                    store.setDevice(originalDevice);
                }

                return exists(originalDevice, response.errorReason()).whenComplete((finalResult, error) -> {
                    if(useOriginalDevice) {
                        store.setDevice(currentDevice);
                    }

                    if(error != null) {
                        Exceptions.rethrow(error);
                    }
                });
            });
        });
    }

    private CompletableFuture<String> getIOSPushCode() {
        if (apnsClient == null) {
            return CompletableFuture.completedFuture(null);
        }

        return apnsClient.waitForPacket(packet -> packet.tag() == ApnsPayloadTag.NOTIFICATION)
                .thenApply(this::readIOSPushCode)
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(ignored -> {
                    throw new RegistrationException(null, "Apns timeout");
                });
    }

    private String readIOSPushCode(ApnsPacket packet) {
        var payload = packet.fields().get(0x3);
        var json = Json.readValue(payload, new TypeReference<Map<String, Object>>() {
        });
        return (String) json.get("regcode");
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

    private CompletableFuture<RegistrationResponse> requestVerificationCode(String pushCode, VerificationCodeError lastError) {
        return getRequestVerificationCodeParameters(pushCode)
                .thenCompose(params -> getRegistrationOptions(store, keys, true, params))
                .thenCompose(attrs -> sendRequest("/code", attrs))
                .thenCompose(result -> onCodeRequestSent(pushCode, lastError, result))
                .thenApply(result -> {
                    saveRegistrationStatus(store, keys, false);
                    return result;
                });
    }

    private CompletableFuture<Entry<String, Object>[]> getRequestVerificationCodeParameters(String pushCode) {
        var countryCode = store.phoneNumber()
                .orElseThrow()
                .countryCode();
        return switch (store.device().platform()) {
            case ANDROID, ANDROID_BUSINESS ->
                    WhatsappMetadata.generateGpiaToken(keys.advertisingId(), keys.deviceId(), store.device().platform().isBusiness())
                            .thenApply(gpiaToken -> getAndroidRequestParameters(gpiaToken, countryCode));
            case IOS, IOS_BUSINESS -> CompletableFuture.completedFuture(getIosRequestParameters(pushCode));
            case KAIOS -> CompletableFuture.completedFuture(getKaiOsRequestParameters(countryCode));
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
    }

    @SuppressWarnings("unchecked")
    private Entry<String, Object>[] getKaiOsRequestParameters(CountryCode countryCode) {
        return new Entry[]{
                Map.entry("mcc", countryCode.mcc()),
                Map.entry("mnc", "000"),
                Map.entry("method", method.data()),
        };
    }

    @SuppressWarnings("unchecked")
    private Entry<String, Object>[] getIosRequestParameters(String pushCode) {
        return new Entry[]{
                Map.entry("method", method.data()),
                Map.entry("sim_mcc", "000"),
                Map.entry("sim_mnc", "000"),
                Map.entry("reason", ""),
                Map.entry("push_code", convertBufferToUrlHex(pushCode.getBytes(StandardCharsets.UTF_8))),
                Map.entry("cellular_strength", ThreadLocalRandom.current().nextInt(2, 5))
        };
    }

    @SuppressWarnings("unchecked")
    private Entry<String, Object>[] getAndroidRequestParameters(String gpiaToken, CountryCode countryCode) {
        return new Entry[]{
                Map.entry("method", method.data()),
                Map.entry("sim_mcc", countryCode.mcc()),
                Map.entry("sim_mnc", countryCode.mcc()),
                Map.entry("reason", ""),
                Map.entry("mcc", countryCode.mcc()),
                Map.entry("mnc", "000"),
                Map.entry("feo2_query_status", "error_security_exception"),
                Map.entry("sim_type", 1),
                Map.entry("network_radio_type", 1),
                Map.entry("prefer_sms_over_flash", true),
                Map.entry("simnum", 0),
                Map.entry("sim_state", 3),
                Map.entry("clicked_education_link", false),
                Map.entry("airplane_mode_type", 0),
                Map.entry("mistyped", 7),
                Map.entry("advertising_id", keys.advertisingId()),
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

    private CompletionStage<RegistrationResponse> onCodeRequestSent(String pushCode, VerificationCodeError lastError, HttpResponse<String> result) {
        if (result.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new RegistrationException(null, result.body());
        }

        var response = Json.readValue(result.body(), RegistrationResponse.class);
        if (response.status() == VerificationCodeStatus.SUCCESS) {
            return CompletableFuture.completedFuture(response);
        }

        return switch (response.errorReason()) {
            case TOO_RECENT, TOO_MANY, TOO_MANY_GUESSES, TOO_MANY_ALL_METHODS ->
                    throw new RegistrationException(response, "Please wait before trying to register this phone number again");
            case NO_ROUTES, BLOCKED -> throw new RegistrationException(response, result.body());
            default -> {
                var newErrorReason = response.errorReason();
                Validate.isTrue(newErrorReason != lastError, () -> new RegistrationException(response, result.body()));
                yield requestVerificationCode(pushCode, newErrorReason);
            }
        };
    }

    public CompletableFuture<RegistrationResponse> sendVerificationCode() {

        var future = CompletableFuture.completedFuture(null);
        if (store.device().platform().isBusiness()) {
            future = clientLog(null, Map.entry("event_name", "smb_client_onboarding_journey"),
                    Map.entry("is_logged_in_on_consumer_app", "0"),
                    Map.entry("sequence_number", "14"),
                    Map.entry("app_install_source", "unknown|unknown"),
                    Map.entry("smb_onboarding_step", "20"),
                    Map.entry("has_consumer_app", "1")
            );
        }

        return future
                .thenComposeAsync((ignored) -> codeHandler.get())
                .thenComposeAsync(code -> getRegistrationOptions(store, keys, true, Map.entry("code", normalizeCodeResult(code)), Map.entry("entered", "1")))
                .thenComposeAsync(attrs -> sendRequest("/register", attrs))
                .thenComposeAsync(result -> {
                    if (result.statusCode() != HttpURLConnection.HTTP_OK) {
                        throw new RegistrationException(null, result.body());
                    }

                    var response = Json.readValue(result.body(), RegistrationResponse.class);
                    if (response.status() == VerificationCodeStatus.SUCCESS) {
                        saveRegistrationStatus(store, keys, true);
                        return CompletableFuture.completedFuture(response);
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
        System.out.println("Sending request to " + path + " with parameters " + params);
        var request = createRequest(path, params);
        return httpClient.sendAsync(request, BodyHandlers.ofString()).thenCompose(response -> {
            System.out.println("Received response " + path + " " + response.body());

            return CompletableFuture.completedFuture(response);
        }).exceptionallyCompose(exception -> {
            System.out.println("Failed to send request " + path + " error: " + exception);
            return CompletableFuture.failedFuture(exception);
        });
    }

    private HttpRequest createRequest(String path, Map<String, Object> params) {
        var encodedParams = toFormParams(params);
        var userAgent = store.device().toUserAgent(store.version());
        if (store.device().platform().isKaiOs()) {
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
        System.out.println("Using user agent " + userAgent);
        if (store.device().platform().isAndroid()) {
            request.header("Accept", "text/json");
            request.header("WaMsysRequest", "1");
            request.header("request_token", UUID.randomUUID().toString());
            request.header("Content-Type", "application/x-www-form-urlencoded");
        }

        return request.build();
    }

    private HttpClient createClient() {
        try {
            var sslContext = SSLContext.getInstance("TLSv1." + (ThreadLocalRandom.current().nextBoolean() ? 2 : 3));
            sslContext.init(null, null, new SecureRandom());
            var sslParameters = sslContext.getDefaultSSLParameters();
            var supportedCiphers = Arrays.stream(sslContext.getDefaultSSLParameters().getCipherSuites())
                    .filter(entry -> ThreadLocalRandom.current().nextBoolean())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), result -> {
                        Collections.shuffle(result);
                        return result;
                    }))
                    .toArray(String[]::new);
            sslParameters.setCipherSuites(supportedCiphers);
            var supportedNamedGroups = Arrays.stream(sslContext.getDefaultSSLParameters().getNamedGroups())
                    .filter(entry -> ThreadLocalRandom.current().nextBoolean())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), result -> {
                        Collections.shuffle(result);
                        return result;
                    }))
                    .toArray(String[]::new);
            sslParameters.setNamedGroups(supportedNamedGroups);
            var version = ThreadLocalRandom.current().nextBoolean() ? HttpClient.Version.HTTP_1_1 : HttpClient.Version.HTTP_2;
            var clientBuilder = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .sslParameters(sslParameters)
                    .version(version);
            store.proxy().ifPresent(proxy -> {
                clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())));
                clientBuilder.authenticator(new ProxyAuthenticator());
            });
            return clientBuilder.build();
        } catch (Throwable exception) {
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
            return Attributes.of()
                    .put("cc", phoneNumber.countryCode().prefix())
                    .put("in", phoneNumber.numberWithoutPrefix())
                    .put("rc", store.releaseChannel().index(), !store.device().platform().isKaiOs())
                    .put("lg", countryCode.lg())
                    .put("lc", countryCode.lc())
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
        if (apnsClient != null) {
            apnsClient.close();
        }
    }
}
