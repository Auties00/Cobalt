package com.github.auties00.cobalt.wire.linked.sync.action.setting;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments for {@link SettingsSyncAction}.
 *
 * <p>Each mutation targeting a single field of the settings bundle is
 * keyed by the triple {@code (platform, settingKey, scope)}:
 * <ul>
 *   <li>{@code platform} identifies the publishing client and is the
 *       string form of a {@link SettingsSyncAction.SettingPlatform}
 *       enum index (for example {@code "1"} for {@code WEB}).</li>
 *   <li>{@code settingKey} identifies which setting changed and is the
 *       string form of a {@link SettingsSyncAction.SettingKey} enum
 *       index.</li>
 *   <li>{@code scope} narrows the mutation to a given context, most often
 *       the value {@code "app"}, allowing future scopes (per-chat,
 *       per-theme, ...) without changing the wire format.</li>
 * </ul>
 *
 * @param platform   the publishing client platform, encoded as the
 *                   decimal string of a {@code SettingPlatform} index
 * @param settingKey the setting key being mutated, encoded as the
 *                   decimal string of a {@code SettingKey} index
 * @param scope      the scope of the mutation, typically {@code "app"}
 */
public record SettingsSyncActionArgs(String platform, String settingKey, String scope) implements SyncActionArgs {
    /**
     * Returns the three-component index for this mutation.
     *
     * <p>The returned array contains {@code platform}, {@code settingKey}
     * and {@code scope} in that order.
     *
     * @return a three-element array encoding the platform, setting key,
     *         and scope
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{platform, settingKey, scope};
    }
}
