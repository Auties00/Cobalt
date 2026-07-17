package com.github.auties00.cobalt.calls.signaling.relay;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import com.github.auties00.cobalt.calls.signaling.session.TransportStanza;

/**
 * Represents the parsed {@code <relay>} element of a call signaling message.
 *
 * <p>The {@code <relay>} block tells a device which relay servers a call may use and supplies the
 * tokens and keys to reach them. It appears as a child of several messages, principally the
 * {@link TransportStanza transport} message and the offer acknowledgement relay, and the same parse
 * applies in every position. The block carries relay level attributes ({@code peer_pid},
 * {@code self_pid}, {@code uuid}, {@code joinable}, {@code warp_mi_tag_len}, {@code attribute_padding}),
 * a list of opaque {@code <token>} children, a list of {@code <auth_token id>} credential children, a
 * list of relay {@link RelayEndpoint endpoints}, a {@code <key>} child, an {@code <hbh_key>} child, and
 * a list of {@code <participant>} children.
 *
 * <p>The endpoints arrive under one of two mutually exclusive tags: a list of {@code <te>} endpoints
 * or a list of {@code <te2>} endpoints, never both. A block with neither endpoint list is rejected by
 * the parser. The two endpoint families are kept in one ordered list; each {@link RelayEndpoint}
 * records its own tag through {@link RelayEndpoint#element()}.
 *
 * <p>The token list holds at most {@value #MAX_TOKENS} {@code <token>} children of at most
 * {@value #MAX_TOKEN_LENGTH} bytes each; a block that exceeds either bound is rejected rather than
 * truncated. The {@code <auth_token id>} children carry the per relay authentication credentials a
 * {@code <te2>} endpoint references through its {@code auth_token_id}.
 *
 * <p>The {@code <key>} child is treated as raw bytes and is NOT base64 decoded: the engine forwards
 * its bytes verbatim as the relay authentication password. The {@code <hbh_key>} child is base64
 * decoded into the hop by hop key bytes. The relay endpoints this block parses feed the in memory
 * {@link RelayCandidate} list.
 *
 * @param peerPid          the {@code peer_pid} attribute, or {@code -1} when absent
 * @param selfPid          the {@code self_pid} attribute, or {@code -1} when absent
 * @param uuid             the {@code uuid} attribute, or {@code null} when absent
 * @param joinable         whether the {@code joinable} attribute was set
 * @param warpMiTagLen     the {@code warp_mi_tag_len} attribute, or {@code -1} when absent
 * @param attributePadding the {@code attribute_padding} attribute, or {@code -1} when absent
 * @param authTokens       the {@code <auth_token id>} credential children, in wire order; never
 *                         {@code null}
 * @param tokens           the {@code <token>} children, in wire order; never {@code null}
 * @param endpoints        the {@code <te>} or {@code <te2>} endpoints, in wire order; never
 *                         {@code null}
 * @param participants     the {@code <participant>} children, in wire order; never {@code null}
 * @param key              the raw {@code <key>} child bytes, kept undecoded, or {@code null} when
 *                         absent
 * @param hbhKey           the base64 decoded {@code <hbh_key>} child bytes, or {@code null} when
 *                         absent
 * @see RelayEndpoint
 * @see RelayCandidate
 * @see TransportStanza
 */
