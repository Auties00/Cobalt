package it.auties.whatsapp4j.socket;

import it.auties.whatsapp4j.binary.*;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.model.*;
import it.auties.whatsapp4j.request.impl.*;
import it.auties.whatsapp4j.response.impl.binary.ChatResponse;
import it.auties.whatsapp4j.response.impl.json.*;
import it.auties.whatsapp4j.response.model.json.JsonListResponse;
import it.auties.whatsapp4j.response.model.json.JsonResponse;
import it.auties.whatsapp4j.response.model.binary.BinaryResponse;
import it.auties.whatsapp4j.response.impl.shared.WhatsappResponse;
import it.auties.whatsapp4j.utils.*;
import jakarta.websocket.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static it.auties.whatsapp4j.utils.CypherUtils.*;

@ClientEndpoint(configurator = WhatsappSocketConfiguration.class)
@AllArgsConstructor
@Data
@Accessors(fluent = true)
public class WhatsappWebSocket {
  private @Nullable Session session;
  private @NotNull SocketState state;
  private final @NotNull ScheduledExecutorService scheduler;
  private final @NotNull WebSocketContainer webSocketContainer;
  private final @NotNull WhatsappDataManager whatsappManager;
  private final @NotNull WhatsappKeysManager whatsappKeys;
  private final @NotNull WhatsappConfiguration options;
  private final @NotNull WhatsappQRCode qrCode;
  private final @NotNull BinaryDecoder binaryDecoder;
  private final @NotNull BinaryEncoder binaryEncoder;

  public WhatsappWebSocket(@NotNull WhatsappConfiguration options, @NotNull WhatsappKeysManager whatsappKeys) {
    this(null, SocketState.NOTHING, Executors.newSingleThreadScheduledExecutor(), ContainerProvider.getWebSocketContainer(), WhatsappDataManager.singletonInstance(), whatsappKeys, options, new WhatsappQRCode(), new BinaryDecoder(), new BinaryEncoder());
    webSocketContainer.setDefaultMaxSessionIdleTimeout(Duration.ofDays(30).toMillis());
  }

  @OnOpen
  public void onOpen(@NotNull Session session) {
    if(this.session == null || !this.session.isOpen()) session(session);
    final var login = new InitialRequest(options, whatsappKeys){};
    login.send(session, () -> state(SocketState.SENT_INITIAL_MESSAGE));
  }

  @OnMessage
  public void onMessage(@NotNull String message) {
    switch (state){
      case SENT_INITIAL_MESSAGE -> sendInitialMessage(message);
      case SOLVE_CHALLENGE -> solveChallenge(message);
      case SENT_CHALLENGE -> handleChallengeResponse(message);
      case WAITING_FOR_LOGIN -> login(message);
      case LOGGED_IN -> handleMessage(message);
    }
  }

  @OnMessage
  public void onBinaryMessage(byte @NotNull [] msg) {
    Validate.isTrue(msg[0] != '!', "Server pong from whatsapp, why did this get through?");
    Validate.isTrue(state == SocketState.LOGGED_IN, "Not logged in, did whatsapp send us a binary message to early?", IllegalStateException.class);

    var binaryMessage = BinaryArray.forArray(msg);
    var tagAndMessagePair = binaryMessage.indexOf(',').map(binaryMessage::split).orElseThrow();

    var messageTag  = tagAndMessagePair.getFirst().toString();
    var messageContent  = tagAndMessagePair.getSecond();

    var message = messageContent.slice(32);
    var hmacValidation = hmacSha256(message, Objects.requireNonNull(whatsappKeys.macKey()));
    Validate.isTrue(hmacValidation.equals(messageContent.cut(32)), "Cannot login: Hmac validation failed!", SecurityException.class);

    var decryptedMessage = aesDecrypt(message, Objects.requireNonNull(whatsappKeys.encKey()));
    var whatsappMessage = binaryDecoder.decodeDecryptedMessage(decryptedMessage);
    if(whatsappManager.resolvePendingRequest(messageTag, new BinaryResponse(whatsappMessage))){
      return;
    }

    whatsappManager.digestWhatsappNode(this, whatsappMessage);
  }

  private void sendInitialMessage(@NotNull String message) {
    if(whatsappKeys.mayRestore()) {
      restoreSession();
      return;
    }

    generateQrCode(message);
  }

  private void generateQrCode(@NotNull String message){
    if(state == SocketState.LOGGED_IN){
      return;
    }

    var res = JsonResponse.fromJson(message);
    var status = res.getInt("status");
    Validate.isTrue(status != 429, "Out of attempts to scan the QR code", IllegalStateException.class);

    var ttl = res.getInt("ttl");
    CompletableFuture.delayedExecutor(ttl, TimeUnit.MILLISECONDS).execute(() -> generateQrCode(message));

    var ref = res.getString("ref");
    Validate.isTrue(ref.isPresent(), "Cannot find ref for QR code generation");

    qrCode.generateQRCodeImage(ref.get(), whatsappKeys.publicKey(), whatsappKeys.clientId()).open();
    state(SocketState.WAITING_FOR_LOGIN);
  }

