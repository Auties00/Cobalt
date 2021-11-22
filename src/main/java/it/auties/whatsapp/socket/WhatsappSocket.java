package it.auties.whatsapp.socket;

import com.google.protobuf.ByteString;
import it.auties.whatsapp.binary.BinaryDecoder;
import it.auties.whatsapp.binary.BinaryEncoder;
import it.auties.whatsapp.binary.BinaryMessage;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.contact.ContactId;
import it.auties.whatsapp.socket.Proto.*;
import it.auties.whatsapp.utils.*;
import it.auties.whatsapp.api.WhatsappConfiguration;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.model.misc.Node;
import jakarta.websocket.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.whispersystems.curve25519.Curve25519;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
    private final @NonNull WhatsappQRCode generator;
    private CountDownLatch lock;
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
        this.generator = new WhatsappQRCode();
        this.lock = new CountDownLatch(2);
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
            lock.countDown();
            return;
        }

        var cipher = MultiDeviceCypher.aesGmc(keys().readKey().data(), null, readCounter++, false);
        var plainText = MultiDeviceCypher.aesGmcEncrypt(cipher, message);
        var decoded = decoder.decode(decoder.unpack(plainText));
        digest(decoded);
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
    
    private void digest(@NonNull Node node) {
        System.out.printf("Received: %s%n", node);
        switch (node.description()){
            case "iq" -> {
                var children = node.childNodes();
                if(children.isEmpty()){
                    return;
                }

                var container = children.getFirst();
                switch (container.description()){
                    case "pair-device" -> generateQrCode(node, container);
                    case "pair-success" -> confirmQrCode(node, container);
                    case "active" -> store().callListeners(listener -> listener.onLoggedIn(keys().me()));
                    default -> throw new IllegalArgumentException("Cannot handle iq request, unknown description. %s%n".formatted(container.description()));
                }
            }

            case "success" -> {
                sendPreKeys();
                confirmConnection();
            }

            case "stream:error" -> {
                var code = node.attributes().getInt("code");
                if(code != 515){
                    return;
                }

                reconnect();
            }

            case "failure" -> {
                Validate.isTrue(node.attributes().getInt("reason") != 401,
                        "WhatsappWeb failure at %s, status code: %s",
                        node.attributes().getNullableString("location"), node.attributes().getNullableString("reason"));
                reconnect();
            }

            default -> System.err.println("Unhandled");
        }
    }

    private void confirmConnection() {
        var stanza = new Node(
                "iq",
                Map.of(
                        "to", ContactId.WHATSAPP_SERVER,
                        "xmlns", "passive",
                        "type", "set",
                        "id", WhatsappUtils.buildRequestTag(options())
                ),
                List.of(new Node("active"))
        );
        sendBinaryRequest(stanza);
    }

    private void sendPreKeys() {
        if(keys().preKeys()){
            return;
        }

        var preKeys = createPreKeys();
        var stanza = new Node(
                "iq",
                Map.of(
                        "id", WhatsappUtils.buildRequestTag(options()),
                        "xmlns", "encrypt",
                        "type", "set",
                        "to", ContactId.WHATSAPP_SERVER
                ),
                List.of(
                        new Node(
                                "registration",
                                Map.of(),
                                BinaryArray.of(keys().id(), 4).data()
                        ),
                        new Node(
                                "type",
                                Map.of(),
                                ""
                        ),
                        new Node(
                                "identity",
                                Map.of(),
                                keys().signedIdentityKey().publicKey()
                        ),
                        new Node(
                                "list",
                                Map.of(),
                                preKeys
                        ),
                        new Node(
                                "skey",
                                Map.of(),
                                List.of(
                                        new Node("id",
                                                Map.of(),
                                                BinaryArray.of(keys().signedPreKey().id(), 3).data()
                                        ),
                                        new Node(
                                                "value",
                                                Map.of(),
                                                keys().signedPreKey().keyPair().publicKey()
                                        ),
                                        new Node(
                                                "signature",
                                                Map.of(),
                                                keys().signedPreKey().signature()
                                        )
                                )
                        )
                )
        );

        sendBinaryRequest(stanza);
        keys().preKeys(true);
    }

    private List<Node> createPreKeys() {
        return IntStream.range(0, 30)
                .mapToObj(index -> new Node("key", Map.of(), List.of(new Node("id", Map.of(), BinaryArray.of(index, 3).data()), new Node("value", Map.of(), MultiDeviceCypher.createKeyPair().publicKey()))))
                .toList();
    }

    private void generateQrCode(Node node, Node container) {
        var qr = decodeQrCode(container);
        var matrix = generator.generate(qr, CypherUtils.raw(keys().keyPair().getPublic()), keys().signedIdentityKey().publicKey(), keys().advSecretKey());
        store().callListeners(listener -> listener.onQRCode(matrix));
        sendConfirmNode(node, null);
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::ping, 0L, 30L, TimeUnit.SECONDS);
    }

    private String decodeQrCode(Node container) {
        return container.findNodeByDescription("ref")
                .filter(ref -> ref.content() instanceof byte[])
                .map(ref -> (byte[]) ref.content())
                .map(String::new)
                .orElseThrow(() -> new NoSuchElementException("Pairing error: missing qr code reference"));
    }

    private void sendConfirmNode(Node node, Object content) {
        var iq = new Node(
                "iq",
                Map.of(
                        "id", Objects.requireNonNull(node.attrs().get("id"), "Missing id"),
                        "to", ContactId.WHATSAPP_SERVER,
                        "type", "result"
                ),
                content
        );

        sendBinaryRequest(iq);
    }

    @SneakyThrows
    private void confirmQrCode(Node node, Node container) {
        keys().me(fetchJid(container));

        var curve = Curve25519.getInstance(Curve25519.BEST);
        var deviceIdentity = fetchDeviceIdentity(container);

        var advIdentity = Proto.ADVSignedDeviceIdentityHMAC.parseFrom(deviceIdentity);
        var advSecret = Base64.getDecoder().decode(keys().advSecretKey());
        var advSign = CypherUtils.hmacSha256(BinaryArray.of(advIdentity.getDetails().toByteArray()), BinaryArray.of(advSecret));
        Validate.isTrue(Arrays.equals(advIdentity.getHmac().toByteArray(), advSign.data()), "Cannot login: Hmac validation failed!", SecurityException.class);

        var account = Proto.ADVSignedDeviceIdentity.parseFrom(advIdentity.getDetails());
        var message = BinaryArray.of(new byte[]{6, 0})
                .append(account.getDetails().toByteArray())
                .append(keys().signedIdentityKey().publicKey())
                .data();
        Validate.isTrue(curve.verifySignature(account.getAccountSignatureKey().toByteArray(), message, account.getAccountSignature().toByteArray()), "Cannot login: Hmac validation failed!", SecurityException.class);

        var deviceSignatureMessage = BinaryArray.of(new byte[]{6, 1})
                .append(account.getDetails().toByteArray())
                .append(keys().signedIdentityKey().publicKey())
                .append(account.getAccountSignatureKey().toByteArray())
                .data();
        var deviceSignature = curve.calculateSignature(keys().signedIdentityKey().privateKey(), deviceSignatureMessage);

        var keyIndex = Proto.ADVDeviceIdentity.parseFrom(account.getDetails()).getKeyIndex();
        var content = new Node(
                "pair-device-sign",
                Map.of(),
                List.of(
                        new Node(
                                "device-identity",
                                Map.of("key-index", keyIndex),
                                account.toBuilder().setDeviceSignature(ByteString.copyFrom(deviceSignature)).clearAccountSignatureKey().build().toByteArray()
                        )
                )
        );
        sendConfirmNode(node, List.of(content));
    }

    private byte[] fetchDeviceIdentity(Node container) {
        return container.findNodeByDescription("device-identity")
                .map(Node::content)
                .filter(data -> data instanceof byte[])
                .map(data -> (byte[]) data)
                .orElseThrow(() -> new NoSuchElementException("Cannot find device identity node for authentication in %s".formatted(container)));
    }

    private ContactId fetchJid(Node container) {
        return container.findNodeByDescription("device")
                .map(Node::attributes)
                .orElseThrow(() -> new NoSuchElementException("Cannot find device node for authentication in %s".formatted(container)))
                .getObject("jid", ContactId.class)
                .orElseThrow(() -> new NoSuchElementException("Cannot find jid attribute in %s".formatted(container)));
    }

    private void ping() {
        var keepAlive = new Node(
                "iq",
                Map.of(
                        "id", WhatsappUtils.buildRequestTag(options()),
                        "to", ContactId.WHATSAPP_SERVER,
                        "type", "get",
                        "xmlns", "w:p"
                ),
                List.of(new Node("ping"))
        );

        sendBinaryRequest(keepAlive);
    }

    private void sendBinaryRequest(@NonNull Node node){
        sendBinaryRequest(encoder.encode(node));
    }

    private void sendBinaryRequest(byte @NonNull [] message){
        sendBinaryRequest(message, false);
    }

    @SneakyThrows
    private void sendBinaryRequest(byte @NonNull [] message, boolean prologue){
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
            lock.await();
        }catch (IOException | DeploymentException | InterruptedException exception){
            throw new RuntimeException("Cannot connect to WhatsappWeb's WebServer", exception);
        }
    }

    @SneakyThrows
    private void reconnect(){
        changeState(false);
        session().close();
        connect();
    }

    private void changeState(boolean loggedIn){
        this.loggedIn = loggedIn;
        this.readCounter = 0;
        this.writeCounter = 0;
        this.lock = new CountDownLatch(1);
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
