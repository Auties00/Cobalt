package it.auties.whatsapp.api;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * The constants of this enumerated type describe the various chat history's codeLength that Whatsapp
 * can send on the first login attempt
 */
@ProtobufMessage
public final class WhatsappWebHistoryPolicy {
    private static final WhatsappWebHistoryPolicy ZERO = new WhatsappWebHistoryPolicy(0, false);
    private static final WhatsappWebHistoryPolicy ZERO_WITH_NEWSLETTERS = new WhatsappWebHistoryPolicy(0, true);
    private static final WhatsappWebHistoryPolicy STANDARD = new WhatsappWebHistoryPolicy(59206, false);
    private static final WhatsappWebHistoryPolicy STANDARD_WITH_NEWSLETTERS = new WhatsappWebHistoryPolicy(59206, true);
    private static final WhatsappWebHistoryPolicy EXTENDED = new WhatsappWebHistoryPolicy(Integer.MAX_VALUE, false);
    private static final WhatsappWebHistoryPolicy EXTENDED_WITH_NEWSLETTERS = new WhatsappWebHistoryPolicy(Integer.MAX_VALUE, true);

    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final int size;

    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean newsletters;

    WhatsappWebHistoryPolicy(int size, boolean newsletters) {
        this.size = size;
        this.newsletters = newsletters;
    }

    /**
     * Discards history
     * This will save a lot of system resources, but you won't have access to messages sent before the session creation
     *
     * @param newsletters whether newsletters should be synced
     */
    public static WhatsappWebHistoryPolicy discard(boolean newsletters) {
        return newsletters ? ZERO_WITH_NEWSLETTERS : ZERO;
    }

    /**
     * This is the default setting for the web client
     * This is also the recommended setting
     *
     * @param newsletters whether newsletters should be synced
     */
    public static WhatsappWebHistoryPolicy standard(boolean newsletters) {
        return newsletters ? STANDARD_WITH_NEWSLETTERS : STANDARD;
    }

    /**
     * This will contain most of your messages
     * Uses more resources
     *
     * @param newsletters whether newsletters should be synced
     */
    public static WhatsappWebHistoryPolicy extended(boolean newsletters) {
        return newsletters ? EXTENDED_WITH_NEWSLETTERS : EXTENDED;
    }

    /**
     * Custom size
     */
    public static WhatsappWebHistoryPolicy custom(int size, boolean newsletters) {
        return new WhatsappWebHistoryPolicy(size, newsletters);
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
        return size > STANDARD.size();
    }

    public int size() {
        return size;
    }

    public boolean hasNewsletters() {
        return newsletters;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (WhatsappWebHistoryPolicy) obj;
        return this.size == that.size &&
                this.newsletters == that.newsletters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, newsletters);
    }

    @Override
    public String toString() {
        return "WebHistorySetting[" +
                "size=" + size + ", " +
                "newsletters=" + newsletters + ']';
    }
}
