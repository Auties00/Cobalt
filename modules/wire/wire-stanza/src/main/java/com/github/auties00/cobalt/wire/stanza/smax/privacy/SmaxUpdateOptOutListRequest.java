package com.github.auties00.cobalt.wire.stanza.smax.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Builds the outbound {@code <iq xmlns="optoutlist" type="set">} stanza that applies a marketing-message opt-out
 * action against a business JID.
 *
 * <p>This request drives the marketing-messages user-controls flows (opt out, opt in, and signup). The three
 * mandatory attributes ({@link #itemJid()}, {@link #itemCategory()}, {@link #itemAction()}) identify the target
 * business and the action being applied, while the five optional attributes pass the cache digest, the opt-out
 * reason, the entry-point, the signup id, and the duration. The {@link #itemAction()} string is derived from the
 * high-level operation: {@code "block"} to opt out, {@code "unblock"} to opt in, and {@code "signup"} to register.
 *
 * @implNote This implementation centralises id generation on the client dispatcher, so {@link #toStanza()} omits the
 * {@code id} attribute; every other SMAX request in the package follows the same convention.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBlocklistsUpdateOptOutListRequest")
public final class SmaxUpdateOptOutListRequest implements SmaxStanza.Request {
    /**
     * The target business JID being opted in or out of marketing messages.
     */
    private final Jid itemJid;

    /**
     * The marketing-messages category that scopes the opt-out.
     */
    private final String itemCategory;

    /**
     * The action string mapped onto the wire ({@code "block"}, {@code "unblock"}, or {@code "signup"}).
     */
    private final String itemAction;

    /**
     * The cached digest of the opt-out list, or {@code null} to skip the cache-match shortcut.
     */
    private final String itemDhash;

    /**
     * The optional marketing-messages reason marker.
     */
    private final String itemReason;

    /**
     * The optional marketing-messages entry-point marker.
     */
    private final String itemEntryPoint;

    /**
     * The optional signup id for the {@code "signup"} action.
     */
    private final String itemSignupId;

    /**
     * The optional opt-out duration in seconds.
     */
    private final Integer itemDuration;

    /**
     * Constructs an update-opt-out request from the three mandatory attributes and every optional field.
     *
     * <p>The {@code itemAction} string is the wire selector derived from the high-level operation: {@code "block"}
     * to opt out, {@code "unblock"} to opt in, and {@code "signup"} to register. Any optional field whose value is
     * unknown may be passed as {@code null}, in which case {@link #toStanza()} omits the corresponding attribute.
     *
     * @param itemJid        the target business JID; never {@code null}
     * @param itemCategory   the marketing category; never {@code null}
     * @param itemAction     the action string; never {@code null}
     * @param itemDhash      the cached digest; may be {@code null}
     * @param itemReason     the reason marker; may be {@code null}
     * @param itemEntryPoint the entry-point marker; may be {@code null}
     * @param itemSignupId   the signup id; may be {@code null}
     * @param itemDuration   the opt-out duration in seconds; may be {@code null}
     * @throws NullPointerException if {@code itemJid}, {@code itemCategory}, or {@code itemAction} is {@code null}
     */
    public SmaxUpdateOptOutListRequest(Jid itemJid, String itemCategory, String itemAction,
                   String itemDhash, String itemReason, String itemEntryPoint,
                   String itemSignupId, Integer itemDuration) {
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
     * Returns the target business JID.
     *
     * @return the JID; never {@code null}
     */
    public Jid itemJid() {
        return itemJid;
    }

    /**
     * Returns the marketing category that scopes the opt-out.
     *
     * @return the category; never {@code null}
     */
    public String itemCategory() {
        return itemCategory;
    }

    /**
     * Returns the wire action string ({@code "block"}, {@code "unblock"}, or {@code "signup"}).
     *
     * @return the action; never {@code null}
     */
    public String itemAction() {
        return itemAction;
    }

    /**
     * Returns the cached digest when set.
     *
     * <p>Absence means the request carries no cache-match shortcut, so the relay always returns a full mismatch
     * reply rather than a cache-match confirmation.
     *
     * @return an {@link Optional} carrying the digest, or empty when no cached digest was supplied
     */
    public Optional<String> itemDhash() {
        return Optional.ofNullable(itemDhash);
    }

    /**
     * Returns the reason marker when set.
     *
     * @return an {@link Optional} carrying the reason, or empty when no reason was supplied
     */
    public Optional<String> itemReason() {
        return Optional.ofNullable(itemReason);
    }

    /**
     * Returns the entry-point marker when set.
     *
     * @return an {@link Optional} carrying the entry-point, or empty when no entry-point was supplied
     */
    public Optional<String> itemEntryPoint() {
        return Optional.ofNullable(itemEntryPoint);
    }

    /**
     * Returns the signup id when set.
     *
     * @return an {@link Optional} carrying the signup id, or empty when no signup id was supplied
     */
    public Optional<String> itemSignupId() {
        return Optional.ofNullable(itemSignupId);
    }

    /**
     * Returns the opt-out duration when set.
     *
     * @return an {@link Optional} carrying the duration in seconds, or empty when no duration was supplied
     */
    public Optional<Integer> itemDuration() {
        return Optional.ofNullable(itemDuration);
    }

    /**
     * Builds the outbound {@code <iq>} stanza ready for dispatch.
     *
     * <p>The returned {@link StanzaBuilder} addresses {@link JidServer#user()} with {@code xmlns="optoutlist"} and
     * {@code type="set"}. The inner {@code <item>} child always carries the three mandatory attributes (target JID,
     * category, action), and the five optional attributes ({@code dhash}, {@code reason}, {@code entry_point},
     * {@code signup_id}, {@code duration}) are added only when their fields are set. The {@code id} attribute is
     * filled in downstream by the client dispatcher.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsUpdateOptOutListRequest",
            exports = "makeUpdateOptOutListRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var itemBuilder = new StanzaBuilder()
                .description("item")
                .attribute("jid", itemJid)
                .attribute("category", itemCategory)
                .attribute("action", itemAction);
        if (itemDhash != null) {
            itemBuilder.attribute("dhash", itemDhash);
        }
        if (itemReason != null) {
            itemBuilder.attribute("reason", itemReason);
        }
        if (itemEntryPoint != null) {
            itemBuilder.attribute("entry_point", itemEntryPoint);
        }
        if (itemSignupId != null) {
            itemBuilder.attribute("signup_id", itemSignupId);
        }
        if (itemDuration != null) {
            itemBuilder.attribute("duration", itemDuration.intValue());
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "optoutlist")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(itemBuilder.build());
    }

    /**
     * Compares this request to another object for value equality across every attribute field.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxUpdateOptOutListRequest} with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxUpdateOptOutListRequest) obj;
        return Objects.equals(this.itemJid, that.itemJid)
                && Objects.equals(this.itemCategory, that.itemCategory)
                && Objects.equals(this.itemAction, that.itemAction)
                && Objects.equals(this.itemDhash, that.itemDhash)
                && Objects.equals(this.itemReason, that.itemReason)
                && Objects.equals(this.itemEntryPoint, that.itemEntryPoint)
                && Objects.equals(this.itemSignupId, that.itemSignupId)
                && Objects.equals(this.itemDuration, that.itemDuration);
    }

    /**
     * Returns a hash code derived from every attribute field.
     *
     * @return the hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(itemJid, itemCategory, itemAction, itemDhash, itemReason,
                itemEntryPoint, itemSignupId, itemDuration);
    }

    /**
     * Returns a debug rendering listing every attribute field.
     *
     * @return a diagnostic string; never {@code null}
     */
    @Override
    public String toString() {
        return "SmaxUpdateOptOutListRequest[itemJid=" + itemJid
                + ", itemCategory=" + itemCategory
                + ", itemAction=" + itemAction
                + ", itemDhash=" + itemDhash
                + ", itemReason=" + itemReason
                + ", itemEntryPoint=" + itemEntryPoint
                + ", itemSignupId=" + itemSignupId
                + ", itemDuration=" + itemDuration + ']';
    }
}
