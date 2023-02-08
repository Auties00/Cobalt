package it.auties.whatsapp.socket;

import static java.lang.Long.parseLong;
import static java.util.Base64.getUrlEncoder;
import static java.util.Map.entry;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.api.HistoryLength;
import it.auties.whatsapp.api.WhatsappOptions.MobileOptions;
import it.auties.whatsapp.api.WhatsappOptions.WebOptions;
import it.auties.whatsapp.crypto.Handshake;
import it.auties.whatsapp.model.phone.PhoneNumber;
import it.auties.whatsapp.model.phone.PhoneNumberResponse;
import it.auties.whatsapp.model.request.Attributes;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.signal.auth.ClientFinish;
import it.auties.whatsapp.model.signal.auth.ClientPayload;
import it.auties.whatsapp.model.signal.auth.Companion;
import it.auties.whatsapp.model.signal.auth.Companion.CompanionPropsPlatformType;
import it.auties.whatsapp.model.signal.auth.CompanionData;
import it.auties.whatsapp.model.signal.auth.HandshakeMessage;
import it.auties.whatsapp.model.signal.auth.UserAgent;
import it.auties.whatsapp.model.signal.auth.WebInfo;
import it.auties.whatsapp.model.signal.auth.WebInfo.WebInfoWebSubPlatform;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.Specification;
import it.auties.whatsapp.util.Specification.Whatsapp;
import it.auties.whatsapp.util.Validate;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
class AuthHandler extends Handler
    implements JacksonProvider {
  private static final String DUMMY_PHONE_NUMBER = "16012345678";

  private final SocketHandler socketHandler;
  private Handshake handshake;

  protected void createHandshake() {
    this.handshake = new Handshake(socketHandler.keys());
    handshake.updateHash(socketHandler.keys().ephemeralKeyPair().publicKey());
  }

  @SneakyThrows
  protected CompletableFuture<Void> login(SocketSession session, byte[] message) {
    var serverHello = PROTOBUF.readMessage(message, HandshakeMessage.class)
        .serverHello();
    handshake.updateHash(serverHello.ephemeral());
    var sharedEphemeral = Curve25519.sharedKey(serverHello.ephemeral(), socketHandler.keys()
        .ephemeralKeyPair()
        .privateKey());
    handshake.mixIntoKey(sharedEphemeral);
    var decodedStaticText = handshake.cipher(serverHello.staticText(), false);
    var sharedStatic = Curve25519.sharedKey(decodedStaticText, socketHandler.keys()
        .ephemeralKeyPair()
        .privateKey());
    handshake.mixIntoKey(sharedStatic);
    handshake.cipher(serverHello.payload(), false);
    var encodedKey = handshake.cipher(socketHandler.keys()
        .noiseKeyPair()
        .publicKey(), true);
    var sharedPrivate = Curve25519.sharedKey(serverHello.ephemeral(), socketHandler.keys()
        .noiseKeyPair()
        .privateKey());
    handshake.mixIntoKey(sharedPrivate);
    var encodedPayload = handshake.cipher(createUserPayload(), true);
    var clientFinish = new ClientFinish(encodedKey, encodedPayload);
    var handshakeMessage = new HandshakeMessage(clientFinish);
    return Request.of(handshakeMessage)
        .sendWithNoResponse(session, socketHandler.keys(), socketHandler.store())
        .thenRunAsync(socketHandler.keys()::clearReadWriteKey)
        .thenRunAsync(handshake::finish);
  }

  private byte[] createUserPayload() {
    try {
      var builder = ClientPayload.builder()
          .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
          .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
          .userAgent(createUserAgent())
          .webInfo(new WebInfo(getWebPlatform()));
      return PROTOBUF.writeValueAsBytes(finishUserPayload(builder));
    }catch (IOException exception){
      throw new RuntimeException("Cannot create user payload", exception);
    }
  }

  private WebInfoWebSubPlatform getWebPlatform() {
    if (!(socketHandler.options() instanceof WebOptions webOptions)) {
      return null;
    }
    return webOptions.historyLength() == HistoryLength.ONE_YEAR ? WebInfoWebSubPlatform.WIN_STORE
        : WebInfoWebSubPlatform.WEB_BROWSER;
  }

  private ClientPayload finishUserPayload(ClientPayload.ClientPayloadBuilder builder) {
    if (socketHandler.store().userCompanionJid() != null) {
      return builder.username(parseLong(socketHandler.store()
              .userCompanionJid()
              .user()))
          .passive(true)
          .device(socketHandler.store()
              .userCompanionJid()
              .device())
          .build();
    }
    return builder.regData(createRegisterData())
        .passive(false)
        .build();
  }

  private UserAgent createUserAgent() {
    if (!(socketHandler.options() instanceof WebOptions webOptions)) {
      return null;
    }
    return UserAgent.builder()
        .appVersion(webOptions.version())
        .platform(UserAgent.UserAgentPlatform.WEB)
        .releaseChannel(UserAgent.UserAgentReleaseChannel.RELEASE)
        .build();
  }

  @SneakyThrows
  private CompanionData createRegisterData() {
    var companion = CompanionData.builder()
        .buildHash(socketHandler.options()
            .version()
            .toHash())
        .id(BytesHelper.intToBytes(socketHandler.keys()
            .id(), 4))
        .keyType(BytesHelper.intToBytes(Specification.Signal.KEY_TYPE, 1))
        .identifier(socketHandler.keys()
            .identityKeyPair()
            .publicKey())
        .signatureId(socketHandler.keys()
            .signedKeyPair()
            .encodedId())
        .signaturePublicKey(socketHandler.keys()
            .signedKeyPair()
            .keyPair()
            .publicKey())
        .signature(socketHandler.keys()
            .signedKeyPair()
            .signature());
    if(socketHandler.options().clientType() == ClientType.WEB_CLIENT){
      var props = PROTOBUF.writeValueAsBytes(createCompanionProps());
      companion.companion(props);
    }

    return companion.build();
  }

  private Companion createCompanionProps() {
    if (!(socketHandler.options() instanceof WebOptions webOptions)) {
      return null;
    }
    return Companion.builder()
        .os(webOptions.name())
        .platformType(getWebBrowser(webOptions))
        .requireFullSync(webOptions.historyLength() == HistoryLength.ONE_YEAR)
        .build();
  }

  private CompanionPropsPlatformType getWebBrowser(WebOptions webOptions) {
    return webOptions.historyLength() == HistoryLength.ONE_YEAR ? CompanionPropsPlatformType.DESKTOP
        : CompanionPropsPlatformType.CHROME;
  }

  @SneakyThrows
  public void registerPhoneNumber(){
    if(socketHandler.options().clientType() != ClientType.APP_CLIENT){
      return;
    }

    var options = (MobileOptions) socketHandler.options();
    var phoneNumber = PhoneNumber.of(options.phoneNumber())
        .orElseThrow(() -> new IllegalArgumentException("Cannot parse phone number: %s".formatted(
            options.phoneNumber())));
    askForVerificationCode(options, phoneNumber);
    var code = options.verificationCodeHandler().apply(options.verificationCodeMethod());
    sendVerificationCode(phoneNumber, code);

  }

  private void sendVerificationCode(PhoneNumber phoneNumber, String code) {
    var registerOptions = getRegistrationOptions(
        phoneNumber,
        entry("code", code.replaceAll("-", ""))
    );
    var response = sendRegistrationRequest("/register", registerOptions);
    Validate.isTrue(response.statusCode() == 200,
        "Unexpected response status code: %s", response.statusCode());
  }

  private void askForVerificationCode(MobileOptions options, PhoneNumber phoneNumber) {
    try {
      var codeOptions = getRegistrationOptions(
          phoneNumber,
          entry("mcc", phoneNumber.countryCode().mcc()),
          entry("mnc", phoneNumber.countryCode().mnc()),
          entry("sim_mcc",  "000"),
          entry("sim_mnc",  "000"),
          entry("method", options.verificationCodeMethod().type()),
          entry("reason", ""),
          entry("hasav", "1")
      );
      var codeResponse = sendRegistrationRequest("/code", codeOptions);
      var phoneNumberResponse = JSON.readValue(codeResponse.body(), PhoneNumberResponse.class);
      Validate.isTrue(isResponseValid(phoneNumberResponse),
          "Unexpected response: %s", phoneNumberResponse);
    }catch (IOException exception){
      throw new RuntimeException("Cannot get verification code", exception);
    }
  }

  private HttpResponse<String> sendRegistrationRequest(String path, Map<String, Object> params) {
    try {
      var client = HttpClient.newHttpClient();
      var request = HttpRequest.newBuilder()
          .uri(URI.create("%s/%s".formatted(Whatsapp.MOBILE_REGISTRATION_ENDPOINT, path)))
          .header("User-Agent", Whatsapp.MOBILE_USER_AGENT)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .PUT(getBodyPublishers(params))
          .build();
      return client.send(request, BodyHandlers.ofString());
    } catch (IOException | InterruptedException exception){
      throw new RuntimeException("Cannot get verification code", exception);
    }
  }

  @SafeVarargs
  private Map<String, Object> getRegistrationOptions(PhoneNumber phoneNumber, Map.Entry<String, Object>... attributes) {
    var registrationId = BytesHelper.intToBytes(socketHandler.keys().id(), 4);
    var signedKeyId = socketHandler.keys().signedKeyPair().encodedId();
    var identityId = URLEncoder.encode(socketHandler.keys().identityId(), StandardCharsets.UTF_8)
        .replace("+", "%20");
    return Attributes.of(attributes)
        .put("cc", phoneNumber.countryCode().prefix())
        .put("in", DUMMY_PHONE_NUMBER)
        .put("lg", "en")
        .put("lc", "GB")
        .put("mistyped", "6")
        .put("authkey", getUrlEncoder().encodeToString(socketHandler.keys().identityKeyPair().publicKey()))
        .put("e_regid", getUrlEncoder().encodeToString(registrationId))
        .put("e_keytype", "BQ")
        .put("e_ident", getUrlEncoder().encodeToString(socketHandler.keys().signedKeyPair().publicKey()))
        .put("e_skey_id", getUrlEncoder().encodeToString(signedKeyId))
        .put("e_skey_val", getUrlEncoder().encodeToString(socketHandler.keys().signedKeyPair().publicKey()))
        .put("e_skey_sig", getUrlEncoder().encodeToString(socketHandler.keys().signedKeyPair().signature()))
        .put("fdid", socketHandler.keys().phoneId())
        .put("expid", socketHandler.keys().deviceId())
        .put("network_radio_type", "1")
        .put("simnum", "1")
        .put("hasinrc", "1")
        .put("pid", Math.floor(Math.random() * 9000 + 100))
        .put("rc", "0")
        .put("id", identityId)
        .toMap();
  }

  private boolean isResponseValid(PhoneNumberResponse phoneNumberResponse) {
    return phoneNumberResponse.ok() &&
        (phoneNumberResponse.status() == null
            || phoneNumberResponse.status().equals("ok")
            || phoneNumberResponse.status().equals("sent"));
  }

  private BodyPublisher getBodyPublishers(Map<String, Object> values){
    var result = values.entrySet()
        .stream()
        .map(entry -> "%s=%s".formatted(entry.getKey(), URLEncoder.encode(String.valueOf(entry), StandardCharsets.UTF_8)))
        .collect(Collectors.joining("&"));
    return BodyPublishers.ofString(result);
  }

  @Override
  protected void dispose() {
    super.dispose();
    handshake = null;
  }
}
