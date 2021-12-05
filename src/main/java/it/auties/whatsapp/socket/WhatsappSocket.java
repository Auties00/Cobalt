package it.auties.whatsapp.socket;

import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.api.WhatsappConfiguration;
import it.auties.whatsapp.binary.BinaryMessage;
import it.auties.whatsapp.cipher.Cipher;
import it.auties.whatsapp.cipher.NoiseHandshake;
import it.auties.whatsapp.cipher.Request;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.authentication.*;
import it.auties.whatsapp.protobuf.contact.ContactId;
import it.auties.whatsapp.protobuf.message.server.HandshakeMessage;
import it.auties.whatsapp.protobuf.model.Node;
import it.auties.whatsapp.protobuf.model.PreKey;
import it.auties.whatsapp.protobuf.model.UserAgent;
import it.auties.whatsapp.protobuf.model.Version;
import it.auties.whatsapp.utils.Qr;
import it.auties.whatsapp.utils.Validate;
import it.auties.whatsapp.utils.WhatsappUtils;
import jakarta.websocket.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static it.auties.protobuf.encoder.ProtobufEncoder.encode;
import static it.auties.whatsapp.binary.BinaryArray.of;
import static it.auties.whatsapp.binary.BinaryArray.ofBase64;
import static java.lang.Long.parseLong;
import static java.util.Map.of;

@RequiredArgsConstructor
@Data
@Accessors(fluent = true)
@ClientEndpoint(configurator = WhatsappSocketConfiguration.class)
@Log
public class WhatsappSocket {
    private static final int ERROR_CONSTANT = 8913411;
    private static final String BUILD_HASH = "S9Kdc4pc4EJryo21snc5cg==";
    private static final String SYSTEM = "Windows";
    private static final int KEY_TYPE = 5;

    private @Getter(onMethod = @__(@NonNull)) Session session;
    private boolean loggedIn;
    private final NoiseHandshake handshake;
    private final @NonNull WebSocketContainer container;
    private final @NonNull WhatsappConfiguration options;
    private final @NonNull WhatsappStore store;
    private final @NonNull WhatsappKeys keys;
    private final @NonNull Digester digester;
    private CountDownLatch lock;

    public WhatsappSocket(@NonNull WhatsappConfiguration options, @NonNull WhatsappStore store, @NonNull WhatsappKeys keys) {
        this.handshake = new NoiseHandshake();
        this.container = ContainerProvider.getWebSocketContainer();
        this.options = options;
        this.store = store;
        this.keys = keys;
        this.digester = new Digester();
        this.lock = new CountDownLatch(1);
    }

    @OnOpen
    public void onOpen(@NonNull Session session) {
        session(session);
        if(loggedIn){
            return;
        }

        handshake.start(keys());
        handshake.updateHash(keys.ephemeralKeyPair().publicKey());
        Request.with(new HandshakeMessage(new ClientHello(keys.ephemeralKeyPair().publicKey())))
                .send(session(), keys(), store(), true);
    }

    @OnMessage
    @SneakyThrows
    public void onBinary(byte @NonNull [] raw) {
        var message = new BinaryMessage(raw);
        System.out.println("Incoming message: " + raw.length);
        if(message.length() == ERROR_CONSTANT){
            disconnect();
            return;
        }

        if(!loggedIn){
            authenticate(message.decoded().data());
            lock.countDown();
            return;
        }

        var deciphered = Cipher.decipherMessage(message.decoded().data(), keys.readKey(), store.readCounter().getAndIncrement());
        if(store().resolvePendingRequest(deciphered, false)){
            return;
        }

        digester.digest(deciphered);
    }

    @SneakyThrows
    private void authenticate(byte[] message) {
        var serverHello = ProtobufDecoder.forType(HandshakeMessage.class).decode(message).serverHello();
        handshake.updateHash(serverHello.ephemeral());
        var sharedEphemeral = Cipher.calculateSharedSecret(serverHello.ephemeral(), keys.ephemeralKeyPair().privateKey());
        handshake.mixIntoKey(sharedEphemeral.data());

        var decodedStaticText = handshake.cypher(serverHello.staticText(), false);
        var sharedStatic = Cipher.calculateSharedSecret(decodedStaticText, keys.ephemeralKeyPair().privateKey());
        handshake.mixIntoKey(sharedStatic.data());
        handshake.cypher(serverHello.payload(), false);

        var encodedKey = handshake.cypher(keys.companionKeyPair().publicKey(), true);
        var sharedPrivate = Cipher.calculateSharedSecret(serverHello.ephemeral(), keys.companionKeyPair().privateKey());
        handshake.mixIntoKey(sharedPrivate.data());

        var encodedPayload = handshake.cypher(createUserPayload(), true);
        var clientFinish = new ClientFinish(encodedKey, encodedPayload);
        Request.with(new HandshakeMessage(clientFinish))
                .send(session(), keys(), store());
        changeState(true);
        handshake.finish();
    }

