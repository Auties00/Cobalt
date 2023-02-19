package it.auties.whatsapp.socket;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.api.HistoryLength;
import it.auties.whatsapp.api.WhatsappOptions.WebOptions;
import it.auties.whatsapp.crypto.Handshake;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.model.signal.auth.Companion.CompanionPropsPlatformType;
import it.auties.whatsapp.model.signal.auth.WebInfo.WebInfoWebSubPlatform;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.Specification;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static java.lang.Long.parseLong;

@RequiredArgsConstructor
class AuthHandler extends Handler implements JacksonProvider {
    private final SocketHandler socketHandler;
    private Handshake handshake;

    protected void createHandshake() {
        this.handshake = new Handshake(socketHandler.keys());
        handshake.updateHash(socketHandler.keys().ephemeralKeyPair().publicKey());
    }

    @SneakyThrows
    protected CompletableFuture<Void> login(SocketSession session, byte[] message) {
        var serverHello = PROTOBUF.readMessage(message, HandshakeMessage.class).serverHello();
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
        try {
            var builder = ClientPayload.builder()
                    .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                    .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                    .userAgent(createUserAgent())
                    .webInfo(new WebInfo(getWebPlatform()));
            return PROTOBUF.writeValueAsBytes(finishUserPayload(builder));
        } catch (IOException exception) {
            throw new RuntimeException("Cannot create user payload", exception);
        }
    }

    private UserAgent createUserAgent() {
        if (socketHandler.options().clientType() != ClientType.WEB_CLIENT) {
            return null;
        }

        var options = (WebOptions) socketHandler.options();
        return UserAgent.builder()
                .appVersion(options.version())
                .platform(UserAgent.UserAgentPlatform.WEB)
                .releaseChannel(UserAgent.UserAgentReleaseChannel.RELEASE)
                .build();
    }

    private WebInfoWebSubPlatform getWebPlatform() {
        if (socketHandler.options().clientType() != ClientType.WEB_CLIENT) {
            return null;
        }

        var options = (WebOptions) socketHandler.options();
        return options.historyLength() == HistoryLength.ONE_YEAR ? WebInfoWebSubPlatform.WIN_STORE : WebInfoWebSubPlatform.WEB_BROWSER;
    }

    private ClientPayload finishUserPayload(ClientPayload.ClientPayloadBuilder builder) {
        if (socketHandler.store().userCompanionJid() != null) {
            return builder.username(parseLong(socketHandler.store().userCompanionJid().user()))
                    .passive(true)
                    .device(socketHandler.store().userCompanionJid().device())
                    .build();
        }
        return builder.regData(createRegisterData()).passive(false).build();
    }

    @SneakyThrows
    private CompanionData createRegisterData() {
        var companion = CompanionData.builder()
                .buildHash(socketHandler.options().version().toHash())
                .id(socketHandler.keys().encodedId())
                .keyType(BytesHelper.intToBytes(Specification.Signal.KEY_TYPE, 1))
                .identifier(socketHandler.keys().identityKeyPair().publicKey())
                .signatureId(socketHandler.keys().signedKeyPair().encodedId())
                .signaturePublicKey(socketHandler.keys().signedKeyPair().keyPair().publicKey())
                .signature(socketHandler.keys().signedKeyPair().signature());
        if (socketHandler.options().clientType() == ClientType.WEB_CLIENT) {
            var props = PROTOBUF.writeValueAsBytes(createCompanionProps());
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

    @Override
    protected void dispose() {
        super.dispose();
        handshake = null;
    }
}
