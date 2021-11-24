package it.auties.whatsapp.socket;

import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryDecoder;
import it.auties.whatsapp.binary.BinaryEncoder;
import it.auties.whatsapp.binary.BinaryMessage;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.contact.ContactId;
import it.auties.whatsapp.protobuf.beta.*;
import it.auties.whatsapp.protobuf.message.server.HandshakeMessage;
import it.auties.whatsapp.utils.*;
import it.auties.whatsapp.api.WhatsappConfiguration;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.model.Node;
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
        var handshakeMessage = new HandshakeMessage(new ClientHello(null, null, keys.ephemeralKeyPair().publicKey()));
        sendBinaryRequest(ProtobufEncoder.encode(handshakeMessage), true);
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
        var serverHello = ProtobufDecoder.forType(HandshakeMessage.class).decode(message).serverHello();
        var ephemeralPrivate = CypherUtils.toPKCS8Encoded(keys.ephemeralKeyPair().privateKey());
        handshake.updateHash(serverHello.ephemeral());
        var sharedEphemeral = CypherUtils.calculateSharedSecret(CypherUtils.toX509Encoded(serverHello.ephemeral()), ephemeralPrivate);
        handshake.mixIntoKey(sharedEphemeral.data());

        var decodedStaticText = handshake.cypher(serverHello._static(), false);
        var sharedStatic = CypherUtils.calculateSharedSecret(CypherUtils.toX509Encoded(decodedStaticText), ephemeralPrivate);
        handshake.mixIntoKey(sharedStatic.data());
        handshake.cypher(serverHello.payload(), false);

        var encodedKey = handshake.cypher(CypherUtils.raw(keys.keyPair().getPublic()), true);
        var sharedPrivate = CypherUtils.calculateSharedSecret(CypherUtils.toX509Encoded(serverHello.ephemeral()), keys.keyPair().getPrivate());
        handshake.mixIntoKey(sharedPrivate.data());

        var encodedPayload = handshake.cypher(createPayload(), true);
        var protobufMessage = new HandshakeMessage(new ClientFinish(encodedPayload, encodedKey));
        sendBinaryRequest(ProtobufEncoder.encode(protobufMessage));

        changeState(true);
        handshake.finish();
    }

    @SneakyThrows
    private byte[] createPayload() {
        var payload = createClientPayload(keys().mayRestore());
        if (!keys().mayRestore()) {
            payload.regData(createRegisterData());
        }else {
            payload.username(Long.parseLong(keys().me().user()))
                    .device(keys().me().device());
        }

        return ProtobufEncoder.encode(payload.build());
    }

    private ClientPayload.ClientPayloadBuilder createClientPayload(boolean passive) {
        return ClientPayload.builder()
                .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .userAgent(createUserAgent())
                .passive(passive)
                .webInfo(WebInfo.builder().webSubPlatform(WebInfo.WebInfoWebSubPlatform.WEB_BROWSER).build());
    }

    private UserAgent createUserAgent() {
        return UserAgent.builder()
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
    }

    @SneakyThrows
    private CompanionRegData createRegisterData() {
        return CompanionRegData.builder()
                .buildHash(Base64.getDecoder().decode("S9Kdc4pc4EJryo21snc5cg=="))
                .companionProps(ProtobufEncoder.encode(createCompanionProps()))
                .eRegid(BinaryArray.of(keys().id(), 4).data())
                .eKeytype(BinaryArray.of(5, 1).data())
                .eIdent(keys().signedIdentityKey().publicKey())
                .eSkeyId(BinaryArray.of(keys().signedPreKey().id(), 3).data())
                .eSkeyVal(keys().signedPreKey().keyPair().publicKey())
                .eSkeySig(keys().signedPreKey().signature())
                .build();
    }

    private CompanionProps createCompanionProps() {
        return CompanionProps.builder()
                .os("Windows")
                .version(new AppVersion(0, 0, 0, 0, 10))
                .platformType(CompanionProps.CompanionPropsPlatformType.CHROME)
                .requireFullSync(false)
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

        var advIdentity = ProtobufDecoder.forType(ADVSignedDeviceIdentityHMAC.class).decode(deviceIdentity);
        var advSecret = Base64.getDecoder().decode(keys().advSecretKey());
        var advSign = CypherUtils.hmacSha256(BinaryArray.of(advIdentity.details()), BinaryArray.of(advSecret));
        Validate.isTrue(Arrays.equals(advIdentity.hmac(), advSign.data()), "Cannot login: Hmac validation failed!", SecurityException.class);

        var account = ProtobufDecoder.forType(ADVSignedDeviceIdentity.class).decode(advIdentity.details());
        var message = BinaryArray.of(new byte[]{6, 0})
                .append(account.details())
                .append(keys().signedIdentityKey().publicKey())
                .data();
        Validate.isTrue(curve.verifySignature(account.accountSignatureKey(), message, account.accountSignature()), "Cannot login: Hmac validation failed!", SecurityException.class);

        var deviceSignatureMessage = BinaryArray.of(new byte[]{6, 1})
                .append(account.details())
                .append(keys().signedIdentityKey().publicKey())
                .append(account.accountSignature())
                .data();
        var deviceSignature = curve.calculateSignature(keys().signedIdentityKey().privateKey(), deviceSignatureMessage);

        var keyIndex = ProtobufDecoder.forType(ADVDeviceIdentity.class).decode(account.details()).keyIndex();
        var content = new Node(
                "pair-device-sign",
                Map.of(),
                List.of(
                        new Node(
                                "device-identity",
                                Map.of("key-index", keyIndex),
                                ProtobufEncoder.encode(account.deviceSignature(deviceSignature).accountSignature(null))
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
