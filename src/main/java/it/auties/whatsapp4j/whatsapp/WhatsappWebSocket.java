package it.auties.whatsapp4j.whatsapp;

import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.binary.BinaryDecoder;
import it.auties.whatsapp4j.binary.BinaryFlag;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.chat.GroupAction;
import it.auties.whatsapp4j.protobuf.chat.GroupPolicy;
import it.auties.whatsapp4j.protobuf.chat.GroupSetting;
import it.auties.whatsapp4j.protobuf.contact.Contact;
import it.auties.whatsapp4j.protobuf.contact.ContactStatus;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.protobuf.model.Node;
import it.auties.whatsapp4j.request.impl.*;
import it.auties.whatsapp4j.request.model.BinaryRequest;
import it.auties.whatsapp4j.response.impl.binary.ChatResponse;
import it.auties.whatsapp4j.response.impl.json.*;
import it.auties.whatsapp4j.response.model.binary.BinaryResponse;
import it.auties.whatsapp4j.response.model.common.Response;
import it.auties.whatsapp4j.response.model.json.JsonListResponse;
import it.auties.whatsapp4j.response.model.json.JsonResponse;
import it.auties.whatsapp4j.utils.internal.CypherUtils;
import it.auties.whatsapp4j.utils.internal.Validate;
import it.auties.whatsapp4j.utils.internal.WhatsappQRCode;
import lombok.NonNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

import static it.auties.whatsapp4j.utils.WhatsappUtils.*;
import static it.auties.whatsapp4j.utils.internal.CypherUtils.*;

/**
 * This class is an interface between this API and WhatsappWeb's WebClient.
 * This methods should not be used by any project, excluding obviously WhatsappWeb4j.
 * Instead, {@link WhatsappAPI} should be used.
 */
@RequiredArgsConstructor
@Data
@Accessors(fluent = true)
public class WhatsappWebSocket implements WebSocket.Listener {
    private WebSocket socket;
    private boolean loggedIn;
    private final @NonNull ScheduledExecutorService pingService;
    private final @NonNull WhatsappDataManager whatsappManager;
    private final @NonNull WhatsappKeysManager whatsappKeys;
    private final @NonNull WhatsappConfiguration options;
    private final @NonNull WhatsappQRCode qrCode;
    private final @NonNull BinaryDecoder decoder;

    public WhatsappWebSocket(@NonNull WhatsappConfiguration options) {
        this(Executors.newSingleThreadScheduledExecutor(), WhatsappDataManager.singletonInstance(), WhatsappKeysManager.singletonInstance(), options, new WhatsappQRCode(), new BinaryDecoder());
    }

    @Override
    public void onOpen(WebSocket socket) {
        System.out.println(socket);
        if (this.socket == null) {
            socket(socket);
        }

        new InitialRequest<InitialResponse>(options, whatsappKeys) {}
                .send(socket())
                .thenAccept(this::handleInitialMessage);
    }

    private void handleInitialMessage(@NonNull InitialResponse response) {
        if (!whatsappKeys.mayRestore()) {
            generateQrCode(response);
        }

        new TakeOverRequest<TakeOverResponse>(options, whatsappKeys) {}
                .send(socket())
                .thenAccept(this::solveChallenge);
    }

    private void generateQrCode(@NonNull InitialResponse response) {
        if(loggedIn){
            return;
        }

        Validate.isTrue(response.status() != 429, "Out of attempts to scan the QR code", IllegalStateException.class);
        CompletableFuture.delayedExecutor(response.ttl(), TimeUnit.MILLISECONDS).execute(() -> generateQrCode(response));
        Validate.isTrue(response.ref() != null, "Cannot find ref for QR code generation");
        qrCode.generateAndPrint(response.ref(), extractRawPublicKey(whatsappKeys.keyPair().getPublic()), whatsappKeys.clientId());
    }

    private void solveChallenge(@NonNull TakeOverResponse response) {
        if (response.status() >= 400) {
            whatsappKeys.deleteKeysFromMemory();
            disconnect(null, false, true);
            return;
        }

        sendChallenge(response);
    }