public record RelayInfo(int peerPid,
                        int selfPid,
                        String uuid,
                        boolean joinable,
                        int warpMiTagLen,
                        int attributePadding,
                        List<RelayAuthToken> authTokens,
                        List<RelayToken> tokens,
                        List<RelayEndpoint> endpoints,
                        List<RelayParticipant> participants,
                        byte[] key,
                        byte[] hbhKey) {
    /**
     * The wire element tag for a relay block.
     */
    public static final String ELEMENT = "relay";

    /**
     * The maximum number of {@code <token>} children a relay block may carry.
     *
     * <p>A relay block carrying more than this many {@code <token>} children is rejected outright rather
     * than truncated.
     */
    public static final int MAX_TOKENS = 8;

    /**
     * The maximum length of a single {@code <token>} child.
     */
    public static final int MAX_TOKEN_LENGTH = 0x200;

    /**
     * The sentinel value standing in for an absent integer relay attribute.
     */
    private static final int UNSET = -1;

    /**
     * The wire attribute naming the peer participant id.
     */
    private static final String PEER_PID_ATTRIBUTE = "peer_pid";

    /**
     * The wire attribute naming the self participant id.
     */
    private static final String SELF_PID_ATTRIBUTE = "self_pid";

    /**
     * The wire attribute naming the relay block UUID.
     */
    private static final String UUID_ATTRIBUTE = "uuid";

    /**
     * The wire attribute naming the joinable flag.
     */
    private static final String JOINABLE_ATTRIBUTE = "joinable";

    /**
     * The wire attribute naming the WARP message integrity tag length.
     */
    private static final String WARP_MI_TAG_LEN_ATTRIBUTE = "warp_mi_tag_len";

    /**
     * The wire attribute naming the attribute padding value.
     */
    private static final String ATTRIBUTE_PADDING_ATTRIBUTE = "attribute_padding";

    /**
     * The wire element tag for a relay authentication token credential child.
     */
    private static final String AUTH_TOKEN_ELEMENT = "auth_token";

    /**
     * The wire element tag for an opaque relay token.
     */
    private static final String TOKEN_ELEMENT = "token";

    /**
     * The wire attribute naming a token's index.
     */
    private static final String TOKEN_ID_ATTRIBUTE = "id";

    /**
     * The wire element tag for the relay password key.
     */
    private static final String KEY_ELEMENT = "key";

    /**
     * The wire element tag for the hop by hop key.
     */
    private static final String HBH_KEY_ELEMENT = "hbh_key";

    /**
     * The wire element tag for a relay participant.
     */
    private static final String PARTICIPANT_ELEMENT = "participant";

    /**
     * The wire attribute naming a participant's id.
     */
    private static final String PARTICIPANT_PID_ATTRIBUTE = "pid";

    /**
     * The wire attribute naming a participant's JID.
     */
    private static final String PARTICIPANT_JID_ATTRIBUTE = "jid";

    /**
     * The value of a boolean attribute that is set.
     */
    private static final String TRUE_LITERAL = "1";

    /**
     * Canonicalizes the record components, copying the lists immutably and the byte arrays defensively.
     *
     * @throws NullPointerException if {@code authTokens}, {@code tokens}, {@code endpoints}, or
     *                              {@code participants} is {@code null}
     */
    public RelayInfo {
        authTokens = List.copyOf(Objects.requireNonNull(authTokens, "authTokens cannot be null"));
        tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens cannot be null"));
        endpoints = List.copyOf(Objects.requireNonNull(endpoints, "endpoints cannot be null"));
        participants = List.copyOf(Objects.requireNonNull(participants, "participants cannot be null"));
        key = key == null ? null : key.clone();
        hbhKey = hbhKey == null ? null : hbhKey.clone();
    }

    /**
     * Returns the peer participant id, if present.
     *
     * @return an {@link OptionalInt} holding the {@code peer_pid}, or empty when absent
     */
    public OptionalInt peerPidValue() {
        return peerPid == UNSET ? OptionalInt.empty() : OptionalInt.of(peerPid);
    }

    /**
     * Returns the self participant id, if present.
     *
     * @return an {@link OptionalInt} holding the {@code self_pid}, or empty when absent
     */
    public OptionalInt selfPidValue() {
        return selfPid == UNSET ? OptionalInt.empty() : OptionalInt.of(selfPid);
    }

    /**
     * Returns the relay block UUID, if present.
     *
     * @return an {@link Optional} holding the {@code uuid}, or empty when absent
     */
    public Optional<String> uuidValue() {
        return Optional.ofNullable(uuid);
    }

    /**
     * Returns the WARP message integrity tag length, if present.
     *
     * @return an {@link OptionalInt} holding the {@code warp_mi_tag_len}, or empty when absent
     */
    public OptionalInt warpMiTagLenValue() {
        return warpMiTagLen == UNSET ? OptionalInt.empty() : OptionalInt.of(warpMiTagLen);
    }

    /**
     * Returns the attribute padding value, if present.
     *
     * @return an {@link OptionalInt} holding the {@code attribute_padding}, or empty when absent
     */
    public OptionalInt attributePaddingValue() {
        return attributePadding == UNSET ? OptionalInt.empty() : OptionalInt.of(attributePadding);
    }

    /**
     * Returns a defensive copy of the raw {@code <key>} child bytes, if present.
     *
     * <p>The bytes are returned undecoded, exactly as the wire carried them, because the engine
     * forwards them verbatim as the relay authentication password.
     *
     * @return an {@link Optional} holding a copy of the key bytes, or empty when absent
     */
    public Optional<byte[]> keyValue() {
        return key == null ? Optional.empty() : Optional.of(key.clone());
    }

    /**
     * Returns the raw {@code <key>} child bytes backing this block.
     *
     * <p>This accessor overrides the implicit record accessor to return a defensive copy so the stored
     * array cannot be mutated through the returned reference.
     *
     * @return a copy of the undecoded key bytes, or {@code null} when absent
     */
    @Override
    public byte[] key() {
        return key == null ? null : key.clone();
    }

    /**
     * Returns a defensive copy of the decoded {@code <hbh_key>} child bytes, if present.
     *
     * @return an {@link Optional} holding a copy of the hop by hop key bytes, or empty when absent
     */
    public Optional<byte[]> hbhKeyValue() {
        return hbhKey == null ? Optional.empty() : Optional.of(hbhKey.clone());
    }

    /**
     * Returns the decoded {@code <hbh_key>} child bytes backing this block.
     *
     * <p>This accessor overrides the implicit record accessor to return a defensive copy so the stored
     * array cannot be mutated through the returned reference.
     *
     * @return a copy of the hop by hop key bytes, or {@code null} when absent
     */
    @Override
    public byte[] hbhKey() {
        return hbhKey == null ? null : hbhKey.clone();
    }

    /**
     * Builds the {@code <relay>} stanza for this block.
     *
     * <p>Absent integer attributes and the absent string attributes are omitted; {@code joinable} is
     * written only when set. The token list is emitted as {@code <token>} children, the auth token
     * credentials as {@code <auth_token>} children, the endpoints as their {@code <te>} or
     * {@code <te2>} children, and the participants as {@code <participant>} children. The raw key bytes
     * become the {@code <key>} child and the hop by hop key is base64 encoded into the {@code <hbh_key>}
     * child.
     *
     * @return the relay block stanza
     */
    public Stanza toNode() {
        var children = new ArrayList<Stanza>();
        for (var participant : participants) {
            children.add(participant.toNode());
        }
        for (var token : tokens) {
            children.add(token.toNode());
        }
        for (var authToken : authTokens) {
            children.add(authToken.toNode());
        }
        for (var endpoint : endpoints) {
            children.add(endpoint.toNode());
        }
        if (key != null) {
            children.add(new StanzaBuilder()
                    .description(KEY_ELEMENT)
                    .content(key)
                    .build());
        }
        if (hbhKey != null) {
            children.add(new StanzaBuilder()
                    .description(HBH_KEY_ELEMENT)
                    .content(Base64.getEncoder().encodeToString(hbhKey))
                    .build());
        }
        return new StanzaBuilder()
                .description(ELEMENT)
                .attribute(ATTRIBUTE_PADDING_ATTRIBUTE, attributePadding, attributePadding != UNSET)
                .attribute(PEER_PID_ATTRIBUTE, peerPid, peerPid != UNSET)
                .attribute(SELF_PID_ATTRIBUTE, selfPid, selfPid != UNSET)
                .attribute(UUID_ATTRIBUTE, uuid)
                .attribute(JOINABLE_ATTRIBUTE, TRUE_LITERAL, joinable)
                .attribute(WARP_MI_TAG_LEN_ATTRIBUTE, warpMiTagLen, warpMiTagLen != UNSET)
                .content(children)
                .build();
    }

    /**
     * Returns a copy of this relay block with every endpoint's client to relay round trip hints cleared.
     *
     * <p>Maps each endpoint through {@link RelayEndpoint#withoutRoundTripHints()} so encoding the copy omits
     * the {@code c2r_rtt} and {@code max_peer_c2r_rtt} attributes, matching the relay block a callee echoes
     * in its accept; the relay level attributes, tokens, auth tokens, key, hop by hop key, and participants
     * are preserved.
     *
     * @return a copy of this relay block without the per endpoint round trip time hints
     */
    public RelayInfo withoutEndpointRoundTripHints() {
        var strippedEndpoints = endpoints.stream()
                .map(RelayEndpoint::withoutRoundTripHints)
                .toList();
        return new RelayInfo(peerPid, selfPid, uuid, joinable, warpMiTagLen, attributePadding, authTokens, tokens,
                strippedEndpoints, participants, key, hbhKey);
    }

    /**
     * Decodes a {@code <relay>} stanza into a {@link RelayInfo}.
     *
     * <p>The endpoints are read from the {@code <te>} children when present and otherwise from the
     * {@code <te2>} children, enforcing that the two are not both present. A relay block carrying more
     * than {@value #MAX_TOKENS} {@code <token>} children is rejected. A stanza that is not a
     * {@code <relay>} element yields an empty result. The {@code <key>} child is kept as raw bytes; the
     * {@code <hbh_key>} child is base64 decoded.
     *
     * @param stanza the relay block stanza
     * @return the decoded relay block, or an empty result when the stanza is not a {@code <relay>}
     *         element
     * @throws NullPointerException     if {@code stanza} is {@code null}
     * @throws IllegalArgumentException if the relay block carries both {@code <te>} and {@code <te2>}
     *                                  endpoint lists, or carries more than {@value #MAX_TOKENS}
     *                                  {@code <token>} children
     */
    public static Optional<RelayInfo> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription(ELEMENT)) {
            return Optional.empty();
        }
        var peerPid = stanza.getAttributeAsInt(PEER_PID_ATTRIBUTE, UNSET);
        var selfPid = stanza.getAttributeAsInt(SELF_PID_ATTRIBUTE, UNSET);
        var uuid = stanza.getAttributeAsString(UUID_ATTRIBUTE, null);
        var joinable = TRUE_LITERAL.equals(stanza.getAttributeAsString(JOINABLE_ATTRIBUTE, null));
        var warpMiTagLen = stanza.getAttributeAsInt(WARP_MI_TAG_LEN_ATTRIBUTE, UNSET);
        var attributePadding = stanza.getAttributeAsInt(ATTRIBUTE_PADDING_ATTRIBUTE, UNSET);

        var tokens = new ArrayList<RelayToken>();
        var authTokens = new ArrayList<RelayAuthToken>();
        var participants = new ArrayList<RelayParticipant>();
        var primaryEndpoints = new ArrayList<RelayEndpoint>();
        var alternateEndpoints = new ArrayList<RelayEndpoint>();
        byte[] key = null;
        byte[] hbhKey = null;

        for (var child : stanza.children()) {
            switch (child.description()) {
                case TOKEN_ELEMENT -> RelayToken.of(child).ifPresent(tokens::add);
                case AUTH_TOKEN_ELEMENT -> RelayAuthToken.of(child).ifPresent(authTokens::add);
                case PARTICIPANT_ELEMENT -> RelayParticipant.of(child).ifPresent(participants::add);
                case RelayEndpoint.ELEMENT_TE -> RelayEndpoint.of(child).ifPresent(primaryEndpoints::add);
                case RelayEndpoint.ELEMENT_TE2 -> RelayEndpoint.of(child).ifPresent(alternateEndpoints::add);
                case KEY_ELEMENT -> key = readKeyBytes(child);
                case HBH_KEY_ELEMENT -> hbhKey = decodeHbhKey(child);
                default -> {
                }
            }
        }

        if (!primaryEndpoints.isEmpty() && !alternateEndpoints.isEmpty()) {
            throw new IllegalArgumentException("relay block cannot carry both te and te2 endpoints");
        }
        if (tokens.size() > MAX_TOKENS) {
            throw new IllegalArgumentException("relay block carries more than " + MAX_TOKENS + " tokens: " + tokens.size());
        }
        var endpoints = primaryEndpoints.isEmpty() ? alternateEndpoints : primaryEndpoints;

        return Optional.of(new RelayInfo(peerPid, selfPid, uuid, joinable, warpMiTagLen,
                attributePadding, authTokens, tokens, endpoints, participants, key, hbhKey));
    }

    /**
     * Reads the raw {@code <key>} child bytes, preferring binary content and falling back to the ASCII
     * bytes of textual content.
     *
     * @param child the {@code <key>} stanza
     * @return the raw key bytes, or {@code null} when the child carried no content
     */
    private static byte[] readKeyBytes(Stanza child) {
        var bytes = child.toContentBytes().orElse(null);
        if (bytes != null) {
            return bytes;
        }
        return child.toContentString()
                .map(value -> value.getBytes(StandardCharsets.US_ASCII))
                .orElse(null);
    }

    /**
     * Decodes the base64 {@code <hbh_key>} child into the hop by hop key bytes.
     *
     * @param child the {@code <hbh_key>} stanza
     * @return the decoded hop by hop key bytes, or {@code null} when the child carried no content or did
     *         not decode
     */
    private static byte[] decodeHbhKey(Stanza child) {
        var value = child.toContentString().orElse(null);
        if (value == null) {
            return child.toContentBytes().orElse(null);
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException _) {
            return value.getBytes(StandardCharsets.US_ASCII);
        }
    }

    /**
     * Represents one opaque {@code <token id>BYTES</token>} child of a relay block.
     *
     * <p>A relay token is a server issued credential a device presents to a relay; the {@code id}
     * indexes it for reference by a relay endpoint. The bytes are opaque and at most
     * {@value RelayInfo#MAX_TOKEN_LENGTH} long.
     *
     * @param id    the token index
     * @param bytes the opaque token bytes; never {@code null}
     */
    public record RelayToken(int id, byte[] bytes) {
        /**
         * Canonicalizes the record components, validating the token length and defensively copying the
         * bytes.
         *
         * @throws NullPointerException     if {@code bytes} is {@code null}
         * @throws IllegalArgumentException if {@code bytes} exceeds {@value RelayInfo#MAX_TOKEN_LENGTH} bytes
         */
        public RelayToken {
            Objects.requireNonNull(bytes, "bytes cannot be null");
            if (bytes.length > MAX_TOKEN_LENGTH) {
                throw new IllegalArgumentException("relay token exceeds " + MAX_TOKEN_LENGTH + " bytes: " + bytes.length);
            }
            bytes = bytes.clone();
        }

        /**
         * Returns the opaque token bytes backing this token.
         *
         * <p>This accessor overrides the implicit record accessor to return a defensive copy so the
         * stored array cannot be mutated through the returned reference.
         *
         * @return a copy of the token bytes; never {@code null}
         */
        @Override
        public byte[] bytes() {
            return bytes.clone();
        }

        /**
         * Builds the {@code <token id=...>BYTES</token>} stanza for this token.
         *
         * @return the token stanza
         */
        public Stanza toNode() {
            return new StanzaBuilder()
                    .description(TOKEN_ELEMENT)
                    .attribute(TOKEN_ID_ATTRIBUTE, id)
                    .content(bytes)
                    .build();
        }

        /**
         * Decodes a {@code <token>} stanza into a {@link RelayToken}.
         *
         * <p>A stanza that is not a {@code <token>} element, or one without binary content, yields an empty
         * result so callers iterating a mixed child list can skip it.
         *
         * @param stanza the {@code <token>} stanza
         * @return the decoded token, or an empty result when the stanza is not a usable {@code <token>}
         *         element
         */
        public static Optional<RelayToken> of(Stanza stanza) {
            if (stanza == null || !stanza.hasDescription(TOKEN_ELEMENT)) {
                return Optional.empty();
            }
            var bytes = stanza.toContentBytes();
            if (bytes.isEmpty()) {
                return Optional.empty();
            }
            var id = stanza.getAttributeAsInt(TOKEN_ID_ATTRIBUTE, 0);
            return Optional.of(new RelayToken(id, bytes.get()));
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof RelayToken that
                    && this.id == that.id
                    && java.util.Arrays.equals(this.bytes, that.bytes));
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, java.util.Arrays.hashCode(bytes));
        }

        @Override
        public String toString() {
            return "RelayToken[id=" + id + ", len=" + bytes.length + ']';
        }
    }

    /**
     * Represents one {@code <auth_token id>BYTES</auth_token>} credential child of a relay block.
     *
     * <p>A relay authentication token is a server issued per relay credential; the {@code id} indexes
     * it so a {@code <te2>} endpoint can reference it through its {@code auth_token_id}. The bytes are
     * opaque.
     *
     * @param id    the credential index
     * @param bytes the opaque credential bytes; never {@code null}
     */
    public record RelayAuthToken(int id, byte[] bytes) {
        /**
         * Canonicalizes the record components, defensively copying the bytes.
         *
         * @throws NullPointerException if {@code bytes} is {@code null}
         */
        public RelayAuthToken {
            Objects.requireNonNull(bytes, "bytes cannot be null");
            bytes = bytes.clone();
        }

        /**
         * Returns the opaque credential bytes backing this token.
         *
         * <p>This accessor overrides the implicit record accessor to return a defensive copy so the
         * stored array cannot be mutated through the returned reference.
         *
         * @return a copy of the credential bytes; never {@code null}
         */
        @Override
        public byte[] bytes() {
            return bytes.clone();
        }

        /**
         * Builds the {@code <auth_token id=...>BYTES</auth_token>} stanza for this credential.
         *
         * @return the auth token stanza
         */
        public Stanza toNode() {
            return new StanzaBuilder()
                    .description(AUTH_TOKEN_ELEMENT)
                    .attribute(TOKEN_ID_ATTRIBUTE, id)
                    .content(bytes)
                    .build();
        }

        /**
         * Decodes an {@code <auth_token>} stanza into a {@link RelayAuthToken}.
         *
         * <p>A stanza that is not an {@code <auth_token>} element, or one without binary content, yields
         * an empty result so callers iterating a mixed child list can skip it.
         *
         * @param stanza the {@code <auth_token>} stanza
         * @return the decoded credential, or an empty result when the stanza is not a usable
         *         {@code <auth_token>} element
         */
        public static Optional<RelayAuthToken> of(Stanza stanza) {
            if (stanza == null || !stanza.hasDescription(AUTH_TOKEN_ELEMENT)) {
                return Optional.empty();
            }
            var bytes = stanza.toContentBytes();
            if (bytes.isEmpty()) {
                return Optional.empty();
            }
            var id = stanza.getAttributeAsInt(TOKEN_ID_ATTRIBUTE, 0);
            return Optional.of(new RelayAuthToken(id, bytes.get()));
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof RelayAuthToken that
                    && this.id == that.id
                    && java.util.Arrays.equals(this.bytes, that.bytes));
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, java.util.Arrays.hashCode(bytes));
        }

        @Override
        public String toString() {
            return "RelayAuthToken[id=" + id + ", len=" + bytes.length + ']';
        }
    }

    /**
     * Represents one {@code <participant pid jid/>} child of a relay block.
     *
     * <p>A relay participant maps a server assigned participant id to a party's JID. The relay block's
     * {@code self_pid} and {@code peer_pid} reference these ids.
     *
     * @param pid the server assigned participant id
     * @param jid the participant's JID; never {@code null}
     */
    public record RelayParticipant(int pid, Jid jid) {
        /**
         * Canonicalizes the record components.
         *
         * @throws NullPointerException if {@code jid} is {@code null}
         */
        public RelayParticipant {
            Objects.requireNonNull(jid, "jid cannot be null");
        }

        /**
         * Builds the {@code <participant pid=... jid=.../>} stanza for this participant.
         *
         * @return the participant stanza
         */
        public Stanza toNode() {
            return new StanzaBuilder()
                    .description(PARTICIPANT_ELEMENT)
                    .attribute(PARTICIPANT_PID_ATTRIBUTE, pid)
                    .attribute(PARTICIPANT_JID_ATTRIBUTE, jid)
                    .build();
        }

        /**
         * Decodes a {@code <participant>} stanza into a {@link RelayParticipant}.
         *
         * <p>A stanza that is not a {@code <participant>} element, or one without a parseable {@code jid},
         * yields an empty result so callers iterating a mixed child list can skip it.
         *
         * @param stanza the {@code <participant>} stanza
         * @return the decoded participant, or an empty result when the stanza is not a usable
         *         {@code <participant>} element
         */
        public static Optional<RelayParticipant> of(Stanza stanza) {
            if (stanza == null || !stanza.hasDescription(PARTICIPANT_ELEMENT)) {
                return Optional.empty();
            }
            var jid = stanza.getAttributeAsJid(PARTICIPANT_JID_ATTRIBUTE);
            if (jid.isEmpty()) {
                return Optional.empty();
            }
            var pid = stanza.getAttributeAsInt(PARTICIPANT_PID_ATTRIBUTE, 0);
            return Optional.of(new RelayParticipant(pid, jid.get()));
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RelayInfo that
                && this.peerPid == that.peerPid
                && this.selfPid == that.selfPid
                && this.joinable == that.joinable
                && this.warpMiTagLen == that.warpMiTagLen
                && this.attributePadding == that.attributePadding
                && Objects.equals(this.uuid, that.uuid)
                && this.authTokens.equals(that.authTokens)
                && this.tokens.equals(that.tokens)
                && this.endpoints.equals(that.endpoints)
                && this.participants.equals(that.participants)
                && java.util.Arrays.equals(this.key, that.key)
                && java.util.Arrays.equals(this.hbhKey, that.hbhKey));
    }

    @Override
    public int hashCode() {
        return Objects.hash(peerPid, selfPid, uuid, joinable, warpMiTagLen, attributePadding,
                authTokens, tokens, endpoints, participants,
                java.util.Arrays.hashCode(key), java.util.Arrays.hashCode(hbhKey));
    }

    @Override
    public String toString() {
        return "RelayInfo[uuid=" + uuid + ", joinable=" + joinable
                + ", tokens=" + tokens.size() + ", endpoints=" + endpoints.size()
                + ", participants=" + participants.size() + ']';
    }
}
