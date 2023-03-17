package it.auties.whatsapp.socket;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.serialization.performance.Protobuf;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.api.HistoryLength;
import it.auties.whatsapp.api.WhatsappOptions.MobileOptions;
import it.auties.whatsapp.api.WhatsappOptions.WebOptions;
import it.auties.whatsapp.crypto.Handshake;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import it.auties.whatsapp.model.request.Attributes;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.model.signal.auth.ClientPayload.ClientPayloadBuilder;
import it.auties.whatsapp.model.signal.auth.Companion.CompanionPropsPlatformType;
import it.auties.whatsapp.model.signal.auth.DNSSource.DNSSourceDNSResolutionMethod;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.model.signal.auth.WebInfo.WebInfoWebSubPlatform;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Json;
import it.auties.whatsapp.util.Spec;
import it.auties.whatsapp.util.Spec.Whatsapp;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HexFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static java.util.Base64.getUrlEncoder;
import static java.util.Map.entry;

@RequiredArgsConstructor
class AuthHandler {
    private final SocketHandler socketHandler;
    private Handshake handshake;

    protected void createHandshake() {
        this.handshake = new Handshake(socketHandler.keys());
        handshake.updateHash(socketHandler.keys().ephemeralKeyPair().publicKey());
    }

    protected CompletableFuture<Void> loginSocket(SocketSession session, byte[] message) {
        if(handshake == null){
            createHandshake();
        }
        var serverHello = Protobuf.readMessage(message, HandshakeMessage.class).serverHello();
        handshake.updateHash(serverHello.ephemeral());
        var sharedEphemeral = Curve25519.sharedKey(serverHello.ephemeral(), socketHandler.keys()
                .ephemeralKeyPair()
                .privateKey());
        handshake.mixIntoKey(sharedEphemeral);
        var decodedStaticText = handshake.cipher(serverHello.staticText(), false);
        var sharedStatic = Curve25519.sharedKey(decodedStaticText, socketHandler.keys()
                .ephemeralKeyPair()
                .privateKey());
        handshake.mixIntoKey(sharedStatic);
        handshake.cipher(serverHello.payload(), false);
        var encodedKey = handshake.cipher(socketHandler.keys().noiseKeyPair().publicKey(), true);
        var sharedPrivate = Curve25519.sharedKey(serverHello.ephemeral(), socketHandler.keys()
                .noiseKeyPair()
                .privateKey());
        handshake.mixIntoKey(sharedPrivate);
        var encodedPayload = handshake.cipher(createUserPayload(), true);
        var clientFinish = new ClientFinish(encodedKey, encodedPayload);
        var handshakeMessage = new HandshakeMessage(clientFinish);
        return Request.of(handshakeMessage)
                .sendWithNoResponse(session, socketHandler.keys(), socketHandler.store())
                .thenRunAsync(socketHandler.keys()::clearReadWriteKey)
                .thenRunAsync(handshake::finish);
    }

    private byte[] createUserPayload() {
        var builder = ClientPayload.builder()
                .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .userAgent(createUserAgent());
        if(socketHandler.options().clientType() == ClientType.WEB_CLIENT){
            builder.webInfo(new WebInfo(getWebPlatform()));
        }

        var result = finishUserPayload(builder);
        return Protobuf.writeMessage(result);
    }

    private UserAgent createUserAgent() {
        var mobile = socketHandler.options().clientType() == ClientType.APP_CLIENT;
        return UserAgent.builder()
                .appVersion(socketHandler.options().version())
                .osVersion(socketHandler.options().osVersion())
                .device(socketHandler.options().deviceName())
                .manufacturer(socketHandler.options().deviceManufacturer())
                .platform(mobile ? UserAgentPlatform.ANDROID : UserAgent.UserAgentPlatform.WEB)
                .releaseChannel(UserAgent.UserAgentReleaseChannel.RELEASE)
                .phoneId(mobile ? socketHandler.keys().phoneId() : null)
                .build();
    }

