package com.github.auties00.cobalt.wire.stanza.smax.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqErrorResponseMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Discriminates an inbound reply to a {@link SmaxGetContactBlacklistRequest} into one of three shapes.
 *
 * <p>The three variants cover the LID-addressed success ({@link SuccessLID}), the PN-addressed success
 * ({@link Success}), and a generic error ({@link Error}).
 *
 * @implNote This implementation preserves the parser priority order in {@link #of(Stanza, Stanza)}: the LID variant
 * is tried first so it catches the migrated wire shape before the PN arm's lenient {@code addressing_mode}
 * matching would otherwise accept it.
 */
public sealed interface SmaxGetContactBlacklistResponse extends SmaxStanza.Response
        permits SmaxGetContactBlacklistResponse.SuccessLID, SmaxGetContactBlacklistResponse.Success, SmaxGetContactBlacklistResponse.Error {

    /**
     * Dispatches the inbound stanza onto the matching variant.
     *
     * <p>An empty result signals that no documented parser arm matched the stanza.
     *
     * @param stanza    the inbound {@code <iq>} stanza; never {@code null}
     * @param request the original {@link SmaxGetContactBlacklistRequest} stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxPrivacyGetContactBlacklistRPC",
            exports = "sendGetContactBlacklistRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGetContactBlacklistResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var successLid = SuccessLID.of(stanza, request);
        if (successLid.isPresent()) {
            return successLid;
        }
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        return Error.of(stanza, request);
    }

    /**
     * Describes one {@code <user/>} entry of a LID-addressed success reply.
     *
     * <p>The {@link #contactListId()} discriminator is mandatory whereas the LID itself is optional; when the LID
     * is absent the consumer recovers it from the discriminator's {@link SmaxGetContactBlacklistContactListId.PnJid}
     * arm.
     */
    final class LidUser {
        /**
         * The LID JID of the entry, or {@code null} when the relay omitted it.
         */
        private final Jid jid;

        /**
         * The mandatory contact-list-id discriminator.
         */
        private final SmaxGetContactBlacklistContactListId contactListId;

        /**
         * Constructs a LID-addressed user entry.
         *
         * @param jid           the LID JID echoed by the relay, or {@code null} when absent
         * @param contactListId the discriminator projecting the auxiliary attributes; never {@code null}
         * @throws NullPointerException if {@code contactListId} is {@code null}
         */
        public LidUser(Jid jid, SmaxGetContactBlacklistContactListId contactListId) {
            this.jid = jid;
            this.contactListId = Objects.requireNonNull(contactListId, "contactListId cannot be null");
        }

        /**
         * Returns the LID JID when present.
         *
         * <p>An absent JID cues the consumer to recover the LID by resolving the entry's
         * {@link SmaxGetContactBlacklistContactListId.PnJid} arm; if neither path yields a LID the entry is
         * dropped.
         *
         * @return an {@link Optional} carrying the LID JID, or empty when the relay omitted it
         */
        public Optional<Jid> jid() {
            return Optional.ofNullable(jid);
        }

        /**
         * Returns the contact-list-id discriminator.
         *
         * @return the discriminator; never {@code null}
         */
        public SmaxGetContactBlacklistContactListId contactListId() {
            return contactListId;
        }

        /**
         * Compares this entry with another for equality by JID and discriminator.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link LidUser}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (LidUser) obj;
            return Objects.equals(this.jid, that.jid)
                    && Objects.equals(this.contactListId, that.contactListId);
        }

        /**
         * Returns a hash code derived from the JID and discriminator.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(jid, contactListId);
        }

        /**
         * Returns a debug representation carrying the JID and discriminator.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetContactBlacklistResponse.LidUser[jid=" + jid
                    + ", contactListId=" + contactListId + ']';
        }
    }

    /**
     * Describes one {@code <user/>} entry of a PN-addressed success reply.
     *
     * <p>The PN JID is mandatory; the optional {@link #lid()} echo lets the consumer eagerly prime the LID-to-PN
     * mapping store before migration completes.
     */
    final class PnUser {
        /**
         * The required phone-number JID of the entry.
         */
        private final Jid jid;

        /**
         * The optional LID echo when the relay has already migrated the entry, or {@code null}.
         */
        private final Jid lid;

        /**
         * Constructs a PN-addressed user entry.
         *
         * @param jid the PN JID; never {@code null}
         * @param lid the optional LID echo; may be {@code null}
         * @throws NullPointerException if {@code jid} is {@code null}
         */
        public PnUser(Jid jid, Jid lid) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
            this.lid = lid;
        }

        /**
         * Returns the phone-number JID.
         *
         * @return the PN JID; never {@code null}
         */
        public Jid jid() {
            return jid;
        }

        /**
         * Returns the LID echo when present.
         *
         * <p>Used to opportunistically prime the LID-to-PN mapping cache so subsequent LID-addressed lookups for
         * the same contact resolve without an extra round-trip.
         *
         * @return an {@link Optional} carrying the LID JID, or empty when the relay omitted it
         */
        public Optional<Jid> lid() {
            return Optional.ofNullable(lid);
        }

        /**
         * Compares this entry with another for equality by PN JID and LID echo.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link PnUser}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (PnUser) obj;
            return Objects.equals(this.jid, that.jid)
                    && Objects.equals(this.lid, that.lid);
        }

        /**
         * Returns a hash code derived from the PN JID and LID echo.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(jid, lid);
        }

        /**
         * Returns a debug representation carrying the PN JID and LID echo.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetContactBlacklistResponse.PnUser[jid=" + jid
                    + ", lid=" + lid + ']';
        }
    }

    /**
     * Validates the IQ-result envelope and extracts the inner {@code <privacy/>} child.
     *
     * <p>Shared by both success variants ({@link SuccessLID} and {@link Success}); the error variant uses
     * {@link SmaxIqErrorResponseMixin} instead.
     *
     * @param stanza    the inbound stanza
     * @param request the original outbound request
     * @return an {@link Optional} carrying the {@code <privacy/>} child, or empty when the envelope check fails
     *         or the child is missing
     */
    private static Optional<Stanza> validateSuccessEnvelope(Stanza stanza, Stanza request) {
        if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
            return Optional.empty();
        }
        return stanza.getChild("privacy");
    }

    /**
     * Parses a {@code <user/>} child into a {@link SmaxGetContactBlacklistContactListId} discriminator.
     *
     * <p>Shared by the LID-addressed parser; the PN variant does not run the discriminator because PN entries
     * carry the {@code jid} and {@code lid} attributes directly.
     *
     * @implNote This implementation mirrors the disjunction priority (username first, then PN JID, then fall
     * through to {@link SmaxGetContactBlacklistContactListId.Empty}); the empty arm is the structural default and
     * no user-visible failure mode arises from an unrecognised wire shape.
     *
     * @param userStanza the {@code <user/>} child stanza
     * @return the parsed discriminator; never {@code null}
     */
    private static SmaxGetContactBlacklistContactListId parseContactListId(Stanza userStanza) {
        var username = userStanza.getAttributeAsString("username").orElse(null);
        if (username != null) {
            return new SmaxGetContactBlacklistContactListId.Username(username);
        }
        var pnJid = userStanza.getAttributeAsString("pn_jid")
                .map(Jid::of)
                .orElse(null);
        if (pnJid != null) {
            return new SmaxGetContactBlacklistContactListId.PnJid(pnJid);
        }
        return new SmaxGetContactBlacklistContactListId.Empty();
    }

    /**
     * Carries the LID-addressed success disallowed-list reply.
     *
     * <p>The consumer pre-seeds the LID-to-PN mapping cache from {@link SmaxGetContactBlacklistContactListId.PnJid}
     * arms and primes the username cache from {@link SmaxGetContactBlacklistContactListId.Username} arms before
     * rendering the contact list.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPrivacyGetContactBlacklistResponseSuccessLID")
    @WhatsAppWebModule(moduleName = "WASmaxInPrivacyDeprecatedIQResultResponseOptionalFromMixin")
    final class SuccessLID implements SmaxGetContactBlacklistResponse {
        /**
         * The list-side digest when the relay emitted a {@code <list/>} child, or {@code null}.
         */
        private final String listDhash;

        /**
         * The parsed LID-addressed user entries; empty when the relay omitted the {@code <list/>} child entirely.
         */
        private final List<LidUser> users;

        /**
         * Constructs a LID-addressed success reply, defensively copying the user list.
         *
         * @param listDhash the optional list digest; may be {@code null}
         * @param users     the parsed user entries; never {@code null}
         * @throws NullPointerException if {@code users} is {@code null}
         */
        public SuccessLID(String listDhash, List<LidUser> users) {
            this.listDhash = listDhash;
            this.users = List.copyOf(Objects.requireNonNull(users, "users cannot be null"));
        }

        /**
         * Returns the list digest when present.
         *
         * <p>Absent when the relay omitted the {@code <list/>} child entirely, indicating the user has no
         * disallowed-list entries for the requested category.
         *
         * @return an {@link Optional} carrying the digest, or empty when the relay omitted the list body
         */
        public Optional<String> listDhash() {
            return Optional.ofNullable(listDhash);
        }

        /**
         * Returns the parsed LID-addressed user entries.
         *
         * @return an unmodifiable list of users; never {@code null}
         */
        public List<LidUser> users() {
            return users;
        }

        /**
         * Parses a LID-addressed success variant.
         *
         * <p>The result is empty when the envelope is wrong, when {@code addressing_mode} is not {@code "lid"},
         * or when the {@code <list/>} child is present but the required {@code dhash} attribute is missing. A
         * {@code <privacy/>} body without a {@code <list/>} child is folded into an empty-users success rather
         * than rejected, mirroring the caller which treats it as a no-disallowed-list state.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPrivacyGetContactBlacklistResponseSuccessLID",
                exports = "parseGetContactBlacklistResponseSuccessLID",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessLID> of(Stanza stanza, Stanza request) {
            var privacy = validateSuccessEnvelope(stanza, request).orElse(null);
            if (privacy == null) {
                return Optional.empty();
            }
            if (!privacy.hasAttribute("addressing_mode", "lid")) {
                return Optional.empty();
            }
            var list = privacy.getChild("list").orElse(null);
            if (list == null) {
                return Optional.of(new SuccessLID(null, List.of()));
            }
            var dhash = list.getAttributeAsString("dhash").orElse(null);
            if (dhash == null) {
                return Optional.empty();
            }
            var users = new ArrayList<LidUser>();
            for (var child : list.getChildren("user")) {
                var jid = child.getAttributeAsString("jid")
                        .map(Jid::of)
                        .orElse(null);
                var contactListId = parseContactListId(child);
                users.add(new LidUser(jid, contactListId));
            }
            return Optional.of(new SuccessLID(dhash, Collections.unmodifiableList(users)));
        }

        /**
         * Compares this reply with another for equality by digest and users.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link SuccessLID}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (SuccessLID) obj;
            return Objects.equals(this.listDhash, that.listDhash)
                    && Objects.equals(this.users, that.users);
        }

        /**
         * Returns a hash code derived from the digest and users.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(listDhash, users);
        }

        /**
         * Returns a debug representation carrying the digest and users.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetContactBlacklistResponse.SuccessLID[listDhash=" + listDhash
                    + ", users=" + users + ']';
        }
    }

    /**
     * Carries the legacy phone-number-addressed success disallowed-list reply.
     *
     * <p>Surfaces when the relay returns a PN-addressed disallowed-list, the historical default. It is rare in
     * production now that LID migration is the default; consumers fold it into the same UI surface as
     * {@link SuccessLID} via the optional LID echo on each {@link PnUser}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPrivacyGetContactBlacklistResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInPrivacyDeprecatedIQResultResponseOptionalFromMixin")
    final class Success implements SmaxGetContactBlacklistResponse {
        /**
         * The list-side digest when the relay emitted a {@code <list/>} child, or {@code null}.
         */
        private final String listDhash;

        /**
         * The parsed PN-addressed user entries; empty when the relay omitted the {@code <list/>} child entirely.
         */
        private final List<PnUser> users;

        /**
         * Constructs a PN-addressed success reply, defensively copying the user list.
         *
         * @param listDhash the optional list digest; may be {@code null}
         * @param users     the parsed user entries; never {@code null}
         * @throws NullPointerException if {@code users} is {@code null}
         */
        public Success(String listDhash, List<PnUser> users) {
            this.listDhash = listDhash;
            this.users = List.copyOf(Objects.requireNonNull(users, "users cannot be null"));
        }

        /**
         * Returns the list digest when present.
         *
         * @return an {@link Optional} carrying the digest, or empty when the relay omitted the list body
         */
        public Optional<String> listDhash() {
            return Optional.ofNullable(listDhash);
        }

        /**
         * Returns the parsed PN-addressed user entries.
         *
         * @return an unmodifiable list of users; never {@code null}
         */
        public List<PnUser> users() {
            return users;
        }

        /**
         * Parses a PN-addressed success variant.
         *
         * <p>The result is empty when the envelope is wrong, when {@code addressing_mode} is set to anything
         * other than {@code "pn"}, when a {@code <list/>} child is present but lacks {@code dhash}, or when any
         * {@code <user/>} child lacks a {@code jid} attribute. The {@code addressing_mode} attribute is tolerated
         * either absent or explicitly {@code "pn"}.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPrivacyGetContactBlacklistResponseSuccess",
                exports = "parseGetContactBlacklistResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            var privacy = validateSuccessEnvelope(stanza, request).orElse(null);
            if (privacy == null) {
                return Optional.empty();
            }
            var addressingMode = privacy.getAttributeAsString("addressing_mode").orElse(null);
            if (addressingMode != null && !addressingMode.equals("pn")) {
                return Optional.empty();
            }
            var list = privacy.getChild("list").orElse(null);
            if (list == null) {
                return Optional.of(new Success(null, List.of()));
            }
            var dhash = list.getAttributeAsString("dhash").orElse(null);
            if (dhash == null) {
                return Optional.empty();
            }
            var users = new ArrayList<PnUser>();
            for (var child : list.getChildren("user")) {
                var jid = child.getAttributeAsString("jid")
                        .map(Jid::of)
                        .orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var lid = child.getAttributeAsString("lid")
                        .map(Jid::of)
                        .orElse(null);
                users.add(new PnUser(jid, lid));
            }
            return Optional.of(new Success(dhash, Collections.unmodifiableList(users)));
        }

        /**
         * Compares this reply with another for equality by digest and users.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link Success}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.listDhash, that.listDhash)
                    && Objects.equals(this.users, that.users);
        }

        /**
         * Returns a hash code derived from the digest and users.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(listDhash, users);
        }

        /**
         * Returns a debug representation carrying the digest and users.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetContactBlacklistResponse.Success[listDhash=" + listDhash
                    + ", users=" + users + ']';
        }
    }

    /**
     * Carries the {@code <iq type="error">} reply covering every documented error shape produced by the relay.
     *
     * <p>The caller treats this as a fatal fetch failure; Cobalt surfaces it as a typed response so the caller
     * chooses the retry policy.
     *
     * @implNote This implementation collapses the per-shape WA Web disjunction (bad request, feature not
     * implemented, service unavailable, rate over limit, internal server error) to the universal
     * {@code (errorCode, errorText)} pair because the per-shape payload is empty.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPrivacyGetContactBlacklistResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInPrivacyGetPrivacyListError")
    @WhatsAppWebModule(moduleName = "WASmaxInPrivacyDeprecatedIQErrorResponseOptionalFromMixin")
    final class Error implements SmaxGetContactBlacklistResponse {
        /**
         * The numeric server-side error code.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text.
         */
        private final String errorText;

        /**
         * Constructs an error reply.
         *
         * @param errorCode the numeric error code echoed by the relay
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public Error(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text when present.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses an error variant.
         *
         * <p>Delegates the envelope check to {@link SmaxIqErrorResponseMixin#validate(Stanza, Stanza)} and the
         * payload extraction to {@link SmaxIqErrorResponseMixin#parseError(Stanza)}.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPrivacyGetContactBlacklistResponseError",
                exports = "parseGetContactBlacklistResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Error> of(Stanza stanza, Stanza request) {
            if (!SmaxIqErrorResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var envelope = SmaxIqErrorResponseMixin.parseError(stanza).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new Error(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with another for equality by error code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link Error}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Error) obj;
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from the error code and text.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation carrying the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetContactBlacklistResponse.Error[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
