package com.github.auties00.cobalt.wire.linked.business.aichannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Current activity status of a WhatsApp GenAI agent channel.
 *
 * <p>A creator can connect a generative-AI agent to a WhatsApp channel so its
 * followers can chat with the agent. The agent reports a live activity status
 * (for example, "online", "busy") that the channel UI renders next to the
 * agent's name. This model is that activity status: the machine-readable
 * {@linkplain #activityCode() activity code} and the human-readable
 * {@linkplain #activityText() activity text}.
 */
@ProtobufMessage(name = "AiChannelAgentStatus")
public final class AiChannelAgentStatus {
    /**
     * Machine-readable activity code, or {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String activityCode;

    /**
     * Human-readable activity text rendered to the user, or {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String activityText;

    /**
     * Constructs a new {@code AiChannelAgentStatus}. Any reference argument
     * may be {@code null} when the server omitted the corresponding field.
     *
     * @param activityCode the machine-readable activity code, or {@code null}
     * @param activityText the human-readable activity text, or {@code null}
     */
    AiChannelAgentStatus(String activityCode, String activityText) {
        this.activityCode = activityCode;
        this.activityText = activityText;
    }

    /**
     * Returns the machine-readable activity code.
     *
     * @return the activity code, or empty when the server omitted it
     */
    public Optional<String> activityCode() {
        return Optional.ofNullable(activityCode);
    }

    /**
     * Returns the human-readable activity text rendered to the user.
     *
     * @return the activity text, or empty when the server omitted it
     */
    public Optional<String> activityText() {
        return Optional.ofNullable(activityText);
    }
}
