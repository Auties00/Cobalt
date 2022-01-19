package it.auties.whatsapp.socket;

import io.netty.buffer.ByteBuf;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.api.WhatsappOptions;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.binary.BinaryDecoder;
import it.auties.whatsapp.binary.BinaryMessage;
import it.auties.whatsapp.crypto.*;
import it.auties.whatsapp.exchange.Node;
import it.auties.whatsapp.exchange.Request;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.media.MediaConnection;
import it.auties.whatsapp.protobuf.signal.auth.*;
import it.auties.whatsapp.protobuf.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.util.*;
import jakarta.websocket.*;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
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
import static it.auties.whatsapp.binary.BinaryArray.ofBase64;
import static it.auties.whatsapp.exchange.Node.with;
import static it.auties.whatsapp.exchange.Node.withChildren;
import static it.auties.whatsapp.util.QrHandler.TERMINAL;
import static jakarta.websocket.ContainerProvider.getWebSocketContainer;
import static java.lang.Long.parseLong;
import static java.util.Map.of;

@Data
@Accessors(fluent = true)
@ClientEndpoint(configurator = WhatsappSocketConfiguration.class)
@Log
public class WhatsappSocket {
    private static final int ERROR_CONSTANT = 8913411;
    private static final String BUILD_HASH = "S9Kdc4pc4EJryo21snc5cg==";
    private static final int KEY_TYPE = 5;

    private @Getter(onMethod = @__(@NonNull)) Session session;
    private boolean loggedIn;
    private final Handshake handshake;
    private final WebSocketContainer container;
    private final WhatsappOptions options;
    private final WhatsappStore store;
    private final Authenticator authenticator;
    private final StreamHandler handler;
    private CountDownLatch lock;
    private WhatsappKeys keys;

    public WhatsappSocket(@NonNull WhatsappOptions options, @NonNull WhatsappStore store, @NonNull WhatsappKeys keys) {
        this.handshake = new Handshake();
        this.container = getWebSocketContainer();
        this.options = options;
        this.store = store;
        this.keys = keys;
        this.authenticator = new Authenticator();
        this.handler = new StreamHandler();
        this.lock = new CountDownLatch(1);
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
        var clientHello = new ClientHello(keys.ephemeralKeyPair().publicKey());
        var handshakeMessage = new HandshakeMessage(clientHello);
        Request.with(handshakeMessage)
                .sendWithPrologue(session(), keys(), store());
    }


    @OnMessage
    @SneakyThrows
    public void onBinary(byte @NonNull [] raw) {
        var message = new BinaryMessage(raw);
        if(message.decoded().isEmpty()){
            return;
        }

        var header = message.decoded().getFirst();
        if(header.size() == ERROR_CONSTANT){
            disconnect();
            return;
        }

        if(!loggedIn){
            authenticator.sendUserPayload(header.data());
            return;
        }

        System.out.printf("Received %s nodes%n", message.decoded().size());
        message.toNodes(keys)
                .forEach(this::handleNode);
    }

    private void handleNode(Node deciphered) {
        System.out.printf("Received: %s%n", deciphered);
        if(store().resolvePendingRequest(deciphered, false)){
            System.out.println("Handled!");
            return;
        }

        System.out.println("Processing");
        handler.digest(deciphered);
    }

    public void connect() {
        try{
            container.connectToServer(this, URI.create(options.whatsappUrl()));
            lock.await();
        }catch (IOException | DeploymentException | InterruptedException exception){
            throw new RuntimeException("Cannot connect to WhatsappWeb's WebServer", exception);
        }
    }

    public void reconnect(){
        disconnect();
        connect();
    }

    public void disconnect(){
        try{
            changeState(false);
            session.close();
        }catch (IOException exception){
            throw new RuntimeException("Cannot close connection to WhatsappWeb's WebServer", exception);
        }
    }

    public void logout(){
        changeKeys();
        if (!keys().hasCompanion()) {
            return;
        }

        var metadata = of("jid", keys().companion(), "reason", "user_initiated");
        var device = with("remove-companion-device", metadata, null);
        sendQuery("set", "md", device);
    }

