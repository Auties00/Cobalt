package it.auties.whatsapp.registration;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.AsyncVerificationCodeSupplier;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.exception.RegistrationException;
import it.auties.whatsapp.model.mobile.CountryCode;
import it.auties.whatsapp.model.mobile.VerificationCodeError;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeStatus;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.response.AbPropsResponse;
import it.auties.whatsapp.model.response.VerificationCodeResponse;
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
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class HttpRegistration {
    private final HttpClient httpClient;
    private final Store store;
    private final Keys keys;
    private final AsyncVerificationCodeSupplier codeHandler;
    private final VerificationCodeMethod method;
    private final ApnsClient apnsClient;

    public HttpRegistration(Store store, Keys keys, AsyncVerificationCodeSupplier codeHandler, VerificationCodeMethod method) {
        this.store = store;
        this.keys = keys;
        this.codeHandler = codeHandler;
        this.method = method;
        this.httpClient = createClient();
        var platform = store.device().platform();
        this.apnsClient = platform.isIOS() && method != VerificationCodeMethod.NONE ? new ApnsClient(store.proxy().orElse(null)) : null;
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

        var originalDevice = store.device();
        store.setDevice(originalDevice.toPersonal());
        var future = switch (store.device().platform()) {
            case IOS -> onboard("1", 2155550000L, null)
                    .thenComposeAsync(response -> onboard(null, null, response.abHash()), CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS))
                    .thenComposeAsync(ignored -> getIOSPushToken())
                    .thenComposeAsync(pushToken -> exists(pushToken, null))
                    .thenComposeAsync(response -> clientLog(response, Map.entry("current_screen", "verify_sms"), Map.entry("previous_screen", "enter_number"), Map.entry("action_taken", "continue")), CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS))
                    .thenComposeAsync(ignored -> getIOSPushCode())
                    .thenComposeAsync(result -> requestVerificationCode(result, null));
            case ANDROID -> exists(null, null)
                    .thenComposeAsync(response -> requestVerificationCode(null, null));
            case KAIOS -> requestVerificationCode(null, null);
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
        return future.whenComplete((result, exception) -> {
            store.setDevice(originalDevice);
            if(closeResources) {
                dispose();
            }

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
                .header("Content-Type","application/x-www-form-urlencoded")
                .build();
        return httpClient.sendAsync(request, BodyHandlers.ofString()).thenApply(response -> {
                    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                        throw new RegistrationException(null, response.body());
                    }

                    return Json.readValue(response.body(), AbPropsResponse.class);
                });
    }

    private CompletableFuture<String> getIOSPushToken() {
        if (apnsClient == null) {
            return CompletableFuture.completedFuture(null);
        }

        return apnsClient.getAppToken(store.device().platform().isBusiness());
    }

    private CompletableFuture<VerificationCodeResponse> exists(String pushToken, VerificationCodeError lastError) {
        var ios = store.device().platform().isIOS();
        var options = getRegistrationOptions(
                store,
                keys,
                false,
                lastError == VerificationCodeError.OLD_VERSION || lastError == VerificationCodeError.BAD_TOKEN,
                ios ? Map.entry("offline_ab", Specification.Whatsapp.MOBILE_OFFLINE_AB) : null,
                pushToken == null ? null : Map.entry("push_token", convertBufferToUrlHex(pushToken.getBytes(StandardCharsets.UTF_8))),
                ios ? Map.entry("recovery_token_error", "-25300") : null
        );
        return options.thenComposeAsync(attrs -> sendRequest("/exist", attrs)).thenComposeAsync(result -> {
            if (result.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new RegistrationException(null, result.body());
            }

            var response = Json.readValue(result.body(), VerificationCodeResponse.class);
            if(response.errorReason() != VerificationCodeError.INCORRECT) {
                if(lastError == null) {
                    return exists(pushToken, response.errorReason());
                }

                throw new RegistrationException(response, result.body());
            }

            return CompletableFuture.completedFuture(response);
        });
    }

    private CompletableFuture<String> getIOSPushCode() {
        if (apnsClient == null) {
            return CompletableFuture.completedFuture(null);
        }

        return apnsClient.waitForPacket(packet -> packet.tag() == ApnsPayloadTag.NOTIFICATION)
                .thenApply(this::readIOSPushCode)
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(ignored -> { throw new RegistrationException(null, "Apns timeout"); });
    }

    private String readIOSPushCode(ApnsPacket packet) {
        var payload = packet.fields().get(0x3);
        var json = Json.readValue(payload, new TypeReference<Map<String, Object>>() {});
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
                false,
                attributes
        );
        return options.thenCompose(attrs -> sendRequest("/client_log", attrs))
                .thenApply(result -> data);
    }

    private CompletableFuture<Void> requestVerificationCode(String pushCode, VerificationCodeError lastError) {
        return getRequestVerificationCodeParameters(pushCode)
                .thenCompose(params -> getRegistrationOptions(store, keys, true, lastError == VerificationCodeError.OLD_VERSION || lastError == VerificationCodeError.BAD_TOKEN, params))
                .thenCompose(attrs -> sendRequest("/code", attrs))
                .thenCompose(result -> onCodeRequestSent(pushCode, lastError, result))
                .thenRun(() -> saveRegistrationStatus(store, keys, false));
    }

    private CompletableFuture<Entry<String, Object>[]> getRequestVerificationCodeParameters(String pushCode) {
        var countryCode = store.phoneNumber()
                .orElseThrow()
                .countryCode();
        return switch (store.device().platform()) {
            case ANDROID, ANDROID_BUSINESS -> MobileMetadata.generateGpiaToken(keys.advertisingId(), keys.deviceId(), store.device().platform().isBusiness())
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
        // advertising_id=08f07498-3538-07af-0a17-de605008a0ac&authkey=oKRW0SeACNR34Z11F2Ta4FjS%2BqxxDjth4isTyKUT2Fw%3D&backup_token=Hf%D7%1Bn1%B7%A4Mo%E6%F2lU~%81e%EF%D6%EA&cc=60&client_metrics=%7B%22attempts%22%3A1%7D&code=619640&device_r
        //am=3.47&e_ident=v4Mhqpyc0pjUSIr5nGyYokOQ5YX3hmCfUWFK9dpsYig%3D&e_keytype=BQ&e_regid=DkMtCw&e_skey_id=AAAA&e_skey_sig=jAbjBtmTtgatB0pTsOjCpPeIJ1YtbI87Gi3UAg04s1N2C6FVZVx0Cn4r_4OuWtR4AqJmfX6Tekl_CH_gJT5SDg&e_skey_val=wxngrgjmyCJ6o5
        //XxF-elcj7ik1K6FDf2z1JVWlNr1Rc&entered=1&expid=tJvRbzwW-fUDhVjC1LP6zw&fdid=47b60d6b-26f5-8531-b036-68e5a3b93502&gpia=%7B%22token%22%3A%22CtcBARCnMGsQvXShsjMKfeEaT9QxHTIdiKXSO1Ub_e5dG7XIZPg4ILDe5ixRrn4aHSV6Vu-2n5hgBOIsonfqom8i-2h76
        //O6BNmPRd0fxOrkeKtYoVH6PDCMQ6iT9LEPEWetHlYLhfkU_JWGCZtVmLAl7MY3I9rWxED8IOl1JwYWkUR5gw7tnPwkP8M6MSs6DNzttmKYYUt7_1OBFYfKhKRpqzBwtJ-fO4R6auZRjNAVuDD-OrAJETcO4wRK5p3WdA8A8kxEWSvIM9UZRQS-2fsfxhEYY5hFcCyoaagHqwkoK2-SlqRBEjagtTnQlwVePYZ
        //XK4J3vaNiAlISep8Z34xIxjoVevHFrqsnZyiYBkmCtAPn5e2g8jQFDeiN99CruNv8x8qweKJdDXuztRMBAytFnwaSapaH6hsflfZP5Nxhcb_Rboo0%22%2C%22error_code%22%3A0%7D&gpia_token=CtcBARCnMGsQvXShsjMKfeEaT9QxHTIdiKXSO1Ub_e5dG7XIZPg4ILDe5ixRrn4aHSV6Vu-2n5h
        //gBOIsonfqom8i-2h76O6BNmPRd0fxOrkeKtYoVH6PDCMQ6iT9LEPEWetHlYLhfkU_JWGCZtVmLAl7MY3I9rWxED8IOl1JwYWkUR5gw7tnPwkP8M6MSs6DNzttmKYYUt7_1OBFYfKhKRpqzBwtJ-fO4R6auZRjNAVuDD-OrAJETcO4wRK5p3WdA8A8kxEWSvIM9UZRQS-2fsfxhEYY5hFcCyoaagHqwkoK2-Sl
        //qRBEjagtTnQlwVePYZXK4J3vaNiAlISep8Z34xIxjoVevHFrqsnZyiYBkmCtAPn5e2g8jQFDeiN99CruNv8x8qweKJdDXuztRMBAytFnwaSapaH6hsflfZP5Nxhcb_Rboo0&hasinrc=1&id=%9C%1B%DC%FE%07S%26%7Ds%B0d%8Dy%90%CA%DA%15%23%5C%AD&in=1128715023&lc=PE&lg=es&mcc=4
        //60&mistyped=7&mnc=000&network_operator_name=CHINA+MOBILE&network_radio_type=1&pid=4719&rc=1&reason=&sim_mcc=515&sim_mnc=515&sim_operator_name=GLOBE&simnum=0&vname=IjN-yKrmVJ9L_GJC3up_c3bzaqYYWw3BnwV6Ib5W_7cWz4PyPxmSIg8vruHm4MpjmW
        //pAydSlsdJJ7efxtFyoCAs
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

    private CompletionStage<Void> onCodeRequestSent(String pushCode, VerificationCodeError lastError, HttpResponse<String> result) {
        if (result.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new RegistrationException(null, result.body());
        }

        var response = Json.readValue(result.body(), VerificationCodeResponse.class);
        if (response.status() == VerificationCodeStatus.SUCCESS) {
            return CompletableFuture.completedFuture(null);
        }

        return switch (response.errorReason()) {
            case TOO_RECENT, TOO_MANY, TOO_MANY_GUESSES, TOO_MANY_ALL_METHODS -> throw new RegistrationException(response, "Please wait before trying to register this phone number again");
            case NO_ROUTES, BLOCKED -> throw new RegistrationException(response, result.body());
            default -> {
                var newErrorReason = response.errorReason();
                Validate.isTrue(newErrorReason != lastError, () -> new RegistrationException(response, result.body()));
                yield requestVerificationCode(pushCode, newErrorReason);
            }
        };
    }

    public CompletableFuture<Void> sendVerificationCode() {
        return codeHandler.get()
                .thenComposeAsync(code -> getRegistrationOptions(store, keys, true, false, Map.entry("code", normalizeCodeResult(code))))
                .thenComposeAsync(attrs -> sendRequest("/register", attrs))
                .thenComposeAsync(result -> {
                    if (result.statusCode() != HttpURLConnection.HTTP_OK) {
                        throw new RegistrationException(null, result.body());
                    }

                    var response = Json.readValue(result.body(), VerificationCodeResponse.class);
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
        return httpClient.sendAsync(request, BodyHandlers.ofString());
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
            var sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, null, null);
            var sslParameters = sslContext.getDefaultSSLParameters();
            var supportedCiphers = Arrays.stream(sslContext.getDefaultSSLParameters().getCipherSuites())
                    .filter(entry -> ThreadLocalRandom.current().nextBoolean())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), result -> { Collections.shuffle(result); return result; }))
                    .toArray(String[]::new);
            sslParameters.setCipherSuites(supportedCiphers);
            var supportedNamedGroups = Arrays.stream(sslContext.getDefaultSSLParameters().getNamedGroups())
                    .filter(entry -> ThreadLocalRandom.current().nextBoolean())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), result -> { Collections.shuffle(result); return result; }))
                    .toArray(String[]::new);
            sslParameters.setNamedGroups(supportedNamedGroups);
            var version = HttpClient.Version.HTTP_1_1;
            var clientBuilder = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .sslParameters(sslParameters)
                    .version(version);
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
    private CompletableFuture<Map<String, Object>> getRegistrationOptions(Store store, Keys keys, boolean useToken, boolean isRetry, Entry<String, Object>... attributes) {
        var phoneNumber = store.phoneNumber()
                .orElseThrow(() -> new NoSuchElementException("Missing phone number"));
        var tokenFuture = !useToken ? CompletableFuture.completedFuture(null) : MobileMetadata.getToken(phoneNumber.numberWithoutPrefix(), store.device().platform(), store.version(), !isRetry);
        return tokenFuture.thenApplyAsync(token -> {
            var certificate = store.device().platform().isBusiness() ? MobileMetadata.generateBusinessCertificate(keys) : null;
            var requiredAttributes = Arrays.stream(attributes)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (first, second) -> first, LinkedHashMap::new));
            return Attributes.of()
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
        if(apnsClient != null) {
            apnsClient.close();
        }
    }
}
