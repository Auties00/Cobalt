package com.github.auties00.cobalt.wire.stanza.iq.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound legacy {@code <iq xmlns="privacy" type="set"><privacy><category/></privacy></iq>} stanza
 * that mutates exactly one row of the user's privacy settings.
 * <p>
 * Dispatching this request changes a single privacy row (last seen, online presence, profile
 * picture, about, group-add audience, call-add audience, messages, defense mode); the matching
 * read-receipts toggle has its own {@link IqSetReadReceiptRequest}. To extend or retract a
 * per-category exclusion list (the {@link IqQueryPrivacySettingsVisibility#CONTACT_BLACKLIST}
 * value), {@link #users()} carries one {@link IqSetPrivacyUserEntry} per peer plus the
 * previous-state {@link #dhash()} digest matching WA Web's {@code privacyDisallowedList} table.
 *
 * @implNote
 * This implementation maps directly to WA Web's {@code WAWebSetPrivacyJob.setPrivacy}; the
 * higher-level batching and snapshot reconciliation that {@code WAWebSetPrivacyForOneCategoryAction}
 * layers on top (per-category disallowed-list sync on {@code 409}, local user-prefs write,
 * disallowed-list table merge) is the caller's responsibility in Cobalt.
 */
@WhatsAppWebModule(moduleName = "WAWebSetPrivacyJob")
public final class IqSetPrivacyRequest implements IqStanza.Request {
    /**
     * The privacy category being set.
     */
    private final IqQueryPrivacySettingsCategoryName name;

    /**
     * The new visibility value for the category.
     */
    private final IqQueryPrivacySettingsVisibility value;

    /**
     * The optional user-list mutation list; when empty the request omits {@code <user>} children
     * entirely (bare-category shape). When non-empty the wire shape depends on
     * {@link #addressingMode}.
     */
    private final List<IqSetPrivacyUserEntry> users;

    /**
     * The wire addressing mode; selects between phone-number and LID envelopes when {@link #users}
     * is non-empty.
     */
    private final IqSetPrivacyAddressingMode addressingMode;

    /**
     * The optional previous-state digest of the per-category exclusion list; emitted as
     * {@code dhash="..."} on the {@code <category>} element when {@link #users} is non-empty. A
     * {@code null} value serialises to the literal {@code "none"} sentinel matching WA Web's
     * fallback.
     */
    private final String dhash;

    /**
     * Constructs a request.
     * <p>
     * Passing {@link List#of()} for {@code users} emits the bare
     * {@code <category name=... value=.../>} shape (the only shape required for non-blacklist
     * values). Passing a non-empty list together with a matching {@code dhash} mutates the
     * per-category exclusion list under either {@link IqSetPrivacyAddressingMode#PN} or
     * {@link IqSetPrivacyAddressingMode#LID}.
     *
     * @implNote
     * This implementation defensively copies {@code users} via
     * {@link List#copyOf(java.util.Collection)}; the resulting instance is immutable.
     *
     * @param name           the category; never {@code null}
     * @param value          the new value; never {@code null}
     * @param users          the user-list mutation list; never {@code null} (use {@link List#of()}
     *                       for the bare-category shape)
     * @param addressingMode the addressing mode; never {@code null}
     * @param dhash          the optional previous-state digest; may be {@code null}
     * @throws NullPointerException if {@code name}, {@code value}, {@code users}, or
     *                              {@code addressingMode} is {@code null}
     */
    public IqSetPrivacyRequest(IqQueryPrivacySettingsCategoryName name,
                   IqQueryPrivacySettingsVisibility value,
                   List<IqSetPrivacyUserEntry> users,
                   IqSetPrivacyAddressingMode addressingMode,
                   String dhash) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(users, "users cannot be null");
        this.users = List.copyOf(users);
        this.addressingMode = Objects.requireNonNull(addressingMode, "addressingMode cannot be null");
        this.dhash = dhash;
    }

    /**
     * Returns the category being set.
     *
     * @return the category; never {@code null}
     */
    public IqQueryPrivacySettingsCategoryName name() {
        return name;
    }

    /**
     * Returns the new value for the category.
     *
     * @return the value; never {@code null}
     */
    public IqQueryPrivacySettingsVisibility value() {
        return value;
    }

    /**
     * Returns the user-list mutation list.
     * <p>
     * Empty for the bare-category shape (every value other than
     * {@link IqQueryPrivacySettingsVisibility#CONTACT_BLACKLIST}); non-empty when extending or
     * retracting an exclusion list.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<IqSetPrivacyUserEntry> users() {
        return users;
    }

    /**
     * Returns the wire addressing mode.
     *
     * @return the addressing mode; never {@code null}
     */
    public IqSetPrivacyAddressingMode addressingMode() {
        return addressingMode;
    }

    /**
     * Returns the optional previous-state digest.
     * <p>
     * Mirrors the digest field WA Web stores on
     * {@code WAWebSchemaPrivacyDisallowedList.PrivacyDisallowedListType} rows; the relay uses it to
     * detect concurrent edits, returning a {@code 409} client error when the caller's view is stale.
     *
     * @return the digest, or {@link Optional#empty()} when the caller did not supply one
     */
    public Optional<String> dhash() {
        return Optional.ofNullable(dhash);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation serialises the typed fields into the canonical
     * {@code <iq xmlns="privacy" type="set">} envelope. The shape is selected as follows:
     * <ul>
     *   <li>empty {@link #users} emits the bare
     *   {@code <privacy><category name=... value=.../></privacy>} payload (WA Web's
     *   {@code _(name, value)} branch);
     *   <li>non-empty {@link #users} under {@link IqSetPrivacyAddressingMode#PN} emits
     *   {@code <privacy><category dhash=...><user action=... jid=PN/>...</category></privacy>} (WA
     *   Web's {@code g(name, value, users, dhash)} branch);
     *   <li>non-empty {@link #users} under {@link IqSetPrivacyAddressingMode#LID} adds
     *   {@code addressing_mode="lid"} on the {@code <privacy>} element and emits each {@code <user>}
     *   with the LID JID and either a {@code username} or {@code pn_jid} discriminator (WA Web's
     *   {@code f(name, value, users, dhash)} branch).
     * </ul>
     * A {@code null} {@link #dhash} serialises as the literal {@code "none"} sentinel, matching WA
     * Web's {@code r!=null?r:"none"} fallback.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSetPrivacyJob",
            exports = "setPrivacy", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var categoryBuilder = new StanzaBuilder()
                .description("category")
                .attribute("name", name.wire())
                .attribute("value", value.wire());
        if (!users.isEmpty()) {
            categoryBuilder.attribute("dhash", dhash != null ? dhash : "none");
            var userNodes = new ArrayList<Stanza>();
            for (var entry : users) {
                var userBuilder = new StanzaBuilder()
                        .description("user")
                        .attribute("action", entry.action().wire())
                        .attribute("jid", entry.jid());
                if (addressingMode == IqSetPrivacyAddressingMode.LID) {
                    if (entry.username().isPresent()) {
                        userBuilder.attribute("username", entry.username().get());
                    } else if (entry.pnJid().isPresent()) {
                        userBuilder.attribute("pn_jid", entry.pnJid().get());
                    }
                }
                userNodes.add(userBuilder.build());
            }
            categoryBuilder.content(userNodes);
        }
        var privacyBuilder = new StanzaBuilder()
                .description("privacy");
        if (addressingMode == IqSetPrivacyAddressingMode.LID && !users.isEmpty()) {
            privacyBuilder.attribute("addressing_mode", "lid");
        }
        var privacyNode = privacyBuilder
                .content(categoryBuilder.build())
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "privacy")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(privacyNode);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation compares every typed field by value.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSetPrivacyRequest) obj;
        return this.name == that.name
                && this.value == that.value
                && this.addressingMode == that.addressingMode
                && Objects.equals(this.users, that.users)
                && Objects.equals(this.dhash, that.dhash);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation hashes every typed field consistently with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, value, users, addressingMode, dhash);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits a debug-only representation of every typed field; the format is not
     * stable and must not be parsed.
     */
    @Override
    public String toString() {
        return "IqSetPrivacyRequest[name=" + name + ", value=" + value
                + ", users=" + users + ", addressingMode=" + addressingMode
                + ", dhash=" + dhash + ']';
    }
}
