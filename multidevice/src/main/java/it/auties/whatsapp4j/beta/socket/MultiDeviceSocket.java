package it.auties.whatsapp4j.beta.socket;

import com.google.protobuf.ByteString;
import it.auties.whatsapp4j.beta.binary.BinaryDecoder;
import it.auties.whatsapp4j.beta.binary.BinaryEncoder;
import it.auties.whatsapp4j.beta.binary.MultiDeviceMessage;
import it.auties.whatsapp4j.beta.manager.MultiDeviceKeysManager;
import it.auties.whatsapp4j.beta.binary.BinaryUnpack;
import it.auties.whatsapp4j.beta.utils.Jid;
import it.auties.whatsapp4j.beta.utils.MultiDeviceCypher;
import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import it.auties.whatsapp4j.common.socket.IWhatsappSocket;
import it.auties.whatsapp4j.common.socket.WhatsappSocketConfiguration;
import it.auties.whatsapp4j.beta.socket.Proto.*;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import jakarta.websocket.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.URI;
import java.util.*;

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
    private final @NonNull BinaryEncoder encoder;
    private final @NonNull BinaryDecoder decoder;
    private long readCounter;
    private long writeCounter;
    public MultiDeviceSocket(@NonNull WhatsappConfiguration options) {
        this(options, new MultiDeviceKeysManager());
    }

    public MultiDeviceSocket(@NonNull WhatsappConfiguration options, @NonNull MultiDeviceKeysManager manager) {
        this(new NoiseHandshake(), ContainerProvider.getWebSocketContainer(), options, manager, new BinaryEncoder(), new BinaryDecoder());
    }

    @OnOpen
    @SneakyThrows
    public void onOpen(@NonNull Session session) {
        session(session);
        handshake.start(keys());
        var publicKey = CypherUtils.raw(keys.ephemeralKeyPair().getPublic());
        handshake.updateHash(publicKey);
        var handshakeMessage = HandshakeMessage.newBuilder().setClientHello(ClientHello.newBuilder().setEphemeral(ByteString.copyFrom(publicKey)).build()).build();
        sendBinaryRequest(handshakeMessage.toByteArray());
    }

    @OnMessage
    @SneakyThrows
    public void onBinary(byte @NonNull [] msg) {
        var multiDeviceMessage = new MultiDeviceMessage(msg);
        if(msg.length != multiDeviceMessage.length() + 3){
            log.warning("Invalid message: %s".formatted(multiDeviceMessage.raw().toHex()));
            return;
        }

        log.info("Length: " + msg.length);
        if (writeCounter == 1) {
            authenticate(multiDeviceMessage);
            return;
        }

        var cipher = MultiDeviceCypher.aesGmc(keys().readKey().data(), null, readCounter, false);
        var plainText = MultiDeviceCypher.aesGmcEncrypt(cipher, multiDeviceMessage.decoded().data());
        var unpacked = BinaryUnpack.unpack(plainText);
        var decoded = decoder.decode(unpacked);
        System.out.println(decoded);
    }

    private void createKeepAlive() {
        var keepAlive = new Node("iq", Map.of("id", BinaryUnpack.generateId(), "to", Jid.WHATSAPP_SERVER,  "type", "get", "xmlns", "w:p"), List.of(new Node("ping")));
        var result = encoder.encode(keepAlive);
        sendBinaryRequest(result);
    }

    private void authenticate(MultiDeviceMessage multiDeviceMessage) throws IOException, IllegalAccessException {
        var serverHello = HandshakeMessage.parseFrom(multiDeviceMessage.decoded().data()).getServerHello();
        handshake.updateHash(serverHello.getEphemeral().toByteArray());
        var sharedEphemeral = CypherUtils.calculateSharedSecret(serverHello.getEphemeral().toByteArray(), keys.ephemeralKeyPair().getPrivate());
        handshake.mixIntoKey(sharedEphemeral.data());

        var decodedStaticText = handshake.cypher(serverHello.getStatic().toByteArray(), false);
        var sharedStatic = CypherUtils.calculateSharedSecret(decodedStaticText, keys.ephemeralKeyPair().getPrivate());
        handshake.mixIntoKey(sharedStatic.data());
        handshake.cypher(serverHello.getPayload().toByteArray(), false);

        var encodedKey = handshake.cypher(CypherUtils.raw(keys.keyPair().getPublic()), true);
        var sharedPrivate = CypherUtils.calculateSharedSecret(serverHello.getEphemeral().toByteArray(), keys.keyPair().getPrivate());
        handshake.mixIntoKey(sharedPrivate.data());

        var encodedPayload = handshake.cypher(createPayload(), true);
        var protobufMessage = HandshakeMessage.newBuilder().setClientFinish(ClientFinish.newBuilder().setStatic(ByteString.copyFrom(encodedKey)).setPayload(ByteString.copyFrom(encodedPayload)).build()).build();
        sendBinaryRequest(protobufMessage.toByteArray());
        handshake.finish();
    }

    private byte[] createPayload() {
        var companion = CompanionProps.newBuilder()
                .setOs("Windows")
                .setVersion(AppVersion.newBuilder().setPrimary(10).build())
                .setPlatformType(CompanionProps.CompanionPropsPlatformType.CHROME)
                .setRequireFullSync(false)
                .build();
        var registerData = CompanionRegData.newBuilder()
                .setBuildHash(ByteString.copyFrom(Base64.getDecoder().decode("S9Kdc4pc4EJryo21snc5cg==")))
                .setCompanionProps(companion.toByteString())
                .setERegid(ByteString.copyFrom(BinaryArray.forInt(keys().registrationId(), 4).data()))
                .setEKeytype(ByteString.copyFrom(BinaryArray.forInt(5, 1).data()))
                .setEIdent(ByteString.copyFrom(CypherUtils.raw(keys().signedIdentityKey().getPublic())))
                .setESkeyId(ByteString.copyFrom(BinaryArray.forInt(1, 3).data()))
                .setESkeyVal(ByteString.copyFrom(keys().signedPreKey().publicKey()))
                .setESkeySig(ByteString.copyFrom(keys().signedPreKey().signature()))
                .build();
        var userAgent = UserAgent.newBuilder()
                .setAppVersion(AppVersion.newBuilder().setPrimary(2).setSecondary(2126).setTertiary(14).build())
                .setPlatform(UserAgent.UserAgentPlatform.WEB)
                .setReleaseChannel(UserAgent.UserAgentReleaseChannel.RELEASE)
                .setMcc("000")
                .setMnc("000")
                .setOsVersion("0.1")
                .setManufacturer("")
                .setDevice("Desktop")
                .setOsBuildNumber("0.1")
                .setLocaleLanguageIso6391("en")
                .setLocaleCountryIso31661Alpha2("en")
                .build();
        return ClientPayload.newBuilder()
                .setConnectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .setConnectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .setPassive(false)
                .setRegData(registerData)
                .setUserAgent(userAgent)
                .setWebInfo(WebInfo.newBuilder().setWebSubPlatform(WebInfo.WebInfoWebSubPlatform.WEB_BROWSER))
                .build()
                .toByteArray();
    }

    @SneakyThrows
    private void sendBinaryRequest(byte @NonNull [] message){
        var counter = writeCounter++;
        var parsed = MultiDeviceCypher.encryptMessage(message, keys.writeKey(), counter);
        System.out.printf("Sending message %s with %s bytes(%s)%n", counter, parsed.size(), parsed.toHex());
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