    private void changeState(boolean loggedIn){
        this.loggedIn = loggedIn;
        this.lock = new CountDownLatch(1);
        keys().clear();
    }

    @OnClose
    public void onClose(){
        System.out.println("Closed");
        reconnect();
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

    public CompletableFuture<Node> sendQuery(String method, String category, Node... body){
        return sendQuery(null, ContactJid.SOCKET, method, category, null, body);
    }

    public CompletableFuture<Node> sendQuery(String method, String category, Map<String, Object> metadata, Node... body){
        return sendQuery(null, ContactJid.SOCKET, method, category, metadata, body);
    }

    public CompletableFuture<Node> sendQuery(ContactJid to, String method, String category, Node... body){
        return sendQuery(null, to, method, category, null, body);
    }

    public CompletableFuture<Node> sendQuery(String id, ContactJid to, String method, String category, Map<String, Object> metadata, Node... body){
        var attributes = new HashMap<String, Object>();
        if(id != null){
            attributes.put("id", id);
        }

        attributes.put("type", method);
        attributes.put("to", to);
        if(category != null) {
            attributes.put("xmlns", category);
        }

        if(metadata != null){
            attributes.putAll(metadata);
        }

        return send(withChildren("iq", attributes, body));
    }

    public CompletableFuture<List<Node>> sendQuery(Node queryNode, Node... queryBody) {
        var query = withChildren("query", queryNode);
        var list = withChildren("list", queryBody);
        var sync = withChildren("usync",
                of("sid", WhatsappUtils.buildRequestTag(), "mode", "query", "last", "true", "index", "0", "context", "interactive"),
                query, list);
        return sendQuery("get", "usync", sync)
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

    private void changeKeys() {
        keys.delete();
        keys(WhatsappKeys.random());
    }

    private class Authenticator {
        @SneakyThrows
        private void sendUserPayload(byte[] message) {
            var serverHello = ProtobufDecoder.forType(HandshakeMessage.class)
                    .decode(message)
                    .serverHello();
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
            var handshakeMessage = new HandshakeMessage(clientFinish);
            Request.with(handshakeMessage)
                    .sendWithNoResponse(session(), keys(), store())
                    .thenRunAsync(() -> changeState(true))
                    .thenRunAsync(handshake::finish);
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
            return UserAgent.builder()
                    .appVersion(new Version(options.whatsappVersion()))
                    .platform(UserAgent.UserAgentPlatform.WEB)
                    .releaseChannel(UserAgent.UserAgentReleaseChannel.RELEASE)
                    .build();
        }

        private CompanionData createRegisterData() {
            return CompanionData.builder()
                    .buildHash(ofBase64(BUILD_HASH).data())
                    .companion(encode(createCompanionProps()))
                    .id(SignalHelper.toBytes(keys().id(), 4))
                    .keyType(SignalHelper.toBytes(KEY_TYPE, 1))
                    .identifier(keys().identityKeyPair().publicKey())
                    .signatureId(keys().signedKeyPair().encodedId())
                    .signaturePublicKey(keys().signedKeyPair().keyPair().publicKey())
                    .signature(keys().signedKeyPair().signature())
                    .build();
        }

        private Companion createCompanionProps() {
            return Companion.builder()
                    .os(options.description())
                    .platformType(Companion.CompanionPropsPlatformType.DESKTOP)
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
                case "message" -> Messages.decodeMessages(node, store, keys);
            }
        }

        private void digestIb(Node node) {
            var offlinePreview = node.findNode("offline_preview");
            if(offlinePreview == null){
                return;
            }

            Validate.isTrue(!node.hasNode("downgrade_webclient"),
                    "Multi device beta is not enabled. Please enable it from Whatsapp");
        }

        private void digestFailure(Node node) {
            var statusCode = node.attributes().getLong("reason");
            var reason = node.attributes().getString("location");
            Validate.isTrue(handleFailure(statusCode, reason),
                    "Invalid or expired credentials: socket failed with status code %s at %s",
                    statusCode, reason);
            changeKeys();
            reconnect();
        }

        private boolean handleFailure(long statusCode, String reason) {
            return store.listeners()
                    .stream()
                    .allMatch(listener -> listener.onFailure(statusCode, reason));
        }

        private void digestError(Node node) {
            if (node.attributes().getInt("code") == 401) {
                var child = node.children().getFirst();
                var reason = child.attributes().getString("type");
                disconnect();
                throw new IllegalStateException("Conflict detected: %s".formatted(reason));
            }

            Validate.isTrue(node.findNode("xml-not-well-formed") == null, "An invalid node was sent to Whatsapp");
            node.children().forEach(error -> store.resolvePendingRequest(error, true));
        }

        private void digestSuccess() {
            sendPreKeys();
            confirmConnection();
            createPingService();
            sendStatusUpdate();
            store.callListeners(WhatsappListener::onLoggedIn);
        }

        private void sendStatusUpdate() {
            var presence = with("presence", of("type", "available"), null);
            send(presence);
        }

        private void createPingService() {
            var scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                if(loggedIn){
                    scheduler.shutdown();
                    return;
                }

                sendQuery("get", "w:p", with("ping"));
            }, 0L, 20L, TimeUnit.SECONDS);
        }

