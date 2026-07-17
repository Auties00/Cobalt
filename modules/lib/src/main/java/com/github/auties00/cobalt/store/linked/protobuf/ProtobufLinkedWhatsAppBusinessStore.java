package com.github.auties00.cobalt.store.linked.protobuf;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.linked.bot.AiThreadTitle;
import com.github.auties00.cobalt.wire.linked.bot.BotWelcomeRequestState;
import com.github.auties00.cobalt.wire.linked.business.AgentState;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastCampaign;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastInsight;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastList;
import com.github.auties00.cobalt.wire.linked.business.BusinessCampaignStatus;
import com.github.auties00.cobalt.wire.linked.business.BusinessFeatureFlag;
import com.github.auties00.cobalt.wire.linked.business.BusinessSubscription;
import com.github.auties00.cobalt.wire.linked.business.MarketingMessage;
import com.github.auties00.cobalt.wire.linked.business.MarketingMessageBroadcast;
import com.github.auties00.cobalt.wire.linked.business.NoteState;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaDataSharingPreference;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaDataSharingSetting;
import com.github.auties00.cobalt.wire.linked.chat.ChatAssignment;
import com.github.auties00.cobalt.wire.linked.chat.InteractiveMessageState;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.payment.OrphanPaymentNotification;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.MaibaAIFeaturesControlAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.MusicUserIdAction;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.CustomPaymentMethod;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.MerchantPaymentPartnerAction;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentTosAction;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The {@link LinkedWhatsAppBusinessStore} holding this session's WhatsApp Business and payments state.
 *
 * <p>This sub-store of {@link ProtobufWhatsAppStore} holds only transient, runtime-rebuilt state, so
 * unlike the other sub-stores it is not a protobuf message: the owning aggregate allocates a fresh
 * instance on every construction and never serializes it.
 *
 * @implNote
 * This implementation backs every collection with a {@link ConcurrentHashMap} (or
 * {@link ArrayList} for the custom payment methods) allocated in the no-argument constructor; all
 * scalar fields start {@code null} or {@code false}.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
final class ProtobufLinkedWhatsAppBusinessStore implements LinkedWhatsAppBusinessStore {
    /**
     * The logger for {@link ProtobufLinkedWhatsAppBusinessStore}.
     */
    private static final System.Logger LOGGER = Log.get(ProtobufLinkedWhatsAppBusinessStore.class);

    /**
     * The per-account business feature flags keyed by flag name.
     */
    private final ConcurrentMap<String, BusinessFeatureFlag> businessFeatureFlags;

    /**
     * The business marketing-campaign statuses keyed by campaign id.
     */
    private final ConcurrentMap<String, BusinessCampaignStatus> businessCampaignStatuses;

    /**
     * The business paid subscriptions keyed by subscription id.
     */
    private final ConcurrentMap<String, BusinessSubscription> businessSubscriptions;

    /**
     * The Click-To-WhatsApp data-sharing preferences keyed by ads-account LID.
     */
    private final ConcurrentMap<String, CtwaDataSharingPreference> ctwaDataSharingPreferences;

    /**
     * The business agent states keyed by agent id.
     */
    private final ConcurrentMap<String, AgentState> agentStates;

    /**
     * The chat-to-agent assignments keyed by chat JID.
     */
    private final ConcurrentMap<Jid, ChatAssignment> chatAssignments;

    /**
     * The saved business marketing messages keyed by template id.
     */
    private final ConcurrentMap<String, MarketingMessage> marketingMessages;

    /**
     * The marketing-message broadcasts keyed by template id.
     */
    private final ConcurrentMap<String, MarketingMessageBroadcast> marketingMessageBroadcasts;

    /**
     * The configured business broadcast lists keyed by list id.
     */
    private final ConcurrentMap<String, BusinessBroadcastList> businessBroadcastLists;

    /**
     * The business broadcast campaigns keyed by campaign id.
     */
    private final ConcurrentMap<String, BusinessBroadcastCampaign> businessBroadcastCampaigns;

