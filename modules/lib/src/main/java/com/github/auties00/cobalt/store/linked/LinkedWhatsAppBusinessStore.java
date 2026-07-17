package com.github.auties00.cobalt.store.linked;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
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
import com.github.auties00.cobalt.wire.linked.payment.OrphanPaymentNotification;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.MaibaAIFeaturesControlAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.MusicUserIdAction;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.CustomPaymentMethod;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.MerchantPaymentPartnerAction;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentTosAction;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;

/**
 * The WhatsApp Business and payments state of a WhatsApp client session.
 *
 * <p>This is the sub-store that owns the business-only and payments-only state: business feature
 * flags, marketing-campaign statuses, paid subscriptions, Click-To-WhatsApp data-sharing
 * preferences, the multi-agent routing state (agent states and chat assignments), the payment
 * configuration (custom methods, merchant partner, Terms-of-Service, instruction id), the marketing
 * messages and broadcast lists/campaigns/insights, orphan payment notifications, the bot/AI state
 * (welcome requests, thread titles, business-agent status, user-created bot definition) and the
 * interactive-message and note states.
 *
 * <p>None of this state is persisted; it is rebuilt at runtime from server pushes and queries.
 *
 * @apiNote
 * Embedders reach this through {@link LinkedWhatsAppStore#businessStore()}; most of it is only meaningful
 * on WhatsApp Business client flavours.
 *
 * @see LinkedWhatsAppStore
 */
