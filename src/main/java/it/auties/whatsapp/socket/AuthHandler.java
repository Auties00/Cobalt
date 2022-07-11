package it.auties.whatsapp.socket;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.HistoryLength;
import it.auties.whatsapp.crypto.Handshake;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.SignalSpecification;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.concurrent.CompletableFuture;

import static java.lang.Long.parseLong;

@RequiredArgsConstructor
class AuthHandler implements JacksonProvider {
    private final Socket socket;
    private Handshake handshake;
    private CompletableFuture<Void> future;

    protected void createHandshake(){
        this.handshake = new Handshake(socket.keys());
        handshake.updateHash(socket.keys().ephemeralKeyPair().publicKey());
    }
    
    @SneakyThrows
    protected CompletableFuture<Void> login(Session session, byte[] message) {
        var serverHello = PROTOBUF.readMessage(message, HandshakeMessage.class)
                .serverHello();
        handshake.updateHash(serverHello.ephemeral());
        var sharedEphemeral = Curve25519.sharedKey(serverHello.ephemeral(), socket.keys().ephemeralKeyPair()
                .privateKey());
        handshake.mixIntoKey(sharedEphemeral);

        var decodedStaticText = handshake.cipher(serverHello.staticText(), false);
        var sharedStatic = Curve25519.sharedKey(decodedStaticText, socket.keys().ephemeralKeyPair()
                .privateKey());
        handshake.mixIntoKey(sharedStatic);
        handshake.cipher(serverHello.payload(), false);

        var encodedKey = handshake.cipher(socket.keys().noiseKeyPair()
                .publicKey(), true);
        var sharedPrivate = Curve25519.sharedKey(serverHello.ephemeral(), socket.keys().noiseKeyPair()
                .privateKey());
        handshake.mixIntoKey(sharedPrivate);

        var encodedPayload = handshake.cipher(createUserPayload(), true);
        var clientFinish = new ClientFinish(encodedKey, encodedPayload);
        var handshakeMessage = new HandshakeMessage(clientFinish);
        return Request.with(handshakeMessage)
                .sendWithNoResponse(session, socket.keys(), socket.store())
                .thenRunAsync(socket.keys()::clear)
                .thenRunAsync(handshake::finish);
    }

    @SneakyThrows
    private byte[] createUserPayload() {
        var builder = ClientPayload.builder()
                .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .userAgent(createUserAgent())
                .passive(true)
                .webInfo(new WebInfo(WebInfo.WebInfoWebSubPlatform.WEB_BROWSER));
        return PROTOBUF.writeValueAsBytes(finishUserPayload(builder));
    }

    private ClientPayload finishUserPayload(ClientPayload.ClientPayloadBuilder builder) {
        if (socket.keys().hasCompanion()) {
            return builder.username(parseLong(socket.keys().companion()
                            .user()))
                    .device(socket.keys().companion()
                            .device())
                    .build();
        }

        return builder.regData(createRegisterData())
                .build();
    }

    private UserAgent createUserAgent() {
        return UserAgent.builder()
                .appVersion(socket.options().version())
                .platform(UserAgent.UserAgentPlatform.WEB)
                .releaseChannel(UserAgent.UserAgentReleaseChannel.RELEASE)
                .build();
    }

    @SneakyThrows
    private CompanionData createRegisterData() {
        return CompanionData.builder()
                .buildHash(socket.options().version()
                        .toHash())
                .companion(PROTOBUF.writeValueAsBytes(createCompanionProps()))
                .id(BytesHelper.intToBytes(socket.keys().id(), 4))
                .keyType(BytesHelper.intToBytes(SignalSpecification.KEY_TYPE, 1))
                .identifier(socket.keys().identityKeyPair()
                        .publicKey())
                .signatureId(socket.keys().signedKeyPair()
                        .encodedId())
                .signaturePublicKey(socket.keys().signedKeyPair()
                        .keyPair()
                        .publicKey())
                .signature(socket.keys().signedKeyPair()
                        .signature())
                .build();
    }

    private Companion createCompanionProps() {
        return Companion.builder()
                .os(socket.options().description())
                .platformType(Companion.CompanionPropsPlatformType.DESKTOP)
                .requireFullSync(socket.options().historyLength() == HistoryLength.ONE_YEAR)
                .build();
    }

    protected void createFuture(){
        this.future = new CompletableFuture<>();
    }

    protected CompletableFuture<Void> future() {
        return future;
    }
}
