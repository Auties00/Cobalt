package it.auties.whatsapp4j.beta.handshake;

import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp4j.beta.binary.MultiDeviceMessage;
import it.auties.whatsapp4j.beta.manager.MultiDeviceKeysManager;
import it.auties.whatsapp4j.beta.serialization.StanzaEncoder;
import it.auties.whatsapp4j.beta.utils.MultiDeviceCypher;
import it.auties.whatsapp4j.beta.utils.Jid;
import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.protobuf.info.WebInfo;
import it.auties.whatsapp4j.common.protobuf.message.server.HandshakeMessage;
import it.auties.whatsapp4j.common.protobuf.model.app.AppVersion;
import it.auties.whatsapp4j.common.protobuf.model.client.*;
import it.auties.whatsapp4j.common.protobuf.model.companion.CompanionProps;
import it.auties.whatsapp4j.common.protobuf.model.companion.CompanionRegData;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import it.auties.whatsapp4j.common.socket.IWhatsappSocket;
import it.auties.whatsapp4j.common.socket.WhatsappSocketConfiguration;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import jakarta.websocket.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.URI;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Data
@Accessors(fluent = true)
@ClientEndpoint(configurator = WhatsappSocketConfiguration.class)
@Log
public class MultiDeviceSocket implements IWhatsappSocket {
    private @Getter(onMethod = @__(@NonNull)) Session session;
    private boolean loggedIn;
    private final NoiseHandshake handshake;
    private final @NonNull WebSocketContainer webSocketContainer;
    private final @NonNull WhatsappConfiguration options;
    private final @NonNull MultiDeviceKeysManager keys;
    private final @NonNull StanzaEncoder stanzaEncoder;
    private long counter;
    public MultiDeviceSocket(@NonNull WhatsappConfiguration options) {
        this(new NoiseHandshake(), ContainerProvider.getWebSocketContainer(), options, new MultiDeviceKeysManager(), new StanzaEncoder());
    }

    @OnOpen
    @SneakyThrows
    public void onOpen(@NonNull Session session) {
        session(session);
        handshake.start(keys());
        var publicKey = CypherUtils.raw(keys.ephemeralKeyPair().getPublic());
        handshake.updateHash(publicKey);
        var handshakeMessage = new HandshakeMessage(new ClientHello(publicKey));
        sendBinaryRequest(ProtobufEncoder.encode(handshakeMessage));
    }

    @OnMessage
    @SneakyThrows
    public void onBinary(byte @NonNull [] msg) {
        var multiDeviceMessage = new MultiDeviceMessage(BinaryArray.forArray(msg));
        if(msg.length != multiDeviceMessage.length() + 3){
            log.warning("Invalid message: %s".formatted(multiDeviceMessage.raw().toHex()));
            return;
        }

        log.info("Length: " + msg.length);
        switch ((int) counter){
            case 1 -> authenticate(multiDeviceMessage);
            case 2 -> {
                var unpacked = stanzaEncoder.unpackStanza(multiDeviceMessage.decoded().data());
                var decoded = stanzaEncoder.decodeStanza(unpacked);
                System.out.println(decoded);
            }
            case 3 -> createKeepAlive();
        }
    }

    private void createKeepAlive() {
        var keepAlive = new Node("iq", Map.of("id", stanzaEncoder.generateId(), "to", Jid.WHATSAPP_SERVER,  "type", "get", "xmlns", "w:p"), new Node("ping"));
        var result = stanzaEncoder.encodeStanza(keepAlive);
        System.out.printf("Creating keep alive: %s%n", HexFormat.of().formatHex(result));
        sendBinaryRequest(result);
    }

