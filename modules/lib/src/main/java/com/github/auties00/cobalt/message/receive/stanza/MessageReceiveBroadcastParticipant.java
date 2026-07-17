package com.github.auties00.cobalt.message.receive.stanza;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;
import java.util.Optional;

/**
 * One entry of the broadcast-contact list parsed from a {@code <to>} child
 * inside the {@code <participants>} stanza of an inbound peer-broadcast or
 * status-broadcast message stanza.
 *
 * <p>Populated for {@link MessageType#PEER_BROADCAST} and
 * {@link MessageType#DIRECT_PEER_STATUS} stanzas where the server attaches the
 * original recipient list so the local device can mirror what the primary
 * device sent. The LID/PN/username triple carries the per-recipient mapping
 * fixed up during the LID-for-PN migration: each entry can be addressed by
 * phone-number JID, by LID, by username, or by any combination, and
 * {@link #recipientLatestLid()} carries the freshest LID the server knows for
 * that recipient. Instances are produced by {@link MessageReceiveStanzaParser}
 * and consumed via {@link MessageReceiveStanza#bclParticipants()}.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgParser")
public final class MessageReceiveBroadcastParticipant {
    /**
     * The recipient's primary JID parsed from the {@code jid} attribute of the
     * {@code <to>} stanza.
     */
    private final Jid jid;

    /**
     * The {@code eph_setting} attribute, encoding the ephemeral-message setting
     * active for this recipient at send time.
     */
    private final String ephSetting;

    /**
     * The recipient's LID from the {@code peer_recipient_lid} attribute.
     */
    private final Jid peerRecipientLid;

    /**
     * The recipient's phone-number JID from the {@code peer_recipient_pn}
     * attribute.
     */
    private final Jid peerRecipientPn;

    /**
     * The recipient's username from the {@code peer_recipient_username}
     * attribute.
     */
    private final String peerRecipientUsername;

    /**
     * The freshest LID known by the server for this recipient, from the
     * {@code recipient_latest_lid} attribute.
     */
    private final Jid recipientLatestLid;

    /**
     * Constructs a populated record from the values extracted by
     * {@link MessageReceiveStanzaParser}.
     *
     * @param jid                   the primary recipient JID, never {@code null}
     * @param ephSetting            the ephemeral setting, or {@code null}
     * @param peerRecipientLid      the recipient LID, or {@code null}
     * @param peerRecipientPn       the recipient phone-number JID, or {@code null}
     * @param peerRecipientUsername the recipient username, or {@code null}
     * @param recipientLatestLid    the latest LID, or {@code null}
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public MessageReceiveBroadcastParticipant(
            Jid jid,
            String ephSetting,
            Jid peerRecipientLid,
            Jid peerRecipientPn,
            String peerRecipientUsername,
            Jid recipientLatestLid
    ) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.ephSetting = ephSetting;
        this.peerRecipientLid = peerRecipientLid;
        this.peerRecipientPn = peerRecipientPn;
        this.peerRecipientUsername = peerRecipientUsername;
        this.recipientLatestLid = recipientLatestLid;
    }

    /**
     * Returns the recipient's primary JID.
     *
     * <p>Always populated; serves as the canonical recipient identifier when
     * persisting the broadcast-contact list alongside the message.
     *
     * @return the recipient JID
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Returns the per-recipient ephemeral setting, when present.
     *
     * <p>Captures the ephemeral-message setting active for that one recipient
     * at the moment the primary device sent the broadcast, letting the local
     * device mirror the disappearing-message duration without re-querying.
     *
     * @return an {@link Optional} wrapping the ephemeral setting
     */
    public Optional<String> ephSetting() {
        return Optional.ofNullable(ephSetting);
    }

    /**
     * Returns the recipient's LID, when present.
     *
     * <p>Pairs with {@link #peerRecipientPn()} for the LID/PN mapping used
     * during the LID-for-PN migration; either or both may be present depending
     * on what the server knows.
     *
     * @return an {@link Optional} wrapping the recipient LID
     */
    public Optional<Jid> peerRecipientLid() {
        return Optional.ofNullable(peerRecipientLid);
    }

    /**
     * Returns the recipient's phone-number JID, when present.
     *
     * <p>Pairs with {@link #peerRecipientLid()} for the LID/PN mapping.
     *
     * @return an {@link Optional} wrapping the recipient phone-number JID
     */
    public Optional<Jid> peerRecipientPn() {
        return Optional.ofNullable(peerRecipientPn);
    }

    /**
     * Returns the recipient's username, when present.
     *
     * <p>Populated only when the username-display gate is on, letting the UI
     * surface the contact by their public username.
     *
     * @return an {@link Optional} wrapping the recipient username
     */
    public Optional<String> peerRecipientUsername() {
        return Optional.ofNullable(peerRecipientUsername);
    }

    /**
     * Returns the freshest LID the server knows for this recipient, when
     * present.
     *
     * <p>If this is present but both {@link #peerRecipientLid()} and
     * {@link #peerRecipientPn()} are absent the server has shipped a fresh LID
     * without the mapping context needed to associate it.
     *
     * @return an {@link Optional} wrapping the latest LID
     */
    public Optional<Jid> recipientLatestLid() {
        return Optional.ofNullable(recipientLatestLid);
    }
}
