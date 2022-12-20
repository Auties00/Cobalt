package it.auties.whatsapp.model.chat;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.api.Whatsapp;

/**
 * The constants of this enumerated type describe the various policies that can be enforced for a {@link GroupSetting} in a {@link Chat}.
 * Said chat should be a group: {@link Chat#isGroup()}.
 * Said actions can be executed using various methods in {@link Whatsapp}.
 */
public enum GroupPolicy
        implements ProtobufMessage {
    /**
     * Allows both admins and users
     */
    ANYONE,

    /**
     * Allows only admins
     */
    ADMINS;

    /**
     * Returns a GroupPolicy based on a boolean value obtained from Whatsapp
     *
     * @param input the boolean value obtained from Whatsapp
     * @return a non-null GroupPolicy
     */
    public static GroupPolicy of(boolean input) {
        return input ?
                ADMINS :
                ANYONE;
    }
}