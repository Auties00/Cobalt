package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Optional;

/**
 * Welcome-message configuration for a Click-to-WhatsApp ad's opening conversation.
 *
 * <p>When a user taps a Click-to-WhatsApp ad, the resulting chat can open with a configured welcome
 * experience. This model carries that configuration: a {@link #greeting() greeting}, a set of
 * {@link #icebreakers() icebreaker} prompts (with an {@link #icebreakersEnabled() enabled} flag), and
 * a {@link #prefill() prefilled} first message (with {@link #prefillEnabled() enabled} and
 * {@link #prefillMessageEdited() edited} flags).
 */
@ProtobufMessage(name = "MessengerWelcomeMessage")
public final class MessengerWelcomeMessage {
    /**
     * Greeting shown at the top of the opening conversation. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String greeting;

    /**
     * Icebreaker prompts offered to the user, in the order they are sent. Never {@code null}, possibly
     * empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> icebreakers;

    /**
     * Whether the icebreaker prompts are enabled.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean icebreakersEnabled;

    /**
     * Prefilled first message the user can send. Empty when unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String prefill;

    /**
     * Whether the prefilled first message is enabled.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final boolean prefillEnabled;

    /**
     * Whether the prefilled first message has been edited by the merchant.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final boolean prefillMessageEdited;

    /**
     * Constructs a new {@code MessengerWelcomeMessage}. A {@code null} {@code icebreakers} is coerced
     * to an empty list; the {@code greeting} and {@code prefill} may be {@code null} to leave them
     * unset.
     *
     * @param greeting             the greeting, or {@code null}
     * @param icebreakers          the icebreaker prompts; {@code null} treated as empty
     * @param icebreakersEnabled   whether icebreakers are enabled
     * @param prefill              the prefilled first message, or {@code null}
     * @param prefillEnabled       whether the prefilled message is enabled
     * @param prefillMessageEdited whether the prefilled message has been edited
     */
    MessengerWelcomeMessage(String greeting, List<String> icebreakers, boolean icebreakersEnabled,
                            String prefill, boolean prefillEnabled, boolean prefillMessageEdited) {
        this.greeting = greeting;
        this.icebreakers = icebreakers == null ? List.of() : List.copyOf(icebreakers);
        this.icebreakersEnabled = icebreakersEnabled;
        this.prefill = prefill;
        this.prefillEnabled = prefillEnabled;
        this.prefillMessageEdited = prefillMessageEdited;
    }

    /**
     * Returns the greeting shown at the top of the opening conversation.
     *
     * @return an {@link Optional} carrying the greeting, or empty when unset
     */
    public Optional<String> greeting() {
        return Optional.ofNullable(greeting);
    }

    /**
     * Returns the icebreaker prompts offered to the user.
     *
     * @return an unmodifiable view of the icebreakers; never {@code null}, possibly empty
     */
    public List<String> icebreakers() {
        return icebreakers;
    }

    /**
     * Returns whether the icebreaker prompts are enabled.
     *
     * @return {@code true} when icebreakers are enabled, {@code false} otherwise
     */
    public boolean icebreakersEnabled() {
        return icebreakersEnabled;
    }

    /**
     * Returns the prefilled first message the user can send.
     *
     * @return an {@link Optional} carrying the prefill, or empty when unset
     */
    public Optional<String> prefill() {
        return Optional.ofNullable(prefill);
    }

    /**
     * Returns whether the prefilled first message is enabled.
     *
     * @return {@code true} when the prefill is enabled, {@code false} otherwise
     */
    public boolean prefillEnabled() {
        return prefillEnabled;
    }

    /**
     * Returns whether the prefilled first message has been edited by the merchant.
     *
     * @return {@code true} when the prefill has been edited, {@code false} otherwise
     */
    public boolean prefillMessageEdited() {
        return prefillMessageEdited;
    }
}
