package it.auties.whatsapp.socket;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.serialization.performance.Protobuf;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.api.WebHistoryLength;
import it.auties.whatsapp.crypto.Handshake;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.model.signal.auth.ClientPayload.ClientPayloadBuilder;
import it.auties.whatsapp.model.signal.auth.Companion.CompanionPropsPlatformType;
import it.auties.whatsapp.model.signal.auth.DNSSource.DNSSourceDNSResolutionMethod;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.model.signal.auth.WebInfo.WebInfoWebSubPlatform;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Spec;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
class AuthHandler {
    private final SocketHandler socketHandler;
    private Handshake handshake;

    protected void createHandshake() {
        this.handshake = new Handshake(socketHandler.keys());
        handshake.updateHash(socketHandler.keys().ephemeralKeyPair().publicKey());
    }

    protected CompletableFuture<Void> loginSocket(SocketSession session, byte[] message) {
        try {
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
        }catch (Throwable throwable){
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private byte[] createUserPayload() {
        var builder = ClientPayload.builder()
                .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .userAgent(createUserAgent());
        if(socketHandler.store().clientType() == ClientType.WEB_CLIENT){
            builder.webInfo(new WebInfo(getWebPlatform()));
        }

        var result = finishUserPayload(builder);
        return Protobuf.writeMessage(result);
    }

    private UserAgent createUserAgent() {
        var mobile = socketHandler.store().clientType() == ClientType.APP_CLIENT;
        return UserAgent.builder()
                .appVersion(socketHandler.store().version())
                .osVersion(Spec.Whatsapp.MOBILE_OS_VERSION)
                .device(Spec.Whatsapp.MOBILE_DEVICE)
                .manufacturer(Spec.Whatsapp.MOBILE_DEVICE_MANUFACTURER)
                .platform(mobile ? Spec.Whatsapp.MOBILE_OS_TYPE : UserAgentPlatform.WEB)
                .releaseChannel(UserAgent.UserAgentReleaseChannel.RELEASE)
                .phoneId(mobile ? socketHandler.keys().phoneId() : null)
                .build();
    }

    private WebInfoWebSubPlatform getWebPlatform() {
        if (socketHandler.store().clientType() != ClientType.WEB_CLIENT) {
            return null;
        }

        if (socketHandler.store().historyLength() == WebHistoryLength.ONE_YEAR) {
            return WebInfoWebSubPlatform.WIN_STORE;
        }

        return WebInfoWebSubPlatform.WEB_BROWSER;
    }

    private ClientPayload finishUserPayload(ClientPayloadBuilder builder) {
        if(socketHandler.store().clientType() == ClientType.APP_CLIENT){
            var phoneNumber = socketHandler.store().phoneNumber();
            return builder.sessionId(socketHandler.keys().registrationId())
                    .shortConnect(true)
                    .connectAttemptCount(0)
                    .device(0)
                    .dnsSource(getDnsSource())
                    .passive(false)
                    .pushName(socketHandler.store().name())
                    .username(Long.parseLong(phoneNumber.toJid().user()))
                    .build();
        }

        if (socketHandler.store().jid() != null) {
            return builder.username(Long.parseLong(socketHandler.store().jid().user()))
                    .passive(true)
                    .device(socketHandler.store().jid().device())
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
                .buildHash(socketHandler.store().version().toHash())
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
    }

    private Companion createCompanionProps() {
        if (socketHandler.store().clientType() != ClientType.WEB_CLIENT) {
            return null;
        }

        return Companion.builder()
                .os(socketHandler.store().name())
                .platformType(socketHandler.store().historyLength() == WebHistoryLength.ONE_YEAR ? CompanionPropsPlatformType.DESKTOP : CompanionPropsPlatformType.CHROME)
                .requireFullSync(socketHandler.store().historyLength() == WebHistoryLength.ONE_YEAR)
                .build();
    }

    protected void dispose() {
        handshake = null;
    }
}
