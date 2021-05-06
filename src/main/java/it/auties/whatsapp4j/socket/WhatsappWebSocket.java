package it.auties.whatsapp4j.socket;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.binary.BinaryFlag;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.model.*;
import it.auties.whatsapp4j.request.impl.*;
import it.auties.whatsapp4j.request.model.BinaryRequest;
import it.auties.whatsapp4j.response.impl.binary.ChatResponse;
import it.auties.whatsapp4j.response.impl.json.*;
import it.auties.whatsapp4j.response.model.binary.BinaryResponse;
import it.auties.whatsapp4j.response.model.json.JsonListResponse;
import it.auties.whatsapp4j.response.model.json.JsonResponse;
import it.auties.whatsapp4j.response.model.common.Response;
import it.auties.whatsapp4j.utils.Validate;
import it.auties.whatsapp4j.utils.WhatsappQRCode;
import jakarta.validation.constraints.NotNull;
import jakarta.websocket.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static it.auties.whatsapp4j.utils.CypherUtils.*;
import static it.auties.whatsapp4j.utils.WhatsappUtils.*;

/**
 * This class is an interface between this API and WhatsappWeb's WebClient.
 * This methods should not be used by any project, excluding obviously WhatsappWeb4j.
 * Instead, {@link WhatsappAPI} should be used.
 */
@ClientEndpoint(configurator = WhatsappSocketConfiguration.class, decoders = {WhatsappJsonMessageDecoder.class, WhatsappBinaryMessageDecoder.class}, encoders = {WhatsappJsonMessageEncoder.class, WhatsappBinaryMessageEncoder.class})
@RequiredArgsConstructor
@Data
@Accessors(fluent = true)
public class WhatsappWebSocket {
    private Session session;
    private boolean loggedIn;
    private final @NotNull ScheduledExecutorService pingService;
    private final @NotNull WebSocketContainer webSocketContainer;
    private final @NotNull WhatsappDataManager whatsappManager;
    private final @NotNull WhatsappKeysManager whatsappKeys;
    private final @NotNull WhatsappConfiguration options;
    private final @NotNull WhatsappQRCode qrCode;

    public WhatsappWebSocket(@NotNull WhatsappConfiguration options) {
        this(Executors.newSingleThreadScheduledExecutor(), ContainerProvider.getWebSocketContainer(), WhatsappDataManager.singletonInstance(), WhatsappKeysManager.singletonInstance(), options, new WhatsappQRCode());
        webSocketContainer.setDefaultMaxSessionIdleTimeout(Duration.ofDays(30).toMillis());
    }

    @OnOpen
    public void onOpen(@NotNull Session session) {
        if (this.session == null || !this.session.isOpen()) session(session);
        new InitialRequest<InitialResponse>(options, whatsappKeys) {}.send(session).thenAccept(this::handleInitialMessage);
    }

    private void handleInitialMessage(@NotNull InitialResponse response) {
        if (whatsappKeys.mayRestore()) {
            new TakeOverRequest<TakeOverResponse>(options, whatsappKeys) {}.send(session()).thenAccept(this::solveChallenge);
            return;
        }

        generateQrCode(response);
    }

    private void generateQrCode(@NotNull InitialResponse response) {
        if(loggedIn){
            return;
        }

        Validate.isTrue(response.status() != 429, "Out of attempts to scan the QR code", IllegalStateException.class);
        CompletableFuture.delayedExecutor(response.ttl(), TimeUnit.MILLISECONDS).execute(() -> generateQrCode(response));
        Validate.isTrue(response.ref() != null, "Cannot find ref for QR code generation");
        qrCode.generateAndPrint(response.ref(), extractRawPublicKey(whatsappKeys.keyPair().getPublic()), whatsappKeys.clientId());
    }

    private void solveChallenge(@NotNull TakeOverResponse response) {
        if (response.status() >= 400) {
            whatsappKeys.deleteKeysFromMemory();
            disconnect(null, false, true);
            return;
        }

        sendChallenge(response);
    }

