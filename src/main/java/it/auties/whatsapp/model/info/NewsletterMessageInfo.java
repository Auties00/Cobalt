package it.auties.whatsapp.model.info;

import com.alibaba.fastjson2.JSONObject;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.newsletter.NewsletterReaction;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.*;

@ProtobufMessage
public final class NewsletterMessageInfo implements MessageInfo, MessageStatusInfo {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    final int serverId;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    final Long timestampSeconds;

    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    final Long views;

    @ProtobufProperty(index = 5, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final Map<String, NewsletterReaction> reactions;

    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    MessageContainer message;

    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    MessageStatus status;

    Newsletter newsletter;

    NewsletterMessageInfo(String id, int serverId, Long timestampSeconds, Long views, Map<String, NewsletterReaction> reactions, MessageContainer message, MessageStatus status) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.serverId = serverId;
        this.timestampSeconds = timestampSeconds;
        this.views = views;
        this.reactions = reactions;
        this.message = message;
        this.status = status;
    }

    public static Optional<NewsletterMessageInfo> ofJson(JSONObject jsonObject) {
        if(jsonObject == null) {
            return Optional.empty();
        }

        var id = jsonObject.getString("id");
        if(id == null) {
            return Optional.empty();
        }

        var serverId = jsonObject.getIntValue("serverId", -1);
        var timestampSeconds = jsonObject.getLongValue("timestampSeconds", 0);
        var views = jsonObject.getLongValue("views", 0);
        var reactionsJsonObject = jsonObject.getJSONObject("reactions");
        Map<String, NewsletterReaction> reactions = HashMap.newHashMap(reactionsJsonObject.size());
        for(var reactionKey : reactionsJsonObject.sequencedKeySet()) {
            var reactionJsonObject = reactionsJsonObject.getJSONObject(reactionKey);
            NewsletterReaction.ofJson(reactionJsonObject)
                    .ifPresent(reaction -> reactions.put(reactionKey, reaction));
        }
        var message = MessageContainer.ofJson(jsonObject.getJSONObject("message"))
                .orElse(MessageContainer.empty());
        var status = MessageStatus.of(jsonObject.getString("status"))
                .orElse(MessageStatus.ERROR);
        return Optional.of(new NewsletterMessageInfo(id, serverId, timestampSeconds, views, reactions, message, status));
    }

    public void setNewsletter(Newsletter newsletter) {
        this.newsletter = newsletter;
    }

    public Jid newsletterJid() {
        return newsletter.jid();
    }

    @Override
    public Jid parentJid() {
        return newsletterJid();
    }

    @Override
    public Jid senderJid() {
        return newsletterJid();
    }

    public Newsletter newsletter() {
        return newsletter;
    }

    public String id() {
        return id;
    }

    public int serverId() {
        return serverId;
    }

    @Override
    public OptionalLong timestampSeconds() {
        return timestampSeconds == null ? OptionalLong.empty() : OptionalLong.of(timestampSeconds);
    }

    public OptionalLong views() {
        return views == null ? OptionalLong.empty() : OptionalLong.of(views);
    }

    public MessageContainer message() {
        return message;
    }

    @Override
    public void setMessage(MessageContainer message) {
        this.message = message;
    }

    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    @Override
    public MessageStatus status() {
        return status;
    }

    @Override
    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public Collection<NewsletterReaction> reactions() {
        return Collections.unmodifiableCollection(reactions.values());
    }

    public Optional<NewsletterReaction> findReaction(String value) {
        return Optional.ofNullable(reactions.get(value));
    }

    public Optional<NewsletterReaction> addReaction(NewsletterReaction reaction) {
        return Optional.ofNullable(reactions.put(reaction.content(), reaction));
    }

    public Optional<NewsletterReaction> removeReaction(String code) {
        return Optional.ofNullable(reactions.remove(code));
    }

    public void incrementReaction(String code, boolean fromMe) {
        findReaction(code).ifPresentOrElse(reaction -> {
            reaction.setCount(reaction.count() + 1);
            reaction.setFromMe(fromMe);
        }, () -> {
            var reaction = new NewsletterReaction(code, 1, fromMe);
            addReaction(reaction);
        });
    }

    public void decrementReaction(String code) {
        findReaction(code).ifPresent(reaction -> {
            if (reaction.count() <= 1) {
                removeReaction(reaction.content());
                return;
            }

            reaction.setCount(reaction.count() - 1);
            reaction.setFromMe(false);
        });
    }

    @Override
    public String toString() {
        return "NewsletterMessageInfo{" +
                "newsletter=" + newsletter +
                ", id='" + id + '\'' +
                ", serverId=" + serverId +
                ", timestampSeconds=" + timestampSeconds +
                ", views=" + views +
                ", reactions=" + reactions +
                ", message=" + message +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterMessageInfo that &&
                serverId == that.serverId &&
                Objects.equals(id, that.id) &&
                Objects.equals(timestampSeconds, that.timestampSeconds) &&
                Objects.equals(views, that.views) &&
                Objects.equals(reactions, that.reactions) &&
                Objects.equals(message, that.message) &&
                Objects.equals(newsletter, that.newsletter) &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serverId, timestampSeconds, views, reactions, message, newsletter, status);
    }
}
