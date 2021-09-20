package it.auties.whatsapp4j.standard.socket;

import com.google.zxing.common.BitMatrix;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.manager.WhatsappDataManager;
import it.auties.whatsapp4j.common.response.BinaryResponse;
import it.auties.whatsapp4j.common.response.JsonListResponse;
import it.auties.whatsapp4j.common.response.JsonResponse;
import it.auties.whatsapp4j.common.response.Response;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import it.auties.whatsapp4j.common.utils.WhatsappUtils;
import it.auties.whatsapp4j.standard.api.WhatsappAPI;
import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.standard.binary.BinaryFlag;
import it.auties.whatsapp4j.standard.binary.BinaryMetric;
import it.auties.whatsapp4j.common.listener.IWhatsappListener;
import it.auties.whatsapp4j.common.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.common.protobuf.chat.Chat;
import it.auties.whatsapp4j.common.protobuf.chat.GroupAction;
import it.auties.whatsapp4j.common.protobuf.chat.GroupPolicy;
import it.auties.whatsapp4j.common.protobuf.chat.GroupSetting;
import it.auties.whatsapp4j.common.protobuf.contact.Contact;
import it.auties.whatsapp4j.common.protobuf.contact.IContactStatus;
import it.auties.whatsapp4j.common.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import it.auties.whatsapp4j.common.serialization.IWhatsappSerializer;
import it.auties.whatsapp4j.common.socket.IWhatsappSocket;
import it.auties.whatsapp4j.common.socket.WhatsappSocketConfiguration;
import it.auties.whatsapp4j.standard.protobuf.ContactStatus;
import it.auties.whatsapp4j.standard.request.*;
import it.auties.whatsapp4j.standard.response.*;
import it.auties.whatsapp4j.standard.serialization.WhatsappSerializer;
import it.auties.whatsapp4j.common.utils.Validate;
import it.auties.whatsapp4j.common.utils.WhatsappQRCode;
import jakarta.websocket.*;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

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

/**
 * This class is an interface between this API and WhatsappWeb's WebClient.
 * These methods should not be used by any project, excluding obviously WhatsappWeb4j.
 * Instead, {@link WhatsappAPI} should be used.
 */
@RequiredArgsConstructor
@Data
@Accessors(fluent = true)
@ClientEndpoint(configurator = WhatsappSocketConfiguration.class)
@Log
public class WhatsappSocket implements IWhatsappSocket {
    private Session session;
    private boolean loggedIn;
    private final @NonNull WebSocketContainer container;
    private final @NonNull ScheduledExecutorService pingService;
    private final @NonNull WhatsappDataManager manager;
    private final @NonNull WhatsappKeysManager keys;
    private final @NonNull WhatsappConfiguration options;
    private final @NonNull WhatsappQRCode qrCode;
    private final @NonNull IWhatsappSerializer serializer;

    public WhatsappSocket(@NonNull WhatsappConfiguration options, @NonNull WhatsappKeysManager keys) {
        this(
                ContainerProvider.getWebSocketContainer(),
                Executors.newSingleThreadScheduledExecutor(),
                WhatsappDataManager.singletonInstance(),
                keys,
                options,
                new WhatsappQRCode(),
                new WhatsappSerializer(keys)
        );
    }

    @OnOpen
    public void onOpen(@NonNull Session session) {
        if(this.session == null || !this.session.isOpen())  {
            session(session);
        }

        handleSocketConnection(session);
    }

    private void handleSocketConnection(Session session) {
        new InitialRequest<InitialResponse>(options, keys){}
                .send(session)
                .thenAcceptAsync(this::handleInitialMessage);
    }

    private void handleInitialMessage(@NonNull InitialResponse response) {
        if (!keys.mayRestore()) {
            generateQrCode(response);
            return;
        }

        new TakeOverRequest<TakeOverResponse>(options, keys) {}
                .send(session())
                .thenAccept(this::solveChallenge);
    }

