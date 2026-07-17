package com.github.auties00.cobalt.wire.linked.setting;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Application-wide theme preference selected by the user through the
 * appearance settings.
 *
 * <p>Mirrors the {@code WAWebThemeType.ThemesSettingValue} JS enum WhatsApp
 * Web ships in its bundle:
 * <ul>
 *   <li>{@code 0} - {@link #SYSTEM_DEFAULT}, follow the OS theme</li>
 *   <li>{@code 1} - {@link #LIGHT}, force the light palette</li>
 *   <li>{@code 2} - {@link #DARK}, force the dark palette</li>
 * </ul>
 * The wire format encodes the constant's {@link #index()} as a varint,
 * matching the protobuf {@code INT32} representation that WhatsApp Web
 * uses for the {@code appTheme} field on the {@code SettingsSyncAction}
 * proto.
 */
@ProtobufEnum
public enum AppTheme {
    /**
     * The client follows the operating system's current theme setting.
     */
    SYSTEM_DEFAULT(0),

    /**
     * The client forces the light palette regardless of the operating
     * system setting.
     */
    LIGHT(1),

    /**
     * The client forces the dark palette regardless of the operating
     * system setting.
     */
    DARK(2);

    /**
     * Protobuf wire index for this constant.
     */
    final int index;

    /**
     * Constructs a constant carrying the supplied protobuf wire index.
     *
     * @param index the protobuf wire index
     */
    AppTheme(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the protobuf wire index of this constant.
     *
     * @return the protobuf wire index
     */
    public int index() {
        return index;
    }
}
