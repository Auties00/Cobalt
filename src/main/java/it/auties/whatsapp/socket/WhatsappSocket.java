package it.auties.whatsapp.socket;

import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.api.WhatsappOptions;
import it.auties.whatsapp.binary.BinaryMessage;
import it.auties.whatsapp.crypto.Curve;
import it.auties.whatsapp.crypto.Handshake;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.exchange.Node;
import it.auties.whatsapp.exchange.Request;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.media.MediaConnection;
import it.auties.whatsapp.protobuf.signal.auth.*;
import it.auties.whatsapp.protobuf.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.util.Messages;
import it.auties.whatsapp.util.Qr;
import it.auties.whatsapp.util.Validate;
import it.auties.whatsapp.util.WhatsappUtils;
import jakarta.websocket.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static it.auties.protobuf.encoder.ProtobufEncoder.encode;
import static it.auties.whatsapp.binary.BinaryArray.of;
import static it.auties.whatsapp.binary.BinaryArray.ofBase64;
import static it.auties.whatsapp.exchange.Node.with;
import static it.auties.whatsapp.exchange.Node.withChildren;
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
    private final Handshake handshake;
    private final WebSocketContainer container;
    private final WhatsappOptions options;
    private final WhatsappStore store;
    private final WhatsappKeys keys;
    private final Authenticator authenticator;
    private final StreamHandler handler;
    private CountDownLatch lock;

    public WhatsappSocket(@NonNull WhatsappOptions options, @NonNull WhatsappStore store, @NonNull WhatsappKeys keys) {
        this.handshake = new Handshake();
        this.container = ContainerProvider.getWebSocketContainer();
        this.options = options;
        this.store = store;
        this.keys = keys;
        this.authenticator = new Authenticator();
        this.handler = new StreamHandler();
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
                .sendWithPrologue(session(), keys(), store());
    }

    @OnMessage
    @SneakyThrows
    public void onBinary(byte @NonNull [] raw) {
        var message = new BinaryMessage(raw);
        if(message.length() == ERROR_CONSTANT){
            disconnect();
            return;
        }

        if(!loggedIn){
            authenticator.sendUserPayload(message.decoded().data());
            lock.countDown();
            return;
        }

        var oldCounter = store.readCounter().get();
        var deciphered = decipherMessage(message);
        var currentCounter = store.readCounter().get();
        if(currentCounter - oldCounter != 1){
            log.warning("Skipped %s IVs to decipher message with length %s".formatted(currentCounter - oldCounter - 1, message.length()));
        }

        System.out.printf("Received: %s%n", deciphered);
        if(store().resolvePendingRequest(deciphered, false)){
            return;
        }

        handler.digest(deciphered);
    }

    private Node decipherMessage(BinaryMessage message) {
        try {
            return message.toNode(keys.readKey(), store.readCounter().getAndIncrement());
        }catch (Throwable throwable){
            return decipherMessage(message);
        }
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
        if(loggedIn) {
            reconnect();
        }
        System.out.println("Closed");
    }

    @OnError
    public void onError(Throwable throwable){
        throwable.printStackTrace();
    }

    public CompletableFuture<Node> send(Node node){
        return node.toRequest()
                .send(session(), keys(), store());
    }

    public CompletableFuture<Node> sendWithNoResponse(Node node){
        return node.toRequest(false)
                .sendWithNoResponse(session(), keys(), store());
    }

    public CompletableFuture<Node> sendQuery(Map<String, Object> query, Node... body){
        return send(withChildren("iq", query, body));
    }

    public CompletableFuture<List<Node>> sendQuery(Node queryNode, Node... queryBody) {
        var query = withChildren("query", queryNode);
        var list = withChildren("list", queryBody);
        var sync = withChildren("usync",
                of("sid", WhatsappUtils.buildRequestTag(), "mode", "query", "last", "true", "index", "0", "context", "interactive"),
                query, list);
        return sendQuery(of("to", ContactJid.SOCKET, "type", "get", "xmlns", "usync"), sync)
                .thenApplyAsync(this::parseQueryResult);
    }

    private List<Node> parseQueryResult(Node result) {
        return result.findNodes("usync")
                .stream()
                .map(node -> node.findNode("list"))
                .map(node -> node.findNodes("user"))
                .flatMap(Collection::stream)
                .toList();
    }

    private class Authenticator {
        @SneakyThrows
        private void sendUserPayload(byte[] message) {
            var serverHello = ProtobufDecoder.forType(HandshakeMessage.class).decode(message).serverHello();
            handshake.updateHash(serverHello.ephemeral());
            var sharedEphemeral = Curve.calculateSharedSecret(serverHello.ephemeral(), keys.ephemeralKeyPair().privateKey());
            handshake.mixIntoKey(sharedEphemeral.data());

            var decodedStaticText = handshake.cipher(serverHello.staticText(), false);
            var sharedStatic = Curve.calculateSharedSecret(decodedStaticText, keys.ephemeralKeyPair().privateKey());
            handshake.mixIntoKey(sharedStatic.data());
            handshake.cipher(serverHello.payload(), false);

            var encodedKey = handshake.cipher(keys.companionKeyPair().publicKey(), true);
            var sharedPrivate = Curve.calculateSharedSecret(serverHello.ephemeral(), keys.companionKeyPair().privateKey());
            handshake.mixIntoKey(sharedPrivate.data());

            var encodedPayload = handshake.cipher(createUserPayload(), true);
            var clientFinish = new ClientFinish(encodedKey, encodedPayload);
            Request.with(new HandshakeMessage(clientFinish))
                    .sendWithNoResponse(session(), keys(), store());
            changeState(true);
            handshake.finish();
        }

        private byte[] createUserPayload() {
            var builder = ClientPayload.builder()
                    .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                    .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                    .userAgent(createUserAgent())
                    .passive(keys.hasCompanion())
                    .webInfo(new WebInfo(WebInfo.WebInfoWebSubPlatform.WEB_BROWSER));
            return encode(keys.hasCompanion() ? builder.username(parseLong(keys().companion().user())).device(keys().companion().device()).build()
                    : builder.regData(createRegisterData()).build());
        }

        private UserAgent createUserAgent() {
            Validate.isTrue(options.whatsappVersion().length == 3, "Invalid version: %s", Arrays.toString(options.whatsappVersion()));
            return UserAgent.builder()
                    .appVersion(new Version(options.whatsappVersion()[0], options.whatsappVersion()[1], options.whatsappVersion()[2]))
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
                    .signatureId(keys().signedKeyPair().encodedId())
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
    }

    private class StreamHandler {
        private void digest(@NonNull Node node) {
            switch (node.description()){
                case "iq" -> digestIq(node);
                case "ib" -> digestIb(node);
                case "success" -> digestSuccess();
                case "stream:error" -> digestError(node);
                case "failure" -> digestFailure(node);
                case "xmlstreamend" -> disconnect();
                case "message" -> System.out.println("Deciphered: " + Messages.decodeMessages(node, store, keys));
            }
        }

        private void digestIb(Node node) {
            Validate.isTrue(!node.hasNode("downgrade_webclient"),
                    "Multi device beta is not enabled. Please enable it from Whatsapp");
        }

        private void digestFailure(Node node) {
            Validate.isTrue(node.attributes().getLong("reason") == 401,
                    "Socket failed at %s with status code %s",
                    node.attributes().getString("location"), node.attributes().getLong("reason"));
            reconnect();
        }

        private void digestError(Node node) {
            switch (node.attributes().getInt("code")) {
                case 401 -> {
                    var child = node.children().getFirst();
                    var reason = child.attributes().getString("type");
                    disconnect();
                    throw new IllegalStateException("Conflict detected: %s".formatted(reason));
                }

                case 515 -> reconnect();

                default -> {
                    Validate.isTrue(node.findNode("xml-not-well-formed") == null,
                            "An invalid node was sent to Whatsapp");
                    node.children().forEach(error -> store.resolvePendingRequest(error, true));
                }
            }
        }

        private void digestSuccess() {
            sendPreKeys();
            confirmConnection();
            Executors.newSingleThreadScheduledExecutor()
                    .scheduleAtFixedRate(() -> sendQuery(of("xmlns", "w:p", "to", ContactJid.SOCKET, "type", "get"), with("ping")), 0L, 20L, TimeUnit.SECONDS);
            store.callListeners(WhatsappListener::onLoggedIn);
        }

        private void createMediaConnection(){
            if(!loggedIn){
                return;
            }

            sendQuery(of("xmlns", "w:m", "to", ContactJid.SOCKET, "type", "set"), with("media_conn"))
                    .thenApplyAsync(MediaConnection::ofNode)
                    .thenApplyAsync(this::scheduleMediaConnection)
                    .thenApplyAsync(store::mediaConnection);
        }

        private MediaConnection scheduleMediaConnection(MediaConnection connection) {
            CompletableFuture.delayedExecutor(connection.ttl(), TimeUnit.SECONDS)
                    .execute(this::createMediaConnection);
            return connection;
        }

        private void digestIq(Node node) {
            var children = node.children();
            if(children.isEmpty()){
                return;
            }

            var container = children.getFirst();
            switch (container.description()){
                case "pair-device" -> generateQrCode(node, container);
                case "pair-success" -> confirmQrCode(node, container);
                default -> throw new IllegalArgumentException("Cannot handle iq request, unknown description. %s%n".formatted(container.description()));
            }
        }

        private void confirmConnection() {
            sendQuery(of("xmlns", "passive", "to", ContactJid.SOCKET, "type", "set"), with("active"))
                    .thenRunAsync(this::createMediaConnection);
        }

        private void sendPreKeys() {
            if(keys().hasPreKeys()){
                return;
            }

            sendQuery(of("xmlns", "encrypt", "type", "set", "to", ContactJid.SOCKET), createPreKeysContent());
        }

        private Node[] createPreKeysContent() {
            return new Node[]{createPreKeysRegistration(), createPreKeysType(),
                    createPreKeysIdentity(), createPreKeys(), keys().signedKeyPair().toNode()};
        }

        private Node createPreKeysIdentity() {
            return with("identity", keys().identityKeyPair().publicKey());
        }

        private Node createPreKeysType() {
            return with("type", "");
        }

        private Node createPreKeysRegistration() {
            return with("registration", of(keys().id(), 4).data());
        }

        private Node createPreKeys(){
            var nodes = IntStream.range(0, 30)
                    .mapToObj(SignalPreKeyPair::ofIndex)
                    .peek(keys.preKeys()::add)
                    .map(SignalPreKeyPair::toNode)
                    .toList();

            return with("list", nodes);
        }

        private void generateQrCode(Node node, Node container) {
            printQrCode(container);
            sendConfirmNode(node, null);
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
            var bytes = (byte[]) container.findNode("ref").content();
            return new String(bytes, StandardCharsets.UTF_8);
        }

        @SneakyThrows
        private void confirmQrCode(Node node, Node container) {
            saveCompanion(container);

            var advIdentity = ProtobufDecoder.forType(SignedDeviceIdentityHMAC.class)
                    .decode(container.findNode("device-identity").bytes());
            var advSign = Hmac.calculate(advIdentity.details(), keys().companionKey());
            Validate.isTrue(Arrays.equals(advIdentity.hmac(), advSign.data()), "Cannot login: Hmac validation failed!", SecurityException.class);

            var account = ProtobufDecoder.forType(SignedDeviceIdentity.class).decode(advIdentity.details());
            var message = of(new byte[]{6, 0})
                    .append(account.details())
                    .append(keys().identityKeyPair().publicKey())
                    .data();
            Validate.isTrue(Curve.verifySignature(account.accountSignatureKey(), message, account.accountSignature()),
                    "Cannot login: Hmac validation failed!", SecurityException.class);

            var deviceSignatureMessage = of(new byte[]{6, 1})
                    .append(account.details())
                    .append(keys().identityKeyPair().publicKey())
                    .append(account.accountSignature())
                    .data();
            var deviceSignature = Curve.calculateSignature(keys().identityKeyPair().privateKey(), deviceSignatureMessage);

            var keyIndex = ProtobufDecoder.forType(DeviceIdentity.class).decode(account.details()).keyIndex();
            var identity = encode(account.deviceSignature(deviceSignature).accountSignature(null));
            var identityNode = with("device-identity", of("key-index", keyIndex), identity);
            var content = withChildren("pair-device-sign", identityNode);

            sendConfirmNode(node, content);
        }

        private void sendConfirmNode(Node node, Node content) {
            sendQuery(of("id", WhatsappUtils.readNullableId(node), "to", ContactJid.SOCKET, "type", "result"), content);
        }

        private void saveCompanion(Node container) {
            var node = Objects.requireNonNull(container.findNode("device"));
            var companion = node.attributes().getJid("jid")
                    .orElseThrow(() -> new NoSuchElementException("Missing identity jid for session"));
            keys.companion(companion)
                    .save();
        }
    }
}