        private void createMediaConnection(){
            if(!loggedIn){
                return;
            }

            sendQuery("set", "w:m", with("media_conn"))
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
            sendQuery("set", "passive", with("active"))
                    .thenRunAsync(this::createMediaConnection);
        }

        private void sendPreKeys() {
            if(keys().hasPreKeys()){
                return;
            }

            sendQuery("set", "encrypt", createPreKeysContent());
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
            return with("registration", SignalHelper.toBytes(keys().id(), 4));
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
            var ref = container.findNode("ref");
            var qr = new String(ref.bytes(), StandardCharsets.UTF_8);
            var matrix = Qr.generate(keys(), qr);
            if (!store.listeners().isEmpty()) {
                store().callListeners(listener -> listener.onQRCode(matrix).accept(matrix));
                return;
            }

            TERMINAL.accept(matrix);
        }

        @SneakyThrows
        private void confirmQrCode(Node node, Node container) {
            lock.countDown();
            saveCompanion(container);

            var deviceIdentity = Objects.requireNonNull(container.findNode("device-identity"), "Missing device identity");
            var advIdentity = ProtobufDecoder.forType(SignedDeviceIdentityHMAC.class)
                    .decode(deviceIdentity.bytes());
            var advSign = Hmac.calculate(advIdentity.details(), keys().companionKey());
            Validate.isTrue(Arrays.equals(advIdentity.hmac(), advSign.data()), "Cannot login: Hmac validation failed!", SecurityException.class);

            var account = ProtobufDecoder.forType(SignedDeviceIdentity.class)
                    .decode(advIdentity.details());
            var message = BinaryArray.of(new byte[]{6, 0})
                    .append(account.details())
                    .append(keys().identityKeyPair().publicKey())
                    .data();
            Validate.isTrue(Curve.verifySignature(account.accountSignatureKey(), message, account.accountSignature()),
                    "Cannot login: Hmac validation failed!", SecurityException.class);

            var deviceSignatureMessage = BinaryArray.of(new byte[]{6, 1})
                    .append(account.details())
                    .append(keys().identityKeyPair().publicKey())
                    .append(account.accountSignature())
                    .data();
            var deviceSignature = Curve.calculateSignature(keys().identityKeyPair().privateKey(), deviceSignatureMessage);

            var keyIndex = ProtobufDecoder.forType(DeviceIdentity.class)
                    .decode(account.details())
                    .keyIndex();
            var identity = ProtobufEncoder.encode(account.deviceSignature(deviceSignature).accountSignature(null));
            var identityNode = with("device-identity", of("key-index", keyIndex), identity);
            var content = withChildren("pair-device-sign", identityNode);

            sendConfirmNode(node, content);
        }

        private void sendConfirmNode(Node node, Node content) {
            var id = WhatsappUtils.readNullableId(node);
            sendQuery(id, ContactJid.SOCKET, "result", null, of(), content);
        }

        private void saveCompanion(Node container) {
            var node = Objects.requireNonNull(container.findNode("device"), "Missing device");
            var companion = node.attributes().getJid("jid")
                    .orElseThrow(() -> new NoSuchElementException("Missing companion"));
            keys.companion(companion)
                    .save();
        }
    }
}
