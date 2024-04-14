package it.auties.whatsapp.api;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Specification;

/**
 * The constants of this enumerated type describe the various chat history's codeLength that Whatsapp
 * can send on the first login attempt
 */
public record WebHistoryLength(
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        int size
) implements ProtobufMessage {
    private static final WebHistoryLength ZERO = new WebHistoryLength(0);
    private static final WebHistoryLength STANDARD = new WebHistoryLength(Specification.Whatsapp.DEFAULT_HISTORY_SIZE);
    private static final WebHistoryLength EXTENDED = new WebHistoryLength(Integer.MAX_VALUE);

    /**
     * Discards history
     * This will save a lot of system resources, but you won't have access to messages sent before the session creation
     */
    public static WebHistoryLength zero() {
        return ZERO;
    }


    /**
     * This is the default setting for the web client
     * This is also the recommended setting
     */
    public static WebHistoryLength standard() {
        return STANDARD;
    }

    /**
     * This will contain most of your messages
     * Unless you 100% know what you are doing don't use this
     * It consumes a lot of system resources
     */
    public static WebHistoryLength extended() {
        return EXTENDED;
    }

    /**
     * Custom size
     */
    public static WebHistoryLength custom(int size) {
        return new WebHistoryLength(size);
    }

    /**
     * Returns whether this history size counts as zero
     *
     * @return a boolean
     */
    public boolean isZero() {
        return size == 0;
    }

    /**
     * Returns whether this history size counts as extended
     *
     * @return a boolean
     */
    public boolean isExtended() {
        return size > Specification.Whatsapp.DEFAULT_HISTORY_SIZE;
    }
}
