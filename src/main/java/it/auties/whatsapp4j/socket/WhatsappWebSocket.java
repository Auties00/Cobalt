package it.auties.whatsapp4j.socket;

import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.api.WhatsappState;
import it.auties.whatsapp4j.constant.UserPresence;
import it.auties.whatsapp4j.model.*;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.request.*;
import it.auties.whatsapp4j.response.ListResponse;
import it.auties.whatsapp4j.response.MapResponse;
import it.auties.whatsapp4j.response.Response;
import it.auties.whatsapp4j.utils.*;
import jakarta.websocket.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
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

@ClientEndpoint(configurator = WebSocketConfiguration.class)
@AllArgsConstructor
@Accessors(fluent = true)
public class WhatsappWebSocket {
  private Session session;
  private ScheduledExecutorService scheduler;
  private @Getter @NotNull WhatsappState state;
  private final @NotNull List<WhatsappListener> listeners;
  private final @NotNull WebSocketContainer webSocketContainer;
  private final @NotNull WhatsappDataManager whatsappManager;
  private final @NotNull WhatsappKeysManager whatsappKeys;
  private final @NotNull WhatsappConfiguration options;
  private final @NotNull WhatsappQRCode qrCode;
  private final @NotNull BinaryDecoder binaryDecoder;
  private final @NotNull BinaryEncoder binaryEncoder;

  public WhatsappWebSocket(@NotNull List<WhatsappListener> listeners, @NotNull WhatsappConfiguration options, @NotNull WhatsappKeysManager whatsappKeys) {
    this(null, null, WhatsappState.NOTHING, listeners, ContainerProvider.getWebSocketContainer(), WhatsappDataManager.singletonInstance(), whatsappKeys, options, new WhatsappQRCode(), new BinaryDecoder(), new BinaryEncoder(new ArrayList<>()));
    webSocketContainer.setDefaultMaxSessionIdleTimeout(Duration.ofDays(30).toMillis());
  }

  @SneakyThrows
  public void sendJsonMessage(@NotNull WhatsappRequest request){
    var json = request.toJson();
    if (options.async()) {
      session.getAsyncRemote().sendObject(json);
    } else {
      session.getBasicRemote().sendObject(json);
    }
  }

  @SneakyThrows
  public void sendJsonMessage(@NotNull WhatsappRequest request, @NotNull Runnable handler){
    var json = request.toJson();
    if (options.async()) {
      session.getAsyncRemote().sendObject(json, __ -> handler.run());
    } else {
      session.getBasicRemote().sendObject(json);
      handler.run();
    }
  }

  public void sendBinaryMessage(@NotNull WhatsappNode node, byte... tags) {
    Validate.isTrue(state == WhatsappState.LOGGED_IN, "WhatsappAPI: Cannot send a binary message, wrong state: %s".formatted(state.name()));

    var messageTag = BytesArray.forString(options.tag() + ',');
    var encodedMessage = binaryEncoder.encodeMessage(node);
    var encrypted = aesEncrypt(encodedMessage, Objects.requireNonNull(whatsappKeys.encKey()));
    var hmacSign = hmacSha256(encrypted, Objects.requireNonNull(whatsappKeys.macKey()));
    var binaryMessage = messageTag.merged(BytesArray.forArray(tags)).merged(hmacSign).merged(encrypted).toBuffer();

    session.getAsyncRemote().sendBinary(binaryMessage);
  }

  @OnOpen
  public void onOpen(@NotNull Session session) {
    if(this.session == null || !session.isOpen()) {
      this.session = session;
    }

    final var login = new InitialRequest(whatsappKeys, options);
    sendJsonMessage(login, () -> this.state = WhatsappState.SENT_INITIAL_MESSAGE);
  }

  @OnMessage
  public void onMessage(@NotNull String message) {
    switch (state){
      case SENT_INITIAL_MESSAGE -> {
        if(whatsappKeys.mayRestore()) {
          restoreSession();
        }else {
          generateQrCode(message);
        }
      }
      case SOLVE_CHALLENGE -> solveChallenge(message);
      case SENT_CHALLENGE -> handleChallengeResponse(message);
      case WAITING_FOR_LOGIN -> login(message);
      case LOGGED_IN -> handleMessage(message);
    }
  }

