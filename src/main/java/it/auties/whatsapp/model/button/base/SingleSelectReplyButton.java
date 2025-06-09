package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents the selection of a row
 */
@ProtobufMessage(name = "Message.ListResponseMessage.SingleSelectReply")
public final class SingleSelectReplyButton {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String rowId;

    SingleSelectReplyButton(String rowId) {
        this.rowId = Objects.requireNonNull(rowId, "rowId cannot be null");
    }

    public String rowId() {
        return rowId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SingleSelectReplyButton that
                && Objects.equals(rowId, that.rowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowId);
    }

    @Override
    public String toString() {
        return "SingleSelectReplyButton[" +
                "rowId=" + rowId + ']';
    }
}