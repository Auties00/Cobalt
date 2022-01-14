package it.auties.whatsapp.protobuf.message.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
@Accessors(fluent = true)
@ToString(exclude = {"storeUuid", "cachedStore"})
public class MessageKey {
  /**
   * The jid of the contact or group that sent the message.
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  @NonNull
  @Getter
  private ContactJid chatJid;

  /**
   * Determines whether the message was sent by you or by someone else
   */
  @JsonProperty("2")
  @JsonPropertyDescription("bool")
  @Getter
  private boolean fromMe;

  /**
   * The id of the message
   */
  @JsonProperty("3")
  @JsonPropertyDescription("string")
  @NonNull
  @Getter
  private String id;

  /**
   * The id of the session that owns this contact
   */
  @Setter
  @NonNull
  private UUID storeUuid;

  /**
   * The cached value of the store
   */
  private WhatsappStore cachedStore;


  /**
   * Constructs a new message key from the input values
   *
   * @param storeUuid the non-null uuid of the store that holds this message
   * @param chatJid the non-null chat jid
   * @param id the id of this message
   * @param fromMe whether this message was sent by this client
   */
  @Builder(builderMethodName = "newMessageKey", buildMethodName = "create")
  public MessageKey(@NonNull UUID storeUuid, @NonNull ContactJid chatJid, String id, boolean fromMe) {
    this.chatJid = chatJid;
    this.fromMe = fromMe;
    this.id = Objects.requireNonNullElseGet(id, WhatsappUtils::randomId);
    this.storeUuid = storeUuid;
  }

  /**
   * Returns the chat where the message was sent
   *
   * @return an optional wrapping a {@link Chat}
   */
  public Optional<Chat> chat(){
    return WhatsappStore.findStoreById(storeUuid)
            .findChatByJid(Objects.toString(chatJid()));
  }

  /**
   * Returns the store where the message is stored
   *
   * @return a non-null store
   */
  public WhatsappStore store(){
    return Objects.requireNonNullElseGet(cachedStore,
            () -> this.cachedStore = WhatsappStore.findStoreById(storeUuid));
  }
}
