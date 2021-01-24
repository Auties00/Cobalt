package it.auties.whatsapp4j.socket;

import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.api.WhatsappState;
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
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

  @SneakyThrows
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
  public void onBinaryMessage(byte[] msg) throws GeneralSecurityException {
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

  @SneakyThrows
  private void generateQrCode(@NotNull String message){
    if(state == WhatsappState.LOGGED_IN){
      return;
    }

    var res = Response.fromWhatsappResponse(message);
    var status = res.getInteger("status");
    Validate.isTrue(status != 429, "Out of attempts to scan the QR code");

    var ttl = res.getInteger("ttl");
    CompletableFuture.delayedExecutor(ttl, TimeUnit.MILLISECONDS).execute(() -> generateQrCode(message));

    qrCode.generateQRCodeImage(res.getNullableString("ref"), whatsappKeys.publicKey(), whatsappKeys.clientId()).open();
    this.state = WhatsappState.WAITING_FOR_LOGIN;
  }

  private void restoreSession(){
    final var login = new TakeOverRequest(whatsappKeys, options);
    sendJsonMessage(login, () -> this.state = WhatsappState.SOLVE_CHALLENGE);
  }

  @SneakyThrows
  private void solveChallenge(@NotNull String message){
    var res = Response.fromWhatsappResponse(message);
    var status = res.getNullableInteger("status");
    if(status != null){
      switch (status) {
        case 200, 405 -> {
          this.state = WhatsappState.LOGGED_IN;
          return;
        }

        case 401, 403, 409 -> {
          whatsappKeys.deleteKeysFromMemory();
          disconnect(null, false, true);
          return;
        }
      }
    }

    var challengeBase64 = res.getNullableString("challenge");
    if(challengeBase64 == null){
      this.state = WhatsappState.LOGGED_IN;
      return;
    }

    var challenge = BytesArray.forBase64(challengeBase64);
    var signedChallenge = hmacSha256(challenge, Objects.requireNonNull(whatsappKeys.macKey()));
    var solveChallengeRequest = new SolveChallengeRequest(whatsappKeys, options, signedChallenge);
    sendJsonMessage(solveChallengeRequest, () -> this.state = WhatsappState.SENT_CHALLENGE);
  }

  @SneakyThrows
  private void handleChallengeResponse(@NotNull String message){
    var res = Response.fromWhatsappResponse(message);
    var status = res.getNullableInteger("status");
    Validate.isTrue(status != null && status == 200, "Could not solve whatsapp challenge for server and client token renewal: %s".formatted(message));
    this.state = WhatsappState.LOGGED_IN;
  }

  @SneakyThrows
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

  @SneakyThrows
  private void handleMessage(@NotNull String message){
    var res = Response.fromJson(message);

    if(res.data() instanceof ListResponse listResponse){
      handleList(listResponse);
      return;
    }

    var mapResponse = (MapResponse) res.data();
    if(whatsappManager.isPendingMessageId(res.tag())){
      whatsappManager.resolvePendingMessage(res.tag(), mapResponse.getInteger("status"));
      return;
    }


    if(res.description() == null){
      return;
    }

    switch (res.description()){
      case "Conn" -> handleUserInformation(mapResponse.toModel(WhatsappUserInformation.class));
      case "Blocklist" -> handleBlocklist(mapResponse.toModel(WhatsappBlocklist.class));
      case "Cmd" -> handleCmd(mapResponse);
    }
  }

  private void handleUserInformation(@NotNull WhatsappUserInformation info) {
    if(info.getRef() == null){
      listeners.forEach(listener -> listener.onInformationUpdate(info));
      return;
    }

    whatsappManager.phoneNumber(info.getWid().replaceAll("@c.us", "@s.whatsapp.net"));
    listeners.forEach(listener -> listener.onConnected(info, false));
  }

  private void handleBlocklist(@NotNull WhatsappBlocklist blocklist) {
    listeners.forEach(listener -> listener.onBlocklistUpdate(blocklist));
  }

  private void handleCmd(@NotNull MapResponse res){
    if (!res.hasKey("type") || !res.hasKey("kind")) {
      return;
    }

    var kind = res.getNullableString("kind");
    disconnect(kind, false, options.reconnectWhenDisconnected().apply(kind));
  }

  private void handleList(@NotNull ListResponse listResponse){

  }
}