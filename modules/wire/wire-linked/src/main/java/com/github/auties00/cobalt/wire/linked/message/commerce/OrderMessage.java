package com.github.auties00.cobalt.wire.linked.message.commerce;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * A message describing a customer order placed against a WhatsApp Business
 * catalog.
 *
 * <p>Order messages are produced when a customer submits an order inquiry
 * from a business's catalog, and again when the business accepts or
 * declines the request. Each message carries the order identifier, a
 * thumbnail preview, the number of items, the current {@link OrderStatus}
 * of the order, the {@link OrderSurface} on which it was created, the
 * seller's JID, a monetary total expressed in thousandths of the unit of
 * the given currency, a payment correlation token, and a {@link MessageKey}
 * pointing back to the original order request message so that the buyer
 * and seller can correlate the full order conversation.
 */
@ProtobufMessage(name = "Message.OrderMessage")
public final class OrderMessage implements ContextualMessage {
    /**
     * The server-issued identifier of this order, used to correlate the
     * buyer- and seller-side messages that make up the same order.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String orderId;

    /**
     * A thumbnail preview of the order, typically showing the first ordered
     * product. Encoded as raw image bytes ready for inline rendering by the
     * recipient client.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] thumbnail;

    /**
     * The number of distinct items included in the order.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    Integer itemCount;

    /**
     * The current lifecycle status of the order, distinguishing inquiries,
     * accepted orders and declined orders.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    OrderStatus status;

    /**
     * Declares the surface of the business account that produced the order
     * (for example a WhatsApp Business catalog).
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    OrderSurface surface;

    /**
     * An optional free-text message accompanying the order, typically a note
     * the buyer attached when submitting the order or a note from the seller
     * when responding.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String message;

    /**
     * The human-readable title of the order, typically the name of the
     * catalog or the lead product, used to label the order in the chat.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String orderTitle;

    /**
     * The JID of the business account that owns the catalog from which the
     * order was placed.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    Jid sellerJid;

    /**
     * The server-issued token used to correlate the order with the
     * downstream payment flow.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String token;

    /**
     * The total price of the order expressed in thousandths of the currency
     * unit declared in {@link #totalCurrencyCode}. For example, a value of
     * {@code 12345} in {@code USD} represents {@code 12.345} US dollars.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.INT64)
    Long totalAmount1000;

    /**
     * The ISO 4217 currency code (for example {@code "USD"} or {@code "EUR"})
     * used to interpret {@link #totalAmount1000}.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    String totalCurrencyCode;

    /**
     * Contextual metadata attached to this message, such as a quoted
     * message reference, mentions or forwarding score.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * The version of the order message format, used by clients to handle
     * backward-compatible changes to the payload.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.INT32)
    Integer messageVersion;

    /**
     * The key of the original order request message that this order
     * responds to, allowing buyers and sellers to correlate the full order
     * conversation.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
    MessageKey orderRequestMessageId;

    /**
     * The type of catalog the order was placed from, encoded as a wire
     * string (see {@link CatalogType} for the canonical set of values).
     */
    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    String catalogType;


    /**
     * Constructs a new order message with every field set explicitly.
     *
     * @param orderId               the server-issued order identifier, or {@code null}
     * @param thumbnail             the preview thumbnail bytes, or {@code null}
     * @param itemCount             the number of items in the order, or {@code null}
     * @param status                the current lifecycle status of the order, or {@code null}
     * @param surface               the originating surface of the order, or {@code null}
     * @param message               a free-text message accompanying the order, or {@code null}
     * @param orderTitle            the human-readable title of the order, or {@code null}
     * @param sellerJid             the JID of the selling business, or {@code null}
     * @param token                 the payment correlation token, or {@code null}
     * @param totalAmount1000       the total price in thousandths of the currency unit, or {@code null}
     * @param totalCurrencyCode     the ISO 4217 currency code, or {@code null}
     * @param contextInfo           the contextual metadata of the message, or {@code null}
     * @param messageVersion        the order message format version, or {@code null}
     * @param orderRequestMessageId the key of the originating order request, or {@code null}
     * @param catalogType           the catalog type from which the order was placed, or {@code null}
     */
    OrderMessage(String orderId, byte[] thumbnail, Integer itemCount, OrderStatus status, OrderSurface surface, String message, String orderTitle, Jid sellerJid, String token, Long totalAmount1000, String totalCurrencyCode, ContextInfo contextInfo, Integer messageVersion, MessageKey orderRequestMessageId, String catalogType) {
        this.orderId = orderId;
        this.thumbnail = thumbnail;
        this.itemCount = itemCount;
        this.status = status;
        this.surface = surface;
        this.message = message;
        this.orderTitle = orderTitle;
        this.sellerJid = sellerJid;
        this.token = token;
        this.totalAmount1000 = totalAmount1000;
        this.totalCurrencyCode = totalCurrencyCode;
        this.contextInfo = contextInfo;
        this.messageVersion = messageVersion;
        this.orderRequestMessageId = orderRequestMessageId;
        this.catalogType = catalogType;
    }

