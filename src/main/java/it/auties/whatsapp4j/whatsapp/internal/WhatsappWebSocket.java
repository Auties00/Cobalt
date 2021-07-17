package it.auties.whatsapp4j.whatsapp.internal;

import com.google.zxing.common.BitMatrix;
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
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import it.auties.whatsapp4j.whatsapp.WhatsappConfiguration;
import jakarta.websocket.*;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
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
@ClientEndpoint(configurator = WhatsappSocketConfiguration.class)
public class WhatsappWebSocket {
    private Session session;
    private boolean loggedIn;
    private final @NonNull WebSocketContainer webSocketContainer;
    private final @NonNull ScheduledExecutorService pingService;
    private final @NonNull WhatsappDataManager whatsappManager;
    private final @NonNull WhatsappKeysManager whatsappKeys;
    private final @NonNull WhatsappConfiguration options;
    private final @NonNull WhatsappQRCode qrCode;
    private final @NonNull BinaryDecoder decoder;

    public WhatsappWebSocket(@NonNull WhatsappConfiguration options, @NonNull WhatsappKeysManager manager) {
        this(
                ContainerProvider.getWebSocketContainer(),
                Executors.newSingleThreadScheduledExecutor(),
                WhatsappDataManager.singletonInstance(),
                manager,
                options,
                new WhatsappQRCode(),
                new BinaryDecoder()
        );
    }


    @OnOpen
    public void onOpen(@NonNull Session session) {
        if(this.session == null || !this.session.isOpen())  {
            session(session);
        }

        new InitialRequest<InitialResponse>(options, whatsappKeys){}
                .send(session)
                .thenAccept(this::handleInitialMessage);
    }

    private void handleInitialMessage(@NonNull InitialResponse response) {
        if (!whatsappKeys.mayRestore()) {
            generateQrCode(response);
        }

        new TakeOverRequest<TakeOverResponse>(options, whatsappKeys) {}
                .send(session())
                .thenAccept(this::solveChallenge);
    }

    private void generateQrCode(@NonNull InitialResponse response) {
        if(loggedIn){
            return;
        }

        scheduleQrCodeUpdate(response);
        var matrix = createMatrix(response);
        whatsappManager.callListeners(listener -> listener.onQRCode(matrix));
    }

    private @NonNull BitMatrix createMatrix(@NonNull InitialResponse response) {
        var ref = Objects.requireNonNull(response.ref(), "Cannot find ref for QR code generation, the version code is probably outdated");
        var publicKey = extractRawPublicKey(whatsappKeys.keyPair().getPublic());
        var clientId = whatsappKeys.clientId();
        return qrCode.generate(ref, publicKey, clientId);
    }

    private void scheduleQrCodeUpdate(InitialResponse response) {
        Validate.isTrue(response.status() != 429, "Out of attempts to scan the QR code", IllegalStateException.class);
        CompletableFuture.delayedExecutor(response.ttl(), TimeUnit.MILLISECONDS)
                .execute(() -> generateQrCode(response));
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
        var signedChallenge = hmacSha256(challenge, whatsappKeys.macKey());

        new SolveChallengeRequest<SimpleStatusResponse>(options, whatsappKeys, signedChallenge) {}
                .send(session())
                .thenAcceptAsync(SimpleStatusResponse::orElseThrow);
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

        whatsappKeys.initializeKeys(
                response.serverToken(),
                response.clientToken(),
                keysDecrypted.cut(32),
                keysDecrypted.slice(32, 64)
        );
    }