@WhatsAppWebModule(moduleName = "WAWebCollections")
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface LinkedWhatsAppBusinessStore {
    /**
     * Returns the per-account business feature flags.
     *
     * @return an unmodifiable copy of the feature flags
     */
    Collection<BusinessFeatureFlag> businessFeatureFlags();

    /**
     * Looks up a business feature flag by name.
     *
     * @param name the flag name, or {@code null}
     * @return the flag, or empty if not set
     */
    Optional<BusinessFeatureFlag> findBusinessFeatureFlag(String name);

    /**
     * Stores a business feature flag.
     *
     * @param flag the flag, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putBusinessFeatureFlag(BusinessFeatureFlag flag);

    /**
     * Removes a business feature flag by name.
     *
     * @param name the flag name, or {@code null}
     * @return the removed flag, or empty if not set
     */
    Optional<BusinessFeatureFlag> removeBusinessFeatureFlag(String name);

    /**
     * Clears all business feature flags.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearBusinessFeatureFlags();

    /**
     * Returns the business marketing-campaign statuses.
     *
     * @return an unmodifiable copy of the campaign statuses
     */
    Collection<BusinessCampaignStatus> businessCampaignStatuses();

    /**
     * Looks up a business campaign status by campaign id.
     *
     * @param campaignId the campaign id, or {@code null}
     * @return the status, or empty if not present
     */
    Optional<BusinessCampaignStatus> findBusinessCampaignStatus(String campaignId);

    /**
     * Stores a business campaign status.
     *
     * @param status the status, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putBusinessCampaignStatus(BusinessCampaignStatus status);

    /**
     * Removes a business campaign status by campaign id.
     *
     * @param campaignId the campaign id, or {@code null}
     * @return the removed status, or empty if not present
     */
    Optional<BusinessCampaignStatus> removeBusinessCampaignStatus(String campaignId);

    /**
     * Clears all business campaign statuses.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearBusinessCampaignStatuses();

    /**
     * Returns the business paid subscriptions.
     *
     * @return an unmodifiable copy of the subscriptions
     */
    Collection<BusinessSubscription> businessSubscriptions();

    /**
     * Looks up a business subscription by id.
     *
     * @param id the subscription id, or {@code null}
     * @return the subscription, or empty if not present
     */
    Optional<BusinessSubscription> findBusinessSubscription(String id);

    /**
     * Stores a business subscription.
     *
     * @param subscription the subscription, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putBusinessSubscription(BusinessSubscription subscription);

    /**
     * Removes a business subscription by id.
     *
     * @param id the subscription id, or {@code null}
     * @return the removed subscription, or empty if not present
     */
    Optional<BusinessSubscription> removeBusinessSubscription(String id);

    /**
     * Clears all business subscriptions.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearBusinessSubscriptions();

    /**
     * Returns the opaque nonce scoping business-only round trips.
     *
     * @return the business account nonce, or empty if unset
     */
    Optional<String> businessAccountNonce();

    /**
     * Sets the business account nonce.
     *
     * @param nonce the nonce, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setBusinessAccountNonce(String nonce);

    /**
     * Awaits a snapshot-diff update to the business account nonce, returning the fresh value once it
     * differs from {@code previous} and is non-{@code null}, or {@link Optional#empty()} once the
     * wait window elapses.
     *
     * <p>The nonce is delivered asynchronously by the server through the
     * {@code <notification type="biz" wa_ad_account_nonce/>} push, which the business notification
     * handler routes through {@link #setBusinessAccountNonce(String)}. Callers that drive a flow
     * conditional on a fresh nonce (the silent click-to-WhatsApp credential refresh, for example)
     * snapshot the previous value, dispatch the request that triggers the push, and then call this
     * method to correlate the asynchronous arrival without polling.
     *
     * @apiNote Used by the silent Facebook GraphQL credential refresh to wait for the push that follows a
     * silent-nonce request; embedders typically have no reason to call it directly.
     *
     * @param previous the snapshot value captured before the triggering request was dispatched;
     *                 may be {@code null} when the slot was empty
     * @param timeout  the maximum time to wait for the push; never {@code null}
     * @return an {@link Optional} carrying the fresh nonce, or empty once the wait window elapses
     * @throws InterruptedException if the calling thread is interrupted while waiting
     * @throws NullPointerException if {@code timeout} is {@code null}
     */
    Optional<String> awaitBusinessAccountNonce(String previous, Duration timeout) throws InterruptedException;

    /**
     * Returns the Click-To-WhatsApp data-sharing preferences.
     *
     * @return an unmodifiable copy of the preferences
     */
    Collection<CtwaDataSharingPreference> ctwaDataSharingPreferences();

    /**
     * Looks up a CTWA data-sharing preference by ads-account LID.
     *
     * @param accountLid the ads-account LID, or {@code null}
     * @return the preference, or empty if not present
     */
    Optional<CtwaDataSharingPreference> findCtwaDataSharing(String accountLid);

    /**
     * Stores a CTWA data-sharing preference.
     *
     * @param preference the preference, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putCtwaDataSharing(CtwaDataSharingPreference preference);

    /**
     * Removes a CTWA data-sharing preference by ads-account LID.
     *
     * @param accountLid the ads-account LID, or {@code null}
     * @return the removed preference, or empty if not present
     */
    Optional<CtwaDataSharingPreference> removeCtwaDataSharing(String accountLid);

    /**
     * Clears all CTWA data-sharing preferences.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearCtwaDataSharing();

    /**
     * Returns the Small-Business data-sharing consent string.
     *
     * @return the consent string, or empty if unset
     */
    Optional<String> businessPrivacySetting();

    /**
     * Sets the Small-Business data-sharing consent string.
     *
     * @param consent the consent string, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setBusinessPrivacySetting(String consent);

    /**
     * Returns the global Click-To-WhatsApp data-sharing setting.
     *
     * <p>This is the account-wide tri-state fetched by the business-settings privacy query; it
     * gates whether the CTWA conversion-signal pipeline may emit. An empty result is treated as
     * {@link CtwaDataSharingSetting#NOT_SET} by callers.
     *
     * @return the CTWA data-sharing setting, or empty if it has not been fetched
     */
    Optional<CtwaDataSharingSetting> ctwaDataSharingSetting();

    /**
     * Sets the global Click-To-WhatsApp data-sharing setting.
     *
     * @param setting the CTWA data-sharing setting, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setCtwaDataSharingSetting(CtwaDataSharingSetting setting);

    /**
     * Returns the hash of the marketing-message opt-out list.
     *
     * @return the opt-out list hash, or empty if unset
     */
    Optional<String> businessOptOutListHash();

    /**
     * Sets the marketing-message opt-out list hash.
     *
     * @param hash the hash, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setBusinessOptOutListHash(String hash);

    /**
     * Returns whether the hosted-automation onboarding wizard is complete.
     *
     * @return {@code true} if hosted automation is onboarded
     */
    boolean hostedAutomationOnboarded();

    /**
     * Sets the hosted-automation onboarding flag.
     *
     * @param onboarded whether hosted automation is onboarded
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setHostedAutomationOnboarded(boolean onboarded);

    /**
     * Returns whether server-side outcome detection is enabled.
     *
     * @return {@code true} if detected outcomes are enabled
     */
    boolean detectedOutcomesEnabled();

    /**
     * Sets the detected-outcomes flag.
     *
     * @param enabled whether detected outcomes are enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setDetectedOutcomesEnabled(boolean enabled);

    /**
     * Returns whether Meta AI assistant features are enabled by the server.
     *
     * @return {@code true} if AI features are available
     */
    boolean aiAvailable();

    /**
     * Sets the AI-available flag.
     *
     * @param aiAvailable whether AI features are available
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setAiAvailable(boolean aiAvailable);

    /**
     * Looks up an orphan payment notification by triggering message id.
     *
     * @param messageId the message id, or {@code null}
     * @return the notification, or empty if not buffered
     */
    Optional<OrphanPaymentNotification> findOrphanPaymentNotification(String messageId);

    /**
     * Buffers an orphan payment notification whose triggering message has not arrived.
     *
     * @param notification the notification, never {@code null}
     */
    void addOrphanPaymentNotification(OrphanPaymentNotification notification);

    /**
     * Removes a buffered orphan payment notification by triggering message id.
     *
     * @param messageId the message id, or {@code null}
     * @return the removed notification, or empty if not buffered
     */
    Optional<OrphanPaymentNotification> removeOrphanPaymentNotification(String messageId);

    /**
     * Returns the business agent states.
     *
     * @return an unmodifiable copy of the agent states
     */
    Collection<AgentState> agentStates();

    /**
     * Looks up an agent state by agent id.
     *
     * @param agentId the agent id, or {@code null}
     * @return the agent state, or empty if not present
     */
    Optional<AgentState> findAgentState(String agentId);

    /**
     * Stores an agent state.
     *
     * @param state the agent state, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putAgentState(AgentState state);

    /**
     * Removes an agent state by agent id.
     *
     * @param agentId the agent id, or {@code null}
     * @return the removed agent state, or empty if not present
     */
    Optional<AgentState> removeAgentState(String agentId);

    /**
     * Clears all agent states.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearAgentStates();

    /**
     * Returns the chat-to-agent assignments.
     *
     * @return an unmodifiable copy of the chat assignments
     */
    Collection<ChatAssignment> chatAssignments();

    /**
     * Looks up a chat assignment by chat JID.
     *
     * @param chatJid the chat JID, or {@code null}
     * @return the assignment, or empty if not present
     */
    Optional<ChatAssignment> findChatAssignment(Jid chatJid);

    /**
     * Stores a chat assignment.
     *
     * @param assignment the assignment, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putChatAssignment(ChatAssignment assignment);

    /**
     * Removes a chat assignment by chat JID.
     *
     * @param chatJid the chat JID, or {@code null}
     * @return the removed assignment, or empty if not present
     */
    Optional<ChatAssignment> removeChatAssignment(Jid chatJid);

    /**
     * Clears all chat assignments.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearChatAssignments();

    /**
     * Returns the configured Customer-Payment-Instructions identifier.
     *
     * @return the CPI, or empty if unset
     */
    Optional<String> paymentInstructionCpi();

    /**
     * Sets the Customer-Payment-Instructions identifier.
     *
     * @param cpi the CPI, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setPaymentInstructionCpi(String cpi);

    /**
     * Returns the configured custom payment methods.
     *
     * @return an unmodifiable copy of the payment methods
     */
    List<CustomPaymentMethod> customPaymentMethods();

    /**
     * Sets the custom payment method list.
     *
     * @param methods the payment methods, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setCustomPaymentMethods(List<CustomPaymentMethod> methods);

    /**
     * Returns the selected merchant payment partner.
     *
     * @return the merchant payment partner, or empty if unset
     */
    Optional<MerchantPaymentPartnerAction> merchantPaymentPartner();

    /**
     * Sets the merchant payment partner.
     *
     * @param partner the partner, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setMerchantPaymentPartner(MerchantPaymentPartnerAction partner);

    /**
     * Returns the payment Terms-of-Service acknowledgement state.
     *
     * @return the payment ToS state, or empty if unset
     */
    Optional<PaymentTosAction> paymentTos();

    /**
     * Sets the payment Terms-of-Service acknowledgement state.
     *
     * @param tos the state, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setPaymentTos(PaymentTosAction tos);

    /**
     * Returns the saved business marketing messages.
     *
     * @return an unmodifiable copy of the marketing messages
     */
    Collection<MarketingMessage> marketingMessages();

    /**
     * Looks up a marketing message by template id.
     *
     * @param templateId the template id, or {@code null}
     * @return the marketing message, or empty if not present
     */
    Optional<MarketingMessage> findMarketingMessage(String templateId);

    /**
     * Stores a marketing message.
     *
     * @param message the marketing message, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putMarketingMessage(MarketingMessage message);

    /**
     * Removes a marketing message by template id.
     *
     * @param templateId the template id, or {@code null}
     * @return the removed marketing message, or empty if not present
     */
    Optional<MarketingMessage> removeMarketingMessage(String templateId);

    /**
     * Clears all marketing messages.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearMarketingMessages();

    /**
     * Returns the marketing-message broadcasts.
     *
     * @return an unmodifiable copy of the broadcasts
     */
    Collection<MarketingMessageBroadcast> marketingMessageBroadcasts();

    /**
     * Looks up a marketing-message broadcast by template id.
     *
     * @param templateId the template id, or {@code null}
     * @return the broadcast, or empty if not present
     */
    Optional<MarketingMessageBroadcast> findMarketingMessageBroadcast(String templateId);

    /**
     * Stores a marketing-message broadcast.
     *
     * @param broadcast the broadcast, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putMarketingMessageBroadcast(MarketingMessageBroadcast broadcast);

    /**
     * Removes a marketing-message broadcast by template id.
     *
     * @param templateId the template id, or {@code null}
     * @return the removed broadcast, or empty if not present
     */
    Optional<MarketingMessageBroadcast> removeMarketingMessageBroadcast(String templateId);

    /**
     * Clears all marketing-message broadcasts.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearMarketingMessageBroadcasts();

    /**
     * Returns the configured business broadcast lists.
     *
     * @return an unmodifiable copy of the broadcast lists
     */
    Collection<BusinessBroadcastList> businessBroadcastLists();

    /**
     * Looks up a business broadcast list by id.
     *
     * @param id the list id, or {@code null}
     * @return the list, or empty if not present
     */
    Optional<BusinessBroadcastList> findBusinessBroadcastList(String id);

    /**
     * Stores a business broadcast list.
     *
     * @param list the list, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putBusinessBroadcastList(BusinessBroadcastList list);

    /**
     * Removes a business broadcast list by id.
     *
     * @param id the list id, or {@code null}
     * @return the removed list, or empty if not present
     */
    Optional<BusinessBroadcastList> removeBusinessBroadcastList(String id);

    /**
     * Clears all business broadcast lists.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearBusinessBroadcastLists();

    /**
     * Returns the JIDs of the configured business broadcast lists.
     *
     * @return the broadcast-list JIDs
     */
    SequencedCollection<Jid> broadcasts();

    /**
     * Returns the business broadcast campaigns.
     *
     * @return an unmodifiable copy of the campaigns
     */
    Collection<BusinessBroadcastCampaign> businessBroadcastCampaigns();

    /**
     * Looks up a business broadcast campaign by id.
     *
     * @param id the campaign id, or {@code null}
     * @return the campaign, or empty if not present
     */
    Optional<BusinessBroadcastCampaign> findBusinessBroadcastCampaign(String id);

    /**
     * Stores a business broadcast campaign.
     *
     * @param campaign the campaign, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putBusinessBroadcastCampaign(BusinessBroadcastCampaign campaign);

    /**
     * Removes a business broadcast campaign by id.
     *
     * @param id the campaign id, or {@code null}
     * @return the removed campaign, or empty if not present
     */
    Optional<BusinessBroadcastCampaign> removeBusinessBroadcastCampaign(String id);

    /**
     * Clears all business broadcast campaigns.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearBusinessBroadcastCampaigns();

    /**
     * Returns the per-campaign broadcast insight records.
     *
     * @return an unmodifiable copy of the insights
     */
    Collection<BusinessBroadcastInsight> businessBroadcastInsights();

    /**
     * Looks up a business broadcast insight by id.
     *
     * @param id the insight id, or {@code null}
     * @return the insight, or empty if not present
     */
    Optional<BusinessBroadcastInsight> findBusinessBroadcastInsight(String id);

    /**
     * Stores a business broadcast insight.
     *
     * @param insight the insight, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putBusinessBroadcastInsight(BusinessBroadcastInsight insight);

    /**
     * Removes a business broadcast insight by id.
     *
     * @param id the insight id, or {@code null}
     * @return the removed insight, or empty if not present
     */
    Optional<BusinessBroadcastInsight> removeBusinessBroadcastInsight(String id);

    /**
     * Returns the interactive-message UI states.
     *
     * @return an unmodifiable copy of the interactive-message states
     */
    Collection<InteractiveMessageState> interactiveMessageStates();

    /**
     * Looks up an interactive-message state by message id.
     *
     * @param messageId the message id, or {@code null}
     * @return the state, or empty if not present
     */
    Optional<InteractiveMessageState> findInteractiveMessageState(String messageId);

    /**
     * Stores an interactive-message state.
     *
     * @param state the state, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putInteractiveMessageState(InteractiveMessageState state);

    /**
     * Removes an interactive-message state by message id.
     *
     * @param messageId the message id, or {@code null}
     * @return the removed state, or empty if not present
     */
    Optional<InteractiveMessageState> removeInteractiveMessageState(String messageId);

    /**
     * Clears all interactive-message states.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearInteractiveMessageStates();

    /**
     * Returns the note states.
     *
     * @return an unmodifiable copy of the note states
     */
    Collection<NoteState> noteStates();

    /**
     * Looks up a note state by id.
     *
     * @param id the note id, or {@code null}
     * @return the note state, or empty if not present
     */
    Optional<NoteState> findNoteState(String id);

    /**
     * Stores a note state.
     *
     * @param state the note state, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putNoteState(NoteState state);

    /**
     * Removes a note state by id.
     *
     * @param id the note id, or {@code null}
     * @return the removed note state, or empty if not present
     */
    Optional<NoteState> removeNoteState(String id);

    /**
     * Clears all note states.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearNoteStates();

    /**
     * Returns the bot welcome-request states.
     *
     * @return an unmodifiable copy of the welcome-request states
     */
    Collection<BotWelcomeRequestState> botWelcomeRequestStates();

    /**
     * Looks up a bot welcome-request state by bot JID.
     *
     * @param botJid the bot JID, or {@code null}
     * @return the state, or empty if not present
     */
    Optional<BotWelcomeRequestState> findBotWelcomeRequestState(Jid botJid);

    /**
     * Stores a bot welcome-request state.
     *
     * @param state the state, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putBotWelcomeRequestState(BotWelcomeRequestState state);

    /**
     * Removes a bot welcome-request state by bot JID.
     *
     * @param botJid the bot JID, or {@code null}
     * @return the removed state, or empty if not present
     */
    Optional<BotWelcomeRequestState> removeBotWelcomeRequestState(Jid botJid);

    /**
     * Clears all bot welcome-request states.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearBotWelcomeRequestStates();

    /**
     * Returns the AI-generated thread titles.
     *
     * @return an unmodifiable copy of the thread titles
     */
    Collection<AiThreadTitle> aiThreadTitles();

    /**
     * Looks up an AI thread title by thread id.
     *
     * @param threadId the thread id, or {@code null}
     * @return the title, or empty if not present
     */
    Optional<AiThreadTitle> findAiThreadTitle(String threadId);

    /**
     * Stores an AI thread title.
     *
     * @param title the title, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore putAiThreadTitle(AiThreadTitle title);

    /**
     * Removes an AI thread title by thread id.
     *
     * @param threadId the thread id, or {@code null}
     * @return the removed title, or empty if not present
     */
    Optional<AiThreadTitle> removeAiThreadTitle(String threadId);

    /**
     * Clears all AI thread titles.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore clearAiThreadTitles();

    /**
     * Returns the music-service integration user identifier state.
     *
     * @return the music user-id state, or empty if unset
     */
    Optional<MusicUserIdAction> musicUserIdState();

    /**
     * Sets the music-service integration user identifier state.
     *
     * @param action the state, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setMusicUserIdState(MusicUserIdAction action);

    /**
     * Returns the user-created bot definition blob.
     *
     * @return the bot definition, or empty if unset
     */
    Optional<byte[]> userCreatedBotDefinition();

    /**
     * Sets the user-created bot definition blob.
     *
     * @param definition the bot definition, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setUserCreatedBotDefinition(byte[] definition);

    /**
     * Returns the business AI-agent feature status.
     *
     * @return the AI-agent status, or empty if unset
     */
    Optional<MaibaAIFeaturesControlAction.MaibaAIFeatureStatus> aiBusinessAgentStatus();

    /**
     * Sets the business AI-agent feature status.
     *
     * @param status the status, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppBusinessStore setAiBusinessAgentStatus(MaibaAIFeaturesControlAction.MaibaAIFeatureStatus status);
}
