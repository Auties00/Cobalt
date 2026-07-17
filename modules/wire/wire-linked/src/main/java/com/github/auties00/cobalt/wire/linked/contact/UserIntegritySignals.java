package com.github.auties00.cobalt.wire.linked.contact;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Server-issued safety signals attached to a peer at the start of the
 * first-message-experience (FMX) flow.
 *
 * <p>WhatsApp's trust-and-safety pipeline evaluates each conversation
 * partner against several signals when the local user is about to start
 * a brand-new chat with them. The two surfaced flags drive the
 * user-visible nudges: {@link #isNewAccount()} backs the "new on
 * WhatsApp" badge shown on unfamiliar contacts, and
 * {@link #isSuspicious()} backs the spam-warning sheet that asks the
 * user to confirm before sending the first outbound message.
 *
 * <p>Both flags are tri-state on the wire — {@code true},
 * {@code false}, or absent. An absent value means the relay had no
 * signal to publish for this peer (for example because the peer is not
 * visible to integrity scoring) and the UI should fall back to its
 * default pre-FMX behaviour rather than treat the absence as
 * {@code false}.
 */
@ProtobufMessage
public final class UserIntegritySignals {
    /**
     * The {@code is_new_account} scalar, surfacing whether the relay
     * considers the peer a recently-registered WhatsApp account.
     *
     * <p>{@code null} when the relay omitted the flag.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean newAccount;

    /**
     * The {@code is_suspicious_start_chat} scalar, surfacing whether
     * the relay's safety pipeline has flagged the peer as a
     * suspicious conversation target.
     *
     * <p>{@code null} when the relay omitted the flag.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean suspicious;

    /**
     * Constructs a new {@code UserIntegritySignals} carrying the two
     * relay-issued flags.
     *
     * @param newAccount the {@code is_new_account} scalar, or
     *                   {@code null} if the relay omitted it
     * @param suspicious the {@code is_suspicious_start_chat} scalar,
     *                   or {@code null} if the relay omitted it
     */
    UserIntegritySignals(Boolean newAccount, Boolean suspicious) {
        this.newAccount = newAccount;
        this.suspicious = suspicious;
    }

    /**
     * Returns whether the relay considers the peer a recently-
     * registered account.
     *
     * @return an {@link Optional} carrying the flag, or empty when
     *         the relay omitted it
     */
    public Optional<Boolean> isNewAccount() {
        return Optional.ofNullable(newAccount);
    }

    /**
     * Returns whether the relay's safety pipeline has flagged the
     * peer as a suspicious conversation target.
     *
     * @return an {@link Optional} carrying the flag, or empty when
     *         the relay omitted it
     */
    public Optional<Boolean> isSuspicious() {
        return Optional.ofNullable(suspicious);
    }

    /**
     * Sets the {@code is_new_account} flag.
     *
     * @param newAccount the new flag, or {@code null} to clear
     */
    public void setNewAccount(Boolean newAccount) {
        this.newAccount = newAccount;
    }

    /**
     * Sets the {@code is_suspicious_start_chat} flag.
     *
     * @param suspicious the new flag, or {@code null} to clear
     */
    public void setSuspicious(Boolean suspicious) {
        this.suspicious = suspicious;
    }
}
