package it.auties.whatsapp.socket;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.exception.ProtobufDeserializationException;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.api.WebHistoryLength;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.model.signal.auth.CompanionProperties.CompanionPropsPlatformType;
import it.auties.whatsapp.model.signal.auth.DNSSource.DNSSourceDNSResolutionMethod;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Spec;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


class AuthHandler {
    private final SocketHandler socketHandler;
    AuthHandler(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    protected CompletableFuture<Boolean> login(SocketSession session, byte[] message) {
        try {
            var serverHello = readHandshake(message);
            if(serverHello.isEmpty()){
                return CompletableFuture.completedFuture(false);
            }

            var handshake = new SocketHandshake(socketHandler.keys());
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
            var handshakeMessage = createHandshakeMessage(handshake, encodedKey, encodeUserPayload());
            return sendHandshake(session, handshake, handshakeMessage);
        }catch (Throwable throwable){
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private Optional<ServerHello> readHandshake(byte[] message) {
        try {
            var handshakeMessage = HandshakeMessageSpec.decode(message);
            return Optional.ofNullable(handshakeMessage.serverHello());
        }catch (ProtobufDeserializationException exception){
            return Optional.empty();
        }
    }

    private CompletableFuture<Boolean> sendHandshake(SocketSession session, SocketHandshake handshake, HandshakeMessage handshakeMessage) {
        return Request.of(HandshakeMessageSpec.encode(handshakeMessage))
                .sendWithNoResponse(session, socketHandler.keys(), socketHandler.store())
                .thenApplyAsync(result -> onHandshakeSent(handshake));
    }

    private boolean onHandshakeSent(SocketHandshake handshake) {
        socketHandler.keys().clearReadWriteKey();
        handshake.finish();
        return true;
    }

    private HandshakeMessage createHandshakeMessage(SocketHandshake handshake, byte[] encodedKey, byte[] userPayload) {
        var encodedPayload = handshake.cipher(userPayload, true);
        var clientFinish = new ClientFinish(encodedKey, encodedPayload);
        return new HandshakeMessageBuilder()
                .clientFinish(clientFinish)
                .build();
    }

    private byte[] encodeUserPayload() {
        var userAgent = createUserAgent();
        var builder = createUserPayloadBuilder(userAgent);
        return ClientPayloadSpec.encode(finishUserPayload(builder));
    }

    public ClientPayloadBuilder createUserPayloadBuilder(UserAgent userAgent){
        return new ClientPayloadBuilder()
                .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .userAgent(userAgent);
    }

    private UserAgent createUserAgent() {
        var mobile = socketHandler.store().clientType() == ClientType.MOBILE;
        return new UserAgentBuilder()
                .appVersion(socketHandler.store().version())
                .osVersion(mobile ? socketHandler.store().device().osVersion().toString() : null)
                .device(mobile ? socketHandler.store().device().model() : null)
                .manufacturer(mobile ? socketHandler.store().device().manufacturer() : null)
                .phoneId(mobile ? socketHandler.keys().phoneId() : null)
                .platform(getPlatform())
                .releaseChannel(socketHandler.store().releaseChannel())
                .mcc("000")
                .mnc("000")
                .build();
    }

    private UserAgentPlatform getPlatform() {
        if(!socketHandler.store().business()){
            return socketHandler.store().device().osType();
        }

        return switch (socketHandler.store().device().osType()){
            case ANDROID -> UserAgentPlatform.SMB_ANDROID;
            case IOS -> UserAgentPlatform.SMB_IOS;
            default -> throw new IllegalStateException("Unexpected platform: " + socketHandler.store().device().osType());
        };
    }

    private ClientPayload finishUserPayload(ClientPayloadBuilder builder) {
        return switch (socketHandler.store().clientType()){
            case MOBILE -> {
                var phoneNumber = socketHandler.store()
                        .phoneNumber()
                        .map(PhoneNumber::number)
                        .orElseThrow(() -> new NoSuchElementException("Missing phone number for mobile registration"));
                yield builder.sessionId(socketHandler.keys().registrationId())
                        .shortConnect(true)
                        .connectAttemptCount(0)
                        .device(0)
                        .dnsSource(getDnsSource())
                        .passive(false)
                        .pushName(socketHandler.store().name())
                        .username(phoneNumber)
                        .build();
            }
            case WEB -> {
                var jid = socketHandler.store().jid();
                if (jid.isPresent()) {
                    yield builder.username(Long.parseLong(jid.get().user()))
                            .passive(true)
                            .device(jid.get().device())
                            .build();
                }

                yield builder.regData(createRegisterData())
                        .passive(false)
                        .build();
            }
        };
    }

    private DNSSource getDnsSource() {
        return new DNSSourceBuilder()
                .appCached(false)
                .dnsMethod(DNSSourceDNSResolutionMethod.SYSTEM)
                .build();
    }

    private CompanionRegistrationData createRegisterData() {
        var companion = new CompanionRegistrationDataBuilder()
                .buildHash(socketHandler.store().version().toHash())
                .eRegid(socketHandler.keys().encodedRegistrationId())
                .eKeytype(BytesHelper.intToBytes(Spec.Signal.KEY_TYPE, 1))
                .eIdent(socketHandler.keys().identityKeyPair().publicKey())
                .eSkeyId(socketHandler.keys().signedKeyPair().encodedId())
                .eSkeyVal(socketHandler.keys().signedKeyPair().keyPair().publicKey())
                .eSkeySig(socketHandler.keys().signedKeyPair().signature());
        if (socketHandler.store().clientType() == ClientType.WEB) {
            var props = createCompanionProps();
            var encodedProps = props == null ? null : CompanionPropertiesSpec.encode(props);
            companion.companionProps(encodedProps);
        }

        return companion.build();
    }

    private CompanionProperties createCompanionProps() {
        if (socketHandler.store().clientType() != ClientType.WEB) {
            return null;
        }

        return new CompanionPropertiesBuilder()
                .os(socketHandler.store().name())
                .platformType(socketHandler.store().historyLength() == WebHistoryLength.EXTENDED ? CompanionPropsPlatformType.DESKTOP : CompanionPropsPlatformType.CHROME)
                .requireFullSync(socketHandler.store().historyLength() == WebHistoryLength.EXTENDED)
                .build();
    }
}