    /**
     * The per-campaign broadcast insight records keyed by campaign id.
     */
    private final ConcurrentMap<String, BusinessBroadcastInsight> businessBroadcastInsights;

    /**
     * The orphan payment notifications buffered before their triggering message arrives, keyed by message id.
     */
    private final ConcurrentMap<String, OrphanPaymentNotification> orphanPaymentNotifications;

    /**
     * The interactive-message UI states keyed by message id.
     */
    private final ConcurrentMap<String, InteractiveMessageState> interactiveMessageStates;

    /**
     * The note states keyed by note id.
     */
    private final ConcurrentMap<String, NoteState> noteStates;

    /**
     * The bot welcome-request states keyed by bot JID.
     */
    private final ConcurrentMap<Jid, BotWelcomeRequestState> botWelcomeRequestStates;

    /**
     * The AI-generated thread titles keyed by thread id.
     */
    private final ConcurrentMap<String, AiThreadTitle> aiThreadTitles;

    /**
     * The opaque nonce scoping business-only round trips.
     */
    private String businessAccountNonce;

    /**
     * Guards {@link #businessAccountNonce} mutations so the
     * {@link #nonceUpdated} condition signals every waiter atomically with
     * the slot write.
     */
    private final ReentrantLock nonceLock;

    /**
     * Signalled by {@link #setBusinessAccountNonce(String)} whenever the
     * slot value changes; awaited by
     * {@link #awaitBusinessAccountNonce(String, Duration)} to correlate
     * the asynchronous nonce-push notification with a calling refresh
     * round.
     */
    private final Condition nonceUpdated;

    /**
     * The Small-Business data-sharing consent string.
     */
    private String businessPrivacySetting;

    /**
     * The global Click-To-WhatsApp data-sharing setting; {@code null} until fetched, which callers
     * treat as {@link CtwaDataSharingSetting#NOT_SET}.
     */
    private CtwaDataSharingSetting ctwaDataSharingSetting;

    /**
     * The hash of the marketing-message opt-out list.
     */
    private String businessOptOutListHash;

    /**
     * Whether server-side outcome detection is enabled.
     */
    private boolean detectedOutcomesEnabled;

    /**
     * Whether the hosted-automation onboarding wizard is complete.
     */
    private boolean hostedAutomationOnboarded;

    /**
     * Whether Meta AI assistant features are enabled by the server.
     */
    private boolean aiAvailable;

    /**
     * The configured Customer-Payment-Instructions identifier.
     */
    private String paymentInstructionCpi;

    /**
     * The configured custom payment methods, in picker order.
     */
    private List<CustomPaymentMethod> customPaymentMethods;

    /**
     * The selected merchant payment partner.
     */
    private MerchantPaymentPartnerAction merchantPaymentPartner;

    /**
     * The payment Terms-of-Service acknowledgement state.
     */
    private PaymentTosAction paymentTos;

    /**
     * The music-service integration user identifier state.
     */
    private MusicUserIdAction musicUserIdState;

    /**
     * The user-created bot definition blob.
     */
    private byte[] userCreatedBotDefinition;

    /**
     * The business AI-agent feature status.
     */
    private MaibaAIFeaturesControlAction.MaibaAIFeatureStatus aiBusinessAgentStatus;

    /**
     * Constructs an empty business sub-store with freshly allocated backing collections.
     */
    ProtobufLinkedWhatsAppBusinessStore() {
        this.businessFeatureFlags = new ConcurrentHashMap<>();
        this.businessCampaignStatuses = new ConcurrentHashMap<>();
        this.businessSubscriptions = new ConcurrentHashMap<>();
        this.ctwaDataSharingPreferences = new ConcurrentHashMap<>();
        this.agentStates = new ConcurrentHashMap<>();
        this.chatAssignments = new ConcurrentHashMap<>();
        this.marketingMessages = new ConcurrentHashMap<>();
        this.marketingMessageBroadcasts = new ConcurrentHashMap<>();
        this.businessBroadcastLists = new ConcurrentHashMap<>();
        this.businessBroadcastCampaigns = new ConcurrentHashMap<>();
        this.businessBroadcastInsights = new ConcurrentHashMap<>();
        this.orphanPaymentNotifications = new ConcurrentHashMap<>();
        this.interactiveMessageStates = new ConcurrentHashMap<>();
        this.noteStates = new ConcurrentHashMap<>();
        this.botWelcomeRequestStates = new ConcurrentHashMap<>();
        this.aiThreadTitles = new ConcurrentHashMap<>();
        this.customPaymentMethods = new ArrayList<>();
        this.nonceLock = new ReentrantLock();
        this.nonceUpdated = nonceLock.newCondition();
    }