    private WebInfoWebSubPlatform getWebPlatform() {
        if (socketHandler.options().clientType() != ClientType.WEB_CLIENT) {
            return null;
        }

        var options = (WebOptions) socketHandler.options();
        return options.historyLength() == HistoryLength.ONE_YEAR ? WebInfoWebSubPlatform.WIN_STORE : WebInfoWebSubPlatform.WEB_BROWSER;
    }

    private ClientPayload finishUserPayload(ClientPayloadBuilder builder) {
        if(socketHandler.options() instanceof MobileOptions options){
            var phoneNumber = PhoneNumber.of(options.phoneNumber());
            return builder.sessionId(socketHandler.store().id())
                    .shortConnect(true)
                    .connectAttemptCount(0)
                    .device(0)
                    .dnsSource(getDnsSource())
                    .passive(false)
                    .pushName("test")
                    .username(parseLong(phoneNumber.toJid().user()))
                    .build();
        }

        if (socketHandler.store().userCompanionJid() != null) {
            return builder.username(parseLong(socketHandler.store().userCompanionJid().user()))
                    .passive(true)
                    .device(socketHandler.store().userCompanionJid().device())
                    .build();
        }

        return builder.regData(createRegisterData())
                .passive(false)
                .build();
    }

    private DNSSource getDnsSource() {
        return DNSSource.builder()
                .appCached(false)
                .dnsMethod(DNSSourceDNSResolutionMethod.SYSTEM)
                .build();
    }

    private CompanionData createRegisterData() {
        var companion = CompanionData.builder()
                .buildHash(socketHandler.options().version().toHash())
                .id(socketHandler.keys().encodedId())
                .keyType(BytesHelper.intToBytes(Spec.Signal.KEY_TYPE, 1))
                .identifier(socketHandler.keys().identityKeyPair().publicKey())
                .signatureId(socketHandler.keys().signedKeyPair().encodedId())
                .signaturePublicKey(socketHandler.keys().signedKeyPair().keyPair().publicKey())
                .signature(socketHandler.keys().signedKeyPair().signature());
        if (socketHandler.options().clientType() == ClientType.WEB_CLIENT) {
            var props = Protobuf.writeMessage(createCompanionProps());
            companion.companion(props);
        }

        return companion.build();
    }

    private Companion createCompanionProps() {
        if (socketHandler.options().clientType() != ClientType.WEB_CLIENT) {
            return null;
        }

        var options = (WebOptions) socketHandler.options();
        return Companion.builder()
                .os(options.name())
                .platformType(getWebBrowser(options))
                .requireFullSync(options.historyLength() == HistoryLength.ONE_YEAR)
                .build();
    }

    private CompanionPropsPlatformType getWebBrowser(WebOptions webOptions) {
        return webOptions.historyLength() == HistoryLength.ONE_YEAR ? CompanionPropsPlatformType.DESKTOP : CompanionPropsPlatformType.CHROME;
    }

    protected void checkRegistrationStatus(){
        var options = (MobileOptions) socketHandler.options();
        if (options.verificationCodeMethod() != VerificationCodeMethod.NONE) {
            registerPhoneNumber();
            return;
        }

        verifyPhoneNumber(options.verificationCodeHandler().apply(null));
    }

    private void registerPhoneNumber() {
        var options = (MobileOptions) socketHandler.options();
        var phoneNumber = PhoneNumber.of(options.phoneNumber());
        var userAgent = createUserAgent(options);
        var response = askForVerificationCode(phoneNumber, userAgent, options.verificationCodeMethod());
        var code = options.verificationCodeHandler().apply(response);
        verifyPhoneNumber(code);
    }

    private void verifyPhoneNumber(@NonNull String code) {
        var options = (MobileOptions) socketHandler.options();
        var phoneNumber = PhoneNumber.of(options.phoneNumber());
        var userAgent = createUserAgent(options);
        sendVerificationCode(phoneNumber, userAgent, code);
        socketHandler.store().userCompanionJid(ContactJid.of(options.phoneNumber()));
        socketHandler.keys().registered(true);
    }

