package it.auties.whatsapp.model.chat;

import static it.auties.whatsapp.model.chat.GroupSetting.SEND_MESSAGES;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.Clock;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * This model class represents the metadata of a group
 */
@AllArgsConstructor
@Value
@Accessors(fluent = true)
public class GroupMetadata
    implements ProtobufMessage {

  /**
   * The jid of the group
   */
  @NonNull ContactJid jid;

  /**
   * The subject or name of the group
   */
  @NonNull String subject;

  /**
   * The timestamp when the subject was last changed
   */
  @NonNull ZonedDateTime subjectTimestamp;

  /**
   * The timestamp for when this group was created
   */
  @NonNull ZonedDateTime foundationTimestamp;

  /**
   * The founder of the group. For very old groups this property is not known.
   */
  ContactJid founder;

  /**
   * The description of the group. Some groups don't have a description.
   */
  String description;

  /**
   * The id of the description. Only used by Whatsapp.
   */
  String descriptionId;

  /**
   * The policies that regulate this group
   */
  @NonNull Map<GroupSetting, GroupPolicy> policies;

  /**
   * The participants of this group
   */
  @NonNull List<GroupParticipant> participants;

  /**
   * The expiration timer for this group if ephemeral messages are enabled
   */
  ZonedDateTime ephemeralExpiration;

  public static GroupMetadata of(@NonNull Node node) {
    var groupId = node.attributes()
        .getOptionalString("id")
        .map(id -> ContactJid.of(id, ContactJid.Server.GROUP))
        .orElseThrow(() -> new NoSuchElementException("Missing group jid"));
    var subject = node.attributes()
        .getString("subject");
    var subjectTimestamp = Clock.parseSeconds(node.attributes()
            .getLong("s_t"))
        .orElse(ZonedDateTime.now());
    var foundationTimestamp = Clock.parseSeconds(node.attributes()
            .getLong("creation"))
        .orElse(ZonedDateTime.now());
    var founder = node.attributes()
        .getJid("creator")
        .orElse(null);
    var policies = new HashMap<GroupSetting, GroupPolicy>();
    policies.put(SEND_MESSAGES, GroupPolicy.of(node.hasNode("announce")));
    var description = node.findNode("description")
        .flatMap(parent -> parent.findNode("body"))
        .map(GroupMetadata::parseDescription)
        .orElse(null);
    var descriptionId = node.findNode("description")
        .map(Node::attributes)
        .flatMap(attributes -> attributes.getOptionalString("id"))
        .orElse(null);
    var ephemeral = node.findNode("ephemeral")
        .map(Node::attributes)
        .map(attributes -> attributes.getLong("expiration"))
        .flatMap(Clock::parseSeconds)
        .orElse(null);
    var participants = node.findNodes("participant")
        .stream()
        .map(GroupParticipant::of)
        .toList();
    return new GroupMetadata(groupId, subject, subjectTimestamp, foundationTimestamp, founder,
        description,
        descriptionId, policies, participants, ephemeral);
  }

  private static String parseDescription(Node wrapper) {
    return switch (wrapper.content()) {
      case null -> null;
      case String string -> string;
      case byte[] bytes -> new String(bytes, StandardCharsets.UTF_8);
      default ->
          throw new IllegalArgumentException("Illegal body type: %s".formatted(wrapper.content()
              .getClass()
              .getName()));
    };
  }

  public Optional<String> description() {
    return Optional.ofNullable(description);
  }

  public Optional<ZonedDateTime> ephemeralExpiration() {
    return Optional.ofNullable(ephemeralExpiration);
  }


  public Optional<ContactJid> founder() {
    return Optional.ofNullable(founder);
  }

  public List<ContactJid> participantsJids() {
    return participants.stream()
        .map(GroupParticipant::jid)
        .toList();
  }
}
