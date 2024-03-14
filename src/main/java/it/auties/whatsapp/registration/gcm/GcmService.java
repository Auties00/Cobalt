package it.auties.whatsapp.registration.gcm;

import it.auties.whatsapp.registration.http.HttpClient;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Json;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GcmService {
    private static final int AUTH_SECRET_LENGTH = 16;
    private static final String CHROME_VERSION = "63.0.3234.0";
    private static final URI CHECK_IN_URL = URI.create("https://android.clients.google.com/checkin");
    private static final String FCM_SERVER_KEY = "BDOU99-h67HcA6JeFXHbSNMu7e2yNNu3RzoMj8TM4W88jITfq7ZmPvIM1Iv-4_l2LxQcYwhqby2xGpWwzjfAnG4";
    private static final String FCM_ENDPOINT = "https://fcm.googleapis.com/fcm/send/";
    private static final URI REGISTER_URL = URI.create("https://android.clients.google.com/c2dm/register3");
    private static final URI FCM_SUBSCRIBE_URL = URI.create("https://fcm.googleapis.com/fcm/connect/subscribe");
    private static final String APP_ID = "wp:receiver.push.com#";
    private static final String TALK_SERVER_HOST = "mtalk.google.com";
    private static final int TALK_SERVER_PORT = 5228;

    private final HttpClient httpClient;
    private final long senderId;
    private final ECDH256KeyPair keyPair;
    private final byte[] authSecret;
    private final String appId;
    private CompletableFuture<Void> loginFuture;
    private long androidId;
    private long securityToken;
    private String token;
    public GcmService(long senderId) {
        this.httpClient = new HttpClient();
        this.senderId = senderId;
        this.keyPair = ECDH256KeyPair.random();
        this.authSecret = Bytes.random(AUTH_SECRET_LENGTH);
        this.appId = APP_ID + UUID.randomUUID();
        this.loginFuture = checkIn()
                .thenComposeAsync(this::register)
                .thenComposeAsync(this::subscribe);
    }

    public void await() {
        loginFuture.join();
        System.out.println(token);
    }

    private CompletableFuture<AndroidCheckInResponse> checkIn() {
        var chromeBuild = new ChromeBuildBuilder()
                .platform(ChromeBuild.Platform.PLATFORM_LINUX)
                .chromeVersion(CHROME_VERSION)
                .channel(ChromeBuild.Channel.CHANNEL_STABLE)
                .build();
        var checkInData = new AndroidCheckInDataBuilder()
                .chromeBuild(chromeBuild)
                .type(AndroidCheckInData.DeviceType.DEVICE_CHROME_BROWSER)
                .userNumber(0)
                .build();
        var checkInRequest = new AndroidCheckInRequestBuilder()
                .data(checkInData)
                .fragment(0)
                .version(3)
                .userSerialNumber(0)
                .build();
        return httpClient.post(CHECK_IN_URL, Map.of("Content-Type", "application/x-protobuf"), AndroidCheckInRequestSpec.encode(checkInRequest))
                .thenApplyAsync(AndroidCheckInResponseSpec::decode);
    }

    private CompletableFuture<String> register(AndroidCheckInResponse checkInResponse) {
        this.androidId = checkInResponse.androidId();
        this.securityToken = checkInResponse.securityToken();
        var params = Map.of(
                "app", "org.chromium.linux",
                "X-subtype", URLEncoder.encode(appId, StandardCharsets.UTF_8),
                "device", checkInResponse.androidId(),
                "sender", FCM_SERVER_KEY
        );
        var formParams = HttpClient.toFormParams(params).getBytes();
        var headers = Map.of(
                "Content-Type", "application/x-www-form-urlencoded",
                "Authorization", "AidLogin %s:%s".formatted(checkInResponse.androidId(), checkInResponse.securityToken())
        );
        return httpClient.post(REGISTER_URL, headers, formParams)
                .thenApplyAsync(this::handleRegistration);
    }

    private String handleRegistration(byte[] registrationResponse) {
        var body = new String(registrationResponse);
        var data = Arrays.stream(body.split("&"))
                .map(entry -> entry.split("=", 2))
                .collect(Collectors.toUnmodifiableMap(entry -> entry[0], entry -> entry[1]));
        return Objects.requireNonNull(data.get("token"), "Missing token");
    }

    private CompletableFuture<Void> subscribe(String gcmToken) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        var params = Map.of(
                "authorized_entity", senderId,
                "endpoint", URLEncoder.encode(FCM_ENDPOINT + gcmToken, StandardCharsets.UTF_8),
                "encryption_key", encoder.encodeToString(keyPair.publicKey()),
                "encryption_auth", encoder.encodeToString(authSecret)
        );
        var formParams = HttpClient.toFormParams(params).getBytes();
        return httpClient.post(FCM_SUBSCRIBE_URL, Map.of("Content-Type", "application/x-www-form-urlencoded"), formParams)
                .thenAcceptAsync(this::handleSubscription);
    }

    private void handleSubscription(byte[] subscriptionResponse) {
        var fcmRegistrationResponse = Json.readValue(subscriptionResponse, FcmRegistrationResponse.class);
        this.token = fcmRegistrationResponse.token();
    }
}
