package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Automatic-reply settings of a WhatsApp Business AI agent.
 *
 * <p>The WhatsApp Business AI agent is the auto-reply assistant a merchant
 * attaches to their business account. Two settings govern when it speaks:
 * which chats it is allowed to reply to, and during which part of the day it
 * is active.
 *
 * <p>The chat scope is named by {@link #triggerChatType()}, a server-defined
 * marker selecting the set of chats the assistant answers (for example only
 * chats with no prior history, or every chat). The daily active window is
 * described by {@link #enabled()}, {@link #fromSecondOfDay()},
 * {@link #toSecondOfDay()}, and {@link #timeZone()}: when the window is
 * enabled, the assistant only replies between the two seconds-into-day
 * offsets, interpreted in the configured time zone.
 */
@ProtobufMessage(name = "BusinessAiReplySettings")
public final class BusinessAiReplySettings {
    /**
     * Server-defined marker naming the set of chats the assistant replies
     * to. The full value set is not recoverable from the WhatsApp client, so
     * the raw marker is exposed as a string. Empty when the server omitted
     * it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String triggerChatType;

    /**
     * Whether the daily active-hours window is enforced. When {@code false}
     * the assistant is active all day and the window bounds do not apply.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean enabled;

    /**
     * Start of the daily active window, as seconds elapsed since the start
     * of the day in {@link #timeZone()}. Absent when the server did not
     * publish a window start.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final Long fromSecondOfDay;

    /**
     * End of the daily active window, as seconds elapsed since the start of
     * the day in {@link #timeZone()}. Absent when the server did not publish
     * a window end.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    final Long toSecondOfDay;

    /**
     * Identifier of the time zone the daily window is anchored to (for
     * example {@code "Europe/Rome"}). Empty when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String timeZone;

    /**
     * Constructs a new {@code BusinessAiReplySettings}. Every argument may
     * be {@code null} when the server omitted the corresponding field; a
     * {@code null} {@code enabled} is read as {@code false}.
     *
     * @param triggerChatType the chat-scope marker, or {@code null}
     * @param enabled         whether the daily window is enforced, or {@code null}
     * @param fromSecondOfDay the window start in seconds-into-day, or {@code null}
     * @param toSecondOfDay   the window end in seconds-into-day, or {@code null}
     * @param timeZone        the window time zone identifier, or {@code null}
     */
    BusinessAiReplySettings(String triggerChatType, Boolean enabled, Long fromSecondOfDay, Long toSecondOfDay, String timeZone) {
        this.triggerChatType = triggerChatType;
        this.enabled = enabled != null && enabled;
        this.fromSecondOfDay = fromSecondOfDay;
        this.toSecondOfDay = toSecondOfDay;
        this.timeZone = timeZone;
    }

    /**
     * Returns the chat-scope marker naming the set of chats the assistant
     * replies to.
     *
     * @return the chat-scope marker, or empty when the server omitted it
     */
    public Optional<String> triggerChatType() {
        return Optional.ofNullable(triggerChatType);
    }

    /**
     * Returns whether the daily active-hours window is enforced.
     *
     * @return {@code true} when the assistant only replies inside the
     *         configured daily window, {@code false} when it is active all
     *         day
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Returns the start of the daily active window, in seconds since the
     * start of the day.
     *
     * @return the window start, or empty when the server omitted it
     */
    public OptionalLong fromSecondOfDay() {
        return fromSecondOfDay != null ? OptionalLong.of(fromSecondOfDay) : OptionalLong.empty();
    }

    /**
     * Returns the end of the daily active window, in seconds since the start
     * of the day.
     *
     * @return the window end, or empty when the server omitted it
     */
    public OptionalLong toSecondOfDay() {
        return toSecondOfDay != null ? OptionalLong.of(toSecondOfDay) : OptionalLong.empty();
    }

    /**
     * Returns the time-zone identifier the daily window is anchored to.
     *
     * @return the time-zone identifier, or empty when the server omitted it
     */
    public Optional<String> timeZone() {
        return Optional.ofNullable(timeZone);
    }
}
