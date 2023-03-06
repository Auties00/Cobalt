package it.auties.whatsapp.model.chat;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.Clock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;

import static it.auties.whatsapp.model.chat.GroupSetting.*;

/**
 * This model class represents the metadata of a group
 */
@AllArgsConstructor
@Value
@Accessors(fluent = true)
@Builder
public class GroupMetadata implements ProtobufMessage {
    /**
     * The jid of the group
     */
    @NonNull ContactJid jid;

    /**
     * The subject or name of the group
     */
    @NonNull String subject;

    /**
     * The person who set the subject of this group
     */
    ContactJid subjectAuthor;

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

    /**
     * Whether this group is the parent group of a community
     */
    boolean community;

    /**
     * Whether new members can join this community without an invitation
     */
    boolean openCommunity;

    public static GroupMetadata of(@NonNull Node node) {
        var groupId = node.attributes()
                .getOptionalString("id")
                .map(id -> ContactJid.of(id, ContactJid.Server.GROUP))
                .orElseThrow(() -> new NoSuchElementException("Missing group jid"));
        var subject = node.attributes().getString("subject");
        var subjectAuthor = node.attributes().getJid("s_o").orElse(null);
        var subjectTimestamp = Clock.parseSeconds(node.attributes().getLong("s_t")).orElse(ZonedDateTime.now());
        var foundationTimestamp = Clock.parseSeconds(node.attributes().getLong("creation")).orElse(ZonedDateTime.now());
        var founder = node.attributes().getJid("creator").orElse(null);
        var policies = new HashMap<GroupSetting, GroupPolicy>();
        policies.put(SEND_MESSAGES, GroupPolicy.of(node.hasNode("restrict")));
        policies.put(EDIT_GROUP_INFO, GroupPolicy.of(node.hasNode("announce")));

        var description = node.findNode("description")
                .flatMap(parent -> parent.findNode("body"))
                .map(GroupMetadata::parseDescription)
                .orElse(null);
        var descriptionId = node.findNode("description")
                .map(Node::attributes)
                .flatMap(attributes -> attributes.getOptionalString("id"))
                .orElse(null);
        var community = node.findNode("parent")
                .isPresent();
        if(community){
            var adminAddCommunity = node.findNode("member_add_mode")
                    .flatMap(Node::contentAsString)
                    .filter("admin_add"::equals)
                    .isPresent();
            policies.put(ADD_COMMUNITY_MEMBER, GroupPolicy.of(adminAddCommunity));
        }
        var openCommunity = node.findNode("parent")
                .filter(entry -> entry.attributes().hasKey("default_membership_approval_mode", "request_required"))
                .isEmpty();
        var ephemeral = node.findNode("ephemeral")
                .map(Node::attributes)
                .map(attributes -> attributes.getLong("expiration"))
                .flatMap(Clock::parseSeconds)
                .orElse(null);
        var participants = node.findNodes("participant")
                .stream()
                .map(GroupParticipant::of)
                .toList();
        return new GroupMetadata(groupId, subject, subjectAuthor, subjectTimestamp, foundationTimestamp, founder, description, descriptionId, Collections.unmodifiableMap(policies), participants, ephemeral, community, openCommunity);
    }

    private static String parseDescription(Node wrapper) {
        return switch (wrapper.content()) {
            case null -> null;
            case String string -> string;
            case byte[] bytes -> new String(bytes, StandardCharsets.UTF_8);
            default -> throw new IllegalArgumentException("Illegal body type: %s".formatted(wrapper.content().getClass().getName()));
        };
    }

    /**
     * Returns the description of this group
     *
     * @return a non-null optional
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the ephemeral expiration for messages sent in this group
     *
     * @return a non-null optional
     */
    public Optional<ZonedDateTime> ephemeralExpiration() {
        return Optional.ofNullable(ephemeralExpiration);
    }

    /**
     * Returns the founder of this group
     *
     * @return a non-null optional
     */
    public Optional<ContactJid> founder() {
        return Optional.ofNullable(founder);
    }

    /**
     * Returns the participants of this group as jids
     *
     * @return a non-null optional
     */
    public List<ContactJid> participantsJids() {
        return participants.stream().map(GroupParticipant::jid).toList();
    }
}
