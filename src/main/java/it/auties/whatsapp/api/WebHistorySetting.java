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
public final class WebHistorySetting {
    private static final WebHistorySetting ZERO = new WebHistorySetting(0, false);
    private static final WebHistorySetting ZERO_WITH_NEWSLETTERS = new WebHistorySetting(0, true);
    private static final WebHistorySetting STANDARD = new WebHistorySetting(59206, false);
    private static final WebHistorySetting STANDARD_WITH_NEWSLETTERS = new WebHistorySetting(59206, true);
    private static final WebHistorySetting EXTENDED = new WebHistorySetting(Integer.MAX_VALUE, false);
    private static final WebHistorySetting EXTENDED_WITH_NEWSLETTERS = new WebHistorySetting(Integer.MAX_VALUE, true);

    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final int size;

    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean newsletters;

    WebHistorySetting(int size, boolean newsletters) {
        this.size = size;
        this.newsletters = newsletters;
    }

    /**
     * Discards history
     * This will save a lot of system resources, but you won't have access to messages sent before the session creation
     *
     */
    public static WebHistorySetting discard(boolean newsletters) {
        return newsletters ? ZERO_WITH_NEWSLETTERS : ZERO;
    }

    /**
     * This is the default setting for the web client
     * This is also the recommended setting
     */
    public static WebHistorySetting standard(boolean newsletters) {
        return newsletters ? STANDARD_WITH_NEWSLETTERS : STANDARD;
    }

    /**
     * This will contain most of your messages
     * Unless you 100% know what you are doing don't use this
     * It consumes a lot of system resources
     */
    public static WebHistorySetting extended(boolean newsletters) {
        return newsletters ? EXTENDED_WITH_NEWSLETTERS : EXTENDED;
    }

    /**
     * Custom size
     */
    public static WebHistorySetting custom(int size, boolean newsletters) {
        return new WebHistorySetting(size, newsletters);
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
        var that = (WebHistorySetting) obj;
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
