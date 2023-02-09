package it.auties.whatsapp;

import static java.util.Base64.getUrlEncoder;
import static java.util.Map.entry;

import it.auties.whatsapp.api.WhatsappOptions.MobileOptions;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import it.auties.whatsapp.model.request.Attributes;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.Specification.Whatsapp;
import it.auties.whatsapp.util.Validate;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HexFormat;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.SneakyThrows;

// https://github.com/Manjit2003/whatsapp-kaios
public class MobileTest implements JacksonProvider {
  private static final MobileOptions options = MobileOptions.builder()
      .phoneNumber("+phone")
      .verificationCodeHandler(type -> {
        var scanner = new Scanner(System.in);
        System.out.println("Enter OTP: " + type);
        var result = scanner.nextLine().trim();
        Validate.isTrue(result.length() == type.codeLength(),
            "Invalid code length: %s != %s",
            result.length(), type.codeLength());
        return result;
      })
      .build();
  private static final Keys keys = Keys.random(options);

  public static void main(String[] args) {
    new MobileTest().registerPhoneNumber();
  }

  @SneakyThrows
  public void registerPhoneNumber(){
    var phoneNumber = PhoneNumber.of(options.phoneNumber());
    var response = askForVerificationCode(phoneNumber);
    var code = options.verificationCodeHandler().apply(response);
    var result = sendVerificationCode(phoneNumber, code);
  }

  private VerificationCodeResponse sendVerificationCode(PhoneNumber phoneNumber, String code) {
    try {
      var registerOptions = getRegistrationOptions(
          phoneNumber,
          entry("code", code.replaceAll("-", ""))
      );
      var codeResponse = sendRegistrationRequest("/register", registerOptions);
      var phoneNumberResponse = JSON.readValue(codeResponse.body(), VerificationCodeResponse.class);
      Validate.isTrue(phoneNumberResponse.status().isSuccessful(),
          "Unexpected response: %s", phoneNumberResponse);
      return phoneNumberResponse;
    }catch (IOException exception){
      throw new RuntimeException("Cannot send verification code", exception);
    }
  }

  private VerificationCodeResponse askForVerificationCode(PhoneNumber phoneNumber) {
    try {
      var codeOptions = getRegistrationOptions(
          phoneNumber,
          entry("mcc", phoneNumber.countryCode().mcc()),
          entry("mnc", phoneNumber.countryCode().mnc()),
          entry("sim_mcc",  "000"),
          entry("sim_mnc",  "000"),
          entry("method", options.verificationCodeMethod().type()),
          entry("errorReason", ""),
          entry("hasav", "1")
      );
      var codeResponse = sendRegistrationRequest("/code", codeOptions);
      var phoneNumberResponse = JSON.readValue(codeResponse.body(), VerificationCodeResponse.class);
      Validate.isTrue(phoneNumberResponse.status().isSuccessful(),
          "Unexpected response: %s", phoneNumberResponse);
      return phoneNumberResponse;
    }catch (IOException exception){
      throw new RuntimeException("Cannot get verification code", exception);
    }
  }

  private HttpResponse<String> sendRegistrationRequest(String path, Map<String, Object> params) {
    try {
      var client = HttpClient.newHttpClient();
      var paramsString = toParams(params);
      var request = HttpRequest.newBuilder()
          .uri(URI.create("%s%s?%s".formatted(Whatsapp.MOBILE_REGISTRATION_ENDPOINT, path, paramsString)))
          .GET()
          .header("Content-Type", "application/x-www-form-urlencoded")
          .header("User-Agent", Whatsapp.MOBILE_USER_AGENT)
          .build();
      System.out.println("Sending request to: " +  request.uri());
      var result = client.send(request, BodyHandlers.ofString());
      System.out.println("Received: " + result.body());
      return result;
    } catch (IOException | InterruptedException exception){
      throw new RuntimeException("Cannot get verification code", exception);
    }
  }

  @SafeVarargs
  private Map<String, Object> getRegistrationOptions(PhoneNumber phoneNumber, Map.Entry<String, Object>... attributes) {
    return Attributes.of(attributes)
        .put("cc", phoneNumber.countryCode().prefix())
        .put("in", phoneNumber.number())
        .put("lg", "en")
        .put("lc", "GB")
        .put("mistyped", "6")
        .put("authkey", getUrlEncoder().encodeToString(keys.noiseKeyPair().publicKey()))
        .put("e_regid", getUrlEncoder().encodeToString(keys.encodedId()))
        .put("e_keytype", "BQ")
        .put("e_ident", getUrlEncoder().encodeToString(keys.identityKeyPair().publicKey()))
        .put("e_skey_id", getUrlEncoder().encodeToString(keys.signedKeyPair().encodedId()))
        .put("e_skey_val", getUrlEncoder().encodeToString(keys.signedKeyPair().publicKey()))
        .put("e_skey_sig", getUrlEncoder().encodeToString(keys.signedKeyPair().signature()))
        .put("fdid", keys.phoneId())
        .put("expid", keys.deviceId())
        .put("network_radio_type", "1")
        .put("simnum", "1")
        .put("hasinrc", "1")
        .put("pid", ThreadLocalRandom.current().nextInt(1000))
        .put("rc", "0")
        .put("id", keys.identityId())
        .put("token", HexFormat.of().formatHex(MD5.calculate(Whatsapp.MOBILE_TOKEN + phoneNumber.number())))
        .toMap();
  }

  private String toParams(Map<String, Object> values){
    return values.entrySet()
        .stream()
        .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
        .collect(Collectors.joining("&"));
  }
}