  private void restoreSession(){
    final var login = new TakeOverRequest(options, whatsappKeys){};
    login.send(session(), () -> state(SocketState.SOLVE_CHALLENGE));
  }

  private void solveChallenge(@NotNull String message){
    var res = JsonResponse.fromJson(message);
    res.getInteger("status").ifPresentOrElse(status -> {
      switch (status) {
        case 200, 405 -> state(SocketState.LOGGED_IN);
        case 401, 403, 409 -> {
          whatsappKeys.deleteKeysFromMemory();
          disconnect(null, false, true);
        }
        default -> sendChallenge(res);
      }
    }, () -> sendChallenge(res));
  }

  private void sendChallenge(@NotNull JsonResponse mapResponse){
    var challengeBase64 = mapResponse.getString("challenge");
    if(challengeBase64.isEmpty()){
      state(SocketState.LOGGED_IN);
      return;
    }

    var challenge = BinaryArray.forBase64(challengeBase64.get());
    var signedChallenge = hmacSha256(challenge, Objects.requireNonNull(whatsappKeys.macKey()));
    var solveChallengeRequest = new SolveChallengeRequest(options, whatsappKeys, signedChallenge){};
    solveChallengeRequest.send(session(), () -> state(SocketState.SENT_CHALLENGE));
  }

  private void handleChallengeResponse(@NotNull String message){
    var res = JsonResponse.fromJson(message);
    var status = res.getInteger("status");
    Validate.isTrue(status.isPresent() && status.get() == 200, "Could not solve whatsapp challenge for server and client token renewal: %s".formatted(message));
    state(SocketState.LOGGED_IN);
  }

