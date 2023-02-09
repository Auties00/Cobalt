package it.auties.whatsapp.model.mobile;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.api.ClientType;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The constants of this enumerated type describe the various types of verification that can be used to receive the OTP required for an {@link ClientType#APP_CLIENT}
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum VerificationCodeMethod {
  /**
   * An SMS containing the countryCode will be sent to the associated phone number
   */
  SMS("sms"),
  /**
   * A call will be received from the associated phone number
   */
  CALL("voice");

  @Getter
  private final String type;

  @JsonCreator
  public static VerificationCodeMethod of(String name) {
    return Arrays.stream(values())
        .filter(entry -> entry.type().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }
}
