package it.auties.whatsapp.socket;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.base.ProtobufDeserializationException;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.api.WebHistoryLength;
import it.auties.whatsapp.crypto.Handshake;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.model.signal.auth.ClientPayload.ClientPayloadBuilder;
import it.auties.whatsapp.model.signal.auth.Companion.CompanionPropsPlatformType;
import it.auties.whatsapp.model.signal.auth.DNSSource.DNSSourceDNSResolutionMethod;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Protobuf;
import it.auties.whatsapp.util.Spec;
import lombok.RequiredArgsConstructor;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
class AuthHandler {
    private final SocketHandler socketHandler;

    protected CompletableFuture<Boolean> login(SocketSession session, byte[] message) {
        try {
            var serverHello = readHandshake(message);
            if(serverHello.isEmpty()){
                return CompletableFuture.completedFuture(false);
            }

            var handshake = new Handshake(socketHandler.keys());
            handshake.updateHash(socketHandler.keys().ephemeralKeyPair().publicKey());
            handshake.updateHash(serverHello.get().ephemeral());
            var sharedEphemeral = Curve25519.sharedKey(serverHello.get().ephemeral(), socketHandler.keys()
                    .ephemeralKeyPair()
                    .privateKey());
            handshake.mixIntoKey(sharedEphemeral);
            var decodedStaticText = handshake.cipher(serverHello.get().staticText(), false);
            var sharedStatic = Curve25519.sharedKey(decodedStaticText, socketHandler.keys()
                    .ephemeralKeyPair()
                    .privateKey());
            handshake.mixIntoKey(sharedStatic);
            handshake.cipher(serverHello.get().payload(), false);
            var encodedKey = handshake.cipher(socketHandler.keys().noiseKeyPair().publicKey(), true);
            var sharedPrivate = Curve25519.sharedKey(serverHello.get().ephemeral(), socketHandler.keys()
                    .noiseKeyPair()
                    .privateKey());
            handshake.mixIntoKey(sharedPrivate);
            return createUserPayload()
                    .thenApplyAsync(userPayload -> createHandshakeMessage(handshake, encodedKey, userPayload))
                    .thenComposeAsync(result -> sendHandshake(session, handshake, result));
        }catch (Throwable throwable){
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private Optional<ServerHello> readHandshake(byte[] message) {
        try {
            var handshakeMessage = Protobuf.readMessage(message, HandshakeMessage.class);
            return Optional.ofNullable(handshakeMessage.serverHello());
        }catch (ProtobufDeserializationException exception){
            return Optional.empty();
        }
    }

    private CompletableFuture<Boolean> sendHandshake(SocketSession session, Handshake handshake, HandshakeMessage handshakeMessage) {
        return Request.of(handshakeMessage)
                .sendWithNoResponse(session, socketHandler.keys(), socketHandler.store())
                .thenApplyAsync(result -> onHandshakeSent(handshake));
    }

    private boolean onHandshakeSent(Handshake handshake) {
        socketHandler.keys().clearReadWriteKey();
        handshake.finish();
        return true;
    }

    private HandshakeMessage createHandshakeMessage(Handshake handshake, byte[] encodedKey, byte[] userPayload) {
        var encodedPayload = handshake.cipher(userPayload, true);
        var clientFinish = new ClientFinish(encodedKey, encodedPayload);
        return new HandshakeMessage(clientFinish);
    }

    private CompletableFuture<byte[]> createUserPayload() {
        return createUserAgent()
                .thenComposeAsync(this::encodeUserPayload);
    }

    private CompletableFuture<byte[]> encodeUserPayload(UserAgent userAgent) {
        var builder = createUserPayloadBuilder(userAgent);
        return finishUserPayload(builder)
                .thenApplyAsync(Protobuf::writeMessage);
    }

    public ClientPayloadBuilder createUserPayloadBuilder(UserAgent userAgent){
        return ClientPayload.builder()
                .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .userAgent(userAgent);
    }

    private CompletableFuture<UserAgent> createUserAgent() {
        return socketHandler.store()
                .version()
                .thenApplyAsync(version -> createUserAgent(version, socketHandler.store().clientType() == ClientType.APP_CLIENT));
    }

    private UserAgent createUserAgent(Version version, boolean mobile) {
        return UserAgent.builder()
                .appVersion(version)
                .osVersion(mobile ? socketHandler.store().osVersion() : null)
                .device(mobile ? socketHandler.store().model() : null)
                .manufacturer(mobile ? socketHandler.store().manufacturer() : null)
                .phoneId(mobile ? socketHandler.keys().phoneId() : null)
                .platform(getPlatform())
                .releaseChannel(socketHandler.store().releaseChannel())
                .mcc("000")
                .mnc("000")
                .build();
    }

    private UserAgentPlatform getPlatform() {
        if(!socketHandler.store().business()){
            return socketHandler.store().os();
        }

        return switch (socketHandler.store().os()){
            case ANDROID -> UserAgentPlatform.SMB_ANDROID;
            case IOS -> UserAgentPlatform.SMB_IOS;
            default -> throw new IllegalStateException("Unexpected platform: " + socketHandler.store().os());
        };
    }

    private CompletableFuture<ClientPayload> finishUserPayload(ClientPayloadBuilder builder) {
        if(socketHandler.store().clientType() == ClientType.APP_CLIENT){
            var phoneNumber = socketHandler.store()
                    .phoneNumber()
                    .map(PhoneNumber::number)
                    .orElseThrow(() -> new NoSuchElementException("Missing phone number for mobile registration"));
            var result = builder.sessionId(socketHandler.keys().registrationId())
                    .shortConnect(true)
                    .connectAttemptCount(0)
                    .device(0)
                    .dnsSource(getDnsSource())
                    .passive(false)
                    .pushName(socketHandler.store().name())
                    .username(phoneNumber)
                    .build();
            return CompletableFuture.completedFuture(result);
        }

        if (socketHandler.store().jid() != null) {
            var result = builder.username(Long.parseLong(socketHandler.store().jid().user()))
                    .passive(true)
                    .device(socketHandler.store().jid().device())
                    .build();
            return CompletableFuture.completedFuture(result);
        }

        return createRegisterData()
                .thenApplyAsync(data -> builder.regData(data).passive(false).build());
    }

    private DNSSource getDnsSource() {
        return DNSSource.builder()
                .appCached(false)
                .dnsMethod(DNSSourceDNSResolutionMethod.SYSTEM)
                .build();
    }

    private CompletableFuture<CompanionData> createRegisterData() {
        return socketHandler.store().version().thenApplyAsync(version -> {
            var companion = CompanionData.builder()
                    .buildHash(version.toHash())
                    .id(socketHandler.keys().encodedRegistrationId())
                    .keyType(BytesHelper.intToBytes(Spec.Signal.KEY_TYPE, 1))
                    .identifier(socketHandler.keys().identityKeyPair().publicKey())
                    .signatureId(socketHandler.keys().signedKeyPair().encodedId())
                    .signaturePublicKey(socketHandler.keys().signedKeyPair().keyPair().publicKey())
                    .signature(socketHandler.keys().signedKeyPair().signature());
            if (socketHandler.store().clientType() == ClientType.WEB_CLIENT) {
                var props = Protobuf.writeMessage(createCompanionProps());
                companion.companion(props);
            }

            return companion.build();
        });
    }

    private Companion createCompanionProps() {
        if (socketHandler.store().clientType() != ClientType.WEB_CLIENT) {
            return null;
        }

        return Companion.builder()
                .os(socketHandler.store().name())
                .platformType(socketHandler.store().historyLength() == WebHistoryLength.EXTENDED ? CompanionPropsPlatformType.DESKTOP : CompanionPropsPlatformType.CHROME)
                .requireFullSync(socketHandler.store().historyLength() == WebHistoryLength.EXTENDED)
                .build();
    }
}
