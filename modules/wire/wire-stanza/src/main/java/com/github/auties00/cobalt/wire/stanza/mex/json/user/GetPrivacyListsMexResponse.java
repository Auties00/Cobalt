package com.github.auties00.cobalt.wire.stanza.mex.json.user;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;

/**
 * Decodes the reply to the privacy contact list fetch query.
 *
 * <p>The reply collapses the {@code privacy_contact_list} projection of the first returned user into
 * a flat ({@link #dhash()}, {@link #contacts()}) view; the outer array always has length one because
 * the request only emits a single {@code query_input} entry. The {@code dhash} value is the
 * server-side digest of the returned list and drives subsequent delta refreshes. Consume this type
 * after dispatching {@link GetPrivacyListsMexRequest}.
 *
 * @see GetPrivacyListsMexRequest
 */
@WhatsAppWebModule(moduleName = "WAWebMexGetPrivacyList")
public final class GetPrivacyListsMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the server-side digest of the list, possibly {@code null}.
     */
    private final String dhash;

    /**
     * Holds the decoded contact entries, never {@code null}.
     */
    private final List<PrivacyContact> contacts;

    /**
     * Wraps the decoded fields of a privacy-list response.
     *
     * @param dhash the {@code dhash} digest of the list
     * @param contacts the decoded contact entries; {@code null} is coerced to an empty list
     */
    private GetPrivacyListsMexResponse(String dhash, List<PrivacyContact> contacts) {
        this.dhash = dhash;
        this.contacts = contacts == null ? List.of() : contacts;
    }

    /**
     * Decodes the {@code <result>} child of an inbound MEX IQ.
     *
     * <p>Pass the IQ stanza received in reply to a stanza dispatched with
     * {@link GetPrivacyListsMexRequest#toStanza()}.
     *
     * @param stanza the IQ reply stanza
     * @return the decoded reply, or {@link Optional#empty()} when the payload is missing or does not
     *         carry a {@code privacy_contact_list} projection for the first user
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGetPrivacyList", exports = "fetchPrivacyList",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<GetPrivacyListsMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(GetPrivacyListsMexResponse::parse);
    }

    /**
     * Decodes the {@code <result>} payload bytes into a {@link GetPrivacyListsMexResponse}.
     *
     * <p>The payload is projected from the {@code privacy_contact_list} of the first
     * {@code xwa2_fetch_wa_users} entry. Missing intermediate envelope nodes yield
     * {@link Optional#empty()}.
     *
     * @implNote This implementation traps the {@link JSON#parseObject(byte[])} exception to honour
     * the {@link Optional#empty()} contract advertised by {@link #of(Stanza)} when the bytes are not
     * valid JSON.
     *
     * @param payload the raw {@code <result>} payload bytes
     * @return the decoded reply, or {@link Optional#empty()} when the payload does not parse or lacks
     *         the required envelope
     */
    private static Optional<GetPrivacyListsMexResponse> parse(byte[] payload) {
        JSONObject envelope;
        try {
            envelope = JSON.parseObject(payload);
        } catch (RuntimeException _) {
            return Optional.empty();
        }
        if (envelope == null) {
            return Optional.empty();
        }
        var users = envelope.getJSONArray("xwa2_fetch_wa_users");
        if (users == null || users.isEmpty()) {
            return Optional.empty();
        }
        var user = users.getJSONObject(0);
        if (user == null) {
            return Optional.empty();
        }
        var list = user.getJSONObject("privacy_contact_list");
        if (list == null) {
            return Optional.empty();
        }
        var dhash = list.getString("dhash");
        var rawContacts = list.getJSONArray("contacts");
        List<PrivacyContact> contacts;
        if (rawContacts == null || rawContacts.isEmpty()) {
            contacts = List.of();
        } else {
            contacts = new ArrayList<>(rawContacts.size());
            for (var i = 0; i < rawContacts.size(); i++) {
                var entry = rawContacts.getJSONObject(i);
                if (entry == null) {
                    continue;
                }
                contacts.add(PrivacyContact.parse(entry));
            }
        }
        return Optional.of(new GetPrivacyListsMexResponse(dhash, contacts));
    }

    /**
     * Returns the server-side digest of the returned list.
     *
     * <p>Pass this value as the {@code dhash} of a subsequent {@link GetPrivacyListsMexRequest} to
     * receive a delta refresh against the same list.
     *
     * @return the digest wrapped in an {@link Optional}, or {@link Optional#empty()} when the relay
     *         omitted the field
     */
    public Optional<String> dhash() {
        return Optional.ofNullable(dhash);
    }

    /**
     * Returns the decoded contact entries.
     *
     * <p>The returned view is unmodifiable; mutation attempts throw
     * {@link UnsupportedOperationException}.
     *
     * @return the contact entries; may be empty, never {@code null}
     */
    public SequencedCollection<PrivacyContact> contacts() {
        return Collections.unmodifiableSequencedCollection(contacts);
    }

    /**
     * Wraps a single contact entry inside a privacy contact list response.
     *
     * <p>The {@link #pnJid()} alternate form is present only when the contact has a phone-number
     * addressed identifier distinct from {@link #jid()}, typically a LID primary. The
     * {@link #username()} is present only when the contact has claimed a WhatsApp username.
     */
    @WhatsAppWebModule(moduleName = "WAWebMexGetPrivacyList")
    public static final class PrivacyContact {
        /**
         * Holds the {@code jid} field carrying the primary contact identifier.
         */
        private final Jid jid;

        /**
         * Holds the {@code pn_jid} alternate identifier, possibly {@code null}.
         */
        private final Jid pnJid;

        /**
         * Holds the {@code username_info.username} field, possibly {@code null}.
         */
        private final String username;

        /**
         * Constructs a privacy contact entry from its decoded fields.
         *
         * @param jid the primary contact JID
         * @param pnJid the phone-number addressed alternate JID, or {@code null} when the contact has
         *              only the primary form
         * @param username the claimed WhatsApp username, or {@code null} when the contact has not
         *                 claimed one
         * @throws NullPointerException if {@code jid} is {@code null}
         */
        public PrivacyContact(Jid jid, Jid pnJid, String username) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
            this.pnJid = pnJid;
            this.username = username;
        }

        /**
         * Decodes a single contact entry from a {@link JSONObject}.
         *
         * <p>Invoked by {@link #parse(byte[])} while walking the {@code contacts} array.
         *
         * @param entry the JSON object to decode
         * @return the decoded entry
         */
        private static PrivacyContact parse(JSONObject entry) {
            var rawJid = entry.getString("jid");
            var rawPnJid = entry.getString("pn_jid");
            var usernameInfo = entry.getJSONObject("username_info");
            var username = usernameInfo == null ? null : usernameInfo.getString("username");
            return new PrivacyContact(
                    Jid.of(rawJid),
                    rawPnJid == null ? null : Jid.of(rawPnJid),
                    username);
        }

        /**
         * Returns the primary contact identifier.
         *
         * @return the primary {@link Jid}, never {@code null}
         */
        public Jid jid() {
            return jid;
        }

        /**
         * Returns the phone-number addressed alternate identifier.
         *
         * @return the alternate {@link Jid} wrapped in an {@link Optional}, or
         *         {@link Optional#empty()} when the contact has only the primary form
         */
        public Optional<Jid> pnJid() {
            return Optional.ofNullable(pnJid);
        }

        /**
         * Returns the claimed WhatsApp username for this contact.
         *
         * @return the username wrapped in an {@link Optional}, or {@link Optional#empty()} when the
         *         contact has not claimed one
         */
        public Optional<String> username() {
            return Optional.ofNullable(username);
        }
    }
}