    /**
     * Returns the server-issued identifier of this order.
     *
     * @return an {@link Optional} containing the order identifier, or empty if not set
     */
    public Optional<String> orderId() {
        return Optional.ofNullable(orderId);
    }

    /**
     * Returns the thumbnail preview of this order.
     *
     * @return an {@link Optional} containing the thumbnail bytes, or empty if not set
     */
    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    /**
     * Returns the number of distinct items included in the order.
     *
     * @return an {@link OptionalInt} containing the item count, or empty if not set
     */
    public OptionalInt itemCount() {
        return itemCount == null ? OptionalInt.empty() : OptionalInt.of(itemCount);
    }

    /**
     * Returns the current lifecycle status of the order.
     *
     * @return an {@link Optional} containing the status, or empty if not set
     */
    public Optional<OrderStatus> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the surface of the business account that produced the order.
     *
     * @return an {@link Optional} containing the surface, or empty if not set
     */
    public Optional<OrderSurface> surface() {
        return Optional.ofNullable(surface);
    }

    /**
     * Returns the free-text message accompanying the order.
     *
     * @return an {@link Optional} containing the message text, or empty if not set
     */
    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    /**
     * Returns the human-readable title of the order.
     *
     * @return an {@link Optional} containing the title, or empty if not set
     */
    public Optional<String> orderTitle() {
        return Optional.ofNullable(orderTitle);
    }

    /**
     * Returns the JID of the business account that owns the catalog from
     * which the order was placed.
     *
     * @return an {@link Optional} containing the seller JID, or empty if not set
     */
    public Optional<Jid> sellerJid() {
        return Optional.ofNullable(sellerJid);
    }

    /**
     * Returns the payment correlation token.
     *
     * @return an {@link Optional} containing the token, or empty if not set
     */
    public Optional<String> token() {
        return Optional.ofNullable(token);
    }

    /**
     * Returns the total price of the order expressed in thousandths of the
     * currency unit declared in {@link #totalCurrencyCode()}.
     *
     * @return an {@link OptionalLong} containing the total in thousandths, or empty if not set
     */
    public OptionalLong totalAmount1000() {
        return totalAmount1000 == null ? OptionalLong.empty() : OptionalLong.of(totalAmount1000);
    }

    /**
     * Returns the ISO 4217 currency code used to interpret the total amount.
     *
     * @return an {@link Optional} containing the currency code, or empty if not set
     */
    public Optional<String> totalCurrencyCode() {
        return Optional.ofNullable(totalCurrencyCode);
    }

    /**
     * Returns the contextual metadata of this message.
     *
     * @return an {@link Optional} containing the context info, or empty if not set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the version of the order message format.
     *
     * @return an {@link OptionalInt} containing the message version, or empty if not set
     */
    public OptionalInt messageVersion() {
        return messageVersion == null ? OptionalInt.empty() : OptionalInt.of(messageVersion);
    }

    /**
     * Returns the key of the original order request message that this order
     * responds to.
     *
     * @return an {@link Optional} containing the request message key, or empty if not set
     */
    public Optional<MessageKey> orderRequestMessageId() {
        return Optional.ofNullable(orderRequestMessageId);
    }

    /**
     * Returns the type of catalog the order was placed from.
     *
     * @return an {@link Optional} containing the catalog type, or empty if not set
     */
    public Optional<String> catalogType() {
        return Optional.ofNullable(catalogType);
    }

    /**
     * Sets the server-issued identifier of this order.
     *
     * @param orderId the order identifier, or {@code null} to clear it
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * Sets the thumbnail preview of this order.
     *
     * @param thumbnail the thumbnail bytes, or {@code null} to clear them
     */
    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    /**
     * Sets the number of items included in the order.
     *
     * @param itemCount the item count, or {@code null} to clear it
     */
    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    /**
     * Sets the current lifecycle status of the order.
     *
     * @param status the status, or {@code null} to clear it
     */
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    /**
     * Sets the surface of the business account that produced the order.
     *
     * @param surface the surface, or {@code null} to clear it
     */
    public void setSurface(OrderSurface surface) {
        this.surface = surface;
    }

    /**
     * Sets the free-text message accompanying the order.
     *
     * @param message the message text, or {@code null} to clear it
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Sets the human-readable title of the order.
     *
     * @param orderTitle the title, or {@code null} to clear it
     */
    public void setOrderTitle(String orderTitle) {
        this.orderTitle = orderTitle;
    }

