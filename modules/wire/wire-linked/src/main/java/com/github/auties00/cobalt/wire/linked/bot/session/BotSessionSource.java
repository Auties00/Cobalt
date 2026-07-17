package com.github.auties00.cobalt.wire.linked.bot.session;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Enumerates the origins that can trigger the creation of a bot conversation
 * session on WhatsApp.
 *
 * <p>Each constant identifies a distinct user action or system event that
 * started the session. The value is recorded in
 * {@link BotSessionMetadata#sessionSource()} and used by the server to tailor
 * the conversation flow (for example, pre-populating suggestions for a
 * {@link #TYPEAHEAD} session or optimizing latency for a {@link #VOICE}
 * session).
 */
@ProtobufEnum(name = "BotSessionSource")
public enum BotSessionSource {
    /**
     * No session source was specified.
     */
    NONE(0),

    /**
     * The session was started from the AI null state (empty conversation
     * screen before any user input).
     */
    NULL_STATE(1),

    /**
     * The session was started from a type-ahead (autocomplete) suggestion
     * in the search bar.
     */
    TYPEAHEAD(2),

    /**
     * The session was started by direct user text input in the compose box.
     */
    USER_INPUT(3),

    /**
     * The session was started by an Emu Flash interaction (Meta AI's
     * proactive quick-response feature).
     */
    EMU_FLASH(4),

    /**
     * The session is a follow-up to a previous Emu Flash interaction.
     */
    EMU_FLASH_FOLLOWUP(5),

    /**
     * The session was started via a voice input interaction.
     */
    VOICE(6),

    /**
     * The session was started from the AI Home screen surface.
     */
    AI_HOME_SESSION(7);

    /**
     * Constructs a new session source constant with the specified protobuf
     * index.
     *
     * @param index the protobuf enum index
     */
    BotSessionSource(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    final int index;

    /**
     * Returns the protobuf enum index of this session source.
     *
     * @return the protobuf index
     */
    public int index() {
        return this.index;
    }
}
