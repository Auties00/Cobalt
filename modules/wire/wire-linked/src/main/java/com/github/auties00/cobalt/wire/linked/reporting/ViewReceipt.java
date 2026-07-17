package com.github.auties00.cobalt.wire.linked.reporting;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;

/**
 * Input model for {@code WhatsAppClient.publishViewReceipt} — publishes
 * a view receipt for one or more items the local user has viewed.
 *
 * <p>{@link #receiptId} and {@link #to} are required.
 * {@link #hasStatusClass} is a primitive flag, and
 * {@link #itemServerIds} carries the items being acknowledged.
 */
@ProtobufMessage
public final class ViewReceipt {
    /**
     * Identifier of the receipt being published.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String receiptId;

    /**
     * JID of the addressee — the receipt's destination.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final Jid to;

    /**
     * Whether the receipt belongs to the status class (vs. regular
     * messaging).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean hasStatusClass;

    /**
     * Server-assigned ids of the items the receipt acknowledges.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final List<Integer> itemServerIds;

    /**
     * Constructs a new {@code ViewReceipt}.
     *
     * @param receiptId      the receipt id; required
     * @param to             the destination JID; required
     * @param hasStatusClass whether the receipt is a status-class one
     * @param itemServerIds  the optional list of acknowledged item ids
     * @throws NullPointerException if {@code receiptId} or {@code to}
     *                              is {@code null}
     */
    ViewReceipt(String receiptId, Jid to, boolean hasStatusClass, List<Integer> itemServerIds) {
        this.receiptId = Objects.requireNonNull(receiptId, "receiptId cannot be null");
        this.to = Objects.requireNonNull(to, "to cannot be null");
        this.hasStatusClass = hasStatusClass;
        this.itemServerIds = itemServerIds;
    }

    /**
     * Returns the receipt identifier.
     *
     * @return the id, never {@code null}
     */
    public String receiptId() {
        return receiptId;
    }

    /**
     * Returns the destination JID.
     *
     * @return the JID, never {@code null}
     */
    public Jid to() {
        return to;
    }

    /**
     * Returns whether the receipt is a status-class one.
     *
     * @return {@code true} for status-class receipts
     */
    public boolean hasStatusClass() {
        return hasStatusClass;
    }

    /**
     * Returns the acknowledged item ids.
     *
     * @return the item ids; never {@code null}
     */
    public List<Integer> itemServerIds() {
        return itemServerIds == null ? List.of() : itemServerIds;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ViewReceipt) obj;
        return Objects.equals(receiptId, that.receiptId) &&
                Objects.equals(to, that.to) &&
                hasStatusClass == that.hasStatusClass &&
                Objects.equals(itemServerIds, that.itemServerIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiptId, to, hasStatusClass, itemServerIds);
    }

    @Override
    public String toString() {
        return "ViewReceipt[" +
                "receiptId=" + receiptId + ", " +
                "to=" + to + ", " +
                "hasStatusClass=" + hasStatusClass + ", " +
                "itemServerIds=" + itemServerIds + ']';
    }
}