    /**
     * Sets the JID of the business account that owns the catalog.
     *
     * @param sellerJid the seller JID, or {@code null} to clear it
     */
    public void setSellerJid(Jid sellerJid) {
        this.sellerJid = sellerJid;
    }

    /**
     * Sets the payment correlation token.
     *
     * @param token the token, or {@code null} to clear it
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Sets the total price of the order expressed in thousandths of the
     * currency unit.
     *
     * @param totalAmount1000 the total in thousandths, or {@code null} to clear it
     */
    public void setTotalAmount1000(Long totalAmount1000) {
        this.totalAmount1000 = totalAmount1000;
    }

    /**
     * Sets the ISO 4217 currency code used to interpret the total amount.
     *
     * @param totalCurrencyCode the currency code, or {@code null} to clear it
     */
    public void setTotalCurrencyCode(String totalCurrencyCode) {
        this.totalCurrencyCode = totalCurrencyCode;
    }

    /**
     * Sets the contextual metadata of this message.
     *
     * @param contextInfo the context info, or {@code null} to clear it
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets the version of the order message format.
     *
     * @param messageVersion the message version, or {@code null} to clear it
     */
    public void setMessageVersion(Integer messageVersion) {
        this.messageVersion = messageVersion;
    }

    /**
     * Sets the key of the original order request message.
     *
     * @param orderRequestMessageId the request message key, or {@code null} to clear it
     */
    public void setOrderRequestMessageId(MessageKey orderRequestMessageId) {
        this.orderRequestMessageId = orderRequestMessageId;
    }

    /**
     * Sets the type of catalog the order was placed from.
     *
     * @param catalogType the catalog type, or {@code null} to clear it
     */
    public void setCatalogType(String catalogType) {
        this.catalogType = catalogType;
    }

    /**
     * Declares the lifecycle state of an {@link OrderMessage}.
     */
    @ProtobufEnum(name = "Message.OrderMessage.OrderStatus")
    public static enum OrderStatus {
        /**
         * The customer has submitted an order inquiry and is awaiting a
         * response from the seller.
         */
        INQUIRY(1),
        /**
         * The seller has accepted the order.
         */
        ACCEPTED(2),
        /**
         * The seller has declined the order.
         */
        DECLINED(3);

        /**
         * Constructs an order status with the given protobuf wire index.
         *
         * @param index the protobuf wire index
         */
        OrderStatus(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index of this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this enum constant.
         *
         * @return the protobuf wire index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Declares the surface from which an {@link OrderMessage} originates.
     */
    @ProtobufEnum(name = "Message.OrderMessage.OrderSurface")
    public static enum OrderSurface {
        /**
         * The order was placed from a WhatsApp Business catalog.
         */
        CATALOG(1);

        /**
         * Constructs an order surface with the given protobuf wire index.
         *
         * @param index the protobuf wire index
         */
        OrderSurface(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index of this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this enum constant.
         *
         * @return the protobuf wire index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Enumerates the canonical catalog backends that can originate an
     * {@link OrderMessage}.
     *
     * <p>The wire representation of {@link OrderMessage#catalogType} is a
     * plain string: each constant of this enum corresponds to one of the
     * recognised string values. Cobalt exposes the set as a typed
     * {@link Enum} so that call sites can branch on a compile-checked value
     * instead of spelling the string literally, but the field on
     * {@link OrderMessage} remains a {@link String} to preserve the
     * over-the-wire format.
     */
    public enum CatalogType {
        /**
         * The order was placed from a Meta-hosted small-business catalog.
         */
        SMB_META_CATALOG,

        /**
         * The order was placed from the business's native WhatsApp catalog.
         */
        NATIVE,

        /**
         * The catalog backend could not be determined for the order.
         */
        UNKNOWN;

        /**
         * Returns the wire-format name of this catalog type, exactly matching
         * the string value used in the {@code catalogType} proto field.
         *
         * <p>This is identical to {@link #name()}; the dedicated accessor
         * exists so that callers do not accidentally couple to
         * {@link Enum#name()} for the wire format.
         *
         * @return the string key used in the {@code catalogType} proto field
         */
        public String wireName() {
            return name();
        }

        /**
         * Resolves a wire-format catalog type name to its enum constant, or
         * returns {@code null} when the name does not correspond to any
         * known catalog type.
         *
         * <p>Unlike {@link #valueOf(String)} this method does not throw on
         * unknown input, supporting defensive lookup for catalog types that
         * may have been introduced on the server since the client was built.
         *
         * @param name the wire-format catalog type name, or {@code null}
         * @return the matching {@link CatalogType}, or {@code null} when
         *         {@code name} is {@code null} or not recognised
         */
        public static CatalogType fromWireName(String name) {
            if (name == null) {
                return null;
            }
            for (var type : values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}
