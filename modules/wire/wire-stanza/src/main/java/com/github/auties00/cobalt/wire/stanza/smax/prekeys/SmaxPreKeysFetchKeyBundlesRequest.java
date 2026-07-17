package com.github.auties00.cobalt.wire.stanza.smax.prekeys;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds the outbound {@code <iq xmlns="encrypt" type="get">} stanza that asks the relay for one or more user pre-key bundles.
 *
 * <p>The relay returns the registration id, identity key, signed pre-key, and an optional
 * unsigned pre-key per user; with these the Signal key-fetch path can seed an outbound session
 * for a chat fan-out. Setting {@link UserKeyRequest#hasUserReasonIdentity()} to {@code true} for a
 * user also asks the relay to attach the device-identity attestation so the bundle can be verified
 * against the claimed account.
 *
 * @implNote
 * This implementation collapses the {@code WASmaxOutPreKeysClientRequestMixin} envelope shaping
 * (xmlns/to/type wiring) and the per-user {@code <user jid reason?/>} child construction into a
 * single {@link #toStanza()} pass; the JS layer routes the same data through three separate mixin
 * functions.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutPreKeysFetchKeyBundlesRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutPreKeysClientRequestMixin")
public final class SmaxPreKeysFetchKeyBundlesRequest implements SmaxStanza.Request {
    /**
     * Holds the per-user fetch entries that appear as {@code <user>} children of the request.
     */
    private final List<UserKeyRequest> users;

    /**
     * Constructs a request for the given list of users.
     *
     * <p>The relay rejects empty requests, so an empty list is refused early. The list is
     * defensively copied so the constructed value is immutable.
     *
     * @param users the per-user fetch entries
     * @throws NullPointerException     if {@code users} is {@code null}
     * @throws IllegalArgumentException if {@code users} is empty
     */
    public SmaxPreKeysFetchKeyBundlesRequest(List<UserKeyRequest> users) {
        Objects.requireNonNull(users, "users cannot be null");
        if (users.isEmpty()) {
            throw new IllegalArgumentException("users cannot be empty");
        }
        this.users = List.copyOf(users);
    }

    /**
     * Returns the list of users carried by this request.
     *
     * <p>The returned list is unmodifiable so callers cannot mutate it after construction.
     *
     * @return an unmodifiable {@link List} of {@link UserKeyRequest}
     */
    public List<UserKeyRequest> users() {
        return users;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation hard-codes {@code xmlns="encrypt"}, {@code type="get"}, and
     * {@code to=s.whatsapp.net} per the
     * {@code WASmaxOutPreKeysFetchKeyBundlesRequest.makeFetchKeyBundlesRequest} fixture, then nests
     * one {@code <user jid reason?/>} per entry under a single {@code <key>} child.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutPreKeysFetchKeyBundlesRequest",
            exports = "makeFetchKeyBundlesRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var userNodes = new ArrayList<Stanza>(users.size());
        for (var user : users) {
            var userBuilder = new StanzaBuilder()
                    .description("user")
                    .attribute("jid", user.userJid());
            if (user.hasUserReasonIdentity()) {
                userBuilder.attribute("reason", "identity");
            }
            userNodes.add(userBuilder.build());
        }
        var keyNode = new StanzaBuilder()
                .description("key")
                .content(userNodes)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "encrypt")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(keyNode);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Two requests are equal when their {@link #users} lists are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxPreKeysFetchKeyBundlesRequest) obj;
        return Objects.equals(this.users, that.users);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Hashes the carried {@link #users} list to stay consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(users);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "SmaxPreKeysFetchKeyBundlesRequest[users=" + users + ']';
    }

    /**
     * Models a per-user entry in the outbound {@code <key>} payload.
     *
     * <p>Pairs a target user {@link Jid} with the optional {@code reason="identity"} hint. When the
     * hint is set, the relay attaches the device-identity attestation so the resulting pre-key
     * bundle can be verified for authenticity.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutPreKeysFetchKeyBundlesRequest")
    public static final class UserKeyRequest {
        /**
         * Holds the target user {@link Jid} whose pre-key bundle is being requested.
         */
        private final Jid userJid;

        /**
         * Records whether {@code reason="identity"} is set on the {@code <user>} child.
         */
        private final boolean hasUserReasonIdentity;

        /**
         * Constructs a per-user request entry.
         *
         * <p>Set {@code hasUserReasonIdentity} to {@code true} when the relay must include the
         * device-identity attestation, typically for first contact with a previously unknown
         * device; for normal re-keying it can stay {@code false}.
         *
         * @param userJid               the target user {@link Jid}
         * @param hasUserReasonIdentity whether to set the identity-reason hint
         * @throws NullPointerException if {@code userJid} is {@code null}
         */
        public UserKeyRequest(Jid userJid, boolean hasUserReasonIdentity) {
            this.userJid = Objects.requireNonNull(userJid, "userJid cannot be null");
            this.hasUserReasonIdentity = hasUserReasonIdentity;
        }

        /**
         * Returns the target user {@link Jid}.
         *
         * <p>Populates the {@code jid} attribute of the corresponding {@code <user>} child in
         * {@link SmaxPreKeysFetchKeyBundlesRequest#toStanza()}.
         *
         * @return the user {@link Jid}
         */
        public Jid userJid() {
            return userJid;
        }

        /**
         * Returns whether the identity-reason hint is set.
         *
         * <p>Decides whether {@link SmaxPreKeysFetchKeyBundlesRequest#toStanza()} emits the
         * {@code reason="identity"} attribute on the {@code <user>} child.
         *
         * @return {@code true} when the hint is set
         */
        public boolean hasUserReasonIdentity() {
            return hasUserReasonIdentity;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Two entries are equal when both their {@link #userJid} and their
         * {@link #hasUserReasonIdentity} flag are equal.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (UserKeyRequest) obj;
            return this.hasUserReasonIdentity == that.hasUserReasonIdentity
                    && Objects.equals(this.userJid, that.userJid);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Hashes both fields to stay consistent with {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(userJid, hasUserReasonIdentity);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxPreKeysFetchKeyBundlesRequest.UserKeyRequest[userJid=" + userJid
                    + ", hasUserReasonIdentity=" + hasUserReasonIdentity + ']';
        }
    }
}
