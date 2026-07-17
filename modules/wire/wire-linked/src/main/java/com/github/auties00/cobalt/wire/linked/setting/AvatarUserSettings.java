package com.github.auties00.cobalt.wire.linked.setting;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Holds the credentials that tie the WhatsApp account to its linked Facebook
 * avatar profile.
 *
 * <p>WhatsApp lets the user create and use a 3D Meta avatar as a sticker pack
 * and as a profile picture. The avatar is hosted by Facebook and requires the
 * WhatsApp client to authenticate against the Facebook user identifier with a
 * dedicated password. Both values are stored inside the user's global
 * settings so that any linked device can reuse the same avatar.
 *
 * @see GlobalSettings
 */
@ProtobufMessage(name = "AvatarUserSettings")
public final class AvatarUserSettings {
    /**
     * The Facebook user identifier associated with the avatar profile.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String fbid;

    /**
     * The password used to authenticate against the linked Facebook account.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String password;


    /**
     * Constructs a new avatar user settings instance with the given credentials.
     *
     * @param fbid     the Facebook user identifier, may be {@code null}
     * @param password the authentication password, may be {@code null}
     */
    AvatarUserSettings(String fbid, String password) {
        this.fbid = fbid;
        this.password = password;
    }

    /**
     * Returns the Facebook user identifier linked to this avatar profile.
     *
     * @return an {@link Optional} containing the identifier, or empty if not set
     */
    public Optional<String> fbid() {
        return Optional.ofNullable(fbid);
    }

    /**
     * Returns the password used to authenticate against the linked Facebook account.
     *
     * @return an {@link Optional} containing the password, or empty if not set
     */
    public Optional<String> password() {
        return Optional.ofNullable(password);
    }

    /**
     * Updates the Facebook user identifier.
     *
     * @param fbid the new identifier, or {@code null} to unset the field
     */
    public void setFbid(String fbid) {
        this.fbid = fbid;
    }

    /**
     * Updates the authentication password.
     *
     * @param password the new password, or {@code null} to unset the field
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
