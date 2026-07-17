package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.*;
import com.github.auties00.cobalt.wire.wam.type.*;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Emits the block of business, commerce, ads, and paid-messaging WhatsApp
 * Metrics events that a genuine WhatsApp Web business (SMB) session logs but
 * that Cobalt has no first-class feature to source from.
 *
 * <p>WhatsApp Web's business surface (the Business Tools drawer, the catalog
 * and Shops manager, the click-to-WhatsApp ads flow, payments, Meta Verified,
 * marketing-message controls, and the quality/paid-message decision pipeline)
 * continuously feeds the WAM telemetry stream while a business operator uses
 * the app. Cobalt does not reimplement any of those product screens, so none
 * of the roughly forty business events these screens produce would ever appear
 * in a Cobalt session's telemetry. A session that stays silent across the whole
 * business event family is trivially distinguishable from a real WhatsApp Web
 * business client. This service closes that gap: it fabricates one plausible,
 * self-consistent emission of each such event so the outbound WAM stream
 * carries the same business heartbeat a real session would.
 *
 * <p>Every event is built from a mix of live store state (the bound account's
 * JID, LID, and phone number where present) and fabricated-but-realistic
 * values: per-session UUID identifiers held stable for the lifetime of the
 * service, HMAC-shaped hex thread and message digests, monotonic sequence
 * counters, and sensible enum, boolean, and count selections drawn from what
 * the corresponding screen would report. No field is left at an obviously
 * synthetic sentinel; fields Cobalt genuinely cannot measure (the elapsed-time
 * timers) are omitted exactly as WhatsApp Web omits fields it cannot supply.
 *
 * <p>The single public entry point is {@link #emitSessionTelemetry()}; the
 * client drives it once per connect. Each event has its own private
 * {@code commit*} helper that fabricates that event's payload and forwards it
 * to {@link WamService#commit(com.github.auties00.cobalt.wam.model.WamEventSpec)}.
 *
 * @implNote
 * This implementation reports {@code isCompanionDevice} as {@code true} on the
 * disclosure and signal events because a Linked (WhatsApp Web) client is, by
 * construction, a companion device; that flag is one of the few pieces of real
 * state Cobalt can supply for this synthetic family. All other values are
 * fabricated per session and carry no cross-session stable fingerprint beyond
 * the account identifiers WhatsApp Web itself would log.
 *
 * @see WamService
 * @see com.github.auties00.cobalt.wam.LiveDailyStatsService
 */
@WhatsAppWebModule(moduleName = "WAWebViewBusinessProfileWamEvent")
@WhatsAppWebModule(moduleName = "WAWebCatalogBizWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPaymentsUserActionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebBusinessToolsEntryWamEvent")
@WhatsAppWebModule(moduleName = "WAWebBusinessToolsClickWamEvent")
@WhatsAppWebModule(moduleName = "WAWebBusinessToolsImpressionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebLwiScreenWamEvent")
@WhatsAppWebModule(moduleName = "WAWebLwiEntryPointImpressionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebWaShopsManagementWamEvent")
@WhatsAppWebModule(moduleName = "WAWebBizCatalogViewWamEvent")
@WhatsAppWebModule(moduleName = "WAWebOtpRetrieverWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSmbDataSharingConsentScreenWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSmbDataSharingConsentSettingWamEvent")
@WhatsAppWebModule(moduleName = "WAWebExtensionScreenProgressWamEvent")
@WhatsAppWebModule(moduleName = "WAWebExtensionsStructuredMessageInteractionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebManageAdsEntryPointImpressionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebCtwaOrderSignalWamEvent")
@WhatsAppWebModule(moduleName = "WAWebWhatsappQuickPromotionClientEligibilityWaterfallWamEvent")
@WhatsAppWebModule(moduleName = "WAWebCtwaConsumerDisclosureWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSmbPaidMessagesButtonLoggerWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMerchantCommerceEventWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPaidMessagingUserInteractionsLoggerWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMetaVerifiedInteractionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMetaVerifiedUserActionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebQbmMessageClickWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSmbUserJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebReachoutTimelockEnforcementSheetInfoWamEvent")
@WhatsAppWebModule(moduleName = "WAWebQbmMessageLevelActionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMarketingMessageUserControlsJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMmDisclosureStateEventWamEvent")
@WhatsAppWebModule(moduleName = "WAWebAutomaticEventsUserJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMmCollectionWindowStateEventWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMmDisclosureStateFsEventWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMmSignalSharingVerificationFsEventWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMmSignalSharingVerificationWithSignalDataEventWamEvent")
@WhatsAppWebModule(moduleName = "WAWebQbmRichOrderStatusInteractionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPsApiSignupFlowWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPaidMessageVpvImpressionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebConsumerBizInteractionJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMmSignalRealtimeWebWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMmSignalUndisclosedWebWamEvent")
@WhatsAppWebModule(moduleName = "WAWebWaPlusBenefitUserJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebFsApiSignupFlowWamEvent")
public final class SyntheticBusinessTelemetry {
    /**
     * The E.164 phone number, without the leading plus, reported for the
     * business when the bound account carries no phone number of its own.
     *
     * <p>Chosen from a North American reserved test range so it is well-formed
     * for {@code libphonenumber} parsing while never colliding with a real
     * subscriber.
     */
    private static final long FALLBACK_BUSINESS_PHONE = 14155550132L;

    /**
     * The linked-id numeric value reported for the business when the bound
     * account carries no LID of its own.
     *
     * <p>Sized to the fifteen-digit range WhatsApp mints for hosted LIDs so it
     * is indistinguishable in shape from a genuine linked identifier.
     */
    private static final long FALLBACK_BUSINESS_LID = 128394756012345L;

    /**
     * The bound WhatsApp client whose store supplies the live account
     * identifiers folded into the fabricated events.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every fabricated business event is
     * committed for batched upload.
     */
    private final WamService wamService;

    /**
     * The monotonic sequence source backing every per-event sequence-number
     * field, so events that carry an ordering counter present a plausible
     * strictly increasing series within the session.
     */
    private final AtomicLong sequence;

    /**
     * The stable catalog-session identifier reused across the catalog, Shops,
     * and Lwi entry-point events for the lifetime of this service.
     */
    private final String catalogSessionId;

    /**
     * The stable Business Tools session identifier reused across the Business
     * Tools entry, click, and impression events.
     */
    private final String businessToolsSessionId;

    /**
     * The stable commerce-session identifier reused across the merchant
     * commerce and extension flow events.
     */
    private final String commerceSessionId;

    /**
     * The stable OTP-session identifier reported by the OTP retriever event.
     */
    private final String otpSessionId;

    /**
     * The stable extensions-session identifier reused across the extension
     * screen-progress and structured-message interaction events.
     */
    private final String extensionsSessionId;

    /**
     * The stable unified-session identifier reused across the quality
     * business-message and paid-message events.
     */
    private final String unifiedSessionId;

    /**
     * The stable SMB user-journey session identifier reported by the SMB user
     * journey event.
     */
    private final String smbUserSessionId;

    /**
     * The stable consumer-to-business session identifier reported by the
     * consumer business interaction journey event.
     */
    private final String consumerBizSessionId;

    /**
     * The stable Meta Verified user-action session identifier reported by the
     * Meta Verified user action event.
     */
    private final String userActionSessionId;

    /**
     * The stable WhatsApp Plus benefit user-journey session identifier reported
     * by the WhatsApp Plus benefit event.
     */
    private final String waPlusBenefitSessionId;

    /**
     * Constructs a new {@code SyntheticBusinessTelemetry} bound to the given
     * client and WAM service.
     *
     * <p>The constructor mints the stable per-session identifiers reused across
     * the related events so the fabricated stream reads as one coherent
     * business session rather than a set of unrelated one-off events.
     *
     * @param client     the WhatsApp client whose store supplies live account
     *                   identifiers, must not be {@code null}
     * @param wamService the WAM service used to commit the emitted events, must
     *                   not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticBusinessTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.sequence = new AtomicLong(1);
        this.catalogSessionId = UUID.randomUUID().toString();
        this.businessToolsSessionId = UUID.randomUUID().toString();
        this.commerceSessionId = UUID.randomUUID().toString();
        this.otpSessionId = UUID.randomUUID().toString();
        this.extensionsSessionId = UUID.randomUUID().toString();
        this.unifiedSessionId = UUID.randomUUID().toString();
        this.smbUserSessionId = UUID.randomUUID().toString();
        this.consumerBizSessionId = UUID.randomUUID().toString();
        this.userActionSessionId = UUID.randomUUID().toString();
        this.waPlusBenefitSessionId = UUID.randomUUID().toString();
    }

    /**
     * Emits the whole block of fabricated business, commerce, ads, and
     * paid-messaging events once.
     *
     * <p>The client invokes this once per connect, after the WAM pipeline has
     * initialised, to mirror the business telemetry a real WhatsApp Web session
     * accrues over its lifetime. Although the underlying screens produce their
     * events at different natural cadences (impressions and clicks per
     * interaction, disclosure and consent events per marketing thread, signup
     * flows per onboarding), this synthetic surface collapses them into a
     * single once-per-session heartbeat: emitting each event exactly once per
     * connect is sufficient to keep the outbound WAM stream indistinguishable
     * from a genuine business client without inventing a spurious interaction
     * volume.
     */
    public void emitSessionTelemetry() {
        commitViewBusinessProfile();
        commitCatalogBiz();
        commitPaymentsUserAction();
        commitBusinessToolsEntry();
        commitBusinessToolsClick();
        commitBusinessToolsImpression();
        commitLwiScreen();
        commitLwiEntryPointImpression();
        commitWaShopsManagement();
        commitBizCatalogView();
        commitOtpRetriever();
        commitSmbDataSharingConsentScreen();
        commitSmbDataSharingConsentSetting();
        commitExtensionScreenProgress();
        commitExtensionsStructuredMessageInteraction();
        commitManageAdsEntryPointImpression();
        commitCtwaOrderSignal();
        commitWhatsappQuickPromotionClientEligibilityWaterfall();
        commitCtwaConsumerDisclosure();
        commitSmbPaidMessagesButtonLogger();
        commitMerchantCommerceEvent();
        commitPaidMessagingUserInteractionsLogger();
        commitMetaVerifiedInteraction();
        commitMetaVerifiedUserAction();
        commitQbmMessageClick();
        commitSmbUserJourney();
        commitReachoutTimelockEnforcementSheetInfo();
        commitQbmMessageLevelAction();
        commitMarketingMessageUserControlsJourney();
        commitMmDisclosureStateEvent();
        commitAutomaticEventsUserJourney();
        commitMmCollectionWindowStateEvent();
        commitMmDisclosureStateFsEvent();
        commitMmSignalSharingVerificationFsEvent();
        commitMmSignalSharingVerificationWithSignalDataEvent();
        commitQbmRichOrderStatusInteraction();
        commitPsApiSignupFlow();
        commitPaidMessageVpvImpression();
        commitConsumerBizInteractionJourney();
        commitMmSignalRealtimeWeb();
        commitMmSignalUndisclosedWeb();
        commitWaPlusBenefitUserJourney();
        commitFsApiSignupFlow();
    }

    /**
     * Fabricates and commits one {@link ViewBusinessProfileEvent} (id 1522).
     *
     * <p>Models a consumer opening the bound account's business profile from a
     * chat header: an impression action against the account JID, with the
     * trust-signal size buckets, linkage flags, and scroll depth a real profile
     * view would carry.
     */
    private void commitViewBusinessProfile() {
        wamService.commit(new ViewBusinessProfileEventBuilder()
                .viewBusinessProfileAction(ViewBusinessProfileAction.ACTION_IMPRESSION)
                .websiteSource(WebsiteSourceType.SOURCE_OTHER)
                .businessProfileJid(businessJid())
                .profileEntryPoint(ProfileEntryPoint.CHAT_HEADER)
                .linkedAccount(BusinessToolsLinkedAccountType.FACEBOOK)
                .catalogSessionId(catalogSessionId)
                .scrollDepth(3)
                .isSelfView(false)
                .isProfileLinked(true)
                .hasCoverPhoto(true)
                .bizFbSize(TrustSignalBuckets.B1K)
                .bizIgSize(TrustSignalBuckets.B101)
                .build());
    }

    /**
     * Fabricates and commits one {@link CatalogBizEvent} (id 1722).
     *
     * <p>Models the business viewing its own catalog list: a list-impression
     * action with a product identifier, product and collection counts, and the
     * catalog session shared with the other catalog surfaces.
     */
    private void commitCatalogBiz() {
        wamService.commit(new CatalogBizEventBuilder()
                .catalogBizAction(CatalogBizAction.ACTION_LIST_IMPRESSION)
                .catalogEntryPoint(CatalogEntryPoint.CATALOG_ENTRY_POINT_PROFILE)
                .catalogSessionId(catalogSessionId)
                .productId(numericId())
                .productCount(12)
                .collectionCount(3)
                .quantity(1)
                .cartToggle(true)
                .lastMessageDirection(LastMessageDirection.OPPOSITE_PARTY_INITIATED)
                .messageDepth(4)
                .threadIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .build());
    }

    /**
     * Fabricates and commits one {@link PaymentsUserActionEvent} (id 2162).
     *
     * <p>Models a completed UPI person-to-merchant payment: a confirm-button
     * click on the payment confirmation screen, with the payment mode, PSP,
     * merchant type, transaction status, and account-availability figures a
     * real payment flow would report.
     */
    private void commitPaymentsUserAction() {
        wamService.commit(new PaymentsUserActionEventBuilder()
                .paymentsEventId(UUID.randomUUID().toString())
                .paymentsCountryCode("IN")
                .paymentActionType(PaymentActionTypes.CLICK)
                .actionTarget(PaymentActionTargets.CONFIRM_BUTTON)
                .screen("payment_confirmation")
                .paymentMode(PaymentModeTypes.CONSUMER)
                .upiPaymentsPspId(UpiPaymentsPspIdType.ICICI)
                .paymentsResponseResult(PaymentsResponseResultType.OK)
                .paymentsRequestName(PaymentsRequestNameType.GET_ACCOUNTS)
                .paymentsContactsBucket(PaymentsContactsBucketType.MEDIUM)
                .merchantType(MerchantTypeType.API)
                .p2mType(P2mTypeType.P2M_PRO)
                .paymentTransactionStatus(PaymentTransactionStatusType.COMPLETED)
                .paymentNumberOfAccountsAvailable(2)
                .paymentsAccountsExist(true)
                .paymentPinSetUp(true)
                .paymentSent(true)
                .paymentsIsOrder(true)
                .build());
    }

    /**
     * Fabricates and commits one {@link BusinessToolsEntryEvent} (id 2216).
     *
     * <p>Models the operator opening the Business Tools drawer from Settings,
     * carrying the shared Business Tools session and the next sequence number.
     */
    private void commitBusinessToolsEntry() {
        wamService.commit(new BusinessToolsEntryEventBuilder()
                .businessToolsSessionId(businessToolsSessionId)
                .businessToolsSequenceNumber(nextSequence())
                .businessToolsEntryPoint(BusinessToolsEntryPointType.ENTRY_SETTINGS)
                .build());
    }

    /**
     * Fabricates and commits one {@link BusinessToolsClickEvent} (id 2218).
     *
     * <p>Models a tap on the catalog item inside the Business Tools drawer,
     * carrying the linked-account target and the entry-point placement.
     */
    private void commitBusinessToolsClick() {
        wamService.commit(new BusinessToolsClickEventBuilder()
                .businessToolsSessionId(businessToolsSessionId)
                .businessToolsSequenceNumber(nextSequence())
                .businessToolsItem(BusinessToolsItemType.CATALOG)
                .linkingTarget(BusinessToolsLinkedAccountType.FACEBOOK)
                .businessToolsEntryPoint(BusinessToolsEntryPointType.ENTRY_SETTINGS)
                .businessToolsEntryPointPlacement(2)
                .build());
    }

    /**
     * Fabricates and commits one {@link BusinessToolsImpressionEvent} (id 2220).
     *
     * <p>Models the Business Tools drawer rendering, carrying the shared
     * session, the next sequence number, and the settings entry point.
     */
    private void commitBusinessToolsImpression() {
        wamService.commit(new BusinessToolsImpressionEventBuilder()
                .businessToolsSessionId(businessToolsSessionId)
                .businessToolsSequenceNumber(nextSequence())
                .businessToolsEntryPoint(BusinessToolsEntryPointType.ENTRY_SETTINGS)
                .build());
    }

    /**
     * Fabricates and commits one {@link LwiScreenEvent} (id 2772).
     *
     * <p>Models a view of the click-to-WhatsApp ads creation hub: a screen-view
     * action against the ads-creation-hub reference, with the budget, currency,
     * duration, identity, audience, billing, and account-consent fields a real
     * ad-creation screen reports.
     */
    private void commitLwiScreen() {
        wamService.commit(new LwiScreenEventBuilder()
                .lwiFlowId(UUID.randomUUID().toString())
                .lwiEventSequenceNumber(nextSequence())
                .lwiScreenAction(LwiScreenAction.LWI_ACTION_VIEW)
                .lwiScreenReference(LwiScreenReference.LWI_SCREEN_ADSCREATION_HUB)
                .lwiAdsIdentityType(LwiAdsIdentityType.PAGE)
                .adMediaTypeSelected(LwiAdMediaType.IMAGE)
                .audienceType(AudienceType.REGION)
                .billingStatus(BillingStatus.NO_ACTION_REQUIRED)
                .validationStatus(ValidationStatus.NO_ACTION_REQUIRED)
                .onboardingEntryPoint(OnboardingEntryPoint.ONBOARDING_ENTRY_POINT_FAST_TRACK)
                .ctwaAdAccountType(CtwaAdAccountType.CTWA_FB_PAGE_LINKED_ACCOUNT)
                .ctwaLoginType(CtwaLoginType.CTWA_LOGIN_TYPE_FB_NATIVE)
                .lwiCurrency("USD")
                .lwiBudgetInLocal(500)
                .lwiDurationInDays(7)
                .itemCount(3)
                .createAdEnabled(true)
                .paymentMethodSet(true)
                .userHasLinkedFbPage(true)
                .userHasCatalogItemsToPromote(true)
                .build());
    }

    /**
     * Fabricates and commits one {@link LwiEntryPointImpressionEvent} (id 2906).
     *
     * <p>Models the advertise entry point rendering inside the Business Tools
     * list, with the catalog item counts and the fetched-recommendation action.
     */
    private void commitLwiEntryPointImpression() {
        wamService.commit(new LwiEntryPointImpressionEventBuilder()
                .catalogSessionId(catalogSessionId)
                .businessToolsSessionId(businessToolsSessionId)
                .lwiEntryPoint(LwiEntryPoint.SMB_BUSINESS_TOOLS_ADVERTISE_LIST_ITEM)
                .lwiSubEntryPoint(LwiSubEntryPoint.SMB_HOME_SCREEN_CONVERSATIONS_TAB)
                .lwiEntryPointImpressionAction(LwiEntryPointImpressionAction.LWI_ACTION_RECOMMENDATION_FETCH_RESPONSE)
                .userHasLinkedFbPage(true)
                .itemsCount(8)
                .activeItemsCount(6)
                .archivedItemsCount(2)
                .build());
    }

    /**
     * Fabricates and commits one {@link WaShopsManagementEvent} (id 2908).
     *
     * <p>Models the Shops product preview becoming visible in the settings
     * manager, carrying the seller JID drawn from the bound account.
     */
    private void commitWaShopsManagement() {
        wamService.commit(new WaShopsManagementEventBuilder()
                .shopsManagementAction(ShopsManagementAction.ACTION_SHOPS_PRODUCT_PREVIEW_VISIBLE)
                .isShopsProductPreviewVisible(true)
                .shopsSellerJid(businessJid())
                .build());
    }

    /**
     * Fabricates and commits one {@link BizCatalogViewEvent} (id 3006).
     *
     * <p>Models a consumer browsing the business catalog list: a list-impression
     * action with the catalog owner JID, product identifier, platform, and the
     * conversation-initiated attribution.
     */
    private void commitBizCatalogView() {
        wamService.commit(new BizCatalogViewEventBuilder()
                .catalogViewAction(CatalogViewAction.ACTION_LIST_IMPRESSION)
                .catalogEntryPoint(CatalogEntryPoint.CATALOG_ENTRY_POINT_PROFILE)
                .catalogSessionId(catalogSessionId)
                .catalogOwnerJid(businessJid())
                .productId(numericId())
                .quantity(1)
                .sequenceNumber(nextSequence())
                .bizPlatform(BizPlatform.SMB)
                .cartToggle(false)
                .hasVariants(true)
                .catalogEventSampled(true)
                .entryPointConversationInitiated(EntryPointConversationInitiated.CONSUMER_INITIATED)
                .build());
    }

    /**
     * Fabricates and commits one {@link OtpRetrieverEvent} (id 3468).
     *
     * <p>Models a one-tap OTP template message being received in the inbox from
     * the business, carrying the business phone number, chat JID, product and
     * event types, and the retriever correlation identifiers.
     */
    private void commitOtpRetriever() {
        wamService.commit(new OtpRetrieverEventBuilder()
                .otpSessionId(otpSessionId)
                .otpEventType(OtpEventType.MESSAGE_RECEIVED)
                .otpEventSource(OtpEventSource.OTP_MESSAGE)
                .otpProductType(OtpProductType.ONE_TAP)
                .ctaType(CtaType.COPY_CODE)
                .businessPhoneNumber(businessPhoneNumber())
                .chatId(businessJid())
                .chatsFolderType(ChatsFolderType.INBOX)
                .isNotificationEnabled(true)
                .templateId(numericId())
                .receiverCountryCode("US")
                .waDeviceId(7)
                .otpCorrelationId(UUID.randomUUID().toString())
                .build());
    }

    /**
     * Fabricates and commits one {@link SmbDataSharingConsentScreenEvent} (id 3972).
     *
     * <p>Models the data-sharing consent screen being viewed from the contact
     * info card, with the screen version, elapsed time, and prior-impression
     * counts a repeat viewer would carry.
     */
    private void commitSmbDataSharingConsentScreen() {
        wamService.commit(new SmbDataSharingConsentScreenEventBuilder()
                .smbDataSharingConsentScreenType(SmbDataSharingConsentScreenType.SMB_DATA_SHARING_CONSENT_SCREEN_VIEW)
                .smbDataSharingConsentScreenEntryPoint(SmbDataSharingConsentScreenEntryPoint.CONTACT_INFO_CARD)
                .smbDataSharingConsentScreenVersion(3)
                .elapsedTimeMs(1200)
                .previousImpressionCount(4)
                .previousOptOutImpressionCount(1)
                .build());
    }

    /**
     * Fabricates and commits one {@link SmbDataSharingConsentSettingEvent} (id 3974).
     *
     * <p>Models the operator toggling the data-sharing consent setting on from
     * the settings screen, carrying the setting version.
     */
    private void commitSmbDataSharingConsentSetting() {
        wamService.commit(new SmbDataSharingConsentSettingEventBuilder()
                .smbDataSharingConsentSettingEntryPoint(SmbDataSharingConsentSettingEntryPoint.ENTRY_POINT_SETTINGS_SCREEN)
                .smbDataSharingConsentSettingType(true)
                .smbDataSharingConsentSettingVersion(3)
                .build());
    }

    /**
     * Fabricates and commits one {@link ExtensionScreenProgressEvent} (id 4112).
     *
     * <p>Models progress through a WhatsApp Flows extension launched from a
     * message call-to-action, carrying the owner JID, flow and message
     * identifiers, screen progress, and the business-initiated attribution.
     */
    private void commitExtensionScreenProgress() {
        wamService.commit(new ExtensionScreenProgressEventBuilder()
                .bizPlatform(BizPlatform.SMB)
                .businessOwnerJid(businessJid())
                .extensionsSessionId(extensionsSessionId)
                .extensionsFlowId(UUID.randomUUID().toString())
                .extensionsMessageId(numericId())
                .extensionCategory("commerce")
                .screenProgress("1/3")
                .sequenceNumber(nextSequence())
                .flowEntryPoint(FlowEntryPoint.MESSAGE_CTA)
                .entryPointConversationInitiated(EntryPointConversationInitiated.BUSINESS_INITIATED)
                .isTemplate(true)
                .isSuccessScreen(false)
                .shoppingCartItemsCount(2)
                .build());
    }

    /**
     * Fabricates and commits one {@link ExtensionsStructuredMessageInteractionEvent} (id 4114).
     *
     * <p>Models the user opening a structured template message inside a flow,
     * carrying the message class, interaction, and header media type.
     */
    private void commitExtensionsStructuredMessageInteraction() {
        wamService.commit(new ExtensionsStructuredMessageInteractionEventBuilder()
                .bizPlatform(BizPlatform.SMB)
                .businessOwnerJid(businessJid())
                .messageClass(StructuredMessageClass.HSM)
                .messageInteraction(InteractionType.USER_START)
                .messageMediaType(MediaType.PHOTO)
                .flowEntryPoint(FlowEntryPoint.MESSAGE_CTA)
                .entryPointConversationInitiated(EntryPointConversationInitiated.BUSINESS_INITIATED)
                .threadIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .build());
    }

    /**
     * Fabricates and commits one {@link ManageAdsEntryPointImpressionEvent} (id 4124).
     *
     * <p>Models the native ads-management entry point rendering.
     */
    private void commitManageAdsEntryPointImpression() {
        wamService.commit(new ManageAdsEntryPointImpressionEventBuilder()
                .manageAdsEntryPoint(ManageAdsEntryPoint.SMB_NATIVE_ADS_MANAGEMENT)
                .build());
    }

    /**
     * Fabricates and commits one {@link CtwaOrderSignalEvent} (id 4264).
     *
     * <p>Models an order-created click-to-WhatsApp conversion signal, carrying
     * the order status, the per-event and global sharing-setting flags, and the
     * thread digest.
     */
    private void commitCtwaOrderSignal() {
        wamService.commit(new CtwaOrderSignalEventBuilder()
                .ctwaOrderSignalVersion(1)
                .orderSignalType(OrderSignalType.CREATED)
                .orderStatus(OrderStatus.PROCESSING)
                .orderPaid(false)
                .eventSharingSettingEnabled(true)
                .globalSharingSettingEnabled(true)
                .customerAdsSharingSettingEnabled(CustomerAdsSharingSettingEnabled.TRUE)
                .deepLinkConversionSource("ig")
                .ctwaSignalMetadata("{\"campaign\":\"ctwa\"}")
                .threadIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .build());
    }

    /**
     * Fabricates and commits one {@link WhatsappQuickPromotionClientEligibilityWaterfallEvent} (id 4360).
     *
     * <p>Models a passing client-side eligibility check for a quick promotion,
     * carrying the promotion identifier and the waterfall step.
     */
    private void commitWhatsappQuickPromotionClientEligibilityWaterfall() {
        wamService.commit(new WhatsappQuickPromotionClientEligibilityWaterfallEventBuilder()
                .eligibilityStatus(true)
                .promotionId(numericId())
                .step("client_eligibility")
                .instanceLogData("{\"surface\":\"business_home\"}")
                .build());
    }

    /**
     * Fabricates and commits one {@link CtwaConsumerDisclosureEvent} (id 4406).
     *
     * <p>Models the consumer disclosure sheet being viewed on thread entry,
     * carrying the disclosure type, entry point, context, and thread digest.
     */
    private void commitCtwaConsumerDisclosure() {
        wamService.commit(new CtwaConsumerDisclosureEventBuilder()
                .disclosureAction(DisclosureAction.SCREEN_VIEW)
                .disclosureType(DisclosureType.NON_BLOCKING)
                .disclosureEntryPoint(DisclosureEntryPointType.ON_THREAD_ENTRY)
                .disclosureContext(DisclosureContextType.PREFILL_TEXT)
                .ctwaConsumerDisclosureVersion(2)
                .threadIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .build());
    }

    /**
     * Fabricates and commits one {@link SmbPaidMessagesButtonLoggerEvent} (id 4508).
     *
     * <p>Models a click on a paid-message call-to-action button, carrying the
     * business phone number, button count, index, type, and server campaign.
     */
    private void commitSmbPaidMessagesButtonLogger() {
        wamService.commit(new SmbPaidMessagesButtonLoggerEventBuilder()
                .businessPhoneNumber(businessPhoneNumber())
                .pmButtonCount(2)
                .pmButtonEventType(PmButtonEventType.CLICK)
                .pmButtonIndex(0)
                .pmButtonType(PmButtonType.CTA_URL)
                .pmServerCampaignId(numericId())
                .pmIsTrackableLink("true")
                .build());
    }

    /**
     * Fabricates and commits one {@link MerchantCommerceEventEvent} (id 4688).
     *
     * <p>Models a product-view commerce interaction on the chat surface, with
     * the commerce session, payment status, catalog and discoverability flags,
     * and accepted payment methods.
     */
    private void commitMerchantCommerceEvent() {
        wamService.commit(new MerchantCommerceEventEventBuilder()
                .bizPlatform(BizPlatform.SMB)
                .commerceExperience("catalog")
                .commerceSessionId(commerceSessionId)
                .commerceSurface("chat")
                .commerceInteractionAction("view_product")
                .commercePaymentStatus("pending")
                .commerceOrderStatus("processing")
                .acceptedPaymentMethods("upi,card")
                .merchantHasCatalog(true)
                .merchantIsDiscoverable(true)
                .isCtwaOriginated(false)
                .isEligibleForAdSignal(true)
                .referral("profile")
                .p2mFlow("p2m_pro")
                .sequenceId(nextSequence())
                .build());
    }

    /**
     * Fabricates and commits one {@link PaidMessagingUserInteractionsLoggerEvent} (id 4740).
     *
     * <p>Models a click on a call-to-action URL button inside a carousel
     * marketing message, carrying the component type, header media type, host
     * storage, template, and delivery timestamp.
     */
    private void commitPaidMessagingUserInteractionsLogger() {
        wamService.commit(new PaidMessagingUserInteractionsLoggerEventBuilder()
                .pmxActionTarget(PaidMessagingUserInteractionsActionTarget.CTA_URL)
                .pmxActionType(PaidMessagingUserInteractionsActionType.CLICK)
                .pmxComponentType(PaidMessagingUserInteractionsComponentType.BUTTON)
                .pmxHeaderMediaType(PaidMessagingUserInteractionsHeaderMediaType.IMAGE)
                .pmxMarketingFormat(PaidMessagingUserInteractionsMarketingFormat.CAROUSEL)
                .pmxHostStorage(PaidMessagingUserInteractionsHostStorage.ON_PREMISE)
                .pmxTapTargetType(TapTargetType.FULL)
                .templateId(numericId())
                .pmxSenderCountryCode("US")
                .pmxCarouselCardIndex(1)
                .pmxHashedMessageId(SyntheticTelemetryUtils.randomHexLower(32))
                .pmxMessageDeliveredTs(System.currentTimeMillis())
                .pmxTextTruncationLimit(1024)
                .build());
    }

    /**
     * Fabricates and commits one {@link MetaVerifiedInteractionEvent} (id 4870).
     *
     * <p>Models a consumer viewing the Meta Verified badge on the business
     * profile, carrying the owner JID and platform, referral, and surface.
     */
    private void commitMetaVerifiedInteraction() {
        wamService.commit(new MetaVerifiedInteractionEventBuilder()
                .businessOwnerJid(businessJid())
                .businessOwnerPlatform(BusinessOwnerPlatform.SMBI)
                .metaVerifiedInteractionAction(MetaVerifiedInteractionAction.VIEW)
                .metaVerifiedInteractionAssetType(MetaVerifiedInteractionAssetType.SMB)
                .metaVerifiedInteractionReferral(MetaVerifiedInteractionReferral.CHAT_PROFILE)
                .metaVerifiedInteractionSurface(MetaVerifiedInteractionSurface.BUSINESS_PROFILE)
                .isMetaVerifiedSubscribed(true)
                .isSelfView(false)
                .build());
    }

    /**
     * Fabricates and commits one {@link MetaVerifiedUserActionEvent} (id 4986).
     *
     * <p>Models the subscribed operator opening the Meta Verified home from
     * settings, carrying the action result, badge visibility, and session.
     */
    private void commitMetaVerifiedUserAction() {
        wamService.commit(new MetaVerifiedUserActionEventBuilder()
                .metaVerifiedUserActionAction(MetaVerifiedUserActionAction.CLICK)
                .metaVerifiedUserActionAssetType(MetaVerifiedUserActionAssetType.SMB)
                .metaVerifiedUserActionReferral(MetaVerifiedUserActionReferral.SETTINGS)
                .metaVerifiedUserActionSurface(MetaVerifiedUserActionSurface.META_VERIFIED_HOME)
                .metaVerifiedUserActionResult(MetaVerifiedUserActionResult.OK)
                .metaVerifiedUserActionIsSubscribed(true)
                .metaVerifiedUserActionVerifiedBadgeVisible(true)
                .metaVerifiedUserActionGreenDotVisible(true)
                .userActionSessionId(userActionSessionId)
                .isProfileLocked(false)
                .isRetryAttempt(false)
                .build());
    }

    /**
     * Fabricates and commits one {@link QbmMessageClickEvent} (id 5178).
     *
     * <p>Models a click on a URL button in a transactional quality
     * business-message template, carrying the folder, contact type, flag, trust
     * tier, timings, and the thread and message digests.
     */
    private void commitQbmMessageClick() {
        wamService.commit(new QbmMessageClickEventBuilder()
                .buttonClickedType(QbmMessageClickButtonClickedType.URL)
                .chatsFolderType(ChatsFolderType.INBOX)
                .contactType(ContactType.SMB)
                .qbmFlag(QbmFlag.TRANSACTIONAL)
                .thumbnailType(ThumbnailType.HQ)
                .messageTypeStr("template")
                .bizTrustTier("tier_1")
                .deltaTime(1200)
                .deltaTimeReceived(3400)
                .threadIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .messageIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .isBizIntent(true)
                .isBroadcastMessage(false)
                .bodyUrlCountInt(1)
                .bodyUrlUniqueCountInt(1)
                .iasEntryPoint(SignupEntryPoint.CHAT_THREAD_BUSINESS)
                .isIasSubscriber(false)
                .build());
    }

    /**
     * Fabricates and commits one {@link SmbUserJourneyEvent} (id 5462).
     *
     * <p>Models the operator opening the catalog feature from the Business
     * Tools list, carrying the feature name, action type, surface, and the
     * account trust flags.
     */
    private void commitSmbUserJourney() {
        wamService.commit(new SmbUserJourneyEventBuilder()
                .actionType(ChatFilterActionTypes.OPEN)
                .entryPoint(EntryPoint.BUSINESS_TOOLS)
                .smbFeatureName(SmbFeatureNameEnum.CATALOG)
                .smbUserActionType(SmbUserActionTypeEnum.CLICK)
                .smbUserSessionId(smbUserSessionId)
                .surface(SurfaceType.TOOL_LIST_ITEM)
                .seqId(nextSequence())
                .contactIsSaved(true)
                .hasCatalog(true)
                .isCoexAccount(false)
                .isMvSubscriber(true)
                .oppositePlatform(OppositePlatformEnum.CONSUMER)
                .build());
    }

    /**
     * Fabricates and commits one {@link ReachoutTimelockEnforcementSheetInfoEvent} (id 5582).
     *
     * <p>Models the marketing-message reachout timelock sheet being seen for
     * the first time from a bottom sheet.
     *
     * @implNote
     * This implementation leaves the {@code timeSinceEnforcemeentEndAndSheetSeenMs}
     * timer field unset because Cobalt has no real enforcement-window clock to
     * measure it against; the generated timer setter would collapse any supplied
     * value to a near-zero elapsed reading, so omitting it is more faithful than
     * emitting a synthetic zero.
     */
    private void commitReachoutTimelockEnforcementSheetInfo() {
        wamService.commit(new ReachoutTimelockEnforcementSheetInfoEventBuilder()
                .reachoutTimelockAction(ReachoutTimelockAction.IMPRESSION)
                .reachoutTimelockEventSource(ReachoutTimelockEventSource.BOTTOM_SHEET)
                .wasSheetSeenForFirstTime(true)
                .build());
    }

    /**
     * Fabricates and commits one {@link QbmMessageLevelActionEvent} (id 5976).
     *
     * <p>Models a forward action on a quality business message from the chat
     * list, carrying the contact type, trust tier, and the thread and message
     * digests.
     */
    private void commitQbmMessageLevelAction() {
        wamService.commit(new QbmMessageLevelActionEventBuilder()
                .messageLevelAction(MessageLevelAction.FORWARD)
                .messageActionEntryPoint(MessageActionEntryPoint.CHATLIST)
                .contactType(ContactType.SMB)
                .bizTrustTier("tier_1")
                .deltaTimeReceived(5000)
                .threadIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .messageIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .decisionId(UUID.randomUUID().toString())
                .isBizIntent(true)
                .isBroadcastMessage(false)
                .messageHasUrl(true)
                .build());
    }

    /**
     * Fabricates and commits one {@link MarketingMessageUserControlsJourneyEvent} (id 6070).
     *
     * <p>Models the user stopping offers from a marketing message bottom sheet,
     * carrying the outcome, business phone number, template, and rollout
     * variant.
     *
     * @implNote
     * This implementation leaves the {@code stopDuration} timer field unset for
     * the same reason as the reachout timelock sheet: the generated timer setter
     * cannot express a meaningful synthetic duration.
     */
    private void commitMarketingMessageUserControlsJourney() {
        wamService.commit(new MarketingMessageUserControlsJourneyEventBuilder()
                .mmUserControlsAction(MmUserControlsAction.STOP)
                .mmUserControlsEntryPoint(MmUserControlsEntryPoint.BOTTOM_SHEET)
                .isSuccess(true)
                .businessPhoneNumber(businessPhoneNumber())
                .templateId(numericId())
                .unifiedSessionId(unifiedSessionId)
                .sequenceNumber(nextSequence())
                .mmUserControlsRolloutVariant(1)
                .build());
    }

    /**
     * Fabricates and commits one {@link MmDisclosureStateEventEvent} (id 6552).
     *
     * <p>Models a call-to-action URL click on a disclosed marketing message,
     * carrying the disclosure interaction, surface, disclosed flags, template,
     * and the companion-device signal.
     */
    private void commitMmDisclosureStateEvent() {
        wamService.commit(new MmDisclosureStateEventEventBuilder()
                .businessLidOrJid(businessJid())
                .disclosureEventType(DisclosureEventType.CTA_URL_CLICK)
                .disclosureInteraction(DisclosureInteraction.CONTINUE)
                .disclosureSource(DisclosureSource.NON_BLOCKING)
                .disclosureSurface(DisclosureSurface.BIZ_PROFILE_SCREEN)
                .isUserDisclosed(true)
                .mmHasDisclosedUrl(true)
                .mmHasShowDisclosureFlag(true)
                .userBecameDisclosed(true)
                .templateId(numericId())
                .isCompanionDevice(true)
                .isNetworkAvailable(true)
                .deltaTimeReceived(1500)
                .mmDisclosureFlags(3)
                .build());
    }

    /**
     * Fabricates and commits one {@link AutomaticEventsUserJourneyEvent} (id 6636).
     *
     * <p>Models the automatic-events onboarding NUX screen being viewed from the
     * Business Tools list.
     */
    private void commitAutomaticEventsUserJourney() {
        wamService.commit(new AutomaticEventsUserJourneyEventBuilder()
                .automaticEventsTargetComponent(AutomaticEventsTargetComponentEnum.NUX_SCREEN)
                .smbUserActionType(SmbUserActionTypeEnum.VIEW)
                .surface(SurfaceType.TOOL_LIST_ITEM)
                .extraAttributes("{\"nux\":\"automatic_events\"}")
                .build());
    }

    /**
     * Fabricates and commits one {@link MmCollectionWindowStateEventEvent} (id 6744).
     *
     * <p>Models the marketing-message collection window reporting its disclosed
     * token and URL state, carrying the business identifier and template.
     */
    private void commitMmCollectionWindowStateEvent() {
        wamService.commit(new MmCollectionWindowStateEventEventBuilder()
                .businessLidOrJid(businessJid())
                .mmHasDisclosedToken(true)
                .mmHasDisclosedUrl(true)
                .mmHasShowDisclosureFlag(true)
                .mmHasUndisclosedToken(false)
                .isUserDisclosed(true)
                .templateId(numericId())
                .mmDisclosureFlags(3)
                .build());
    }

    /**
     * Fabricates and commits one {@link MmDisclosureStateFsEventEvent} (id 6796).
     *
     * <p>Models the first-party-storage variant of a body-URL click on a
     * disclosed marketing message, carrying the disclosure interaction and the
     * companion-device signal.
     */
    private void commitMmDisclosureStateFsEvent() {
        wamService.commit(new MmDisclosureStateFsEventEventBuilder()
                .disclosureEventType(DisclosureEventType.BODY_URL_CLICK)
                .disclosureInteraction(DisclosureInteraction.CONTINUE)
                .disclosureSource(DisclosureSource.BLOCKING)
                .disclosureSurface(DisclosureSurface.BIZ_PROFILE_SCREEN)
                .isCompanionDevice(true)
                .isUserDisclosed(true)
                .mmHasDisclosedUrl(true)
                .mmHasShowDisclosureFlag(true)
                .userBecameDisclosed(true)
                .threadIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .mmDisclosureFlags(3)
                .build());
    }

    /**
     * Fabricates and commits one {@link MmSignalSharingVerificationFsEventEvent} (id 6798).
     *
     * <p>Models the first-party-storage verification of a shared first-customer
     * message signal, carrying the signal type, sharing status, surface,
     * conversation depth, and account-linkage state.
     */
    private void commitMmSignalSharingVerificationFsEvent() {
        wamService.commit(new MmSignalSharingVerificationFsEventEventBuilder()
                .signalType(SignalType.FIRST_CUSTOMER_MESSAGE)
                .signalMessageType(SignalMessageType.NFM)
                .signalMessageState(SignalMessageState.TRUNCATED)
                .signalOrigin(SignalOrigin.CTA_URL_CLICK)
                .signalSharingStatus(SignalSharingStatus.ONE_PD)
                .signalSurface(SignalSurface.CHAT_THREAD)
                .consentSource(ConsentSource.DISCLOSURE)
                .mmDirectionFrom(MmDirectionFrom.CUSTOMER)
                .isCompanionDevice(true)
                .isUserDisclosed(true)
                .accountLinked(true)
                .isShimmingSignal(false)
                .isLatestConversionToken(true)
                .mmConversationDepth(2)
                .mmConversationRepeat(1)
                .threadIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .build());
    }

    /**
     * Fabricates and commits one {@link MmSignalSharingVerificationWithSignalDataEventEvent} (id 6856).
     *
     * <p>Models the signal-data-bearing verification of a shared first
     * business-reply signal, carrying the serialized signal payload, matching
     * state, network availability, and companion-device signal.
     */
    private void commitMmSignalSharingVerificationWithSignalDataEvent() {
        wamService.commit(new MmSignalSharingVerificationWithSignalDataEventEventBuilder()
                .signalType(SignalType.FIRST_BIZ_REPLY)
                .signalMessageType(SignalMessageType.NFM)
                .signalMessageState(SignalMessageState.EXPANDED)
                .signalOrigin(SignalOrigin.BODY_URL_CLICK)
                .signalSharingStatus(SignalSharingStatus.ONE_PD)
                .signalSurface(SignalSurface.CHAT_THREAD)
                .consentSource(ConsentSource.DISCLOSURE)
                .mmDirectionFrom(MmDirectionFrom.CUSTOMER)
                .mmSignalData("{\"signal\":\"first_biz_reply\"}")
                .entSourceSubplatform("smba")
                .isCompanionDevice(true)
                .isUserDisclosed(true)
                .isUserMatched(true)
                .isNetworkAvailable(true)
                .accountLinked(true)
                .isShimmingSignal(false)
                .isLatestConversionToken(true)
                .mmConversationDepth(2)
                .mmConversationRepeat(1)
                .build());
    }

    /**
     * Fabricates and commits one {@link QbmRichOrderStatusInteractionEvent} (id 6940).
     *
     * <p>Models a view-order interaction on a rich order-status message in the
     * chat thread, carrying the folder, contact type, timings, and digests.
     */
    private void commitQbmRichOrderStatusInteraction() {
        wamService.commit(new QbmRichOrderStatusInteractionEventBuilder()
                .actionTypeRichOrderStatus("view_order")
                .chatsFolderType(ChatsFolderType.INBOX)
                .contactType(ContactType.SMB)
                .entryPoint(EntryPoint.CHAT_THREAD)
                .qbmFlag(QbmFlag.TRANSACTIONAL)
                .deltaTime(900)
                .deltaTimeReceived(2500)
                .isBizIntent(true)
                .isMuted(false)
                .readReceiptsEnabled(true)
                .threadIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .messageIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .unifiedSessionId(unifiedSessionId)
                .build());
    }

    /**
     * Fabricates and commits one {@link PsApiSignupFlowEvent} (id 7628).
     *
     * <p>Models landing on a business chat thread during private-stats API
     * signup, carrying the business LID and phone number and the thread-creation
     * recency bucket.
     */
    private void commitPsApiSignupFlow() {
        wamService.commit(new PsApiSignupFlowEventBuilder()
                .businessLid(businessLid())
                .businessPhoneNumber(businessPhoneNumber())
                .signupEntryPoint(SignupEntryPoint.CHAT_THREAD_BUSINESS)
                .signupUserJourneyOperation(SignupUserJourneyOperation.LAND_ON_CHAT_THREAD)
                .threadCreationTime(ThreadCreationTime.LESS_THAN_1_DAY_AGO)
                .signupDeepLinkId(numericId())
                .build());
    }

    /**
     * Fabricates and commits one {@link PaidMessageVpvImpressionEvent} (id 7652).
     *
     * <p>Models a viewport-visible impression of a promotional paid message,
     * carrying the folder, contact type, dwell time, bubble dimensions, and
     * digests.
     */
    private void commitPaidMessageVpvImpression() {
        wamService.commit(new PaidMessageVpvImpressionEventBuilder()
                .chatsFolderType(ChatsFolderType.INBOX)
                .contactType(ContactType.SMB)
                .qbmFlag(QbmFlag.PROMOTIONAL)
                .messageBodyType(MessageBodyTypeEnum.MESSAGE)
                .deltaTime(1500)
                .deltaTimeReceived(3000)
                .isBizIntent(true)
                .isBroadcastMessage(true)
                .readReceiptsEnabled(true)
                .vpvDwellTimeMs(2400)
                .messageBubbleHeightPx(320)
                .messageBubbleWidthPx(280)
                .bodyUrlCountInt(1)
                .urlUniqueCountInt(1)
                .threadIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .messageIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .unifiedSessionId(unifiedSessionId)
                .build());
    }

    /**
     * Fabricates and commits one {@link ConsumerBizInteractionJourneyEvent} (id 7760).
     *
     * <p>Models a consumer tapping the profile button on a business chat thread,
     * carrying the feature, surface, sequence, and the business JID.
     */
    private void commitConsumerBizInteractionJourney() {
        wamService.commit(new ConsumerBizInteractionJourneyEventBuilder()
                .consumerBizActionTarget(ConsumerBizActionTargetEnum.PROFILE_BUTTON)
                .consumerBizActionType(ConsumerBizActionTypeEnum.TAP)
                .consumerBizEntryPoint(ConsumerBizEntryPointEnum.CHAT_THREAD)
                .consumerBizFeature(ConsumerBizFeatureEnum.BUSINESS_PROFILE)
                .consumerBizSurface(ConsumerBizSurfaceEnum.BUSINESS_PROFILE)
                .consumerBizSeqId(nextSequence())
                .consumerBizSessionId(consumerBizSessionId)
                .businessJid(businessJid())
                .build());
    }

    /**
     * Fabricates and commits one {@link MmSignalRealtimeWebEvent} (id 7860).
     *
     * <p>Models a realtime URL call-to-action click signal, carrying the
     * carousel and button indices and the serialized signal payload.
     */
    private void commitMmSignalRealtimeWeb() {
        wamService.commit(new MmSignalRealtimeWebEventBuilder()
                .mmSignalType(MmSignalType.URL_CTA_CLICK)
                .mmCarouselCardIndex(1)
                .mmCtaButtonIndex(0)
                .mmSignalData("{\"signal\":\"url_cta_click\"}")
                .build());
    }

    /**
     * Fabricates and commits one {@link MmSignalUndisclosedWebEvent} (id 7862).
     *
     * <p>Models an undisclosed body-URL click signal, carrying the carousel and
     * button indices and the serialized signal payload.
     */
    private void commitMmSignalUndisclosedWeb() {
        wamService.commit(new MmSignalUndisclosedWebEventBuilder()
                .mmSignalType(MmSignalType.BODY_URL_CLICK)
                .mmCarouselCardIndex(0)
                .mmCtaButtonIndex(1)
                .mmSignalData("{\"signal\":\"body_url_click\"}")
                .build());
    }

    /**
     * Fabricates and commits one {@link WaPlusBenefitUserJourneyEvent} (id 7896).
     *
     * <p>Models viewing an inactive WhatsApp Plus stickers benefit from the
     * sticker tray, carrying the benefit type, source, surface, and product.
     */
    private void commitWaPlusBenefitUserJourney() {
        wamService.commit(new WaPlusBenefitUserJourneyEventBuilder()
                .wpbujAction(WpbujAction.VIEW)
                .wpbujBenefitStatus(WpbujBenefitStatus.NOT_ACTIVE)
                .wpbujBenefitType(WpbujBenefitType.STICKERS)
                .wpbujOutcomeName(WpbujOutcomeName.SUCCESS)
                .wpbujSource(WpbujSource.APP_WIDE)
                .wpbujSurface(WpbujSurface.STICKER_TRAY)
                .wsuaProductType(WsuaProductType.WA_PLUS)
                .wpbujSessionId(waPlusBenefitSessionId)
                .wpbujActionTarget("sticker_pack")
                .build());
    }

    /**
     * Fabricates and commits one {@link FsApiSignupFlowEvent} (id 7952).
     *
     * <p>Models a Facebook-sourced full-storage signup request being sent,
     * carrying the entry point, operation, thread-creation recency, and digests.
     */
    private void commitFsApiSignupFlow() {
        wamService.commit(new FsApiSignupFlowEventBuilder()
                .signupEntryPoint(SignupEntryPoint.FACEBOOK)
                .signupUserJourneyOperation(SignupUserJourneyOperation.SIGNUP_REQUEST_SENT)
                .threadCreationTime(ThreadCreationTime.LESS_THAN_7_DAYS_AGO)
                .threadIdHmac(SyntheticTelemetryUtils.randomHexLower(32))
                .unifiedSessionId(unifiedSessionId)
                .build());
    }

    /**
     * Returns the next value of the per-session sequence counter.
     *
     * <p>Each call yields a strictly greater value than the previous one so the
     * events that carry an ordering field present a plausible monotonic series.
     *
     * @return the next sequence number
     */
    private long nextSequence() {
        return sequence.getAndIncrement();
    }

    /**
     * Returns the business phone number folded into the fabricated events.
     *
     * <p>The bound account's own phone number is used when present so the value
     * is real; otherwise {@link #FALLBACK_BUSINESS_PHONE} supplies a well-formed
     * placeholder.
     *
     * @return the E.164 phone number without the leading plus
     */
    private long businessPhoneNumber() {
        return client.store().accountStore().phoneNumber().orElse(FALLBACK_BUSINESS_PHONE);
    }

    /**
     * Returns the business JID string folded into the fabricated events.
     *
     * <p>The bound account's own JID is used when present so the value is real;
     * otherwise a placeholder JID is synthesised from
     * {@link #FALLBACK_BUSINESS_PHONE} on the standard user domain.
     *
     * @return the canonical {@code user@server} JID string
     */
    private String businessJid() {
        return client.store().accountStore().jid()
                .map(Jid::toString)
                .orElse(FALLBACK_BUSINESS_PHONE + "@s.whatsapp.net");
    }

    /**
     * Returns the business linked-id number folded into the fabricated events.
     *
     * <p>The numeric user component of the bound account's own LID is used when
     * present and parseable so the value is real; otherwise
     * {@link #FALLBACK_BUSINESS_LID} supplies a shape-correct placeholder.
     *
     * @return the linked-id numeric value
     */
    private long businessLid() {
        var lid = client.store().accountStore().lid();
        if (lid.isPresent()) {
            try {
                return Long.parseLong(lid.get().user());
            } catch (NumberFormatException _) {
                // fall through to the fabricated value when the LID has no numeric user
            }
        }
        return FALLBACK_BUSINESS_LID;
    }

    /**
     * Returns a fabricated numeric identifier shaped like the sixteen-digit
     * product, template, and campaign identifiers WhatsApp mints.
     *
     * @return a sixteen-digit numeric identifier string
     */
    private String numericId() {
        return Long.toString(1_000_000_000_000_000L + ThreadLocalRandom.current().nextLong(9_000_000_000_000_000L));
    }

}