    private void sendChallenge(@NotNull TakeOverResponse response) {
        var challengeBase64 = response.challenge();
        if (challengeBase64 == null) {
            return;
        }

        var challenge = BinaryArray.forBase64(challengeBase64);
        var signedChallenge = hmacSha256(challenge, Objects.requireNonNull(whatsappKeys.macKey()));

        var request = new SolveChallengeRequest<SimpleStatusResponse>(options, whatsappKeys, signedChallenge) {};
        request.send(session()).thenAccept(result -> Validate.isTrue(result.status() == 200, "Could not solve whatsapp challenge for server and client token renewal: %s".formatted(result)));
    }

    private void login(@NotNull UserInformationResponse response) {
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

    @OnMessage
    public void onMessage(@NotNull Response<?> response) {
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

    @OnMessage
    public void onBinaryMessage(@NotNull BinaryResponse response) {
        if (whatsappManager.resolvePendingRequest(response.tag(), response)) {
            return;
        }

        whatsappManager.digestWhatsappNode(this, response.content());
    }

    @OnError
    public void onError(@NotNull Throwable throwable) {
        throwable.printStackTrace();
    }

    public void connect() {
        Validate.isTrue(!loggedIn, "WhatsappAPI: Cannot establish a connection with whatsapp as one already exists", IllegalStateException.class);
        openConnection();
        pingService.scheduleAtFixedRate(this::sendPing, 0, 1, TimeUnit.MINUTES);
    }

    @SneakyThrows
    public void disconnect(String reason, boolean logout, boolean reconnect) {
        Validate.isTrue(loggedIn, "WhatsappAPI: Cannot terminate the connection with whatsapp as it doesn't exist", IllegalStateException.class);
        whatsappManager.clear();
        if (logout) new LogOutRequest(options) {
        }.send(session()).thenRun(whatsappKeys::deleteKeysFromMemory);
        session().close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason));
        whatsappManager.listeners().forEach(WhatsappListener::onDisconnected);
        if (reconnect) openConnection();
    }

    @SneakyThrows
    private void openConnection() {
        webSocketContainer.connectToServer(this, URI.create(options.whatsappUrl()));
    }

    @SneakyThrows
    private void sendPing() {
        if (options.async()) {
            session().getAsyncRemote().sendPing(ByteBuffer.allocate(0));
            return;
        }

        session.getBasicRemote().sendPing(ByteBuffer.allocate(0));
    }

    @SneakyThrows
    private void handleChatCmd(@NotNull ChatCmdResponse cmdResponse) {
        if (cmdResponse.cmd() == null) {
            return;
        }

        var chatOpt = whatsappManager.findChatByJid(cmdResponse.jid());
        if (chatOpt.isEmpty()) {
            return;
        }

        var chat = chatOpt.get();
        var node = WhatsappNode.fromList(cmdResponse.data());
        var content = (String) node.content();

        switch (node.description()) {
            case "restrict" -> notifyGroupSettingChange(chat, WhatsappGroupSetting.EDIT_GROUP_INFO, content);
            case "announce" -> notifyGroupSettingChange(chat, WhatsappGroupSetting.SEND_MESSAGES, content);
            case "add", "remove", "promote", "demote" -> notifyGroupAction(chat, node, content);
            case "ephemeral" -> updateAndNotifyEphemeralStatus(chat, content);
            case "desc_add" -> notifyGroupDescriptionChange(chat, content);
            case "subject" -> updateAndNotifyGroupSubject(chat, content);
        }
    }

