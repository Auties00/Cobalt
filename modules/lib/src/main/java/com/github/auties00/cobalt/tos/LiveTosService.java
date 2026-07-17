package com.github.auties00.cobalt.tos;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.pairing.LinkedPrimaryPlatform;
import com.github.auties00.cobalt.wire.linked.tos.TosNotice;
import com.github.auties00.cobalt.props.ABPropsService;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Production {@link TosService} backed by a {@link LinkedWhatsAppClient}.
 *
 * <p>This is the Cobalt counterpart of WhatsApp Web's {@code WAWebTos.TosManager}.
 * It resolves a {@link TosNotice} catalog entry to its runtime notice id(s),
 * reads acceptance state from the bound client's
 * {@linkplain com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore#acknowledgedTosNotices()
 * acknowledged-notice set}, and refreshes that state from the relay through the
 * client's {@link LinkedWhatsAppClient#refreshTosNotices(Collection)} {@code w:tos}
 * acceptance query.
 *
 * @implNote
 * This implementation does not maintain a standalone state machine nor the
 * recurring pull loop of WA Web's {@code TosManager.run}; the recurring pull
 * cadence is driven by the caller (a single login-time refresh of the gating
 * notices), and acceptance state is the acknowledged-only set persisted by
 * {@link LinkedWhatsAppClient#refreshTosNotices(Collection)} rather than the
 * per-notice {@code TOS_STATE_<id>} entries WA Web keeps in {@code UserPrefs}.
 *
 * @see TosNotice
 */
@WhatsAppWebModule(moduleName = "WAWebTos")
@WhatsAppWebModule(moduleName = "WAWebTosGatingUtils")
public final class LiveTosService implements TosService {
    /**
     * The logger for {@link LiveTosService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveTosService.class);

    /**
     * The bound client used to read the store and to issue the {@code w:tos}
     * acceptance query.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The AB-props service consulted to resolve AB-prop-driven notice ids and
     * their SMB variants.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs a service bound to the given client and AB-props service.
     *
     * @param client         the bound client, must not be {@code null}
     * @param abPropsService the AB-props service, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public LiveTosService(LinkedWhatsAppClient client, ABPropsService abPropsService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation reads the SMB variant when {@link #isSmb()} and the
     * notice carries a {@link TosNotice#smbProp()}; the chosen AB-prop value is
     * trimmed and used when non-blank, otherwise the static
     * {@link TosNotice#defaultId()} is used. This mirrors the per-getter
     * {@code WAWebBotTosIds}/{@code WAWebNewsletterGatingUtils} resolution
     * (default-or-AB-prop, with the {@code _smb_web} variant under
     * {@code WAWebMobilePlatforms.isSMB()}).
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBotTosIds", exports = "getBotShortcutTosId", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebNewsletterGatingUtils", exports = "getNewsletterConsumerTos", adaptation = WhatsAppAdaptation.ADAPTED)
    public List<String> resolveIds(TosNotice notice) {
        Objects.requireNonNull(notice, "notice cannot be null");
        var prop = isSmb() && notice.smbProp() != null ? notice.smbProp() : notice.webProp();
        String value;
        if (prop != null) {
            var raw = abPropsService.getString(prop);
            value = raw != null && !raw.isBlank()
                    ? raw.trim()
                    : (notice.defaultId() != null ? notice.defaultId() : "");
        } else {
            value = notice.defaultId() != null ? notice.defaultId() : "";
        }
        if (value.isBlank()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tos notice {0} resolved to no id", notice);
            return List.of();
        }
        if (!notice.multiValued()) {
            return List.of(value);
        }
        var ids = new ArrayList<String>();
        for (var part : value.split(",")) {
            var trimmed = part.trim();
            if (!trimmed.isBlank()) {
                ids.add(trimmed);
            }
        }
        return List.copyOf(ids);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation reports the notice acknowledged only when every
     * resolved id is present in the persisted acknowledged set, so a
     * {@linkplain TosNotice#multiValued() multi-valued} notice group counts as
     * acknowledged exactly when all of its ids are; an empty resolution is
     * never acknowledged.
     */
    @Override
    public boolean isAcknowledged(TosNotice notice) {
        Objects.requireNonNull(notice, "notice cannot be null");
        var ids = resolveIds(notice);
        if (ids.isEmpty()) {
            return false;
        }
        var acknowledged = client.store().settingsStore().acknowledgedTosNotices().containsAll(ids);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tos notice {0} acknowledged={1}", notice, acknowledged);
        return acknowledged;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation resolves every notice to its id(s), de-duplicates
     * them, and issues a single {@link LinkedWhatsAppClient#refreshTosNotices(Collection)}
     * query for the union; that call performs the {@code w:tos} IQ and replaces
     * the persisted acknowledged set. It is a no-op when nothing resolves to a
     * concrete id.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebTos", exports = "TosManager.run", adaptation = WhatsAppAdaptation.ADAPTED)
    public void refresh(Collection<TosNotice> notices) {
        Objects.requireNonNull(notices, "notices cannot be null");
        var ids = new LinkedHashSet<String>();
        for (var notice : notices) {
            ids.addAll(resolveIds(notice));
        }
        if (ids.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tos refresh: no ids resolved, skipping query");
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tos refresh: querying {0} notice id(s)", ids.size());
        client.refreshTosNotices(ids);
    }

    /**
     * Returns whether the linked primary device is a WhatsApp Business (SMB)
     * client, selecting the SMB AB-prop variant of a notice id over the web
     * one.
     *
     * @return {@code true} when the primary platform is a Business variant,
     *         {@code false} otherwise or when no primary platform is recorded
     */
    @WhatsAppWebExport(moduleName = "WAWebMobilePlatforms", exports = "isSMB", adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isSmb() {
        return client.store()
                .accountStore()
                .primaryPlatform()
                .map(LinkedPrimaryPlatform::isBusiness)
                .orElse(false);
    }
}