    @Override
    public Collection<BusinessFeatureFlag> businessFeatureFlags() {
        return List.copyOf(businessFeatureFlags.values());
    }

    @Override
    public Optional<BusinessFeatureFlag> findBusinessFeatureFlag(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(businessFeatureFlags.get(name));
    }

    @Override
    public LinkedWhatsAppBusinessStore putBusinessFeatureFlag(BusinessFeatureFlag flag) {
        Objects.requireNonNull(flag, "flag cannot be null");
        businessFeatureFlags.put(flag.name(), flag);
        return this;
    }

    @Override
    public Optional<BusinessFeatureFlag> removeBusinessFeatureFlag(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(businessFeatureFlags.remove(name));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearBusinessFeatureFlags() {
        businessFeatureFlags.clear();
        return this;
    }

    @Override
    public Collection<BusinessCampaignStatus> businessCampaignStatuses() {
        return List.copyOf(businessCampaignStatuses.values());
    }

    @Override
    public Optional<BusinessCampaignStatus> findBusinessCampaignStatus(String campaignId) {
        return campaignId == null ? Optional.empty() : Optional.ofNullable(businessCampaignStatuses.get(campaignId));
    }

    @Override
    public LinkedWhatsAppBusinessStore putBusinessCampaignStatus(BusinessCampaignStatus status) {
        Objects.requireNonNull(status, "status cannot be null");
        businessCampaignStatuses.put(status.campaignId(), status);
        return this;
    }

    @Override
    public Optional<BusinessCampaignStatus> removeBusinessCampaignStatus(String campaignId) {
        return campaignId == null ? Optional.empty() : Optional.ofNullable(businessCampaignStatuses.remove(campaignId));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearBusinessCampaignStatuses() {
        businessCampaignStatuses.clear();
        return this;
    }

    @Override
    public Collection<BusinessSubscription> businessSubscriptions() {
        return List.copyOf(businessSubscriptions.values());
    }

    @Override
    public Optional<BusinessSubscription> findBusinessSubscription(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(businessSubscriptions.get(id));
    }

    @Override
    public LinkedWhatsAppBusinessStore putBusinessSubscription(BusinessSubscription subscription) {
        Objects.requireNonNull(subscription, "subscription cannot be null");
        businessSubscriptions.put(subscription.id(), subscription);
        return this;
    }

    @Override
    public Optional<BusinessSubscription> removeBusinessSubscription(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(businessSubscriptions.remove(id));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearBusinessSubscriptions() {
        businessSubscriptions.clear();
        return this;
    }

    @Override
    public Optional<String> businessAccountNonce() {
        return Optional.ofNullable(businessAccountNonce);
    }

    @Override
    public LinkedWhatsAppBusinessStore setBusinessAccountNonce(String nonce) {
        nonceLock.lock();
        try {
            this.businessAccountNonce = nonce;
            nonceUpdated.signalAll();
        } finally {
            nonceLock.unlock();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "business account nonce updated: {0}", new LogRedactable.Token(nonce));
        return this;
    }

    @Override
    public Optional<String> awaitBusinessAccountNonce(String previous, Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "timeout cannot be null");
        var remaining = timeout.toNanos();
        nonceLock.lock();
        try {
            while (businessAccountNonce == null || Objects.equals(businessAccountNonce, previous)) {
                if (remaining <= 0L) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "business account nonce wait timed out after {0}", timeout);
                    }
                    return Optional.empty();
                }
                remaining = nonceUpdated.awaitNanos(remaining);
            }
            return Optional.of(businessAccountNonce);
        } finally {
            nonceLock.unlock();
        }
    }

    @Override
    public Collection<CtwaDataSharingPreference> ctwaDataSharingPreferences() {
        return List.copyOf(ctwaDataSharingPreferences.values());
    }

    @Override
    public Optional<CtwaDataSharingPreference> findCtwaDataSharing(String accountLid) {
        return accountLid == null ? Optional.empty() : Optional.ofNullable(ctwaDataSharingPreferences.get(accountLid));
    }

    @Override
    public LinkedWhatsAppBusinessStore putCtwaDataSharing(CtwaDataSharingPreference preference) {
        Objects.requireNonNull(preference, "preference cannot be null");
        ctwaDataSharingPreferences.put(preference.accountLid(), preference);
        return this;
    }

    @Override
    public Optional<CtwaDataSharingPreference> removeCtwaDataSharing(String accountLid) {
        return accountLid == null ? Optional.empty() : Optional.ofNullable(ctwaDataSharingPreferences.remove(accountLid));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearCtwaDataSharing() {
        ctwaDataSharingPreferences.clear();
        return this;
    }

    @Override
    public Optional<String> businessPrivacySetting() {
        return Optional.ofNullable(businessPrivacySetting);
    }

    @Override
    public LinkedWhatsAppBusinessStore setBusinessPrivacySetting(String consent) {
        this.businessPrivacySetting = consent;
        return this;
    }

    @Override
    public Optional<CtwaDataSharingSetting> ctwaDataSharingSetting() {
        return Optional.ofNullable(ctwaDataSharingSetting);
    }

    @Override
    public LinkedWhatsAppBusinessStore setCtwaDataSharingSetting(CtwaDataSharingSetting setting) {
        this.ctwaDataSharingSetting = setting;
        return this;
    }

    @Override
    public Optional<String> businessOptOutListHash() {
        return Optional.ofNullable(businessOptOutListHash);
    }

    @Override
    public LinkedWhatsAppBusinessStore setBusinessOptOutListHash(String hash) {
        this.businessOptOutListHash = hash;
        return this;
    }

    @Override
    public boolean hostedAutomationOnboarded() {
        return hostedAutomationOnboarded;
    }

    @Override
    public LinkedWhatsAppBusinessStore setHostedAutomationOnboarded(boolean onboarded) {
        this.hostedAutomationOnboarded = onboarded;
        return this;
    }

    @Override
    public boolean detectedOutcomesEnabled() {
        return detectedOutcomesEnabled;
    }

    @Override
    public LinkedWhatsAppBusinessStore setDetectedOutcomesEnabled(boolean enabled) {
        this.detectedOutcomesEnabled = enabled;
        return this;
    }

    @Override
    public boolean aiAvailable() {
        return aiAvailable;
    }

    @Override
    public LinkedWhatsAppBusinessStore setAiAvailable(boolean aiAvailable) {
        this.aiAvailable = aiAvailable;
        return this;
    }

    @Override
    public Optional<OrphanPaymentNotification> findOrphanPaymentNotification(String messageId) {
        return messageId == null ? Optional.empty() : Optional.ofNullable(orphanPaymentNotifications.get(messageId));
    }

    @Override
    public void addOrphanPaymentNotification(OrphanPaymentNotification notification) {
        Objects.requireNonNull(notification, "notification cannot be null");
        orphanPaymentNotifications.put(notification.messageId(), notification);
    }

    @Override
    public Optional<OrphanPaymentNotification> removeOrphanPaymentNotification(String messageId) {
        return messageId == null ? Optional.empty() : Optional.ofNullable(orphanPaymentNotifications.remove(messageId));
    }

    @Override
    public Collection<AgentState> agentStates() {
        return List.copyOf(agentStates.values());
    }

    @Override
    public Optional<AgentState> findAgentState(String agentId) {
        return agentId == null ? Optional.empty() : Optional.ofNullable(agentStates.get(agentId));
    }

    @Override
    public LinkedWhatsAppBusinessStore putAgentState(AgentState state) {
        Objects.requireNonNull(state, "state cannot be null");
        agentStates.put(state.agentId(), state);
        return this;
    }

    @Override
    public Optional<AgentState> removeAgentState(String agentId) {
        return agentId == null ? Optional.empty() : Optional.ofNullable(agentStates.remove(agentId));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearAgentStates() {
        agentStates.clear();
        return this;
    }

    @Override
    public Collection<ChatAssignment> chatAssignments() {
        return List.copyOf(chatAssignments.values());
    }

    @Override
    public Optional<ChatAssignment> findChatAssignment(Jid chatJid) {
        return chatJid == null ? Optional.empty() : Optional.ofNullable(chatAssignments.get(chatJid));
    }

    @Override
    public LinkedWhatsAppBusinessStore putChatAssignment(ChatAssignment assignment) {
        Objects.requireNonNull(assignment, "assignment cannot be null");
        chatAssignments.put(assignment.chatJid(), assignment);
        return this;
    }

    @Override
    public Optional<ChatAssignment> removeChatAssignment(Jid chatJid) {
        return chatJid == null ? Optional.empty() : Optional.ofNullable(chatAssignments.remove(chatJid));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearChatAssignments() {
        chatAssignments.clear();
        return this;
    }

    @Override
    public Optional<String> paymentInstructionCpi() {
        return Optional.ofNullable(paymentInstructionCpi);
    }

    @Override
    public LinkedWhatsAppBusinessStore setPaymentInstructionCpi(String cpi) {
        this.paymentInstructionCpi = cpi;
        return this;
    }

    @Override
    public List<CustomPaymentMethod> customPaymentMethods() {
        return List.copyOf(customPaymentMethods);
    }

    @Override
    public LinkedWhatsAppBusinessStore setCustomPaymentMethods(List<CustomPaymentMethod> methods) {
        this.customPaymentMethods = new ArrayList<>(Objects.requireNonNull(methods, "methods cannot be null"));
        return this;
    }

    @Override
    public Optional<MerchantPaymentPartnerAction> merchantPaymentPartner() {
        return Optional.ofNullable(merchantPaymentPartner);
    }

    @Override
    public LinkedWhatsAppBusinessStore setMerchantPaymentPartner(MerchantPaymentPartnerAction partner) {
        this.merchantPaymentPartner = partner;
        return this;
    }

    @Override
    public Optional<PaymentTosAction> paymentTos() {
        return Optional.ofNullable(paymentTos);
    }

    @Override
    public LinkedWhatsAppBusinessStore setPaymentTos(PaymentTosAction tos) {
        this.paymentTos = tos;
        return this;
    }

    @Override
    public Collection<MarketingMessage> marketingMessages() {
        return List.copyOf(marketingMessages.values());
    }

    @Override
    public Optional<MarketingMessage> findMarketingMessage(String templateId) {
        return templateId == null ? Optional.empty() : Optional.ofNullable(marketingMessages.get(templateId));
    }

    @Override
    public LinkedWhatsAppBusinessStore putMarketingMessage(MarketingMessage message) {
        Objects.requireNonNull(message, "message cannot be null");
        marketingMessages.put(message.templateId(), message);
        return this;
    }

    @Override
    public Optional<MarketingMessage> removeMarketingMessage(String templateId) {
        return templateId == null ? Optional.empty() : Optional.ofNullable(marketingMessages.remove(templateId));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearMarketingMessages() {
        marketingMessages.clear();
        return this;
    }

    @Override
    public Collection<MarketingMessageBroadcast> marketingMessageBroadcasts() {
        return List.copyOf(marketingMessageBroadcasts.values());
    }

    @Override
    public Optional<MarketingMessageBroadcast> findMarketingMessageBroadcast(String templateId) {
        return templateId == null ? Optional.empty() : Optional.ofNullable(marketingMessageBroadcasts.get(templateId));
    }

    @Override
    public LinkedWhatsAppBusinessStore putMarketingMessageBroadcast(MarketingMessageBroadcast broadcast) {
        Objects.requireNonNull(broadcast, "broadcast cannot be null");
        marketingMessageBroadcasts.put(broadcast.templateId(), broadcast);
        return this;
    }

    @Override
    public Optional<MarketingMessageBroadcast> removeMarketingMessageBroadcast(String templateId) {
        return templateId == null ? Optional.empty() : Optional.ofNullable(marketingMessageBroadcasts.remove(templateId));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearMarketingMessageBroadcasts() {
        marketingMessageBroadcasts.clear();
        return this;
    }

    @Override
    public Collection<BusinessBroadcastList> businessBroadcastLists() {
        return List.copyOf(businessBroadcastLists.values());
    }

    @Override
    public Optional<BusinessBroadcastList> findBusinessBroadcastList(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(businessBroadcastLists.get(id));
    }

    @Override
    public LinkedWhatsAppBusinessStore putBusinessBroadcastList(BusinessBroadcastList list) {
        Objects.requireNonNull(list, "list cannot be null");
        businessBroadcastLists.put(list.id(), list);
        return this;
    }

    @Override
    public Optional<BusinessBroadcastList> removeBusinessBroadcastList(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(businessBroadcastLists.remove(id));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearBusinessBroadcastLists() {
        businessBroadcastLists.clear();
        return this;
    }

    @Override
    public SequencedCollection<Jid> broadcasts() {
        return businessBroadcastLists.keySet().stream()
                .map(id -> Jid.of(id, JidServer.broadcast()))
                .toList();
    }

    @Override
    public Collection<BusinessBroadcastCampaign> businessBroadcastCampaigns() {
        return List.copyOf(businessBroadcastCampaigns.values());
    }

    @Override
    public Optional<BusinessBroadcastCampaign> findBusinessBroadcastCampaign(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(businessBroadcastCampaigns.get(id));
    }

    @Override
    public LinkedWhatsAppBusinessStore putBusinessBroadcastCampaign(BusinessBroadcastCampaign campaign) {
        Objects.requireNonNull(campaign, "campaign cannot be null");
        businessBroadcastCampaigns.put(campaign.id(), campaign);
        return this;
    }

    @Override
    public Optional<BusinessBroadcastCampaign> removeBusinessBroadcastCampaign(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(businessBroadcastCampaigns.remove(id));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearBusinessBroadcastCampaigns() {
        businessBroadcastCampaigns.clear();
        return this;
    }

    @Override
    public Collection<BusinessBroadcastInsight> businessBroadcastInsights() {
        return List.copyOf(businessBroadcastInsights.values());
    }

    @Override
    public Optional<BusinessBroadcastInsight> findBusinessBroadcastInsight(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(businessBroadcastInsights.get(id));
    }

    @Override
    public LinkedWhatsAppBusinessStore putBusinessBroadcastInsight(BusinessBroadcastInsight insight) {
        Objects.requireNonNull(insight, "insight cannot be null");
        businessBroadcastInsights.put(insight.id(), insight);
        return this;
    }

    @Override
    public Optional<BusinessBroadcastInsight> removeBusinessBroadcastInsight(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(businessBroadcastInsights.remove(id));
    }

    @Override
    public Collection<InteractiveMessageState> interactiveMessageStates() {
        return List.copyOf(interactiveMessageStates.values());
    }

    @Override
    public Optional<InteractiveMessageState> findInteractiveMessageState(String messageId) {
        return messageId == null ? Optional.empty() : Optional.ofNullable(interactiveMessageStates.get(messageId));
    }

    @Override
    public LinkedWhatsAppBusinessStore putInteractiveMessageState(InteractiveMessageState state) {
        Objects.requireNonNull(state, "state cannot be null");
        interactiveMessageStates.put(state.messageId(), state);
        return this;
    }

    @Override
    public Optional<InteractiveMessageState> removeInteractiveMessageState(String messageId) {
        return messageId == null ? Optional.empty() : Optional.ofNullable(interactiveMessageStates.remove(messageId));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearInteractiveMessageStates() {
        interactiveMessageStates.clear();
        return this;
    }

    @Override
    public Collection<NoteState> noteStates() {
        return List.copyOf(noteStates.values());
    }

    @Override
    public Optional<NoteState> findNoteState(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(noteStates.get(id));
    }

    @Override
    public LinkedWhatsAppBusinessStore putNoteState(NoteState state) {
        Objects.requireNonNull(state, "state cannot be null");
        noteStates.put(state.id(), state);
        return this;
    }

    @Override
    public Optional<NoteState> removeNoteState(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(noteStates.remove(id));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearNoteStates() {
        noteStates.clear();
        return this;
    }

    @Override
    public Collection<BotWelcomeRequestState> botWelcomeRequestStates() {
        return List.copyOf(botWelcomeRequestStates.values());
    }

    @Override
    public Optional<BotWelcomeRequestState> findBotWelcomeRequestState(Jid botJid) {
        return botJid == null ? Optional.empty() : Optional.ofNullable(botWelcomeRequestStates.get(botJid));
    }

    @Override
    public LinkedWhatsAppBusinessStore putBotWelcomeRequestState(BotWelcomeRequestState state) {
        Objects.requireNonNull(state, "state cannot be null");
        botWelcomeRequestStates.put(state.botJid(), state);
        return this;
    }

    @Override
    public Optional<BotWelcomeRequestState> removeBotWelcomeRequestState(Jid botJid) {
        return botJid == null ? Optional.empty() : Optional.ofNullable(botWelcomeRequestStates.remove(botJid));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearBotWelcomeRequestStates() {
        botWelcomeRequestStates.clear();
        return this;
    }

    @Override
    public Collection<AiThreadTitle> aiThreadTitles() {
        return List.copyOf(aiThreadTitles.values());
    }

    @Override
    public Optional<AiThreadTitle> findAiThreadTitle(String threadId) {
        return threadId == null ? Optional.empty() : Optional.ofNullable(aiThreadTitles.get(threadId));
    }

    @Override
    public LinkedWhatsAppBusinessStore putAiThreadTitle(AiThreadTitle title) {
        Objects.requireNonNull(title, "title cannot be null");
        aiThreadTitles.put(title.threadId(), title);
        return this;
    }

    @Override
    public Optional<AiThreadTitle> removeAiThreadTitle(String threadId) {
        return threadId == null ? Optional.empty() : Optional.ofNullable(aiThreadTitles.remove(threadId));
    }

    @Override
    public LinkedWhatsAppBusinessStore clearAiThreadTitles() {
        aiThreadTitles.clear();
        return this;
    }

    @Override
    public Optional<MusicUserIdAction> musicUserIdState() {
        return Optional.ofNullable(musicUserIdState);
    }

    @Override
    public LinkedWhatsAppBusinessStore setMusicUserIdState(MusicUserIdAction action) {
        this.musicUserIdState = action;
        return this;
    }

    @Override
    public Optional<byte[]> userCreatedBotDefinition() {
        return Optional.ofNullable(userCreatedBotDefinition);
    }

    @Override
    public LinkedWhatsAppBusinessStore setUserCreatedBotDefinition(byte[] definition) {
        this.userCreatedBotDefinition = definition;
        return this;
    }

    @Override
    public Optional<MaibaAIFeaturesControlAction.MaibaAIFeatureStatus> aiBusinessAgentStatus() {
        return Optional.ofNullable(aiBusinessAgentStatus);
    }

    @Override
    public LinkedWhatsAppBusinessStore setAiBusinessAgentStatus(MaibaAIFeaturesControlAction.MaibaAIFeatureStatus status) {
        this.aiBusinessAgentStatus = status;
        return this;
    }
}
