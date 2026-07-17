package com.github.auties00.cobalt.wire.linked.message.addon;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Carries expiry information for a {@link MessageAddOn}.
 *
 * <p>Some add-ons such as reactions or poll updates have a limited lifetime
 * after which clients are expected to hide or remove them. This object
 * captures:
 * <ul>
 *   <li>The duration in seconds for which the add-on remains valid</li>
 *   <li>The {@link ExpiryType} that describes whether the expiry is fixed or
 *       tied to the lifetime of the parent message</li>
 * </ul>
 */
@ProtobufMessage(name = "MessageAddOnContextInfo")
public final class MessageAddOnContextInfo {
    /**
     * The validity duration of the add-on expressed in seconds.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    Integer messageAddOnDurationInSecs;

    /**
     * The strategy used to compute when the add-on expires.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    ExpiryType messageAddOnExpiryType;


    /**
     * Constructs a new context info instance.
     *
     * @param messageAddOnDurationInSecs the validity duration in seconds, or {@code null} if unspecified
     * @param messageAddOnExpiryType the expiry strategy, or {@code null} if unspecified
     */
    MessageAddOnContextInfo(Integer messageAddOnDurationInSecs, ExpiryType messageAddOnExpiryType) {
        this.messageAddOnDurationInSecs = messageAddOnDurationInSecs;
        this.messageAddOnExpiryType = messageAddOnExpiryType;
    }

    /**
     * Returns the validity duration of the add-on in seconds.
     *
     * @return an {@link OptionalInt} containing the duration, or empty if not set
     */
    public OptionalInt messageAddOnDurationInSecs() {
        return messageAddOnDurationInSecs == null ? OptionalInt.empty() : OptionalInt.of(messageAddOnDurationInSecs);
    }

    /**
     * Returns the strategy used to compute when the add-on expires.
     *
     * @return an {@link Optional} containing the expiry type, or empty if not set
     */
    public Optional<ExpiryType> expiryType() {
        return Optional.ofNullable(messageAddOnExpiryType);
    }

    /**
     * Sets the validity duration of the add-on in seconds.
     *
     * @param messageAddOnDurationInSecs the new duration in seconds, or {@code null} to clear
     */
    public void setMessageAddOnDurationInSecs(Integer messageAddOnDurationInSecs) {
        this.messageAddOnDurationInSecs = messageAddOnDurationInSecs;
    }

    /**
     * Sets the strategy used to compute when the add-on expires.
     *
     * @param expiryType the new expiry strategy, or {@code null} to clear
     */
    public void setExpiryType(ExpiryType expiryType) {
        this.messageAddOnExpiryType = expiryType;
    }

    /**
     * Describes how the expiry of an add-on is computed.
     *
     * <ul>
     *   <li>{@link #STATIC}: the duration is measured from a fixed reference
     *       such as the instant the add-on was created</li>
     *   <li>{@link #DEPENDENT_ON_PARENT}: the expiry follows the lifetime of
     *       the parent message to which the add-on is attached</li>
     * </ul>
     */
    @ProtobufEnum(name = "MessageContextInfo.MessageAddonExpiryType")
    public enum ExpiryType {
        /**
         * The add-on expires after a fixed duration, independent of its parent message.
         */
        STATIC(1),
        /**
         * The add-on expires together with its parent message.
         */
        DEPENDENT_ON_PARENT(2);

        /**
         * Constructs a new expiry type with the given protobuf wire index.
         *
         * @param index the protobuf wire index backing this constant
         */
        ExpiryType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the protobuf wire index
         */
        public int index() {
            return this.index;
        }
    }
}
