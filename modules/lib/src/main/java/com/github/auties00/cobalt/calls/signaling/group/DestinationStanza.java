package com.github.auties00.cobalt.calls.signaling.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.SignalingType;
import com.github.auties00.cobalt.calls.signaling.session.CallKeyDistribution;

/**
 * Represents the {@code <destination>} fanout addressing block of a group call signaling message.
 *
 * <p>A destination block names the set of device JIDs a fanout signaling message is addressed to. It is
 * attached to a group message that must reach several specific devices rather than the single peer of a
 * one to one exchange: each addressed device is a {@code <to jid="..."/>} child, and the receiver
 * delivers the enclosing message to exactly those devices. This record models the block as the ordered
 * list of addressed device JIDs.
 *
 * <p>On the wire the element is {@code <destination> <to jid="..."/>* </destination>}. The {@code <to>}
 * children carry the addressed device JID in a {@code jid} attribute, not as element content; a
 * destination block addressing no devices is an empty {@code <destination/>} element.
 *
 * <p>This block addresses devices but carries no per device key material; the offer's per device call
 * key fanout, where each {@code <to>} additionally wraps an {@code <enc>} ciphertext child, is modeled
 * by {@link CallKeyDistribution}. A bare destination block is the addressing only form used by the
 * group rekey and extension fanout paths.
 *
 * @param devices the addressed device JIDs in fanout order; never {@code null}, possibly empty
 * @see CallKeyDistribution
 * @see GroupUpdateStanza
 */
public record DestinationStanza(List<Jid> devices) implements CallMessage {
    /**
     * The wire element tag for a destination fanout block.
     */
    public static final String ELEMENT = "destination";

    /**
     * The wire element tag for one addressed device entry inside a destination block.
     */
    private static final String TO_ELEMENT = "to";

    /**
     * The wire attribute naming the addressed device JID on a {@code <to>} element.
     */
    private static final String JID_ATTRIBUTE = "jid";

    /**
     * Canonicalizes the record components, defensively copying the device list.
     *
     * @throws NullPointerException if {@code devices} is {@code null} or any element is {@code null}
     */
    public DestinationStanza {
        Objects.requireNonNull(devices, "devices cannot be null");
        devices = List.copyOf(devices);
    }

    /**
     * Returns a destination block addressing the given device JIDs.
     *
     * @param devices the addressed device JIDs in fanout order
     * @return the destination block
     * @throws NullPointerException if {@code devices} is {@code null} or any element is {@code null}
     */
    public static DestinationStanza of(List<Jid> devices) {
        return new DestinationStanza(devices);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@linkplain Optional#empty() empty}; a {@code <destination>} fanout carries no
     *         {@code call-id} header
     */
    @Override
    public Optional<String> callId() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@linkplain Optional#empty() empty}; a {@code <destination>} fanout carries no
     *         {@code call-creator} header
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * <p>A destination block is a structural addressing element attached to another signaling action,
     * not a standalone action of its own, so it maps to no {@link SignalingType} and this method returns
     * {@code null}.
     *
     * @return {@code null}, since a destination block carries no signaling type
     */
    @Override
    public SignalingType type() {
        return null;
    }

    /**
     * Builds the {@code <destination> <to jid="..."/>* </destination>} block stanza.
     *
     * <p>Each addressed device becomes a keyless {@code <to jid="..."/>} child; a block addressing no
     * devices produces an empty {@code <destination/>} element.
     *
     * @return the destination block stanza
     */
    @Override
    public Stanza toStanza() {
        var builder = new StanzaBuilder()
                .description(ELEMENT);
        if (!devices.isEmpty()) {
            var children = devices.stream()
                    .map(device -> new StanzaBuilder()
                            .description(TO_ELEMENT)
                            .attribute(JID_ATTRIBUTE, device)
                            .build())
                    .toList();
            builder.content(children);
        }
        return builder.build();
    }

    /**
     * Decodes a {@code <destination>} stanza into a {@link DestinationStanza}.
     *
     * <p>Each {@code <to>} child contributes its {@code jid} attribute to the addressed device list; a
     * {@code <to>} child without a parseable {@code jid} attribute is skipped. A stanza that is not a
     * {@code <destination>} element yields an empty result so callers iterating a mixed child list can
     * skip it.
     *
     * @param stanza the {@code <destination>} stanza
     * @return the decoded destination block, or an empty result when the stanza is not a
     *         {@code <destination>} element
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    public static Optional<DestinationStanza> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription(ELEMENT)) {
            return Optional.empty();
        }
        var devices = stanza.streamChildren(TO_ELEMENT)
                .flatMap(child -> child.streamAttributeAsJid(JID_ATTRIBUTE))
                .toList();
        return Optional.of(new DestinationStanza(devices));
    }
}
