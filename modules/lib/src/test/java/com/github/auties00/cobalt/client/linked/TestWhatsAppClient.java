package com.github.auties00.cobalt.client.linked;
import com.github.auties00.cobalt.listener.MessageDeletedListener;
import com.github.auties00.cobalt.listener.MessageStatusListener;
import com.github.auties00.cobalt.listener.DisconnectedListener;
import com.github.auties00.cobalt.listener.LoggedInListener;
import com.github.auties00.cobalt.listener.NewMessageListener;

import com.github.auties00.cobalt.client.WhatsAppClientDisconnectReason;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.calls.stream.AudioInput;
import com.github.auties00.cobalt.calls.stream.AudioOutput;
import com.github.auties00.cobalt.calls.stream.VideoInput;
import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.listener.WhatsAppListener;
import com.github.auties00.cobalt.wire.linked.call.Call;
import com.github.auties00.cobalt.wire.linked.call.IncomingCall;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.exception.WhatsAppException;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.listener.linked.*;
import com.github.auties00.cobalt.wire.linked.bot.profile.BotDirectory;
import com.github.auties00.cobalt.wire.linked.bot.profile.BotProfile;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastBillingAccount;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastGenAiRecommendation;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastGenAiRecommendationQuery;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastQuota;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastQuotaData;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastTargetInfo;
import com.github.auties00.cobalt.wire.linked.business.BusinessDataSharingConsent;
import com.github.auties00.cobalt.wire.linked.business.BusinessSignedUserInfo;
import com.github.auties00.cobalt.wire.linked.business.ads.AdEntryPointEntitlement;
import com.github.auties00.cobalt.wire.linked.business.ads.AdMediaUploadOptions;
import com.github.auties00.cobalt.wire.linked.business.ads.AdMediaLink;
import com.github.auties00.cobalt.wire.linked.business.ads.AdMediaRegistration;
import com.github.auties00.cobalt.wire.linked.business.ads.AdMediaUpload;
import com.github.auties00.cobalt.wire.linked.business.ads.NativeAdsEligibility;
import com.github.auties00.cobalt.wire.linked.business.ads.WhatsAppAdsIdentityPage;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelAgentStatus;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelCommand;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelIdentity;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelLinkedStatus;
import com.github.auties00.cobalt.wire.linked.business.auth.AuthorizedAgentFeaturePolicy;
import com.github.auties00.cobalt.wire.linked.business.auth.BusinessPlatformAuthToken;
import com.github.auties00.cobalt.wire.linked.business.auth.BusinessSignupMetadata;
import com.github.auties00.cobalt.wire.linked.business.auth.ExternalChatDeepLinkAuthorization;
import com.github.auties00.cobalt.wire.linked.business.auth.ExternalChatDeepLinkAuthorizationOptions;
import com.github.auties00.cobalt.wire.linked.business.auth.FacebookOidcAccessToken;
import com.github.auties00.cobalt.wire.linked.business.ads.FacebookPage;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdAccount;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdAudienceSection;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdAudienceSectionQuery;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdBudgetOptions;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdCreationRootQuery;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdCreationScreen;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdCreationSummary;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdDraft;
import com.github.auties00.cobalt.wire.linked.business.ads.LwiBoostedComponentInput;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdEmailOnboardingConfirmation;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdEmailVerificationCodeRequest;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdEstimatedReachQuery;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdInterest;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdInterestSuggestionQuery;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdLocation;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdManagementRootQuery;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdManagementScreen;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdPaymentSection;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdRegulatedCategoryBatchTuning;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdRegulatedCategoryTuning;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdSavedAudience;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdSuccessScreen;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdTargetingDescription;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdTargetingSentencesQuery;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessBoostedComponent;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiAgentHome;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiAutoReplyState;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiKnowledgeReview;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiLeadGenForm;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ai.MetaAiMode;
import com.github.auties00.cobalt.wire.linked.business.ai.MetaAiSearchSuggestions;
import com.github.auties00.cobalt.wire.linked.business.ai.MetaAiSearchTypeAheadQuery;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiProductInfo;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiProductInfoCreate;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiProductInfoEdit;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiReplyBotSchedule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiReplySettings;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiRule;
import com.github.auties00.cobalt.wire.linked.business.ai.AiChatHistoryUploadRequest;
import com.github.auties00.cobalt.wire.linked.business.ai.AiFaqEntry;
import com.github.auties00.cobalt.wire.linked.business.ai.AiLeadGenFlowInput;
import com.github.auties00.cobalt.wire.linked.business.ai.AiRuleInput;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartRefresh;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessCartRefreshOptions;
import com.github.auties00.cobalt.wire.linked.business.cart.BusinessRefreshedCart;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalog;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogCollectionCreate;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogCollectionEdit;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogCollectionMove;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogCollections;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogMutationResult;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogProductCreate;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogProductEdit;
import com.github.auties00.cobalt.wire.linked.business.catalog.CatalogProductInfo;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogProductVisibility;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogEntry;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogPage;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogPublicKey;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProduct;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductCollection;
import com.github.auties00.cobalt.wire.linked.business.catalog.CatalogFetchOptions;
import com.github.auties00.cobalt.wire.linked.business.compliance.BusinessMerchantCompliance;
import com.github.auties00.cobalt.wire.linked.business.compliance.MerchantComplianceEdit;
import com.github.auties00.cobalt.wire.linked.business.ctwa.BusinessCtwaContext;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaConversionEvent;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaDataSharingSetting;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaAccessTokenSession;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaAdMediaEntry;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaSilentNonceResult;
import com.github.auties00.cobalt.wire.linked.business.webgraphql.WhatsAppWebGraphQlSession;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingEligibility;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingEligibilityQuery;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingServiceData;
import com.github.auties00.cobalt.wire.linked.business.linking.BusinessAccountNonce;
import com.github.auties00.cobalt.wire.linked.business.linking.BusinessEligibility;
import com.github.auties00.cobalt.wire.linked.business.linking.BusinessLinkedAccounts;
import com.github.auties00.cobalt.wire.linked.business.linking.BusinessLinkedAdAccounts;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialIssuance;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialIssuanceRequest;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialServiceConfig;
import com.github.auties00.cobalt.wire.linked.business.ads.AdBudgetEstimate;
import com.github.auties00.cobalt.wire.linked.business.ads.AdTargetingComplianceStatus;
import com.github.auties00.cobalt.wire.linked.business.ads.AdTargetingTuningResult;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdBillingActor;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiToolsEligibility;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionActionLog;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionLogAcknowledgement;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionSurfaceBatch;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionTriggerContext;
import com.github.auties00.cobalt.wire.linked.business.support.SupportBugReportSubmission;
import com.github.auties00.cobalt.wire.linked.business.support.SupportBugReportSubmissionRequest;
import com.github.auties00.cobalt.wire.linked.business.support.SupportContactFormSubmission;
import com.github.auties00.cobalt.wire.linked.business.support.SupportMessageFeedbackKind;
import com.github.auties00.cobalt.wire.linked.business.support.SupportMessageFeedbackSubmission;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupSuspensionAppeal;
import com.github.auties00.cobalt.wire.linked.business.waa.NativeMachineLearningModelManifest;
import com.github.auties00.cobalt.wire.linked.business.waa.ClientCapabilityMetadata;
import com.github.auties00.cobalt.wire.linked.business.waa.ModelRequestMetadata;
import com.github.auties00.cobalt.wire.linked.business.waa.WhatsAppAdsAccountTypeReset;
import com.github.auties00.cobalt.wire.linked.business.waa.WhatsAppAdsAdAccount;
import com.github.auties00.cobalt.wire.linked.business.waa.WhatsAppAdsEligibility;
import com.github.auties00.cobalt.wire.linked.business.waa.WhatsAppAdsPageEligibility;
import com.github.auties00.cobalt.wire.linked.business.marketing.BusinessMeteredMessagingCheckout;
import com.github.auties00.cobalt.wire.linked.business.marketing.BusinessMeteredMessagingCheckoutRequest;
import com.github.auties00.cobalt.wire.linked.business.marketing.BusinessMarketingCampaign;
import com.github.auties00.cobalt.wire.linked.business.marketing.BusinessMarketingCampaignCreate;
import com.github.auties00.cobalt.wire.linked.business.marketing.BrandPhoneNumberMapping;
import com.github.auties00.cobalt.wire.linked.business.subscription.BusinessSubscriptionEntryPoints;
import com.github.auties00.cobalt.wire.linked.business.subscription.BusinessSubscriptions;
import com.github.auties00.cobalt.wire.linked.business.flow.BusinessFlowMetadata;
import com.github.auties00.cobalt.wire.linked.business.order.BusinessOrder;
import com.github.auties00.cobalt.wire.linked.business.order.BusinessOrderItem;
import com.github.auties00.cobalt.wire.linked.business.order.OrderLifecycleStatus;
import com.github.auties00.cobalt.wire.linked.business.order.OrderPaymentStatus;
import com.github.auties00.cobalt.wire.linked.business.postcode.BusinessPostcodeVerification;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessAddressAutocompleteQuery;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessAddressSuggestion;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessCategory;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessCategoryNode;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessCategoryTypeahead;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessCustomUrlIdentity;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessPriceTier;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessProfile;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessWebsiteLink;
import com.github.auties00.cobalt.wire.linked.call.CallLink;
import com.github.auties00.cobalt.wire.linked.call.CallLinkCreate;
import com.github.auties00.cobalt.wire.linked.call.CallLinkMedia;
import com.github.auties00.cobalt.wire.linked.call.CallLog;
import com.github.auties00.cobalt.wire.linked.chat.*;
import com.github.auties00.cobalt.wire.linked.chat.community.*;
import com.github.auties00.cobalt.wire.linked.chat.group.*;
import com.github.auties00.cobalt.wire.linked.contact.*;
import com.github.auties00.cobalt.wire.linked.federated.*;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.core.jid.LidChange;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.stanza.model.SizedInputStream;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.newsletter.*;
import com.github.auties00.cobalt.wire.linked.payment.BrazilCustomPaymentMethod;
import com.github.auties00.cobalt.wire.linked.payment.BrazilCustomPaymentMethodCreate;
import com.github.auties00.cobalt.wire.linked.payment.PaymentsTosV3ConsumerVariant;
import com.github.auties00.cobalt.wire.linked.preference.*;
import com.github.auties00.cobalt.wire.linked.privacy.*;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.wire.linked.reporting.*;
import com.github.auties00.cobalt.wire.linked.setting.*;
import com.github.auties00.cobalt.wire.linked.setting.notice.UserNoticeBundle;
import com.github.auties00.cobalt.wire.linked.setting.notice.UserNoticeStage;
import com.github.auties00.cobalt.wire.linked.setting.notice.UserNoticeStageQuery;
import com.github.auties00.cobalt.wire.linked.setting.privacy.*;
import com.github.auties00.cobalt.wire.linked.setting.push.PushConfig;
import com.github.auties00.cobalt.wire.linked.signal.*;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RecentEmojiWeight;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.CustomPaymentMethod;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentTosAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NotificationActivitySettingAction;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncQuery;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncResult;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.wam.threadlogging.ThreadLoggingActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * In-memory {@link LinkedWhatsAppClient} test double whose behaviour is configured through {@code with*}
 * builder methods. Only a handful of overrides are wired to caller-installed state; every other
 * contract method throws {@link UnsupportedOperationException} naming itself, so a test that leans
 * on an unstubbed call fails loudly rather than silently. The wired overrides are {@link #store()}
 * ({@link #withStore(LinkedWhatsAppStore)}), {@link #sendNode(StanzaBuilder)} (a caller-supplied
 * {@link Function} returning canned responses), {@link #handleFailure(WhatsAppException)} (records
 * into {@link #failures()}), {@link #queryChatMetadata(JidProvider)} (a preset map from
 * {@link #withChatMetadata(JidProvider, ChatMetadata)}), and {@link #isConnected()}
 * ({@link #withIsConnected(boolean)}).
 */
public final class TestWhatsAppClient implements LinkedWhatsAppClient {
    private LinkedWhatsAppStore store;
    private ABPropsService abPropsService;
    private Function<StanzaBuilder, Stanza> sendNodeHandler = node -> {
        throw new UnsupportedOperationException("TestWhatsAppClient.sendNode: no handler configured");
    };
    private final List<WhatsAppException> failures = new ArrayList<>();
    private final Map<JidProvider, ChatMetadata> chatMetadata = new HashMap<>();
    private Boolean isConnected;

    /** Returns a new test client with no preset state. */
    public static TestWhatsAppClient create() {
        return new TestWhatsAppClient();
    }

    /** Installs the {@link LinkedWhatsAppStore} returned by {@link #store()}. */
    public TestWhatsAppClient withStore(LinkedWhatsAppStore store) {
        this.store = store;
        return this;
    }

    /** Installs the handler used by {@link #sendNode(StanzaBuilder)}. */
    public TestWhatsAppClient withSendNodeHandler(Function<StanzaBuilder, Stanza> handler) {
        this.sendNodeHandler = handler;
        return this;
    }

    /** Installs a canned {@link ChatMetadata} for the given JID. */
    public TestWhatsAppClient withChatMetadata(JidProvider jid, ChatMetadata metadata) {
        this.chatMetadata.put(jid, metadata);
        return this;
    }

    /** Installs the {@link ABPropsService} returned by {@link #abPropsService()}. */
    public TestWhatsAppClient withAbPropsService(ABPropsService abPropsService) {
        this.abPropsService = abPropsService;
        return this;
    }

    /**
     * Pins the value returned by {@link #isConnected()}; left unset, that method throws.
     */
    public TestWhatsAppClient withIsConnected(boolean connected) {
        this.isConnected = connected;
        return this;
    }

    /** Returns every exception passed to {@link #handleFailure} in order. */
    public List<WhatsAppException> failures() {
        return Collections.unmodifiableList(failures);
    }

    @Override
    public LinkedWhatsAppStore store() {
        return store;
    }

    /**
     * Returns the {@link ABPropsService} installed via {@link #withAbPropsService}, throwing if none
     * was installed. Not part of {@link LinkedWhatsAppClient}; tests use it to hand the service to
     * constructor-DI consumers.
     */
    public ABPropsService abPropsService() {
        if (abPropsService == null) {
            throw new UnsupportedOperationException(
                    "TestWhatsAppClient: abPropsService is not configured; call withAbPropsService(..) first");
        }
        return abPropsService;
    }

    @Override
    public LinkedWhatsAppClient connect() {
        throw new UnsupportedOperationException("TestWhatsAppClient: connect(..) is not stubbed");
    }

    @Override
    public void resolvePendingRequest(Stanza stanza) {
        throw new UnsupportedOperationException("TestWhatsAppClient: resolvePendingRequest(..) is not stubbed");
    }

    @Override
    public void disconnect(WhatsAppClientDisconnectReason reason) {
        throw new UnsupportedOperationException("TestWhatsAppClient: disconnect(..) is not stubbed");
    }

    @Override
    public void sendNodeWithNoResponse(Stanza stanza) {
        if (store != null) {
            for (var listener : store.listeners()) {
                if (listener instanceof LinkedNodeSentListener typed) {
                    typed.onNodeSent(this, stanza);
                }
            }
        }
    }

    @Override
    public Stanza sendNode(StanzaBuilder node) {
        return sendNodeHandler.apply(node);
    }

    @Override
    public Stanza sendNode(StanzaBuilder node, Function<Stanza, Boolean> filter) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendNode(..) is not stubbed");
    }

    @Override
    public Stanza sendNode(MexStanza.Request request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendNode(..) is not stubbed");
    }

    @Override
    public void sendNodeWithNoResponse(MexStanza.Request request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendNodeWithNoResponse(..) is not stubbed");
    }

    @Override
    public JSONObject sendGraphQl(WhatsAppWebGraphQlOperation.Request request, String sessionCookie, String lsdToken) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendGraphQl(..) is not stubbed");
    }

    @Override
    public JSONObject sendGraphQl(WhatsAppWebGraphQlOperation.Request request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendGraphQl(..) is not stubbed");
    }

    @Override
    public Optional<WhatsAppWebGraphQlSession> refreshWhatsAppWebGraphQlSession() {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshWhatsAppWebGraphQlSession(..) is not stubbed");
    }

    @Override
    public void establishWhatsAppWebGraphQlSession(String sessionCookie, String lsdToken) {
        throw new UnsupportedOperationException("TestWhatsAppClient: establishWhatsAppWebGraphQlSession(..) is not stubbed");
    }

    @Override
    public JSONObject sendGraphQl(FacebookGraphQlOperation.Request request, CtwaAccessTokenSession session) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendGraphQl(..) is not stubbed");
    }

    @Override
    public JSONObject sendGraphQl(FacebookGraphQlOperation.Request request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendGraphQl(..) is not stubbed");
    }

    @Override
    public JSONObject sendGraphQl(WhatsAppGraphQlOperation.Request request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendGraphQl(..) is not stubbed");
    }

    @Override
    public Optional<CtwaAccessTokenSession> refreshFacebookGraphQlSession() {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshFacebookGraphQlSession(..) is not stubbed");
    }

    @Override
    public Stanza sendNode(SmaxStanza.Request request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendNode(..) is not stubbed");
    }

    @Override
    public void sendNodeWithNoResponse(SmaxStanza.Request request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendNodeWithNoResponse(..) is not stubbed");
    }

    @Override
    public UsyncResult sendNode(UsyncQuery query) throws InterruptedException {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendNode(..) is not stubbed");
    }

    @Override
    public void disconnect() {
        throw new UnsupportedOperationException("TestWhatsAppClient: disconnect(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient reconnect() {
        throw new UnsupportedOperationException("TestWhatsAppClient: reconnect(..) is not stubbed");
    }

    @Override
    public void logout() {
        throw new UnsupportedOperationException("TestWhatsAppClient: logout(..) is not stubbed");
    }

    @Override
    public void logoutCompanion(JidProvider companionProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: logoutCompanion(..) is not stubbed");
    }

    @Override
    public void refreshLinkedDevices() {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshLinkedDevices(..) is not stubbed");
    }

    @Override
    public boolean isConnected() {
        if (isConnected == null) {
            throw new UnsupportedOperationException("TestWhatsAppClient: isConnected(..) is not stubbed; call withIsConnected(..) first");
        }
        return isConnected;
    }

    @Override
    public LinkedWhatsAppClient waitForDisconnection() {
        throw new UnsupportedOperationException("TestWhatsAppClient: waitForDisconnection(..) is not stubbed");
    }

    @Override
    public void handleFailure(WhatsAppException exception) {
        failures.add(exception);
    }

    @Override
    public void pushWebAppState(SyncPatchType type, List<SyncPendingMutation> patches) {
        throw new UnsupportedOperationException("TestWhatsAppClient: pushWebAppState(..) is not stubbed");
    }

    @Override
    public boolean pullWebAppState(SyncPatchType... patches) {
        throw new UnsupportedOperationException("TestWhatsAppClient: pullWebAppState(..) is not stubbed");
    }

    @Override
    public void sendAck(Stanza stanza) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendAck(..) is not stubbed");
    }

    @Override
    public void sendPreKeys(long keysCount) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendPreKeys(..) is not stubbed");
    }

    @Override
    public void sendReceipt(String id, JidProvider fromProvider, String type) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendReceipt(..) is not stubbed");
    }

    @Override
    public ChatMetadata queryChatMetadata(JidProvider chat) {
        var hit = chatMetadata.get(chat);
        if (hit == null) throw new UnsupportedOperationException("TestWhatsAppClient.queryChatMetadata: no preset for " + chat);
        return hit;
    }

    @Override
    public Optional<BusinessProfile> queryBusinessProfile(JidProvider contact) {
        return Optional.empty();
    }

    @Override
    public BusinessCategory parseBusinessCategory(Stanza stanza) {
        throw new UnsupportedOperationException("TestWhatsAppClient: parseBusinessCategory(..) is not stubbed");
    }

    @Override
    public void editBusinessProfile(BusinessProfile profile) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editBusinessProfile(..) is not stubbed");
    }

    @Override
    public void enableBusinessCart() {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableBusinessCart(..) is not stubbed");
    }

    @Override
    public void disableBusinessCart() {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableBusinessCart(..) is not stubbed");
    }

    @Override
    public void deleteBusinessCoverPhoto() {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteBusinessCoverPhoto(..) is not stubbed");
    }

    @Override
    public Optional<String> queryBusinessPublicKey(JidProvider businessJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessPublicKey(..) is not stubbed");
    }

    @Override
    public BusinessSignedUserInfo queryBusinessSignedUserInfo(JidProvider businessJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessSignedUserInfo(..) is not stubbed");
    }

    @Override
    public List<BusinessMerchantCompliance> queryMerchantCompliance(List<? extends JidProvider> jids) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryMerchantCompliance(..) is not stubbed");
    }

    @Override
    public BusinessMerchantCompliance editMerchantCompliance(MerchantComplianceEdit edit) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editMerchantCompliance(..) is not stubbed");
    }

    @Override
    public BusinessCategoryTypeahead queryBusinessCategoryTypeahead(String query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessCategoryTypeahead(..) is not stubbed");
    }

    @Override
    public Optional<BusinessOrder> queryOrder(String messageId, String tokenBase64) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryOrder(..) is not stubbed");
    }

    @Override
    public String createQuickReply(QuickReplyCreate create) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createQuickReply(..) is not stubbed");
    }

    @Override
    public Optional<QuickReply> editQuickReply(QuickReplyEdit edit) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editQuickReply(..) is not stubbed");
    }

    @Override
    public Optional<QuickReply> deleteQuickReply(String quickReplyId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteQuickReply(..) is not stubbed");
    }

    @Override
    public Optional<BotProfile> queryBotProfile(JidProvider botJid) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBotProfile(..) is not stubbed");
    }

    @Override
    public Optional<BotProfile> queryBotProfile(JidProvider botJid, String personaId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBotProfile(..) is not stubbed");
    }

    @Override
    public Call startCall(JidProvider targetProvider, AudioOutput audioOut, AudioInput audioIn,
                          VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: startCall(..) is not stubbed");
    }

    @Override
    public Call startCall(JidProvider targetProvider, AudioOutput audioOut, AudioInput audioIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: startCall(..) is not stubbed");
    }

    @Override
    public Call startCall(JidProvider targetProvider, VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: startCall(..) is not stubbed");
    }

    @Override
    public Call startCall(JidProvider targetProvider, AudioOutput audioOut, VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: startCall(..) is not stubbed");
    }

    @Override
    public Call startCall(JidProvider targetProvider, VideoOutput videoOut, AudioInput audioIn, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: startCall(..) is not stubbed");
    }

    @Override
    public Call acceptCall(IncomingCall offer, AudioOutput audioOut, AudioInput audioIn,
                           VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acceptCall(..) is not stubbed");
    }

    @Override
    public Call acceptCall(IncomingCall offer, AudioOutput audioOut, AudioInput audioIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acceptCall(..) is not stubbed");
    }

    @Override
    public Call acceptCall(IncomingCall offer, VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acceptCall(..) is not stubbed");
    }

    @Override
    public Call acceptCall(IncomingCall offer, AudioOutput audioOut, VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acceptCall(..) is not stubbed");
    }

    @Override
    public Call acceptCall(IncomingCall offer, VideoOutput videoOut, AudioInput audioIn, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acceptCall(..) is not stubbed");
    }

    @Override
    public void rejectCall(IncomingCall offer, CallEndReason reason) {
        throw new UnsupportedOperationException("TestWhatsAppClient: rejectCall(..) is not stubbed");
    }

    @Override
    public void terminateCall(String callId, CallEndReason reason) {
        throw new UnsupportedOperationException("TestWhatsAppClient: terminateCall(..) is not stubbed");
    }

    @Override
    public void terminateCall(Call call, CallEndReason reason) {
        throw new UnsupportedOperationException("TestWhatsAppClient: terminateCall(..) is not stubbed");
    }

    @Override
    public void preacceptCall(String callId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: preacceptCall(..) is not stubbed");
    }

    @Override
    public void preacceptCall(IncomingCall call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: preacceptCall(..) is not stubbed");
    }

    @Override
    public void muteCall(String callId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: muteCall(..) is not stubbed");
    }

    @Override
    public void unmuteCall(String callId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unmuteCall(..) is not stubbed");
    }

    @Override
    public void muteCall(Call call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: muteCall(..) is not stubbed");
    }

    @Override
    public void unmuteCall(Call call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unmuteCall(..) is not stubbed");
    }

    @Override
    public void enableCallVideo(Call call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableCallVideo(..) is not stubbed");
    }

    @Override
    public void disableCallVideo(Call call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableCallVideo(..) is not stubbed");
    }

    @Override
    public void requestCallVideoUpgrade(Call call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: requestCallVideoUpgrade(..) is not stubbed");
    }

    @Override
    public void acceptCallVideoUpgrade(Call call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acceptCallVideoUpgrade(..) is not stubbed");
    }

    @Override
    public void rejectCallVideoUpgrade(Call call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: rejectCallVideoUpgrade(..) is not stubbed");
    }

    @Override
    public void startCallScreenShare(Call call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: startCallScreenShare(..) is not stubbed");
    }

    @Override
    public void stopCallScreenShare(Call call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: stopCallScreenShare(..) is not stubbed");
    }

    @Override
    public void sendCallReaction(Call call, String emoji) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendCallReaction(..) is not stubbed");
    }

    @Override
    public void raiseCallHand(Call call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: raiseCallHand(..) is not stubbed");
    }

    @Override
    public void lowerCallHand(Call call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: lowerCallHand(..) is not stubbed");
    }

    @Override
    public void requestCallPeerMute(Call call, JidProvider participant) {
        throw new UnsupportedOperationException("TestWhatsAppClient: requestCallPeerMute(..) is not stubbed");
    }

    @Override
    public void requestCallKeyFrame(Call call) {
        throw new UnsupportedOperationException("TestWhatsAppClient: requestCallKeyFrame(..) is not stubbed");
    }

    @Override
    public void addCallParticipants(String callId, Collection<? extends JidProvider> participantsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallParticipants(..) is not stubbed");
    }

    @Override
    public void addCallParticipants(Call call, Collection<? extends JidProvider> participantsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallParticipants(..) is not stubbed");
    }

    @Override
    public void removeCallParticipants(String callId, Collection<? extends JidProvider> participantsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: removeCallParticipants(..) is not stubbed");
    }

    @Override
    public void removeCallParticipants(Call call, Collection<? extends JidProvider> participantsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: removeCallParticipants(..) is not stubbed");
    }

    @Override
    public void issueTrustedContactToken(JidProvider peerProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: issueTrustedContactToken(..) is not stubbed");
    }

    @Override
    public Optional<byte[]> queryTrustedContactToken(JidProvider peerProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryTrustedContactToken(..) is not stubbed");
    }

    @Override
    public Optional<byte[]> queryTrustedContactToken(JidProvider peerProvider, Duration timeout) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryTrustedContactToken(..) is not stubbed");
    }


    @Override
    public void sendScheduledCall(JidProvider chatProvider, String title, Instant scheduledAt, boolean video) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendScheduledCall(..) is not stubbed");
    }

    @Override
    public void cancelScheduledCall(MessageKey creationKey) {
        throw new UnsupportedOperationException("TestWhatsAppClient: cancelScheduledCall(..) is not stubbed");
    }

    @Override
    public Call joinCallLink(URI link, AudioOutput audioOut, AudioInput audioIn,
                             VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinCallLink(..) is not stubbed");
    }

    @Override
    public Call joinCallLink(URI link, AudioOutput audioOut, AudioInput audioIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinCallLink(..) is not stubbed");
    }

    @Override
    public Call joinCallLink(URI link, VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinCallLink(..) is not stubbed");
    }

    @Override
    public Call joinCallLink(URI link, AudioOutput audioOut, VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinCallLink(..) is not stubbed");
    }

    @Override
    public Call joinCallLink(URI link, VideoOutput videoOut, AudioInput audioIn, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinCallLink(..) is not stubbed");
    }

    @Override
    public Call joinGroupCall(IncomingCall call, AudioOutput audioOut, AudioInput audioIn, VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinGroupCall(..) is not stubbed");
    }

    @Override
    public Call joinGroupCall(IncomingCall call, AudioOutput audioOut, AudioInput audioIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinGroupCall(..) is not stubbed");
    }

    @Override
    public Call joinGroupCall(IncomingCall call, VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinGroupCall(..) is not stubbed");
    }

    @Override
    public Call joinGroupCall(IncomingCall call, AudioOutput audioOut, VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinGroupCall(..) is not stubbed");
    }

    @Override
    public Call joinGroupCall(IncomingCall call, VideoOutput videoOut, AudioInput audioIn, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinGroupCall(..) is not stubbed");
    }

    @Override
    public Call joinGroupCall(JidProvider group, AudioOutput audioOut, AudioInput audioIn, VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinGroupCall(..) is not stubbed");
    }

    @Override
    public Call joinGroupCall(JidProvider group, AudioOutput audioOut, AudioInput audioIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinGroupCall(..) is not stubbed");
    }

    @Override
    public Call joinGroupCall(JidProvider group, VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinGroupCall(..) is not stubbed");
    }

    @Override
    public Call joinGroupCall(JidProvider group, AudioOutput audioOut, VideoOutput videoOut, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinGroupCall(..) is not stubbed");
    }

    @Override
    public Call joinGroupCall(JidProvider group, VideoOutput videoOut, AudioInput audioIn, VideoInput videoIn) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinGroupCall(..) is not stubbed");
    }

    @Override
    public void refreshNewsletters() {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshNewsletters(..) is not stubbed");
    }

    @Override
    public void refreshGroups() {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshGroups(..) is not stubbed");
    }

    @Override
    public Optional<String> queryGroupInviteCode(JidProvider groupProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupInviteCode(..) is not stubbed");
    }

    @Override
    public String revokeGroupInviteCode(JidProvider groupProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: revokeGroupInviteCode(..) is not stubbed");
    }

    @Override
    public Jid joinGroupViaInvite(String inviteCode) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinGroupViaInvite(..) is not stubbed");
    }

    @Override
    public GroupInvitePicture queryGroupInvitePicture(JidProvider groupProvider, String inviteCode) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupInvitePicture(..) is not stubbed");
    }

    @Override
    public GroupInvitePicture queryGroupInvitePicturePreview(JidProvider groupProvider, String inviteCode) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupInvitePicturePreview(..) is not stubbed");
    }

    @Override
    public GroupMetadata queryGroupInfoByInvite(GroupInvite invite) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupInfoByInvite(..) is not stubbed");
    }

    @Override
    public void sendGroupInvite(JidProvider groupProvider, JidProvider targetProvider, Instant inviteTimestamp) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendGroupInvite(..) is not stubbed");
    }

    @Override
    public List<Jid> queryGroupJoinRequests(JidProvider groupProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupJoinRequests(..) is not stubbed");
    }

    @Override
    public void acceptGroupJoinRequest(JidProvider groupProvider, JidProvider applicantProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acceptGroupJoinRequest(..) is not stubbed");
    }

    @Override
    public void rejectGroupJoinRequest(JidProvider groupProvider, JidProvider applicantProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: rejectGroupJoinRequest(..) is not stubbed");
    }

    @Override
    public void sendPeerMessage(JidProvider chatJidProvider, ChatMessageInfo response) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendPeerMessage(..) is not stubbed");
    }

    @Override
    public void recordThreadActivity(JidProvider chat, ThreadLoggingActivity activity) {
        // Best-effort thread-logging telemetry sink; the test double records nothing.
    }

    @Override
    public void queryChatMessage(MessageKey key) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryChatMessage(..) is not stubbed");
    }

    @Override
    public void queryChatMessages(JidProvider chat, int count) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryChatMessages(..) is not stubbed");
    }

    @Override
    public void queryChatMessages(JidProvider chat, MessageKey before, int count) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryChatMessages(..) is not stubbed");
    }

    @Override
    public void queryFullChatsHistory(int days) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryFullChatsHistory(..) is not stubbed");
    }

    @Override
    public Map<Jid, Boolean> hasWhatsapp(Collection<? extends JidProvider> phoneNumbersProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: hasWhatsapp(..) is not stubbed");
    }

    @Override
    public boolean hasWhatsapp(JidProvider phoneProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: hasWhatsapp(..) is not stubbed");
    }

    @Override
    public Optional<String> queryName(JidProvider jidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryName(..) is not stubbed");
    }

    @Override
    public Map<Jid, Jid> syncContacts(Collection<ContactCard> contacts) {
        throw new UnsupportedOperationException("TestWhatsAppClient: syncContacts(..) is not stubbed");
    }

    @Override
    public Optional<Newsletter> queryNewsletter(JidProvider newsletterJidProvider, NewsletterViewerRole role) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletter(..) is not stubbed");
    }

    @Override
    public Optional<Newsletter> queryNewsletter(JidProvider newsletterJidProvider, NewsletterViewerRole role, boolean dehydrated) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletter(..) is not stubbed");
    }

    @Override
    public Newsletter createNewsletter(NewsletterCreate create) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createNewsletter(..) is not stubbed");
    }

    @Override
    public void editNewsletterMetadata(NewsletterMetadataEdit edit) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editNewsletterMetadata(..) is not stubbed");
    }

    @Override
    public Optional<Newsletter> deleteNewsletter(JidProvider newsletterProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteNewsletter(..) is not stubbed");
    }

    @Override
    public void joinNewsletter(JidProvider newsletterProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinNewsletter(..) is not stubbed");
    }

    @Override
    public void leaveNewsletter(JidProvider newsletterProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: leaveNewsletter(..) is not stubbed");
    }

    @Override
    public void muteNewsletter(JidProvider newsletterProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: muteNewsletter(..) is not stubbed");
    }

    @Override
    public void unmuteNewsletter(JidProvider newsletterProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unmuteNewsletter(..) is not stubbed");
    }

    @Override
    public void revokeNewsletterMessage(JidProvider newsletterProvider, String serverMessageId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: revokeNewsletterMessage(..) is not stubbed");
    }

    @Override
    public void revokeNewsletterStatus(JidProvider newsletterProvider, String statusId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: revokeNewsletterStatus(..) is not stubbed");
    }

    @Override
    public void acceptNewsletterAdminInvite(JidProvider newsletterProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acceptNewsletterAdminInvite(..) is not stubbed");
    }

    @Override
    public void revokeNewsletterAdminInvite(JidProvider newsletterProvider, JidProvider adminProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: revokeNewsletterAdminInvite(..) is not stubbed");
    }

    @Override
    public List<NewsletterCapability> queryNewsletterAdminCapabilities(JidProvider newsletterProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterAdminCapabilities(..) is not stubbed");
    }

    @Override
    public OptionalLong queryNewsletterAdminsCount(JidProvider newsletterProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterAdminInfo(..) is not stubbed");
    }

    @Override
    public List<NewsletterFollower> queryNewsletterFollowers(JidProvider newsletterProvider, int count) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterFollowers(..) is not stubbed");
    }

    @Override
    public List<NewsletterAdminInvite> queryNewsletterPendingInvites(JidProvider newsletterProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterPendingInvites(..) is not stubbed");
    }

    @Override
    public NewsletterDirectoryPage queryNewsletterDirectoryList(NewsletterDirectoryListQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterDirectoryList(..) is not stubbed");
    }

    @Override
    public NewsletterDirectoryPage searchNewsletterDirectory(String searchText) {
        throw new UnsupportedOperationException("TestWhatsAppClient: searchNewsletterDirectory(..) is not stubbed");
    }

    @Override
    public NewsletterDirectoryPage searchNewsletterDirectory(String searchText, List<String> categories) {
        throw new UnsupportedOperationException("TestWhatsAppClient: searchNewsletterDirectory(..) is not stubbed");
    }

    @Override
    public NewsletterDirectoryPage searchNewsletterDirectory(String searchText, List<String> categories, Long limit, String cursorToken, boolean fetchStatusMetadata) {
        throw new UnsupportedOperationException("TestWhatsAppClient: searchNewsletterDirectory(..) is not stubbed");
    }

    @Override
    public List<NewsletterDirectoryCategory> queryNewsletterDirectoryCategoriesPreview(String input) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterDirectoryCategoriesPreview(..) is not stubbed");
    }

    @Override
    public NewsletterDirectoryPage queryRecommendedNewsletters(RecommendedNewslettersQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryRecommendedNewsletters(..) is not stubbed");
    }

    @Override
    public List<NewsletterDirectoryEntry> querySimilarNewsletters(SimilarNewslettersQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: querySimilarNewsletters(..) is not stubbed");
    }

    @Override
    public Optional<NewsletterLinkPreview> queryNewsletterLinkPreview(String url) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterLinkPreview(..) is not stubbed");
    }

    @Override
    public boolean isNewsletterDomainPreviewable(String url) {
        throw new UnsupportedOperationException("TestWhatsAppClient: isNewsletterDomainPreviewable(..) is not stubbed");
    }

    @Override
    public List<NewsletterReactor> queryNewsletterMessageReactionSenders(NewsletterMessageInfo message) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterMessageReactionSenders(..) is not stubbed");
    }

    @Override
    public List<NewsletterPollVoter> queryNewsletterPollVoters(NewsletterMessageInfo message, long limit) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterPollVoters(..) is not stubbed");
    }

    @Override
    public List<NewsletterPollVoter> queryNewsletterPollVoters(NewsletterMessageInfo message, long limit, String voteHash) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterPollVoters(..) is not stubbed");
    }

    @Override
    public void transferNewsletterOwnership(JidProvider newsletterProvider, JidProvider newOwnerProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: transferNewsletterOwnership(..) is not stubbed");
    }

    @Override
    public NewsletterAdminInvite createNewsletterAdminInvite(JidProvider newsletterProvider, JidProvider inviteeProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createNewsletterAdminInvite(..) is not stubbed");
    }

    @Override
    public void addNewsletterPaidPartnershipLabel(NewsletterMessageInfo message) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addNewsletterPaidPartnershipLabel(..) is not stubbed");
    }

    @Override
    public void logNewsletterExposures(List<NewsletterExposure> exposures) {
        throw new UnsupportedOperationException("TestWhatsAppClient: logNewsletterExposures(..) is not stubbed");
    }

    @Override
    public NewsletterReportAppeal createNewsletterReportAppeal(String reason, String reportId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createNewsletterReportAppeal(..) is not stubbed");
    }

    @Override
    public List<NewsletterEnforcement> queryNewsletterEnforcements(JidProvider newsletterProvider, String locale) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterEnforcements(..) is not stubbed");
    }

    @Override
    public List<NewsletterReport> queryNewsletterReports() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterReports(..) is not stubbed");
    }

    @Override
    public List<NewsletterInsightMetric> queryNewsletterInsights(JidProvider newsletterProvider, List<String> metrics) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterInsights(..) is not stubbed");
    }

    @Override
    public String queryNewsletterDsbInfo(String entityId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterDsbInfo(..) is not stubbed");
    }

    @Override
    public Optional<String> queryAbout(JidProvider jidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAbout(..) is not stubbed");
    }

    @Override
    public Optional<String> refreshAbout(JidProvider jidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshAbout(..) is not stubbed");
    }

    @Override
    public Optional<String> queryUsername() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryUsername(..) is not stubbed");
    }

    @Override
    public boolean editUsername(String username) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editUsername(..) is not stubbed");
    }

    @Override
    public void editUsernameRecoveryKey(String pin) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editUsernameRecoveryKey(..) is not stubbed");
    }

    @Override
    public boolean checkUsernameAvailability(String candidate) {
        throw new UnsupportedOperationException("TestWhatsAppClient: checkUsernameAvailability(..) is not stubbed");
    }

    @Override
    public boolean removeUsername() {
        throw new UnsupportedOperationException("TestWhatsAppClient: removeUsername(..) is not stubbed");
    }

    @Override
    public void editTextStatus(String text, String emoji, Duration ephemeralDuration) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editTextStatus(..) is not stubbed");
    }

    @Override
    public Map<Jid, ContactTextStatus> refreshUserTextStatuses(List<? extends JidProvider> usersProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshUserTextStatuses(..) is not stubbed");
    }

    @Override
    public Optional<LidChange> queryLidChangeNotification() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryLidChangeNotification(..) is not stubbed");
    }

    @Override
    public List<OhaiKeyConfig> queryOhaiKeyConfig() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryOhaiKeyConfig(..) is not stubbed");
    }

    @Override
    public List<BusinessCatalogEntry> queryBusinessCatalog(JidProvider businessJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessCatalog(..) is not stubbed");
    }

    @Override
    public List<BusinessCatalogEntry> queryBusinessCatalog(JidProvider businessJidProvider, int limit) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessCatalog(..) is not stubbed");
    }

    @Override
    public List<BusinessCatalog> queryBusinessCollections(JidProvider businessJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessCollections(..) is not stubbed");
    }

    @Override
    public List<BusinessCatalog> queryBusinessCollections(JidProvider businessJidProvider, int limit) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessCollections(..) is not stubbed");
    }

    @Override
    public List<BusinessCatalog> queryBusinessCollections(JidProvider businessJidProvider, int limit, int itemLimit) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessCollections(..) is not stubbed");
    }

    @Override
    public BusinessPostcodeVerification verifyBusinessPostcode(JidProvider businessJidProvider, String directConnectionEncryptedInfo) {
        throw new UnsupportedOperationException("TestWhatsAppClient: verifyBusinessPostcode(..) is not stubbed");
    }

    @Override
    public BusinessRefreshedCart refreshBusinessCart(BusinessCartRefresh refresh) {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshBusinessCart(..) is not stubbed");
    }

    @Override
    public BusinessCtwaContext queryCtwaContext(JidProvider businessJidProvider, String inviteCode, String expectedSourceUrl) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryCtwaContext(..) is not stubbed");
    }

    @Override
    public void refreshBlockList() {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshBlockList(..) is not stubbed");
    }

    @Override
    public void blockContact(JidProvider contactProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: blockContact(..) is not stubbed");
    }

    @Override
    public void unblockContact(JidProvider contactProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unblockContact(..) is not stubbed");
    }

    @Override
    public void refreshOptOutList(String category) {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshOptOutList(..) is not stubbed");
    }

    @Override
    public void refreshContactBlacklist(String category, ContactBlacklistAddressingMode addressingMode) {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshContactBlacklist(..) is not stubbed");
    }

    @Override
    public void updateOptOutList(OptOutListUpdate update) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateOptOutList(..) is not stubbed");
    }

    @Override
    public Optional<URI> queryPicture(JidProvider jidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryPicture(..) is not stubbed");
    }

    @Override
    public Optional<URI> refreshPicture() {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshPicture(..) is not stubbed");
    }

    @Override
    public void editName(String newPushName) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editName(..) is not stubbed");
    }

    @Override
    public void editAbout(String aboutText) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editAbout(..) is not stubbed");
    }

    @Override
    public void editProfilePicture(SizedInputStream jpeg) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editProfilePicture(..) is not stubbed");
    }

    @Override
    public void removeProfilePicture() {
        throw new UnsupportedOperationException("TestWhatsAppClient: removeProfilePicture(..) is not stubbed");
    }

    @Override
    public void editPresence(ContactStatus status) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editPresence(..) is not stubbed");
    }

    @Override
    public void editPresence(ContactStatus status, String presenceName) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editPresence(..) is not stubbed");
    }

    @Override
    public void editChatState(JidProvider chatProvider, ContactStatus state) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editChatState(..) is not stubbed");
    }

    @Override
    public void subscribeToPresence(JidProvider targetProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: subscribeToPresence(..) is not stubbed");
    }

    @Override
    public void subscribeToPresence(JidProvider presenceToProvider, String presenceName, JidProvider presenceContextProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: subscribeToPresence(..) is not stubbed");
    }

    @Override
    public void unsubscribeFromPresence(JidProvider targetProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unsubscribeFromPresence(..) is not stubbed");
    }

    @Override
    public MessageKey sendMessage(JidProvider jidProvider, LinkedMessageContainer container) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendMessage(..) is not stubbed");
    }

    @Override
    public void sendMessage(LinkedMessageInfo messageInfo) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendMessage(..) is not stubbed");
    }

    @Override
    public void editMessage(MessageKey originalKey, LinkedMessageContainer newContent) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editMessage(..) is not stubbed");
    }

    @Override
    public void deleteMessage(MessageKey key, boolean everyone) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteMessage(..) is not stubbed");
    }

    @Override
    public ChatMessageInfo sendStatus(LinkedMessageContainer content) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendStatus(..) is not stubbed");
    }

    @Override
    public void deleteStatus(String statusId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteStatus(..) is not stubbed");
    }

    @Override
    public ChatMessageInfo reshareStatus(MessageKey sourceKey) {
        throw new UnsupportedOperationException("TestWhatsAppClient: reshareStatus(..) is not stubbed");
    }

    @Override
    public void eagerlyEstablishSession(JidProvider chat) {
        throw new UnsupportedOperationException("TestWhatsAppClient: eagerlyEstablishSession(..) is not stubbed");
    }

    @Override
    public void markStatusViewed(String statusId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: markStatusViewed(..) is not stubbed");
    }

    @Override
    public void refreshStatusPrivacy() {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshStatusPrivacy(..) is not stubbed");
    }

    @Override
    public void editStatusPrivacy(StatusPrivacyMode mode, Collection<? extends JidProvider> jidsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editStatusPrivacy(..) is not stubbed");
    }

    @Override
    public void forwardMessage(MessageKey sourceKey, JidProvider destinationProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: forwardMessage(..) is not stubbed");
    }

    @Override
    public void forwardMessages(Collection<MessageKey> sourceKeys, Collection<? extends JidProvider> destinationsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: forwardMessages(..) is not stubbed");
    }

    @Override
    public void addReaction(MessageKey messageKey, String emoji) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addReaction(..) is not stubbed");
    }

    @Override
    public void removeReaction(MessageKey messageKey) {
        throw new UnsupportedOperationException("TestWhatsAppClient: removeReaction(..) is not stubbed");
    }

    @Override
    public void starMessage(MessageKey key) {
        throw new UnsupportedOperationException("TestWhatsAppClient: starMessage(..) is not stubbed");
    }

    @Override
    public void unstarMessage(MessageKey key) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unstarMessage(..) is not stubbed");
    }

    @Override
    public void archiveChat(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: archiveChat(..) is not stubbed");
    }

    @Override
    public void unarchiveChat(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unarchiveChat(..) is not stubbed");
    }

    @Override
    public void pinChat(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: pinChat(..) is not stubbed");
    }

    @Override
    public void unpinChat(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unpinChat(..) is not stubbed");
    }

    @Override
    public void muteChat(JidProvider chatProvider, Instant muteUntil) {
        throw new UnsupportedOperationException("TestWhatsAppClient: muteChat(..) is not stubbed");
    }

    @Override
    public void unmuteChat(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unmuteChat(..) is not stubbed");
    }

    @Override
    public void markChatAsRead(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: markChatAsRead(..) is not stubbed");
    }

    @Override
    public void markChatAsUnread(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: markChatAsUnread(..) is not stubbed");
    }

    @Override
    public void clearChat(JidProvider chatProvider, boolean keepStarred) {
        throw new UnsupportedOperationException("TestWhatsAppClient: clearChat(..) is not stubbed");
    }

    @Override
    public Optional<Chat> deleteChat(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteChat(..) is not stubbed");
    }

    @Override
    public void lockChat(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: lockChat(..) is not stubbed");
    }

    @Override
    public void unlockChat(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unlockChat(..) is not stubbed");
    }

    @Override
    public String createLabel(LabelCreate create) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createLabel(..) is not stubbed");
    }

    @Override
    public Optional<Label> editLabel(LabelEdit edit) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editLabel(..) is not stubbed");
    }

    @Override
    public Optional<Label> deleteLabel(String labelId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteLabel(..) is not stubbed");
    }

    @Override
    public Optional<Label> deleteLabel(Label label) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteLabel(..) is not stubbed");
    }

    @Override
    public void reorderLabels(List<String> labelIds) {
        throw new UnsupportedOperationException("TestWhatsAppClient: reorderLabels(..) is not stubbed");
    }

    @Override
    public void associateLabel(String labelId, JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: associateLabel(..) is not stubbed");
    }

    @Override
    public void associateLabel(Label label, JidProvider chat) {
        throw new UnsupportedOperationException("TestWhatsAppClient: associateLabel(..) is not stubbed");
    }

    @Override
    public void dissociateLabel(String labelId, JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: dissociateLabel(..) is not stubbed");
    }

    @Override
    public void dissociateLabel(Label label, JidProvider chat) {
        throw new UnsupportedOperationException("TestWhatsAppClient: dissociateLabel(..) is not stubbed");
    }

    @Override
    public void associateLabel(String labelId, MessageKey messageKey) {
        throw new UnsupportedOperationException("TestWhatsAppClient: associateLabel(..) is not stubbed");
    }

    @Override
    public void associateLabel(Label label, MessageKey messageKey) {
        throw new UnsupportedOperationException("TestWhatsAppClient: associateLabel(..) is not stubbed");
    }

    @Override
    public void dissociateLabel(String labelId, MessageKey messageKey) {
        throw new UnsupportedOperationException("TestWhatsAppClient: dissociateLabel(..) is not stubbed");
    }

    @Override
    public void dissociateLabel(Label label, MessageKey messageKey) {
        throw new UnsupportedOperationException("TestWhatsAppClient: dissociateLabel(..) is not stubbed");
    }

    @Override
    public Jid createBroadcastList(String name, Collection<? extends JidProvider> recipientsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createBroadcastList(..) is not stubbed");
    }

    @Override
    public void editBroadcastList(JidProvider broadcastListIdProvider, String newName, Collection<? extends JidProvider> newRecipientsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editBroadcastList(..) is not stubbed");
    }

    @Override
    public void deleteBroadcastList(JidProvider broadcastListIdProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteBroadcastList(..) is not stubbed");
    }

    @Override
    public ChatMessageInfo sendBroadcast(JidProvider broadcastListIdProvider, LinkedMessageContainer message) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendBroadcast(..) is not stubbed");
    }

    @Override
    public void assignChatToAgent(JidProvider chatProvider, String agentId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: assignChatToAgent(..) is not stubbed");
    }

    @Override
    public void unassignChatFromAgent(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unassignChatFromAgent(..) is not stubbed");
    }

    @Override
    public void markChatAssignmentOpened(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: markChatAssignmentOpened(..) is not stubbed");
    }

    @Override
    public void markChatAssignmentUnopened(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: markChatAssignmentUnopened(..) is not stubbed");
    }

    @Override
    public void refreshDisappearingMode() {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshDisappearingMode(..) is not stubbed");
    }

    @Override
    public void editEphemeralTimer(JidProvider chatProvider, ChatEphemeralTimer timer) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editEphemeralTimer(..) is not stubbed");
    }

    @Override
    public TosNotices refreshTosNotices(Collection<String> noticeIds) {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshTosNotices(..) is not stubbed");
    }

    @Override
    public void cancelGdprRequest(GdprReportType reportType) {
        throw new UnsupportedOperationException("TestWhatsAppClient: cancelGdprRequest(..) is not stubbed");
    }

    @Override
    public Optional<String> queryPushServerKey() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryPushServerKey(..) is not stubbed");
    }

    @Override
    public Map<PrivacySettingType<?>, PrivacySettingValue> refreshPrivacySettings() {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshPrivacySettings(..) is not stubbed");
    }

    @Override
    public void editPrivacySetting(PrivacySettingValue value) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editPrivacySetting(..) is not stubbed");
    }

    @Override
    public boolean restoreQuarantinedMessage(MessageKey key) {
        throw new UnsupportedOperationException("TestWhatsAppClient: restoreQuarantinedMessage(..) is not stubbed");
    }

    @Override
    public void exportChat(JidProvider chat, ChatExportOptions options, OutputStream output) {
        throw new UnsupportedOperationException("TestWhatsAppClient: exportChat(..) is not stubbed");
    }

    @Override
    public void enableReadReceipts() {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableReadReceipts(..) is not stubbed");
    }

    @Override
    public void disableReadReceipts() {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableReadReceipts(..) is not stubbed");
    }

    @Override
    public void issuePrivacyTokens(JidProvider userJidProvider, Collection<PrivacyTokenType> tokenTypes, Instant timestamp) {
        throw new UnsupportedOperationException("TestWhatsAppClient: issuePrivacyTokens(..) is not stubbed");
    }

    @Override
    public PrivacyDisallowedList queryPrivacyDisallowedList(JidProvider jidProvider, String dhash, String category, String type) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryPrivacyDisallowedList(..) is not stubbed");
    }

    @Override
    public Optional<ReachoutTimelock> queryReachoutTimelock() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryReachoutTimelock(..) is not stubbed");
    }

    @Override
    public Optional<UserIntegritySignals> queryUserIntegritySignals(JidProvider userJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryUserIntegritySignals(..) is not stubbed");
    }

    @Override
    public Optional<NewChatMessageCappingInfo> queryNewChatMessageCappingInfo() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewChatMessageCappingInfo(..) is not stubbed");
    }

    @Override
    public void submitPasskeyIntegrityChallenge(byte[] signedChallenge, boolean prfAvailable) {
        throw new UnsupportedOperationException("TestWhatsAppClient: submitPasskeyIntegrityChallenge(..) is not stubbed");
    }

    @Override
    public Optional<String> queryUserCountryCode(JidProvider userJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryUserCountryCode(..) is not stubbed");
    }

    @Override
    public Optional<Username> queryUserUsername(JidProvider userJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryUserUsername(..) is not stubbed");
    }

    @Override
    public void editDefaultDisappearingMode(ChatEphemeralTimer timer) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editDefaultDisappearingMode(..) is not stubbed");
    }

    @Override
    public GroupMetadata createGroup(String subject, Collection<? extends JidProvider> participantsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createGroup(..) is not stubbed");
    }

    @Override
    public GroupMetadata createGroup(String subject, ChatEphemeralTimer ephemeralTimer, Collection<? extends JidProvider> participantsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createGroup(..) is not stubbed");
    }

    @Override
    public void leaveGroup(JidProvider groupProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: leaveGroup(..) is not stubbed");
    }

    @Override
    public void leaveGroup(JidProvider... groups) {
        throw new UnsupportedOperationException("TestWhatsAppClient: leaveGroup(..) is not stubbed");
    }

    @Override
    public boolean isGroupInternal(JidProvider groupJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: isGroupInternal(..) is not stubbed");
    }

    @Override
    public Optional<GroupMetadata> queryGroupInfo(JidProvider groupProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupInfo(..) is not stubbed");
    }

    @Override
    public Optional<GroupMetadata> queryGroupInfo(JidProvider groupProvider, boolean includeUsername, String participantsPhash) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupInfo(..) is not stubbed");
    }

    @Override
    public Optional<GroupMetadata> queryGroupInfoIncludingBots(JidProvider groupProvider, boolean includeUsername, String participantsPhash) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupInfoIncludingBots(..) is not stubbed");
    }

    @Override
    public String createGroupInviteCode(JidProvider receiverProvider, String entryPoint) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createGroupInviteCode(..) is not stubbed");
    }

    @Override
    public CommunityMetadata createCommunity(CommunityCreate create) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createCommunity(..) is not stubbed");
    }

    @Override
    public void deactivateCommunity(JidProvider communityProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deactivateCommunity(..) is not stubbed");
    }

    @Override
    public void leaveCommunity(JidProvider communityProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: leaveCommunity(..) is not stubbed");
    }

    @Override
    public void transferCommunityOwnership(JidProvider communityProvider, JidProvider newOwnerProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: transferCommunityOwnership(..) is not stubbed");
    }

    @Override
    public List<Jid> querySubgroupSuggestions(JidProvider communityProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: querySubgroupSuggestions(..) is not stubbed");
    }

    @Override
    public void approveSubgroupSuggestion(SubgroupSuggestion suggestion) {
        throw new UnsupportedOperationException("TestWhatsAppClient: approveSubgroupSuggestion(..) is not stubbed");
    }

    @Override
    public void rejectSubgroupSuggestion(SubgroupSuggestion suggestion) {
        throw new UnsupportedOperationException("TestWhatsAppClient: rejectSubgroupSuggestion(..) is not stubbed");
    }

    @Override
    public long querySubgroupParticipantCount(JidProvider subgroupProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: querySubgroupParticipantCount(..) is not stubbed");
    }

    @Override
    public List<CommunityLinkedGroup> querySubgroups(JidProvider communityProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: querySubgroups(..) is not stubbed");
    }

    @Override
    public Map<Jid, GroupParticipantStatus> addGroupParticipants(JidProvider groupProvider, Collection<? extends JidProvider> toAddProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addGroupParticipants(..) is not stubbed");
    }

    @Override
    public Map<Jid, OptionalLong> storeGroupInviteSms(JidProvider groupProvider, Collection<? extends JidProvider> inviteesProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: storeGroupInviteSms(..) is not stubbed");
    }

    @Override
    public Map<Jid, GroupParticipantStatus> removeGroupParticipants(JidProvider groupProvider, Collection<? extends JidProvider> toRemoveProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: removeGroupParticipants(..) is not stubbed");
    }

    @Override
    public Map<Jid, GroupParticipantStatus> removeGroupParticipants(JidProvider groupProvider, Collection<? extends JidProvider> toRemoveProvider, boolean removeLinkedGroups) {
        throw new UnsupportedOperationException("TestWhatsAppClient: removeGroupParticipants(..) is not stubbed");
    }

    @Override
    public void promoteGroupParticipants(JidProvider groupProvider, Collection<? extends JidProvider> toPromoteProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: promoteGroupParticipants(..) is not stubbed");
    }

    @Override
    public void demoteGroupParticipants(JidProvider groupProvider, Collection<? extends JidProvider> toDemoteProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: demoteGroupParticipants(..) is not stubbed");
    }

    @Override
    public void favoriteSticker(String stickerHash) {
        throw new UnsupportedOperationException("TestWhatsAppClient: favoriteSticker(..) is not stubbed");
    }

    @Override
    public void unfavoriteSticker(String stickerHash) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unfavoriteSticker(..) is not stubbed");
    }

    @Override
    public void removeRecentSticker(String stickerHash) {
        throw new UnsupportedOperationException("TestWhatsAppClient: removeRecentSticker(..) is not stubbed");
    }


    @Override
    public void closePoll(MessageKey pollKey) {
        throw new UnsupportedOperationException("TestWhatsAppClient: closePoll(..) is not stubbed");
    }

    @Override
    public void sendBotWelcomeRequest(JidProvider botJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendBotWelcomeRequest(..) is not stubbed");
    }

    @Override
    public void renameAiThread(String chatJid, String threadId, String newName) {
        throw new UnsupportedOperationException("TestWhatsAppClient: renameAiThread(..) is not stubbed");
    }

    @Override
    public void deleteAiThread(String chatJid, String threadId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteAiThread(..) is not stubbed");
    }

    @Override
    public void favoriteChat(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: favoriteChat(..) is not stubbed");
    }

    @Override
    public void unfavoriteChat(JidProvider chatProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unfavoriteChat(..) is not stubbed");
    }

    @Override
    public String addNoteToChat(JidProvider chatProvider, String noteText) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addNoteToChat(..) is not stubbed");
    }

    @Override
    public void editNoteOnChat(JidProvider chatProvider, String noteId, String newText) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editNoteOnChat(..) is not stubbed");
    }

    @Override
    public void deleteNoteFromChat(JidProvider chatProvider, String noteId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteNoteFromChat(..) is not stubbed");
    }

    @Override
    public void pinMessage(MessageKey msgKey) {
        throw new UnsupportedOperationException("TestWhatsAppClient: pinMessage(..) is not stubbed");
    }

    @Override
    public void unpinMessage(MessageKey msgKey) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unpinMessage(..) is not stubbed");
    }

    @Override
    public void editLocale(String locale) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editLocale(..) is not stubbed");
    }

    @Override
    public void enableLinkPreviews() {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableLinkPreviews(..) is not stubbed");
    }

    @Override
    public void disableLinkPreviews() {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableLinkPreviews(..) is not stubbed");
    }

    @Override
    public void enableTwentyFourHourFormat() {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableTwentyFourHourFormat(..) is not stubbed");
    }

    @Override
    public void disableTwentyFourHourFormat() {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableTwentyFourHourFormat(..) is not stubbed");
    }

    @Override
    public void enableAIFeatures() {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableAIFeatures(..) is not stubbed");
    }

    @Override
    public void disableAIFeatures() {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableAIFeatures(..) is not stubbed");
    }

    @Override
    public void enableUnarchiveChatsOnNewMessage() {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableUnarchiveChatsOnNewMessage(..) is not stubbed");
    }

    @Override
    public void disableUnarchiveChatsOnNewMessage() {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableUnarchiveChatsOnNewMessage(..) is not stubbed");
    }

    @Override
    public void editNotificationActivity(NotificationActivitySettingAction.NotificationActivitySetting setting) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editNotificationActivity(..) is not stubbed");
    }

    @Override
    public Optional<CtwaAccessTokenSession> queryAccessTokenAndSessionCookies(String code, JidProvider fromUserJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAccessTokenAndSessionCookies(..) is not stubbed");
    }

    @Override
    public Optional<String> queryAccountNonce(String identifierScope) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAccountNonce(..) is not stubbed");
    }

    @Override
    public Optional<BusinessEligibility> queryBusinessEligibility(boolean featuresMetaVerified, boolean featuresMarketingMessages, boolean featuresGenai) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessEligibility(..) is not stubbed");
    }

    @Override
    public Optional<BusinessLinkedAccounts> queryLinkedAccounts() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryLinkedAccounts(..) is not stubbed");
    }

    @Override
    public Optional<BusinessDataSharingConsent> refreshBusinessPrivacySetting() {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshBusinessPrivacySetting(..) is not stubbed");
    }

    @Override
    public void editBusinessPrivacySetting(BusinessDataSharingConsent dataSharingConsent) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editBusinessPrivacySetting(..) is not stubbed");
    }

    @Override
    public CtwaDataSharingSetting refreshBusinessDataSharingSetting() {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshBusinessDataSharingSetting(..) is not stubbed");
    }

    @Override
    public void updateMessageFeedbackPreference(MessageFeedbackAction action, JidProvider jidProvider, String feedback) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateMessageFeedbackPreference(..) is not stubbed");
    }

    @Override
    public Optional<BusinessMeteredMessagingCheckout> queryMeteredMessagingCheckout(List<? extends JidProvider> participantsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryMeteredMessagingCheckout(..) is not stubbed");
    }

    @Override
    public Optional<BusinessMeteredMessagingCheckout> queryMeteredMessagingCheckout(BusinessMeteredMessagingCheckoutRequest request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryMeteredMessagingCheckout(..) is not stubbed");
    }

    @Override
    public Optional<CtwaSilentNonceResult> querySilentNonce(JidProvider fromUserJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: querySilentNonce(..) is not stubbed");
    }

    @Override
    public boolean sendAccountRecoveryNonce(JidProvider fromUserJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendAccountRecoveryNonce(..) is not stubbed");
    }

    @Override
    public void uploadAdMedia(CtwaAdMediaEntry media, List<CtwaAdMediaEntry> mediaList) {
        throw new UnsupportedOperationException("TestWhatsAppClient: uploadAdMedia(..) is not stubbed");
    }

    @Override
    public void editPaymentsTosV3Acceptance(int acceptPayTosVersion, PaymentsTosV3ConsumerVariant variant) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editPaymentsTosV3Acceptance(..) is not stubbed");
    }

    @Override
    public BrazilCustomPaymentMethod createBrazilCustomPaymentMethod(BrazilCustomPaymentMethodCreate create) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createBrazilCustomPaymentMethod(..) is not stubbed");
    }

    @Override
    public NewsletterPublishAck publishNewsletterMessage(JidProvider newsletterJidProvider, NewsletterPublishMessageRequest request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: publishNewsletterMessage(..) is not stubbed");
    }

    @Override
    public NewsletterPublishAck publishNewsletterStatus(JidProvider newsletterJidProvider, NewsletterPublishStatusRequest request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: publishNewsletterStatus(..) is not stubbed");
    }

    @Override
    public Stanza sendNode(IqStanza.Request request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendNode(..) is not stubbed");
    }

    @Override
    public List<BusinessProduct> queryBusinessCatalogProducts(JidProvider catalogJidProvider, List<String> productIds, int width, int height) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessCatalogProducts(..) is not stubbed");
    }

    @Override
    public List<BusinessProduct> queryBusinessCatalogProducts(JidProvider catalogJidProvider, List<String> productIds) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessCatalogProducts(..) is not stubbed");
    }

    @Override
    public List<BusinessProduct> queryBusinessCatalogProducts(JidProvider catalogJidProvider, List<String> productIds, int width, int height, String directConnectionEncryptedInfo) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessCatalogProducts(..) is not stubbed");
    }

    @Override
    public void editBusinessCoverPhoto(long id, Instant ts, byte[] token) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editBusinessCoverPhoto(..) is not stubbed");
    }

    @Override
    public void clearDirtyBits(Map<String, Long> dirtyBits) {
        throw new UnsupportedOperationException("TestWhatsAppClient: clearDirtyBits(..) is not stubbed");
    }

    @Override
    public boolean uploadMedia(MediaProvider provider, InputStream inputStream) {
        throw new UnsupportedOperationException("TestWhatsAppClient: uploadMedia(..) is not stubbed");
    }

    @Override
    public boolean uploadMedia(MediaProvider provider, Path source) {
        throw new UnsupportedOperationException("TestWhatsAppClient: uploadMedia(Path) is not stubbed");
    }

    @Override
    public InputStream downloadMedia(MediaProvider provider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: downloadMedia(..) is not stubbed");
    }

    @Override
    public void deleteTosNotice(String noticeId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteTosNotice(..) is not stubbed");
    }

    @Override
    public void acknowledgeTosNotices(List<String> noticeIds) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acknowledgeTosNotices(..) is not stubbed");
    }

    @Override
    public Optional<IdentityKeyDigest> queryKeyDigest() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryKeyDigest(..) is not stubbed");
    }

    @Override
    public List<IdentityKey> queryIdentityKeys(List<? extends JidProvider> deviceJidsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryIdentityKeys(..) is not stubbed");
    }

    @Override
    public void rotateSignedPreKey(SignalSignedPreKey signedPreKey) {
        throw new UnsupportedOperationException("TestWhatsAppClient: rotateSignedPreKey(..) is not stubbed");
    }

    @Override
    public void uploadSignalPreKeys(SignalPreKeyBundle bundle) {
        throw new UnsupportedOperationException("TestWhatsAppClient: uploadSignalPreKeys(..) is not stubbed");
    }

    @Override
    public void uploadRegistrationPreKeys(SignalPreKeyBundle bundle) {
        throw new UnsupportedOperationException("TestWhatsAppClient: uploadRegistrationPreKeys(..) is not stubbed");
    }

    @Override
    public Optional<PrivateStatsToken> issuePrivateStatsToken(byte[] blindedCredential, byte[] projectName) {
        throw new UnsupportedOperationException("TestWhatsAppClient: issuePrivateStatsToken(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addListener(LinkedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient removeListener(WhatsAppListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: removeListener(..) is not stubbed");
    }

    @Override
    public void markMessageAsRead(MessageKey messageKey) {
        throw new UnsupportedOperationException("TestWhatsAppClient: markMessageAsRead(..) is not stubbed");
    }

    @Override
    public void sendTypingIndicator(MessageKey inboundMessage) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendTypingIndicator(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProfile> queryBusinessProfile() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessProfile(..) is not stubbed");
    }

    @Override
    public List<Jid> queryBlockedContacts() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBlockedContacts(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addChatsListener(LinkedChatsListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addChatsListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addContactsListener(LinkedContactsListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addContactsListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addStatusListener(LinkedStatusListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addStatusListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addNodeSentListener(LinkedNodeSentListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addNodeSentListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addLoggedInListener(LoggedInListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addLoggedInListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallListener(LinkedCallListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addWebHistorySyncPastParticipantsListener(LinkedWebHistorySyncPastParticipantsListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addWebHistorySyncPastParticipantsListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addDisconnectedListener(DisconnectedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addDisconnectedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addWebAppPrimaryFeaturesListener(LinkedWebAppPrimaryFeaturesListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addWebAppPrimaryFeaturesListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addContactPresenceListener(LinkedContactPresenceListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addContactPresenceListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addNewslettersListener(LinkedNewslettersListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addNewslettersListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addNodeReceivedListener(LinkedNodeReceivedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addNodeReceivedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addWebAppStateActionListener(LinkedWebAppStateActionListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addWebAppStateActionListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addWebHistorySyncMessagesListener(LinkedWebHistorySyncMessagesListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addWebHistorySyncMessagesListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addNewStatusListener(LinkedNewStatusListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addNewStatusListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addAboutChangedListener(LinkedAboutChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addAboutChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addIntegrityChallengeListener(LinkedIntegrityChallengeListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addIntegrityChallengeListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addNewMessageListener(NewMessageListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addNewMessageListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addMessageDeletedListener(MessageDeletedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addMessageDeletedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addPrivacySettingChangedListener(LinkedPrivacySettingChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addPrivacySettingChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addMessageQuarantinedListener(LinkedMessageQuarantinedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addMessageQuarantinedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addWebHistorySyncProgressListener(LinkedWebHistorySyncProgressListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addWebHistorySyncProgressListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addProfilePictureChangedListener(LinkedProfilePictureChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addProfilePictureChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addMessageStatusListener(MessageStatusListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addMessageStatusListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addNameChangedListener(LinkedNameChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addNameChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addMessageReplyListener(LinkedMessageReplyListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addMessageReplyListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addDeviceIdentityChangedListener(LinkedDeviceIdentityChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addDeviceIdentityChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addNewContactListener(LinkedNewContactListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addNewContactListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addContactBlockedListener(LinkedContactBlockedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addContactBlockedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addContactTextStatusListener(LinkedContactTextStatusListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addContactTextStatusListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addLocaleChangedListener(LinkedLocaleChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addLocaleChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addRegistrationCodeListener(LinkedRegistrationCodeListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addRegistrationCodeListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addBlockedContactsListener(LinkedBlockedContactsListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addBlockedContactsListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addBusinessPrivacySettingChangedListener(LinkedBusinessPrivacySettingChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addBusinessPrivacySettingChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallEndedListener(LinkedCallEndedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallEndedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallInteractionListener(LinkedCallInteractionListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallInteractionListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallLinkAdmittedListener(LinkedCallLinkAdmittedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallLinkAdmittedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallLinkDeniedListener(LinkedCallLinkDeniedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallLinkDeniedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallLinkLobbyJoinRequestListener(LinkedCallLinkLobbyJoinRequestListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallLinkLobbyJoinRequestListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallMuteChangedListener(LinkedCallMuteChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallMuteChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallOfferNoticeListener(LinkedCallOfferNoticeListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallOfferNoticeListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallParticipantsChangedListener(LinkedCallParticipantsChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallParticipantsChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallPeerStateChangedListener(LinkedCallPeerStateChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallPeerStateChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallPreacceptListener(LinkedCallPreacceptListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallPreacceptListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallVideoStateChangedListener(LinkedCallVideoStateChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallVideoStateChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addCallVideoUpgradeRequestListener(LinkedCallVideoUpgradeRequestListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallVideoUpgradeRequestListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addFacebookGraphQlSessionChangedListener(LinkedFacebookGraphQlSessionChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addFacebookGraphQlSessionChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addContactBlacklistListener(LinkedContactBlacklistListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addContactBlacklistListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addDisappearingModeChangedListener(LinkedDisappearingModeChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addDisappearingModeChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addGroupsListener(LinkedGroupsListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addGroupsListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addLinkedDevicesListener(LinkedDevicesListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addLinkedDevicesListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addOptOutListListener(LinkedOptOutListListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addOptOutListListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addWhatsAppWebGraphQlSessionChangedListener(LinkedGraphQlSessionChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addWhatsAppWebGraphQlSessionChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addStatusPrivacyChangedListener(LinkedStatusPrivacyChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addStatusPrivacyChangedListener(..) is not stubbed");
    }

    @Override
    public LinkedWhatsAppClient addTosNoticesChangedListener(LinkedTosNoticesChangedListener listener) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addTosNoticesChangedListener(..) is not stubbed");
    }

    @Override
    public void acknowledgeGroup(JidProvider groupProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acknowledgeGroup(..) is not stubbed");
    }

    @Override
    public boolean acceptGroupAdd(GroupAddAccept accept) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acceptGroupAdd(..) is not stubbed");
    }

    @Override
    public List<GroupMetadata> queryGroupMetadata(JidProvider... groups) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupMetadata(..) is not stubbed");
    }

    @Override
    public List<GroupMetadata> queryGroupMetadata(Collection<? extends JidProvider> groupsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupMetadata(..) is not stubbed");
    }

    @Override
    public Map<Jid, GroupParticipantStatus> cancelGroupMembershipRequests(JidProvider groupProvider, Collection<? extends JidProvider> applicantsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: cancelGroupMembershipRequests(..) is not stubbed");
    }

    @Override
    public SubgroupSuggestionResult suggestNewSubgroup(SubgroupSuggestionNew suggestion) {
        throw new UnsupportedOperationException("TestWhatsAppClient: suggestNewSubgroup(..) is not stubbed");
    }

    @Override
    public SubgroupSuggestionResult suggestExistingSubgroups(JidProvider communityProvider, Collection<? extends JidProvider> candidateGroupsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: suggestExistingSubgroups(..) is not stubbed");
    }

    @Override
    public Optional<GroupMetadata> deleteParentGroup(JidProvider communityProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteParentGroup(..) is not stubbed");
    }

    @Override
    public List<GroupProfilePicture> queryGroupProfilePictures(Collection<? extends JidProvider> groupsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupProfilePictures(..) is not stubbed");
    }

    @Override
    public Optional<GroupMetadata> queryInviteGroupInfo(String inviteCode) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryInviteGroupInfo(..) is not stubbed");
    }

    @Override
    public Optional<ChatMetadata> queryLinkedGroup(JidProvider communityProvider, LinkedGroupType queryLinkedType, JidProvider queryLinkedJidProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryLinkedGroup(..) is not stubbed");
    }

    @Override
    public Map<Jid, Jid> queryLinkedGroupsParticipants(JidProvider communityProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryLinkedGroupsParticipants(..) is not stubbed");
    }

    @Override
    public List<GroupMembershipApprovalRequest> queryGroupMembershipApprovalRequests(JidProvider groupProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupMembershipApprovalRequests(..) is not stubbed");
    }

    @Override
    public List<GroupMetadata> queryParticipatingGroups(boolean includeParticipants, boolean includeDescription) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryParticipatingGroups(..) is not stubbed");
    }

    @Override
    public List<GroupMessageReport> queryReportedMessages(JidProvider groupProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryReportedMessages(..) is not stubbed");
    }

    @Override
    public boolean joinLinkedGroup(JidProvider communityProvider, JidProvider subgroupProvider, LinkedGroupType linkedGroupType) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinLinkedGroup(..) is not stubbed");
    }

    @Override
    public List<LinkedSubgroupResult> linkSubgroups(JidProvider communityProvider, List<? extends JidProvider> subgroupsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: linkSubgroups(..) is not stubbed");
    }

    @Override
    public void reportGroupMessages(JidProvider groupProvider, String messageId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: reportGroupMessages(..) is not stubbed");
    }

    @Override
    public Map<Jid, GroupParticipantStatus> revokeGroupRequestCode(JidProvider groupProvider, List<? extends JidProvider> participantsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: revokeGroupRequestCode(..) is not stubbed");
    }

    @Override
    public Optional<GroupMetadata> editGroupMetadata(GroupMetadataEdit edit) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editGroupMetadata(..) is not stubbed");
    }

    @Override
    public List<UnlinkedSubgroupResult> unlinkSubgroups(JidProvider communityProvider, List<? extends JidProvider> subgroupsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: unlinkSubgroups(..) is not stubbed");
    }

    @Override
    public NewsletterMessageHistory queryNewsletterMessageUpdates(JidProvider newsletterProvider, int count, NewsletterHistoryDirection direction) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterMessageUpdates(..) is not stubbed");
    }

    @Override
    public NewsletterMessageHistory queryNewsletterMessageUpdates(JidProvider newsletterProvider, int count, Instant since, NewsletterHistoryDirection direction) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterMessageUpdates(..) is not stubbed");
    }

    @Override
    public NewsletterMessageHistory queryNewsletterMessages(JidProvider newsletterProvider, int count) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterMessages(..) is not stubbed");
    }

    @Override
    public NewsletterMessageHistory queryNewsletterMessages(JidProvider newsletterProvider, int count, NewsletterViewerRole viewRole, NewsletterHistoryDirection direction) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterMessages(..) is not stubbed");
    }

    @Override
    public NewsletterMessageHistory queryNewsletterMessages(String inviteKey, int count) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterMessages(..) is not stubbed");
    }

    @Override
    public NewsletterMessageHistory queryNewsletterMessages(String inviteKey, int count, NewsletterViewerRole viewRole, NewsletterHistoryDirection direction) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterMessages(..) is not stubbed");
    }

    @Override
    public List<NewsletterQuestionResponse> queryNewsletterResponses(JidProvider newsletterProvider, long questionResponsesServerId, int questionResponsesCount) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterResponses(..) is not stubbed");
    }

    @Override
    public List<NewsletterQuestionResponse> queryNewsletterResponses(JidProvider newsletterProvider, long questionResponsesServerId, int questionResponsesCount, String questionResponsesBefore, NewsletterResponsesFilter filter, String searchText) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterResponses(..) is not stubbed");
    }

    @Override
    public NewsletterStatusHistory queryNewsletterStatusUpdates(JidProvider newsletterProvider, int count, NewsletterHistoryDirection direction) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterStatusUpdates(..) is not stubbed");
    }

    @Override
    public NewsletterStatusHistory queryNewsletterStatusUpdates(JidProvider newsletterProvider, int count, Instant since, NewsletterHistoryDirection direction) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterStatusUpdates(..) is not stubbed");
    }

    @Override
    public NewsletterStatusHistory queryNewsletterStatuses(JidProvider newsletterProvider, int count) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterStatuses(..) is not stubbed");
    }

    @Override
    public NewsletterStatusHistory queryNewsletterStatuses(JidProvider newsletterProvider, int count, NewsletterViewerRole viewRole, NewsletterHistoryDirection direction) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterStatuses(..) is not stubbed");
    }

    @Override
    public NewsletterStatusHistory queryNewsletterStatuses(String inviteKey, int count) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterStatuses(..) is not stubbed");
    }

    @Override
    public NewsletterStatusHistory queryNewsletterStatuses(String inviteKey, int count, NewsletterViewerRole viewRole, NewsletterHistoryDirection direction) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterStatuses(..) is not stubbed");
    }

    @Override
    public Map<Jid, List<NewsletterMyAddOn>> queryNewsletterMyAddOns(int limit, JidProvider newsletterProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNewsletterMyAddOns(..) is not stubbed");
    }

    @Override
    public Duration subscribeToNewsletterLiveUpdates(JidProvider newsletterProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: subscribeToNewsletterLiveUpdates(..) is not stubbed");
    }

    @Override
    public Optional<AbPropsBundle> queryExperimentConfig(String propsHash, Integer propsRefreshId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryExperimentConfig(..) is not stubbed");
    }

    @Override
    public Optional<AbPropsBundle> queryGroupExperimentConfig(JidProvider groupProvider, String propsHash) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryGroupExperimentConfig(..) is not stubbed");
    }

    @Override
    public Optional<BotDirectory> queryBotList(String botV, String botBhash, List<? extends JidProvider> botArgsProvider) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBotList(..) is not stubbed");
    }

    @Override
    public Optional<String> reportBug(BugReport report) {
        throw new UnsupportedOperationException("TestWhatsAppClient: reportBug(..) is not stubbed");
    }

    @Override
    public void reportInAppCommsEvent(InAppCommsEvent event) {
        throw new UnsupportedOperationException("TestWhatsAppClient: reportInAppCommsEvent(..) is not stubbed");
    }

    @Override
    public void acknowledgeOfflineBatch(int offlineBatchCount) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acknowledgeOfflineBatch(..) is not stubbed");
    }

    @Override
    public void enableActiveMode() {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableActiveMode(..) is not stubbed");
    }

    @Override
    public void enablePassiveMode() {
        throw new UnsupportedOperationException("TestWhatsAppClient: enablePassiveMode(..) is not stubbed");
    }

    @Override
    public Optional<PreKeyBundleResult> queryPreKeyBundles(List<PreKeyBundleRequest> users) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryPreKeyBundles(..) is not stubbed");
    }

    @Override
    public Optional<PreKeyBundleResult> queryMissingPreKeys(List<MissingPreKeyUserRequest> users) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryMissingPreKeys(..) is not stubbed");
    }

    @Override
    public Optional<SignedAttributionCredential> signAnonymousAttributionCredential(byte[] blindedCredentialElementValue, String projectNameElementValue) {
        throw new UnsupportedOperationException("TestWhatsAppClient: signAnonymousAttributionCredential(..) is not stubbed");
    }

    @Override
    public boolean queryPublicAnnouncementBlocked() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryPublicAnnouncementBlocked(..) is not stubbed");
    }

    @Override
    public void editPublicAnnouncementBlocked(PsaChatBlockAction blockingAction) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editPublicAnnouncementBlocked(..) is not stubbed");
    }

    @Override
    public void editPushConfig(PushConfig config) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editPushConfig(..) is not stubbed");
    }


    @Override
    public void sendSupportFeedback(JidProvider fromProvider, String messageId, List<String> feedbackKinds) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendSupportFeedback(..) is not stubbed");
    }

    @Override
    public Optional<SupportTicketAcknowledgement> sendSupportContactForm(SupportContactForm form) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendSupportContactForm(..) is not stubbed");
    }

    @Override
    public void reportIndividualForSpam(IndividualSpamReport report) {
        throw new UnsupportedOperationException("TestWhatsAppClient: reportIndividualForSpam(..) is not stubbed");
    }

    @Override
    public void reportGroupForSpam(GroupSpamReport report) {
        throw new UnsupportedOperationException("TestWhatsAppClient: reportGroupForSpam(..) is not stubbed");
    }

    @Override
    public void reportNewsletterForSpam(NewsletterSpamReport report) {
        throw new UnsupportedOperationException("TestWhatsAppClient: reportNewsletterForSpam(..) is not stubbed");
    }

    @Override
    public void reportStatus(LinkedMessageInfo status, String reason, String subject) {
        throw new UnsupportedOperationException("TestWhatsAppClient: reportStatus(..) is not stubbed");
    }

    @Override
    public void joinUnifiedSession(String unifiedSessionId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: joinUnifiedSession(..) is not stubbed");
    }

    @Override
    public Optional<UserNoticeBundle> queryPendingUserNotices(Instant getUserDisclosuresT) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryPendingUserNotices(..) is not stubbed");
    }

    @Override
    public List<UserNoticeStage> queryUserNoticeStages(List<UserNoticeStageQuery> queries) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryUserNoticeStages(..) is not stubbed");
    }

    @Override
    public CallLink createCallLink(CallLinkCreate create) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createCallLink(..) is not stubbed");
    }

    @Override
    public Optional<CallLink> queryCallLink(String token, CallLinkMedia media, String action) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryCallLink(..) is not stubbed");
    }

    @Override
    public void enableCallLinkWaitingRoom(URI link) {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableCallLinkWaitingRoom(..) is not stubbed");
    }

    @Override
    public void disableCallLinkWaitingRoom(URI link) {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableCallLinkWaitingRoom(..) is not stubbed");
    }

    @Override
    public Optional<FederatedIdentityState> checkFederatedIdentityExists(Instant timestamp) {
        throw new UnsupportedOperationException("TestWhatsAppClient: checkFederatedIdentityExists(..) is not stubbed");
    }

    @Override
    public Optional<FederatedIdentityPing> sendFederatedIdentityPing(FederatedRsaEncryption encryption, Instant timestamp, byte[] fbid) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendFederatedIdentityPing(..) is not stubbed");
    }

    @Override
    public Optional<FederatedIdentityCertificate> queryFederatedIdentityCertificate(Instant timestamp, boolean hasPayloadEncCertificates, boolean hasPasswordPem) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryFederatedIdentityCertificate(..) is not stubbed");
    }

    @Override
    public Optional<FederatedAccessTokenRefresh> refreshFederatedIdentityAccessTokens(FederatedRsaEncryption encryption, Instant timestamp, byte[] fbid) {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshFederatedIdentityAccessTokens(..) is not stubbed");
    }

    @Override
    public Optional<FederatedEncryptedAction> sendFederatedIdentityEncryptedPayload(FederatedRsaEncryption encryption, Instant timestamp, byte[] fbid, byte[] action) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendFederatedIdentityEncryptedPayload(..) is not stubbed");
    }

    @Override
    public FederatedEnterpriseCustomer createEnterpriseAuthenticatedCustomer(EnterpriseAuthenticatedCustomerCreate create) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createEnterpriseAuthenticatedCustomer(..) is not stubbed");
    }

    @Override
    public String queryAbPropString(ABProp prop) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAbPropString(..) is not stubbed");
    }

    @Override
    public boolean queryAbPropBool(ABProp prop) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAbPropBool(..) is not stubbed");
    }

    @Override
    public int queryAbPropInt(ABProp prop) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAbPropInt(..) is not stubbed");
    }

    @Override
    public long queryAbPropLong(ABProp prop) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAbPropLong(..) is not stubbed");
    }

    @Override
    public double queryAbPropDouble(ABProp prop) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAbPropDouble(..) is not stubbed");
    }

    @Override
    public void enableWebBetaEnrollment() {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableWebBetaEnrollment(..) is not stubbed");
    }

    @Override
    public void disableWebBetaEnrollment() {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableWebBetaEnrollment(..) is not stubbed");
    }

    @Override
    public void enableAlwaysRelayCalls() {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableAlwaysRelayCalls(..) is not stubbed");
    }

    @Override
    public void disableAlwaysRelayCalls() {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableAlwaysRelayCalls(..) is not stubbed");
    }

    @Override
    public void enableAiPrivateProcessing() {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableAiPrivateProcessing(..) is not stubbed");
    }

    @Override
    public void disableAiPrivateProcessing() {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableAiPrivateProcessing(..) is not stubbed");
    }

    @Override
    public void enableAutomatedDetections() {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableAutomatedDetections(..) is not stubbed");
    }

    @Override
    public void disableAutomatedDetections() {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableAutomatedDetections(..) is not stubbed");
    }

    @Override
    public void dismissOnboardingHint(String hintId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: dismissOnboardingHint(..) is not stubbed");
    }

    @Override
    public void restoreOnboardingHint(String hintId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: restoreOnboardingHint(..) is not stubbed");
    }

    @Override
    public void disableInteractiveMessageButton(String buttonId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableInteractiveMessageButton(..) is not stubbed");
    }

    @Override
    public void editRecentEmojiUsage(List<RecentEmojiWeight> usage) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editRecentEmojiUsage(..) is not stubbed");
    }

    @Override
    public void editContact(ContactEdit edit) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editContact(..) is not stubbed");
    }

    @Override
    public void deleteContact(JidProvider contact) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteContact(..) is not stubbed");
    }

    @Override
    public void addCallLog(CallLog entry) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCallLog(..) is not stubbed");
    }

    @Override
    public void enableAdvertiserDataSharing(Jid customer) {
        throw new UnsupportedOperationException("TestWhatsAppClient: enableAdvertiserDataSharing(..) is not stubbed");
    }

    @Override
    public void disableAdvertiserDataSharing(Jid customer) {
        throw new UnsupportedOperationException("TestWhatsAppClient: disableAdvertiserDataSharing(..) is not stubbed");
    }

    @Override
    public void editCustomPaymentMethods(List<CustomPaymentMethod> methods) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editCustomPaymentMethods(..) is not stubbed");
    }

    @Override
    public void acceptPaymentTos(PaymentTosAction.PaymentNotice notice) {
        throw new UnsupportedOperationException("TestWhatsAppClient: acceptPaymentTos(..) is not stubbed");
    }

    @Override
    public void revokePaymentTos(PaymentTosAction.PaymentNotice notice) {
        throw new UnsupportedOperationException("TestWhatsAppClient: revokePaymentTos(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProduct> addCatalogProduct(JidProvider bizJid, CatalogProductInfo productInfo) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCatalogProduct(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProduct> addCatalogProduct(BusinessCatalogProductCreate input) {
        throw new UnsupportedOperationException("TestWhatsAppClient: addCatalogProduct(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProduct> editCatalogProduct(JidProvider bizJid, String productId, CatalogProductInfo productInfo) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editCatalogProduct(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProduct> editCatalogProduct(BusinessCatalogProductEdit input) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editCatalogProduct(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogMutationResult> deleteCatalogProduct(JidProvider bizJid, List<String> productIds) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteCatalogProduct(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogMutationResult> createCatalog(JidProvider bizJid) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createCatalog(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogMutationResult> createCatalog(JidProvider bizJid, String platform) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createCatalog(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProductCollection> createCatalogCollection(JidProvider bizJid, String name, List<String> productIds) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createCatalogCollection(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProductCollection> createCatalogCollection(BusinessCatalogCollectionCreate input) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createCatalogCollection(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogMutationResult> deleteCatalogCollections(List<String> collectionIds, JidProvider bizJid, String catalogSessionId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteCatalogCollections(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProductCollection> updateCatalogCollection(BusinessCatalogCollectionEdit input) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateCatalogCollection(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogMutationResult> reorderCatalogCollections(JidProvider bizJid, List<BusinessCatalogCollectionMove> moves) {
        throw new UnsupportedOperationException("TestWhatsAppClient: reorderCatalogCollections(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogMutationResult> appealCatalogProduct(JidProvider jid, String productId, String reason) {
        throw new UnsupportedOperationException("TestWhatsAppClient: appealCatalogProduct(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogMutationResult> appealCatalogCollection(String productSetId, JidProvider jid, String reason) {
        throw new UnsupportedOperationException("TestWhatsAppClient: appealCatalogCollection(..) is not stubbed");
    }

    @Override
    public boolean updateCatalogCommerceSettings(JidProvider bizJid, boolean cartEnabled) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateCatalogCommerceSettings(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogMutationResult> updateCatalogProductVisibility(JidProvider jid, List<BusinessCatalogProductVisibility> products) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateCatalogProductVisibility(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogPage> fetchCatalog(JidProvider jid) {
        throw new UnsupportedOperationException("TestWhatsAppClient: fetchCatalog(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogPage> fetchCatalog(JidProvider jid, CatalogFetchOptions options) {
        throw new UnsupportedOperationException("TestWhatsAppClient: fetchCatalog(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogCollections> fetchCatalogCollections(JidProvider bizJid) {
        throw new UnsupportedOperationException("TestWhatsAppClient: fetchCatalogCollections(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogCollections> fetchCatalogCollections(JidProvider bizJid, CatalogFetchOptions options) {
        throw new UnsupportedOperationException("TestWhatsAppClient: fetchCatalogCollections(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProduct> fetchCatalogProduct(JidProvider jid, String productId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: fetchCatalogProduct(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProduct> fetchCatalogProduct(JidProvider jid, String productId, CatalogFetchOptions options) {
        throw new UnsupportedOperationException("TestWhatsAppClient: fetchCatalogProduct(..) is not stubbed");
    }

    @Override
    public List<BusinessProduct> fetchCatalogProductList(JidProvider jid, List<String> productIds) {
        throw new UnsupportedOperationException("TestWhatsAppClient: fetchCatalogProductList(..) is not stubbed");
    }

    @Override
    public List<BusinessProduct> fetchCatalogProductList(JidProvider jid, List<String> productIds, CatalogFetchOptions options) {
        throw new UnsupportedOperationException("TestWhatsAppClient: fetchCatalogProductList(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProductCollection> fetchCatalogSingleCollection(JidProvider bizJid, String id) {
        throw new UnsupportedOperationException("TestWhatsAppClient: fetchCatalogSingleCollection(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProductCollection> fetchCatalogSingleCollection(JidProvider bizJid, String id, CatalogFetchOptions options) {
        throw new UnsupportedOperationException("TestWhatsAppClient: fetchCatalogSingleCollection(..) is not stubbed");
    }

    @Override
    public boolean queryCatalogHasCategories(JidProvider bizJid) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryCatalogHasCategories(..) is not stubbed");
    }

    @Override
    public boolean queryCatalogHasCategories(JidProvider bizJid, CatalogFetchOptions options) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryCatalogHasCategories(..) is not stubbed");
    }

    @Override
    public List<BusinessProduct> queryProductListJob(JidProvider catalogJid, List<String> productIds) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryProductListJob(..) is not stubbed");
    }

    @Override
    public List<BusinessProduct> queryProductListJob(JidProvider catalogJid, List<String> productIds, CatalogFetchOptions options) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryProductListJob(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProductCollection> querySingleProductCollection(JidProvider catalogJid, String collectionId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: querySingleProductCollection(..) is not stubbed");
    }

    @Override
    public Optional<BusinessProductCollection> querySingleProductCollection(JidProvider catalogJid, String collectionId, CatalogFetchOptions options) {
        throw new UnsupportedOperationException("TestWhatsAppClient: querySingleProductCollection(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiAgentHome> queryAiAbilities() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAiAbilities(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> createAiChatHistory(AiChatHistoryUploadRequest input) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createAiChatHistory(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> deleteAiExampleResponses(List<String> knowledgeTypesToDelete) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteAiExampleResponses(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> deleteAiExampleResponses(List<String> knowledgeTypesToDelete, List<String> faqIds) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteAiExampleResponses(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiAgentHome> queryAiExampleResponses() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAiExampleResponses(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> updateAiExampleResponses(List<AiFaqEntry> faq) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateAiExampleResponses(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiKnowledgeReview> queryAiKnowledgeReview() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAiKnowledgeReview(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiKnowledgeReview> queryAiKnowledgeReview(Instant timestamp) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAiKnowledgeReview(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> commitAiKnowledgeReview(List<String> ids) {
        throw new UnsupportedOperationException("TestWhatsAppClient: commitAiKnowledgeReview(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> deleteAiPendingKnowledge(String id) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteAiPendingKnowledge(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> deleteAiChatHistorySource() {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteAiChatHistorySource(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> deleteAiWebsiteSource(String websiteDataSourceId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteAiWebsiteSource(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> deleteAiFileSource(String uploadedFileDataSourceId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteAiFileSource(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiAgentHome> queryAiKnowledgeSources() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAiKnowledgeSources(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> uploadAiKnowledgeSource(String manifoldFilePath, String userProvidedFileName) {
        throw new UnsupportedOperationException("TestWhatsAppClient: uploadAiKnowledgeSource(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiLeadGenForm> createAiLeadGenFlow(AiLeadGenFlowInput request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createAiLeadGenFlow(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiLeadGenForm> updateAiLeadGenFlow(AiLeadGenFlowInput request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateAiLeadGenFlow(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> deleteAiLeadGenFlow(String flowId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteAiLeadGenFlow(..) is not stubbed");
    }

    @Override
    public List<BusinessAiLeadGenForm> queryAiLeadGenForms() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAiLeadGenForms(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> markAiLeadGenAllSeen(String flowId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: markAiLeadGenAllSeen(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiProductInfo> createAiProductInfo(String name, String priceJson, String description) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createAiProductInfo(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiProductInfo> createAiProductInfo(BusinessAiProductInfoCreate input) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createAiProductInfo(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiProductInfo> updateAiProductInfo(BusinessAiProductInfoEdit input) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateAiProductInfo(..) is not stubbed");
    }

    @Override
    public List<BusinessAiMutationResult> deleteAiProductInfo(List<String> ids) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteAiProductInfo(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> updateAiReengagement(Boolean enabled, Long amount) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateAiReengagement(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> updateAiReplyBotEnabledTime(boolean enabled) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateAiReplyBotEnabledTime(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> updateAiReplyBotEnabledTime(BusinessAiReplyBotSchedule schedule) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateAiReplyBotEnabledTime(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> updateAiReplyChatTrigger(String triggerChatType) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateAiReplyChatTrigger(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiReplySettings> queryAiReplySettings() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAiReplySettings(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiRule> createAiRule(AiRuleInput request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createAiRule(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiRule> updateAiRule(AiRuleInput request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateAiRule(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> deleteAiRule(String ruleId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteAiRule(..) is not stubbed");
    }

    @Override
    public List<BusinessAiRule> generateAiRules() {
        throw new UnsupportedOperationException("TestWhatsAppClient: generateAiRules(..) is not stubbed");
    }

    @Override
    public Optional<BusinessOrder> createBusinessOrder(JidProvider seller, List<BusinessOrderItem> products, String directConnectionEncryptedInfo) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createBusinessOrder(..) is not stubbed");
    }

    @Override
    public MessageKey updateOrderStatus(JidProvider chat, BusinessOrder order, OrderLifecycleStatus status, OrderPaymentStatus paymentStatus) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateOrderStatus(..) is not stubbed");
    }

    @Override
    public MessageKey updateOrderPaymentStatus(JidProvider chat, BusinessOrder order, OrderLifecycleStatus status, OrderPaymentStatus paymentStatus) {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateOrderPaymentStatus(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCustomUrlIdentity> queryCustomUrlUser(String path) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryCustomUrlUser(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCustomUrlIdentity> queryCustomUrlUserProfile(String path) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryCustomUrlUserProfile(..) is not stubbed");
    }

    @Override
    public List<BusinessCategory> queryBusinessProfileCategories(String query, String locale) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessProfileCategories(..) is not stubbed");
    }

    @Override
    public List<BusinessCategoryNode> queryBusinessProfileCategoryTree(String query, String locale) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessProfileCategoryTree(..) is not stubbed");
    }

    @Override
    public List<BusinessPriceTier> queryBusinessPriceTiers(String locale) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessPriceTiers(..) is not stubbed");
    }

    @Override
    public List<BusinessWebsiteLink> queryBusinessProfileShimlinks(JidProvider biz) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessProfileShimlinks(..) is not stubbed");
    }

    @Override
    public Optional<BusinessRefreshedCart> refreshBusinessCart(JidProvider biz, List<String> productIds) {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshBusinessCart(..) is not stubbed");
    }

    @Override
    public Optional<BusinessRefreshedCart> refreshBusinessCart(BusinessCartRefreshOptions options) {
        throw new UnsupportedOperationException("TestWhatsAppClient: refreshBusinessCart(..) is not stubbed");
    }

    @Override
    public List<BusinessAddressSuggestion> queryBusinessAddressAutocomplete(BusinessAddressAutocompleteQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessAddressAutocomplete(..) is not stubbed");
    }

    @Override
    public Optional<BusinessMerchantCompliance> setMerchantCompliance(MerchantComplianceEdit edit) {
        throw new UnsupportedOperationException("TestWhatsAppClient: setMerchantCompliance(..) is not stubbed");
    }

    @Override
    public Optional<BusinessCatalogPublicKey> queryCatalogPublicKey(JidProvider biz) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryCatalogPublicKey(..) is not stubbed");
    }

    @Override
    public boolean reportCatalogProduct(JidProvider catalog, String productId, String reason) {
        throw new UnsupportedOperationException("TestWhatsAppClient: reportCatalogProduct(..) is not stubbed");
    }

    @Override
    public Optional<BusinessBroadcastBillingAccount> queryBroadcastBillingInfo(String assetId, Long budget, String entrypoint) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBroadcastBillingInfo(..) is not stubbed");
    }

    @Override
    public Optional<BusinessBroadcastTargetInfo> queryBroadcastBusinessInfo(Boolean shouldReturnAdAccount) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBroadcastBusinessInfo(..) is not stubbed");
    }

    @Override
    public Optional<BusinessBroadcastGenAiRecommendation> queryBroadcastGenAiRecommendation(BusinessBroadcastGenAiRecommendationQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBroadcastGenAiRecommendation(..) is not stubbed");
    }

    @Override
    public Optional<BusinessBroadcastQuota> queryBroadcastQuota(BusinessBroadcastQuotaData data) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBroadcastQuota(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiMutationResult> setAiAutoReplyControl(JidProvider consumerLid, String phoneNumber, BusinessAiAutoReplyState threadStatus) {
        throw new UnsupportedOperationException("TestWhatsAppClient: setAiAutoReplyControl(..) is not stubbed");
    }

    @Override
    public Optional<BusinessMarketingCampaign> createMarketingCampaign(BusinessMarketingCampaignCreate campaign) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createMarketingCampaign(..) is not stubbed");
    }

    @Override
    public List<CtwaConversionEvent> query3pdEventsByCustomLabels(List<String> customLabels, String exptGroup) {
        throw new UnsupportedOperationException("TestWhatsAppClient: query3pdEventsByCustomLabels(..) is not stubbed");
    }

    @Override
    public List<AdEntryPointEntitlement> queryClickToWhatsAppAdEntryPoints() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryClickToWhatsAppAdEntryPoints(..) is not stubbed");
    }

    @Override
    public List<AdEntryPointEntitlement> queryClickToWhatsAppAdEntryPointsWithCopy() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryClickToWhatsAppAdEntryPointsWithCopy(..) is not stubbed");
    }

    @Override
    public List<MetaAiMode> queryDynamicAiModes() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryDynamicAiModes(..) is not stubbed");
    }

    @Override
    public Optional<MetaAiSearchSuggestions> queryMetaAiSearchNullStateSuggestions(String locale, String nullStateSource, List<Integer> expConfig) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryMetaAiSearchNullStateSuggestions(..) is not stubbed");
    }

    @Override
    public Optional<String> queryOidcState() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryOidcState(..) is not stubbed");
    }

    @Override
    public Optional<BusinessSubscriptionEntryPoints> querySubscriptionEntryPoints() {
        throw new UnsupportedOperationException("TestWhatsAppClient: querySubscriptionEntryPoints(..) is not stubbed");
    }

    @Override
    public Optional<BusinessSubscriptions> queryBusinessSubscriptions(String platform) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessSubscriptions(..) is not stubbed");
    }

    @Override
    public Optional<BusinessFlowMetadata> queryFlowMetadata(JidProvider bizJid, String flowId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryFlowMetadata(..) is not stubbed");
    }

    @Override
    public List<FacebookPage> queryPromotableFacebookPages(String userId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryPromotableFacebookPages(..) is not stubbed");
    }

    @Override
    public List<BrandPhoneNumberMapping> queryPhoneNumbersForBrandIds(List<String> brandIds, Boolean lidBasedResponse) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryPhoneNumbersForBrandIds(..) is not stubbed");
    }

    @Override
    public Optional<WhatsAppAdsEligibility> queryWaaEligibility(String flowId, Instant requestId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryWaaEligibility(..) is not stubbed");
    }

    @Override
    public Optional<BusinessPostcodeVerification> verifyCatalogPostcode(JidProvider bizJid, String directConnectionEncryptedInfo) {
        throw new UnsupportedOperationException("TestWhatsAppClient: verifyCatalogPostcode(..) is not stubbed");
    }

    @Override
    public Optional<NativeMachineLearningModelManifest> queryNativeMlModelManifest(List<ModelRequestMetadata> modelRequestMetadatas, ClientCapabilityMetadata clientCapabilityMetadata) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNativeMlModelManifest(..) is not stubbed");
    }

    @Override
    public Optional<WhatsAppAdsAccountTypeReset> queryAccountTypeAndAdPage() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAccountTypeAndAdPage(..) is not stubbed");
    }

    @Override
    public Optional<WhatsAppAdsPageEligibility> queryPageEligibility(String pageId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryPageEligibility(..) is not stubbed");
    }

    @Override
    public Optional<SupportBugReportSubmission> submitSupportBugReport(SupportBugReportSubmissionRequest request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: submitSupportBugReport(..) is not stubbed");
    }

    @Override
    public Optional<SupportMessageFeedbackSubmission> submitSupportMessageFeedback(String messageId, List<SupportMessageFeedbackKind> feedbackTypes) {
        throw new UnsupportedOperationException("TestWhatsAppClient: submitSupportMessageFeedback(..) is not stubbed");
    }

    @Override
    public Optional<WhatsAppAdsAdAccount> onboardWaaAccount(String flowId, Instant requestId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: onboardWaaAccount(..) is not stubbed");
    }

    @Override
    public Optional<CrossPostingServiceData> queryCrossPostingServiceData() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryCrossPostingServiceData(..) is not stubbed");
    }

    @Override
    public boolean updateGlobalPrivacyControlOptOut() {
        throw new UnsupportedOperationException("TestWhatsAppClient: updateGlobalPrivacyControlOptOut(..) is not stubbed");
    }

    @Override
    public Optional<CrossPostingEligibility> checkCrossPostingEligibility(CrossPostingEligibilityQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: checkCrossPostingEligibility(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAccountNonce> queryBusinessAccountNonce(String scope) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBusinessAccountNonce(..) is not stubbed");
    }

    @Override
    public Optional<BusinessLinkedAdAccounts> queryRelayLinkedAccounts() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryRelayLinkedAccounts(..) is not stubbed");
    }

    @Override
    public Optional<AuthorizedAgentFeaturePolicy> queryAgentFeaturePolicy() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAgentFeaturePolicy(..) is not stubbed");
    }

    @Override
    public Optional<BusinessPlatformAuthToken> exchangeBusinessPlatformAuthCode(long applicationId, String code) {
        throw new UnsupportedOperationException("TestWhatsAppClient: exchangeBusinessPlatformAuthCode(..) is not stubbed");
    }

    @Override
    public boolean queryWebSessionUserValidity() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryWebSessionUserValidity(..) is not stubbed");
    }

    @Override
    public Optional<AiChannelAgentStatus> queryAiChannelAgentStatus() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAiChannelAgentStatus(..) is not stubbed");
    }

    @Override
    public List<AiChannelCommand> queryAiChannelCommands() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAiChannelCommands(..) is not stubbed");
    }

    @Override
    public Optional<AiChannelIdentity> queryAiChannelIdentity() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAiChannelIdentity(..) is not stubbed");
    }

    @Override
    public Optional<AiChannelLinkedStatus> queryAiChannelLinkedStatus() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAiChannelLinkedStatus(..) is not stubbed");
    }

    @Override
    public Optional<ExternalChatDeepLinkAuthorization> authorizeExternalChatDeepLink(ExternalChatDeepLinkAuthorizationOptions options) {
        throw new UnsupportedOperationException("TestWhatsAppClient: authorizeExternalChatDeepLink(..) is not stubbed");
    }

    @Override
    public Optional<FacebookOidcAccessToken> exchangeOidcCodeForAccessToken(String code, String state) {
        throw new UnsupportedOperationException("TestWhatsAppClient: exchangeOidcCodeForAccessToken(..) is not stubbed");
    }

    @Override
    public Optional<BusinessSignupMetadata> querySignupMetadata(String signupId, String phoneNumber) {
        throw new UnsupportedOperationException("TestWhatsAppClient: querySignupMetadata(..) is not stubbed");
    }

    @Override
    public Optional<WhatsAppAdsIdentityPage> createWhatsAppAdsIdentity(String phoneNumber, String code) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createWhatsAppAdsIdentity(..) is not stubbed");
    }

    @Override
    public List<AdMediaRegistration> linkMediaToNativeAd(List<AdMediaLink> mediaList) {
        throw new UnsupportedOperationException("TestWhatsAppClient: linkMediaToNativeAd(..) is not stubbed");
    }

    @Override
    public List<AdMediaUpload> uploadAdMedia(AdMediaUploadOptions options) {
        throw new UnsupportedOperationException("TestWhatsAppClient: uploadAdMedia(..) is not stubbed");
    }

    @Override
    public Optional<MetaAiSearchSuggestions> queryMetaAiSearchTypeAheadSuggestions(MetaAiSearchTypeAheadQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryMetaAiSearchTypeAheadSuggestions(..) is not stubbed");
    }

    @Override
    public Optional<NativeAdsEligibility> queryNativeAdsEligibility(String phoneNumber) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryNativeAdsEligibility(..) is not stubbed");
    }

    @Override
    public List<QuickPromotionSurfaceBatch> queryQuickPromotions(List<String> surfaceIds, QuickPromotionTriggerContext triggerContext) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryQuickPromotions(..) is not stubbed");
    }

    @Override
    public List<QuickPromotionSurfaceBatch> queryConsumerQuickPromotions(List<String> surfaceIds, QuickPromotionTriggerContext triggerContext) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryConsumerQuickPromotions(..) is not stubbed");
    }

    @Override
    public Optional<QuickPromotionLogAcknowledgement> logPromotionAction(QuickPromotionActionLog log) {
        throw new UnsupportedOperationException("TestWhatsAppClient: logPromotionAction(..) is not stubbed");
    }

    @Override
    public Optional<QuickPromotionLogAcknowledgement> logConsumerPromotionAction(QuickPromotionActionLog log) {
        throw new UnsupportedOperationException("TestWhatsAppClient: logConsumerPromotionAction(..) is not stubbed");
    }

    @Override
    public Optional<AnonymousCredentialServiceConfig> queryAnonymousCredentialServiceConfig(String projectName) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAnonymousCredentialServiceConfig(..) is not stubbed");
    }

    @Override
    public Optional<AnonymousCredentialIssuance> issueAnonymousCredentials(AnonymousCredentialIssuanceRequest request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: issueAnonymousCredentials(..) is not stubbed");
    }

    @Override
    public List<BotProfile> queryBotProfiles(List<String> personaIds) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryBotProfiles(..) is not stubbed");
    }

    @Override
    public Optional<SupportContactFormSubmission> submitSupportContactForm(String description, String diagnosticsJson, SupportContactFormContextFlow contextFlow) {
        throw new UnsupportedOperationException("TestWhatsAppClient: submitSupportContactForm(..) is not stubbed");
    }

    @Override
    public Optional<GroupSuspensionAppeal> appealGroupSuspension(JidProvider groupJid, String appealReason, String clientDebugBundle) {
        throw new UnsupportedOperationException("TestWhatsAppClient: appealGroupSuspension(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdDraft> createAdDraft(LwiBoostedComponentInput input) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createAdDraft(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdDraft> editAdDraft(LwiBoostedComponentInput input) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editAdDraft(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdMutationResult> deleteAdDraft(String draftId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteAdDraft(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdMutationResult> deleteAd(String boostId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteAd(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdMutationResult> pauseAd(String boostId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: pauseAd(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdMutationResult> resumeAd(String boostId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: resumeAd(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdMutationResult> certifyAd(String inputJson) {
        throw new UnsupportedOperationException("TestWhatsAppClient: certifyAd(..) is not stubbed");
    }

    @Override
    public Optional<BusinessBoostedComponent> createBoostedComponent(LwiBoostedComponentInput input) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createBoostedComponent(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdAccount> queryAdAccountDetails(String adAccountId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdAccountDetails(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdBudgetOptions> queryAdBudgetOptions(String legacyAdAccountId, Long budget, String currency) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdBudgetOptions(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdPaymentSection> queryAdPaymentSection(String assetId, Long budget) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdPaymentSection(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdMutationResult> sendAdPaymentNotification() {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendAdPaymentNotification(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdPaymentSection> queryAdBillingSetupRequired(String assetId, Long budget) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdBillingSetupRequired(..) is not stubbed");
    }

    @Override
    public Optional<String> queryAdCertificationRequired() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdCertificationRequired(..) is not stubbed");
    }

    @Override
    public boolean queryAdAccountFeatureFlag(String accountId, Boolean defaultValue, Boolean checkWithMultipleAdAccounts, Boolean recordCheck, String flagName, Boolean shouldFetch, String flagGroupName) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdAccountFeatureFlag(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdMutationResult> confirmAdEmailOnboarding(BusinessAdEmailOnboardingConfirmation confirmation) {
        throw new UnsupportedOperationException("TestWhatsAppClient: confirmAdEmailOnboarding(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdMutationResult> sendAdEmailVerificationCode(BusinessAdEmailVerificationCodeRequest request) {
        throw new UnsupportedOperationException("TestWhatsAppClient: sendAdEmailVerificationCode(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdAudienceSection> queryAdAudienceSection(BusinessAdAudienceSectionQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdAudienceSection(..) is not stubbed");
    }

    @Override
    public List<BusinessAdTargetingDescription> queryAdTargetingSentences(BusinessAdTargetingSentencesQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdTargetingSentences(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdSavedAudience> createSavedAudience(String legacyAdAccountId, String targetingSpecString, String name) {
        throw new UnsupportedOperationException("TestWhatsAppClient: createSavedAudience(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdSavedAudience> editSavedAudience(String name, String savedAudienceId, String targetingSpecString) {
        throw new UnsupportedOperationException("TestWhatsAppClient: editSavedAudience(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdMutationResult> deleteSavedAudience(String savedAudienceId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: deleteSavedAudience(..) is not stubbed");
    }

    @Override
    public List<BusinessAdInterest> browseAdInterests(String adAccountId, String audiencePath) {
        throw new UnsupportedOperationException("TestWhatsAppClient: browseAdInterests(..) is not stubbed");
    }

    @Override
    public List<BusinessAdInterest> searchAdInterests(String query, String adAccountId, Integer count) {
        throw new UnsupportedOperationException("TestWhatsAppClient: searchAdInterests(..) is not stubbed");
    }

    @Override
    public List<BusinessAdInterest> suggestAdInterests(BusinessAdInterestSuggestionQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: suggestAdInterests(..) is not stubbed");
    }

    @Override
    public List<BusinessAdLocation> searchAdLocalLocations(String query, Integer first) {
        throw new UnsupportedOperationException("TestWhatsAppClient: searchAdLocalLocations(..) is not stubbed");
    }

    @Override
    public List<BusinessAdLocation> searchAdRegionalLocations(String query, Integer first, List<String> locationTypes) {
        throw new UnsupportedOperationException("TestWhatsAppClient: searchAdRegionalLocations(..) is not stubbed");
    }

    @Override
    public boolean queryAdTargetingEuComplianceStatus(String adAccountId, String targetSpecString) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdTargetingEuComplianceStatus(..) is not stubbed");
    }

    @Override
    public Optional<URI> queryAdImageUrl(String legacyAdAccountId, String imageHash) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdImageUrl(..) is not stubbed");
    }

    @Override
    public Optional<URI> queryAdVideoUrl(String videoId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdVideoUrl(..) is not stubbed");
    }

    @Override
    public Optional<URI> queryAdVideoThumbnailUrl(String videoId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdVideoThumbnailUrl(..) is not stubbed");
    }

    @Override
    public Optional<URI> queryAdPreviewVideo(String videoId) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdPreviewVideo(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdCreationScreen> queryAdCreationRoot(BusinessAdCreationRootQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdCreationRoot(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdManagementScreen> queryAdManagementRoot(BusinessAdManagementRootQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdManagementRoot(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdCreationSummary> queryAdCreationSummaryContent(String assetId, Long budget, String budgetType, String currency, Integer durationInDays) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdCreationSummaryContent(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdSuccessScreen> queryAdCreationSuccessModal(String assetId, Long budget) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdCreationSuccessModal(..) is not stubbed");
    }

    @Override
    public Optional<AdBudgetEstimate> queryEstimatedDailyReach(BusinessAdEstimatedReachQuery query) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryEstimatedDailyReach(..) is not stubbed");
    }

    @Override
    public Optional<AdTargetingTuningResult> adjustAdTargetingForRegulatedCategory(BusinessAdRegulatedCategoryTuning tuning) {
        throw new UnsupportedOperationException("TestWhatsAppClient: adjustAdTargetingForRegulatedCategory(..) is not stubbed");
    }

    @Override
    public Optional<AdTargetingTuningResult> adjustAdTargetingForRegulatedCategories(BusinessAdRegulatedCategoryBatchTuning tuning) {
        throw new UnsupportedOperationException("TestWhatsAppClient: adjustAdTargetingForRegulatedCategories(..) is not stubbed");
    }

    @Override
    public Optional<AdTargetingComplianceStatus> queryAdTargetingEuDigitalServicesActStatus(String adAccountId, String targetingSpec) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdTargetingEuDigitalServicesActStatus(..) is not stubbed");
    }

    @Override
    public Optional<AdTargetingComplianceStatus> queryAdTargetingTaiwanFinancialServicesStatus(String adAccountId, String targetingSpec) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdTargetingTaiwanFinancialServicesStatus(..) is not stubbed");
    }

    @Override
    public Optional<AdTargetingComplianceStatus> queryAdTargetingAustraliaFinancialServicesStatus(String adAccountId, String targetingSpec) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdTargetingAustraliaFinancialServicesStatus(..) is not stubbed");
    }

    @Override
    public Optional<AdTargetingComplianceStatus> queryAdTargetingSingaporeUniversalCategoryStatus(String adAccountId, String targetingSpec) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdTargetingSingaporeUniversalCategoryStatus(..) is not stubbed");
    }

    @Override
    public Optional<AdTargetingComplianceStatus> queryAdTargetingIndiaFinancialServicesStatus(String adAccountId, String targetingSpec) {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdTargetingIndiaFinancialServicesStatus(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAiToolsEligibility> queryAiToolsEligibility() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAiToolsEligibility(..) is not stubbed");
    }

    @Override
    public Optional<BusinessAdBillingActor> queryAdBillingInfoProfile() {
        throw new UnsupportedOperationException("TestWhatsAppClient: queryAdBillingInfoProfile(..) is not stubbed");
    }
}
