package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumerates the optional features that can be activated on a WhatsApp
 * Channel (the underlying entity that powers a "newsletter").
 *
 * <p>Channels are one-to-many broadcast surfaces in which an admin pushes
 * announcements to a roster of followers. Beyond the basic publish flow,
 * the server selectively turns on extra features for individual channels:
 * insights dashboards, polls, quizzes, music attachments, status producer
 * APIs, contextual onboarding cards, sticker pack sharing, and more.
 * Whenever the relay describes a channel, it returns a list of the
 * capability tokens currently enabled for it; the client uses those tokens
 * to reveal or hide the matching UI affordances.
 *
 * <p>Each constant is paired with a stable protobuf wire index so the
 * enum can be persisted in serialized state. {@link #of(String)} performs
 * a case-insensitive lookup against the constant name and falls back to
 * {@link #UNKNOWN} when the relay returns a token this client does not
 * yet recognise.
 */
@ProtobufEnum
public enum NewsletterCapability {
    /**
     * The capability was not reported by the server or is not recognized
     * by this version of the client.
     */
    UNKNOWN(0),

    /**
     * The channel exposes analytics and insights dashboards to its admins.
     */
    INSIGHTS(1),

    /**
     * Admins may publish polls that include photo attachments.
     */
    PHOTO_POLLS(2),

    /**
     * Admins may publish question posts that collect text responses from
     * followers.
     */
    QUESTIONS(3),

    /**
     * The channel receives admin-targeted server notifications.
     */
    ADMIN_NOTIFICATIONS(4),

    /**
     * The admin UI surfaces a dedicated button for inviting additional
     * admins to the channel.
     */
    INVITE_ADMINS_BUTTON(5),

    /**
     * Admins may generate invite links or QR codes aimed at followers.
     */
    INVITE_FOLLOWERS(6),

    /**
     * Admins may publish quiz posts with multiple-choice questions.
     */
    QUIZ(7),

    /**
     * The first admin context card surfaced in the admin onboarding flow.
     */
    ADMIN_CONTEXT_CARD_1(8),

    /**
     * The second admin context card surfaced in the admin onboarding flow.
     */
    ADMIN_CONTEXT_CARD_2(9),

    /**
     * The third admin context card surfaced in the admin onboarding flow.
     */
    ADMIN_CONTEXT_CARD_3(10),

    /**
     * Admins may share sticker packs directly into the channel.
     */
    SHARE_STICKER_PACKS(11),

    /**
     * The initial admin onboarding flow is enabled for this channel.
     */
    ADMIN_ONBOARDING(12),

    /**
     * The second-iteration admin onboarding flow is enabled for this
     * channel.
     */
    ADMIN_ONBOARDING_2(13),

    /**
     * Admins may attach music tracks to published messages.
     */
    MUSIC(14),

    /**
     * The client surfaces a tooltip that advertises newly-supported
     * message types.
     */
    NEW_MESSAGE_TYPES_TOOLTIP(15),

    /**
     * The client surfaces a nudge encouraging admins to pin important
     * messages.
     */
    PINNING_NUDGE(16),

    /**
     * The client surfaces the thread-menu feature inside the channel view.
     */
    THREAD_MENU(17),

    /**
     * The channel exposes the admin-profile attribution feature, which
     * surfaces the human admin behind a post.
     */
    ADMIN_PROFILE(18),

    /**
     * The channel exposes the channel-status producer feature, letting
     * admins publish ephemeral status updates tied to the channel.
     */
    CHANNEL_STATUS_PRODUCER(19);

    /**
     * Lookup table from the lowercase enum name to the constant, used by
     * {@link #of(String)} for case-insensitive parsing.
     */
    private static final Map<String, NewsletterCapability> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(key -> key.name().toLowerCase(), Function.identity()));

    /**
     * The protobuf wire index associated with this constant.
     */
    final int index;

    /**
     * Constructs a new enum constant bound to the supplied protobuf wire
     * index.
     *
     * @param index the protobuf wire index
     */
    NewsletterCapability(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the constant whose name matches the supplied string,
     * case-insensitively.
     *
     * @param name the capability name as reported by the server, may be
     *             {@code null}
     * @return the matching capability constant, or {@link #UNKNOWN} when
     *         {@code name} is {@code null} or does not match any constant
     */
    public static NewsletterCapability of(String name) {
        return name == null ? UNKNOWN : BY_NAME.getOrDefault(name.toLowerCase(), UNKNOWN);
    }

    /**
     * Returns the name of this enum constant.
     *
     * @return the constant name as written in source code
     */
    @Override
    public String toString() {
        return name();
    }
}