  private void login(@NotNull String message){
    var res = JsonResponse.fromJson(message).toModel(UserInformationResponse.class);

    var base64Secret = res.secret();
    var secret = BinaryArray.forBase64(base64Secret);
    var pubKey = secret.cut(32);
    var sharedSecret = calculateSharedSecret(pubKey.data(), whatsappKeys.privateKey());
    var sharedSecretExpanded = hkdfExpand(sharedSecret, 80);

    var hmacValidation = hmacSha256(secret.cut(32).merged(secret.slice(64)), sharedSecretExpanded.slice(32, 64));
    Validate.isTrue(hmacValidation.equals(secret.slice(32, 64)), "Cannot login: Hmac validation failed!", SecurityException.class);

    var keysEncrypted = sharedSecretExpanded.slice(64).merged(secret.slice(64));
    var key = sharedSecretExpanded.cut(32);
    var keysDecrypted = aesDecrypt(keysEncrypted, key);

    whatsappKeys.initializeKeys(res.serverToken(), res.clientToken(), keysDecrypted.cut(32), keysDecrypted.slice(32, 64));
    state(SocketState.LOGGED_IN);
    whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onLoggedIn(res, true)));
  }

  public void connect() {
    Validate.isTrue(state == SocketState.NOTHING,  "WhatsappAPI: Cannot establish a connection with whatsapp as one already exists", IllegalStateException.class);
    openConnection();
    scheduler.scheduleAtFixedRate(this::sendPing, 0, 1, TimeUnit.MINUTES);
  }

  @SneakyThrows
  public void disconnect(@Nullable String reason, boolean logout, boolean reconnect){
    Validate.isTrue(state != SocketState.NOTHING, "WhatsappAPI: Cannot terminate the connection with whatsapp as it doesn't exist", IllegalStateException.class);
    whatsappManager.clear();
    if(logout) new LogOutRequest(options){}.send(session(), whatsappKeys::deleteKeysFromMemory);
    session().close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason));
    whatsappManager.listeners().forEach(WhatsappListener::onDisconnected);
    if(reconnect) openConnection();
  }

  @SneakyThrows
  private void openConnection() {
    webSocketContainer.connectToServer(this, URI.create(options.whatsappUrl()));
  }

  @SneakyThrows
  private void sendPing(){
    session().getAsyncRemote().sendPing(ByteBuffer.allocate(0));
  }

  private void handleMessage(@NotNull String message){
    var res = WhatsappResponse.fromJson(message);
    if(res.data() instanceof JsonListResponse listResponse){
      handleList(listResponse);
      return;
    }

    var mapResponse = (JsonResponse) res.data();
    if(mapResponse.data().isEmpty()){
      return;
    }

    if(whatsappManager.resolvePendingRequest(res.tag(), mapResponse)){
      return;
    }

    if(res.description() == null){
      return;
    }

    switch (res.description()){
      case "Conn" -> handleUserInformation(mapResponse.toModel(UserInformationResponse.class));
      case "Blocklist" -> handleBlocklist(mapResponse.toModel(BlocklistResponse.class));
      case "Cmd" -> handleCmd(mapResponse);
      case "Props" -> handleProps(mapResponse.toModel(PropsResponse.class));
      case "Presence" -> handlePresence(mapResponse.toModel(PresenceResponse.class));
      case "Msg", "MsgInfo" -> handleMessageInfo(mapResponse.toModel(AckResponse.class));
      case "Chat" -> handleChatCmd(mapResponse.toModel(ChatCmdResponse.class));
    }
  }

  private void handleChatCmd(@NotNull ChatCmdResponse cmdResponse){
    System.out.println("Cmd: " + cmdResponse);
    if(cmdResponse.cmd() == null){
      return;
    }

    var chatOpt = whatsappManager.findChatByJid(cmdResponse.id());
    if(chatOpt.isEmpty()){
      return;
    }

    var chat = chatOpt.get();
    if(!cmdResponse.data().contains("ephemeral")){
      return;
    }

    var time = (int) cmdResponse.data().get(2);
    chat.ephemeralMessageDuration(time);
    chat.ephemeralMessagesToggleTime(ZonedDateTime.now().toEpochSecond());
    whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onChatEphemeralStatusChange(chat)));
  }

  private void handleMessageInfo(@NotNull AckResponse ackResponse){
    if(ackResponse.cmd() == null){
      return;
    }

    var to = whatsappManager.findContactByJid(ackResponse.participant() != null ?  ackResponse.participant() : ackResponse.to());
    if(to.isEmpty()){
      return;
    }

    var chat = whatsappManager.findChatByJid(ackResponse.to());
    if(chat.isEmpty()){
      return;
    }

    Arrays.stream(ackResponse.ids())
            .map(id -> whatsappManager.findMessageById(chat.get(), id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(message -> {
              message.readStatus().put(to.get(), WhatsappProtobuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.forNumber(ackResponse.ack()));
              whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onMessageReadStatusUpdate(chat.get(), to.get(), message)));
            });
  }

  private void handleUserInformation(@NotNull UserInformationResponse info) {
    if(info.ref() == null){
      whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onInformationUpdate(info)));
      return;
    }

    whatsappManager.phoneNumber(WhatsappUtils.parseJid(info.ref()));
    whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onLoggedIn(info, false)));
  }

  private void handleBlocklist(@NotNull BlocklistResponse blocklist) {
    whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onBlocklistUpdate(blocklist)));
  }

  private void handleProps(@NotNull PropsResponse props) {
    whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onPropsUpdate(props)));
  }

  private void handleCmd(@NotNull JsonResponse res){
    if (!res.hasKey("type") || !res.hasKey("kind")) {
      return;
    }

    var kind = res.getString("kind").orElse("unknown");
    disconnect(kind, false, options.reconnectWhenDisconnected().apply(kind));
  }

  private void handlePresence(@NotNull PresenceResponse res){
    var chatOpt = whatsappManager.findChatByJid(res.jid());
    if(chatOpt.isEmpty()){
      return;
    }

    var chat = chatOpt.get();
    if(chat.isGroup()){
      if(res.participant() == null) {
        return;
      }

      var participantOpt = whatsappManager.findContactByJid(res.participant());
      if(participantOpt.isEmpty()){
        return;
      }

      var participant = participantOpt.get();
      chat.presences().put(participant, res.presence());
      whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onContactPresenceUpdate(chat, participant)));
      return;
    }

    var contactOpt = whatsappManager.findContactByJid(res.jid());
    if(contactOpt.isEmpty()){
      return;
    }

    var contact = contactOpt.get();
    if(res.offsetFromLastSeen() != null) {
      var instant = Instant.ofEpochSecond(res.offsetFromLastSeen() + contact.lastSeen().map(e -> e.toInstant().getEpochSecond()).orElse(0L));
      contact.lastSeen(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }else if(res.presence() == WhatsappContactStatus.UNAVAILABLE && contact.lastKnownPresence().isPresent() && (contact.lastKnownPresence().get() == WhatsappContactStatus.AVAILABLE || contact.lastKnownPresence().get() == WhatsappContactStatus.COMPOSING)){
      contact.lastSeen(ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
    }

    contact.lastKnownPresence(res.presence());
    whatsappManager.listeners().forEach(listener -> whatsappManager.callOnListenerThread(() -> listener.onContactPresenceUpdate(chat, contact)));
  }

  private void handleList(@NotNull JsonListResponse response) {
    whatsappManager.callOnListenerThread(() -> whatsappManager.listeners().forEach(whatsappListener -> whatsappListener.onListResponse(response)));
  }

  public @NotNull Session session() {
    return Objects.requireNonNull(session, "WhatsappAPI: The session linked to the WebSocket is null");
  }

  public @NotNull CompletableFuture<ChatResponse> queryChat(@NotNull String jid) {
    var node = WhatsappNodeBuilder
            .builder()
            .description("query")
            .attrs(Map.of("jid", jid, "epoch", String.valueOf(whatsappManager.tagAndIncrement()), "type", "chat"))
            .build();

    return new NodeRequest<ChatResponse>(options, node){}
            .send(session(), whatsappKeys, BinaryFlag.IGNORE, BinaryMetric.QUERY_CHAT)
            .future();
  }
}