    @OnMessage
    public void onText(@NonNull String data) {
        var response = Response.fromTaggedResponse(data);
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
    public void onBinary(byte @NonNull [] msg) {
        Validate.isTrue(msg[0] != '!', "Server pong from whatsapp, why did this get through?");

        var binaryMessage = BinaryArray.forArray(msg);
        var tagAndMessagePair = binaryMessage.indexOf(',').map(binaryMessage::split).orElseThrow();

        var messageTag = tagAndMessagePair.key().toString();
        var messageContent = tagAndMessagePair.value();

        var message = messageContent.slice(32);
        var hmacValidation = CypherUtils.hmacSha256(message, Objects.requireNonNull(whatsappKeys.macKey()));
        Validate.isTrue(hmacValidation.equals(messageContent.cut(32)), "Cannot read message: Hmac validation failed!", SecurityException.class);

        var decryptedMessage = CypherUtils.aesDecrypt(message, Objects.requireNonNull(whatsappKeys.encKey()));
        var response = new BinaryResponse(messageTag, decoder.decodeDecryptedMessage(decryptedMessage));
        if (whatsappManager.resolvePendingRequest(response.tag(), response)) {
            return;
        }

        whatsappManager.digestWhatsappNode(this, response.content());
    }

    @OnError
    public void onError(@NonNull Throwable throwable){
        throw new RuntimeException("An uncaught exception was thrown during the WebSocket lifecycle", throwable);
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
        if (logout) {
            new LogOutRequest(options) {}
                    .send(session())
                    .thenRunAsync(whatsappKeys::deleteKeysFromMemory);
        }

        session().close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason));
        session(null);
        whatsappManager.callListeners(WhatsappListener::onDisconnected);
        if (reconnect) {
            openConnection();
        }
    }

    private void openConnection() {
        try{
            webSocketContainer.connectToServer(this, URI.create(options.whatsappUrl()));
        }catch (IOException | DeploymentException exception){
            throw new RuntimeException("Cannot connect to WhatsappWeb's WebServer", exception);
        }
    }

    @SneakyThrows
    private void sendPing() {
        session().getAsyncRemote().sendPing(ByteBuffer.allocate(0));
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
        var description = response.description();
        var descriptionId = response.descriptionId();
        whatsappManager.callListeners(listener ->
                listener.onGroupDescriptionChange(chat, description, descriptionId));
    }

    private void updateAndNotifyGroupSubject(@NonNull Chat chat, @NonNull String content) {
        var response = JsonResponse.fromJson(content).toModel(SubjectChangeResponse.class);
        chat.displayName(response.subject());
        whatsappManager.callListeners(listener -> listener.onGroupSubjectChange(chat));
    }

    private void updateAndNotifyEphemeralStatus(@NonNull Chat chat, @NonNull String content) {
        chat.ephemeralMessageDuration(Long.parseLong(content));
        chat.ephemeralMessagesToggleTime(ZonedDateTime.now().toEpochSecond());
        whatsappManager.callListeners(listener -> listener.onChatEphemeralStatusChange(chat));
    }

    private void notifyGroupAction(@NonNull Chat chat, @NonNull Node node, @NonNull String content) {
        JsonResponse.fromJson(content)
                .toModel(GroupActionResponse.class)
                .participants()
                .stream()
                .map(whatsappManager::findContactByJid)
                .map(Optional::orElseThrow)
                .forEach(contact -> notifyGroupAction(chat, node, contact));
    }

    private void notifyGroupAction(@NonNull Chat chat, @NonNull Node node, @NonNull Contact contact) {
        var action =  GroupAction.valueOf(node.description().toUpperCase());
        whatsappManager.callListeners(listener ->
                listener.onGroupAction(chat, contact, action));
    }

    private void notifyGroupSettingChange(@NonNull Chat chat, @NonNull GroupSetting setting, @NonNull String content) {
        var policy = GroupPolicy.forData(Boolean.parseBoolean(content));
        whatsappManager.callListeners(listener ->
                listener.onGroupSettingsChange(chat, setting, policy));
    }

    private void handleMessageInfo(@NonNull AckResponse ackResponse) {
        if (ackResponse.cmd() == null) {
            return;
        }

        var participant = Objects.requireNonNullElse(ackResponse.participant(), ackResponse.to());
        var to = whatsappManager.findContactByJid(participant);
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
        var status = MessageInfo.MessageInfoStatus.forIndex(ackResponse.ack());
        message.individualReadStatus().put(to, status);
        whatsappManager.callListeners(listener -> listener.onMessageReadStatusUpdate(chat, to, message));
    }

    private void handleUserInformation(@NonNull UserInformationResponse info) {
        if (info.ref() == null) {
            whatsappManager.callListeners(listener -> listener.onInformationUpdate(info));
            return;
        }

        Validate.isTrue(info.connected(), "WhatsappAPI: Cannot establish a connection with WhatsappWeb");
        if (!whatsappKeys.mayRestore()) {
            login(info);
        }

        configureSelfContact(info);
        scheduleMediaConnection(0);
        loggedIn(true);
        whatsappManager.callListeners(listener -> listener.onLoggedIn(info));
    }

    private void configureSelfContact(@NonNull UserInformationResponse info) {
        var jid = parseJid(info.wid());
        whatsappManager.contacts().add(Contact.fromJid(jid));
        whatsappManager.phoneNumberJid(jid);
    }

    private void scheduleMediaConnection(int delay) {
        CompletableFuture.delayedExecutor(delay, TimeUnit.SECONDS)
                .execute(this::createMediaConnection);
    }

    private void createMediaConnection() {
        new MediaConnectionRequest<MediaConnectionResponse>(options) {}
                .send(session())
                .thenApplyAsync(MediaConnectionResponse::connection)
                .thenApplyAsync(whatsappManager::mediaConnection)
                .thenRunAsync(() -> scheduleMediaConnection(whatsappManager.mediaConnection().ttl()));
    }

    private void handleBlocklist(@NonNull BlocklistResponse blocklist) {
        whatsappManager.callListeners(listener -> listener.onBlocklistUpdate(blocklist));
    }

    private void handleProps(@NonNull PropsResponse props) {
        whatsappManager.callListeners(listener -> listener.onPropsUpdate(props));
    }

    // This is not a very good approach probably as there should be more CMDs
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
            handleGroupPresence(res, chat);
            return;
        }

        var contact = whatsappManager.findContactByJid(res.jid())
                .orElseThrow(() -> new IllegalArgumentException("Cannot update presence of unknown contact"));
        handleContactPresence(res, chat, contact);
    }

    private void handleContactPresence(@NonNull PresenceResponse res, @NonNull Chat chat, @NonNull Contact contact) {
        var offset = res.offsetFromLastSeen();
        if (offset != null) {
            var lastSeen = contact.lastSeen()
                    .map(time  -> time.plusSeconds(offset))
                    .orElse(ZonedDateTime.ofInstant(Instant.ofEpochSecond(offset), ZoneId.systemDefault()));
            contact.lastSeen(lastSeen);
        } else if (res.presence() == ContactStatus.UNAVAILABLE) {
            contact.lastKnownPresence()
                    .filter(lastPresence -> lastPresence != ContactStatus.UNAVAILABLE)
                    .ifPresent(__ -> contact.lastSeen(ZonedDateTime.now()));
        }

        contact.lastKnownPresence(res.presence());
        chat.presences().put(contact, res.presence());
        whatsappManager.callListeners(listener -> listener.onContactPresenceUpdate(chat, contact));
    }

    private void handleGroupPresence(@NonNull PresenceResponse res, @NonNull Chat chat) {
        if (res.participant() == null) {
            return;
        }

        var participantOpt = whatsappManager.findContactByJid(res.participant());
        if (participantOpt.isEmpty()) {
            return;
        }

        var participant = participantOpt.get();
        chat.presences().put(participant, res.presence());
        whatsappManager.callListeners(listener -> listener.onContactPresenceUpdate(chat, participant));
    }

    private void handleList(@NonNull JsonListResponse response) {
        whatsappManager.callListeners(listener -> listener.onListResponse(response.content()));
    }

    public @NonNull Session session() {
        return Objects.requireNonNull(session, "WhatsappAPI: The session linked to the WebSocket is null");
    }

    public @NonNull CompletableFuture<ChatResponse> queryChat(@NonNull String jid) {
        var node = new Node("query", attributes(attr("type", "chat"), attr("jid", jid)), null);
        return new BinaryRequest<ChatResponse>(options, whatsappKeys, node, BinaryFlag.IGNORE, BinaryMetric.QUERY_CHAT) {}.send(session());
    }
}