  @OnMessage
  public void onBinaryMessage(byte[] msg) {
    Validate.isTrue(msg[0] != '!', "Server pong from whatsapp, why did this get through?");
    Validate.isTrue(state == WhatsappState.LOGGED_IN, "Not logged in, did whatsapp send us a binary message to early?");

    var binaryMessage = BytesArray.forArray(msg);
    var tagAndMessagePair = binaryMessage.indexOf(',').map(binaryMessage::split).orElseThrow();

    var messageTag  = tagAndMessagePair.getFirst().toString();
    var messageContent  = tagAndMessagePair.getSecond();

    var message = messageContent.slice(32);
    var hmacValidation = hmacSha256(message, Objects.requireNonNull(whatsappKeys.macKey()));
    Validate.isTrue(hmacValidation.equals(messageContent.cut(32)), "Cannot login: Hmac validation failed!");

    var decryptedMessage = aesDecrypt(message, Objects.requireNonNull(whatsappKeys.encKey()));
    var whatsappMessage = binaryDecoder.decodeDecryptedMessage(decryptedMessage);
    whatsappManager.digestWhatsappNode(whatsappMessage, listeners);
  }

  private void generateQrCode(@NotNull String message){
    if(state == WhatsappState.LOGGED_IN){
      return;
    }

    var res = Response.fromWhatsappResponse(message);
    var status = res.getInt("status");
    Validate.isTrue(status != 429, "Out of attempts to scan the QR code");

    var ttl = res.getInt("ttl");
    CompletableFuture.delayedExecutor(ttl, TimeUnit.MILLISECONDS).execute(() -> generateQrCode(message));

    var ref = res.getString("ref");
    Validate.isTrue(ref.isPresent(), "Cannot find ref for QR code generation");

    qrCode.generateQRCodeImage(ref.get(), whatsappKeys.publicKey(), whatsappKeys.clientId()).open();
    this.state = WhatsappState.WAITING_FOR_LOGIN;
  }

  private void restoreSession(){
    final var login = new TakeOverRequest(whatsappKeys, options);
    sendJsonMessage(login, () -> this.state = WhatsappState.SOLVE_CHALLENGE);
  }

  private void solveChallenge(@NotNull String message){
    var res = Response.fromWhatsappResponse(message);
    res.getInteger("status").ifPresentOrElse(status -> {
      switch (status) {
        case 200, 405 -> this.state = WhatsappState.LOGGED_IN;
        case 401, 403, 409 -> {
          whatsappKeys.deleteKeysFromMemory();
          disconnect(null, false, true);
        }
        default -> sendChallenge(res);
      }
    }, () -> sendChallenge(res));
  }

  private void sendChallenge(@NotNull MapResponse res){
    var challengeBase64 = res.getString("challenge");
    if(challengeBase64.isEmpty()){
      this.state = WhatsappState.LOGGED_IN;
      return;
    }

    var challenge = BytesArray.forBase64(challengeBase64.get());
    var signedChallenge = hmacSha256(challenge, Objects.requireNonNull(whatsappKeys.macKey()));
    var solveChallengeRequest = new SolveChallengeRequest(whatsappKeys, options, signedChallenge);
    sendJsonMessage(solveChallengeRequest, () -> this.state = WhatsappState.SENT_CHALLENGE);
  }

  private void handleChallengeResponse(@NotNull String message){
    var res = Response.fromWhatsappResponse(message);
    var status = res.getInteger("status");
    Validate.isTrue(status.isPresent() && status.get() == 200, "Could not solve whatsapp challenge for server and client token renewal: %s".formatted(message));
    this.state = WhatsappState.LOGGED_IN;
  }

  private void login(@NotNull String message){
    var res = Response.fromWhatsappResponse(message).toModel(WhatsappUserInformation.class);

    var base64Secret = res.getSecret();
    var secret = BytesArray.forBase64(base64Secret);
    var pubKey = secret.cut(32);
    var sharedSecret = calculateSharedSecret(pubKey.data(), whatsappKeys.privateKey());
    var sharedSecretExpanded = hkdfExpand(sharedSecret, 80);


    var hmacValidation = hmacSha256(secret.cut(32).merged(secret.slice(64)), sharedSecretExpanded.slice(32, 64));
    Validate.isTrue(hmacValidation.equals(secret.slice(32, 64)), "Cannot login: Hmac validation failed!");

    var keysEncrypted = sharedSecretExpanded.slice(64).merged(secret.slice(64));
    var key = sharedSecretExpanded.cut(32);
    var keysDecrypted = aesDecrypt(keysEncrypted, key);

    whatsappKeys.initializeKeys(res.getServerToken(), res.getClientToken(), keysDecrypted.cut(32), keysDecrypted.slice(32, 64));
    this.state = WhatsappState.LOGGED_IN;
    listeners.forEach(listener -> listener.onConnected(res, true));
  }

