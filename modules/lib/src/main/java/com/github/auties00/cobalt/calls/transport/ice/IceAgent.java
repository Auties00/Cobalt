package com.github.auties00.cobalt.calls.transport.ice;

import com.github.auties00.cobalt.calls.platform.VoipCryptoNative;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.transport.stun.StunAttributeType;
import com.github.auties00.cobalt.calls.transport.stun.StunIntegrity;
import com.github.auties00.cobalt.calls.transport.stun.StunMessage;
import com.github.auties00.cobalt.calls.transport.warp.WarpCodecSupport;

/**
 * Runs the RFC 8445 ICE agent for the Web P2P interop path: it gathers local candidates, forms and
 * prioritizes candidate pairs against the remote candidates, builds and parses STUN binding requests
 * and responses, and nominates a pair when the controlling agent confirms one.
 *
 * <p>This agent is used only when the call peer is a WhatsApp Web client and a direct peer to peer
 * transport is attempted; the default relay/SFU path does not run it. It owns the Java half of ICE: the
 * candidate checklist, the short term credential username {@code "remoteUfrag:localUfrag"}, the binding
 * request build ({@code USERNAME}, {@code PRIORITY}, the role tiebreaker as
 * {@link StunAttributeType#ICE_CONTROLLING} when controlling or {@link StunAttributeType#ICE_CONTROLLED}
 * when controlled, an optional {@code USE-CANDIDATE}, then {@code MESSAGE-INTEGRITY} and
 * {@code FINGERPRINT}) and the binding response build ({@code XOR-MAPPED-ADDRESS} then
 * {@code MESSAGE-INTEGRITY}). Both the request and the response are finalized in the order
 * {@code MESSAGE-INTEGRITY} then {@code FINGERPRINT} through
 * {@link StunMessage#finalizeWithIntegrity(byte[])}. The remote candidate count is bounded by
 * {@value #MAX_REMOTE_CANDIDATES}.
 *
 * <p>The DTLS record layer and the actual socket I/O are host provided; this agent never sends bytes
 * itself, it produces the message bytes for the transport to send and consumes the bytes the transport
 * receives.
 *
 * <p>The agent is driven by the single call transport thread and is not thread safe.
 */
public final class IceAgent {
    /**
     * The logger for {@link IceAgent}.
     */
    private static final System.Logger LOGGER = Log.get(IceAgent.class);

    /**
     * The maximum number of remote candidates the agent accepts from signaling.
     */
    public static final int MAX_REMOTE_CANDIDATES = 100;

    /**
     * The length, in bytes, of the role tiebreaker value of the {@code ICE-CONTROLLING} and
     * {@code ICE-CONTROLLED} attribute.
     */
    private static final int TIEBREAKER_LENGTH = 8;

    /**
     * Holds the local ICE username fragment, the short term credential local ufrag.
     */
    private final String localUfrag;

    /**
     * Holds the local ICE password, keying the {@code MESSAGE-INTEGRITY} of outbound messages and
     * verifying inbound ones.
     */
    private final byte[] localPassword;

    /**
     * Holds the remote ICE username fragment, learned from signaling.
     */
    private final String remoteUfrag;

    /**
     * Holds the remote ICE password, keying the {@code MESSAGE-INTEGRITY} this agent puts on a request
     * the remote will verify.
     */
    private final byte[] remotePassword;

    /**
     * Holds whether this agent is the controlling agent, which sends {@code ICE-CONTROLLING} and may
     * nominate.
     */
    private final boolean controlling;

    /**
     * Holds the role tiebreaker this agent advertises, eight random bytes drawn at construction.
     */
    private final byte[] tiebreaker;

    /**
     * Holds the gathered local candidates.
     */
    private final List<IceCandidate> localCandidates = new ArrayList<>();

    /**
     * Holds the remote candidates received from signaling.
     */
    private final List<IceCandidate> remoteCandidates = new ArrayList<>();

    /**
     * Holds the candidate pair checklist, kept sorted descending by pair priority.
     */
    private final List<IceCandidatePair> checklist = new ArrayList<>();

