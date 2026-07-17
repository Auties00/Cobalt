package com.github.auties00.cobalt.wire.stanza.smax.presence;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed family of inbound {@code <presence/>} server-pushed update variants.
 *
 * <p>The relay pushes these after a peer is subscribed through {@link SmaxSubscribeRequest}. A
 * client pattern-matches on the parsed variant to update its local view of peer online state and
 * last-seen timestamps. The permitted variants are {@link GroupAvailable}, {@link GroupUnavailable},
 * {@link LastSeenWithOtherValue}, {@link UserUnavailable}, and {@link Available}; {@link #of(Stanza)}
 * resolves a raw {@link Stanza} into one of them.
 */
@WhatsAppWebModule(moduleName = "WASmaxInPresenceServerUpdateRequest")
@WhatsAppWebModule(moduleName = "WASmaxInPresencePresenceUpdates")
public sealed interface SmaxServerUpdateResponse extends SmaxStanza.Response
        permits SmaxServerUpdateResponse.GroupAvailable, SmaxServerUpdateResponse.GroupUnavailable,
        SmaxServerUpdateResponse.LastSeenWithOtherValue, SmaxServerUpdateResponse.UserUnavailable,
        SmaxServerUpdateResponse.Available {

    /**
     * Resolves an inbound presence stanza into its matching {@link SmaxServerUpdateResponse} variant.
     *
     * <p>The variants are tried in declared order: {@link GroupAvailable} first, then
     * {@link GroupUnavailable}, then {@link LastSeenWithOtherValue}, then {@link UserUnavailable},
     * and finally {@link Available}. The first variant whose own {@code of(Stanza)} factory accepts
     * the stanza wins; the result is empty when no documented variant matches.
     *
     * @implNote
     * This implementation returns {@link Optional#empty()} when no documented variant matches the
     * stanza shape, leaving the caller to route the miss through Cobalt's configurable error
     * handler; WA Web instead raises a disjunction error so its upstream presence promise rejects
     * through a catch block.
     *
     * @param stanza the inbound {@code <presence/>} stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxPresenceServerUpdateRPC",
            exports = "receiveServerUpdateRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInPresencePresenceUpdates",
            exports = "parsePresenceUpdates", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxServerUpdateResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var groupAvailable = GroupAvailable.of(stanza);
        if (groupAvailable.isPresent()) {
            return groupAvailable;
        }
        var groupUnavailable = GroupUnavailable.of(stanza);
        if (groupUnavailable.isPresent()) {
            return groupUnavailable;
        }
        var lastSeenWithOtherValue = LastSeenWithOtherValue.of(stanza);
        if (lastSeenWithOtherValue.isPresent()) {
            return lastSeenWithOtherValue;
        }
        var userUnavailable = UserUnavailable.of(stanza);
        if (userUnavailable.isPresent()) {
            return userUnavailable;
        }
        return Available.of(stanza);
    }

    /**
     * Models the variant reporting how many members of a group are currently online.
     *
     * <p>The {@link #count()} is always positive; a client surfaces it in the group's online-member
     * badge. A group that has dropped to zero online members is reported by
     * {@link GroupUnavailable} instead.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPresenceGroupAvailableMixin")
    final class GroupAvailable implements SmaxServerUpdateResponse {
        /**
         * Holds the group {@link Jid} the count applies to.
         */
        private final Jid from;

        /**
         * Holds the number of currently-online members, constrained to {@code [1, 1024]}.
         *
         * @implNote
         * The {@code 1024} ceiling matches the upper bound of the {@code count} attribute range
         * enforced by WA Web's presence parse utilities.
         */
        private final int count;

        /**
         * Constructs a new {@code GroupAvailable} projection.
         *
         * <p>Invoked by {@link #of(Stanza)} after the stanza passes the group-JID and {@code count}
         * range validation.
         *
         * @param from  the group {@link Jid}; never {@code null}
         * @param count the online-member count
         * @throws NullPointerException if {@code from} is {@code null}
         */
        public GroupAvailable(Jid from, int count) {
            this.from = Objects.requireNonNull(from, "from cannot be null");
            this.count = count;
        }

        /**
         * Returns the group {@link Jid}.
         *
         * @return the group JID; never {@code null}
         */
        public Jid from() {
            return from;
        }

        /**
         * Returns the online-member count.
         *
         * <p>Always within {@code [1, 1024]}; values outside the range cause {@link #of(Stanza)} to
         * reject the stanza.
         *
         * @return the count
         */
        public int count() {
            return count;
        }

        /**
         * Tries to parse a {@link GroupAvailable} variant from an inbound presence stanza.
         *
         * <p>The result is empty when the stanza is not a {@code <presence/>}, when its {@code from}
         * attribute is not a {@code g.us} {@link Jid}, or when {@code count} is missing or outside
         * {@code [1, 1024]}.
         *
         * @param stanza the inbound presence stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPresenceGroupAvailableMixin",
                exports = "parseGroupAvailableMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<GroupAvailable> of(Stanza stanza) {
            if (!stanza.hasDescription("presence")) {
                return Optional.empty();
            }
            var from = stanza.getAttributeAsJid("from").orElse(null);
            if (from == null) {
                return Optional.empty();
            }
            if (!"g.us".equals(from.server().toString())) {
                return Optional.empty();
            }
            var count = stanza.getAttributeAsInt("count");
            if (count.isEmpty()) {
                return Optional.empty();
            }
            if (count.getAsInt() < 1 || count.getAsInt() > 1024) {
                return Optional.empty();
            }
            return Optional.of(new GroupAvailable(from, count.getAsInt()));
        }

        /**
         * Compares this variant with another for value equality.
         *
         * <p>Two instances are equal when both the group {@link Jid} and the count are equal.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} if {@code obj} is an equal {@link GroupAvailable}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (GroupAvailable) obj;
            return this.count == that.count && Objects.equals(this.from, that.from);
        }

        /**
         * Returns a hash code derived from the group {@link Jid} and the count.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(from, count);
        }

        /**
         * Returns a debug string exposing the group {@link Jid} and the count.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxServerUpdateResponse.GroupAvailable[from=" + from
                    + ", count=" + count + ']';
        }
    }

    /**
     * Models the variant reporting a group has dropped to zero online members.
     *
     * <p>A client clears the group's online-member badge on receipt. A group with one or more
     * online members is reported by {@link GroupAvailable} instead.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPresenceGroupUnavailableMixin")
    final class GroupUnavailable implements SmaxServerUpdateResponse {
        /**
         * Holds the group {@link Jid} gone idle.
         */
        private final Jid from;

        /**
         * Constructs a new {@code GroupUnavailable} projection.
         *
         * <p>Invoked by {@link #of(Stanza)} once the stanza is confirmed as a group-scoped
         * {@code type="unavailable"} presence.
         *
         * @param from the group {@link Jid}; never {@code null}
         * @throws NullPointerException if {@code from} is {@code null}
         */
        public GroupUnavailable(Jid from) {
            this.from = Objects.requireNonNull(from, "from cannot be null");
        }

        /**
         * Returns the group {@link Jid}.
         *
         * @return the group JID; never {@code null}
         */
        public Jid from() {
            return from;
        }

        /**
         * Tries to parse a {@link GroupUnavailable} variant from an inbound presence stanza.
         *
         * <p>The result is empty when the {@code from} attribute is not a {@code g.us} {@link Jid}
         * or when {@code type} is not {@code "unavailable"}.
         *
         * @param stanza the inbound presence stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPresenceGroupUnavailableMixin",
                exports = "parseGroupUnavailableMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<GroupUnavailable> of(Stanza stanza) {
            if (!stanza.hasDescription("presence")) {
                return Optional.empty();
            }
            var from = stanza.getAttributeAsJid("from").orElse(null);
            if (from == null) {
                return Optional.empty();
            }
            if (!"g.us".equals(from.server().toString())) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "unavailable")) {
                return Optional.empty();
            }
            return Optional.of(new GroupUnavailable(from));
        }

        /**
         * Compares this variant with another for value equality.
         *
         * <p>Two instances are equal when their group {@link Jid} values are equal.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} if {@code obj} is an equal {@link GroupUnavailable}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (GroupUnavailable) obj;
            return Objects.equals(this.from, that.from);
        }

        /**
         * Returns a hash code derived from the group {@link Jid}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(from);
        }

        /**
         * Returns a debug string exposing the group {@link Jid}.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxServerUpdateResponse.GroupUnavailable[from=" + from + ']';
        }
    }

    /**
     * Models the variant reporting a peer is offline with a privacy-suppressed last-seen sentinel.
     *
     * <p>This surfaces when the peer's privacy settings forbid the subscriber from seeing a real
     * last-seen timestamp, so {@link #last()} carries a sentinel rather than a time value. A peer
     * offline with a real last-seen timestamp is reported by {@link UserUnavailable} instead.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPresenceLastSeenWithOtherValueMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInPresenceEnums")
    final class LastSeenWithOtherValue implements SmaxServerUpdateResponse {
        /**
         * Holds the user {@link Jid} gone offline.
         */
        private final Jid from;

        /**
         * Holds the optional sentinel value: one of {@code "deny"} (privacy block), {@code "error"}
         * (relay-side fault), or {@code "none"} (no last-seen recorded).
         */
        private final String last;

        /**
         * Constructs a new {@code LastSeenWithOtherValue} projection.
         *
         * <p>Invoked by {@link #of(Stanza)} after the stanza passes the user-JID and sentinel-set
         * validation.
         *
         * @param from the user {@link Jid}; never {@code null}
         * @param last the optional sentinel; may be {@code null}
         * @throws NullPointerException if {@code from} is {@code null}
         */
        public LastSeenWithOtherValue(Jid from, String last) {
            this.from = Objects.requireNonNull(from, "from cannot be null");
            this.last = last;
        }

        /**
         * Returns the user {@link Jid}.
         *
         * @return the user JID; never {@code null}
         */
        public Jid from() {
            return from;
        }

        /**
         * Returns the optional sentinel value.
         *
         * <p>Empty when the relay omitted the {@code last} attribute; otherwise one of
         * {@code "deny"}, {@code "error"}, or {@code "none"}.
         *
         * @return an {@link Optional} carrying the sentinel
         */
        public Optional<String> last() {
            return Optional.ofNullable(last);
        }

        /**
         * Tries to parse a {@link LastSeenWithOtherValue} variant from an inbound presence stanza.
         *
         * <p>The result is empty when the {@code from} attribute is not a user {@link Jid} (either
         * {@code lid} or {@code s.whatsapp.net}), when {@code type} is not {@code "unavailable"},
         * or when {@code last} carries a value outside the {@code deny}/{@code error}/{@code none}
         * enum.
         *
         * @param stanza the inbound presence stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPresenceLastSeenWithOtherValueMixin",
                exports = "parseLastSeenWithOtherValueMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<LastSeenWithOtherValue> of(Stanza stanza) {
            if (!stanza.hasDescription("presence")) {
                return Optional.empty();
            }
            var from = stanza.getAttributeAsJid("from").orElse(null);
            if (from == null) {
                return Optional.empty();
            }
            var server = from.server().toString();
            if (!"lid".equals(server) && !"s.whatsapp.net".equals(server)) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "unavailable")) {
                return Optional.empty();
            }
            var last = stanza.getAttributeAsString("last").orElse(null);
            if (last != null && !"deny".equals(last) && !"error".equals(last) && !"none".equals(last)) {
                return Optional.empty();
            }
            return Optional.of(new LastSeenWithOtherValue(from, last));
        }

        /**
         * Compares this variant with another for value equality.
         *
         * <p>Two instances are equal when both the user {@link Jid} and the sentinel are equal.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} if {@code obj} is an equal {@link LastSeenWithOtherValue}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (LastSeenWithOtherValue) obj;
            return Objects.equals(this.from, that.from)
                    && Objects.equals(this.last, that.last);
        }

        /**
         * Returns a hash code derived from the user {@link Jid} and the sentinel.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(from, last);
        }

        /**
         * Returns a debug string exposing the user {@link Jid} and the sentinel.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxServerUpdateResponse.LastSeenWithOtherValue[from=" + from
                    + ", last=" + last + ']';
        }
    }

    /**
     * Models the variant reporting a peer is offline with a free-form last-seen timestamp.
     *
     * <p>This surfaces when the peer's privacy settings permit a real last-seen, so {@link #last()}
     * carries a Unix timestamp as text. A peer offline with a privacy-suppressed sentinel is
     * reported by {@link LastSeenWithOtherValue} instead.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPresenceUserUnavailableMixin")
    final class UserUnavailable implements SmaxServerUpdateResponse {
        /**
         * Holds the user {@link Jid} gone offline.
         */
        private final Jid from;

        /**
         * Holds the optional free-form {@code last} attribute, a Unix timestamp as text.
         */
        private final String last;

        /**
         * Constructs a new {@code UserUnavailable} projection.
         *
         * <p>Invoked by {@link #of(Stanza)} after the stanza passes the user-JID and
         * {@code type="unavailable"} validation.
         *
         * @param from the user {@link Jid}; never {@code null}
         * @param last the optional {@code last} attribute; may be {@code null}
         * @throws NullPointerException if {@code from} is {@code null}
         */
        public UserUnavailable(Jid from, String last) {
            this.from = Objects.requireNonNull(from, "from cannot be null");
            this.last = last;
        }

        /**
         * Returns the user {@link Jid}.
         *
         * @return the user JID; never {@code null}
         */
        public Jid from() {
            return from;
        }

        /**
         * Returns the optional {@code last} value.
         *
         * <p>Empty when the relay omitted the attribute; otherwise a Unix timestamp as text that the
         * caller converts to an {@link java.time.Instant}.
         *
         * @return an {@link Optional} carrying the value
         */
        public Optional<String> last() {
            return Optional.ofNullable(last);
        }

        /**
         * Tries to parse a {@link UserUnavailable} variant from an inbound presence stanza.
         *
         * <p>The result is empty when the {@code from} attribute is not a user {@link Jid} or when
         * {@code type} is not {@code "unavailable"}.
         *
         * @param stanza the inbound presence stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPresenceUserUnavailableMixin",
                exports = "parseUserUnavailableMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<UserUnavailable> of(Stanza stanza) {
            if (!stanza.hasDescription("presence")) {
                return Optional.empty();
            }
            var from = stanza.getAttributeAsJid("from").orElse(null);
            if (from == null) {
                return Optional.empty();
            }
            var server = from.server().toString();
            if (!"lid".equals(server) && !"s.whatsapp.net".equals(server)) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "unavailable")) {
                return Optional.empty();
            }
            var last = stanza.getAttributeAsString("last").orElse(null);
            return Optional.of(new UserUnavailable(from, last));
        }

        /**
         * Compares this variant with another for value equality.
         *
         * <p>Two instances are equal when both the user {@link Jid} and the {@code last} value are
         * equal.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} if {@code obj} is an equal {@link UserUnavailable}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (UserUnavailable) obj;
            return Objects.equals(this.from, that.from)
                    && Objects.equals(this.last, that.last);
        }

        /**
         * Returns a hash code derived from the user {@link Jid} and the {@code last} value.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(from, last);
        }

        /**
         * Returns a debug string exposing the user {@link Jid} and the {@code last} value.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxServerUpdateResponse.UserUnavailable[from=" + from
                    + ", last=" + last + ']';
        }
    }

    /**
     * Models the variant reporting a peer is online.
     *
     * <p>The peer may be either a group or a user. The optional {@link #type()} carries the literal
     * {@code "available"} when present, and the optional {@link #last()} carries the timestamp of
     * the last activity that triggered the presence push.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPresenceAvailableMixin")
    final class Available implements SmaxServerUpdateResponse {
        /**
         * Holds the peer {@link Jid}, either a group or a user.
         */
        private final Jid from;

        /**
         * Holds the optional literal {@code "available"} type tag.
         */
        private final String type;

        /**
         * Holds the optional free-form {@code last} attribute.
         */
        private final String last;

        /**
         * Constructs a new {@code Available} projection.
         *
         * <p>Invoked by {@link #of(Stanza)} once the stanza passes the peer-JID and optional
         * {@code type="available"} validation.
         *
         * @param from the peer {@link Jid}; never {@code null}
         * @param type the optional literal {@code "available"}; may be {@code null}
         * @param last the optional {@code last} attribute; may be {@code null}
         * @throws NullPointerException if {@code from} is {@code null}
         */
        public Available(Jid from, String type, String last) {
            this.from = Objects.requireNonNull(from, "from cannot be null");
            this.type = type;
            this.last = last;
        }

        /**
         * Returns the peer {@link Jid}.
         *
         * @return the JID; never {@code null}
         */
        public Jid from() {
            return from;
        }

        /**
         * Returns the optional {@code type} attribute.
         *
         * <p>Empty when the relay omitted it; otherwise the literal {@code "available"}.
         *
         * @return an {@link Optional} carrying the type
         */
        public Optional<String> type() {
            return Optional.ofNullable(type);
        }

        /**
         * Returns the optional {@code last} value.
         *
         * <p>Empty when the relay omitted the attribute; otherwise a Unix timestamp as text.
         *
         * @return an {@link Optional} carrying the value
         */
        public Optional<String> last() {
            return Optional.ofNullable(last);
        }

        /**
         * Tries to parse an {@link Available} variant from an inbound presence stanza.
         *
         * <p>The result is empty when the {@code from} attribute is not a peer {@link Jid} (group or
         * user) or when {@code type} carries a non-{@code "available"} literal.
         *
         * @param stanza the inbound presence stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPresenceAvailableMixin",
                exports = "parseAvailableMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Available> of(Stanza stanza) {
            if (!stanza.hasDescription("presence")) {
                return Optional.empty();
            }
            var from = stanza.getAttributeAsJid("from").orElse(null);
            if (from == null) {
                return Optional.empty();
            }
            var server = from.server().toString();
            if (!"g.us".equals(server)
                    && !"lid".equals(server)
                    && !"s.whatsapp.net".equals(server)) {
                return Optional.empty();
            }
            var type = stanza.getAttributeAsString("type").orElse(null);
            if (type != null && !"available".equals(type)) {
                return Optional.empty();
            }
            var last = stanza.getAttributeAsString("last").orElse(null);
            return Optional.of(new Available(from, type, last));
        }

        /**
         * Compares this variant with another for value equality.
         *
         * <p>Two instances are equal when the peer {@link Jid}, the type, and the {@code last} value
         * are all equal.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} if {@code obj} is an equal {@link Available}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Available) obj;
            return Objects.equals(this.from, that.from)
                    && Objects.equals(this.type, that.type)
                    && Objects.equals(this.last, that.last);
        }

        /**
         * Returns a hash code derived from the peer {@link Jid}, the type, and the {@code last}
         * value.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(from, type, last);
        }

        /**
         * Returns a debug string exposing the peer {@link Jid}, the type, and the {@code last}
         * value.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxServerUpdateResponse.Available[from=" + from
                    + ", type=" + type
                    + ", last=" + last + ']';
        }
    }
}
