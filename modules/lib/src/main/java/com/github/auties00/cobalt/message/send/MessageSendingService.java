package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.ack.AckResult;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;

/**
 * Single entry point for the outgoing-message send pipeline.
 *
 * <p>The service prepares a raw {@link LinkedMessageContainer} into a fully-populated
 * {@link LinkedMessageInfo}, dedupes concurrent sends keyed by message id, and routes by the parent
 * JID's server kind: 1:1 PN or LID chats, groups and communities, status broadcasts, business
 * broadcast lists, and {@code @newsletter} publishes each take a dedicated wire path. Peer
 * protocol messages addressed to one of the account's own devices bypass the chat routing and go
 * through {@link #sendPeer(Jid, ChatMessageInfo)}.
 *
 * @implSpec
 * Implementations must reject a second concurrent send carrying the same wire id and must route
 * every send by the {@link LinkedMessageInfo} subtype combined with the parent JID's server kind.
 */
public interface MessageSendingService {
    /**
     * Prepares the raw container and dispatches it to the supplied chat.
     *
     * <p>Top-level entry point for embedders sending a message: the call
     * generates a wire id and {@code messageSecret}, populates the
     * device-context info, auto-converts reactions and comments to their
     * encrypted addon variants in CAG groups, decorates an extended-text body
     * with a link preview when applicable, and then forwards to the
     * chat-kind-specific path.
     *
     * @implSpec
     * Implementations must prepare {@code container} into a {@link LinkedMessageInfo} (using the
     * newsletter path for {@code @newsletter} recipients and the chat path otherwise) and then
     * dispatch it through {@link #send(LinkedMessageInfo)}.
     *
     * @param chatJid   the recipient chat, group, status, or newsletter
     *                  {@link Jid}
     * @param container the raw {@link LinkedMessageContainer}
     * @return the parsed server {@link AckResult}
     * @throws NullPointerException if any argument is {@code null}
     */
    AckResult send(Jid chatJid, LinkedMessageContainer container);

    /**
     * Dispatches a fully-prepared {@link LinkedMessageInfo} to its target chat.
     *
     * <p>This overload is for callers that have already prepared the
     * {@link LinkedMessageInfo} (typically a resend, a debug-injection harness, or a
     * follow-up to a {@link #sendKeyDistribution(Jid, MessageKey)} call). The
     * routing predicate keys on the message-info subtype combined with the
     * parent JID's server. A {@link ChatMessageInfo} is accepted for user, group,
     * community, status, and broadcast parents; a {@link NewsletterMessageInfo}
     * is accepted only for a {@code @newsletter} parent.
     *
     * @implSpec
     * Implementations must reject a duplicate in-flight send for the same message id and must
     * resolve the wire path from the {@link LinkedMessageInfo} subtype paired with the parent JID's
     * server kind.
     *
     * @param messageInfo the fully-prepared outgoing {@link LinkedMessageInfo}
     * @return the parsed server {@link AckResult}
     * @throws NullPointerException                           if
     *                                                        {@code messageInfo}
     *                                                        is {@code null}
     * @throws IllegalArgumentException                       if the
     *                                                        {@link MessageKey}
     *                                                        is missing the id or
     *                                                        parent JID
     * @throws WhatsAppMessageException.Send.InvalidRecipient if the parent JID's
     *                                                        server does not
     *                                                        match the
     *                                                        {@link LinkedMessageInfo}
     *                                                        subtype
     * @throws WhatsAppMessageException.Send.Unknown          if a send is
     *                                                        already in flight
     *                                                        for the same id
     */
    AckResult send(LinkedMessageInfo messageInfo);

    /**
     * Dispatches a standalone sender-key distribution to a group with no message
     * content.
     *
     * <p>Use to pre-seed sender keys before sending a media-heavy or otherwise
     * latency-sensitive message; the call returns silently when every
     * participant already holds the key. The stanza carries the {@code text}
     * type marker and {@code device_fanout="false"}.
     *
     * @implSpec
     * Implementations must reject a non-group, non-community {@code groupJid} and must require
     * the key to carry a wire id.
     *
     * @param groupJid the target group {@link Jid}
     * @param key      the {@link MessageKey} carrying the wire id and parent JID
     * @throws NullPointerException                           if any argument is
     *                                                        {@code null}
     * @throws IllegalArgumentException                       if the key has no id
     * @throws WhatsAppMessageException.Send.InvalidRecipient if {@code groupJid}
     *                                                        is not a group or
     *                                                        community
     */
    void sendKeyDistribution(Jid groupJid, MessageKey key);

    /**
     * Dispatches a peer protocol message to one of the user's own devices.
     *
     * <p>Peer messages cover app-state sync, key shares, fatal-exception
     * notifications, and peer-data operation requests and responses; the wire
     * stanza is encrypted per device and tagged with {@code category="peer"} so
     * the server routes it to the linked-device shelf rather than to the main
     * chat fanout.
     *
     * @implSpec
     * Implementations must encrypt the stanza per target device and tag it with the
     * {@code peer} category rather than the chat fanout category.
     *
     * @param targetDevice the target device {@link Jid}, typically the user's
     *                     primary device
     * @param messageInfo  the {@link ChatMessageInfo} wrapping the protocol
     *                     payload
     * @return the parsed server {@link AckResult}
     * @throws NullPointerException if any argument is {@code null}
     */
    AckResult sendPeer(Jid targetDevice, ChatMessageInfo messageInfo);
}
