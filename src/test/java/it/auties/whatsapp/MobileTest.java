package it.auties.whatsapp;

import it.auties.whatsapp.api.WhatsappOptions.MobileOptions;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.RegistrationHelper;
import it.auties.whatsapp.util.Validate;
import java.util.Scanner;

public class MobileTest implements JacksonProvider {

  public static void main(String[] args) {
    var options = MobileOptions.builder()
        .phoneNumber("+")
        .verificationCodeHandler(MobileTest::onScanCode)
        .build();
    var keys = Keys.random(options);
    RegistrationHelper.register(keys, options);
  }

  private static String onScanCode(VerificationCodeResponse type) {
    var scanner = new Scanner(System.in);
    System.out.println("Enter OTP: " + type);
    var result = scanner.nextLine().trim();
    Validate.isTrue(result.length() == type.codeLength(),
        "Invalid code length: %s != %s",
        result.length(), type.codeLength());
    return result;
  }
}
