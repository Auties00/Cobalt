package com.github.auties00.cobalt.wire.linked.sync.action.setting;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents the user's preferred interface locale, synchronised across
 * all linked devices.
 *
 * <p>The locale drives the language used by WhatsApp for UI strings,
 * localised date and number formatting, and message templates rendered
 * server-side (for example business system messages). Changing the locale
 * on one device publishes this setting so every companion device adopts
 * the same language.
 *
 * <p>Values are standard BCP-47 language tags such as {@code "en"},
 * {@code "en_US"} or {@code "pt_BR"}.
 */
@ProtobufMessage(name = "SyncActionValue.LocaleSetting")
public final class LocaleSetting implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used when this setting is encoded in an app
     * state sync mutation.
     */
    public static final String ACTION_NAME = "setting_locale";

    /**
     * Canonical action version emitted alongside this setting when it is
     * published to the app state sync collection.
     */
    public static final int ACTION_VERSION = 3;

    /**
     * App state collection that stores this setting.
     *
     * <p>The locale is considered critical because it influences UI
     * rendering across all devices and is therefore placed in the
     * {@code CRITICAL_BLOCK} patch type.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.CRITICAL_BLOCK;

    /**
     * Returns the canonical action name for this setting.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical action version for this setting.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * The selected locale as a BCP-47 language tag.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String locale;


    /**
     * Constructs a new locale setting for the given language tag.
     *
     * @param locale the locale string, or {@code null} if no value is set
     */
    LocaleSetting(String locale) {
        this.locale = locale;
    }

    /**
     * Returns the currently selected locale.
     *
     * @return an {@link Optional} containing the locale string, or empty
     *         when the setting has never been written
     */
    public Optional<String> locale() {
        return Optional.ofNullable(locale);
    }

    /**
     * Updates the locale for this setting.
     *
     * @param locale the new locale string, or {@code null} to clear the value
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }
}
