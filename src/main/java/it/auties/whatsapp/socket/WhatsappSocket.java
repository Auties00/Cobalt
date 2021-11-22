package it.auties.whatsapp.socket;

import com.google.protobuf.ByteString;
import it.auties.whatsapp.binary.BinaryDecoder;
import it.auties.whatsapp.binary.BinaryEncoder;
import it.auties.whatsapp.binary.BinaryMessage;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.socket.Proto.*;
import it.auties.whatsapp.utils.CypherUtils;
import it.auties.whatsapp.utils.MultiDeviceCypher;
import it.auties.whatsapp.api.WhatsappConfiguration;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.model.misc.Node;
import it.auties.whatsapp.utils.WhatsappQRCode;
import jakarta.websocket.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;

@RequiredArgsConstructor
@Data
@Accessors(fluent = true)
@ClientEndpoint(configurator = WhatsappSocketConfiguration.class)
@Log
public class WhatsappSocket {
    private @Getter(onMethod = @__(@NonNull)) Session session;
    private boolean loggedIn;
    private final NoiseHandshake handshake;
    private final @NonNull WebSocketContainer container;
    private final @NonNull WhatsappConfiguration options;
    private final @NonNull WhatsappStore store;
    private final @NonNull WhatsappKeys keys;
    private final @NonNull BinaryEncoder encoder;
    private final @NonNull BinaryDecoder decoder;
    private final @NonNull NodeDigester digester;
    private long readCounter;
    private long writeCounter;

    public WhatsappSocket(@NonNull WhatsappConfiguration options, @NonNull WhatsappStore store, @NonNull WhatsappKeys keys) {
        this.handshake = new NoiseHandshake();
        this.container = ContainerProvider.getWebSocketContainer();
        this.options = options;
        this.store = store;
        this.keys = keys;
        this.encoder = new BinaryEncoder();
        this.decoder = new BinaryDecoder();
        this.digester = new NodeDigester(this, new WhatsappQRCode());
    }

    @OnOpen
    @SneakyThrows
    public void onOpen(@NonNull Session session) {
        session(session);
        if(loggedIn){
            return;
        }

        handshake.start(keys());
        handshake.updateHash(keys.ephemeralKeyPair().publicKey());
        var handshakeMessage = HandshakeMessage.newBuilder().setClientHello(ClientHello.newBuilder().setEphemeral(ByteString.copyFrom(keys.ephemeralKeyPair().publicKey())).build()).build();
        sendBinaryRequest(handshakeMessage.toByteArray(), true);
    }

    @OnMessage
    @SneakyThrows
    public void onBinary(byte @NonNull [] raw) {
        System.err.printf("Message length: %s%n", raw.length);
        var message = new BinaryMessage(raw).decoded().data();
        if(!loggedIn){
            authenticate(message);
            return;
        }

        var cipher = MultiDeviceCypher.aesGmc(keys().readKey().data(), null, readCounter++, false);
        var plainText = MultiDeviceCypher.aesGmcEncrypt(cipher, message);
        var decoded = decoder.decode(decoder.unpack(plainText));
        digester.digest(decoded);
    }

    @SneakyThrows
    private void authenticate(byte[] message) {
        var serverHello = HandshakeMessage.parseFrom(message).getServerHello();
        var ephemeralPrivate = CypherUtils.toPKCS8Encoded(keys.ephemeralKeyPair().privateKey());
        handshake.updateHash(serverHello.getEphemeral().toByteArray());
        var sharedEphemeral = CypherUtils.calculateSharedSecret(CypherUtils.toX509Encoded(serverHello.getEphemeral().toByteArray()), ephemeralPrivate);
        handshake.mixIntoKey(sharedEphemeral.data());

        var decodedStaticText = handshake.cypher(serverHello.getStatic().toByteArray(), false);
        var sharedStatic = CypherUtils.calculateSharedSecret(CypherUtils.toX509Encoded(decodedStaticText), ephemeralPrivate);
        handshake.mixIntoKey(sharedStatic.data());
        handshake.cypher(serverHello.getPayload().toByteArray(), false);

        var encodedKey = handshake.cypher(CypherUtils.raw(keys.keyPair().getPublic()), true);
        var sharedPrivate = CypherUtils.calculateSharedSecret(CypherUtils.toX509Encoded(serverHello.getEphemeral().toByteArray()), keys.keyPair().getPrivate());
        handshake.mixIntoKey(sharedPrivate.data());

        var encodedPayload = handshake.cypher(createPayload(), true);
        var protobufMessage = HandshakeMessage.newBuilder().setClientFinish(ClientFinish.newBuilder().setStatic(ByteString.copyFrom(encodedKey)).setPayload(ByteString.copyFrom(encodedPayload)).build()).build();
        sendBinaryRequest(protobufMessage.toByteArray());

        changeState(true);
        handshake.finish();
    }

