package it.auties.whatsapp4j.api;

import it.auties.whatsapp4j.configuration.WebSocketConfiguration;
import it.auties.whatsapp4j.configuration.WhatsappConfiguration;
import it.auties.whatsapp4j.model.InitialRequest;
import it.auties.whatsapp4j.model.LogOutRequest;
import it.auties.whatsapp4j.model.Response;
import it.auties.whatsapp4j.utils.BinaryMessengerReader;
import it.auties.whatsapp4j.utils.BytesArray;
import it.auties.whatsapp4j.utils.QRUtils;
import it.auties.whatsapp4j.utils.Validate;
import it.auties.whatsapp4j.model.WhatsappKeys;
import jakarta.websocket.*;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.whispersystems.curve25519.Curve25519KeyPair;

import java.net.URI;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static it.auties.whatsapp4j.utils.CypherUtils.*;

@ClientEndpoint(configurator = WebSocketConfiguration.class)
@Data
@Accessors(fluent = true)
public class WhatsappAPI {
  private final Session session;
  private final WhatsappConfiguration options;
  private final Curve25519KeyPair pair;
  private WhatsappState state;
  private WhatsappKeys keys;

  public WhatsappAPI() {
    this(WhatsappConfiguration.defaultOptions());
  }

  @SneakyThrows
  public WhatsappAPI(WhatsappConfiguration options) {
    this.pair = calculateRandomKeyPair();
    this.options = options;
    this.state = WhatsappState.NOTHING;
    this.session = startWebSocket();
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
    Runtime.getRuntime().addShutdownHook(new Thread(this::closeSession));
    return session;
  }

  public void closeSession(){
    final var logout = new LogOutRequest();
    session.getAsyncRemote().sendObject(logout.toJson(), __ -> this.state = WhatsappState.LOGGED_OFF);
  }

  @SneakyThrows
  @OnOpen
  public void onOpen(@NotNull Session session) {
    final var login = new InitialRequest(options);
    session.getAsyncRemote().sendObject(login.toJson(), __ -> this.state = WhatsappState.SENT_INITIAL_MESSAGE);
  }

  @OnMessage
  public void onMessage(@NotNull String message) {
    switch (state){
      case SENT_INITIAL_MESSAGE -> generateQrCode(message);
      case WAITING_FOR_LOGIN -> login(message);
      case LOGGED_OFF -> System.exit(0);
    }
  }

  @SneakyThrows
  private void generateQrCode(@NotNull String message){
    var ref = Response.fromJson(message).getValue("ref").orElseThrow();
    var qr = "%s,%s,%s".formatted(ref, Base64.getEncoder().encodeToString(pair.getPublicKey()), options.clientId());
    var image = QRUtils.generateQRCodeImage(qr);
    QRUtils.saveAndOpenQrCode(image);

    this.state = WhatsappState.WAITING_FOR_LOGIN;
  }

  @SneakyThrows
  private void login(@NotNull String message){
    var base64Secret = Response.fromJson(message).getValue("secret").orElseThrow();
    var secret = BytesArray.forBase64(base64Secret);
    var pubKey = secret.cut(32);
    var sharedSecret = calculateSharedSecret(pubKey.data(), pair.getPrivateKey());
    var sharedSecretExpanded = hkdfExpand(sharedSecret, 80);

    var hmacValidation = hmacSha256(secret.cut(32).merged(secret.slice(64)), sharedSecretExpanded.slice(32, 64));
    Validate.isTrue(hmacValidation.equals(secret.slice(32, 64)), "Cannot login: Hmac validation failed!");

    var keysEncrypted = sharedSecretExpanded.slice(64).merged(secret.slice(64));
    var key = sharedSecretExpanded.cut(32);
    var keysDecrypted = aesDecrypt(keysEncrypted, key);

    this.keys = new WhatsappKeys(keysDecrypted.cut(32), keysDecrypted.slice(32, 64));
    Validate.isTrue(keys.encKey().size() == 32, "Enc key is not 32 bytes!");
    Validate.isTrue(keys.macKey().size() == 32, "Mac key is not 32 bytes!");
    this.state = WhatsappState.LOGGED_IN;
  }

  @OnMessage
  public void onBinaryMessage(byte[] msg) throws GeneralSecurityException {
    Validate.isTrue(state == WhatsappState.LOGGED_IN, "Not logged in, did whatsapp send us a binary message to early?");
    Validate.notNull(keys, "Keys are null, did whatsapp send us a binary message to early?");

    var binaryMessage = BytesArray.forArray(msg);
    var tagAndMessagePair = binaryMessage.indexOf((byte) ',').map(binaryMessage::split).orElseThrow();

    var tag = tagAndMessagePair.getFirst();
    var message = tagAndMessagePair.getSecond();
    var hmacValidation = hmacSha256(message.slice(32), keys.macKey());
    Validate.isTrue(hmacValidation.equals(message.cut(32)), "Cannot login: Hmac validation failed!");

    var decryptedMessage = aesDecrypt(message.slice(32), keys.encKey());
    Validate.isTrue(decryptedMessage.isNotEmpty(), "Empty message, magic?");

    System.out.println();
    System.out.printf("Tag: %s%n", tag.toASCIIString());
    System.out.printf("Data: %s%nKey: %s%nResult: %s%n", message, keys.encKey(), decryptedMessage);
  }
}