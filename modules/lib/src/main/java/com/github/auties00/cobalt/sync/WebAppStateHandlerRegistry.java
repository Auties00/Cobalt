package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.sync.handler.*;
import com.github.auties00.cobalt.wam.WamService;

import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dispatch table that maps every supported sync action name onto the
 * {@link WebAppStateActionHandler} that processes incoming mutations of that
 * action.
 *
 * <p>The registry is owned by {@link WebAppStateService} and consulted for
 * every incoming patch the server delivers: the syncd response parser reads a
 * decrypted mutation's action name, looks up the handler with
 * {@link #findHandler(String)}, and invokes it. Outgoing mutation generation
 * does not live here; per-handler factories build the WAM-side payloads, so
 * the registry stays a pure incoming-mutation router. The
 * {@link #maxSupportedVersion()} value is consumed by the response parser to
 * drop mutations whose declared version exceeds any handler's capability.
 *
 * @implNote This implementation eagerly registers every default handler in
 * the constructor, a once-only initialisation rather than lazy population.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdGetActionHandler")
public final class WebAppStateHandlerRegistry {
    /**
     * The logger for {@link WebAppStateHandlerRegistry}.
     */
    private static final System.Logger LOGGER = Log.get(WebAppStateHandlerRegistry.class);

    /**
     * The action-name to handler map populated at construction time and
     * mutable through {@link #registerHandler(WebAppStateActionHandler)}.
     */
    private final Map<String, WebAppStateActionHandler> handlers;

    /**
     * Builds a registry pre-populated with every default handler.
     *
     * <p>Constructed once by {@link WebAppStateService}; the dependencies are
     * forwarded to the handlers that need them. Callers that want a custom
     * dispatch table register additional or replacement handlers via
     * {@link #registerHandler(WebAppStateActionHandler)} after construction.
     *
     * @param abPropsService      the AB-prop service forwarded to every
     *                            handler that gates behaviour on remote
     *                            configuration
     * @param lidMigrationService the LID migration service forwarded to
     *                            handlers that observe LID 1:1 migration state
     * @param wamService          the WAM telemetry service forwarded to
     *                            handlers that emit per-mutation events
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdGetActionHandler", exports = "setActionHandlers", adaptation = WhatsAppAdaptation.ADAPTED)
    public WebAppStateHandlerRegistry(ABPropsService abPropsService, LidMigrationService lidMigrationService, WamService wamService) {
        this.handlers = new HashMap<>();
        registerDefaultHandlers(abPropsService, lidMigrationService, wamService);
    }

    /**
     * Instantiates every default action handler and registers it under its
     * declared action name.
     *
     * <p>Invoked exactly once from the constructor. Callers that want to swap
     * a handler call {@link #registerHandler(WebAppStateActionHandler)} after
     * construction instead of going through this method.
     *
     * @param abPropsService      the AB-prop service injected into every
     *                            feature-gated handler
     * @param lidMigrationService the LID migration service injected into
     *                            handlers that observe LID 1:1 migration
     * @param wamService          the WAM telemetry service injected into
     *                            handlers that emit per-mutation events
     */
    private void registerDefaultHandlers(ABPropsService abPropsService, LidMigrationService lidMigrationService, WamService wamService) {
        var userStatusMuteHandler = new UserStatusMuteHandler(wamService);

        registerHandler(new ArchiveChatHandler());
        registerHandler(new PinChatHandler(wamService));
        registerHandler(new MuteChatHandler(abPropsService));
        registerHandler(new MarkChatAsReadHandler());
        registerHandler(new ClearChatHandler());
        registerHandler(new DeleteChatHandler());
        registerHandler(new LockChatHandler());

        registerHandler(new StarMessageHandler());
        registerHandler(new DeleteMessageForMeHandler());
        registerHandler(new InteractiveMessageHandler());

        registerHandler(new ContactActionHandler(abPropsService, userStatusMuteHandler));
        registerHandler(new LidContactHandler(abPropsService, userStatusMuteHandler));
        registerHandler(new PnForLidChatHandler(abPropsService));
        registerHandler(new ShareOwnPnHandler(abPropsService));

        registerHandler(new LabelEditHandler());
        registerHandler(new LabelAssociationHandler());
        registerHandler(new LabelReorderingHandler());

        registerHandler(new QuickReplyHandler());
        registerHandler(new AgentActionHandler());
        registerHandler(new ChatAssignmentHandler());
        registerHandler(new ChatAssignmentOpenedStatusHandler());
        registerHandler(new MarketingMessageHandler());
        registerHandler(new MarketingMessageBroadcastHandler());
        registerHandler(new BusinessBroadcastListHandler());
        registerHandler(new BusinessBroadcastCampaignHandler());
        registerHandler(new BusinessBroadcastInsightsHandler());
        registerHandler(new CustomerDataHandler());
        registerHandler(new MerchantPaymentPartnerHandler(abPropsService));
        registerHandler(new NoteEditHandler());

        registerHandler(new FavoriteStickerHandler());
        registerHandler(new RemoveRecentStickerHandler());
        registerHandler(new AvatarUpdatedHandler(abPropsService));

        registerHandler(new AiThreadDeleteHandler());
        registerHandler(new AiThreadRenameHandler());

        registerHandler(new PaymentInfoHandler(abPropsService));
        registerHandler(new PaymentTosHandler(abPropsService));
        registerHandler(new CustomPaymentMethodsHandler(abPropsService));

        registerHandler(userStatusMuteHandler);
        registerHandler(new TimeFormatHandler());
        registerHandler(new FavoritesHandler());

        registerHandler(new NuxActionHandler());
        registerHandler(new PrimaryVersionHandler());
        registerHandler(new SentinelHandler());
        registerHandler(new PrimaryFeatureHandler());
        registerHandler(new AndroidUnsupportedActionsHandler());
        registerHandler(new DeviceCapabilitiesHandler(lidMigrationService, wamService));
        registerHandler(new BotWelcomeRequestHandler());
        registerHandler(new DetectedOutcomesStatusHandler());
        registerHandler(new WaffleAccountLinkStateHandler(abPropsService, wamService));
        registerHandler(new CtwaPerCustomerDataSharingHandler(wamService));

        registerHandler(new PushNameSettingHandler(wamService));
        registerHandler(new LocaleSettingHandler());
        registerHandler(new UnarchiveChatsSettingHandler());
        registerHandler(new StatusPrivacyHandler());
        registerHandler(new DisableLinkPreviewsHandler());
        registerHandler(new VoipRelayAllCallsHandler());
        registerHandler(new ChatLockSettingsHandler());
        registerHandler(new ExternalWebBetaHandler(abPropsService));
        registerHandler(new SettingsSyncHandler(abPropsService));
        registerHandler(new NctSaltSyncHandler());
        registerHandler(new CallLogHandler());
        registerHandler(new DeleteIndividualCallLogHandler());
        registerHandler(new SubscriptionHandler());
        registerHandler(new OutContactHandler(abPropsService));
    }

    /**
     * Stores {@code handler} under its declared
     * {@link WebAppStateActionHandler#actionName()} key, replacing any
     * existing entry.
     *
     * <p>The registry behaves like a map: the last writer wins per action
     * name, so test suites and integration cycles can register a custom
     * handler over a default one.
     *
     * @param handler the handler to register
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdGetActionHandler", exports = "setActionHandlers", adaptation = WhatsAppAdaptation.ADAPTED)
    public void registerHandler(WebAppStateActionHandler handler) {
        handlers.put(handler.actionName(), handler);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "registered sync action handler for {0}", handler.actionName());
        }
    }

    /**
     * Looks up a handler by action name.
     *
     * <p>Called by the syncd response parser for every decrypted mutation; an
     * empty result means the action name is unknown to this version of Cobalt
     * and the mutation is recorded as orphan.
     *
     * @param actionName the action name read from the mutation's index
     * @return the registered handler wrapped in {@link Optional}, or
     *         {@link Optional#empty()} when no handler was registered
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdGetActionHandler", exports = "getActionHandler", adaptation = WhatsAppAdaptation.ADAPTED)
    public Optional<WebAppStateActionHandler> findHandler(String actionName) {
        var handler = handlers.get(actionName);
        if (handler == null) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "no sync action handler registered for action {0}", actionName);
            }
            return Optional.empty();
        }
        return Optional.of(handler);
    }

    /**
     * Returns the maximum {@link WebAppStateActionHandler#version()} over
     * every registered handler.
     *
     * <p>Read by the syncd response parser to drop mutations whose declared
     * version exceeds any registered handler's capability. Returns {@code 0}
     * when no handlers are registered.
     *
     * @return the highest supported action version across the registry
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdGetActionHandler", exports = "maxSupportedVersion", adaptation = WhatsAppAdaptation.ADAPTED)
    public int maxSupportedVersion() {
        return handlers.values().stream()
                .mapToInt(WebAppStateActionHandler::version)
                .max()
                .orElse(0);
    }
}
