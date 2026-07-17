package com.github.auties00.cobalt.wire.stanza.usync;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One user entry inside a {@link UsyncQuery}.
 *
 * <p>One entry is built per peer the relay should report on, then {@code with*}
 * setters are chained to attach the optional hints the per-protocol builders
 * consume (device hash, LID, persona id, username PIN, trusted-contact token).
 * The relay accepts four ways to address a peer (canonical {@link Jid}, phone
 * number, username, phone-JID hint); to make the "at least one addressing
 * identifier" invariant impossible to violate at compile time, instances are
 * created through one of the four {@code by*} static factories instead of a
 * public no-arg constructor.
 *
 * @implNote
 * WA Web uses a no-arg constructor plus a runtime validation call inside the
 * query serialiser that throws when none of the slots are populated; this
 * implementation narrows the entry point to four typed factories so the
 * invariant is enforced at compile time instead.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncUser")
public final class UsyncUser {
    /**
     * Canonical user JID, when set.
     */
    private Jid id;

    /**
     * LID JID, when set.
     */
    private Jid lid;

    /**
     * Phone-number JID, when set.
     */
    private Jid phoneJid;

    /**
     * Phone number in E.164 form without the leading {@code +}, when set.
     */
    private String phoneNumber;

    /**
     * Cached device-list hash (base64-encoded), when set.
     */
    private String deviceHash;

    /**
     * Timestamp the local cache last refreshed at, when set.
     */
    private Instant timestamp;

    /**
     * Timestamp the relay should compare against, when set.
     */
    private Instant expectedTimestamp;

    /**
     * Persona id consumed by the bot-profile protocol, when set.
     */
    private String personaId;

    /**
     * Username consumed by the contact protocol's username addressing, when
     * set.
     */
    private String username;

    /**
     * Username PIN that accompanies a username addressing, when set.
     */
    private String pin;

    /**
     * Contact-protocol type discriminator (e.g. {@code "in"}, {@code "out"}),
     * when set.
     */
    private String contactType;

    /**
     * Trusted-contact token consumed by the status protocol, when set.
     */
    private byte[] trustedContactToken;

    /**
     * Hidden constructor invoked through the {@code by*} factories.
     *
     * <p>Kept private to force creation through a factory that populates at
     * least one addressing slot.
     */
    private UsyncUser() {
    }