    private void sendChallenge(@NonNull TakeOverResponse response) {
        var challengeBase64 = response.challenge();
        if (challengeBase64 == null) {
            return;
        }

        var challenge = BinaryArray.forBase64(challengeBase64);
        var signedChallenge = hmacSha256(challenge, Objects.requireNonNull(whatsappKeys.macKey()));

        var request = new SolveChallengeRequest<SimpleStatusResponse>(options, whatsappKeys, signedChallenge) {};
        request.send(socket()).thenAccept(result -> Validate.isTrue(result.status() == 200, "Could not solve whatsapp challenge for server and client token renewal: %s".formatted(result)));
    }

    private void login(@NonNull UserInformationResponse response) {
        var base64Secret = response.secret();
        var secret = BinaryArray.forBase64(base64Secret);
        var pubKey = secret.cut(32);
        var sharedSecret = calculateSharedSecret(pubKey.data(), whatsappKeys.keyPair().getPrivate());
        var sharedSecretExpanded = hkdfExpand(sharedSecret, 80);

        var hmacValidation = hmacSha256(secret.cut(32).merged(secret.slice(64)), sharedSecretExpanded.slice(32, 64));
        Validate.isTrue(hmacValidation.equals(secret.slice(32, 64)), "Cannot login: Hmac validation failed!", SecurityException.class);

        var keysEncrypted = sharedSecretExpanded.slice(64).merged(secret.slice(64));
        var key = sharedSecretExpanded.cut(32);
        var keysDecrypted = aesDecrypt(keysEncrypted, key);

        whatsappKeys.initializeKeys(response.serverToken(), response.clientToken(), keysDecrypted.cut(32), keysDecrypted.slice(32, 64));
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, @NonNull CharSequence data, boolean last) {
        handleTextMessage(data);
        return null;
    }

    private void handleTextMessage(@NonNull CharSequence data) {
        var response = Response.fromTaggedResponse(String.valueOf(data));
        if (response instanceof JsonListResponse listResponse) {
            handleList(listResponse);
            return;
        }

        var mapResponse = (JsonResponse) response;
        if (mapResponse.content().isEmpty()) {
            return;
        }

        if (whatsappManager.resolvePendingRequest(response.tag(), mapResponse)) {
            return;
        }

        if (response.description() == null) {
            return;
        }

        switch (response.description()) {
            case "Conn" -> handleUserInformation(mapResponse.toModel(UserInformationResponse.class));
            case "Blocklist" -> handleBlocklist(mapResponse.toModel(BlocklistResponse.class));
            case "Cmd" -> handleCmd(mapResponse);
            case "Props" -> handleProps(mapResponse.toModel(PropsResponse.class));
            case "Presence" -> handlePresence(mapResponse.toModel(PresenceResponse.class));
            case "Msg", "MsgInfo" -> handleMessageInfo(mapResponse.toModel(AckResponse.class));
            case "Chat" -> handleChatCmd(mapResponse.toModel(ChatCmdResponse.class));
        }
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, @NonNull ByteBuffer msg, boolean last) {
        Validate.isTrue(msg.get(0) != '!', "Server pong from whatsapp, why did this get through?");

        var binaryMessage = BinaryArray.forArray(msg.array());
        var tagAndMessagePair = binaryMessage.indexOf(',').map(binaryMessage::split).orElseThrow();

        var messageTag = tagAndMessagePair.key().toString();
        var messageContent = tagAndMessagePair.value();

        var message = messageContent.slice(32);
        var hmacValidation = CypherUtils.hmacSha256(message, Objects.requireNonNull(whatsappKeys.macKey()));
        Validate.isTrue(hmacValidation.equals(messageContent.cut(32)), "Cannot read message: Hmac validation failed!", SecurityException.class);

        var decryptedMessage = CypherUtils.aesDecrypt(message, Objects.requireNonNull(whatsappKeys.encKey()));
        var response = new BinaryResponse(messageTag, decoder.decodeDecryptedMessage(decryptedMessage));
        if (whatsappManager.resolvePendingRequest(response.tag(), response)) {
            return null;
        }

        whatsappManager.digestWhatsappNode(this, response.content());
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, @NonNull Throwable error) {
        error.printStackTrace();
    }

    public void connect() {
        Validate.isTrue(!loggedIn, "WhatsappAPI: Cannot establish a connection with whatsapp as one already exists", IllegalStateException.class);
        System.out.println("Connect");
        openConnection();
        pingService.scheduleAtFixedRate(this::sendPing, 0, 1, TimeUnit.MINUTES);
    }

