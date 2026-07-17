package com.github.auties00.cobalt.stream.notification.business;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.linked.LinkedBusinessPrivacySettingChangedListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.BusinessCampaignStatusBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessFeatureFlagBuilder;
import com.github.auties00.cobalt.wire.linked.business.BusinessDataSharingConsent;
import com.github.auties00.cobalt.wire.linked.business.BusinessSubscriptionBuilder;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessProfile;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.stanza.smax.biz.SmaxBannerSuggestionAction;
import com.github.auties00.cobalt.wire.stanza.smax.biz.SmaxBannerSuggestionBanner;
import com.github.auties00.cobalt.wire.stanza.smax.biz.SmaxBannerSuggestionFalseTrueFlag;
import com.github.auties00.cobalt.wire.stanza.smax.biz.SmaxBannerSuggestionResponse;
import com.github.auties00.cobalt.wire.stanza.smax.biz.SmaxCampaignStateChangedNotificationResponse;
import com.github.auties00.cobalt.wire.stanza.smax.biz.SmaxNonceNotificationResponse;
import com.github.auties00.cobalt.wire.stanza.smax.biz.SmaxSyncPrivacySettingResponse;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.CtwaActionBannerUnderstandEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.PreferredLinkType;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Locale;

/**
 * Handles {@code type="business"}, {@code type="digital_commerce_subscription"}, and {@code type="fb:update"}
 * notifications carrying WhatsApp Business mutations.
 *
 * <p>Dispatched by {@link NotificationBusinessDispatcher}. The {@code business} type covers verified-name changes,
 * business deregistration, business-profile updates, product-catalog updates, subscription and feature-flag pushes,
 * click-to-WhatsApp banner suggestions, small-business privacy-settings sync, ad-account nonce delivery, and
 * marketing-campaign state changes. The {@code digital_commerce_subscription} type reuses the subscription parser, and
 * {@code fb:update} delivers bot-profile updates. Every processed stanza is followed by a protocol-level
 * {@code <ack class="notification">}; the ack may carry an optional {@code <user side_list="out"/>} child when a
 * hash-based contact lookup failed, asking the server to redistribute the notification to companion devices that may
 * hold the contact record this device could not resolve.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleBusinessNotification")
public final class NotificationBusinessStreamHandler extends SocketStreamHandler.Concurrent {

    /**
     * The logger for {@link NotificationBusinessStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(NotificationBusinessStreamHandler.class);

    /**
     * Reads the store and issues business-profile queries.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Ships the post-processing {@code <ack class="notification">} stanzas for {@code business},
     * {@code digital_commerce_subscription}, and {@code fb:update} notifications.
     */
    private final AckSender ackSender;

    /**
     * Sink for the {@code CtwaActionBannerUnderstand} telemetry committed when a click-to-WhatsApp
     * {@code <ctwa_suggestion>} banner notification is parsed.
     */
    private final WamService wamService;

    /**
     * Constructs the handler with the shared client, ack sender, and telemetry sink.
     *
     * <p>Called once by {@link NotificationBusinessDispatcher}.
     *
     * @param whatsapp   the client used for store reads and business-profile queries
     * @param ackSender  the ack sender used for the post-processing acks
     * @param wamService the telemetry sink used to commit the click-to-WhatsApp banner-understand event
     */
    public NotificationBusinessStreamHandler(LinkedWhatsAppClient whatsapp, AckSender ackSender, WamService wamService) {
        this.whatsapp = whatsapp;
        this.ackSender = ackSender;
        this.wamService = wamService;
    }

    /**
     * Validates the stanza shape, dispatches to the per-type sub-handler, and always sends the protocol-level ack.
     *
     * <p>Stanzas whose description is not {@code notification} or that carry no {@code type} are dropped. The
     * {@code type} attribute routes between {@code business} (handled by {@link #handleBusinessNotification(Stanza)}),
     * {@code digital_commerce_subscription} (handled by {@link #handleDigitalCommerceSubscription(Stanza)}), and
     * {@code fb:update} (handled by {@link #handleBotProfileUpdate(Stanza)}).
     *
     * @param stanza the incoming {@code <notification>} stanza
     */
    @Override
    public void handle(Stanza stanza) {
        if (!stanza.hasDescription("notification")) {
            return;
        }

        var type = stanza.getAttributeAsString("type", null);
        if (type == null) {
            return;
        }

        switch (type) {
            case "business" -> handleBusinessNotification(stanza);
            case "digital_commerce_subscription" -> handleDigitalCommerceSubscription(stanza);
            case "fb:update" -> handleBotProfileUpdate(stanza);
            default -> {
            }
        }
    }

    /**
     * Routes a {@code business}-type stanza through {@link #dispatch(Stanza)} and sends the ack with the side-list child
     * the dispatch requested.
     *
     * <p>The side-list flag asks the server to redistribute the notification to companions that may hold the contact
     * record this device could not resolve from a hash. A failure during dispatch is logged and the ack is still sent.
     *
     * @param stanza the {@code <notification type="business"/>} stanza
     */
    private void handleBusinessNotification(Stanza stanza) {
        var needsSideList = false;
        try {
            needsSideList = dispatch(stanza);
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "failed to handle business notification id=" + stanza.getAttributeAsString("id", "[missing-id]"),
                        throwable);
            }
        } finally {
            sendBusinessAck(stanza, needsSideList);
        }
    }

    /**
     * Routes a {@code digital_commerce_subscription}-type stanza through the shared {@link #handleSubscriptions(Stanza)}
     * path and sends the dedicated digital-commerce ack.
     *
     * <p>The notification type is distinct from {@code business} so the server can route it based on subscriber
     * eligibility, but the wire shape and the applied subscription and feature-flag fields are identical. A failure
     * during processing is logged and the ack is still sent.
     *
     * @param stanza the {@code <notification type="digital_commerce_subscription"/>} stanza
     */
    private void handleDigitalCommerceSubscription(Stanza stanza) {
        try {
            handleSubscriptions(stanza);
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "failed to handle digital_commerce_subscription notification id=" + stanza.getAttributeAsString("id", "[missing-id]"),
                        throwable);
            }
        } finally {
            sendDigitalCommerceSubscriptionAck(stanza);
        }
    }

    /**
     * Processes a {@code fb:update} stanza by iterating {@code <update type="bot_profile"/>} children and noting each
     * referenced bot profile.
     *
     * <p>These updates drive the in-chat bot affordances for accounts that have registered Meta AI bots. A pruned
     * update (an {@code <update>} with no {@code jid}) signals that the server has invalidated the cache and a full bot
     * resync is needed. A failure during processing is logged and the ack is still sent.
     *
     * @implNote
     * This implementation only logs the bot JID and category. Cobalt does not maintain an in-memory bot-profile
     * collection and lets callers re-query on demand, so no refresh is fired per bot id.
     *
     * @param stanza the {@code <notification type="fb:update"/>} stanza
     */
    private void handleBotProfileUpdate(Stanza stanza) {
        try {
            var pruned = false;
            for (var child : stanza.children()) {
                if (!"update".equals(child.description())) {
                    continue;
                }

                var childType = child.getAttributeAsString("type", null);
                if (!"bot_profile".equals(childType)) {
                    continue;
                }

                var botJid = child.getAttributeAsString("jid", null);
                if (botJid == null || botJid.isEmpty()) {
                    pruned = true;
                    break;
                }

                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "bot profile update for {0}, category={1}",
                            new LogRedactable.User(botJid),
                            child.getAttributeAsString("category", null));
                }
            }

            if (pruned && Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "pruned bot profile update, full bot sync needed");
            }
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "failed to handle fb:update notification id=" + stanza.getAttributeAsString("id", "[missing-id]"),
                        throwable);
            }
        } finally {
            sendBotProfileAck(stanza);
        }
    }

    /**
     * Selects the {@code business}-type branch from the first recognised child element and returns whether the ack
     * should request a side-list redistribution.
     *
     * <p>The recognised children, in priority order, are {@code remove}, {@code verified_name}, {@code profile},
     * {@code product_catalog}, {@code subscriptions}, {@code ctwa_suggestion}, {@code privacy},
     * {@code wa_ad_account_nonce}, and {@code mm_campaign}; an unknown child falls through to a debug log. The
     * remove, verified-name, and profile branches return {@code true} when their targeting is hash-based and the
     * contact could not be resolved; all other branches return {@code false}.
     *
     * @param stanza the full {@code <notification>} stanza
     * @return {@code true} when a hash-based lookup failed and the ack should include {@code <user side_list="out"/>}; {@code false} otherwise
     */
    private boolean dispatch(Stanza stanza) {
        if (stanza.hasChild("remove")) {
            return handleRemove(stanza.getRequiredChild("remove"));
        }

        if (stanza.hasChild("verified_name")) {
            return handleVerifiedName(stanza.getRequiredChild("verified_name"));
        }

        if (stanza.hasChild("profile")) {
            return handleProfile(stanza, stanza.getRequiredChild("profile"));
        }

        if (stanza.hasChild("product_catalog")) {
            handleProductCatalog(stanza.getRequiredChild("product_catalog"));
            return false;
        }

        if (stanza.hasChild("subscriptions")) {
            handleSubscriptions(stanza);
            return false;
        }

        if (stanza.hasChild("ctwa_suggestion")) {
            handleCtwaSuggestion(stanza);
            return false;
        }

        if (stanza.hasChild("privacy")) {
            handlePrivacy(stanza);
            return false;
        }

        if (stanza.hasChild("wa_ad_account_nonce")) {
            handleAdAccountNonce(stanza);
            return false;
        }

        if (stanza.hasChild("mm_campaign")) {
            handleMarketingCampaign(stanza);
            return false;
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "received unknown business notification subtype");
        }
        return false;
    }

    /**
     * Clears the local business fields when the {@code <remove>} child targets self by JID, and returns whether a
     * hash-only remove needs the side-list ack.
     *
     * <p>When the remove targets the local account by JID, this clears the verified name, address, description, email,
     * websites, categories, and the {@code syncedBusinessCertificate} flag. The hash branch always returns {@code true}
     * because Cobalt has no hash-keyed contact lookup and instead asks a companion that holds the contact to apply the
     * remove.
     *
     * @implNote
     * This implementation does not emit an in-thread privacy system message on removal; Cobalt's message-generation
     * pipeline runs from a different stream.
     *
     * @param removeStanza the {@code <remove>} child stanza
     * @return {@code true} when hash-based lookup failed; {@code false} otherwise
     */
    private boolean handleRemove(Stanza removeStanza) {
        var jid = removeStanza.getAttributeAsJid("jid").orElse(null);
        if (jid != null) {
            var targetJid = jid.withoutData();
            if (isSelf(targetJid)) {
                whatsapp.store().accountStore().setVerifiedName(null)
                        .setBusinessAddress(null)
                        .setBusinessDescription(null)
                        .setBusinessEmail(null)
                        .setBusinessWebsites(null)
                        .setBusinessCategories(null);
                whatsapp.store().syncStore().setSyncedBusinessCertificate(false);
            }
            return false;
        }

        // TODO: resolve the contact via hash and apply the remove locally. Today Cobalt requests side-list redistribution so a companion that owns the contact applies it instead.
        var hash = removeStanza.getAttributeAsString("hash", null);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "cannot handle hash-based business removal (hash={0}), requesting side-list redistribution",
                    new LogRedactable.Secret(hash));
        }
        return true;
    }

    /**
     * Re-queries the verified name (self branch) or the business profile (peer branch) when the
     * {@code <verified_name>} child targets a JID, and returns the side-list flag when the targeting is hash-based.
     *
     * <p>For self, the verified name is resolved through
     * {@link LinkedWhatsAppClient#queryName(com.github.auties00.cobalt.wire.core.jid.JidProvider)} and the
     * {@code syncedBusinessCertificate} flag is set; for a peer, the change is reflected through
     * {@link LinkedWhatsAppClient#queryBusinessProfile(com.github.auties00.cobalt.wire.core.jid.JidProvider)}. The hash branch
     * always returns {@code true} because Cobalt has no hash-keyed contact lookup.
     *
     * @param verifiedNameStanza the {@code <verified_name>} child stanza
     * @return {@code true} when hash-based and the contact was not resolved; {@code false} otherwise
     */
    private boolean handleVerifiedName(Stanza verifiedNameStanza) {
        var jid = verifiedNameStanza.getAttributeAsJid("jid").orElse(null);
        if (jid != null) {
            var targetJid = jid.withoutData();
            if (isSelf(targetJid)) {
                whatsapp.queryName(targetJid)
                        .ifPresent(whatsapp.store().accountStore()::setVerifiedName);
                whatsapp.store().syncStore().setSyncedBusinessCertificate(true);
            } else {
                whatsapp.queryBusinessProfile(targetJid);
            }
            return false;
        }

        // TODO: resolve the contact via hash. Today Cobalt requests side-list redistribution so a companion that owns the contact applies the verified-name change instead.
        var hash = verifiedNameStanza.getAttributeAsString("hash", null);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "cannot handle hash-based verified name change (hash={0}), requesting side-list redistribution",
                    new LogRedactable.Secret(hash));
        }
        return true;
    }

    /**
     * Re-queries the business profile when the {@code <profile>} child has no hash attribute, and returns the side-list
     * flag otherwise.
     *
     * <p>The target JID is read from the notification's {@code from} attribute. The queried profile is applied to the
     * local store and the {@code syncedBusinessCertificate} flag is set only when the target is self; for a non-self
     * target the queried profile is discarded after the server-side state is refreshed. The hash branch always returns
     * {@code true} because Cobalt cannot resolve a contact from a hash.
     *
     * @param stanza        the full {@code <notification>} stanza, used to read {@code from}
     * @param profileStanza the {@code <profile>} child stanza
     * @return {@code true} when hash-based and the contact cannot be resolved; {@code false} otherwise
     */
    private boolean handleProfile(Stanza stanza, Stanza profileStanza) {
        var hash = profileStanza.getAttributeAsString("hash", null);
        if (hash == null || hash.isEmpty()) {
            var targetJid = stanza.getAttributeAsJid("from")
                    .map(Jid::withoutData)
                    .orElse(null);
            if (targetJid != null) {
                var refreshed = whatsapp.queryBusinessProfile(targetJid).orElse(null);
                if (refreshed != null && isSelf(targetJid)) {
                    applyOwnBusinessProfile(refreshed);
                    whatsapp.store().syncStore().setSyncedBusinessCertificate(true);
                }
            }
            return false;
        }

        // TODO: resolve the contact via hash. Today Cobalt requests side-list redistribution for the hash branch.
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "cannot handle hash-based business profile update (hash={0}), requesting side-list redistribution",
                    new LogRedactable.Secret(hash));
        }
        return true;
    }

    /**
     * Logs the product or collection counts contained in a {@code <product_catalog>} child.
     *
     * @implNote
     * This implementation does not refresh the catalog from the server. Cobalt has no in-memory catalog cache, so the
     * change is observable only on the next explicit
     * {@link LinkedWhatsAppClient#queryBusinessCatalog(com.github.auties00.cobalt.wire.core.jid.JidProvider)} call.
     *
     * @param catalogStanza the {@code <product_catalog>} child stanza
     */
    private void handleProductCatalog(Stanza catalogStanza) {
        if (catalogStanza.hasChild("product")) {
            var productIds = catalogStanza.getChildren("product").stream()
                    .flatMap(product -> product.getChild("id").stream())
                    .flatMap(idNode -> idNode.toContentString().stream())
                    .toList();
            if (!productIds.isEmpty() && Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "received product catalog notification for {0} products", productIds.size());
            }
        } else if (catalogStanza.hasChild("collection")) {
            var collectionCount = catalogStanza.getChildren("collection").size();
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "received collection catalog notification for {0} collections", collectionCount);
            }
        }
    }

    /**
     * Applies the queried business profile fields to the local store for the authenticated business account.
     *
     * @param profile the refreshed business profile
     */
    private void applyOwnBusinessProfile(BusinessProfile profile) {
        whatsapp.store().accountStore().setBusinessDescription(profile.description().orElse(null))
                .setBusinessAddress(profile.address().orElse(null))
                .setBusinessEmail(profile.email().orElse(null))
                .setBusinessWebsites(profile.websites())
                .setBusinessCategories(profile.categories());
    }

    /**
     * Returns whether the given JID identifies the authenticated account.
     *
     * <p>Used by {@link #handleRemove(Stanza)}, {@link #handleVerifiedName(Stanza)}, and {@link #handleProfile(Stanza, Stanza)}
     * to distinguish self-business mutations from peer-business mutations.
     *
     * @param jid the JID to check
     * @return {@code true} when {@code jid} matches the local account; {@code false} otherwise
     */
    private boolean isSelf(Jid jid) {
        return whatsapp.store().accountStore().jid()
                .map(self -> self.isSameAccount(jid))
                .orElse(false);
    }

    /**
     * Reads {@code feature_flags} and {@code subscriptions} children from a {@code business} or
     * {@code digital_commerce_subscription} notification and merges each entry into the store.
     *
     * <p>The children live directly under the notification, not under the {@code subscriptions} child. Each feature
     * flag with a {@code name} and {@code enabled} attribute is written. Each subscription with an {@code id} is
     * applied: when a stanza is partial (subscription stanzas often carry only the changed attribute) the existing
     * status, expiration, and creation time are preserved and only the supplied fields are overwritten. A subscription
     * id with no status, expiration, or creation time is still applied so the record is created.
     *
     * @param stanza the full {@code <notification>} stanza
     */
    private void handleSubscriptions(Stanza stanza) {
        stanza.getChild("feature_flags").ifPresent(featureFlagsNode -> {
            featureFlagsNode.getChildren("feature_flag").forEach(featureFlag -> {
                var name = featureFlag.getAttributeAsString("name", null);
                var enabled = featureFlag.getAttributeAsString("enabled", null);
                if (name != null && enabled != null) {
                    whatsapp.store().businessStore().putBusinessFeatureFlag(new BusinessFeatureFlagBuilder()
                            .name(name)
                            .enabled("true".equalsIgnoreCase(enabled))
                            .build());
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "applied business feature flag {0}={1}", name, enabled);
                    }
                }
            });
        });

        stanza.getChild("subscriptions").ifPresent(subscriptionsNode -> {
            subscriptionsNode.getChildren("subscription").forEach(subscription -> {
                var id = subscription.getAttributeAsString("id", null);
                if (id == null) {
                    return;
                }
                var existing = whatsapp.store().businessStore().findBusinessSubscription(id).orElse(null);
                var builder = new BusinessSubscriptionBuilder().id(id);
                if (existing != null) {
                    existing.status().ifPresent(builder::status);
                    existing.expiration().ifPresent(builder::expiration);
                    existing.createdAt().ifPresent(builder::createdAt);
                }
                var status = subscription.getAttributeAsString("status", null);
                if (status != null) {
                    builder.status(status);
                }
                var expiration = subscription.getAttributeAsLong("subscription_end_time", (Long) null);
                if (expiration != null) {
                    builder.expiration(Instant.ofEpochSecond(expiration));
                }
                var creationTime = subscription.getAttributeAsLong("subscription_creation_time", (Long) null);
                if (creationTime != null) {
                    builder.createdAt(Instant.ofEpochSecond(creationTime));
                }
                whatsapp.store().businessStore().putBusinessSubscription(builder.build());
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "applied business subscription id={0} status={1}", id, status);
                }
            });
        });
    }

    /**
     * Writes the small-business data-sharing-with-Meta consent value parsed from the {@code <privacy>} child to the
     * store.
     *
     * <p>The wire literal ({@code "false"}, {@code "notset"}, or {@code "true"}) is persisted directly via
     * {@link LinkedWhatsAppBusinessStore#setBusinessPrivacySetting(String)}.
     *
     * @implNote
     * This implementation routes through the typed {@link SmaxSyncPrivacySettingResponse} parser so that the SMAX
     * export remains the single source of truth for envelope validation.
     *
     * @param stanza the {@code <notification>} stanza
     */
    private void handlePrivacy(Stanza stanza) {
        var consent = SmaxSyncPrivacySettingResponse.of(stanza)
                .flatMap(SmaxSyncPrivacySettingResponse.Notification::dataSharingConsent)
                .orElse(null);
        if (consent == null) {
            return;
        }
        whatsapp.store().businessStore().setBusinessPrivacySetting(consent);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "applied business data-sharing consent {0}", consent);
        }
        BusinessDataSharingConsent.ofWire(consent).ifPresent(value -> {
            for (var listener : whatsapp.store().listeners()) {
                if (listener instanceof LinkedBusinessPrivacySettingChangedListener typed) {
                    Thread.startVirtualThread(() -> typed.onBusinessPrivacySettingChanged(whatsapp, value));
                }
            }
        });
    }

    /**
     * Stores the ad-account nonce parsed from a {@code <wa_ad_account_nonce>} child.
     *
     * <p>The nonce is a short-lived token consumed by the next ad-creation authentication call. It is written to the
     * store via {@link LinkedWhatsAppBusinessStore#setBusinessAccountNonce(String)}.
     *
     * @implNote
     * This implementation routes through the typed {@link SmaxNonceNotificationResponse} parser before the store write.
     *
     * @param stanza the {@code <notification>} stanza
     */
    private void handleAdAccountNonce(Stanza stanza) {
        SmaxNonceNotificationResponse.of(stanza)
                .map(SmaxNonceNotificationResponse.Notification::nonce)
                .ifPresent(nonce -> {
                    whatsapp.store().businessStore().setBusinessAccountNonce(nonce);
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "stored ad-account nonce {0}", new LogRedactable.Token(nonce));
                    }
                });
    }

    /**
     * Writes the marketing-campaign status carried by a {@code <mm_campaign>} child to the store.
     *
     * <p>The campaign status is keyed by the ad-creative id. The notification is dropped when any of the
     * {@code adCreativeId}, {@code adGroupId}, or {@code adId} fields is missing.
     *
     * @implNote
     * This implementation routes through the typed {@link SmaxCampaignStateChangedNotificationResponse} parser so the
     * SMAX export remains the single source of truth for the wire attribute names ({@code ad_id}, {@code ad_group_id},
     * {@code ad_creative_id}) and the envelope validation.
     *
     * @param stanza the {@code <notification>} stanza
     */
    private void handleMarketingCampaign(Stanza stanza) {
        var notification = SmaxCampaignStateChangedNotificationResponse.of(stanza).orElse(null);
        if (notification == null) {
            return;
        }

        var adCreativeId = notification.adCreativeId().orElse(null);
        var adGroupId = notification.adGroupId().orElse(null);
        var adId = notification.adId().orElse(null);
        if (adCreativeId == null || adGroupId == null || adId == null) {
            return;
        }

        whatsapp.store().businessStore().putBusinessCampaignStatus(new BusinessCampaignStatusBuilder()
                .campaignId(adCreativeId)
                .status(notification.status())
                .build());
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "applied marketing campaign status {0} for ad group {1}",
                    notification.status(), adGroupId);
        }
    }

    /**
     * Parses the click-to-WhatsApp {@code <ctwa_suggestion>} banner notification and commits the resulting
     * {@code CtwaActionBannerUnderstand} telemetry.
     *
     * <p>The suggestion is parsed through {@link SmaxBannerSuggestionResponse#of(Stanza)}. A banner whose
     * {@code <config revoked="true"/>} marks it as pulled server-side is dismissed by id and carries no telemetry, in line
     * with WA Web. Every other suggestion, including one with missing or malformed banner data, produces exactly one
     * committed event through {@link #emitCtwaActionBannerUnderstand(Stanza, String, SmaxBannerSuggestionBanner)}.
     *
     * @implNote
     * This implementation resolves the banner identifier from the parsed {@code target_entity_id} when the notification
     * parses, and otherwise reads it directly from the {@code <ctwa_suggestion>} child so an unparseable banner still
     * reports the entity it targeted.
     *
     * @param stanza the {@code <notification type="business"/>} stanza carrying the {@code <ctwa_suggestion>} child
     */
    @WhatsAppWebExport(moduleName = "WAWebCTWAParseSuggestion", exports = "parseCTWASuggestion", adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleCtwaSuggestion(Stanza stanza) {
        var notification = SmaxBannerSuggestionResponse.of(stanza).orElse(null);
        var banner = notification == null ? null : notification.banner().orElse(null);
        if (banner != null && banner.config().revoked() == SmaxBannerSuggestionFalseTrueFlag.TRUE) {
            return;
        }

        var bannerIdentifier = notification != null
                ? notification.targetEntityId()
                : stanza.getChild("ctwa_suggestion")
                        .flatMap(child -> child.getAttributeAsString("target_entity_id"))
                        .orElse(null);
        emitCtwaActionBannerUnderstand(stanza, bannerIdentifier, banner);
    }

    /**
     * Builds and commits a single {@code CtwaActionBannerUnderstand} event describing the parsed banner suggestion.
     *
     * <p>Mirrors WA Web's metric reporter: {@code bannerIdentifier} and {@code validNotification} are always populated,
     * while {@code bannerLocale} and {@code validLocale} are populated only when banner content is present. A notification
     * is considered valid when the banner ships exactly one {@code platform="web"} native action whose deep link is a
     * usable HTTPS URL or a recognised API command; a lone action with an unusable link additionally reports that link
     * through {@code invalidLink}. The {@code hasLocalLink}, {@code hasUniversalLink}, and {@code preferredLink} fields are
     * derived from the web native action and the cross-platform {@code <action>} element; {@code clientLocale} is the
     * account locale (host default when unset) and {@code notificationLogId} echoes the notification's server id.
     *
     * @implNote
     * This implementation commits the event unconditionally when a {@code <ctwa_suggestion>} arrives rather than behind
     * WA Web's {@code WAWebBizSuggestionsGatingUtils.adsActionBannersLoggingEnabled} logging gate, because the AB-props
     * service is not injected into this handler.
     *
     * @param stanza           the {@code <notification>} stanza, used for the notification log id
     * @param bannerIdentifier the CTWA target-entity id, or {@code null} when the suggestion could not be parsed
     * @param banner           the parsed banner payload, or {@code null} when banner data was missing or malformed
     */
    @WhatsAppWebExport(moduleName = "WAWebCTWAParseSuggestion", exports = "maybeReportMetric", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitCtwaActionBannerUnderstand(Stanza stanza, String bannerIdentifier, SmaxBannerSuggestionBanner banner) {
        // TODO: gate this emission behind WAWebBizSuggestionsGatingUtils.adsActionBannersLoggingEnabled once the AB-props service is injected into this handler.
        var clientLocale = whatsapp.store().accountStore().locale()
                .orElseGet(() -> Locale.getDefault().toLanguageTag());
        var builder = new CtwaActionBannerUnderstandEventBuilder()
                .clientLocale(clientLocale)
                .notificationLogId(stanza.getAttributeAsString("id", null));
        if (bannerIdentifier != null) {
            builder.bannerIdentifier(bannerIdentifier);
        }

        if (banner == null) {
            wamService.commit(builder.validNotification(false)
                    .hasLocalLink(false)
                    .hasUniversalLink(false)
                    .preferredLink(PreferredLinkType.UNIVERSAL)
                    .build());
            return;
        }

        var bannerLocale = banner.content().locale();
        var webActions = banner.nativeActions().stream()
                .filter(action -> "web".equals(action.platform()))
                .toList();
        var webAction = webActions.size() == 1 ? webActions.getFirst() : null;
        var actionLink = webAction == null ? null : webAction.localLink();
        var validLink = isValidActionLink(actionLink);
        var hasLocalLink = webAction != null
                || banner.action().flatMap(SmaxBannerSuggestionAction::localLink).isPresent();
        var hasUniversalLink = (webAction != null && webAction.universalLink().isPresent())
                || banner.action().flatMap(SmaxBannerSuggestionAction::deepLink).isPresent();

        builder.bannerLocale(bannerLocale)
                .validLocale(localesMatch(bannerLocale, clientLocale))
                .validNotification(webAction != null && validLink)
                .hasLocalLink(hasLocalLink)
                .hasUniversalLink(hasUniversalLink)
                .preferredLink(hasLocalLink ? PreferredLinkType.LOCAL : PreferredLinkType.UNIVERSAL);
        if (webAction != null && !validLink) {
            builder.invalidLink(actionLink);
        }
        wamService.commit(builder.build());
    }

    /**
     * Returns whether a banner action deep link is a usable navigation target.
     *
     * <p>A link is usable when it is a non-blank HTTPS URL or an all-uppercase API command token such as
     * {@code MANAGE_ADS} or {@code RECREATE_AD}; every other value is treated as an invalid link.
     *
     * @implNote
     * This implementation approximates WA Web's {@code WAWebApiParse.parseAPICmd} acceptance without porting the full
     * command grammar: it accepts the {@code https://} scheme and the recognised command-token shape and rejects the rest.
     *
     * @param link the deep-link value, or {@code null}
     * @return {@code true} when the link is a usable HTTPS URL or an API command token; {@code false} otherwise
     */
    private static boolean isValidActionLink(String link) {
        if (link == null || link.isBlank()) {
            return false;
        }
        if (link.startsWith("https://")) {
            return true;
        }
        return link.chars().allMatch(character -> Character.isUpperCase(character) || character == '_');
    }

    /**
     * Returns whether two locale identifiers share the same primary language subtag.
     *
     * <p>Comparison is case-insensitive on the substring preceding the first {@code '_'} or {@code '-'} separator, so
     * {@code "en_US"} matches {@code "en-GB"} but not {@code "fr_FR"}.
     *
     * @param bannerLocale the banner content locale
     * @param clientLocale the account or host locale
     * @return {@code true} when both carry the same language subtag; {@code false} otherwise
     */
    private static boolean localesMatch(String bannerLocale, String clientLocale) {
        return languageSubtag(bannerLocale).equalsIgnoreCase(languageSubtag(clientLocale));
    }

    /**
     * Returns the primary language subtag of a locale identifier.
     *
     * <p>Splits on the first {@code '_'} or {@code '-'} separator and returns the leading component; a value with no
     * separator is returned unchanged and a {@code null} value yields the empty string.
     *
     * @param locale the locale identifier, or {@code null}
     * @return the language subtag, or the empty string when {@code locale} is {@code null}
     */
    private static String languageSubtag(String locale) {
        if (locale == null) {
            return "";
        }
        var separator = locale.indexOf('_');
        if (separator < 0) {
            separator = locale.indexOf('-');
        }
        return separator < 0 ? locale : locale.substring(0, separator);
    }

    /**
     * Sends the {@code <ack class="notification" type="business"/>} stanza, with an optional
     * {@code <user side_list="out"/>} child when {@code needsSideList} is {@code true}.
     *
     * <p>Fire-and-forget. The side-list child asks the server to redistribute the notification to companions that may
     * own the contact record this device could not resolve.
     *
     * @param stanza          the original {@code <notification>} stanza
     * @param needsSideList whether the ack should include the side-list child
     */
    private void sendBusinessAck(Stanza stanza, boolean needsSideList) {
        var builder = ackSender.ack(AckClass.NOTIFICATION, stanza).type("business");
        if (needsSideList) {
            builder.child(new StanzaBuilder()
                    .description("user")
                    .attribute("side_list", "out")
                    .build());
        }
        builder.send();
    }

    /**
     * Sends the {@code <ack class="notification" type="digital_commerce_subscription"/>} stanza.
     *
     * <p>Fire-and-forget.
     *
     * @param stanza the original {@code <notification>} stanza
     */
    private void sendDigitalCommerceSubscriptionAck(Stanza stanza) {
        ackSender.ack(AckClass.NOTIFICATION, stanza).type("digital_commerce_subscription").send();
    }

    /**
     * Sends the {@code <ack class="notification" type="fb:update"/>} stanza for a bot-profile update notification.
     *
     * <p>Fire-and-forget. The {@code type} attribute reflects the original notification's type read back from the
     * stanza, defaulting to {@code fb:update} when absent.
     *
     * @param stanza the original {@code <notification>} stanza
     */
    private void sendBotProfileAck(Stanza stanza) {
        var type = stanza.getAttributeAsString("type", "fb:update");
        ackSender.ack(AckClass.NOTIFICATION, stanza).type(type).send();
    }
}
