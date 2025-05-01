package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model that represents a chat disappear mode
 */
@ProtobufMessage(name = "DisappearingMode")
public record ChatDisappear(
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        Initiator initiator
) {
    public ChatDisappear(Initiator initiator) {
        this.initiator = Objects.requireNonNullElse(initiator, Initiator.UNKNOWN);
    }

    /**
     * The constants of this enumerated type describe the various actors that can initialize
     * disappearing messages in a chat
     */
    @ProtobufEnum(name = "DisappearingMode.Initiator")
    public enum Initiator {
        /**
         * Unknown
         */
        UNKNOWN(999),

        /**
         * Changed in chat
         */
        CHANGED_IN_CHAT(0),
        /**
         * Initiated by me
         */
        INITIATED_BY_ME(1),
        /**
         * Initiated by other
         */
        INITIATED_BY_OTHER(2);

        final int index;

        Initiator(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(initiator.index());
    }
}