package it.auties.whatsapp.model.privacy;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The constants of this enumerated type describe the various types of preferences that can be
 * toggled for a corresponding setting
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum PrivacySettingValue {
  /**
   * Everyone
   */
  EVERYONE("all"),
  /**
   * All the contacts saved on your Whatsapp's user
   */
  CONTACTS("contacts"),
  /**
   * All the contacts saved on your Whatsapp's user except some
   */
  CONTACT_EXCEPT("contact_blacklist"),
  /**
   * Nobody
   */
  NOBODY("none");

  @Getter
  private final String data;

  public static Optional<PrivacySettingValue> of(String id) {
    return Arrays.stream(values())
        .filter(entry -> Objects.equals(entry.data(), id))
        .findFirst();
  }
}
