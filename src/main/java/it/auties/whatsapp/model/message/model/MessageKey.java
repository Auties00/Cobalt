package it.auties.whatsapp.model.message.model;

import it.auties.bytes.Bytes;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.MessageInfo;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Locale;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BOOLEAN;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A container for unique identifiers and metadata linked to a {@link Message} and contained in {@link MessageInfo}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@NoArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder(builderMethodName = "newMessageKey", buildMethodName = "create")
public class MessageKey implements ProtobufMessage {
  /**
   * The jid of the contact or group that sent the message.
   */
  @ProtobufProperty(index = 1, type = STRING,
          concreteType = ContactJid.class, requiresConversion = true)
  @NonNull
  private ContactJid chatJid;

  /**
   * Determines whether the message was sent by you or by someone else
   */
  @ProtobufProperty(index = 2, type = BOOLEAN)
  private boolean fromMe;

  /**
   * The jid of the message
   */
  @ProtobufProperty(index = 3, type = STRING)
  @NonNull
  @Default
  private String id = randomId();

  /**
   * Generates a random message id
   *
   * @return a non-null String
   */
  public static String randomId() {
    return Bytes.ofRandom(8)
            .toHex()
            .toUpperCase(Locale.ROOT);
  }
}
