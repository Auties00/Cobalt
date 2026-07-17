package com.github.auties00.cobalt.quarantine;

import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.QuarantinedMessage;
import com.github.auties00.cobalt.wire.linked.privacy.DefenseModePrivacyValue;
import com.github.auties00.cobalt.wire.linked.privacy.PrivacySettingType;
import com.github.auties00.cobalt.wire.linked.props.ABProp;

/**
 * Classifies inbound messages against WhatsApp's Defense Mode quarantine policy.
 *
 * <p>Defense Mode withholds unsolicited content from senders that are not in the user's
 * address book while the feature is enabled. This service decides, per inbound message,
 * whether the message must be quarantined and, when so, the plain text to surface in its
 * place. The decision is the conjunction of three gates and one content classification:
 * <ul>
 *   <li>the {@link ABProp#DEFENSE_MODE_QUARANTINE} feature flag is enabled;</li>
 *   <li>Defense Mode is active ({@link ABProp#DEFENSE_MODE_AVAILABLE} is at least one and the
 *       {@link PrivacySettingType#DEFENSE_MODE} setting is
 *       {@link DefenseModePrivacyValue.OnStandard});</li>
 *   <li>the sender is quarantinable (a non-self user that is not a saved contact);</li>
 *   <li>the message content is quarantinable (media, or a structured message carrying media,
 *       rather than plain text or a non-content control message).</li>
 * </ul>
 */
public interface QuarantineService {
    /**
     * Returns the quarantine action for an inbound message from the given sender.
     *
     * <p>Returns {@link QuarantineAction#NO_QUARANTINE} unless every gate passes and the content
     * is quarantinable, in which case it returns {@link QuarantineAction#WITHOUT_TEXT} or a
     * {@linkplain QuarantineAction#of(String) with-text} action carrying the caption or
     * link-preview text to display in place of the withheld message.
     *
     * @implSpec Implementations return {@link QuarantineAction#NO_QUARANTINE} when the message is
     *           {@code null}, when any of the {@link ABProp#DEFENSE_MODE_QUARANTINE} flag, the
     *           active-Defense-Mode gate or the quarantinable-sender gate fails, and otherwise the
     *           action that classifies the displayable content.
     * @param message the inbound message, or {@code null}
     * @param sender  the message sender, or {@code null}
     * @return the quarantine action, never {@code null}
     */
    QuarantineAction getQuarantineAction(LinkedMessageContainer message, Jid sender);

    /**
     * Quarantines an inbound message that Defense Mode classifies as unsolicited.
     *
     * <p>When {@link #getQuarantineAction(LinkedMessageContainer, Jid)} returns a quarantine verdict, the
     * message's original payload and the extracted replacement text are preserved on the
     * {@link ChatMessageInfo} through
     * {@link ChatMessageInfo#setQuarantinedMessage(QuarantinedMessage)}, its key is recorded for
     * {@link #restoreAll()}, and a {@code QUARANTINED_MSG} metric is committed. The caller is
     * responsible for withholding a quarantined message from the {@code onNewMessage} listeners.
     *
     * @implSpec Implementations quarantine exactly when
     *           {@link #getQuarantineAction(LinkedMessageContainer, Jid)} for the message's content and
     *           sender returns a quarantine verdict, and return {@code false} without side effects
     *           otherwise.
     * @param info the parsed inbound chat message
     * @return {@code true} when the message was quarantined, {@code false} otherwise
     */
    boolean quarantine(ChatMessageInfo info);

    /**
     * Restores a single quarantined message, re-delivering it to the {@code onNewMessage}
     * listeners and committing the {@code QUARANTINE_RESTORE_SUCCESS} or
     * {@code QUARANTINE_RESTORE_FAILED} metric.
     *
     * @implSpec Implementations clear the quarantine flag and re-deliver the message, committing
     *           the success or failure metric according to the outcome; a message that was not
     *           quarantined restores to {@code false}.
     * @param info the quarantined chat message to restore
     * @return {@code true} when the message was restored, {@code false} otherwise
     */
    boolean restore(ChatMessageInfo info);

    /**
     * Restores every message quarantined during this session, committing a single
     * {@code QUARANTINE_RESTORE_AUTO} metric carrying the restored count.
     *
     * <p>Invoked when Defense Mode transitions off so that withheld messages are released back to
     * the chat.
     *
     * @implSpec Implementations re-deliver every message quarantined during the current session and
     *           commit the auto-restore metric only when at least one message was restored.
     */
    void restoreAll();
}