    private void authenticate(MultiDeviceMessage multiDeviceMessage) throws IOException, IllegalAccessException {
        var serverHello = ProtobufDecoder.forType(HandshakeMessage.class)
                .decode(multiDeviceMessage.decoded().data())
                .serverHello();

        handshake.updateHash(serverHello.ephemeral());
        var sharedEphemeral = CypherUtils.calculateSharedSecret(serverHello.ephemeral(), keys.ephemeralKeyPair().getPrivate());
        handshake.mixIntoKey(sharedEphemeral.data());

        var decodedStaticText = handshake.cypher(serverHello.staticText(), false);
        var sharedStatic = CypherUtils.calculateSharedSecret(decodedStaticText, keys.ephemeralKeyPair().getPrivate());
        handshake.mixIntoKey(sharedStatic.data());

        var decodedPayload = handshake.cypher(serverHello.payload(), false);
        ProtobufDecoder.forType(NoiseCertificate.class).decode(decodedPayload);

        var encodedKey = handshake.cypher(CypherUtils.raw(keys.keyPair().getPublic()), true);
        var sharedPrivate = CypherUtils.calculateSharedSecret(serverHello.ephemeral(), keys.keyPair().getPrivate());
        handshake.mixIntoKey(sharedPrivate.data());

        var payload = createPayload();
        var encodedPayload = handshake.cypher(payload, true);
        var protobufMessage = new HandshakeMessage(new ClientFinish(encodedKey, encodedPayload));
        sendBinaryRequest(ProtobufEncoder.encode(protobufMessage));
        handshake.finish();
    }

    private byte[] createPayload() throws IOException, IllegalAccessException {
        var appVersionBuf = BinaryArray.forBase64("S9Kdc4pc4EJryo21snc5cg==");
        var companion = CompanionProps.builder()
                .os("Windows")
                .version(new AppVersion(10))
                .platformType(CompanionProps.CompanionPropsPlatformType.CHROME)
                .requireFullSync(false)
                .build();
        var companionProto = ProtobufEncoder.encode(companion);
        var registerData = CompanionRegData.builder()
                .buildHash(appVersionBuf.data())
                .companionProps(companionProto)
                .eRegid(BinaryArray.forInt(keys().registrationId(), 4).data())
                .eRegid(BinaryArray.forInt(5, 1).data())
                .eIdent(CypherUtils.raw(keys().signedIdentityKey().getPublic()))
                .eSkeyId(BinaryArray.forInt(1, 3).data())
                .eSkeyVal(keys().signedPreKey().publicKey())
                .eSkeySig(keys().signedPreKey().signature())
                .build();
        var userAgent = UserAgent.builder()
                .appVersion(new AppVersion(0, 0, 14, 2126, 2))
                .platform(UserAgent.UserAgentPlatform.WEB)
                .releaseChannel(UserAgent.UserAgentReleaseChannel.RELEASE)
                .mcc("000")
                .mnc("000")
                .osVersion("0.1")
                .manufacturer("")
                .device("Desktop")
                .osBuildNumber("0.1")
                .localeLanguageIso6391("en")
                .localeCountryIso31661Alpha2("en")
                .build();
        var registerPayload = ClientPayload.builder()
                .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .passive(false)
                .regData(registerData)
                .userAgent(userAgent)
                .webInfo(new WebInfo(WebInfo.WebInfoWebSubPlatform.WEB_BROWSER))
                .build();
        return ProtobufEncoder.encode(registerPayload);
    }

    @SneakyThrows
    private void sendBinaryRequest(byte @NonNull [] message){
        var parsed = MultiDeviceCypher.encryptMessage(message, keys.writeKey(), counter++);
        if(options.async()) {
            session.getAsyncRemote().sendBinary(parsed.toBuffer());
            return;
        }

        session.getBasicRemote().sendBinary(parsed.toBuffer());
    }

    @Override
    public void connect() {
        try{
            webSocketContainer.connectToServer(this, URI.create(options.whatsappUrlBeta()));
        }catch (IOException | DeploymentException exception){
            throw new RuntimeException("Cannot connect to WhatsappWeb's WebServer", exception);
        }
    }

    @OnClose
    public void onClose(){
        System.out.println("Closed");
    }

    @Override
    public @NonNull MultiDeviceKeysManager keys() {
        return keys;
    }
}