    private byte[] createPayload() {
        var payload = createClientPayload(keys().mayRestore());
        if (!keys().mayRestore()) {
            return payload.setRegData(createRegisterData())
                    .build()
                    .toByteArray();
        }

        return payload.setUsername(Long.parseLong(keys().me().user()))
                .setDevice(keys().me().device())
                .build()
                .toByteArray();
    }

    private ClientPayload.Builder createClientPayload(boolean passive) {
        return ClientPayload.newBuilder()
                .setConnectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .setConnectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .setUserAgent(createUserAgent())
                .setPassive(passive)
                .setWebInfo(WebInfo.newBuilder().setWebSubPlatform(WebInfo.WebInfoWebSubPlatform.WEB_BROWSER));
    }

    private UserAgent createUserAgent() {
        return UserAgent.newBuilder()
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
    }

    private CompanionRegData createRegisterData() {
        return CompanionRegData.newBuilder()
                .setBuildHash(ByteString.copyFrom(Base64.getDecoder().decode("S9Kdc4pc4EJryo21snc5cg==")))
                .setCompanionProps(createCompanionProps().toByteString())
                .setERegid(ByteString.copyFrom(BinaryArray.of(keys().id(), 4).data()))
                .setEKeytype(ByteString.copyFrom(BinaryArray.of(5, 1).data()))
                .setEIdent(ByteString.copyFrom(keys().signedIdentityKey().publicKey()))
                .setESkeyId(ByteString.copyFrom(BinaryArray.of(keys().signedPreKey().id(), 3).data()))
                .setESkeyVal(ByteString.copyFrom(keys().signedPreKey().keyPair().publicKey()))
                .setESkeySig(ByteString.copyFrom(keys().signedPreKey().signature()))
                .build();
    }

    private CompanionProps createCompanionProps() {
        return CompanionProps.newBuilder()
                .setOs("Windows")
                .setVersion(AppVersion.newBuilder().setPrimary(10).build())
                .setPlatformType(CompanionProps.CompanionPropsPlatformType.CHROME)
                .setRequireFullSync(false)
                .build();
    }

    void sendBinaryRequest(@NonNull Node node){
        sendBinaryRequest(encoder.encode(node));
    }

    void sendBinaryRequest(byte @NonNull [] message){
        sendBinaryRequest(message, false);
    }

    @SneakyThrows
    void sendBinaryRequest(byte @NonNull [] message, boolean prologue){
        var parsed = MultiDeviceCypher.encryptMessage(message, keys.writeKey(), writeCounter++, prologue);
        if(options.async()) {
            session.getAsyncRemote().sendBinary(parsed.toBuffer());
            return;
        }

        session.getBasicRemote().sendBinary(parsed.toBuffer());
    }

    public void connect() {
        try{
            container.connectToServer(this, URI.create(options.whatsappUrlBeta()));
        }catch (IOException | DeploymentException exception){
            throw new RuntimeException("Cannot connect to WhatsappWeb's WebServer", exception);
        }
    }

    @SneakyThrows
    void reconnect(){
        changeState(false);
        session().close();
        connect();
    }

    void changeState(boolean loggedIn){
        this.loggedIn = loggedIn;
        this.readCounter = 0;
        this.writeCounter = 0;
        keys().readKey(null).writeKey(null);
    }

    @OnClose
    public void onClose(){
        System.out.println("Closed");
    }

    @OnError
    public void onError(Throwable throwable){
        throwable.printStackTrace();
    }
}
