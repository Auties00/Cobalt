package com.github.auties00.cobalt.wire.linked.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.stream.Collectors;

/**
 * Identifies a privacy setting that a WhatsApp user can configure on their account.
 *
 * <p>Each constant is parameterized over the {@link PrivacySettingValue} sub-interface that
 * enumerates the values the setting accepts, so the type system pairs a setting with its
 * legal audiences: {@link #LAST_SEEN} is a {@code PrivacySettingType<LastSeenPrivacyValue>}
 * and can therefore only ever be paired with a {@link LastSeenPrivacyValue}. The nine
 * constants are exactly the categories the relay's {@code <iq xmlns="privacy">} surface
 * recognises.
 *
 * <p>The {@link #wire()} token matches the server's {@code <category name=...>} attribute
 * and round-trips through {@link #of(String)}; {@link #parse(String, List)} resolves a
 * server value token (and its refinement list) into the concrete {@link PrivacySettingValue}
 * for this setting.
 *
 * @param <V>  the {@link PrivacySettingValue} sub-interface accepted by this setting
 * @param wire the server-side token used in the {@code <category name=...>} attribute
 */
public record PrivacySettingType<V extends PrivacySettingValue>(String wire) {
    /**
     * Controls who can see the timestamp of the last time the user was online.
     */
    public static final PrivacySettingType<LastSeenPrivacyValue> LAST_SEEN =
            new PrivacySettingType<>("last");

    /**
     * Controls who can see whether the user is currently online.
     */
    public static final PrivacySettingType<OnlinePrivacyValue> ONLINE =
            new PrivacySettingType<>("online");

    /**
     * Controls who can see the user's profile picture.
     */
    public static final PrivacySettingType<ProfilePicturePrivacyValue> PROFILE_PICTURE =
            new PrivacySettingType<>("profile");

    /**
     * Controls who can see the user's about text.
     *
     * <p>This is the about-text visibility, not the Status story feed audience, which is a
     * separate setting carried by {@link StatusPrivacySetting}.
     */
    public static final PrivacySettingType<AboutPrivacyValue> ABOUT =
            new PrivacySettingType<>("status");

    /**
     * Controls whether read receipts are exchanged for one-to-one chats.
     */
    public static final PrivacySettingType<ReadReceiptsPrivacyValue> READ_RECEIPTS =
            new PrivacySettingType<>("readreceipts");

    /**
     * Controls who can add the user to new group chats without an invitation.
     */
    public static final PrivacySettingType<GroupAddPrivacyValue> GROUP_ADD =
            new PrivacySettingType<>("groupadd");

    /**
     * Controls who can add the user to ongoing voice or video calls.
     */
    public static final PrivacySettingType<CallAddPrivacyValue> CALL_ADD =
            new PrivacySettingType<>("calladd");

    /**
     * Controls who can message the user without a prior conversation.
     */
    public static final PrivacySettingType<MessagesPrivacyValue> MESSAGES =
            new PrivacySettingType<>("messages");

    /**
     * Controls WhatsApp's Defense Mode, which quarantines unsolicited messages from senders
     * that are not in the user's address book.
     */
    public static final PrivacySettingType<DefenseModePrivacyValue> DEFENSE_MODE =
            new PrivacySettingType<>("defense");

    /**
     * The immutable registry of every known setting, used by {@link #of(String)} and
     * {@link #values()}.
     */
    private static final List<PrivacySettingType<?>> VALUES = List.of(
            LAST_SEEN, ONLINE, PROFILE_PICTURE, ABOUT, READ_RECEIPTS, GROUP_ADD, CALL_ADD, MESSAGES, DEFENSE_MODE);

    /**
     * The {@link #VALUES} registry indexed by {@link #wire()} token for constant-time
     * {@link #of(String)} resolution.
     */
    private static final Map<String, PrivacySettingType<?>> BY_WIRE = VALUES.stream()
            .collect(Collectors.toUnmodifiableMap(PrivacySettingType::wire, value -> value));

    /**
     * Returns the immutable list of every known privacy setting.
     *
     * @return the registry of settings, never {@code null}
     */
    public static SequencedCollection<PrivacySettingType<?>> values() {
        return VALUES;
    }

    /**
     * Resolves a setting from the wire token used in a {@code <category name=...>} attribute.
     *
     * @param wire the wire token to resolve, for example {@code "last"} or {@code "defense"}
     * @return the matching setting, or empty if no setting uses the given token
     */
    public static Optional<PrivacySettingType<?>> of(String wire) {
        return Optional.ofNullable(BY_WIRE.get(wire));
    }

    /**
     * Resolves a value token (and its refinement list) into the concrete value for this
     * setting.
     *
     * @param token    the server value token, for example {@code "all"} or {@code "contact_blacklist"}
     * @param excluded the refinement list for the contacts-except value, otherwise ignored
     * @return the resolved value, or empty if the token is not accepted by this setting
     */
    public Optional<? extends PrivacySettingValue> parse(String token, List<Jid> excluded) {
        return switch (wire) {
            case "last" -> LastSeenPrivacyValue.of(token, excluded);
            case "online" -> OnlinePrivacyValue.of(token, excluded);
            case "profile" -> ProfilePicturePrivacyValue.of(token, excluded);
            case "status" -> AboutPrivacyValue.of(token, excluded);
            case "readreceipts" -> ReadReceiptsPrivacyValue.of(token, excluded);
            case "groupadd" -> GroupAddPrivacyValue.of(token, excluded);
            case "calladd" -> CallAddPrivacyValue.of(token, excluded);
            case "messages" -> MessagesPrivacyValue.of(token, excluded);
            case "defense" -> DefenseModePrivacyValue.of(token, excluded);
            default -> Optional.empty();
        };
    }

    /**
     * Compares this setting to another for equality by {@link #wire()} token.
     *
     * @param o the object to compare against
     * @return {@code true} if the given object is a setting with the same wire token
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof PrivacySettingType<?>(var thatWire) && wire.equals(thatWire);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}, derived from the wire token.
     *
     * @return the wire token hash code
     */
    @Override
    public int hashCode() {
        return wire.hashCode();
    }

    /**
     * Returns a concise representation naming the wire token.
     *
     * @return a string representation of this setting
     */
    @Override
    public String toString() {
        return "PrivacySettingType[" + wire + "]";
    }
}