    @SneakyThrows
    public void disconnect(String reason, boolean logout, boolean reconnect) {
        Validate.isTrue(loggedIn, "WhatsappAPI: Cannot terminate the connection with whatsapp as it doesn't exist", IllegalStateException.class);
        whatsappManager.clear();
        if (logout) new LogOutRequest(options) {}.send(socket()).thenRun(whatsappKeys::deleteKeysFromMemory);
        socket().sendClose(WebSocket.NORMAL_CLOSURE, reason);
        socket(null);
        whatsappManager.listeners().forEach(WhatsappListener::onDisconnected);
        if (reconnect) openConnection();
    }

    @SneakyThrows
    private void openConnection() {
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .header("Origin", "https://web.whatsapp.com")
                .header("Host", "web.whatsapp.com")
                .connectTimeout(Duration.of(30, ChronoUnit.SECONDS))
                .buildAsync(URI.create(options.whatsappUrl()), this)
                .exceptionallyAsync(this::handleConnectionError);
    }

    private WebSocket handleConnectionError(Throwable ex) {
        throw new RuntimeException("Cannot open connection with WhatsappWeb", ex);
    }

    @SneakyThrows
    private void sendPing() {
        var request = socket.sendPing(ByteBuffer.allocate(0));
        if (options.async()) {
            return;
        }

        request.get();
    }

    @SneakyThrows
    private void handleChatCmd(@NonNull ChatCmdResponse cmdResponse) {
        if (cmdResponse.cmd() == null) {
            return;
        }

        var chatOpt = whatsappManager.findChatByJid(cmdResponse.jid());
        if (chatOpt.isEmpty()) {
            return;
        }

        var chat = chatOpt.get();
        var node = Node.fromList(cmdResponse.data());
        var content = (String) node.content();

        switch (node.description()) {
            case "restrict" -> notifyGroupSettingChange(chat, GroupSetting.EDIT_GROUP_INFO, content);
            case "announce" -> notifyGroupSettingChange(chat, GroupSetting.SEND_MESSAGES, content);
            case "add", "remove", "promote", "demote" -> notifyGroupAction(chat, node, content);
            case "ephemeral" -> updateAndNotifyEphemeralStatus(chat, content);
            case "desc_add" -> notifyGroupDescriptionChange(chat, content);
            case "subject" -> updateAndNotifyGroupSubject(chat, content);
        }
    }

