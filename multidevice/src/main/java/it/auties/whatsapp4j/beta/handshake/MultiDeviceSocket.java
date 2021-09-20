package it.auties.whatsapp4j.beta.handshake;

import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp4j.beta.binary.MultiDeviceMessage;
import it.auties.whatsapp4j.beta.manager.MultiDeviceKeysManager;
import it.auties.whatsapp4j.beta.utils.MultiDeviceCypher;
import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.common.protobuf.info.WebInfo;
import it.auties.whatsapp4j.common.protobuf.message.server.HandshakeMessage;
import it.auties.whatsapp4j.common.protobuf.model.app.AppVersion;
import it.auties.whatsapp4j.common.protobuf.model.client.*;
import it.auties.whatsapp4j.common.protobuf.model.companion.CompanionProps;
import it.auties.whatsapp4j.common.protobuf.model.companion.CompanionRegData;
import it.auties.whatsapp4j.common.socket.IWhatsappSocket;
import it.auties.whatsapp4j.common.socket.WhatsappSocketConfiguration;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import jakarta.websocket.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Base64;

@RequiredArgsConstructor
@ClientEndpoint(configurator = WhatsappSocketConfiguration.class)
@Log
public class MultiDeviceSocket implements IWhatsappSocket {
    private NoiseHandshake handshake;
    private final @NonNull WebSocketContainer webSocketContainer;
    private final @NonNull WhatsappConfiguration options;
    private final @NonNull MultiDeviceKeysManager whatsappKeys;
    public MultiDeviceSocket(@NonNull WhatsappConfiguration options, @NonNull MultiDeviceKeysManager whatsappKeys) {
        this(ContainerProvider.getWebSocketContainer(), options, whatsappKeys);
    }

    @OnOpen
    @SneakyThrows
    public void onOpen(@NonNull Session session) {
        this.handshake = new NoiseHandshake();
        handshake.start();
        var publicKey = CypherUtils.parseKey(whatsappKeys.ephemeralKeyPair().getPublic());
        handshake.updateHash(publicKey);
        var handshakeMessage = new HandshakeMessage(new ClientHello(publicKey));
        var protobufMessage = MultiDeviceCypher.encryptMessage(ProtobufEncoder.encode(handshakeMessage));
        session.getBasicRemote().sendBinary(protobufMessage.toBuffer());
    }

    @OnMessage
    @SneakyThrows
    public void onBinary(@NonNull Session session, byte @NonNull [] msg) {
        var multiDeviceMessage = new MultiDeviceMessage(BinaryArray.forArray(msg));
        if(msg.length != multiDeviceMessage.length() + 3){
            log.warning("Invalid length: " + multiDeviceMessage.length());
            return;
        }

        log.info("Length: " + msg.length);
        var serverHello = ProtobufDecoder.forType(HandshakeMessage.class)
                .decode(multiDeviceMessage.decoded().data())
                .serverHello();

        handshake.updateHash(serverHello.ephemeral());
        var sharedEphemeral = CypherUtils.calculateSharedSecret(serverHello.ephemeral(), whatsappKeys.ephemeralKeyPair().getPrivate());
        handshake.mixIntoKey(sharedEphemeral.data());

        var decodedStaticText = handshake.decrypt(serverHello.staticText());
        var sharedStatic = CypherUtils.calculateSharedSecret(decodedStaticText.data(), whatsappKeys.ephemeralKeyPair().getPrivate());
        handshake.mixIntoKey(sharedStatic.data());

        var decodedPayload = handshake.decrypt(serverHello.payload());
        ProtobufDecoder.forType(NoiseCertificate.class).decode(decodedPayload.data());

        var encodedKey = handshake.encrypt(CypherUtils.parseKey(whatsappKeys.keyPair().getPublic()));
        var sharedKey = CypherUtils.calculateSharedSecret(serverHello.ephemeral(), whatsappKeys.keyPair().getPrivate());
        handshake.mixIntoKey(sharedKey.data());

        var payload = createPayload();
        var encodedPayload = handshake.encrypt(payload);

        var protobufMessage = new HandshakeMessage(new ClientFinish(encodedKey.data(), encodedPayload.data()));
        var encodedProto = ProtobufEncoder.encode(protobufMessage);
        session.getBasicRemote().sendBinary(ByteBuffer.wrap(encodedProto));

        handshake.finish();
    }

    private byte[] createPayload() throws IOException, IllegalAccessException {
        var companion = CompanionProps.builder()
                .os("Windows")
                .version(new AppVersion(10))
                .platformType(CompanionProps.CompanionPropsPlatformType.DESKTOP)
                .requireFullSync(false)
                .build();

        var data = CompanionRegData.builder()
                .buildHash(Base64.getDecoder().decode("S9Kdc4pc4EJryo21snc5cg=="))
                .companionProps(ProtobufEncoder.encode(companion))
                .eRegid(BinaryArray.forInt(whatsappKeys.registrationId(), 4).data())
                .eKeytype(BinaryArray.forInt(5, 1).data())
                .eIdent(CypherUtils.parseKey(whatsappKeys.ephemeralKeyPair().getPublic()))
                .eSkeyId(BinaryArray.forInt(1, 3).data())
                .eSkeyVal(whatsappKeys.signedPreKey().publicKey())
                .eSkeySig(whatsappKeys.signedPreKey().signature())
                .build();

        var userAgent = UserAgent.builder()
                .platform(UserAgent.UserAgentPlatform.WEB)
                .mcc("000")
                .mnc("000")
                .osVersion("92")
                .manufacturer("Google")
                .device("Google Chrome")
                .osBuildNumber("92.0.4515.107")
                .localeLanguageIso6391("it")
                .localeCountryIso31661Alpha2("IT")
                .appVersion(new AppVersion(0, 0, 14, 2126, 2))
                .build();

        var payload = ClientPayload.builder()
                .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .passive(false)
                .regData(data)
                .userAgent(userAgent)
                .webInfo(new WebInfo(WebInfo.WebInfoWebSubPlatform.WEB_BROWSER))
                .build();

        return ProtobufEncoder.encode(payload);
    }

    @OnError
    public void onError(@NonNull Throwable throwable){
        throwable.printStackTrace();
    }

    @Override
    public void connect() {
        try{
            webSocketContainer.connectToServer(this, URI.create(options.whatsappUrlBeta()));
        }catch (IOException | DeploymentException exception){
            throw new RuntimeException("Cannot connect to WhatsappWeb's WebServer", exception);
        }
    }

    @Override
    public @NonNull WhatsappKeysManager keys() {
        return whatsappKeys;
    }
}
