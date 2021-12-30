package it.auties.whatsapp.protobuf.message.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.chat.Chat;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.util.WhatsappUtils;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A container for unique identifiers and metadata linked to a {@link Message} and contained in {@link MessageInfo}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "newMessageKey", buildMethodName = "create")
@Accessors(fluent = true)
@ToString(exclude = {"storeUuid", "cachedStore"})
public class MessageKey {
  /**
   * The jid of the contact or group that sent the message.
   */
  @JsonProperty("1")
  @Getter
  private @NonNull ContactJid chatJid;

  /**
   * Determines whether the message was sent by you or by someone else
   */
  @JsonProperty("2")
  @Getter
  private boolean fromMe;

  /**
   * The id of the message
   */
  @JsonProperty("3")
  @Builder.Default
  @Getter
  private String id = WhatsappUtils.randomId();

  /**
   * The id of the session that owns this contact
   */
  private @NonNull UUID storeUuid;

  /**
   * The cached value of the store
   */
  private WhatsappStore cachedStore;

  /**
   * Returns the chat where the message was sent
   *
   * @return an optional wrapping a {@link Chat}
   */
  public Optional<Chat> chat(){
    return WhatsappStore.findStoreById(storeUuid)
            .findChatByJid(Objects.toString(chatJid()));
  }

  public WhatsappStore store(){
    return Objects.requireNonNullElseGet(cachedStore,
            () -> this.cachedStore = WhatsappStore.findStoreById(storeUuid));
  }
}