    private void notifyGroupDescriptionChange(@NotNull WhatsappChat chat, @NotNull String content) {
        var response = JsonResponse.fromJson(content).toModel(DescriptionChangeResponse.class);
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onGroupDescriptionChange(chat, response.description(), response.descriptionId())));
    }

    private void updateAndNotifyGroupSubject(@NotNull WhatsappChat chat, @NotNull String content) {
        var response = JsonResponse.fromJson(content).toModel(SubjectChangeResponse.class);
        chat.name(response.subject());
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onGroupSubjectChange(chat)));
    }

    private void updateAndNotifyEphemeralStatus(@NotNull WhatsappChat chat, @NotNull String content) {
        chat.ephemeralMessageDuration(Long.parseLong(content));
        chat.ephemeralMessagesToggleTime(ZonedDateTime.now().toEpochSecond());
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onChatEphemeralStatusChange(chat)));
    }

    private void notifyGroupAction(@NotNull WhatsappChat chat, @NotNull WhatsappNode node, @NotNull String content) {
        JsonResponse.fromJson(content).toModel(GroupActionResponse.class).participants().stream().map(whatsappManager::findContactByJid).map(Optional::orElseThrow).forEach(contact -> whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onGroupAction(chat, contact, WhatsappGroupAction.valueOf(node.description().toUpperCase())))));
    }

    private void notifyGroupSettingChange(@NotNull WhatsappChat chat, @NotNull WhatsappGroupSetting setting, @NotNull String content) {
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onGroupSettingsChange(chat, setting, WhatsappGroupPolicy.forData(Boolean.parseBoolean(content)))));
    }

    private void handleMessageInfo(@NotNull AckResponse ackResponse) {
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

        Arrays.stream(ackResponse.ids()).map(id -> whatsappManager.findUserMessageById(chat.get(), id)).filter(Optional::isPresent).map(Optional::get).forEach(message -> updateAndNotifyMessageReadStatusChange(ackResponse, to.get(), chat.get(), message));
    }

    private void updateAndNotifyMessageReadStatusChange(@NotNull AckResponse ackResponse, @NotNull WhatsappContact to, @NotNull WhatsappChat chat, WhatsappUserMessage message) {
        message.individualReadStatus().put(to, WhatsappProtobuf.WebMessageInfo.WebMessageInfoStatus.forNumber(ackResponse.ack()));
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onMessageReadStatusUpdate(chat, to, message)));
    }

    private void handleUserInformation(@NotNull UserInformationResponse info) {
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

    private void configureSelfContact(@NotNull UserInformationResponse info) {
        var jid = parseJid(info.wid());
        whatsappManager.contacts().add(new WhatsappContact(jid, null, null, null, null, null));
        whatsappManager.phoneNumberJid(jid);
    }

    private void scheduleMediaConnection(int delay) {
        CompletableFuture.delayedExecutor(delay, TimeUnit.SECONDS).execute(this::createMediaConnection);
    }

    @SneakyThrows
    private void createMediaConnection() {
        var connection = new MediaConnectionRequest<MediaConnectionResponse>(options) {}.send(session()).get().connection();
        whatsappManager.mediaConnection(connection);
        scheduleMediaConnection(connection.ttl());
    }

    private void handleBlocklist(@NotNull BlocklistResponse blocklist) {
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onBlocklistUpdate(blocklist)));
    }

    private void handleProps(@NotNull PropsResponse props) {
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onPropsUpdate(props)));
    }

    private void handleCmd(@NotNull JsonResponse res) {
        if (!res.hasKey("type") || !res.hasKey("kind")) {
            return;
        }

        var kind = res.getString("kind").orElse("unknown");
        disconnect(kind, false, options.reconnectWhenDisconnected().apply(kind));
    }

    private void handlePresence(@NotNull PresenceResponse res) {
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
        } else if (res.presence() == WhatsappContactStatus.UNAVAILABLE && contact.lastKnownPresence().isPresent() && (contact.lastKnownPresence().get() == WhatsappContactStatus.AVAILABLE || contact.lastKnownPresence().get() == WhatsappContactStatus.COMPOSING)) {
            contact.lastSeen(ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
        }

        contact.lastKnownPresence(res.presence());
        chat.presences().put(contact, res.presence());
        whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onContactPresenceUpdate(chat, contact)));
    }

    private void handleList(@NotNull JsonListResponse response) {
        whatsappManager.callOnListenerThread(() -> whatsappManager.listeners().forEach(whatsappListener -> whatsappListener.onListResponse(response.content())));
    }

    public @NotNull Session session() {
        return Objects.requireNonNull(session, "WhatsappAPI: The session linked to the WebSocket is null");
    }

    public @NotNull CompletableFuture<ChatResponse> queryChat(@NotNull String jid) {
        var node = new WhatsappNode("query", attributes(attr("type", "chat"), attr("jid", jid)), null);
        return new BinaryRequest<ChatResponse>(options, node, BinaryFlag.IGNORE, BinaryMetric.QUERY_CHAT) {}.send(session());
    }
}