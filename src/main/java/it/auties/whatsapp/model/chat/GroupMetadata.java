package it.auties.whatsapp.model.chat;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.Clock;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;

import static it.auties.whatsapp.model.chat.GroupPolicy.forData;
import static it.auties.whatsapp.model.chat.GroupSetting.SEND_MESSAGES;

@AllArgsConstructor
@Value
@Accessors(fluent = true)
public class GroupMetadata implements ProtobufMessage {
    @NonNull ContactJid jid;

    @NonNull String subject;

    @NonNull ZonedDateTime subjectTimestamp;

    @NonNull ZonedDateTime foundationTimestamp;

    ContactJid founder;

    String description;

    String descriptionId;

    @NonNull Map<GroupSetting, GroupPolicy> policies;

    @NonNull List<GroupParticipant> participants;

    ZonedDateTime ephemeralExpiration;

    public static GroupMetadata of(@NonNull Node node) {
        var groupId = node.attributes()
                .getOptionalString("id")
                .map(id -> ContactJid.of(id, ContactJid.Server.GROUP))
                .orElseThrow(() -> new NoSuchElementException("Missing group jid"));
        var subject = node.attributes()
                .getString("subject");
        var subjectTimestamp = Clock.parse(node.attributes()
                        .getLong("s_t"))
                .orElse(ZonedDateTime.now());
        var foundationTimestamp = Clock.parse(node.attributes()
                        .getLong("creation"))
                .orElse(ZonedDateTime.now());
        var founder = node.attributes()
                .getJid("creator")
                .orElse(null);
        var policies = new HashMap<GroupSetting, GroupPolicy>();
        policies.put(SEND_MESSAGES, forData(node.hasNode("announce")));
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
                .flatMap(Clock::parse)
                .orElse(null);
        var participants = node.findNodes("participant")
                .stream()
                .map(GroupParticipant::of)
                .toList();
        return new GroupMetadata(groupId, subject, subjectTimestamp, foundationTimestamp, founder, description,
                descriptionId, policies, participants, ephemeral);
    }

    private static String parseDescription(Node wrapper) {
        return switch (wrapper.content()) {
            case null -> null;
            case String string -> string;
            case byte[] bytes -> new String(bytes, StandardCharsets.UTF_8);
            default -> throw new IllegalArgumentException("Illegal body type: %s".formatted(wrapper.content()
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
