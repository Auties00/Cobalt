package it.auties.whatsapp.socket;

import static java.lang.Long.parseLong;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.HistoryLength;
import it.auties.whatsapp.crypto.Handshake;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.signal.auth.ClientFinish;
import it.auties.whatsapp.model.signal.auth.ClientPayload;
import it.auties.whatsapp.model.signal.auth.Companion;
import it.auties.whatsapp.model.signal.auth.Companion.CompanionPropsPlatformType;
import it.auties.whatsapp.model.signal.auth.CompanionData;
import it.auties.whatsapp.model.signal.auth.HandshakeMessage;
import it.auties.whatsapp.model.signal.auth.UserAgent;
import it.auties.whatsapp.model.signal.auth.WebInfo;
import it.auties.whatsapp.model.signal.auth.WebInfo.WebInfoWebSubPlatform;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.Specification;
import jakarta.websocket.Session;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
class AuthHandler extends Handler
    implements JacksonProvider {

  private final SocketHandler socketHandler;
  private Handshake handshake;

  protected void createHandshake() {
    this.handshake = new Handshake(socketHandler.keys());
    handshake.updateHash(socketHandler.keys()
        .ephemeralKeyPair()
        .publicKey());
  }

  @SneakyThrows
  protected CompletableFuture<Void> login(Session session, byte[] message) {
    var serverHello = PROTOBUF.readMessage(message, HandshakeMessage.class)
        .serverHello();
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
    var encodedKey = handshake.cipher(socketHandler.keys()
        .noiseKeyPair()
        .publicKey(), true);
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

  @SneakyThrows
  private byte[] createUserPayload() {
    var builder = ClientPayload.builder()
        .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
        .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
        .userAgent(createUserAgent())
        .webInfo(new WebInfo(socketHandler.options().historyLength() == HistoryLength.ONE_YEAR
            ? WebInfoWebSubPlatform.WIN_STORE : WebInfo.WebInfoWebSubPlatform.WEB_BROWSER));
    return PROTOBUF.writeValueAsBytes(finishUserPayload(builder));
  }

  private ClientPayload finishUserPayload(ClientPayload.ClientPayloadBuilder builder) {
    if (socketHandler.store().userCompanionJid() != null) {
      return builder.username(parseLong(socketHandler.store()
              .userCompanionJid()
              .user()))
          .passive(true)
          .device(socketHandler.store()
              .userCompanionJid()
              .device())
          .build();
    }

    return builder.regData(createRegisterData())
        .passive(false)
        .build();
  }

  private UserAgent createUserAgent() {
    return UserAgent.builder()
        .appVersion(socketHandler.options()
            .version())
        .platform(UserAgent.UserAgentPlatform.WEB)
        .releaseChannel(UserAgent.UserAgentReleaseChannel.RELEASE)
        .build();
  }

  @SneakyThrows
  private CompanionData createRegisterData() {
    return CompanionData.builder()
        .buildHash(socketHandler.options()
            .version()
            .toHash())
        .companion(PROTOBUF.writeValueAsBytes(createCompanionProps()))
        .id(BytesHelper.intToBytes(socketHandler.keys()
            .id(), 4))
        .keyType(BytesHelper.intToBytes(Specification.Signal.KEY_TYPE, 1))
        .identifier(socketHandler.keys()
            .identityKeyPair()
            .publicKey())
        .signatureId(socketHandler.keys()
            .signedKeyPair()
            .encodedId())
        .signaturePublicKey(socketHandler.keys()
            .signedKeyPair()
            .keyPair()
            .publicKey())
        .signature(socketHandler.keys()
            .signedKeyPair()
            .signature())
        .build();
  }

  private Companion createCompanionProps() {
    return Companion.builder()
        .os(socketHandler.options().description())
        .platformType(socketHandler.options().historyLength() == HistoryLength.ONE_YEAR ? Companion.CompanionPropsPlatformType.DESKTOP : CompanionPropsPlatformType.CHROME)
        .requireFullSync(socketHandler.options().historyLength() == HistoryLength.ONE_YEAR)
        .build();
  }

  @Override
  protected void dispose() {
    super.dispose();
    handshake = null;
  }
}
