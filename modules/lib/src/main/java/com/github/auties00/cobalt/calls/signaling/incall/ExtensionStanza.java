package com.github.auties00.cobalt.calls.signaling.incall;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents an {@code <extension>} in call action: the addition or removal of an end to end call
 * extension.
 *
 * <p>A call extension is an additional E2E secured participant (such as a bot session) attached to an
 * ongoing call. The same {@code <extension>} element serves both the add and remove flows,
 * distinguished only by the message type that carries it; this type records that distinction in
 * {@link #removal()}. The element carries the universal call header, an {@code extension_id} naming the
 * extension and a {@code scope} naming its domain, plus the cryptographic material that establishes the
 * extension's E2E identity: the creator's JID, the creator's identity public key, the signature over
 * that identity key, a session identifier, a connection {@code state} ({@code connected} or
 * {@code terminated}), an {@code outgoing} direction flag, and an opaque token. Every field beyond the
 * common header and {@code extension_id} is optional and omitted when absent.
 *
 * <p>On the wire the element is:
 * {@snippet lang = xml:
 * <extension call-id="..." call-creator="..." extension_id="..." scope="..."
 *            extension_creator="..." extension_creator_identity_key="..."
 *            identity_key_signature="..." session="..." state="connected|terminated"
 *            outgoing="1" token="..."/>
 *}
 *
 * <p>The identity key, signature, and token are carried as raw binary stanza attributes. The
 * {@code call-id} and {@code call-creator} attributes form the common call header shared by every
 * in call action. An AddExtension is carried by {@link SignalingType#ADD_EXTENSION} and a
 * RemoveExtension by {@link SignalingType#REMOVE_EXTENSION}.
 *
 * @see SignalingType#ADD_EXTENSION
 * @see SignalingType#REMOVE_EXTENSION
 */
public final class ExtensionStanza implements InCallActionStanza {
    /**
     * The wire element tag for an extension action.
     */
    public static final String ELEMENT = "extension";

    /**
     * The wire attribute naming the extension identifier.
     */
    private static final String EXTENSION_ID_ATTRIBUTE = "extension_id";

    /**
     * The wire attribute naming the extension scope.
     */
    private static final String SCOPE_ATTRIBUTE = "scope";

    /**
     * The wire attribute naming the extension creator's JID.
     */
    private static final String CREATOR_ATTRIBUTE = "extension_creator";

    /**
     * The wire attribute naming the extension creator's identity public key.
     */
    private static final String IDENTITY_KEY_ATTRIBUTE = "extension_creator_identity_key";

    /**
     * The wire attribute naming the signature over the identity key.
     */
    private static final String IDENTITY_KEY_SIGNATURE_ATTRIBUTE = "identity_key_signature";

    /**
     * The wire attribute naming the extension session identifier.
     */
    private static final String SESSION_ATTRIBUTE = "session";

    /**
     * The wire attribute naming the extension connection state.
     */
    private static final String STATE_ATTRIBUTE = "state";

    /**
     * The wire attribute flagging an outgoing extension.
     */
    private static final String OUTGOING_ATTRIBUTE = "outgoing";

    /**
     * The wire attribute naming the opaque extension token.
     */
    private static final String TOKEN_ATTRIBUTE = "token";

    /**
     * The wire literal for a set ({@code true}) voip boolean flag.
     */
    private static final String FLAG_TRUE = "1";

    /**
     * The wire literal for a clear ({@code false}) voip boolean flag.
     */
    private static final String FLAG_FALSE = "0";

    /**
     * Enumerates the connection states an extension reports through its {@code state} attribute.
     */
    public enum State {
        /**
         * The extension session is established and connected.
         */
        CONNECTED("connected"),

        /**
         * The extension session has been terminated.
         */
        TERMINATED("terminated");

        /**
         * Resolves a wire literal to its state, backing {@link #ofWire(String)}.
         *
         * <p>Built once at class initialization from each constant's {@link #wire}, so a literal resolves
         * to its state in constant time rather than by scanning {@link #values()}. Keys are the raw
         * literals, so matching is case sensitive, preserving the {@link String#equals(Object)} semantics
         * this lookup replaces.
         */
        private static final Map<String, State> BY_WIRE;

        static {
            var byWire = new HashMap<String, State>();
            for (var state : values()) {
                if (byWire.put(state.wire, state) != null) {
                    throw new AssertionError("Conflict");
                }
            }
            BY_WIRE = Map.copyOf(byWire);
        }

        /**
         * The wire literal for this state.
         */
        private final String wire;

        /**
         * Constructs a state bound to its wire literal.
         *
         * @param wire the wire literal for this state
         */
        State(String wire) {
            this.wire = wire;
        }

        /**
         * Returns the wire literal for this state.
         *
         * @return the wire literal
         */
        public String wire() {
            return wire;
        }

        /**
         * Returns the state whose wire literal equals the given value.
         *
         * @implNote This implementation resolves through the prebuilt {@link #BY_WIRE} map rather than
         * scanning {@link #values()}; a {@code null} literal maps to no entry and yields an empty result.
         * @param wire the wire literal to resolve
         * @return an {@link Optional} holding the matching state, or empty when no state matches
         */
        public static Optional<State> ofWire(String wire) {
            return Optional.ofNullable(BY_WIRE.get(wire));
        }
    }

    /**
     * The call identifier; never {@code null}.
     */
    private final String callId;

    /**
     * The call creator's device JID; never {@code null}.
     */
    private final Jid callCreator;

    /**
     * {@code true} when this is a RemoveExtension action, {@code false} when an AddExtension action.
     */
    private final boolean removal;

    /**
     * The extension identifier; never {@code null}.
     */
    private final String extensionId;

    /**
     * The extension scope, or {@code null} when absent.
     */
    private final String scope;

    /**
     * The JID of the participant that created the extension, or {@code null} when absent.
     */
    private final Jid extensionCreator;

    /**
     * The extension creator's identity public key, or {@code null} when absent.
     */
    private final byte[] identityKey;

    /**
     * The signature over the identity key, or {@code null} when absent.
     */
    private final byte[] identityKeySignature;

    /**
     * The extension session identifier, or {@code null} when absent.
     */
    private final String session;

    /**
     * The extension connection state, or {@code null} when absent.
     */
    private final State state;

    /**
     * {@code true} when the extension is outgoing.
     */
    private final boolean outgoing;

    /**
     * The opaque extension token, or {@code null} when absent.
     */
    private final byte[] token;

    /**
     * Constructs an extension action, canonicalizing the components and defensively copying the binary
     * fields.
     *
     * @param callId               the call identifier; never {@code null}
     * @param callCreator          the call creator's device JID; never {@code null}
     * @param removal              {@code true} when this is a RemoveExtension action, {@code false} when an
     *                             AddExtension action
     * @param extensionId          the extension identifier; never {@code null}
     * @param scope                the extension scope, or {@code null} when absent
     * @param extensionCreator     the JID of the participant that created the extension, or {@code null}
     *                             when absent
     * @param identityKey          the extension creator's identity public key, or {@code null} when absent
     * @param identityKeySignature the signature over the identity key, or {@code null} when absent
     * @param session              the extension session identifier, or {@code null} when absent
     * @param state                the extension connection state, or {@code null} when absent
     * @param outgoing             {@code true} when the extension is outgoing
     * @param token                the opaque extension token, or {@code null} when absent
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code extensionId} is
     *                              {@code null}
     */
    public ExtensionStanza(String callId, Jid callCreator, boolean removal, String extensionId, String scope,
                           Jid extensionCreator, byte[] identityKey, byte[] identityKeySignature, String session,
                           State state, boolean outgoing, byte[] token) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.removal = removal;
        this.extensionId = Objects.requireNonNull(extensionId, "extensionId cannot be null");
        this.scope = scope;
        this.extensionCreator = extensionCreator;
        this.identityKey = identityKey == null ? null : identityKey.clone();
        this.identityKeySignature = identityKeySignature == null ? null : identityKeySignature.clone();
        this.session = session;
        this.state = state;
        this.outgoing = outgoing;
        this.token = token == null ? null : token.clone();
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for an extension action
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for an extension action
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns whether this is a RemoveExtension action rather than an AddExtension action.
     *
     * @return {@code true} when this is a RemoveExtension action, {@code false} when an AddExtension
     *         action
     */
    public boolean removal() {
        return removal;
    }

    /**
     * Returns the extension identifier.
     *
     * @return the extension identifier
     */
    public String extensionId() {
        return extensionId;
    }

    /**
     * Returns the extension scope, or {@code null} when absent.
     *
     * @return the raw scope
     */
    public String scope() {
        return scope;
    }

    /**
     * Returns the extension scope, if present.
     *
     * @return an {@link Optional} holding the scope, or empty when absent
     */
    public Optional<String> scopeValue() {
        return Optional.ofNullable(scope);
    }

    /**
     * Returns the JID of the participant that created the extension, or {@code null} when absent.
     *
     * @return the raw extension creator JID
     */
    public Jid extensionCreator() {
        return extensionCreator;
    }

    /**
     * Returns the extension creator's JID, if present.
     *
     * @return an {@link Optional} holding the creator JID, or empty when absent
     */
    public Optional<Jid> extensionCreatorValue() {
        return Optional.ofNullable(extensionCreator);
    }

    /**
     * Returns the extension creator's identity public key.
     *
     * <p>This accessor returns a defensive copy so the stored array cannot be mutated through the
     * returned reference.
     *
     * @return a copy of the identity key, or {@code null} when absent
     */
    public byte[] identityKey() {
        return identityKey == null ? null : identityKey.clone();
    }

    /**
     * Returns the signature over the identity key.
     *
     * <p>This accessor returns a defensive copy so the stored array cannot be mutated through the
     * returned reference.
     *
     * @return a copy of the identity key signature, or {@code null} when absent
     */
    public byte[] identityKeySignature() {
        return identityKeySignature == null ? null : identityKeySignature.clone();
    }

    /**
     * Returns the extension session identifier, or {@code null} when absent.
     *
     * @return the raw session identifier
     */
    public String session() {
        return session;
    }

    /**
     * Returns the extension session identifier, if present.
     *
     * @return an {@link Optional} holding the session identifier, or empty when absent
     */
    public Optional<String> sessionValue() {
        return Optional.ofNullable(session);
    }

    /**
     * Returns the extension connection state, or {@code null} when absent.
     *
     * @return the raw connection state
     */
    public State state() {
        return state;
    }

    /**
     * Returns the extension connection state, if present.
     *
     * @return an {@link Optional} holding the state, or empty when absent
     */
    public Optional<State> stateValue() {
        return Optional.ofNullable(state);
    }

    /**
     * Returns whether the extension is outgoing.
     *
     * @return {@code true} when the extension is outgoing
     */
    public boolean outgoing() {
        return outgoing;
    }

    /**
     * Returns the opaque extension token.
     *
     * <p>This accessor returns a defensive copy so the stored array cannot be mutated through the
     * returned reference.
     *
     * @return a copy of the token, or {@code null} when absent
     */
    public byte[] token() {
        return token == null ? null : token.clone();
    }

    /**
     * {@inheritDoc}
     *
     * <p>An extension carries one of two taxonomy ordinals depending on {@link #removal()}: a removal
     * projects to {@link SignalingType#REMOVE_EXTENSION}, otherwise to
     * {@link SignalingType#ADD_EXTENSION}.
     *
     * @return {@link SignalingType#REMOVE_EXTENSION} when {@link #removal()} is {@code true},
     *         {@link SignalingType#ADD_EXTENSION} otherwise
     */
    @Override
    public SignalingType type() {
        return removal ? SignalingType.REMOVE_EXTENSION : SignalingType.ADD_EXTENSION;
    }

    /**
     * Builds the {@code <extension call-id call-creator extension_id .../>} action stanza.
     *
     * <p>Every attribute beyond the common header and {@code extension_id} is omitted when its value
     * is absent; the {@code outgoing} flag is written only when {@link #outgoing()} is {@code true}.
     *
     * @return the extension action stanza
     */
    @Override
    public Stanza toStanza() {
        return CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(EXTENSION_ID_ATTRIBUTE, extensionId)
                .attribute(SCOPE_ATTRIBUTE, scope)
                .attribute(CREATOR_ATTRIBUTE, extensionCreator)
                .attribute(IDENTITY_KEY_ATTRIBUTE, identityKey)
                .attribute(IDENTITY_KEY_SIGNATURE_ATTRIBUTE, identityKeySignature)
                .attribute(SESSION_ATTRIBUTE, session)
                .attribute(STATE_ATTRIBUTE, state == null ? null : state.wire())
                .attribute(OUTGOING_ATTRIBUTE, FLAG_TRUE, outgoing)
                .attribute(TOKEN_ATTRIBUTE, token)
                .build();
    }

    /**
     * Decodes an {@code <extension>} action stanza into an {@link ExtensionStanza}.
     *
     * <p>The {@code removal} flag is supplied by the caller because the add and remove flows share the
     * {@code <extension>} wire tag and are distinguished only by the enclosing message type, which the
     * element itself does not carry. An unrecognized {@code state} literal and any absent optional
     * attribute decode to {@code null}; an absent {@code outgoing} decodes to {@code false}.
     *
     * @param stanza    the {@code <extension>} stanza
     * @param removal {@code true} when the stanza was carried by a RemoveExtension message, {@code false}
     *                when carried by an AddExtension message
     * @return the decoded extension action
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id}, {@code call-creator}, or
     *                                {@code extension_id} attribute is absent
     */
    public static ExtensionStanza of(Stanza stanza, boolean removal) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var extensionId = stanza.getRequiredAttributeAsString(EXTENSION_ID_ATTRIBUTE);
        var scope = stanza.getAttributeAsString(SCOPE_ATTRIBUTE).orElse(null);
        var extensionCreator = stanza.getAttributeAsJid(CREATOR_ATTRIBUTE).orElse(null);
        var identityKey = stanza.getAttributeAsBytes(IDENTITY_KEY_ATTRIBUTE).orElse(null);
        var identityKeySignature = stanza.getAttributeAsBytes(IDENTITY_KEY_SIGNATURE_ATTRIBUTE).orElse(null);
        var session = stanza.getAttributeAsString(SESSION_ATTRIBUTE).orElse(null);
        var state = stanza.getAttributeAsString(STATE_ATTRIBUTE).flatMap(State::ofWire).orElse(null);
        var outgoing = FLAG_TRUE.equals(stanza.getAttributeAsString(OUTGOING_ATTRIBUTE, FLAG_FALSE));
        var token = stanza.getAttributeAsBytes(TOKEN_ATTRIBUTE).orElse(null);
        return new ExtensionStanza(callId, callCreator, removal, extensionId, scope, extensionCreator,
                identityKey, identityKeySignature, session, state, outgoing, token);
    }

    /**
     * Compares this extension to another for value equality, comparing the binary fields by content.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is an {@link ExtensionStanza} with equal components
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof ExtensionStanza that
                && this.removal == that.removal
                && this.outgoing == that.outgoing
                && this.callId.equals(that.callId)
                && this.callCreator.equals(that.callCreator)
                && this.extensionId.equals(that.extensionId)
                && Objects.equals(this.scope, that.scope)
                && Objects.equals(this.extensionCreator, that.extensionCreator)
                && Arrays.equals(this.identityKey, that.identityKey)
                && Arrays.equals(this.identityKeySignature, that.identityKeySignature)
                && Objects.equals(this.session, that.session)
                && this.state == that.state
                && Arrays.equals(this.token, that.token);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}, hashing the binary fields by
     * content.
     *
     * @return the content based hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, removal, extensionId, scope, extensionCreator,
                Arrays.hashCode(identityKey), Arrays.hashCode(identityKeySignature), session, state, outgoing,
                Arrays.hashCode(token));
    }

    /**
     * Returns a string representation that renders the binary fields by length rather than by
     * reference.
     *
     * @return a diagnostic string representation
     */
    @Override
    public String toString() {
        return "ExtensionStanza[callId=" + callId
                + ", callCreator=" + callCreator
                + ", removal=" + removal
                + ", extensionId=" + extensionId
                + ", scope=" + scope
                + ", extensionCreator=" + extensionCreator
                + ", identityKey=" + (identityKey == null ? "null" : "byte[" + identityKey.length + "]")
                + ", identityKeySignature="
                + (identityKeySignature == null ? "null" : "byte[" + identityKeySignature.length + "]")
                + ", session=" + session
                + ", state=" + state
                + ", outgoing=" + outgoing
                + ", token=" + (token == null ? "null" : "byte[" + token.length + "]")
                + "]";
    }
}
