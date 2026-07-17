package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the MEX response of the fetch-newsletter-pending-invites query built by
 * {@link FetchNewsletterPendingInvitesMexRequest}.
 *
 * <p>Exposes the {@code pending_admin_invites} list and the newsletter {@code id} echoed under
 * {@code xwa2_newsletter_admin}; each {@link PendingAdminInvites} carries one invited user whose
 * admin invite has not yet been accepted.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterPendingInvitesJob")
public final class FetchNewsletterPendingInvitesMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the list of pending admin invites.
     */
    private final List<PendingAdminInvites> pendingAdminInvites;

    /**
     * Holds the newsletter Jid string echoed back by the server.
     */
    private final String id;

    /**
     * Constructs a response wrapping the parsed pending invites and the echoed newsletter id.
     *
     * @param pendingAdminInvites the pending admin invites
     * @param id                  the newsletter Jid string
     */
    private FetchNewsletterPendingInvitesMexResponse(List<PendingAdminInvites> pendingAdminInvites, String id) {
        this.pendingAdminInvites = pendingAdminInvites;
        this.id = id;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser; the returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_admin} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result payload
     */
    public static Optional<FetchNewsletterPendingInvitesMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchNewsletterPendingInvitesMexResponse::of);
    }

    /**
     * Returns the pending admin invites.
     *
     * @return the parsed entries, empty when the relay returned none
     */
    public List<PendingAdminInvites> pendingAdminInvites() {
        return pendingAdminInvites;
    }

    /**
     * Returns the newsletter Jid string echoed back by the server.
     *
     * @return the newsletter id, or empty when the relay omitted the field
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Wraps one pending admin invite.
     *
     * <p>Carries only the invited {@link User} sub-object; WA Web maps each user to a {@code Wid} via
     * the user's phone number or id.
     */
    public static final class PendingAdminInvites {
        /**
         * Holds the invited user sub-object.
         */
        private final User user;

        /**
         * Constructs a pending-invite wrapper from the parsed sub-fields.
         *
         * @param user the invited user sub-object
         */
        private PendingAdminInvites(User user) {
            this.user = user;
        }

        /**
         * Returns the invited user sub-object.
         *
         * @return the parsed {@link User}, or empty when the relay omitted the field
         */
        public Optional<User> user() {
            return Optional.ofNullable(user);
        }

        /**
         * Wraps the invited {@code user} sub-object.
         *
         * <p>Carries the user's phone number string ({@code pn}) and Jid string ({@code id}); WA Web
         * prefers {@code pn} when present and falls back to {@code id} otherwise to construct the
         * destination Wid.
         */
        public static final class User {
            /**
             * Holds the user phone number string.
             */
            private final String pn;

            /**
             * Holds the user Jid string.
             */
            private final String id;

            /**
             * Constructs a user wrapper from the parsed sub-fields.
             *
             * @param pn the user phone number string
             * @param id the user Jid string
             */
            private User(String pn, String id) {
                this.pn = pn;
                this.id = id;
            }

            /**
             * Returns the user phone number string.
             *
             * @return the phone number, or empty when the relay omitted the field
             */
            public Optional<String> pn() {
                return Optional.ofNullable(pn);
            }

            /**
             * Returns the user Jid string.
             *
             * @return the Jid string, or empty when the relay omitted the field
             */
            public Optional<String> id() {
                return Optional.ofNullable(id);
            }

            /**
             * Parses a {@link User} from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed entry, or empty when {@code obj} is {@code null}
             */
            static Optional<User> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var pn = obj.getString("pn");
                var id = obj.getString("id");
                return Optional.of(new User(pn, id));
            }

            /**
             * Parses a list of {@link User} entries from the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<User> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<User>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a {@link PendingAdminInvites} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed entry, or empty when {@code obj} is {@code null}
         */
        static Optional<PendingAdminInvites> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var user = User.of(obj.getJSONObject("user")).orElse(null);
            return Optional.of(new PendingAdminInvites(user));
        }

        /**
         * Parses a list of {@link PendingAdminInvites} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<PendingAdminInvites> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<PendingAdminInvites>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Parses the response from the raw UTF-8 JSON payload of the {@code <result>} child.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletter_admin} root
     */
    private static Optional<FetchNewsletterPendingInvitesMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_admin");
        if (root == null) {
            return Optional.empty();
        }

        var pendingAdminInvites = PendingAdminInvites.ofArray(root.getJSONArray("pending_admin_invites"));
        var id = root.getString("id");

        return Optional.of(new FetchNewsletterPendingInvitesMexResponse(pendingAdminInvites, id));
    }
}
