package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A model clas that represents the addition or deletion of a quick reply
 */
@ProtobufMessage(name = "SyncActionValue.QuickReplyAction")
public final class QuickReplyAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String shortcut;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String message;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> keywords;

    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final int count;

    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final boolean deleted;

    QuickReplyAction(String shortcut, String message, List<String> keywords, int count, boolean deleted) {
        this.shortcut = shortcut;
        this.message = message;
        this.keywords = keywords;
        this.count = count;
        this.deleted = deleted;
    }

    @Override
    public String indexName() {
        return "quick_reply";
    }

    @Override
    public int actionVersion() {
        return 2;
    }

    public Optional<String> shortcut() {
        return Optional.ofNullable(shortcut);
    }

    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    public List<String> keywords() {
        return keywords;
    }

    public int count() {
        return count;
    }

    public boolean deleted() {
        return deleted;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof QuickReplyAction that
                && count == that.count
                && deleted == that.deleted
                && Objects.equals(shortcut, that.shortcut)
                && Objects.equals(message, that.message)
                && Objects.equals(keywords, that.keywords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortcut, message, keywords, count, deleted);
    }

    @Override
    public String toString() {
        return "QuickReplyAction[" +
                "shortcut=" + shortcut + ", " +
                "message=" + message + ", " +
                "keywords=" + keywords + ", " +
                "count=" + count + ", " +
                "deleted=" + deleted + ']';
    }
}