    private void generateQrCode(@NonNull InitialResponse response) {
        if(loggedIn){
            return;
        }

        if(response.outdated()){
            log.warning("Outdated whatsapp web client, a new version is available: %s".formatted(response.latestVersion()));
            log.warning("This version is still supported, but it is recommend to manually set the new version by specifying a custom configuration to avoid problems in the future");
        }

        if(response.unsupported()){
            onUnsupportedVersion();
            return;
        }

        scheduleQrCodeUpdate(response);
        var matrix = createMatrix(response);
        manager.callListeners(listener -> listener.onQRCode(matrix));
    }

    private @NonNull BitMatrix createMatrix(@NonNull InitialResponse response) {
        var ref = Objects.requireNonNull(response.ref(), "Cannot find ref for QR code generation, the version code is probably outdated");
        var publicKey = CypherUtils.parseKey(keys.keyPair().getPublic());
        var clientId = keys.clientId();
        return qrCode.generate(ref, publicKey, clientId);
    }

    private void scheduleQrCodeUpdate(InitialResponse response) {
        Validate.isTrue(response.status() != 429, "Out of attempts to scan the QR code", IllegalStateException.class);
        CompletableFuture.delayedExecutor(response.ttl(), TimeUnit.MILLISECONDS)
                .execute(() -> generateQrCode(response));
    }

