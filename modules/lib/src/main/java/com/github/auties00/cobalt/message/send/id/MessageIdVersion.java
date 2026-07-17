package com.github.auties00.cobalt.message.send.id;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

/**
 * Identifies the algorithm a {@link MessageIdGenerator#generate(MessageIdVersion, com.github.auties00.cobalt.wire.core.jid.Jid)}
 * call uses to produce a WhatsApp stanza id.
 * <p>
 * {@link #V2} is the algorithm used for normal operation; {@link #V1} exists for the SHA-256-unavailable fallback path
 * inside {@link MessageIdGenerator#generate(MessageIdVersion, com.github.auties00.cobalt.wire.core.jid.Jid)} and is not
 * normally selected directly.
 *
 * @see MessageIdGenerator
 */
@WhatsAppWebModule(moduleName = "WAWebMsgKey")
public enum MessageIdVersion {
    /**
     * Selects the deprecated random-only algorithm.
     * <p>
     * The id is the prefix {@value MessageIdGenerator#PREFIX} followed by 16 uppercase hex characters drawn from 8
     * random bytes. {@link MessageIdGenerator#generate(MessageIdVersion, com.github.auties00.cobalt.wire.core.jid.Jid)}
     * uses this version as the fallback when {@link #V2} is requested but SHA-256 is unavailable.
     */
    V1,

    /**
     * Selects the current SHA-256 based algorithm.
     * <p>
     * The id is the prefix {@value MessageIdGenerator#PREFIX} followed by 18 uppercase hex characters taken from the
     * first 9 bytes of {@code SHA256(int64(unixTime) || utf8(senderJid) || random(16))}. The sender JID is mixed into
     * the pre-image so two accounts sending at the same instant cannot collide on identical random bytes.
     */
    V2
}
