package it.auties.whatsapp.protobuf.chat;

import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.socket.Node;
import it.auties.whatsapp.util.WhatsappUtils;
import lombok.NonNull;

import java.time.ZonedDateTime;
import java.util.*;

import static it.auties.whatsapp.protobuf.chat.GroupPolicy.forData;
import static it.auties.whatsapp.protobuf.chat.GroupSetting.EDIT_GROUP_INFO;
import static it.auties.whatsapp.protobuf.chat.GroupSetting.SEND_MESSAGES;

public record GroupMetadata(ContactJid jid, String subject, ZonedDateTime foundationTimestamp, ContactJid founder,
                            String description, String descriptionId, Map<GroupSetting, GroupPolicy> policies,
                            List<GroupParticipant> participants, ZonedDateTime ephemeralExpiration) {
    public static GroupMetadata of(@NonNull Node node){
        var groupId = node.attributes()
                .getOptionalString("id")
                .map(id -> ContactJid.ofUser(id, ContactJid.Server.GROUP))
                .orElseThrow(() -> new NoSuchElementException("Missing group jid"));
        var subject = node.attributes().getString("subject");
        var foundationTimestamp = WhatsappUtils.parseWhatsappTime(node.attributes().getLong("creation"))
                .orElse(ZonedDateTime.now());
        var founder = node.attributes()
                .getJid("creator")
                .orElse(null);
        var policies = new HashMap<GroupSetting, GroupPolicy>();
        policies.put(SEND_MESSAGES, forData(node.hasNode("announce")));
        policies.put(EDIT_GROUP_INFO, forData(node.hasNode("locked")));
        var descWrapper = node.findNode("description");
        var description = Optional.ofNullable(descWrapper)
                .map(parent -> parent.findNode("body"))
                .map(wrapper -> (String) wrapper.content())
                .orElse(null);
        var descriptionId = Optional.ofNullable(descWrapper)
                .map(Node::attributes)
                .flatMap(attributes -> attributes.getOptionalString("id"))
                .orElse(null);
        var ephemeral = Optional.ofNullable(node.findNode("ephemeral"))
                .map(Node::attributes)
                .map(attributes -> attributes.getLong("expiration"))
                .flatMap(WhatsappUtils::parseWhatsappTime)
                .orElse(null);
        var participants = node.findNodes("participant")
                .stream()
                .map(GroupParticipant::of)
                .toList();
        return new GroupMetadata(groupId, subject, foundationTimestamp, founder, description, descriptionId, policies, participants, ephemeral);
    }
}
