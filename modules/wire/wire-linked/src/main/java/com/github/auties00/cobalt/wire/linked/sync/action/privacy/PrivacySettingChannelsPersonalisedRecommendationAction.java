package com.github.auties00.cobalt.wire.linked.sync.action.privacy;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Records the user's preference to opt out of personalised channel
 * recommendations on WhatsApp.
 *
 * <p>When the user disables personalised channel recommendations from any
 * linked device, WhatsApp persists this preference as a sync action so that
 * the choice is mirrored on every other linked device. The action carries a
 * single boolean flag that indicates whether the user has actively opted
 * out.
 *
 * <p>This type is a sync action payload and is intended to be read or
 * produced when synchronising privacy settings. It is not instantiated by
 * application code directly; instances are deserialised from the protobuf
 * wire format or constructed through the generated builder.
 */
@ProtobufMessage(name = "SyncActionValue.PrivacySettingChannelsPersonalisedRecommendationAction")
public final class PrivacySettingChannelsPersonalisedRecommendationAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The canonical action name used to identify this sync action on the
     * wire.
     */
    public static final String ACTION_NAME = "setting_channels_personalised_recommendation_optout";

    /**
     * The canonical protocol version of this sync action.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Returns the canonical action name used to identify this sync action on
     * the wire.
     *
     * @return the action name {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical protocol version of this sync action.
     *
     * @return the action version {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Flag indicating whether the user has opted out of personalised channel
     * recommendations.
     *
     * <p>A {@code null} value means the field was absent on the wire.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean isUserOptedOut;


    /**
     * Constructs a new action carrying the supplied opt-out flag.
     *
     * @param isUserOptedOut the opt-out flag to persist, or {@code null} if
     *                       the field is absent
     */
    PrivacySettingChannelsPersonalisedRecommendationAction(Boolean isUserOptedOut) {
        this.isUserOptedOut = isUserOptedOut;
    }

    /**
     * Returns whether the user has opted out of personalised channel
     * recommendations.
     *
     * <p>An absent value on the wire is coalesced to {@code false},
     * consistent with the WhatsApp default where personalised recommendations
     * remain enabled until the user explicitly opts out.
     *
     * @return {@code true} if the user has opted out, {@code false} otherwise
     *         including when the field is unset
     */
    public boolean isUserOptedOut() {
        return isUserOptedOut != null && isUserOptedOut;
    }

    /**
     * Sets whether the user has opted out of personalised channel
     * recommendations.
     *
     * @param isUserOptedOut the new opt-out flag, or {@code null} to clear
     *                       the field
     */
    public void setUserOptedOut(Boolean isUserOptedOut) {
        this.isUserOptedOut = isUserOptedOut;
    }
}
