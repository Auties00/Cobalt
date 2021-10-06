package it.auties.whatsapp4j.socket;

import com.google.protobuf.ByteString;
import it.auties.whatsapp4j.binary.BinaryDecoder;
import it.auties.whatsapp4j.binary.BinaryEncoder;
import it.auties.whatsapp4j.binary.BinaryUnpack;
import it.auties.whatsapp4j.binary.MultiDeviceMessage;
import it.auties.whatsapp4j.manager.MultiDeviceKeysManager;
import it.auties.whatsapp4j.socket.Proto.*;
import it.auties.whatsapp4j.utils.Jid;
import it.auties.whatsapp4j.utils.MultiDeviceCypher;
import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.manager.WhatsappDataManager;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import it.auties.whatsapp4j.common.socket.IWhatsappSocket;
import it.auties.whatsapp4j.common.socket.WhatsappSocketConfiguration;
import it.auties.whatsapp4j.common.utils.WhatsappQRCode;
import jakarta.websocket.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static it.auties.whatsapp4j.common.utils.CypherUtils.*;

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
    private final @NonNull WhatsappDataManager manager;
    private final @NonNull MultiDeviceKeysManager keys;
    private final @NonNull WhatsappQRCode qrCode;
    private final @NonNull BinaryEncoder encoder;
    private final @NonNull BinaryDecoder decoder;
    private long readCounter;
    private long writeCounter;

    public MultiDeviceSocket(@NonNull WhatsappConfiguration options, @NonNull MultiDeviceKeysManager keys) {
        this(new NoiseHandshake(), ContainerProvider.getWebSocketContainer(), options, WhatsappDataManager.singletonInstance(), keys, new WhatsappQRCode(), new BinaryEncoder(), new BinaryDecoder());
    }

    @OnOpen
    @SneakyThrows
    public void onOpen(@NonNull Session session) {
        session(session);
        handshake.start(keys());
        handshake.updateHash(keys.ephemeralKeyPair().publicKey());
        var handshakeMessage = HandshakeMessage.newBuilder().setClientHello(ClientHello.newBuilder().setEphemeral(ByteString.copyFrom(keys.ephemeralKeyPair().publicKey())).build()).build();
        sendBinaryRequest(handshakeMessage.toByteArray());
    }

    @OnMessage
    @SneakyThrows
    public void onBinary(byte @NonNull [] raw) {
        System.err.printf("Message length: %s%n", raw.length);
        var message = new MultiDeviceMessage(raw).decoded().data();
        if(!loggedIn){
            authenticate(message);
            return;
        }

        var cipher = MultiDeviceCypher.aesGmc(keys().readKey().data(), null, readCounter++, false);
        var plainText = MultiDeviceCypher.aesGmcEncrypt(cipher, message);
        var unpacked = BinaryUnpack.unpack(plainText);
        var decoded = decoder.decode(unpacked);
        NodeDigester.digestWhatsappNode(this, decoded);
    }

    @SneakyThrows
    private void authenticate(byte[] message) {
        var serverHello = HandshakeMessage.parseFrom(message).getServerHello();
        var ephemeralPrivate = toPKCS8Encoded(keys.ephemeralKeyPair().privateKey());
        handshake.updateHash(serverHello.getEphemeral().toByteArray());
        var sharedEphemeral = calculateSharedSecret(toX509Encoded(serverHello.getEphemeral().toByteArray()), ephemeralPrivate);
        handshake.mixIntoKey(sharedEphemeral.data());

        var decodedStaticText = handshake.cypher(serverHello.getStatic().toByteArray(), false);
        var sharedStatic = calculateSharedSecret(toX509Encoded(decodedStaticText), ephemeralPrivate);
        handshake.mixIntoKey(sharedStatic.data());
        handshake.cypher(serverHello.getPayload().toByteArray(), false);

        var encodedKey = handshake.cypher(raw(keys.keyPair().getPublic()), true);
        var sharedPrivate = calculateSharedSecret(toX509Encoded(serverHello.getEphemeral().toByteArray()), keys.keyPair().getPrivate());
        handshake.mixIntoKey(sharedPrivate.data());

        var encodedPayload = handshake.cypher(createPayload(), true);
        var protobufMessage = HandshakeMessage.newBuilder().setClientFinish(ClientFinish.newBuilder().setStatic(ByteString.copyFrom(encodedKey)).setPayload(ByteString.copyFrom(encodedPayload)).build()).build();
        sendBinaryRequest(protobufMessage.toByteArray());

        handshake.finish();
        ping();
        this.loggedIn = true;
        this.readCounter = 0;
        this.writeCounter = 0;
    }

    private void ping() {
        var keepAlive = new Node(
                "iq",
                Map.of(
                        "id", BinaryUnpack.generateId(),
                        "to", Jid.WHATSAPP_SERVER,
                        "type", "get",
                        "xmlns", "w:p"
                ),
                List.of(new Node("ping"))
        );

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> sendBinaryRequest(keepAlive), 0, 20, TimeUnit.SECONDS);
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
                .setEIdent(ByteString.copyFrom(keys().signedIdentityKey().publicKey()))
                .setESkeyId(ByteString.copyFrom(BinaryArray.forInt(keys().signedPreKey().id(), 3).data()))
                .setESkeyVal(ByteString.copyFrom(keys().signedPreKey().keyPair().publicKey()))
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

    void sendBinaryRequest(@NonNull Node node){
        sendBinaryRequest(encoder.encode(node));
    }

    @SneakyThrows
    void sendBinaryRequest(byte @NonNull [] message){
        var parsed = MultiDeviceCypher.encryptMessage(message, keys.writeKey(), writeCounter++);
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
