package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Emoji-usage configuration attached to a WhatsApp Business AI agent rule.
 *
 * <p>A rule may tune how liberally the auto-reply assistant decorates its
 * replies with emojis. This model carries that single tuning knob, expressed
 * as the frequency band the merchant selected.
 */
@ProtobufMessage(name = "AiEmojisConfig")
public final class AiEmojisConfig {
    /**
     * Frequency band controlling how often the assistant uses emojis in its
     * replies. Empty when the merchant left it unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String emojisFreq;

    /**
     * Constructs a new {@code AiEmojisConfig}. The {@code emojisFreq} may be
     * {@code null} to leave the frequency band unset.
     *
     * @param emojisFreq the emoji-frequency band, or {@code null}
     */
    AiEmojisConfig(String emojisFreq) {
        this.emojisFreq = emojisFreq;
    }

    /**
     * Returns the frequency band controlling how often the assistant uses
     * emojis.
     *
     * @return an {@link Optional} carrying the frequency band, or empty when
     *         unset
     */
    public Optional<String> emojisFreq() {
        return Optional.ofNullable(emojisFreq);
    }
}
