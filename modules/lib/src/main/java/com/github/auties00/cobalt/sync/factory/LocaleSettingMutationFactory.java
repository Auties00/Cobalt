package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.LocaleSetting;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.LocaleSettingBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Builds outgoing locale-setting sync mutations.
 *
 * <p>Backs the language picker in the Settings surface. A single call produces one
 * {@link SyncPendingMutation} that propagates the chosen BCP-47 locale tag to every linked
 * device, where it is consumed by
 * {@link com.github.auties00.cobalt.sync.handler.LocaleSettingHandler}.
 *
 * @implNote
 * This implementation has no dedicated WA Web counterpart module; the
 * {@code WAWebLocaleSettingSync} module exposes only the inbound {@code applyMutations} half,
 * while outgoing locale changes are wrapped via the generic
 * {@code WAWebSyncdActionUtils.buildPendingMutation} pathway shared by every
 * {@code AccountSyncdActionBase} subclass. This factory reproduces that pathway directly so the
 * caller can build a single mutation without hand-rolling the protobuf wrapping.
 */
public final class LocaleSettingMutationFactory {
    /**
     * The logger for {@link LocaleSettingMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(LocaleSettingMutationFactory.class);

    /**
     * Constructs a locale-setting mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public LocaleSettingMutationFactory() {

    }

    /**
     * Builds a pending {@code setting_locale} mutation that broadcasts the given locale to every
     * linked device.
     *
     * <p>Receiving devices feed the BCP-47 tag into their locale setter with phone-level priority
     * and without a reload. The tag must be passed in the same shape WA Web reads back, for
     * example:
     * {@snippet :
     *     factory.getLocaleMutation(Instant.now(), "en_US");
     * }
     *
     * @implNote
     * This implementation models the {@code SyncActionValue.localeSetting} shape used by
     * {@code WAWebSyncdActionUtils.buildPendingMutation}; the index carries only the
     * {@link LocaleSetting#ACTION_NAME} entry because the action is a singleton per account.
     *
     * @param timestamp the mutation timestamp recorded on both the outer mutation and the inner
     *                  {@code SyncActionValue}
     * @param locale    the new BCP-47 locale tag (e.g. {@code "en_US"})
     * @return a pending mutation carrying the {@code setting_locale} action
     * @throws NullPointerException if {@code timestamp} or {@code locale} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdActionUtils", exports = "buildPendingMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getLocaleMutation(Instant timestamp, String locale) {
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(locale, "locale cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building locale setting mutation locale={0}", locale);
        var setting = new LocaleSettingBuilder()
                .locale(locale)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .localeSetting(setting)
                .build();
        var index = JSON.toJSONString(List.of(LocaleSetting.ACTION_NAME));
        var pending = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                LocaleSetting.ACTION_VERSION
        );
        return new SyncPendingMutation(pending, 0);
    }
}