    /**
     * Holds the nominated pair once a controlling agent confirms one, or {@code null} until then.
     */
    private IceCandidatePair nominatedPair;

    /**
     * Constructs an ICE agent with the local and remote short term credentials and the controlling
     * role.
     *
     * <p>The local and remote passwords are defensively cloned; the role tiebreaker is drawn from
     * {@value #TIEBREAKER_LENGTH} random bytes at construction.
     *
     * @param localUfrag     the local ICE username fragment
     * @param localPassword  the local ICE password, in raw bytes
     * @param remoteUfrag    the remote ICE username fragment from signaling
     * @param remotePassword the remote ICE password from signaling, in raw bytes
     * @param controlling    whether this agent is the controlling agent
     * @throws NullPointerException if any argument is {@code null}
     */
    public IceAgent(String localUfrag,
                    byte[] localPassword,
                    String remoteUfrag,
                    byte[] remotePassword,
                    boolean controlling) {
        this.localUfrag = Objects.requireNonNull(localUfrag, "localUfrag cannot be null");
        this.localPassword = Objects.requireNonNull(localPassword, "localPassword cannot be null").clone();
        this.remoteUfrag = Objects.requireNonNull(remoteUfrag, "remoteUfrag cannot be null");
        this.remotePassword = Objects.requireNonNull(remotePassword, "remotePassword cannot be null").clone();
        this.controlling = controlling;
        this.tiebreaker = VoipCryptoNative.randomBytes(TIEBREAKER_LENGTH);
    }