    private void notifyGroupDescriptionChange(@NonNull Chat chat, @NonNull String content) {
        var response = JsonResponse.fromJson(content).toModel(DescriptionChangeResponse.class);
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onGroupDescriptionChange(chat, response.description(), response.descriptionId())));
    }

    private void updateAndNotifyGroupSubject(@NonNull Chat chat, @NonNull String content) {
        var response = JsonResponse.fromJson(content).toModel(SubjectChangeResponse.class);
        chat.displayName(response.subject());
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onGroupSubjectChange(chat)));
    }

    private void updateAndNotifyEphemeralStatus(@NonNull Chat chat, @NonNull String content) {
        chat.ephemeralMessageDuration(Long.parseLong(content));
        chat.ephemeralMessagesToggleTime(ZonedDateTime.now().toEpochSecond());
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onChatEphemeralStatusChange(chat)));
    }

    private void notifyGroupAction(@NonNull Chat chat, @NonNull Node node, @NonNull String content) {
        JsonResponse.fromJson(content)
                .toModel(GroupActionResponse.class)
                .participants()
                .stream()
                .map(whatsappManager::findContactByJid)
                .map(Optional::orElseThrow)
                .forEach(contact -> whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onGroupAction(chat, contact, GroupAction.valueOf(node.description().toUpperCase())))));
    }

    private void notifyGroupSettingChange(@NonNull Chat chat, @NonNull GroupSetting setting, @NonNull String content) {
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onGroupSettingsChange(chat, setting, GroupPolicy.forData(Boolean.parseBoolean(content)))));
    }

    private void handleMessageInfo(@NonNull AckResponse ackResponse) {
        if (ackResponse.cmd() == null) {
            return;
        }

        var to = whatsappManager.findContactByJid(ackResponse.participant() != null ? ackResponse.participant() : ackResponse.to());
        if (to.isEmpty()) {
            return;
        }

        var chat = whatsappManager.findChatByJid(ackResponse.to());
        if (chat.isEmpty()) {
            return;
        }

        Arrays.stream(ackResponse.ids())
                .map(id -> whatsappManager.findMessageById(chat.get(), id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(message -> updateAndNotifyMessageReadStatusChange(ackResponse, to.get(), chat.get(), message));
    }

    private void updateAndNotifyMessageReadStatusChange(@NonNull AckResponse ackResponse, @NonNull Contact to, @NonNull Chat chat, MessageInfo message) {
        message.individualReadStatus().put(to, MessageInfo.MessageInfoStatus.forIndex(ackResponse.ack()));
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onMessageReadStatusUpdate(chat, to, message)));
    }

    private void handleUserInformation(@NonNull UserInformationResponse info) {
        if (info.ref() == null) {
            whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onInformationUpdate(info)));
            return;
        }

        Validate.isTrue(info.connected(), "WhatsappAPI: Cannot establish a connection with WhatsappWeb");
        if (!whatsappKeys.mayRestore()) {
            login(info);
        }

        configureSelfContact(info);
        scheduleMediaConnection(0);
        loggedIn(true);
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onLoggedIn(info)));
    }

    private void configureSelfContact(@NonNull UserInformationResponse info) {
        var jid = parseJid(info.wid());
        whatsappManager.contacts().add(new Contact(jid, null, null, null, null, null));
        whatsappManager.phoneNumberJid(jid);
    }

    private void scheduleMediaConnection(int delay) {
        CompletableFuture.delayedExecutor(delay, TimeUnit.SECONDS).execute(this::createMediaConnection);
    }

    @SneakyThrows
    private void createMediaConnection() {
        var connection = new MediaConnectionRequest<MediaConnectionResponse>(options) {}.send(socket()).get().connection();
        whatsappManager.mediaConnection(connection);
        scheduleMediaConnection(connection.ttl());
    }

    private void handleBlocklist(@NonNull BlocklistResponse blocklist) {
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onBlocklistUpdate(blocklist)));
    }

    private void handleProps(@NonNull PropsResponse props) {
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onPropsUpdate(props)));
    }

    private void handleCmd(@NonNull JsonResponse res) {
        if (!res.hasKey("type") || !res.hasKey("kind")) {
            return;
        }

        var kind = res.getString("kind").orElse("unknown");
        disconnect(kind, false, options.reconnectWhenDisconnected().apply(kind));
    }

    private void handlePresence(@NonNull PresenceResponse res) {
        var chatOpt = whatsappManager.findChatByJid(res.jid());
        if (chatOpt.isEmpty()) {
            return;
        }

        var chat = chatOpt.get();
        if (chat.isGroup()) {
            if (res.participant() == null) {
                return;
            }

            var participantOpt = whatsappManager.findContactByJid(res.participant());
            if (participantOpt.isEmpty()) {
                return;
            }

            var participant = participantOpt.get();
            chat.presences().put(participant, res.presence());
            whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onContactPresenceUpdate(chat, participant)));
            return;
        }

        var contactOpt = whatsappManager.findContactByJid(res.jid());
        if (contactOpt.isEmpty()) {
            return;
        }

        var contact = contactOpt.get();
        if (res.offsetFromLastSeen() != null) {
            var instant = Instant.ofEpochSecond(res.offsetFromLastSeen() + contact.lastSeen().map(e -> e.toInstant().getEpochSecond()).orElse(0L));
            contact.lastSeen(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
        } else if (res.presence() == ContactStatus.UNAVAILABLE && contact.lastKnownPresence().isPresent() && (contact.lastKnownPresence().get() == ContactStatus.AVAILABLE || contact.lastKnownPresence().get() == ContactStatus.COMPOSING)) {
            contact.lastSeen(ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
        }

        contact.lastKnownPresence(res.presence());
        chat.presences().put(contact, res.presence());
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onContactPresenceUpdate(chat, contact)));
    }

    private void handleList(@NonNull JsonListResponse response) {
        whatsappManager.callOnListenerThread(() -> whatsappManager.listeners().forEach(whatsappListener -> whatsappListener.onListResponse(response.content())));
    }

    public @NonNull WebSocket socket() {
        return Objects.requireNonNull(socket, "WhatsappAPI: The session linked to the WebSocket is null");
    }

    public @NonNull CompletableFuture<ChatResponse> queryChat(@NonNull String jid) {
        var node = new Node("query", attributes(attr("type", "chat"), attr("jid", jid)), null);
        return new BinaryRequest<ChatResponse>(options, node, BinaryFlag.IGNORE, BinaryMetric.QUERY_CHAT) {}.send(socket());
    }
}