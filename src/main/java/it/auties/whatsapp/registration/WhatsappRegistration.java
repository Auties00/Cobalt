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
import it.auties.whatsapp.model.response.CheckNumberResponse;
import it.auties.whatsapp.model.response.RegistrationResponse;
import it.auties.whatsapp.model.signal.auth.UserAgent;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.registration.apns.ApnsClient;
import it.auties.whatsapp.registration.apns.ApnsPacket;
import it.auties.whatsapp.registration.apns.ApnsPayloadTag;
import it.auties.whatsapp.registration.gcm.GcmClient;
import it.auties.whatsapp.registration.http.HttpClient;
import it.auties.whatsapp.registration.metadata.WhatsappMetadata;
import it.auties.whatsapp.util.*;
import it.auties.whatsapp.util.Specification.Whatsapp;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public final class WhatsappRegistration {
    private static final int MAX_REGISTRATION_RETRIES = 3;

    private final HttpClient httpClient;
    private final Store store;
    private final Keys keys;
    private final AsyncVerificationCodeSupplier codeHandler;
    private final VerificationCodeMethod method;
    private final ApnsClient apnsClient;
    private final GcmClient gcmClient;
    private final CountryCode countryCode;
    private final boolean printRequests;
    private volatile CompletableFuture<String> androidToken;

    public WhatsappRegistration(Store store, Keys keys, AsyncVerificationCodeSupplier codeHandler, VerificationCodeMethod method, boolean cloudMessagingVerification, boolean printRequests) {
        this.store = store;
        this.keys = keys;
        this.codeHandler = codeHandler;
        this.method = method;
        var ios = store.device().platform().isIOS();
        var android = store.device().platform().isAndroid();
        var requiresVerification = method != VerificationCodeMethod.NONE;
        var proxy = ProxyAuthenticator.getProxy(store.proxy().orElse(null));
        this.httpClient = new HttpClient(ios);
        this.apnsClient = ios && requiresVerification && cloudMessagingVerification ? new ApnsClient(httpClient, proxy) : null;
        this.gcmClient = android && requiresVerification && cloudMessagingVerification ? new GcmClient(httpClient, proxy) : null;
        this.countryCode = store.phoneNumber().orElseThrow().countryCode();
        this.printRequests = printRequests;
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
        if(originalDevice.platform().isIOS()) {
            store.setDevice(originalDevice.toPersonal());
        }
        var future = switch (store.device().platform()) {
            case IOS, IOS_BUSINESS -> onboard("1", 2155550000L, null)
                    .thenComposeAsync(response -> onboard(null, null, response.abHash()), CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS))
                    .thenComposeAsync(ignored -> exists(originalDevice, true, false, null))
                    .thenComposeAsync(response -> clientLog(response, Map.entry("current_screen", "verify_sms"), Map.entry("previous_screen", "enter_number"), Map.entry("action_taken", "continue")), CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS))
                    .thenComposeAsync(ignored -> getPushCode())
                    .thenComposeAsync(result -> requestVerificationCode(result, null));
            case ANDROID, ANDROID_BUSINESS ->
                    onboard("1", 2155550000L, null)
                            .thenComposeAsync(response -> onboard(null, null, response.abHash()), CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS))
                            .thenComposeAsync(ignored -> exists(null, true, false, null))
                            .thenComposeAsync(response -> clientLog(response, Map.entry("current_screen", "verify_sms"), Map.entry("previous_screen", "enter_number"), Map.entry("action_taken", "continue")), CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS))
                            .thenComposeAsync(ignored -> getPushCode())
                            .thenComposeAsync(pushCode -> requestVerificationCode(pushCode, null));
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

    public CompletableFuture<CheckNumberResponse> exists() {
        var originalDevice = store.device();
        store.setDevice(originalDevice.toBusiness());
        return exists(null, false, true, null)
                .thenApplyAsync(registrationResponse -> {
                    var hasWhatsapp = registrationResponse.whatsappOldEligible() || registrationResponse.possibleMigration();
                    var hasBan = registrationResponse.errorReason() == VerificationCodeError.BLOCKED;
                    return new CheckNumberResponse(hasWhatsapp, hasBan, registrationResponse);
                })
                .whenCompleteAsync((result, exception) -> {
                    store.setDevice(originalDevice);
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
        var uri = URI.create(Whatsapp.MOBILE_REGISTRATION_ENDPOINT + "/reg_onboard_abprop?" + HttpClient.toFormParams(attributes));
        var userAgent = store.device().toUserAgent(store.version());
        var headers = Map.of(
                "User-Agent", userAgent,
                "Content-Type", "application/x-www-form-urlencoded"
        );
        if(printRequests) {
            System.out.println("Using user agent " + userAgent);
            System.out.println("Sending GET request to /reg_onboard_abprop with parameters " + attributes);
        }
        return httpClient.get(uri, ProxyAuthenticator.getProxy(store.proxy().orElse(null)), headers).thenApplyAsync(response -> {
            if(printRequests) {
                System.out.println("Received respose /reg_onboard_abprop " + response);
            }
            return Json.readValue(response, AbPropsResponse.class);
        });
    }

    private CompletableFuture<String> getPushToken() {
        if (apnsClient != null) {
            return apnsClient.getAppToken(store.device().platform().isBusiness());
        }

        if(gcmClient != null) {
            return gcmClient.getPushToken();
        }

        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<RegistrationResponse> exists(CompanionDevice originalDevice, boolean throwError, boolean swapDevice, VerificationCodeError lastError) {
        return getPushToken().thenComposeAsync(pushToken -> {
            var requiresToken = store.device().platform().isAndroid();
            return getExistsParameters(pushToken).thenComposeAsync(existsParameters -> {
                var options = getRegistrationOptions(
                        store,
                        keys,
                        requiresToken,
                        existsParameters
                );
                return options.thenComposeAsync(attrs -> sendRequest("/exist", attrs)).thenComposeAsync(result -> {
                    var response = Json.readValue(result, RegistrationResponse.class);
                    var currentDevice = store.device();
                    if (response.errorReason() == VerificationCodeError.INCORRECT || !throwError) {
                        return CompletableFuture.completedFuture(response);
                    }

                    if (response.errorReason() == VerificationCodeError.BLOCKED || lastError != null) {
                        throw new RegistrationException(response, result);
                    }

                    var useOriginalDevice = originalDevice != null
                            && !Objects.equals(currentDevice, originalDevice)
                            && response.errorReason() == VerificationCodeError.FORMAT_WRONG;
                    if(useOriginalDevice) {
                        store.setDevice(originalDevice);
                    }

                    return exists(originalDevice, true, swapDevice, response.errorReason()).whenComplete((finalResult, error) -> {
                        if(useOriginalDevice && swapDevice) {
                            store.setDevice(currentDevice);
                        }

                        if(error != null) {
                            Exceptions.rethrow(error);
                        }
                    });
                });
            });
        });
    }

    private CompletableFuture<Entry<String, Object>[]> getExistsParameters(String pushToken) {
        var platform = store.device().platform();
        return switch (platform) {
            case ANDROID, ANDROID_BUSINESS -> getAndroidToken().thenApplyAsync(androidToken -> Attributes.of()
                    .put("gpia", androidToken, androidToken != null)
                    .put("read_phone_permission_granted", 0)
                    .put("offline_ab", Whatsapp.MOBILE_ANDROID_OFFLINE_AB)
                    .put("device_ram", "3.57")
                    .put("language_selector_clicked_count", 0)
                    .put("backup_token", convertBufferToUrlHex(keys.backupToken()))
                    .put("roaming_type", 0)
                    .put("backup_token_error", "success")
                    .put("feo2_query_status", "error_security_exception")
                    .put("sim_type", 0)
                    .put("network_radio_type", 1)
                    .put("network_operator_name", "")
                    .put("sim_operator_name", "")
                    .put("simnum", 0)
                    .put("db", 1)
                    .put("sim_state", 1)
                    .put("airplane_mode_type", 0)
                    .put("mistyped", 7)
                    .put("advertising_id", keys.advertisingId().toString().toLowerCase())
                    .put("hasinrc", 1)
                    .put("roaming_type", 0)
                    .put("client_metrics", Whatsapp.MOBILE_ANDROID_CLIENT_METRICS)
                    .put("pid", ProcessHandle.current().pid())
                    .put("cellular_strength", ThreadLocalRandom.current().nextInt(3, 6))
                    .put("recaptcha", "%7B%22stage%22%3A%22ABPROP_DISABLED%22%7D")
                    .put("device_name", "walleye")
                    .put("language_selector_time_spent", 0)
                    .put("push_token", pushToken == null ? "" : pushToken, pushToken != null)
                    .toEntries());
            case IOS, IOS_BUSINESS -> {
                var attributes = Attributes.of()
                        .put("offline_ab", platform.isBusiness() ? Whatsapp.MOBILE_BUSINESS_IOS_OFFLINE_AB : Whatsapp.MOBILE_IOS_OFFLINE_AB)
                        .put("push_token", pushToken == null ? "" : convertBufferToUrlHex(pushToken.getBytes(StandardCharsets.UTF_8)), pushToken != null)
                        .put("recovery_token_error", "-25300")
                        .toEntries();
                yield CompletableFuture.completedFuture(attributes);
            }
            case KAIOS -> CompletableFuture.completedFuture(Attributes.of().toEntries());
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
    }

    private CompletableFuture<String> getPushCode() {
        if (apnsClient != null) {
            return apnsClient.waitForPacket(packet -> packet.tag() == ApnsPayloadTag.NOTIFICATION)
                    .thenApply(this::readIOSPushCode)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionallyAsync(error -> {
                        if(error instanceof TimeoutException) {
                            throw new RegistrationException(null, "Apns timeout");
                        }

                        var exception = new RegistrationException(null, "Apns error");
                        exception.addSuppressed(error);
                        throw exception;
                    });
        }

        if(gcmClient != null) {
            return gcmClient.getPushCode()
                    .orTimeout(60, TimeUnit.SECONDS)
                    .exceptionallyAsync(error -> {
                        if(error instanceof TimeoutException) {
                            throw new RegistrationException(null, "Gcm timeout");
                        }

                        var exception = new RegistrationException(null, "Gcm error");
                        exception.addSuppressed(error);
                        throw exception;
                    });
        }

        return CompletableFuture.completedFuture(null);
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
            case ANDROID, ANDROID_BUSINESS -> getAndroidToken()
                    .thenApplyAsync(gpiaToken -> getAndroidRequestParameters(pushCode, gpiaToken, countryCode));
            case IOS, IOS_BUSINESS -> CompletableFuture.completedFuture(getIosRequestParameters(pushCode));
            case KAIOS -> CompletableFuture.completedFuture(getKaiOsRequestParameters(countryCode));
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
    }

    private CompletableFuture<String> getAndroidToken() {
        if(androidToken != null) {
            return androidToken;
        }

        var publicKey = keys.noiseKeyPair().publicKey();
        var business = store.device().platform().isBusiness();
        return androidToken = WhatsappMetadata.getGpiaToken(publicKey, business);
    }
    
    private Entry<String, Object>[] getKaiOsRequestParameters(CountryCode countryCode) {
        return Attributes.of()
                .put("mcc", countryCode.mcc())
                .put("mnc", "000")
                .put("method", method.data())
                .toEntries();
    }
    
    private Entry<String, Object>[] getIosRequestParameters(String pushCode) {
        return Attributes.of()
                .put("method", method.data())
                .put("sim_mcc", "000")
                .put("sim_mnc", "000")
                .put("reason", "")
                .put("push_code", convertBufferToUrlHex(pushCode.getBytes(StandardCharsets.UTF_8)))
                .put("cellular_strength", ThreadLocalRandom.current().nextInt(1, 5))
                .toEntries();
    }

    private Entry<String, Object>[] getAndroidRequestParameters(String pushCode, String gpiaToken, CountryCode countryCode) {
        return Attributes.of()
                .put("method", method.data())
                .put("sim_mcc", countryCode.mcc())
                .put("sim_mnc", "002")
                .put("reason", "")
                .put("mcc", countryCode.mcc())
                .put("mnc", "000")
                .put("feo2_query_status", "error_security_exception")
                .put("db", 1)
                .put("sim_type", 0)
                .put("recaptcha", "%7B%22stage%22%3A%22ABPROP_DISABLED%22%7D")
                .put("network_radio_type", 1)
                .put("prefer_sms_over_flash", true)
                .put("simnum", 0)
                .put("clicked_education_link", false)
                .put("airplane_mode_type", 0)
                .put("client_metrics", Whatsapp.MOBILE_ANDROID_CLIENT_METRICS)
                .put("mistyped", 7)
                .put("advertising_id", keys.advertisingId())
                .put("hasinrc", 1)
                .put("roaming_type", 0)
                .put("device_ram", "3.57")
                .put("education_screen_displayed", false)
                .put("pid", ProcessHandle.current().pid())
                .put("cellular_strength", ThreadLocalRandom.current().nextInt(3, 6))
                .put("gpia", gpiaToken, gpiaToken != null)
                .put("push_code", pushCode == null ? "" : convertBufferToUrlHex(pushCode.getBytes()), pushCode != null)
                .put("backup_token", convertBufferToUrlHex(keys.backupToken()))
                .put("hasav", 2)
                .toEntries();
    }

    private CompletionStage<RegistrationResponse> onCodeRequestSent(String pushCode, VerificationCodeError lastError, String result) {
        var response = Json.readValue(result, RegistrationResponse.class);
        if (response.status() == VerificationCodeStatus.SUCCESS) {
            return CompletableFuture.completedFuture(response);
        }

        return switch (response.errorReason()) {
            case TOO_RECENT, TOO_MANY, TOO_MANY_GUESSES, TOO_MANY_ALL_METHODS, NO_ROUTES, BLOCKED -> throw new RegistrationException(response, result);
            default -> {
                var newErrorReason = response.errorReason();
                Validate.isTrue(newErrorReason != lastError, () -> new RegistrationException(response, result));
                yield requestVerificationCode(pushCode, newErrorReason);
            }
        };
    }

    public CompletableFuture<RegistrationResponse> sendVerificationCode() {
        return sendVerificationCode(0);
    }

    private CompletableFuture<RegistrationResponse> sendVerificationCode(int retryIndex) {
        return logIosRegistration()
                .thenComposeAsync((ignored) -> codeHandler.get())
                .thenComposeAsync(code -> getRegistrationOptions(store, keys, true, Map.entry("code", normalizeCodeResult(code)), Map.entry("entered", "1")))
                .thenComposeAsync(attrs -> sendRequest("/register", attrs))
                .thenComposeAsync(result -> {
                    var response = Json.readValue(result, RegistrationResponse.class);
                    if (response.status() == VerificationCodeStatus.SUCCESS) {
                        saveRegistrationStatus(store, keys, true);
                        return CompletableFuture.completedFuture(response);
                    }

                    var retryTimes = retryIndex + 1;
                    if(response.errorReason() == VerificationCodeError.TEMPORARILY_UNAVAILABLE && retryTimes < MAX_REGISTRATION_RETRIES) {
                        randomRegistrationSleep();
                        return sendVerificationCode(retryTimes);
                    }

                    if(response.errorReason() == VerificationCodeError.SECURITY_CODE || response.errorReason() == VerificationCodeError.DEVICE_CONFIRM_OR_SECOND_CODE) {
                        return reset2fa(response);
                    }

                    throw new RegistrationException(response, result);
                });
    }

    private CompletableFuture<RegistrationResponse> reset2fa(RegistrationResponse registrationResponse) {
        var wipeToken = registrationResponse.wipeToken();
        if(wipeToken == null) {
            throw new RegistrationException(registrationResponse, "Missing wipe_token");
        }

        return getRegistrationOptions(store, keys, false, Map.entry("reset", "wipe"), Map.entry("wipe_token", wipeToken))
                .thenComposeAsync(attrs -> sendRequest("/security", attrs))
                .thenComposeAsync(result -> {
                    var response = Json.readValue(result, RegistrationResponse.class);
                    if (response.status() == VerificationCodeStatus.SUCCESS) {
                        saveRegistrationStatus(store, keys, true);
                        return CompletableFuture.completedFuture(response);
                    }

                    throw new RegistrationException(response, result);
                });
    }

    private void randomRegistrationSleep() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(3, 6) * 1000L);
        }catch (InterruptedException exception) {
            throw new RuntimeException("Cannot sleep", exception);
        }
    }

    private CompletableFuture<Object> logIosRegistration() {
        if(store.device().platform() != UserAgent.PlatformType.IOS_BUSINESS) {
            return CompletableFuture.completedFuture(null);
        }

        return clientLog(null, Map.entry("event_name", "smb_client_onboarding_journey"),
                Map.entry("is_logged_in_on_consumer_app", "0"),
                Map.entry("sequence_number", "14"),
                Map.entry("app_install_source", "unknown|unknown"),
                Map.entry("smb_onboarding_step", "20"),
                Map.entry("has_consumer_app", "1")
        );
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
        var proxy = ProxyAuthenticator.getProxy(store.proxy().orElse(null));
        var encodedParams = HttpClient.toFormParams(params).getBytes();
        var userAgent = store.device().toUserAgent(store.version());
        if(printRequests) {
            System.out.println("Using user agent " + userAgent);
        }
        var keypair = SignalKeyPair.random();
        var key = Curve25519.sharedKey(Whatsapp.REGISTRATION_PUBLIC_KEY, keypair.privateKey());
        var buffer = AesGcm.encrypt(new byte[12], encodedParams, key);
        var enc = Bytes.concat(keypair.publicKey(), buffer);
        var encBase64 = getBase64UrlEncoder().encodeToString(enc);
        return switch (store.device().platform()) {
            case IOS, IOS_BUSINESS -> {
                if(printRequests) {
                    System.out.println("Sending POST request to " + path + " with parameters " + Json.writeValueAsString(params, true));
                }
                var uri = URI.create("%s%s".formatted(Whatsapp.MOBILE_REGISTRATION_ENDPOINT, path));
                var headers = Attributes.of()
                        .put("User-Agent", userAgent)
                        .put("Content-Type", "application/x-www-form-urlencoded")
                        .toMap();
                var body = "ENC=%s".formatted(encBase64);
                yield httpClient.post(uri, proxy, headers, body.getBytes()).thenApplyAsync(result -> {
                    var resultAsString = new String(result);
                    if(printRequests) {
                        System.out.println("Received response " + path + " " + resultAsString);
                    }
                    return resultAsString;
                });
            }
            case ANDROID, ANDROID_BUSINESS -> WhatsappMetadata.getAndroidCert(keys.noiseKeyPair().publicKey(), enc, store.device().platform().isBusiness()).thenComposeAsync(androidCert -> {
                if(printRequests) {
                    System.out.println("Sending POST request to " + path + " with parameters " + Json.writeValueAsString(params, true));
                }
                var uri = URI.create("%s%s".formatted(
                        Whatsapp.MOBILE_REGISTRATION_ENDPOINT,
                        path
                ));
                var headers = Attributes.of()
                        .put("User-Agent", userAgent)
                        .put("WaMsysRequest", "1")
                        .put("Authorization", androidCert == null ? "" : androidCert.certificate(), androidCert != null)
                        .put("request_token", UUID.randomUUID().toString())
                        .put("Content-Type", "application/x-www-form-urlencoded")
                        .toMap();
                var body = "ENC=%s%s".formatted(
                        encBase64,
                        androidCert == null ? "" : "&H=" + androidCert.signature()
                );
                return httpClient.post(uri, proxy, headers, body.getBytes()).thenApplyAsync(result -> {
                    var resultAsString = new String(result);
                    if(printRequests) {
                        System.out.println("Received response " + path + " " + resultAsString);
                    }
                    return resultAsString;
                });
            });
            case KAIOS -> {
                if(printRequests) {
                    System.out.println("Sending GET request to " + path + " with parameters " + Json.writeValueAsString(params, true));
                }
                var uri = URI.create("%s%s?%s".formatted(Whatsapp.MOBILE_KAIOS_REGISTRATION_ENDPOINT, path, encodedParams));
                yield httpClient.get(uri, proxy, Map.of("User-Agent", userAgent)).thenApplyAsync(result -> {
                    if(printRequests) {
                        System.out.println("Received response " + path + " " + result);
                    }
                    return result;
                });
            }
            default -> throw new IllegalStateException("Unsupported mobile os");
        };
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
                    .put("authkey", getBase64UrlEncoder().encodeToString(keys.noiseKeyPair().publicKey()))
                    .put("vname", certificate, certificate != null)
                    .put("e_regid", getBase64UrlEncoder().encodeToString(keys.encodedRegistrationId()))
                    .put("e_keytype", getBase64UrlEncoder().encodeToString(Specification.Signal.KEY_BUNDLE_TYPE))
                    .put("e_ident", getBase64UrlEncoder().encodeToString(keys.identityKeyPair().publicKey()))
                    .put("e_skey_id", getBase64UrlEncoder().encodeToString(keys.signedKeyPair().encodedId()))
                    .put("e_skey_val", getBase64UrlEncoder().encodeToString(keys.signedKeyPair().publicKey()))
                    .put("e_skey_sig", getBase64UrlEncoder().encodeToString(keys.signedKeyPair().signature()))
                    .put("fdid", keys.fdid().toLowerCase(Locale.ROOT), store.device().platform().isAndroid())
                    .put("fdid", keys.fdid().toUpperCase(Locale.ROOT), store.device().platform().isIOS())
                    .put("expid", getBase64UrlEncoder().encodeToString(keys.deviceId()), !store.device().platform().isKaiOs())
                    .put("id", convertBufferToUrlHex(keys.identityId()))
                    .put("token", token, useToken)
                    .putAll(requiredAttributes)
                    .toMap();
        });
    }

    private Base64.Encoder getBase64UrlEncoder() {
        if(store.device().platform().isIOS()) {
            return Base64.getUrlEncoder();
        }

        return Base64.getUrlEncoder().withoutPadding();
    }

    private void dispose() {
        if (apnsClient != null) {
            apnsClient.close();
        }

        if(gcmClient != null) {
            gcmClient.close();
        }
    }
}