    /**
     * Creates a user entry addressed by canonical JID.
     *
     * <p>Used by callers that already hold a resolved
     * {@link com.github.auties00.cobalt.wire.core.jid.JidServer#user()} or
     * {@link com.github.auties00.cobalt.wire.core.jid.JidServer#lid()} JID; the
     * relay resolves the rest from the device list.
     *
     * @param id the canonical JID
     * @return a fresh entry
     * @throws NullPointerException if {@code id} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withId", adaptation = WhatsAppAdaptation.ADAPTED)
    public static UsyncUser byId(Jid id) {
        Objects.requireNonNull(id, "id cannot be null");
        var user = new UsyncUser();
        user.id = id;
        return user;
    }

    /**
     * Creates a user entry addressed by phone number.
     *
     * <p>Used by contact-import flows that ship phone numbers from the local
     * address book and ask the relay to resolve them to canonical JIDs.
     *
     * @param phoneNumber the phone number, in E.164 form without the leading
     *                    {@code +}
     * @return a fresh entry
     * @throws NullPointerException if {@code phoneNumber} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withPhone", adaptation = WhatsAppAdaptation.ADAPTED)
    public static UsyncUser byPhoneNumber(String phoneNumber) {
        Objects.requireNonNull(phoneNumber, "phoneNumber cannot be null");
        var user = new UsyncUser();
        user.phoneNumber = phoneNumber;
        return user;
    }

    /**
     * Creates a user entry addressed by username.
     *
     * <p>Used by username-lookup flows that resolve a claimed username to its
     * canonical JID.
     *
     * @param username the username, without the leading {@code @}
     * @return a fresh entry
     * @throws NullPointerException if {@code username} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withUsername", adaptation = WhatsAppAdaptation.ADAPTED)
    public static UsyncUser byUsername(String username) {
        Objects.requireNonNull(username, "username cannot be null");
        var user = new UsyncUser();
        user.username = username;
        return user;
    }

    /**
     * Creates a user entry addressed by phone-JID hint.
     *
     * <p>Used when the local store has a phone-JID hint but no canonical JID
     * yet; the relay resolves the hint to a canonical JID before processing the
     * per-protocol elements.
     *
     * @param phoneJid the phone-number JID
     * @return a fresh entry
     * @throws NullPointerException if {@code phoneJid} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withPnJid", adaptation = WhatsAppAdaptation.ADAPTED)
    public static UsyncUser byPhoneJid(Jid phoneJid) {
        Objects.requireNonNull(phoneJid, "phoneJid cannot be null");
        var user = new UsyncUser();
        user.phoneJid = phoneJid;
        return user;
    }

    /**
     * Attaches a canonical JID alongside the primary addressing slot.
     *
     * <p>Useful when the canonical JID is learned after the entry was created
     * via one of the non-JID factories.
     *
     * @param id the canonical JID
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withId", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncUser withId(Jid id) {
        this.id = id;
        return this;
    }

    /**
     * Attaches a LID identifier for this user.
     *
     * <p>Consumed by {@link com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncLidProtocol}
     * as a hint the relay should confirm, and by the contact protocol when the
     * request uses username addressing.
     *
     * @param lid the LID JID
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withLid", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncUser withLid(Jid lid) {
        this.lid = lid;
        return this;
    }

    /**
     * Attaches a phone-JID hint.
     *
     * <p>Emitted onto the {@code pn_jid} attribute of the {@code <user>} entry;
     * the relay uses it to disambiguate when the canonical JID is also a LID.
     *
     * @param phoneJid the phone-number JID
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withPnJid", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncUser withPhoneJid(Jid phoneJid) {
        this.phoneJid = phoneJid;
        return this;
    }

    /**
     * Sets the cached device-list hash for the device protocol.
     *
     * <p>When the hash matches the relay's current device-list hash the
     * response carries {@code <devices type="omitted">} instead of the full
     * list, so device sync can skip a re-fetch.
     *
     * @implNote
     * This implementation treats {@code null} and blank strings as "no hash".
     *
     * @param deviceHash the hash, base64-encoded
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withDeviceHash", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncUser withDeviceHash(String deviceHash) {
        this.deviceHash = (deviceHash == null || deviceHash.isBlank()) ? null : deviceHash;
        return this;
    }

    /**
     * Sets the timestamp the local cache last refreshed at.
     *
     * <p>Emitted on the {@code ts} attribute of the per-user {@code <devices>}
     * element; combined with {@link #withDeviceHash(String)} to drive the
     * relay's omit-vs-resend decision.
     *
     * @param timestamp the cache timestamp
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withTs", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncUser withTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Sets the expected timestamp the relay should compare against.
     *
     * <p>Emitted on the {@code expected_ts} attribute of the per-user
     * {@code <devices>} element; consumed by the relay's stale-cache detection.
     *
     * @param expectedTimestamp the expected timestamp
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withExpectedTs", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncUser withExpectedTimestamp(Instant expectedTimestamp) {
        this.expectedTimestamp = expectedTimestamp;
        return this;
    }

    /**
     * Sets the persona id for the bot-profile protocol.
     *
     * <p>Consumed by {@link com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncBotProfileProtocol#buildUserElement(UsyncUser)}
     * to attach a {@code persona_id} attribute on the inner {@code <profile/>}
     * child of the {@code <bot>} element; identifies which persona of a
     * multi-persona bot the request targets.
     *
     * @param personaId the persona identifier
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withPersonaId", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncUser withPersonaId(String personaId) {
        this.personaId = personaId;
        return this;
    }

    /**
     * Sets the username PIN that accompanies a username addressing.
     *
     * <p>Emitted on the {@code pin} attribute of the per-user {@code <contact>}
     * element when the user is addressed by {@link #byUsername(String) username};
     * the relay uses the PIN to verify the caller's right to resolve the
     * username.
     *
     * @param pin the username PIN
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withUsernameKey", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncUser withPin(String pin) {
        this.pin = pin;
        return this;
    }

    /**
     * Sets the contact-protocol type discriminator.
     *
     * <p>Emitted on the {@code type} attribute of the per-user {@code <contact>}
     * element when the entry carries neither a phone number nor a username;
     * used by contact-direction queries (e.g. {@code "in"}, {@code "out"}).
     *
     * @param contactType the type literal
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withType", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncUser withContactType(String contactType) {
        this.contactType = contactType;
        return this;
    }

    /**
     * Sets the trusted-contact token for the status protocol.
     *
     * <p>Wrapped into a per-user {@code <tctoken>} child of the {@code <status>}
     * element.
     *
     * @implNote
     * WA Web skips the {@code <tctoken>} entirely when its profile-scraping
     * gating utility returns false; this implementation always attaches the
     * token when present and lets the relay enforce the gate.
     *
     * @param trustedContactToken the raw token bytes
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.withTcToken", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncUser withTrustedContactToken(byte[] trustedContactToken) {
        this.trustedContactToken = trustedContactToken;
        return this;
    }

    /**
     * Returns the canonical JID, when present.
     *
     * @return the canonical JID, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.getId", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Jid> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the LID, when present.
     *
     * @return the LID, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.getLid", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Jid> lid() {
        return Optional.ofNullable(lid);
    }

    /**
     * Returns the phone-number JID, when present.
     *
     * @return the phone-number JID, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.getPnJid", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Jid> phoneJid() {
        return Optional.ofNullable(phoneJid);
    }

    /**
     * Returns the phone number, when present.
     *
     * @return the phone number, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.getPhone", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<String> phoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    /**
     * Returns the cached device-list hash, when present.
     *
     * @return the device-list hash, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.getDeviceHash", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<String> deviceHash() {
        return Optional.ofNullable(deviceHash);
    }

    /**
     * Returns the cache timestamp, when present.
     *
     * @return the cache timestamp, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.getTs", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Returns the expected timestamp, when present.
     *
     * @return the expected timestamp, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.getExpectedTs", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Instant> expectedTimestamp() {
        return Optional.ofNullable(expectedTimestamp);
    }

    /**
     * Returns the persona id, when present.
     *
     * @return the persona id, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.getPersonaId", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<String> personaId() {
        return Optional.ofNullable(personaId);
    }

    /**
     * Returns the username, when present.
     *
     * @return the username, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.getUsername", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    /**
     * Returns the username PIN, when present.
     *
     * @return the username PIN, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.getPin", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<String> pin() {
        return Optional.ofNullable(pin);
    }

    /**
     * Returns the contact-protocol type discriminator, when present.
     *
     * @return the contact-protocol type, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.getType", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<String> contactType() {
        return Optional.ofNullable(contactType);
    }

    /**
     * Returns the trusted-contact token, when present.
     *
     * @return the trusted-contact token, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncUser",
            exports = "USyncUser.getTcToken", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<byte[]> trustedContactToken() {
        return Optional.ofNullable(trustedContactToken);
    }
}