  @SneakyThrows
  public void connect() {
    Validate.isTrue(state == WhatsappState.NOTHING, "WhatsappAPI: Cannot establish a connection with whatsapp as one already exists");
    webSocketContainer.connectToServer(this, URI.create(options.whatsappUrl()));
    this.scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(this::sendPing, 0, 1, TimeUnit.MINUTES);
  }

  @SneakyThrows
  public @NotNull WhatsappWebSocket disconnect(@Nullable String reason, boolean logout, boolean reconnect){
    Validate.isTrue(state != WhatsappState.NOTHING, "WhatsappAPI: Cannot terminate the connection with whatsapp as it doesn't exist");
    if(logout) sendJsonMessage(new LogOutRequest(whatsappKeys, options), whatsappKeys::deleteKeysFromMemory);

    whatsappManager.clear();
    session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason));
    listeners.forEach(WhatsappListener::onDisconnected);

    this.state = WhatsappState.NOTHING;
    if(!reconnect) {
      return this;
    }

    var newSocket = new WhatsappWebSocket(listeners, options, whatsappKeys);
    newSocket.connect();
    return newSocket;
  }

  @SneakyThrows
  private void sendPing(){
    session.getAsyncRemote().sendPing(ByteBuffer.allocate(0));
  }

  private void handleMessage(@NotNull String message){
    var res = Response.fromJson(message);
    if(res.data() instanceof ListResponse listResponse){
      handleList(listResponse);
      return;
    }

    var mapResponse = (MapResponse) res.data();
    if(whatsappManager.isPendingMessageId(res.tag())){
      whatsappManager.resolvePendingMessage(res.tag(), mapResponse.getInt("status"));
      return;
    }

    if(res.description() == null){
      return;
    }

    switch (res.description()){
      case "Conn" -> handleUserInformation(mapResponse.toModel(WhatsappUserInformation.class));
      case "Blocklist" -> handleBlocklist(mapResponse.toModel(WhatsappBlocklist.class));
      case "Cmd" -> handleCmd(mapResponse);
      case "Props" -> handleProps(mapResponse.toModel(WhatsappProps.class));
      case "Presence" -> handlePresence(mapResponse.toModel(WhatsappPresence.class));
      case "Msg", "MsgInfo" -> {}
    }
  }

  private void handleUserInformation(@NotNull WhatsappUserInformation info) {
    if(info.getRef() == null){
      listeners.forEach(listener -> listener.onInformationUpdate(info));
      return;
    }

    whatsappManager.phoneNumber(WhatsappIdUtils.parseJid(info.getWid()));
    listeners.forEach(listener -> listener.onConnected(info, false));
  }

  private void handleBlocklist(@NotNull WhatsappBlocklist blocklist) {
    listeners.forEach(listener -> listener.onBlocklistUpdate(blocklist));
  }

  private void handleProps(@NotNull WhatsappProps props) {
    listeners.forEach(listener -> listener.onPropsReceived(props));
  }

  private void handleCmd(@NotNull MapResponse res){
    if (!res.hasKey("type") || !res.hasKey("kind")) {
      return;
    }

    var kind = res.getString("kind").orElse("unknown");
    disconnect(kind, false, options.reconnectWhenDisconnected().apply(kind));
  }

  private void handlePresence(@NotNull WhatsappPresence res){
    var chatOpt = whatsappManager.findChatByJid(res.getId());
    if(chatOpt.isEmpty()){
      return;
    }

    var chat = chatOpt.get();
    if(chat.isGroup()){
      if(res.getParticipant() == null) {
        return;
      }

      var participantOpt = whatsappManager.findContactByJid(res.getParticipant());
      if(participantOpt.isEmpty()){
        return;
      }

      var participant = participantOpt.get();
      chat.presences().put(participant, res.getPresence());
      return;
    }

    var contactOpt = whatsappManager.findContactByJid(res.getId());
    if(contactOpt.isEmpty()){
      return;
    }

    var contact = contactOpt.get();
    if(res.getOffsetFromLastSeen() != null) {
      var lastSeen = contact.lastSeen();
      var instant = lastSeen == null ? Instant.ofEpochSecond(res.getOffsetFromLastSeen()) : lastSeen.toInstant().plusSeconds(res.getOffsetFromLastSeen());
      contact.lastSeen(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }else if(res.getPresence() == UserPresence.UNAVAILABLE && (contact.lastKnownPresence() == UserPresence.AVAILABLE || contact.lastKnownPresence() == UserPresence.COMPOSING)){
      contact.lastSeen(ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
    }
    contact.lastKnownPresence(res.getPresence());
  }

  private void handleList(@NotNull ListResponse listResponse){
    //TODO: Handle List Response
  }
}