package com.github.auties00.cobalt.wire.linked.setting.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Input model for {@code LinkedWhatsAppClient.updateOptOutList} — applies a
 * single mutation to the user's opt-out registry.
 *
 * <p>{@link #itemJid}, {@link #itemCategory} and {@link #itemAction}
 * are required; every other field is optional context routed through
 * to the {@code SmaxUpdateOptOutListRequest}.
 */
@ProtobufMessage
public final class OptOutListUpdate {
    /**
     * JID of the contact / chat the mutation targets.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid itemJid;

    /**
     * Opt-out category code identifying the registry the item belongs to.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String itemCategory;

    /**
     * Action code applied to the registry entry (e.g. add or remove).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String itemAction;

    /**
     * Optional dedup hash carried for idempotent reconciliation.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String itemDhash;

    /**
     * Optional reason code surfaced to the trust-and-safety pipeline.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String itemReason;

    /**
     * Optional entry-point tag identifying the UI surface that triggered
     * the mutation.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String itemEntryPoint;

    /**
     * Optional signup identifier tying the mutation to a specific
     * registration flow.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String itemSignupId;

    /**
     * Optional duration (in seconds) for time-bounded opt-out entries.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT32)
    final Integer itemDuration;

    /**
     * Constructs a new {@code OptOutListUpdate}.
     *
     * @param itemJid        the target JID; required
     * @param itemCategory   the opt-out category code; required
     * @param itemAction     the action code; required
     * @param itemDhash      the optional dedup hash, or {@code null}
     * @param itemReason     the optional reason code, or {@code null}
     * @param itemEntryPoint the optional entry-point tag, or {@code null}
     * @param itemSignupId   the optional signup id, or {@code null}
     * @param itemDuration   the optional duration in seconds, or {@code null}
     * @throws NullPointerException if {@code itemJid}, {@code itemCategory}
     *                              or {@code itemAction} is {@code null}
     */
    OptOutListUpdate(Jid itemJid, String itemCategory, String itemAction, String itemDhash,
                     String itemReason, String itemEntryPoint, String itemSignupId, Integer itemDuration) {
        this.itemJid = Objects.requireNonNull(itemJid, "itemJid cannot be null");
        this.itemCategory = Objects.requireNonNull(itemCategory, "itemCategory cannot be null");
        this.itemAction = Objects.requireNonNull(itemAction, "itemAction cannot be null");
        this.itemDhash = itemDhash;
        this.itemReason = itemReason;
        this.itemEntryPoint = itemEntryPoint;
        this.itemSignupId = itemSignupId;
        this.itemDuration = itemDuration;
    }

    /**
     * Returns the target JID.
     *
     * @return the JID, never {@code null}
     */
    public Jid itemJid() {
        return itemJid;
    }

    /**
     * Returns the opt-out category code.
     *
     * @return the category, never {@code null}
     */
    public String itemCategory() {
        return itemCategory;
    }

    /**
     * Returns the action code.
     *
     * @return the action, never {@code null}
     */
    public String itemAction() {
        return itemAction;
    }

    /**
     * Returns the optional dedup hash.
     *
     * @return an {@link Optional} carrying the hash, or empty when unset
     */
    public Optional<String> itemDhash() {
        return Optional.ofNullable(itemDhash);
    }

    /**
     * Returns the optional reason code.
     *
     * @return an {@link Optional} carrying the reason, or empty when unset
     */
    public Optional<String> itemReason() {
        return Optional.ofNullable(itemReason);
    }

    /**
     * Returns the optional entry-point tag.
     *
     * @return an {@link Optional} carrying the tag, or empty when unset
     */
    public Optional<String> itemEntryPoint() {
        return Optional.ofNullable(itemEntryPoint);
    }

    /**
     * Returns the optional signup identifier.
     *
     * @return an {@link Optional} carrying the signup id, or empty when unset
     */
    public Optional<String> itemSignupId() {
        return Optional.ofNullable(itemSignupId);
    }

    /**
     * Returns the optional duration (seconds).
     *
     * @return an {@link OptionalInt} carrying the duration, or empty when unset
     */
    public OptionalInt itemDuration() {
        return itemDuration == null ? OptionalInt.empty() : OptionalInt.of(itemDuration);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (OptOutListUpdate) obj;
        return Objects.equals(itemJid, that.itemJid) &&
                Objects.equals(itemCategory, that.itemCategory) &&
                Objects.equals(itemAction, that.itemAction) &&
                Objects.equals(itemDhash, that.itemDhash) &&
                Objects.equals(itemReason, that.itemReason) &&
                Objects.equals(itemEntryPoint, that.itemEntryPoint) &&
                Objects.equals(itemSignupId, that.itemSignupId) &&
                Objects.equals(itemDuration, that.itemDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemJid, itemCategory, itemAction, itemDhash, itemReason, itemEntryPoint,
                itemSignupId, itemDuration);
    }

    @Override
    public String toString() {
        return "OptOutListUpdate[" +
                "itemJid=" + itemJid + ", " +
                "itemCategory=" + itemCategory + ", " +
                "itemAction=" + itemAction + ", " +
                "itemDhash=" + itemDhash + ", " +
                "itemReason=" + itemReason + ", " +
                "itemEntryPoint=" + itemEntryPoint + ", " +
                "itemSignupId=" + itemSignupId + ", " +
                "itemDuration=" + itemDuration + ']';
    }
}
