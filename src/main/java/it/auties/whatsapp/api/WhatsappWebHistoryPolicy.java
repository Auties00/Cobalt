package it.auties.whatsapp.api;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Represents a policy configuration for WhatsApp Web history synchronization during the initial connection.
 * <p>
 * This class defines how much chat history WhatsApp should send when a Web client first connects or scans a QR code.
 * The policy controls both the amount of message history to sync and whether newsletters should be included.
 * </p>
 * <p>
 * The history sync process affects:
 * <ul>
 *   <li>System resource usage (memory, bandwidth, storage)</li>
 *   <li>Initial connection time</li>
 *   <li>Available offline message access</li>
 *   <li>Newsletter content synchronization</li>
 * </ul>
 * </p>
 * <p>
 * This class is immutable and thread-safe. It uses a factory pattern with pre-configured instances
 * for common use cases, as well as support for custom configurations.
 * </p>
 *
 * @see Whatsapp
 * @see WhatsappBuilder
 * @since 1.0
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
     * Creates a policy that discards all chat history, keeping only new messages from session creation onwards.
     * <p>
     * This is the most resource-efficient option but provides no access to historical messages.
     * Recommended for applications that only need real-time messaging capabilities.
     * </p>
     *
     * @param newsletters whether newsletters should be synchronized during the initial connection
     * @return a policy that discards all previous chat history
     */
    public static WhatsappWebHistoryPolicy discard(boolean newsletters) {
        return newsletters ? ZERO_WITH_NEWSLETTERS : ZERO;
    }

    /**
     * Creates a policy using WhatsApp Web's default history synchronization settings.
     * <p>
     * This policy provides a balanced approach between resource usage and message availability,
     * syncing approximately the last few weeks of chat history. This is the recommended setting
     * for most applications as it matches the official WhatsApp Web behavior.
     * </p>
     *
     * @param newsletters whether newsletters should be synchronized during the initial connection
     * @return a policy using standard WhatsApp Web history limits
     */
    public static WhatsappWebHistoryPolicy standard(boolean newsletters) {
        return newsletters ? STANDARD_WITH_NEWSLETTERS : STANDARD;
    }

    /**
     * Creates a policy that attempts to synchronize most available chat history.
     * <p>
     * This policy requests the maximum amount of chat history that WhatsApp allows,
     * which may include several months or years of messages depending on account age.
     * <strong>Warning:</strong> This can consume significant system resources and bandwidth.
     * </p>
     *
     * @param newsletters whether newsletters should be synchronized during the initial connection
     * @return a policy that requests extended chat history
     */
    public static WhatsappWebHistoryPolicy extended(boolean newsletters) {
        return newsletters ? EXTENDED_WITH_NEWSLETTERS : EXTENDED;
    }

    /**
     * Creates a policy with a custom history size limit.
     * <p>
     * Allows fine-grained control over the amount of history to synchronize.
     * The actual amount of history received may be less than requested if the account
     * doesn't have enough historical data or if WhatsApp imposes server-side limits.
     * </p>
     *
     * @param size        the maximum value of historical items to synchronize (must be non-negative)
     * @param newsletters whether newsletters should be synchronized during the initial connection
     * @return a policy with the specified custom size limit
     * @throws IllegalArgumentException if size is negative
     */
    public static WhatsappWebHistoryPolicy custom(int size, boolean newsletters) {
        return new WhatsappWebHistoryPolicy(size, newsletters);
    }

    /**
     * Checks if this policy discards all chat history.
     * <p>
     * A zero-size policy means no historical messages will be synchronized,
     * and only new messages from session creation onwards will be available.
     * </p>
     *
     * @return {@code true} if this policy discards all history, {@code false} otherwise
     */
    public boolean isZero() {
        return size == 0;
    }

    /**
     * Checks if this policy requests extended chat history beyond the standard amount.
     * <p>
     * Extended policies typically result in longer sync times and higher resource usage
     * but provide access to more historical messages.
     * </p>
     *
     * @return {@code true} if this policy requests more than the standard amount of history
     */
    public boolean isExtended() {
        return size > STANDARD.size();
    }

    /**
     * Returns the maximum value of historical items this policy will attempt to synchronize.
     * <p>
     * This represents the upper limit of history items to request from WhatsApp's servers.
     * The actual amount synchronized may be less due to server limitations or account history.
     * </p>
     *
     * @return the history size limit, or {@link Integer#MAX_VALUE} for unlimited requests
     */
    public int size() {
        return size;
    }

    /**
     * Checks if this policy includes newsletter synchronization.
     * <p>
     * When enabled, newsletters and their associated metadata will be synchronized
     * along with regular chat history during the initial connection.
     * </p>
     *
     * @return {@code true} if newsletters should be synchronized, {@code false} otherwise
     */
    public boolean hasNewsletters() {
        return newsletters;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof WhatsappWebHistoryPolicy that
                && size == that.size
                && newsletters == that.newsletters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, newsletters);
    }

    @Override
    public String toString() {
        return "WhatsappWebHistorySetting[" +
                "size=" + size + ", " +
                "newsletters=" + newsletters + ']';
    }
}