    private byte[] createUserPayload() {
        var builder = ClientPayload.builder()
                .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .userAgent(createUserAgent())
                .passive(keys.hasUser())
                .webInfo(new WebInfo(WebInfo.WebInfoWebSubPlatform.WEB_BROWSER));
        return encode(keys.hasUser() ? builder.username(parseLong(keys().companion().user())).device(keys().companion().device()).build()
                : builder.regData(createRegisterData()).build());
    }

    private UserAgent createUserAgent() {
        return UserAgent.builder()
                .appVersion(new Version(2, 2144, 11))
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

    private CompanionData createRegisterData() {
        return CompanionData.builder()
                .buildHash(ofBase64(BUILD_HASH).data())
                .companion(encode(createCompanionProps()))
                .id(of(keys().id(), 4).data())
                .keyType(of(KEY_TYPE, 1).data())
                .identifier(keys().identityKeyPair().publicKey())
                .signatureId(keys().signedKeyPair().id())
                .signaturePublicKey(keys().signedKeyPair().keyPair().publicKey())
                .signature(keys().signedKeyPair().signature())
                .build();
    }

    private Companion createCompanionProps() {
        return Companion.builder()
                .os(SYSTEM)
                .version(new Version(10))
                .platformType(Companion.CompanionPropsPlatformType.CHROME)
                .requireFullSync(false)
                .build();
    }

    public void connect() {
        try{
            container.connectToServer(this, URI.create(options.whatsappUrlBeta()));
            lock.await();
        }catch (IOException | DeploymentException | InterruptedException exception){
            throw new RuntimeException("Cannot connect to WhatsappWeb's WebServer", exception);
        }
    }

    public void reconnect(){
        try {
            changeState(false);
            session().close();
            connect();
        }catch (IOException exception){
            throw new RuntimeException("Cannot reconnect to WhatsappWeb's WebServer", exception);
        }
    }

    public void disconnect(){
        try{
            session.close();
        }catch (IOException exception){
            throw new RuntimeException("Cannot close connection to WhatsappWeb's WebServer", exception);
        }
    }

    private void changeState(boolean loggedIn){
        this.loggedIn = loggedIn;
        this.lock = new CountDownLatch(1);
        keys().clear();
        store().clearCounters();
    }

    @OnClose
    public void onClose(){
        System.out.println("Closed");
    }

    @OnError
    public void onError(Throwable throwable){
        throwable.printStackTrace();
    }

    private class Digester {
        private void digest(@NonNull Node node) {
            System.out.printf("Received: %s%n", node);
            switch (node.description()){
                case "iq" -> digestIq(node);
                case "success" -> digestSuccess();
                case "stream:error" -> digestError(node);
                case "failure" -> digestFailure(node);
                case "xmlstreamend" -> disconnect();
                default -> System.err.println("Unhandled");
            }
        }

        private void digestFailure(Node node) {
            Validate.isTrue(node.attributes().get("reason").equals(401),
                    "WhatsappWeb failure at %s, status code: %s",
                    node.attributes().get("location"), node.attributes().get("reason"));
            reconnect();
        }

        private void digestError(Node node) {
            var code = node.attributes().getOrDefault("code", "");
            if(code.equals("515")){
                reconnect();
                return;
            }

            node.childNodes()
                    .forEach(error -> store.resolvePendingRequest(error, true));
        }

        private void digestSuccess() {
            sendPreKeys();
            confirmConnection();
        }

        private void digestIq(Node node) {
            var children = node.childNodes();
            if(children.isEmpty()){
                return;
            }

            var container = children.getFirst();
            switch (container.description()){
                case "pair-device" -> generateQrCode(node, container);
                case "pair-success" -> confirmQrCode(node, container);
                case "active" -> store().callListeners(listener -> listener.onLoggedIn(keys().companion()));
                default -> throw new IllegalArgumentException("Cannot handle iq request, unknown description. %s%n".formatted(container.description()));
            }
        }

        private void confirmConnection() {
            Node.withChildren("iq", of("id", WhatsappUtils.buildRequestTag(options()), "to", ContactId.WHATSAPP, "xmlns", "passive", "type", "set"), Node.with("active"))
                    .toRequest()
                    .send(session(), keys(), store());
        }

        private void sendPreKeys() {
            if(keys().preKeys()){
                return;
            }

            keys().preKeys(true);
            Node.withChildren("iq", of("id", WhatsappUtils.buildRequestTag(options()), "xmlns", "encrypt", "type", "set", "to", ContactId.WHATSAPP), createPreKeysContent())
                    .toRequest()
                    .send(session(), keys(), store());
        }

        private Node[] createPreKeysContent() {
            return new Node[]{createPreKeysRegistration(), createPreKeysType(),
                    createPreKeysIdentity(), createPreKeys(), keys().signedKeyPair().encode()};
        }

        private Node createPreKeysIdentity() {
            return Node.with("identity", keys().identityKeyPair().publicKey());
        }

        private Node createPreKeysType() {
            return Node.with("type", "");
        }

        private Node createPreKeysRegistration() {
            return Node.with("registration", of(keys().id(), 4).data());
        }

        private Node createPreKeys(){
            var nodes = IntStream.range(0, 30)
                    .mapToObj(PreKey::fromIndex)
                    .map(PreKey::encode)
                    .toList();
            return Node.with("list", nodes);
        }

        private void generateQrCode(Node node, Node container) {
            printQrCode(container);
            sendConfirmNode(node, null);
            Executors.newSingleThreadScheduledExecutor()
                    .scheduleAtFixedRate(this::ping, 0L, 30L, TimeUnit.SECONDS);
        }

        private void ping() {
            Node.with("iq", of("id", WhatsappUtils.buildRequestTag(options()), "to", ContactId.WHATSAPP, "type", "get", "xmlns", "w:p"), Node.with("ping"))
                    .toRequest()
                    .send(session(), keys(), store());
        }

        private void printQrCode(Node container) {
            var qr = decodeQrCode(container);
            var matrix = Qr.generate(keys(), qr);
            if (!store.listeners().isEmpty()) {
                store().callListeners(listener -> listener.onQRCode(matrix));
                return;
            }

            Qr.print(matrix);
        }

        private String decodeQrCode(Node container) {
            return container.findNodeByDescription("ref")
                    .filter(ref -> ref.content() instanceof byte[])
                    .map(ref -> (byte[]) ref.content())
                    .map(String::new)
                    .orElseThrow(() -> new NoSuchElementException("Pairing error: missing qr code reference"));
        }

        @SneakyThrows
        private void confirmQrCode(Node node, Node container) {
               var userIdentity = fetchJid(container);
               keys().companion(userIdentity);

               var deviceIdentity = fetchDeviceIdentity(container);
               var advIdentity = ProtobufDecoder.forType(ADVSignedDeviceIdentityHMAC.class).decode(deviceIdentity);
               var advSign = Cipher.hmacSha256(of(advIdentity.details()), of(keys().companionKey()));
               Validate.isTrue(Arrays.equals(advIdentity.hmac(), advSign.data()), "Cannot login: Hmac validation failed!", SecurityException.class);

               var account = ProtobufDecoder.forType(ADVSignedDeviceIdentity.class).decode(advIdentity.details());
               var message = of(new byte[]{6, 0})
                       .append(account.details())
                       .append(keys().identityKeyPair().publicKey())
                       .data();
               Validate.isTrue(Cipher.verifySignature(account.accountSignatureKey(), message, account.accountSignature()),
                       "Cannot login: Hmac validation failed!", SecurityException.class);

               var deviceSignatureMessage = of(new byte[]{6, 1})
                       .append(account.details())
                       .append(keys().identityKeyPair().publicKey())
                       .append(account.accountSignature())
                       .data();
               var deviceSignature = Cipher.calculateSignature(keys().identityKeyPair().privateKey(), deviceSignatureMessage);

               var keyIndex = ProtobufDecoder.forType(ADVDeviceIdentity.class).decode(account.details()).keyIndex();
               var identity = encode(account.deviceSignature(deviceSignature).accountSignature(null));
               var identityNode = Node.with("device-identity", of("key-index", keyIndex), identity);
               var content = Node.withChildren("pair-device-sign", identityNode);

               sendConfirmNode(node, content);
        }

        private void sendConfirmNode(Node node, Node content) {
            Node.withChildren("iq", of("id", WhatsappUtils.readNullableId(node), "to", ContactId.WHATSAPP, "type", "result"), content)
                    .toRequest()
                    .send(session(), keys(), store());
        }

        private byte[] fetchDeviceIdentity(Node container) {
            return container.findNodeByDescription("device-identity")
                    .map(Node::content)
                    .filter(data -> data instanceof byte[])
                    .map(data -> (byte[]) data)
                    .orElseThrow(() -> new NoSuchElementException("Cannot find device identity body for authentication in %s".formatted(container)));
        }

        private ContactId fetchJid(Node container) {
            return container.findNodeByDescription("device")
                    .map(node -> (ContactId) node.attributes().get("jid"))
                    .orElseThrow(() -> new NoSuchElementException("Cannot find jid attribute in %s".formatted(container)));
        }
    }
}
