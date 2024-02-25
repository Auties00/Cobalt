package it.auties.whatsapp.model.chat;

/**
 * The constants of this enumerated type describe the various policies that can be enforced for a {@link GroupSetting} or {@link CommunitySetting} in a {@link Chat}
 */
public enum ChatSettingPolicy {
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
    public static ChatSettingPolicy of(boolean input) {
        return input ? ADMINS : ANYONE;
    }
}