    private String createUserAgent(MobileOptions options) {
        return "WhatsApp/%s %s/%s Device/%s-%s".formatted(options.version(), options.osName(), options.osVersion(), options.deviceManufacturer(), options.deviceName());
    }

    private void sendVerificationCode(PhoneNumber phoneNumber, String userAgent, String code) {
        var registerOptions = getRegistrationOptions(phoneNumber, entry("code", code.replaceAll("-", "")));
        var codeResponse = sendRegistrationRequest(userAgent,"/register", registerOptions);
        var phoneNumberResponse = Json.readValue(codeResponse.body(), VerificationCodeResponse.class);
        Validate.isTrue(phoneNumberResponse.status()
                .isSuccessful(), "Unexpected response: %s", phoneNumberResponse);
    }

    private VerificationCodeResponse askForVerificationCode(PhoneNumber phoneNumber, String userAgent, VerificationCodeMethod method) {
        var codeOptions = getRegistrationOptions(phoneNumber, entry("mcc", phoneNumber.countryCode()
                .mcc()), entry("mnc", phoneNumber.countryCode()
                .mnc()), entry("sim_mcc", "000"), entry("sim_mnc", "000"), entry("method", method.type()), entry("reason", ""), entry("hasav", "1"));
        var codeResponse = sendRegistrationRequest(userAgent, "/code", codeOptions);
        var phoneNumberResponse = Json.readValue(codeResponse.body(), VerificationCodeResponse.class);
        Validate.isTrue(phoneNumberResponse.status()
                .isSuccessful(), "Unexpected response: %s", phoneNumberResponse);
        return phoneNumberResponse;
    }

    private HttpResponse<String> sendRegistrationRequest(String userAgent, String path, Map<String, Object> params) {
        try {
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("%s%s?%s".formatted(Whatsapp.MOBILE_REGISTRATION_ENDPOINT, path, toFormParams(params))))
                    .GET()
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", userAgent)
                    .build();
            System.out.println("Sending request to: " + request.uri());
            var result = client.send(request, BodyHandlers.ofString());
            System.out.println("Received: " + result.body());
            return result;
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException("Cannot get verification code", exception);
        }
    }

    @SafeVarargs
    private Map<String, Object> getRegistrationOptions(PhoneNumber phoneNumber, Entry<String, Object>... attributes) {
        var keys = socketHandler.keys();
        return Attributes.of(attributes)
                .put("cc", phoneNumber.countryCode().prefix())
                .put("in", phoneNumber.number())
                .put("lg", "en")
                .put("lc", "GB")
                .put("mistyped", "6")
                .put("authkey", getUrlEncoder().encodeToString(keys.noiseKeyPair().publicKey()))
                .put("e_regid", getUrlEncoder().encodeToString(keys.encodedId()))
                .put("e_keytype", "BQ")
                .put("e_ident", getUrlEncoder().encodeToString(keys.identityKeyPair().publicKey()))
                .put("e_skey_id", getUrlEncoder().encodeToString(keys.signedKeyPair().encodedId()))
                .put("e_skey_val", getUrlEncoder().encodeToString(keys.signedKeyPair().publicKey()))
                .put("e_skey_sig", getUrlEncoder().encodeToString(keys.signedKeyPair().signature()))
                .put("fdid", keys.phoneId())
                .put("expid", keys.deviceId())
                .put("network_radio_type", "1")
                .put("simnum", "1")
                .put("hasinrc", "1")
                .put("pid", ThreadLocalRandom.current().nextInt(1000))
                .put("rc", "0")
                .put("id", keys.identityId())
                .put("token", HexFormat.of().formatHex(MD5.calculate(Whatsapp.MOBILE_TOKEN + phoneNumber.number())))
                .toMap();
    }

    private String toFormParams(Map<String, Object> values) {
        return values.entrySet()
                .stream()
                .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    protected void dispose() {
        handshake = null;
    }
}
