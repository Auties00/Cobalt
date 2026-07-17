package com.github.auties00.cobalt.wire.linked.device.pairing;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Top level wrapper for the three messages that make up WhatsApp's Noise XX handshake.
 *
 * <p>Every time a WhatsApp client opens its WebSocket to the server it has to negotiate a
 * Noise protocol session before any application traffic can flow. The handshake follows
 * the Noise XX pattern and is carried inside three protobuf messages: the client's
 * opening greeting ({@link ClientHello}), the server's reply ({@link ServerHello}) and
 * the client's closing step ({@link ClientFinish}). Each wire frame sent during the
 * handshake is a {@code HandshakeMessage} with exactly one of these three inner messages
 * populated, mimicking a oneof without using the protobuf oneof construct.
 *
 * <p>Once all three messages have been exchanged the two peers have agreed on symmetric
 * keys and can switch to encrypted application traffic (the payload of that traffic is
 * the {@link ClientPayload} sent inside {@link ClientFinish#payload()} and the session
 * related flags returned by the server inside {@link ServerHello#payload()}).
 */
@ProtobufMessage(name = "HandshakeMessage")
public final class HandshakeMessage {
    /**
     * Set on the first frame the client sends, identifying the message as the opening
     * step of the handshake.
     *
     * <p>Serialised as wire index {@code 2}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    ClientHello clientHello;

    /**
     * Set on the single frame the server replies with, carrying the server's handshake
     * material and an encrypted payload meant for the client.
     *
     * <p>Serialised as wire index {@code 3}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    ServerHello serverHello;

    /**
     * Set on the third and last handshake frame, sent by the client to complete the
     * Noise XX exchange and deliver the initial application payload.
     *
     * <p>Serialised as wire index {@code 4}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    ClientFinish clientFinish;

    /**
     * Full protobuf constructor invoked by the generated builder and the deserializer.
     *
     * @param clientHello  the opening handshake frame, or {@code null} on other steps
     * @param serverHello  the server reply frame, or {@code null} on other steps
     * @param clientFinish the closing handshake frame, or {@code null} on other steps
     */
    HandshakeMessage(ClientHello clientHello, ServerHello serverHello, ClientFinish clientFinish) {
        this.clientHello = clientHello;
        this.serverHello = serverHello;
        this.clientFinish = clientFinish;
    }

    /**
     * Returns the opening handshake frame when this message represents the first step.
     *
     * @return the {@link ClientHello}, or {@link Optional#empty()} when this is not a
     *         first step frame
     */
    public Optional<ClientHello> clientHello() {
        return Optional.ofNullable(clientHello);
    }

    /**
     * Returns the server reply frame when this message represents the second step.
     *
     * @return the {@link ServerHello}, or {@link Optional#empty()} when this is not a
     *         second step frame
     */
    public Optional<ServerHello> serverHello() {
        return Optional.ofNullable(serverHello);
    }

    /**
     * Returns the closing handshake frame when this message represents the third step.
     *
     * @return the {@link ClientFinish}, or {@link Optional#empty()} when this is not a
     *         third step frame
     */
    public Optional<ClientFinish> clientFinish() {
        return Optional.ofNullable(clientFinish);
    }

    /**
     * Replaces the opening handshake frame.
     *
     * @param clientHello the new opening frame, or {@code null} to clear it
     */
    public void setClientHello(ClientHello clientHello) {
        this.clientHello = clientHello;
    }

    /**
     * Replaces the server reply frame.
     *
     * @param serverHello the new server reply, or {@code null} to clear it
     */
    public void setServerHello(ServerHello serverHello) {
        this.serverHello = serverHello;
    }

    /**
     * Replaces the closing handshake frame.
     *
     * @param clientFinish the new closing frame, or {@code null} to clear it
     */
    public void setClientFinish(ClientFinish clientFinish) {
        this.clientFinish = clientFinish;
    }

    /**
     * Closing frame of the Noise XX handshake, sent by the client after it has received
     * the {@link ServerHello}.
     *
     * <p>At this point the client has processed the server's ephemeral and static public
     * keys and can derive the final traffic keys. The frame delivers the client's own
     * encrypted static public key in {@link #_static}, the first encrypted application
     * payload (typically a serialised {@link ClientPayload}) in {@link #payload} and,
     * when post quantum key exchange is in use, an extra Kyber ciphertext in
     * {@link #extendedCiphertext}.
     */
    @ProtobufMessage(name = "HandshakeMessage.ClientFinish")
    public static final class ClientFinish {
        /**
         * Encrypted client static public key, authenticated via the running Noise
         * handshake hash. Serialised as wire index {@code 1}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] _static;

        /**
         * Encrypted first application payload delivered alongside the handshake close,
         * typically a serialised {@link ClientPayload}. Serialised as wire index
         * {@code 2}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] payload;

        /**
         * Optional Kyber ciphertext used when the session upgrades to the post quantum
         * hybrid handshake. Absent on classical Noise XX sessions. Serialised as wire
         * index {@code 3}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] extendedCiphertext;

        /**
         * Full protobuf constructor invoked by the generated builder and the
         * deserializer.
         *
         * @param _static            the encrypted client static public key
         * @param payload            the encrypted first application payload
         * @param extendedCiphertext the optional Kyber ciphertext
         */
        ClientFinish(byte[] _static, byte[] payload, byte[] extendedCiphertext) {
            this._static = _static;
            this.payload = payload;
            this.extendedCiphertext = extendedCiphertext;
        }

        /**
         * Returns the encrypted client static public key.
         *
         * @return the encrypted static key bytes, or {@link Optional#empty()} when
         *         absent on the wire
         */
        public Optional<byte[]> _static() {
            return Optional.ofNullable(_static);
        }

        /**
         * Returns the encrypted first application payload.
         *
         * @return the encrypted payload bytes, or {@link Optional#empty()} when absent
         *         on the wire
         */
        public Optional<byte[]> payload() {
            return Optional.ofNullable(payload);
        }

        /**
         * Returns the optional Kyber ciphertext used for post quantum hybrid mode.
         *
         * @return the Kyber ciphertext bytes, or {@link Optional#empty()} when absent
         *         on the wire
         */
        public Optional<byte[]> extendedCiphertext() {
            return Optional.ofNullable(extendedCiphertext);
        }

        /**
         * Replaces the encrypted client static public key.
         *
         * @param _static the new encrypted static key, or {@code null} to clear it
         */
        public void setStatic(byte[] _static) {
            this._static = _static;
        }

        /**
         * Replaces the encrypted first application payload.
         *
         * @param payload the new encrypted payload, or {@code null} to clear it
         */
        public void setPayload(byte[] payload) {
            this.payload = payload;
        }

        /**
         * Replaces the optional Kyber ciphertext.
         *
         * @param extendedCiphertext the new Kyber ciphertext, or {@code null} to clear
         *                           it
         */
        public void setExtendedCiphertext(byte[] extendedCiphertext) {
            this.extendedCiphertext = extendedCiphertext;
        }
    }

    /**
     * Opening frame of the Noise XX handshake, sent by the client as the very first
     * message on a new connection.
     *
     * <p>The client contributes its ephemeral public key (always in the clear), its
     * static public key (encrypted against the mixed ephemeral and static keys) and an
     * initial payload that is also encrypted. When the client supports the post quantum
     * hybrid variant it sets {@link #useExtended} to {@code true} and includes an extra
     * Kyber ciphertext in {@link #extendedCiphertext}, signalling to the server that the
     * final keys should be mixed with Kyber shared secrets.
     */
    @ProtobufMessage(name = "HandshakeMessage.ClientHello")
    public static final class ClientHello {
        /**
         * Client ephemeral Curve25519 public key, sent in the clear as part of the
         * first handshake message. Serialised as wire index {@code 1}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] ephemeral;

        /**
         * Encrypted client static Curve25519 public key. The client usually omits this
         * field on very first connections and fills it on subsequent sessions that are
         * resuming an existing identity. Serialised as wire index {@code 2}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] _static;

        /**
         * Encrypted initial payload, used to carry routing metadata that the server
         * consumes during the handshake itself. Serialised as wire index {@code 3}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] payload;

        /**
         * Flag indicating that the client is willing to negotiate the post quantum
         * hybrid handshake. When {@code true} the client also includes
         * {@link #extendedCiphertext}. Serialised as wire index {@code 4}.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
        Boolean useExtended;

        /**
         * Kyber ciphertext supplied only when {@link #useExtended} is {@code true}.
         * Serialised as wire index {@code 5}.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
        byte[] extendedCiphertext;

        /**
         * Full protobuf constructor invoked by the generated builder and the
         * deserializer.
         *
         * @param ephemeral          the client ephemeral public key
         * @param _static            the encrypted client static public key, or
         *                           {@code null} on first connections
         * @param payload            the encrypted opening payload
         * @param useExtended        whether the client requests the post quantum
         *                           hybrid handshake
         * @param extendedCiphertext the Kyber ciphertext when the hybrid handshake is
         *                           requested
         */
        ClientHello(byte[] ephemeral, byte[] _static, byte[] payload, Boolean useExtended, byte[] extendedCiphertext) {
            this.ephemeral = ephemeral;
            this._static = _static;
            this.payload = payload;
            this.useExtended = useExtended;
            this.extendedCiphertext = extendedCiphertext;
        }

        /**
         * Returns the client's ephemeral public key.
         *
         * @return the ephemeral key bytes, or {@link Optional#empty()} when absent on
         *         the wire
         */
        public Optional<byte[]> ephemeral() {
            return Optional.ofNullable(ephemeral);
        }

        /**
         * Returns the encrypted client static public key.
         *
         * @return the encrypted static key bytes, or {@link Optional#empty()} when
         *         absent on the wire
         */
        public Optional<byte[]> _static() {
            return Optional.ofNullable(_static);
        }

        /**
         * Returns the encrypted opening payload.
         *
         * @return the encrypted payload bytes, or {@link Optional#empty()} when absent
         *         on the wire
         */
        public Optional<byte[]> payload() {
            return Optional.ofNullable(payload);
        }

        /**
         * Returns whether the client is requesting the post quantum hybrid handshake.
         *
         * @return {@code true} when the client signalled the extension, otherwise
         *         {@code false} (including when the field was absent on the wire)
         */
        public boolean useExtended() {
            return useExtended != null && useExtended;
        }

        /**
         * Returns the Kyber ciphertext supplied when the hybrid handshake is in use.
         *
         * @return the ciphertext bytes, or {@link Optional#empty()} when absent on the
         *         wire
         */
        public Optional<byte[]> extendedCiphertext() {
            return Optional.ofNullable(extendedCiphertext);
        }

        /**
         * Replaces the client's ephemeral public key.
         *
         * @param ephemeral the new ephemeral key, or {@code null} to clear it
         */
        public void setEphemeral(byte[] ephemeral) {
            this.ephemeral = ephemeral;
        }

        /**
         * Replaces the encrypted client static public key.
         *
         * @param _static the new encrypted static key, or {@code null} to clear it
         */
        public void setStatic(byte[] _static) {
            this._static = _static;
        }

        /**
         * Replaces the encrypted opening payload.
         *
         * @param payload the new encrypted payload, or {@code null} to clear it
         */
        public void setPayload(byte[] payload) {
            this.payload = payload;
        }

        /**
         * Replaces the post quantum extension flag.
         *
         * @param useExtended the new flag value, or {@code null} to clear it
         */
        public void setUseExtended(Boolean useExtended) {
            this.useExtended = useExtended;
        }

        /**
         * Replaces the Kyber ciphertext.
         *
         * @param extendedCiphertext the new ciphertext, or {@code null} to clear it
         */
        public void setExtendedCiphertext(byte[] extendedCiphertext) {
            this.extendedCiphertext = extendedCiphertext;
        }
    }

    /**
     * Middle frame of the Noise XX handshake, sent by the server in response to a
     * {@link ClientHello}.
     *
     * <p>The server contributes its own ephemeral and static public keys together with
     * an encrypted payload that typically contains the server certificate chain and
     * other session bootstrap information. When the post quantum hybrid handshake is in
     * use the server also sends its Kyber public key inside {@link #extendedStatic} so
     * the client can encapsulate the post quantum shared secret in the following
     * {@link ClientFinish}.
     */
    @ProtobufMessage(name = "HandshakeMessage.ServerHello")
    public static final class ServerHello {
        /**
         * Server ephemeral Curve25519 public key, sent in the clear. Serialised as
         * wire index {@code 1}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] ephemeral;

        /**
         * Encrypted server static Curve25519 public key. Serialised as wire index
         * {@code 2}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] _static;

        /**
         * Encrypted server payload, usually carrying the Noise certificate chain that
         * authenticates the static key. Serialised as wire index {@code 3}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] payload;

        /**
         * Server post quantum public key, sent only when the client requested the
         * hybrid handshake in its {@link ClientHello}. Serialised as wire index
         * {@code 4}.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
        byte[] extendedStatic;

        /**
         * Full protobuf constructor invoked by the generated builder and the
         * deserializer.
         *
         * @param ephemeral      the server ephemeral public key
         * @param _static        the encrypted server static public key
         * @param payload        the encrypted server payload
         * @param extendedStatic the server post quantum public key when in hybrid mode
         */
        ServerHello(byte[] ephemeral, byte[] _static, byte[] payload, byte[] extendedStatic) {
            this.ephemeral = ephemeral;
            this._static = _static;
            this.payload = payload;
            this.extendedStatic = extendedStatic;
        }

        /**
         * Returns the server's ephemeral public key.
         *
         * @return the ephemeral key bytes, or {@link Optional#empty()} when absent on
         *         the wire
         */
        public Optional<byte[]> ephemeral() {
            return Optional.ofNullable(ephemeral);
        }

        /**
         * Returns the encrypted server static public key.
         *
         * @return the encrypted static key bytes, or {@link Optional#empty()} when
         *         absent on the wire
         */
        public Optional<byte[]> _static() {
            return Optional.ofNullable(_static);
        }

        /**
         * Returns the encrypted server payload.
         *
         * @return the encrypted payload bytes, or {@link Optional#empty()} when absent
         *         on the wire
         */
        public Optional<byte[]> payload() {
            return Optional.ofNullable(payload);
        }

        /**
         * Returns the server's post quantum public key for the hybrid handshake.
         *
         * @return the Kyber public key bytes, or {@link Optional#empty()} when absent
         *         on the wire
         */
        public Optional<byte[]> extendedStatic() {
            return Optional.ofNullable(extendedStatic);
        }

        /**
         * Replaces the server's ephemeral public key.
         *
         * @param ephemeral the new ephemeral key, or {@code null} to clear it
         */
        public void setEphemeral(byte[] ephemeral) {
            this.ephemeral = ephemeral;
        }

        /**
         * Replaces the encrypted server static public key.
         *
         * @param _static the new encrypted static key, or {@code null} to clear it
         */
        public void setStatic(byte[] _static) {
            this._static = _static;
        }

        /**
         * Replaces the encrypted server payload.
         *
         * @param payload the new encrypted payload, or {@code null} to clear it
         */
        public void setPayload(byte[] payload) {
            this.payload = payload;
        }

        /**
         * Replaces the server post quantum public key.
         *
         * @param extendedStatic the new Kyber public key, or {@code null} to clear it
         */
        public void setExtendedStatic(byte[] extendedStatic) {
            this.extendedStatic = extendedStatic;
        }
    }
}
