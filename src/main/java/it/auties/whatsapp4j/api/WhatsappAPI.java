package it.auties.whatsapp4j.api;

import it.auties.whatsapp4j.configuration.WebSocketConfiguration;
import it.auties.whatsapp4j.configuration.WhatsappConfiguration;
import it.auties.whatsapp4j.model.*;
import it.auties.whatsapp4j.utils.*;
import jakarta.websocket.*;
import lombok.SneakyThrows;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static it.auties.whatsapp4j.utils.CypherUtils.*;

@ClientEndpoint(configurator = WebSocketConfiguration.class)
public class WhatsappAPI {
  private final @NotNull Session session;
  private final @NotNull WhatsappConfiguration options;
  private final @NotNull WhatsappQRCode qrCode;
  private final @NotNull WhatsappKeys whatsappKeys;
  private final @NotNull BinaryDecoder binaryDecoder;
  private @NotNull WhatsappState state;

  public WhatsappAPI() {
    this(WhatsappConfiguration.defaultOptions());
  }

  @SneakyThrows
  public WhatsappAPI(@NotNull WhatsappConfiguration options) {
    this.whatsappKeys = WhatsappKeys.buildInstance();
    this.options = options;
    this.qrCode = new WhatsappQRCode();
    this.state = WhatsappState.NOTHING;
    this.session = startWebSocket();
    this.binaryDecoder = new BinaryDecoder();
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::sendPing, 0, 1, TimeUnit.MINUTES);
  }

  @SneakyThrows
  private void sendPing(){
    session.getAsyncRemote().sendPing(ByteBuffer.allocate(0));
  }

  @SneakyThrows
  private Session startWebSocket(){
    var container = ContainerProvider.getWebSocketContainer();
    container.setDefaultMaxSessionIdleTimeout(Duration.ofDays(30).toMillis());

    var session = container.connectToServer(this, URI.create(options.whatsappUrl()));
    Runtime.getRuntime().addShutdownHook(new Thread(this::logout));
    return session;
  }

  public void logout(){
    final var logout = new LogOutRequest();
    session.getAsyncRemote().sendObject(logout.toJson(), __ -> this.state = WhatsappState.LOGGED_OFF);
  }

  @SneakyThrows
  @OnOpen
  public void onOpen(@NotNull Session session) {
    final var login = new InitialRequest(whatsappKeys, options);
    session.getAsyncRemote().sendObject(login.toJson(), __ -> this.state = WhatsappState.SENT_INITIAL_MESSAGE);
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
      case LOGGED_OFF -> System.exit(0);
    }
  }

  @SneakyThrows
  private void generateQrCode(@NotNull String message){
    var res = Response.fromJson(message);

    var status = res.getNumber("status");
    Validate.isTrue(status != 429, "Out of attempts to scan the QR code");

    var ttl = res.getNumber("ttl");
    CompletableFuture.delayedExecutor(ttl, TimeUnit.MILLISECONDS).execute(() -> onOpen(session));

    qrCode.generateQRCodeImage(res.getNullableString("ref"), whatsappKeys.publicKey(), whatsappKeys.clientId()).open();
    this.state = WhatsappState.WAITING_FOR_LOGIN;
  }

  @SneakyThrows
  private void restoreSession(){
    final var login = new TakeOverRequest(whatsappKeys, options);
    session.getAsyncRemote().sendObject(login.toJson(), __ -> this.state = WhatsappState.SOLVE_CHALLENGE);
  }

  @SneakyThrows
  private void solveChallenge(@NotNull String message){
    var res = Response.fromJson(message);
    var status = res.getNullableInteger("status");
    if (status != null && status == 200){
      this.state = WhatsappState.LOGGED_IN;
      return;
    }

    var challenge = BytesArray.forBase64(res.getString("challenge"));
    var signedChallenge = hmacSha256(challenge, Objects.requireNonNull(whatsappKeys.macKey()));
    var solveChallengeRequest = new SolveChallengeRequest(signedChallenge, whatsappKeys, options);
    session.getAsyncRemote().sendObject(solveChallengeRequest.toJson(), __ -> this.state = WhatsappState.SENT_CHALLENGE);
  }

  @SneakyThrows
  private void handleChallengeResponse(@NotNull String message){
    var status = Response.fromJson(message).getNullableInteger("status");
    if (status == null || status != 200) {
      throw new RuntimeException("Could not solve whatsapp challenge for server and client token renewal: %s".formatted(message));
    }

    this.state = WhatsappState.LOGGED_IN;
  }

  @SneakyThrows
  private void login(@NotNull String message){
    var res = Response.fromJson(message);

    var base64Secret = res.getString("secret");
    var secret = BytesArray.forBase64(base64Secret);
    var pubKey = secret.cut(32);
    var sharedSecret = calculateSharedSecret(pubKey.data(), whatsappKeys.privateKey());
    var sharedSecretExpanded = hkdfExpand(sharedSecret, 80);

    var hmacValidation = hmacSha256(secret.cut(32).merged(secret.slice(64)), sharedSecretExpanded.slice(32, 64));
    Validate.isTrue(hmacValidation.equals(secret.slice(32, 64)), "Cannot login: Hmac validation failed!");

    var keysEncrypted = sharedSecretExpanded.slice(64).merged(secret.slice(64));
    var key = sharedSecretExpanded.cut(32);
    var keysDecrypted = aesDecrypt(keysEncrypted, key);

    whatsappKeys.initializeKeys(res.getString("serverToken"), res.getString("clientToken"), keysDecrypted.cut(32), keysDecrypted.slice(32, 64));
    this.state = WhatsappState.LOGGED_IN;
  }

  @SneakyThrows
  private void handleMessage(@NotNull String message){
    var res = Response.fromJson(message);
    var type = res.getNullableString("type");
    var kind = res.getNullableString("kind");
    if(type != null && kind != null){
      System.out.printf("Disconnected from whatsapp web, reason: %s%n", kind);
      System.exit(0);
    }
  }

  @OnMessage
  public void onBinaryMessage(@NotNull byte[] msg) throws GeneralSecurityException {
    Validate.isTrue(state == WhatsappState.LOGGED_IN, "Not logged in, did whatsapp send us a binary message to early?");

    var binaryMessage = BytesArray.forArray(msg);
    var tagAndMessagePair = binaryMessage.indexOf((byte) ',').map(binaryMessage::split).orElseThrow();

    var messageTag  = tagAndMessagePair.getFirst().toASCIIString();
    var messageContent  = tagAndMessagePair.getSecond();

    var message = messageContent.slice(32);
    var hmacValidation = hmacSha256(message, Objects.requireNonNull(whatsappKeys.macKey()));
    Validate.isTrue(hmacValidation.equals(messageContent.cut(32)), "Cannot login: Hmac validation failed!");

    var decryptedMessage = aesDecrypt(message, Objects.requireNonNull(whatsappKeys.encKey()));
    System.out.println(binaryDecoder.decodeDecryptedMessage(messageTag, decryptedMessage));
    System.out.println();
  }
}