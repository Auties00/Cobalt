package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the sealed family of inbound reply variants for a
 * {@link SmaxNewslettersMyAddOnsRequest}.
 * Consumers pattern-match on the three permitted variants
 * ({@link Success}, {@link ClientError}, {@link ServerError}); only
 * {@link Success#blocks()} carries the user's per-message own-reaction
 * and own-poll-vote add-ons, while the two error variants describe a
 * relay-side rejection or failure.
 */
public sealed interface SmaxNewslettersMyAddOnsResponse extends SmaxStanza.Response
        permits SmaxNewslettersMyAddOnsResponse.Success, SmaxNewslettersMyAddOnsResponse.ClientError, SmaxNewslettersMyAddOnsResponse.ServerError {

    /**
     * Dispatches the inbound IQ stanza to the first matching variant parser.
     * Tries {@link Success#of(Stanza, Stanza)}, then
     * {@link ClientError#of(Stanza, Stanza)}, then
     * {@link ServerError#of(Stanza, Stanza)}, in that order, and returns the
     * first variant that matches the stanza shape.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza, used to validate echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxNewslettersMyAddOnsRPC",
            exports = "sendMyAddOnsRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxNewslettersMyAddOnsResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Represents the variant that carries the user's per-newsletter
     * add-on list.
     * Each {@link NewsletterBlock} is keyed by newsletter JID and carries
     * a list of {@link MessageAddOns} entries; consumers project
     * {@link #blocks()} onto the local own-reaction and own-poll-vote
     * caches.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersMyAddOnsResponseSuccess")
    final class Success implements SmaxNewslettersMyAddOnsResponse {
        /**
         * Holds the per-newsletter blocks returned by the relay.
         */
        private final List<NewsletterBlock> blocks;

        /**
         * Constructs a new successful reply.
         * An empty {@code blocks} list is permitted and signals the user
         * has no add-ons on the queried newsletters.
         *
         * @param blocks the per-newsletter blocks; never {@code null} (empty allowed)
         */
        public Success(List<NewsletterBlock> blocks) {
            this.blocks = List.copyOf(Objects.requireNonNullElse(blocks, List.of()));
        }

        /**
         * Returns the per-newsletter blocks.
         *
         * @return an unmodifiable {@link List} of blocks; never {@code null}
         */
        public List<NewsletterBlock> blocks() {
            return blocks;
        }

        /**
         * Parses a {@link Success} from the inbound stanza.
         * Returns {@link Optional#empty()} when the IQ envelope fails
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, the
         * {@code <my_addons>} envelope is missing, or any
         * {@code <messages>} block fails its own
         * {@link NewsletterBlock#of(Stanza)} parse.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersMyAddOnsResponseSuccess",
                exports = "parseMyAddOnsResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var myAddOns = stanza.getChild("my_addons").orElse(null);
            if (myAddOns == null) {
                return Optional.empty();
            }
            var blocks = new ArrayList<NewsletterBlock>();
            for (var messagesNode : myAddOns.getChildren("messages")) {
                var block = NewsletterBlock.of(messagesNode).orElse(null);
                if (block == null) {
                    return Optional.empty();
                }
                blocks.add(block);
            }
            return Optional.of(new Success(blocks));
        }

        /**
         * Compares two replies for value equality on {@link #blocks()}.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link Success} carrying equal {@link #blocks()}
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
            return Objects.equals(this.blocks, that.blocks);
        }

        /**
         * Returns the hash code derived from {@link #blocks()}.
         *
         * @return the hash of the {@link #blocks()} {@link List}
         */
        @Override
        public int hashCode() {
            return Objects.hash(blocks);
        }

        /**
         * Returns a debug representation including the blocks.
         *
         * @return a record-like rendering of this reply
         */
        @Override
        public String toString() {
            return "SmaxNewslettersMyAddOnsResponse.Success[blocks=" + blocks + ']';
        }

        /**
         * Represents one per-newsletter block of add-ons.
         * Pairs a newsletter {@link Jid} with the list of
         * {@link MessageAddOns} the user has on that newsletter's
         * messages, looked up by {@code (newsletterJid, serverId)}.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInNewslettersMyAddOnsResponseSuccess")
        public static final class NewsletterBlock {
            /**
             * Holds the newsletter {@link Jid} for this block.
             */
            private final Jid newsletterJid;

            /**
             * Holds the per-message add-on entries belonging to this
             * newsletter.
             */
            private final List<MessageAddOns> messages;

            /**
             * Constructs a new block.
             * An empty {@code messages} list is permitted and signals the
             * user has no add-ons on this specific newsletter.
             *
             * @param newsletterJid the newsletter {@link Jid}; never {@code null}
             * @param messages      the message entries; never {@code null} (empty allowed)
             * @throws NullPointerException if {@code newsletterJid} is {@code null}
             */
            public NewsletterBlock(Jid newsletterJid, List<MessageAddOns> messages) {
                this.newsletterJid = Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
                this.messages = List.copyOf(Objects.requireNonNullElse(messages, List.of()));
            }

            /**
             * Returns the newsletter {@link Jid} for this block.
             *
             * @return the newsletter {@link Jid}; never {@code null}
             */
            public Jid newsletterJid() {
                return newsletterJid;
            }

            /**
             * Returns the per-message add-on entries.
             *
             * @return an unmodifiable {@link List}; never {@code null}
             */
            public List<MessageAddOns> messages() {
                return messages;
            }

            /**
             * Parses a {@link NewsletterBlock} from a {@code <messages>}
             * {@link Stanza}.
             * Returns {@link Optional#empty()} when the description is not
             * {@code messages}, the {@code jid} attribute is missing, or
             * any nested {@code <message>} fails its own
             * {@link MessageAddOns#of(Stanza)} parse.
             *
             * @param messagesStanza the source {@link Stanza}; never {@code null}
             * @return an {@link Optional} carrying the parsed block, or empty on no-match
             */
            @WhatsAppWebExport(moduleName = "WASmaxInNewslettersMyAddOnsResponseSuccess",
                    exports = "parseMyAddOnsResponseSuccessMyAddonsMessages",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<NewsletterBlock> of(Stanza messagesStanza) {
                if (!messagesStanza.hasDescription("messages")) {
                    return Optional.empty();
                }
                var jid = messagesStanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var messages = new ArrayList<MessageAddOns>();
                for (var messageNode : messagesStanza.getChildren("message")) {
                    var entry = MessageAddOns.of(messageNode).orElse(null);
                    if (entry == null) {
                        return Optional.empty();
                    }
                    messages.add(entry);
                }
                return Optional.of(new NewsletterBlock(jid, messages));
            }

            /**
             * Compares two blocks for value equality on both fields.
             *
             * @param obj the reference object to compare against
             * @return {@code true} when {@code obj} is a {@link NewsletterBlock} with equal {@link #newsletterJid()} and {@link #messages()}
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (NewsletterBlock) obj;
                return Objects.equals(this.newsletterJid, that.newsletterJid)
                        && Objects.equals(this.messages, that.messages);
            }

            /**
             * Returns the hash code derived from both fields.
             *
             * @return the combined hash of {@link #newsletterJid()} and {@link #messages()}
             */
            @Override
            public int hashCode() {
                return Objects.hash(newsletterJid, messages);
            }

            /**
             * Returns a debug representation including both fields.
             *
             * @return a record-like rendering of this block
             */
            @Override
            public String toString() {
                return "SmaxNewslettersMyAddOnsResponse.Success.NewsletterBlock[newsletterJid="
                        + newsletterJid + ", messages=" + messages + ']';
            }
        }

        /**
         * Represents one per-message bundle of own add-ons.
         * Pairs a message server-id with the user's own reaction and
         * own-poll-vote projections, when present; either or both may be
         * {@link Optional#empty()}.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterMessageMyAddOnsMixin")
        public static final class MessageAddOns {
            /**
             * Holds the server-assigned message id within the newsletter.
             */
            private final long serverId;

            /**
             * Holds the optional own-reaction projection.
             */
            private final MyReaction reaction;

            /**
             * Holds the optional own-poll-vote projection.
             */
            private final MyPollVote pollVote;

            /**
             * Constructs a new add-on bundle.
             * Passing {@code null} for either projection indicates the
             * relay did not echo that add-on for the message.
             *
             * @param serverId the server-assigned message id
             * @param reaction the optional own-reaction; may be {@code null}
             * @param pollVote the optional own-poll-vote; may be {@code null}
             */
            public MessageAddOns(long serverId, MyReaction reaction, MyPollVote pollVote) {
                this.serverId = serverId;
                this.reaction = reaction;
                this.pollVote = pollVote;
            }

            /**
             * Returns the server-assigned message id.
             *
             * @return the server-assigned id
             */
            public long serverId() {
                return serverId;
            }

            /**
             * Returns the optional own-reaction projection.
             *
             * @return an {@link Optional} carrying the reaction, or empty when the relay omitted it
             */
            public Optional<MyReaction> reaction() {
                return Optional.ofNullable(reaction);
            }

            /**
             * Returns the optional own-poll-vote projection.
             *
             * @return an {@link Optional} carrying the poll vote, or empty when the relay omitted it
             */
            public Optional<MyPollVote> pollVote() {
                return Optional.ofNullable(pollVote);
            }

            /**
             * Parses a {@link MessageAddOns} from a {@code <message>}
             * {@link Stanza}.
             * Returns {@link Optional#empty()} when the description is not
             * {@code message} or the {@code server_id} is missing or
             * outside the {@code [99, 2147476647]} range; both
             * {@link MyReaction} and {@link MyPollVote} child parses fall
             * back to {@link Optional#empty()} when absent.
             *
             * @param messageStanza the source {@link Stanza}; never {@code null}
             * @return an {@link Optional} carrying the parsed entry, or empty on no-match
             */
            @WhatsAppWebExport(moduleName = "WASmaxInNewslettersNewsletterMessageMyAddOnsMixin",
                    exports = "parseNewsletterMessageMyAddOnsMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<MessageAddOns> of(Stanza messageStanza) {
                if (!messageStanza.hasDescription("message")) {
                    return Optional.empty();
                }
                var serverIdOpt = messageStanza.getAttributeAsLong("server_id");
                if (serverIdOpt.isEmpty()) {
                    return Optional.empty();
                }
                var serverId = serverIdOpt.getAsLong();
                if (serverId < 99 || serverId > 2147476647L) {
                    return Optional.empty();
                }
                var reaction = MyReaction.of(messageStanza).orElse(null);
                var pollVote = MyPollVote.of(messageStanza).orElse(null);
                return Optional.of(new MessageAddOns(serverId, reaction, pollVote));
            }

            /**
             * Compares two bundles for value equality on every field.
             *
             * @param obj the reference object to compare against
             * @return {@code true} when {@code obj} is a {@link MessageAddOns} with equal field values
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (MessageAddOns) obj;
                return this.serverId == that.serverId
                        && Objects.equals(this.reaction, that.reaction)
                        && Objects.equals(this.pollVote, that.pollVote);
            }

            /**
             * Returns the hash code derived from every field.
             *
             * @return the combined hash of every field
             */
            @Override
            public int hashCode() {
                return Objects.hash(serverId, reaction, pollVote);
            }

            /**
             * Returns a debug representation including every field.
             *
             * @return a record-like rendering of this entry
             */
            @Override
            public String toString() {
                return "SmaxNewslettersMyAddOnsResponse.Success.MessageAddOns[serverId="
                        + serverId + ", reaction=" + reaction + ", pollVote=" + pollVote + ']';
            }
        }

        /**
         * Represents the user's own reaction on a newsletter message.
         * Materialises the {@code <reaction code t>} child as a typed
         * pair of emoji code and placement timestamp.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterMyReactionMixin")
        public static final class MyReaction {
            /**
             * Holds the emoji code chosen by the user.
             */
            private final String code;

            /**
             * Holds the unix-second timestamp when the reaction was placed.
             */
            private final long timestamp;

            /**
             * Constructs a new own-reaction projection.
             *
             * @param code      the emoji code; never {@code null}
             * @param timestamp the unix-second timestamp
             * @throws NullPointerException if {@code code} is {@code null}
             */
            public MyReaction(String code, long timestamp) {
                this.code = Objects.requireNonNull(code, "code cannot be null");
                this.timestamp = timestamp;
            }

            /**
             * Returns the emoji code.
             *
             * @return the emoji code; never {@code null}
             */
            public String code() {
                return code;
            }

            /**
             * Returns the unix-second timestamp.
             *
             * @return the timestamp
             */
            public long timestamp() {
                return timestamp;
            }

            /**
             * Parses a {@link MyReaction} from a parent {@code <message>}
             * {@link Stanza}.
             * Returns {@link Optional#empty()} when the {@code <reaction>}
             * child is absent, the {@code code} attribute is missing, or
             * the {@code t} attribute is missing or negative.
             *
             * @param messageStanza the parent {@link Stanza}; never {@code null}
             * @return an {@link Optional} carrying the parsed reaction, or empty when absent or malformed
             */
            @WhatsAppWebExport(moduleName = "WASmaxInNewslettersNewsletterMyReactionMixin",
                    exports = "parseNewsletterMyReactionMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<MyReaction> of(Stanza messageStanza) {
                var reactionNode = messageStanza.getChild("reaction").orElse(null);
                if (reactionNode == null) {
                    return Optional.empty();
                }
                var code = reactionNode.getAttributeAsString("code").orElse(null);
                if (code == null) {
                    return Optional.empty();
                }
                var tOpt = reactionNode.getAttributeAsLong("t");
                if (tOpt.isEmpty()) {
                    return Optional.empty();
                }
                var t = tOpt.getAsLong();
                if (t < 0) {
                    return Optional.empty();
                }
                return Optional.of(new MyReaction(code, t));
            }

            /**
             * Compares two reactions for value equality on both fields.
             *
             * @param obj the reference object to compare against
             * @return {@code true} when {@code obj} is a {@link MyReaction} with equal {@link #code()} and {@link #timestamp()}
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (MyReaction) obj;
                return this.timestamp == that.timestamp
                        && Objects.equals(this.code, that.code);
            }

            /**
             * Returns the hash code derived from both fields.
             *
             * @return the combined hash of {@link #code()} and {@link #timestamp()}
             */
            @Override
            public int hashCode() {
                return Objects.hash(code, timestamp);
            }

            /**
             * Returns a debug representation including both fields.
             *
             * @return a record-like rendering of this reaction
             */
            @Override
            public String toString() {
                return "SmaxNewslettersMyAddOnsResponse.Success.MyReaction[code="
                        + code + ", timestamp=" + timestamp + ']';
            }
        }

        /**
         * Represents the user's own poll-vote projection on a newsletter
         * question message.
         * Materialises the {@code <votes t><vote>*</votes>} block where
         * each {@code <vote/>} child carries an opaque 32-byte option id
         * as its content bytes; lengths other than 32 are rejected.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterMyPollVoteMixin")
        public static final class MyPollVote {
            /**
             * Holds the unix-second timestamp when the vote was cast.
             */
            private final long timestamp;

            /**
             * Holds the list of opaque 32-byte option ids the user
             * selected.
             */
            private final List<byte[]> votes;

            /**
             * Constructs a new own-poll-vote projection.
             * Each byte array in {@code votes} is expected to be exactly
             * 32 bytes long; the parser fails upstream otherwise.
             *
             * @param timestamp the unix-second timestamp
             * @param votes     the option ids; never {@code null} (empty allowed)
             */
            public MyPollVote(long timestamp, List<byte[]> votes) {
                this.timestamp = timestamp;
                this.votes = List.copyOf(Objects.requireNonNullElse(votes, List.of()));
            }

            /**
             * Returns the unix-second timestamp.
             *
             * @return the timestamp
             */
            public long timestamp() {
                return timestamp;
            }

            /**
             * Returns the option ids the user selected.
             *
             * @return an unmodifiable {@link List} of 32-byte arrays; never {@code null}
             */
            public List<byte[]> votes() {
                return votes;
            }

            /**
             * Parses a {@link MyPollVote} from a parent {@code <message>}
             * {@link Stanza}.
             * Returns {@link Optional#empty()} when the {@code <votes>}
             * child is absent, the {@code t} attribute is missing or
             * negative, or any {@code <vote/>} content is not exactly 32
             * bytes.
             *
             * @param messageStanza the parent {@link Stanza}; never {@code null}
             * @return an {@link Optional} carrying the parsed vote block, or empty when absent or malformed
             */
            @WhatsAppWebExport(moduleName = "WASmaxInNewslettersNewsletterMyPollVoteMixin",
                    exports = "parseNewsletterMyPollVoteMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<MyPollVote> of(Stanza messageStanza) {
                var votesNode = messageStanza.getChild("votes").orElse(null);
                if (votesNode == null) {
                    return Optional.empty();
                }
                var tOpt = votesNode.getAttributeAsLong("t");
                if (tOpt.isEmpty()) {
                    return Optional.empty();
                }
                var t = tOpt.getAsLong();
                if (t < 0) {
                    return Optional.empty();
                }
                var voteIds = new ArrayList<byte[]>();
                for (var voteNode : votesNode.getChildren("vote")) {
                    var content = voteNode.toContentBytes().orElse(null);
                    if (content == null || content.length != 32) {
                        return Optional.empty();
                    }
                    voteIds.add(content);
                }
                return Optional.of(new MyPollVote(t, voteIds));
            }

            /**
             * Compares two vote blocks for value equality on every field.
             *
             * @implNote This implementation compares {@link #votes()} element-wise via {@link Arrays#equals(byte[], byte[])} because {@link List#equals(Object)} would compare the raw byte arrays by reference rather than by content.
             * @param obj the reference object to compare against
             * @return {@code true} when {@code obj} is a {@link MyPollVote} with equal {@link #timestamp()} and element-wise-equal {@link #votes()}
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (MyPollVote) obj;
                if (this.timestamp != that.timestamp) {
                    return false;
                }
                if (this.votes.size() != that.votes.size()) {
                    return false;
                }
                for (var i = 0; i < this.votes.size(); i++) {
                    if (!Arrays.equals(this.votes.get(i), that.votes.get(i))) {
                        return false;
                    }
                }
                return true;
            }

            /**
             * Returns the hash code derived from {@link #timestamp()} and
             * the element-wise {@link Arrays#hashCode(byte[])} of each
             * vote.
             *
             * @return the combined hash
             */
            @Override
            public int hashCode() {
                var result = Long.hashCode(timestamp);
                for (var vote : votes) {
                    result = 31 * result + Arrays.hashCode(vote);
                }
                return result;
            }

            /**
             * Returns a debug representation showing the timestamp and
             * vote count.
             *
             * @return a record-like rendering of this vote block, truncated to the vote count for brevity
             */
            @Override
            public String toString() {
                return "SmaxNewslettersMyAddOnsResponse.Success.MyPollVote[timestamp="
                        + timestamp + ", votes=" + votes.size() + ']';
            }
        }
    }

    /**
     * Represents the variant carrying a relay-side client-rejection.
     * Every documented sub-error name collapses onto the same numeric
     * {@link #errorCode()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersMyAddOnsResponseClientError")
    final class ClientError implements SmaxNewslettersMyAddOnsResponse {
        /**
         * Holds the numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the relay.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply.
         * The text is optional because not every sub-error carries a
         * human-readable message.
         *
         * @param errorCode the numeric error code echoed by the relay
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code echoed by the relay
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a {@link ClientError} from the inbound stanza by
         * delegating to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersMyAddOnsResponseClientError",
                exports = "parseMyAddOnsResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares two replies for value equality on both fields.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link ClientError} with equal {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns the hash code derived from both fields.
         *
         * @return the combined hash of {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation including both fields.
         *
         * @return a record-like rendering of this reply
         */
        @Override
        public String toString() {
            return "SmaxNewslettersMyAddOnsResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Represents the variant carrying a transient relay-side failure.
     * Mirrors {@link ClientError} for relay-side internal failures; a
     * consuming layer may retry through its helper.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersMyAddOnsResponseServerError")
    final class ServerError implements SmaxNewslettersMyAddOnsResponse {
        /**
         * Holds the numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the relay.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply.
         * The text is optional because not every sub-error carries a
         * human-readable message.
         *
         * @param errorCode the numeric error code echoed by the relay
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code echoed by the relay
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a {@link ServerError} from the inbound stanza by
         * delegating to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersMyAddOnsResponseServerError",
                exports = "parseMyAddOnsResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares two replies for value equality on both fields.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link ServerError} with equal {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns the hash code derived from both fields.
         *
         * @return the combined hash of {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation including both fields.
         *
         * @return a record-like rendering of this reply
         */
        @Override
        public String toString() {
            return "SmaxNewslettersMyAddOnsResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