    /**
     * Adds a gathered local candidate to the agent.
     *
     * <p>Adding a local candidate rebuilds the checklist against the known remote candidates so a newly
     * gathered candidate immediately pairs with them.
     *
     * @param candidate the local candidate to add
     * @throws NullPointerException if {@code candidate} is {@code null}
     */
    public void addLocalCandidate(IceCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate cannot be null");
        localCandidates.add(candidate);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "ice local candidate added, type={0} protocol={1} priority={2}",
                    candidate.type(), candidate.protocol(), candidate.priority());
        }
        rebuildChecklist();
    }

    /**
     * Appends the remote candidates received from signaling and rebuilds the checklist.
     *
     * <p>The combined remote candidate count is bounded by {@value #MAX_REMOTE_CANDIDATES}; a batch that
     * would exceed the bound is rejected before any of it is added.
     *
     * @param candidates the remote candidates to append
     * @throws NullPointerException     if {@code candidates} is {@code null}
     * @throws IllegalArgumentException if appending would exceed {@value #MAX_REMOTE_CANDIDATES}
     *                                  remote candidates
     */
    public void appendRemoteCandidates(List<IceCandidate> candidates) {
        Objects.requireNonNull(candidates, "candidates cannot be null");
        if (remoteCandidates.size() + candidates.size() > MAX_REMOTE_CANDIDATES) {
            throw new IllegalArgumentException("too many candidates: "
                    + (remoteCandidates.size() + candidates.size()) + " exceeds " + MAX_REMOTE_CANDIDATES);
        }
        remoteCandidates.addAll(candidates);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "ice remote candidates appended, added={0} total={1}",
                    candidates.size(), remoteCandidates.size());
        }
        rebuildChecklist();
    }

    /**
     * Returns the candidate pair checklist, sorted descending by pair priority.
     *
     * @return an unmodifiable snapshot of the checklist in check order
     */
    public List<IceCandidatePair> checklist() {
        return List.copyOf(checklist);
    }

    /**
     * Builds an ICE binding request for a candidate pair, optionally nominating it.
     *
     * <p>The request carries the {@code USERNAME} {@code "remoteUfrag:localUfrag"}, the local
     * candidate's {@code PRIORITY}, this agent's role tiebreaker ({@code ICE-CONTROLLING} when
     * controlling, else {@code ICE-CONTROLLED}), an empty {@code USE-CANDIDATE} when {@code nominate}
     * is set (only a controlling agent should nominate), and is finalized with {@code MESSAGE-INTEGRITY}
     * keyed by the remote password then {@code FINGERPRINT}. The pair's state is advanced to
     * {@link IceCheckState#IN_PROGRESS}.
     *
     * @param pair          the candidate pair to check
     * @param transactionId the twelve byte STUN transaction id for this request
     * @param nominate      whether to add {@code USE-CANDIDATE} to nominate the pair
     * @return the finalized binding request bytes
     * @throws NullPointerException     if {@code pair} or {@code transactionId} is {@code null}
     * @throws IllegalArgumentException if {@code transactionId} is not
     *                                  {@value StunMessage#TRANSACTION_ID_LENGTH} bytes
     * @throws IllegalStateException    if {@code nominate} is set but this agent is not controlling
     */
    public byte[] buildBindingRequest(IceCandidatePair pair, byte[] transactionId, boolean nominate) {
        Objects.requireNonNull(pair, "pair cannot be null");
        Objects.requireNonNull(transactionId, "transactionId cannot be null");
        if (transactionId.length != StunMessage.TRANSACTION_ID_LENGTH) {
            throw new IllegalArgumentException("transactionId must be "
                    + StunMessage.TRANSACTION_ID_LENGTH + " bytes, got " + transactionId.length);
        }
        if (nominate && !controlling) {
            throw new IllegalStateException("only the controlling agent may nominate a pair");
        }

        var attributes = new ArrayList<StunMessage.Attribute>();
        var username = (remoteUfrag + ':' + localUfrag).getBytes(StandardCharsets.UTF_8);
        attributes.add(new StunMessage.Attribute(StunAttributeType.USERNAME, username));
        attributes.add(new StunMessage.Attribute(StunAttributeType.PRIORITY, u32(pair.local().priority())));
        var roleType = controlling ? StunAttributeType.ICE_CONTROLLING : StunAttributeType.ICE_CONTROLLED;
        attributes.add(new StunMessage.Attribute(roleType, tiebreaker.clone()));
        if (nominate) {
            attributes.add(new StunMessage.Attribute(StunAttributeType.USE_CANDIDATE, DataUtils.EMPTY_BYTE_ARRAY));
            pair.nominate();
        }

        var message = new StunMessage(StunMessage.TYPE_BINDING_REQUEST, StunMessage.MAGIC_COOKIE,
                transactionId, attributes);
        pair.setState(IceCheckState.IN_PROGRESS);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "ice binding request built, nominate={0} controlling={1}",
                    nominate, controlling);
        }
        return message.finalizeWithIntegrity(remotePassword);
    }

    /**
     * Builds an ICE binding success response for a received request.
     *
     * <p>The response echoes the request's transaction id, carries the requester's reflexive transport
     * address as an {@code XOR-MAPPED-ADDRESS}, and is finalized with {@code MESSAGE-INTEGRITY} keyed by
     * the local password then {@code FINGERPRINT}.
     *
     * @param transactionId  the transaction id of the request being answered
     * @param reflexiveSource the transport address the request arrived from, returned as
     *                        {@code XOR-MAPPED-ADDRESS}
     * @return the finalized binding response bytes
     * @throws NullPointerException     if {@code transactionId} or {@code reflexiveSource} is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code transactionId} is not
     *                                  {@value StunMessage#TRANSACTION_ID_LENGTH} bytes
     */
    public byte[] buildBindingResponse(byte[] transactionId, InetSocketAddress reflexiveSource) {
        Objects.requireNonNull(transactionId, "transactionId cannot be null");
        Objects.requireNonNull(reflexiveSource, "reflexiveSource cannot be null");
        if (transactionId.length != StunMessage.TRANSACTION_ID_LENGTH) {
            throw new IllegalArgumentException("transactionId must be "
                    + StunMessage.TRANSACTION_ID_LENGTH + " bytes, got " + transactionId.length);
        }
        var xorMapped = StunMessage.encodeXorMappedAddress(reflexiveSource, transactionId);
        var attributes = List.of(new StunMessage.Attribute(StunAttributeType.XOR_MAPPED_ADDRESS, xorMapped));
        var message = new StunMessage(StunMessage.TYPE_BINDING_SUCCESS_RESPONSE, StunMessage.MAGIC_COOKIE,
                transactionId, attributes);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "ice binding response built");
        }
        return message.finalizeWithIntegrity(localPassword);
    }

    /**
     * Parses an inbound STUN message and verifies its {@code MESSAGE-INTEGRITY} under the credential the
     * message's direction implies.
     *
     * <p>A message that does not carry the magic cookie, or whose {@code MESSAGE-INTEGRITY} does not
     * verify, is rejected with an empty result so the caller drops it. A binding success or error
     * response is the answer to a check this agent sent keyed with the remote password, so its integrity
     * is verified under the remote password; an inbound binding request or indication is a peer's check
     * toward this agent, keyed with this agent's local password, so its integrity is verified under the
     * local password. A verified message is returned for the caller to act on (answer a request,
     * complete a check on a response).
     *
     * @implNote This implementation follows the RFC 8489 short term credential rule that a transaction's
     *           response reuses the request's password ({@code 9.1.4}): the relay path keys the request
     *           toward the relay with the relay {@code <key>} (the remote password), so the relay's
     *           binding success response carries a {@code MESSAGE-INTEGRITY} under that same
     *           {@code <key>}, not under this client's locally generated password. Verifying every
     *           inbound message under the local password would silently drop the relay's success
     *           response and stall ICE nomination.
     *
     * @param message the received STUN message bytes
     * @return the parsed message when it verifies, or an empty result when it must be dropped
     * @throws NullPointerException if {@code message} is {@code null}
     */
    public Optional<StunMessage> parseInbound(byte[] message) {
        Objects.requireNonNull(message, "message cannot be null");
        StunMessage parsed;
        try {
            parsed = StunMessage.decode(message);
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "ice inbound stun message failed to decode, length=" + message.length,
                        exception);
            }
            return Optional.empty();
        }
        if (parsed.magicCookie() != StunMessage.MAGIC_COOKIE) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "ice inbound stun message has wrong magic cookie, type={0}",
                        parsed.messageType());
            }
            return Optional.empty();
        }
        var integrityOffset = locateIntegrityOffset(message);
        if (integrityOffset < 0) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "ice inbound stun message missing message-integrity, type={0}",
                        parsed.messageType());
            }
            return Optional.empty();
        }
        var key = isResponse(parsed.messageType()) ? remotePassword : localPassword;
        if (!StunIntegrity.verifyMessageIntegrity(message, integrityOffset, key)) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "ice inbound stun message failed integrity check, type={0}",
                        parsed.messageType());
            }
            return Optional.empty();
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "ice inbound stun message verified, type={0}", parsed.messageType());
        }
        return Optional.of(parsed);
    }

    /**
     * Returns whether a STUN message type is a response, the success or error answer to a request.
     *
     * <p>The short term credential password that keys a message's {@code MESSAGE-INTEGRITY} is the
     * password of the {@code USERNAME} its transaction was opened with: a response to a request this
     * agent sent verifies under the remote password, whereas a request another agent sends toward this
     * agent verifies under the local password. This test selects between the two in
     * {@link #parseInbound(byte[])}.
     *
     * @param messageType the STUN message type
     * @return {@code true} for a binding success or error response, {@code false} for a request or indication
     */
    private static boolean isResponse(int messageType) {
        return messageType == StunMessage.TYPE_BINDING_SUCCESS_RESPONSE
                || messageType == StunMessage.TYPE_BINDING_ERROR_RESPONSE;
    }

    /**
     * Records a successful connectivity check on a pair and, for a controlling agent, nominates it.
     *
     * <p>The pair's state advances to {@link IceCheckState#SUCCEEDED}. When this agent is controlling and
     * no pair is yet nominated, this pair becomes the nominated pair; the controlling agent is expected
     * to have sent {@code USE-CANDIDATE} for it.
     *
     * @param pair the pair whose check succeeded
     * @throws NullPointerException if {@code pair} is {@code null}
     */
    public void onCheckSucceeded(IceCandidatePair pair) {
        Objects.requireNonNull(pair, "pair cannot be null");
        pair.setState(IceCheckState.SUCCEEDED);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "ice pair check succeeded, priority=0x{0}", Long.toHexString(pair.priority()));
        }
        if (controlling && nominatedPair == null) {
            pair.nominate();
            nominatedPair = pair;
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "ice pair nominated, priority=0x{0}", Long.toHexString(pair.priority()));
            }
        }
    }

    /**
     * Records that the remote nominated a pair, the controlled agent's view of {@code USE-CANDIDATE}.
     *
     * <p>A controlled agent calls this when it receives a binding request with {@code USE-CANDIDATE} on
     * a pair whose check has succeeded, adopting it as the nominated pair.
     *
     * @param pair the pair the remote nominated
     * @throws NullPointerException if {@code pair} is {@code null}
     */
    public void onRemoteNomination(IceCandidatePair pair) {
        Objects.requireNonNull(pair, "pair cannot be null");
        pair.nominate();
        nominatedPair = pair;
        if (Log.INFO) {
            LOGGER.log(Level.INFO, "ice remote nomination adopted, priority=0x{0}",
                    Long.toHexString(pair.priority()));
        }
    }

    /**
     * Returns the nominated candidate pair, if one has been chosen.
     *
     * @return the nominated pair, or an empty result before nomination
     */
    public Optional<IceCandidatePair> nominatedPair() {
        return Optional.ofNullable(nominatedPair);
    }

    /**
     * Returns whether this agent is the controlling agent.
     *
     * @return {@code true} when this agent is controlling
     */
    public boolean controlling() {
        return controlling;
    }

    /**
     * Rebuilds the candidate pair checklist from the current local and remote candidates.
     *
     * <p>Every compatible local/remote combination becomes a pair (incompatible combinations are
     * dropped per {@link IceCandidatePair#isCompatible(IceCandidate, IceCandidate)}); the checklist is
     * then sorted descending by pair priority. A pair already present keeps neither its old state nor
     * its nomination because the checklist is rebuilt wholesale; the nominated pair reference, held
     * separately, survives.
     */
    private void rebuildChecklist() {
        checklist.clear();
        for (var local : localCandidates) {
            for (var remote : remoteCandidates) {
                if (IceCandidatePair.isCompatible(local, remote)) {
                    checklist.add(new IceCandidatePair(local, remote, controlling));
                }
            }
        }
        checklist.sort(Comparator.comparingLong(IceCandidatePair::priority).reversed());
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "ice checklist rebuilt, pairs={0}", checklist.size());
        }
    }

    /**
     * Finds the byte offset of the {@code MESSAGE-INTEGRITY} attribute header within an encoded STUN
     * message.
     *
     * <p>The offset is needed to verify the HMAC over the message prefix that precedes the attribute.
     * The attributes are walked from the header, advancing past each value's four byte padding, until
     * the {@code MESSAGE-INTEGRITY} type is found.
     *
     * @param message the encoded STUN message bytes
     * @return the offset of the {@code MESSAGE-INTEGRITY} attribute header, or {@code -1} when it is
     *         absent
     */
    private static int locateIntegrityOffset(byte[] message) {
        var cursor = StunMessage.HEADER_LENGTH;
        while (cursor + 4 <= message.length) {
            var type = WarpCodecSupport.readU16(message, cursor);
            var length = WarpCodecSupport.readU16(message, cursor + 2);
            if (type == StunAttributeType.MESSAGE_INTEGRITY.value()) {
                return cursor;
            }
            var padded = (length + 3) & ~3;
            cursor += 4 + padded;
        }
        return -1;
    }

    /**
     * Encodes a {@code u32} value big endian into a four byte attribute value.
     *
     * @param value the value to encode, masked to thirty two bits
     * @return the four byte big endian encoding
     */
    private static byte[] u32(long value) {
        return DataUtils.intToBytes((int) value, 4);
    }
}
