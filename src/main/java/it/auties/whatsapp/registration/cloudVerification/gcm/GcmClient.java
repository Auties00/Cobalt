package it.auties.whatsapp.registration.cloudVerification.gcm;

import it.auties.whatsapp.crypto.HttpEce;
import it.auties.whatsapp.net.HttpClient;
import it.auties.whatsapp.net.SocketClient;
import it.auties.whatsapp.registration.cloudVerification.CloudVerificationClient;
import it.auties.whatsapp.registration.cloudVerification.gcm.McsExchange.AppData;
import it.auties.whatsapp.registration.cloudVerification.gcm.McsExchange.DataMessageStanza;
import it.auties.whatsapp.registration.cloudVerification.gcm.McsExchange.LoginRequest.AuthService;
import it.auties.whatsapp.registration.cloudVerification.gcm.McsExchange.LoginResponse;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Json;
import it.auties.whatsapp.util.Validate;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GcmClient implements CloudVerificationClient  {
    private static final long DEFAULT_GCM_SENDER_ID = 293955441834L;
    private static final int AUTH_SECRET_LENGTH = 16;
    private static final String CHROME_VERSION = "63.0.3234.0";
    private static final URI CHECK_IN_URL = URI.create("https://android.clients.google.com/checkin");
    private static final String FCM_SERVER_KEY = "BDOU99-h67HcA6JeFXHbSNMu7e2yNNu3RzoMj8TM4W88jITfq7ZmPvIM1Iv-4_l2LxQcYwhqby2xGpWwzjfAnG4";
    private static final String FCM_ENDPOINT = "https://fcm.googleapis.com/fcm/send/";
    private static final URI REGISTER_URL = URI.create("https://android.clients.google.com/c2dm/register3");
    private static final URI FCM_SUBSCRIBE_URL = URI.create("https://fcm.googleapis.com/fcm/connect/subscribe");
    private static final String APP_ID = "wp:receiver.push.com#";
    private static final String TALK_SERVER_HOST = "mtalk.google.com";
    private static final int TALK_SERVER_PORT = 443;
    private static final String MCS_DOMAIN = "mcs.android.com";
    private static final byte[] FCM_VERSION = {41};

    private final HttpClient httpClient;
    private final URI proxy;
    private final long senderId;
    private final ECDH256KeyPair keyPair;
    private final byte[] authSecret;
    private final String appId;
    private final CompletableFuture<String> loginFuture;
    private final CompletableFuture<String> dataFuture;
    private SocketClient socket;
    private long androidId;
    private long securityToken;
    private String token;
    public GcmClient(HttpClient httpClient, URI proxy) {
        this.httpClient = httpClient;
        this.proxy = proxy;
        this.senderId = DEFAULT_GCM_SENDER_ID;
        this.keyPair = ECDH256KeyPair.random();
        this.authSecret = Bytes.random(AUTH_SECRET_LENGTH);
        this.appId = APP_ID + UUID.randomUUID();
        this.loginFuture = new CompletableFuture<>();
        this.dataFuture = new CompletableFuture<>();
        login();
    }

    private void login() {
        checkIn().thenComposeAsync(this::register)
                .thenComposeAsync(this::subscribe)
                .thenComposeAsync(ignored -> openConnection())
                .exceptionallyAsync(this::handleLoginError);
    }

    private Void handleLoginError(Throwable error) {
        loginFuture.completeExceptionally(error);
        return null;
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
        var headers = Map.of(
                "Content-Type", "application/x-protobuf"
        );
        return httpClient.postRaw(CHECK_IN_URL, headers, AndroidCheckInRequestSpec.encode(checkInRequest))
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
        var headers = Map.of(
                "Content-Type", "application/x-www-form-urlencoded",
                "Authorization", "AidLogin %s:%s".formatted(checkInResponse.androidId(), checkInResponse.securityToken())
        );
        return httpClient.postRaw(REGISTER_URL, headers, HttpClient.toFormParams(params).getBytes())
                .thenApplyAsync(this::handleRegistration);
    }

    private String handleRegistration(byte[] registrationResponse) {
        var body = new String(registrationResponse);
        var data = HttpClient.parseFormParams(body);
        var token = data.get("token");
        if(token == null) {
            throw new IllegalArgumentException("Invalid registration response: " + body);
        }

        return token;
    }

    private CompletableFuture<Void> subscribe(String gcmToken) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        var params = Map.of(
                "authorized_entity", senderId,
                "endpoint", URLEncoder.encode(FCM_ENDPOINT + gcmToken, StandardCharsets.UTF_8),
                "encryption_key", encoder.encodeToString(keyPair.publicKey()),
                "encryption_auth", encoder.encodeToString(authSecret)
        );
        var userAgent = Map.of(
                "Content-Type", "application/x-www-form-urlencoded",
                "Accept", "application/json",
                "Accept-Encoding", "gzip, deflate"
        );
        return httpClient.postRaw(FCM_SUBSCRIBE_URL, userAgent, HttpClient.toFormParams(params).getBytes())
                .thenAcceptAsync(this::handleSubscription);
    }

    private void handleSubscription(byte[] subscriptionResponse) {
        var fcmRegistrationResponse = Json.readValue(subscriptionResponse, FcmRegistrationResponse.class);
        this.token = fcmRegistrationResponse.token();
    }

    private CompletableFuture<Void> openConnection() {
        try {
            var sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, null, null);
            var sslEngine = sslContext.createSSLEngine(TALK_SERVER_HOST, TALK_SERVER_PORT);
            sslEngine.setUseClientMode(true);
            this.socket = SocketClient.newSecureClient(sslEngine, proxy);
            return socket.connectAsync(proxy == null ? new InetSocketAddress(TALK_SERVER_HOST, TALK_SERVER_PORT) : InetSocketAddress.createUnresolved(TALK_SERVER_HOST, TALK_SERVER_PORT))
                    .thenRunAsync(this::onLoggedIn);
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private void onLoggedIn() {
        socket.readFullyAsync(1).thenAcceptAsync(versionBuffer -> {
            var version = versionBuffer.get();
            Validate.isTrue(version == FCM_VERSION[0],
                    "Versions mismatch, expected %s got %s", FCM_VERSION[0], version);
            readMessages();
            sendLoginPacket();
        });
    }

    private void readMessages() {
        socket.readFullyAsync(1).thenAcceptAsync(tagBuffer -> {
            var tag = tagBuffer.get();
            socket.readFullyAsync(10).thenAcceptAsync(lengthBuffer -> {
                var messageLength = getMessageLength(lengthBuffer);
                socket.readFullyAsync(messageLength - lengthBuffer.remaining()).thenAcceptAsync(messageBuffer -> {
                    var message = new byte[messageLength];
                    lengthBuffer.get(messageLength);
                    messageBuffer.get(message);
                    var payload = McsExchange.readMessage(tag, message);
                    payload.ifPresent(this::handleMessage);
                });
            });
        });
    }

    private int getMessageLength(ByteBuffer lengthBuffer) {
        byte varIntByte;
        var result = new StringBuilder();
        do {
            varIntByte = lengthBuffer.get();
            result.insert(0, getVarIntValue(varIntByte));
        }while ((varIntByte & 0x80) != 0);
        return Integer.parseUnsignedInt(result.toString(), 2);
    }

    private String getVarIntValue(byte number) {
        var result = Integer.toBinaryString(Byte.toUnsignedInt(number));
        return ("0".repeat(8 - result.length()) + result).substring(1);
    }

    private void handleMessage(McsExchange payload) {
        switch (payload) {
            case LoginResponse ignored -> onLogin();
            case DataMessageStanza dataMessageStanza -> onStanza(dataMessageStanza);
            default -> {}
        }
    }

    private void onLogin() {
        loginFuture.complete(token);
    }

    private void onStanza(DataMessageStanza dataMessageStanza) {
        try {
            var dataMap = dataMessageStanza.appData()
                    .stream()
                    .filter(entry -> entry.value() != null)
                    .collect(Collectors.toUnmodifiableMap(AppData::key, AppData::value));
            var salt = Base64.getUrlDecoder().decode(dataMap.get("encryption").substring(5));
            var dh = Base64.getUrlDecoder().decode(dataMap.get("crypto-key").substring(3));
            var deciphered = HttpEce.decrypt(
                    dataMessageStanza.rawData(),
                    dh,
                    keyPair.publicKey(),
                    keyPair.jcaPrivateKey(),
                    salt,
                    authSecret
            );
            var cleanText = Arrays.copyOfRange(deciphered, 2, deciphered.length);
            var whatsappResponse = Json.readValue(cleanText, GcmWhatsappResponse.class);
            dataFuture.complete(whatsappResponse.data().pushCode());
        }catch (Throwable throwable) {
            dataFuture.completeExceptionally(throwable);
        }
    }

    private void sendLoginPacket() {
        var newVc = new McsExchangeSettingBuilder()
                .name("new_vc")
                .value("1")
                .build();
        var request = new McsExchangeLoginRequestBuilder()
                .accountId(1000000L)
                .authService(AuthService.ANDROID_ID)
                .authToken(String.valueOf(securityToken))
                .id("chrome-" + CHROME_VERSION)
                .domain(MCS_DOMAIN)
                .deviceId("android-" + Long.toHexString(androidId))
                .networkType(1)
                .resource(String.valueOf(androidId))
                .user(String.valueOf(androidId))
                .useRmq2(true)
                .lastRmqId(1L)
                .setting(List.of(newVc))
                .adaptiveHeartbeat(false)
                .build();
        var message = McsExchangeLoginRequestSpec.encode(request);
        var requestSize = Bytes.intToVarInt(message.length);
        var data = Bytes.concat(FCM_VERSION, new byte[]{McsExchange.TAG_LOGIN_REQUEST}, requestSize, message);
        socket.writeAsync(data);
    }

    @Override
    public CompletableFuture<String> getPushToken(boolean business) {
        return loginFuture;
    }

    @Override
    public CompletableFuture<String> getPushCode() {
        return dataFuture;
    }

    @Override
    public void close() {
        try {
            if(!loginFuture.isDone()) {
                loginFuture.completeExceptionally(new RuntimeException("Closed"));
            }

            if(!dataFuture.isDone()) {
                dataFuture.completeExceptionally(new RuntimeException("Closed"));
            }

            if(socket != null) {
                socket.close();
            }


            // Do not close the http client
        }catch (IOException exception) {
            // Ignored
        }
    }
}