    private void solveChallenge(@NonNull TakeOverResponse response) {
        if (response.status() >= 400) {
            keys.deleteKeysFromMemory();
            closeConnection("reconnect_400_status_code");
            openConnection();
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
        var signedChallenge = CypherUtils.hmacSha256(challenge, keys.macKey());
        new SolveChallengeRequest<SimpleStatusResponse>(options, keys, signedChallenge) {}
                .send(session())
                .thenAcceptAsync(SimpleStatusResponse::orElseThrow);
    }

    private void login(@NonNull UserInformationResponse response) {
        var base64Secret = response.secret();
        var secret = BinaryArray.forBase64(base64Secret);
        var pubKey = secret.cut(32);
        var sharedSecret = CypherUtils.calculateSharedSecret(pubKey.data(), keys.keyPair().getPrivate());
        var sharedSecretExpanded = CypherUtils.hkdfExtractAndExpand(sharedSecret, 80);

        var hmacValidation = CypherUtils.hmacSha256(secret.cut(32).append(secret.slice(64)), sharedSecretExpanded.slice(32, 64));
        Validate.isTrue(hmacValidation.equals(secret.slice(32, 64)), "Cannot login: Hmac validation failed!", SecurityException.class);

        var keysEncrypted = sharedSecretExpanded.slice(64).append(secret.slice(64));
        var key = sharedSecretExpanded.cut(32);
        var keysDecrypted = CypherUtils.aesDecrypt(keysEncrypted, key);

        keys.initializeKeys(
                response.serverToken(),
                response.clientToken(),
                keysDecrypted.cut(32),
                keysDecrypted.slice(32, 64)
        );
    }

    @OnMessage
    public void onText(@NonNull String data) {
        var response = Response.fromTaggedResponse(data);
        switch (response) {
            case JsonListResponse listResponse -> handleList(listResponse);
            case JsonResponse mapResponse -> {
                if (mapResponse.content().isEmpty()) {
                    return;
                }

                if (manager.resolvePendingRequest(mapResponse)) {
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

            case BinaryResponse ignore -> throw new IllegalArgumentException("Unexpected value: onText is not supposed to handle binary responses");
        }
    }

    @OnMessage
    public void onBinary(byte @NonNull [] msg) {
        var response = serializer.deserialize(BinaryArray.forArray(msg));
        if (manager.resolvePendingRequest(response)) {
            return;
        }

        NodeDigester.digestWhatsappNode(this, response.content());
    }

    public void connect() {
        Validate.isTrue(!loggedIn, "WhatsappAPI: Cannot establish a connection with whatsapp as one already exists", IllegalStateException.class);
        openConnection();
        pingService.scheduleAtFixedRate(this::sendPing, 0, 1, TimeUnit.MINUTES);
    }

    public void disconnect(String reason, boolean logout, boolean reconnect) {
        Validate.isTrue(loggedIn, "WhatsappAPI: Cannot terminate the connection with whatsapp as it doesn't exist", IllegalStateException.class);
        manager.clear();
        if (logout) {
            new LogOutRequest(options) {}
                    .send(session())
                    .thenRunAsync(keys::deleteKeysFromMemory);
        }

        closeConnection(reason);
        manager.callListeners(IWhatsappListener::onDisconnected);
        if (!reconnect) {
            return;
        }

        openConnection();
    }

    private void closeConnection(String reason){
      try {
          session().close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason));
          session(null);
      }catch (IOException exception){
          throw new RuntimeException("Cannot close connection to WhatsappWeb's WebServer", exception);
      }
    }
    
    private void openConnection() {
        try{
            container.connectToServer(this, URI.create(options.whatsappUrl()));
        }catch (IOException | DeploymentException exception){
            throw new RuntimeException("Cannot connect to WhatsappWeb's WebServer", exception);
        }
    }

    private void sendPing() {
        try {
            session().getAsyncRemote().sendPing(ByteBuffer.allocate(0));
        }catch (IOException exception){
            throw new RuntimeException("Cannot send ping to WhatsappWeb's WebServer", exception);
        }
    }

    private void handleChatCmd(@NonNull ChatCmdResponse cmdResponse) {
        if (cmdResponse.cmd() == null) {
            return;
        }

        var chatOpt = manager.findChatByJid(cmdResponse.jid());
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
        manager.callListeners(listener ->
                listener.onGroupDescriptionChange(chat, description, descriptionId));
    }

    private void updateAndNotifyGroupSubject(@NonNull Chat chat, @NonNull String content) {
        var response = JsonResponse.fromJson(content).toModel(SubjectChangeResponse.class);
        chat.displayName(response.subject());
        manager.callListeners(listener -> listener.onGroupSubjectChange(chat));
    }

    private void updateAndNotifyEphemeralStatus(@NonNull Chat chat, @NonNull String content) {
        chat.ephemeralMessageDuration(Long.parseLong(content));
        chat.ephemeralMessagesToggleTime(ZonedDateTime.now().toEpochSecond());
        manager.callListeners(listener -> listener.onChatEphemeralStatusChange(chat));
    }

    private void notifyGroupAction(@NonNull Chat chat, @NonNull Node node, @NonNull String content) {
        JsonResponse.fromJson(content)
                .toModel(GroupActionResponse.class)
                .participants()
                .stream()
                .map(manager::findContactByJid)
                .map(Optional::orElseThrow)
                .forEach(contact -> notifyGroupAction(chat, node, contact));
    }

    private void notifyGroupAction(@NonNull Chat chat, @NonNull Node node, @NonNull Contact contact) {
        var action =  GroupAction.valueOf(node.description().toUpperCase());
        manager.callListeners(listener ->
                listener.onGroupAction(chat, contact, action));
    }

    private void notifyGroupSettingChange(@NonNull Chat chat, @NonNull GroupSetting setting, @NonNull String content) {
        var policy = GroupPolicy.forData(Boolean.parseBoolean(content));
        manager.callListeners(listener ->
                listener.onGroupSettingsChange(chat, setting, policy));
    }

    private void handleMessageInfo(@NonNull AckResponse ackResponse) {
        if (ackResponse.cmd() == null) {
            return;
        }

        var participant = Objects.requireNonNullElse(ackResponse.participant(), ackResponse.to());
        var to = manager.findContactByJid(participant);
        if (to.isEmpty()) {
            return;
        }

        var chat = manager.findChatByJid(ackResponse.to());
        if (chat.isEmpty()) {
            return;
        }

        Arrays.stream(ackResponse.ids())
                .map(id -> manager.findMessageById(chat.get(), id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(message -> updateAndNotifyMessageReadStatusChange(ackResponse, to.get(), chat.get(), message));
    }

    private void updateAndNotifyMessageReadStatusChange(@NonNull AckResponse ackResponse, @NonNull Contact to, @NonNull Chat chat, MessageInfo message) {
        var status = MessageInfo.MessageInfoStatus.forIndex(ackResponse.ack());
        message.individualReadStatus().put(to, status);
        manager.callListeners(listener -> listener.onMessageReadStatusUpdate(chat, to, message));
    }

    private void handleUserInformation(@NonNull UserInformationResponse info) {
        if (info.ref() == null) {
            manager.callListeners(listener -> listener.onInformationUpdate(info));
            return;
        }

        Validate.isTrue(info.connected(), "WhatsappAPI: Cannot establish a connection with WhatsappWeb");
        if (!keys.mayRestore()) {
            login(info);
        }

        configureSelfContact(info);
        scheduleMediaConnection(0);
        loggedIn(true);
        manager.callListeners(listener -> listener.onLoggedIn(info));
    }

    private void configureSelfContact(@NonNull UserInformationResponse info) {
        var jid = WhatsappUtils.parseJid(info.wid());
        manager.contacts().add(Contact.fromJid(jid));
        manager.phoneNumberJid(jid);
    }

    private void scheduleMediaConnection(int delay) {
        CompletableFuture.delayedExecutor(delay, TimeUnit.SECONDS)
                .execute(this::createMediaConnection);
    }

    private void createMediaConnection() {
        new MediaConnectionRequest<MediaConnectionResponse>(options) {}
                .send(session())
                .thenApplyAsync(MediaConnectionResponse::connection)
                .thenApplyAsync(manager::mediaConnection)
                .thenRunAsync(() -> scheduleMediaConnection(manager.mediaConnection().ttl()));
    }

    private void handleBlocklist(@NonNull BlocklistResponse blocklist) {
        manager.callListeners(listener -> listener.onBlocklistUpdate(blocklist));
    }

    private void handleProps(@NonNull PropsResponse props) {
        manager.callListeners(listener -> listener.onPropsUpdate(props));
    }

    private void handleCmd(@NonNull JsonResponse res) {
        var type = res.getString("type")
                .orElseThrow(() -> new IllegalArgumentException("Cannot parse cmd action with no type: %s".formatted(res)));
        switch (type){
            case "disconnect" -> handleDisconnection(res);
            case "update" -> onUnsupportedVersion();
            case "upgrade_md_prod" -> activateMultiDeviceBeta();
            default -> log.warning("Unknown cmd type(%s): %s".formatted(type, res));
        }
    }

    private void activateMultiDeviceBeta(){
        throw new UnsupportedOperationException("The standard module of WhatsappWeb4j doesn't support the multi device beta");
    }

    private void onUnsupportedVersion() {
        throw new IllegalStateException("Unsupported client version, please provide a supported version code using WhatsappConfiguration or notify the maintainers");
    }

    private void handleDisconnection(JsonResponse res) {
        var kind = res.getString("kind").orElse("unknown");
        disconnect(kind, false, options.reconnectWhenDisconnected().apply(kind));
    }

    private void handlePresence(@NonNull PresenceResponse res) {
        var chatOpt = manager.findChatByJid(res.jid());
        if (chatOpt.isEmpty()) {
            return;
        }

        var chat = chatOpt.get();
        if (chat.isGroup()) {
            handleGroupPresence(res, chat);
            return;
        }

        var contact = manager.findContactByJid(res.jid())
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
        manager.callListeners(listener -> listener.onContactPresenceUpdate(chat, contact));
    }

    private void handleGroupPresence(@NonNull PresenceResponse res, @NonNull Chat chat) {
        if (res.participant() == null) {
            return;
        }

        var participantOpt = manager.findContactByJid(res.participant());
        if (participantOpt.isEmpty()) {
            return;
        }

        var participant = participantOpt.get();
        chat.presences().put(participant, res.presence());
        manager.callListeners(listener -> listener.onContactPresenceUpdate(chat, participant));
    }

    private void handleList(@NonNull JsonListResponse response) {
        manager.callListeners(listener -> listener.onListResponse(response.content()));
    }

    public @NonNull Session session() {
        return Objects.requireNonNull(session, "WhatsappAPI: The session linked to the WebSocket is null");
    }

    public @NonNull CompletableFuture<ChatResponse> queryChat(@NonNull String jid) {
        var node = new Node("query", WhatsappUtils.attributes(WhatsappUtils.attribute("type", "chat"), WhatsappUtils.attribute("jid", jid)), null);
        return new BinaryRequest<ChatResponse>(node, options, keys, BinaryFlag.IGNORE, BinaryMetric.QUERY_CHAT) {}.send(session());
    }
}