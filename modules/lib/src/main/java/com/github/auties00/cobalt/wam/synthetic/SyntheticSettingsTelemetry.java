package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.ChatListWallpaperEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChatThemeScreenEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChatThreadWallpaperEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ChatWallpaperEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ContactNotificationSettingUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.NotificationDeliveryEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.NotificationEngagementEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.NotificationSettingEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SettingsChangeEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SettingsClickEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SettingsSearchInitiateEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SettingsSearchTapEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ChatThemeEntryType;
import com.github.auties00.cobalt.wire.wam.type.ChatWallpaperEntryType;
import com.github.auties00.cobalt.wire.wam.type.ChatWallpaperType;
import com.github.auties00.cobalt.wire.wam.type.ClientGroupSizeBucket;
import com.github.auties00.cobalt.wire.wam.type.ContactNotificationSettingActionType;
import com.github.auties00.cobalt.wire.wam.type.DeviceAppearanceType;
import com.github.auties00.cobalt.wire.wam.type.GroupTypeClient;
import com.github.auties00.cobalt.wire.wam.type.InAppNotificationAlertStyle;
import com.github.auties00.cobalt.wire.wam.type.MessageChatType;
import com.github.auties00.cobalt.wire.wam.type.NotificationActionType;
import com.github.auties00.cobalt.wire.wam.type.NotificationDestinationType;
import com.github.auties00.cobalt.wire.wam.type.NotificationSoundTone;
import com.github.auties00.cobalt.wire.wam.type.NotificationSourceType;
import com.github.auties00.cobalt.wire.wam.type.NotificationTypeEnum;
import com.github.auties00.cobalt.wire.wam.type.SettingType;
import com.github.auties00.cobalt.wire.wam.type.SettingsClickEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.SettingsItemType;
import com.github.auties00.cobalt.wire.wam.type.SettingsPageType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;
import com.github.auties00.cobalt.wire.wam.type.UnreadBadgeSettingType;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Emits the settings-and-notifications family of WhatsApp Metrics events that a
 * genuine WhatsApp Web session logs from configuration surfaces Cobalt does not
 * render.
 *
 * <p>WhatsApp Web instruments its whole preferences chrome: the settings-screen
 * item taps and in-settings search ({@code WAWebSettingsLoggerUtils}), the daily
 * notification-preferences snapshot and the per-notification delivery and
 * engagement counters, the chat-wallpaper and chat-theme picker screens
 * ({@code WAWebChatThemeLogger}), the chat-list wallpaper summary, the per-thread
 * wallpaper and theme apply, and the contact/group notification-setting journey
 * ({@code WAWebContactNotificationSettingUserJourneyWamLogging}). Every event in
 * this class is settings-UI impression or interaction telemetry: the WhatsApp Web
 * surface that would fire it has no counterpart in a headless client, so Cobalt has
 * no natural trigger for any of them. A Cobalt session that never emitted these
 * events would be trivially separable from a real Web client whose telemetry stream
 * always carries them.
 *
 * <p>This service therefore synthesises a plausible per-connect snapshot of that
 * surface. It sources real store state wherever a faithful value exists (a real
 * thread identifier, the group flag and participant count of a real chat, whether
 * any chat carries a wallpaper) and fabricates the remaining UI-only fields (which
 * settings item was tapped, the notification toggles, the picked theme and
 * wallpaper ids). Per-interaction identifiers (the client message id, the trace id,
 * the app-session id) are minted fresh on every emit, and the device-appearance and
 * current-theme picks that recur across several of these events are drawn once when
 * this service is constructed, so a single connect reports a self-consistent device
 * identity while successive connects differ. The theme and wallpaper identifiers are
 * drawn from the real WhatsApp Web chat-theme catalogue ({@code WAWebChatThemeEnums}).
 *
 * <p>The single public entrypoint {@link #emitSettingsTelemetry()} is intended to
 * run once per successful connect. It always emits the daily-aggregate snapshot
 * events (the notification-preferences snapshot and engagement counters that
 * WhatsApp Web batches from its scheduled daily-stats task, plus the chat-list
 * wallpaper summary) and emits the interaction events under probability
 * gates so any given session reports an organic subset rather than an identical
 * every-connect burst; across sessions every event fires. Emission cadence is
 * best-effort synthetic: the snapshot events model the once-per-day WhatsApp Web
 * task, the interaction events model sporadic user actions.
 *
 * @implNote
 * This implementation draws the recurring device-identity picks (the device
 * appearance and the current chat-theme, colour-scheme and wallpaper identifiers)
 * once in the constructor from {@link ThreadLocalRandom} and holds them in final
 * fields, so every event a single connect emits reports the same self-consistent
 * device while each new service instance (each connect) redraws them. The
 * per-interaction identifiers and the tally counts are instead minted fresh on every
 * emit through {@link SyntheticTelemetryUtils}, matching how WhatsApp Web re-mints
 * them per interaction rather than reusing one host-stable value.
 *
 * @see WamService
 * @see SyntheticPrivacySafetyTelemetry
 */
@WhatsAppWebModule(moduleName = "WAWebSettingsClickWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSettingsChangeWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSettingsSearchInitiateWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSettingsSearchTapWamEvent")
@WhatsAppWebModule(moduleName = "WAWebNotificationSettingWamEvent")
@WhatsAppWebModule(moduleName = "WAWebNotificationDeliveryWamEvent")
@WhatsAppWebModule(moduleName = "WAWebNotificationEngagementWamEvent")
@WhatsAppWebModule(moduleName = "WAWebChatWallpaperWamEvent")
@WhatsAppWebModule(moduleName = "WAWebChatListWallpaperWamEvent")
@WhatsAppWebModule(moduleName = "WAWebChatThreadWallpaperWamEvent")
@WhatsAppWebModule(moduleName = "WAWebChatThemeScreenWamEvent")
@WhatsAppWebModule(moduleName = "WAWebContactNotificationSettingUserJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSettingsLoggerUtils")
@WhatsAppWebModule(moduleName = "WAWebChatThemeLogger")
public final class SyntheticSettingsTelemetry {
    /**
     * The candidate chat-theme identifiers reported in the theme and per-thread
     * wallpaper events.
     *
     * <p>The values are the real WhatsApp Web theme identifiers exported by
     * {@code WAWebChatThemeEnums.Theme}, so the fabricated {@code chatThemeId}
     * always names a theme the client actually ships.
     */
    private static final String[] CHAT_THEME_IDS = {
            "Default", "WhatsApp-Green", "Sky-Blue", "Emerald",
            "Royal-Blue", "Pearl-Indigo", "Sunset-Orange", "Merino-Teal"
    };

    /**
     * The candidate colour-scheme identifiers reported alongside a chat theme.
     *
     * <p>The values mirror the tonal and minimal scheme identifiers exported by
     * {@code WAWebChatThemeEnums.TonalScheme} and {@code MinimalScheme}, the shape a
     * real {@code colorSchemeId} takes on the wire.
     */
    private static final String[] COLOR_SCHEME_IDS = {
            "Default-Blue@Tonal", "Default-Mono@Tonal", "Sky-Blue@Minimal",
            "Pearl-Indigo@Minimal", "WhatsApp-Green@Tonal"
    };

    /**
     * The candidate top-level settings categories a user browses, drawn from the
     * items a real settings screen exposes.
     */
    private static final SettingsItemType[] SETTINGS_ITEMS = {
            SettingsItemType.ACCOUNT, SettingsItemType.PRIVACY, SettingsItemType.CHATS,
            SettingsItemType.NOTIFICATIONS, SettingsItemType.CHAT_WALLPAPER,
            SettingsItemType.THEME, SettingsItemType.DATA_STORAGE_USAGE
    };

    /**
     * The bound WhatsApp client whose store supplies the live chat and account
     * state sampled when populating the synthesised events.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every synthesised settings event is committed
     * for batched upload.
     */
    private final WamService wamService;

    /**
     * The device appearance reported by every event in this connect that carries an
     * appearance field.
     *
     * <p>Drawn once when this service is constructed so the wallpaper, chat-thread
     * wallpaper, and chat-theme screen events all report the same light-or-dark
     * device, mirroring a real client whose appearance is fixed for the session; a
     * fresh draw per connect keeps successive sessions from sharing one value.
     */
    private final DeviceAppearanceType deviceAppearance;

    /**
     * The current chat-theme identifier reported by the chat-thread wallpaper and
     * chat-theme screen events.
     *
     * <p>Drawn once from {@link #CHAT_THEME_IDS} when this service is constructed so
     * both events report the same applied theme within a connect while successive
     * connects differ.
     */
    private final String chatThemeId;

    /**
     * The current colour-scheme identifier reported alongside {@link #chatThemeId}.
     *
     * <p>Drawn once from {@link #COLOR_SCHEME_IDS} when this service is constructed so
     * both events that report a colour scheme carry the same value within a connect.
     */
    private final String colorSchemeId;

    /**
     * The current numeric wallpaper identifier reported by the chat-thread wallpaper
     * and chat-theme screen events.
     *
     * <p>Drawn once when this service is constructed and stringified to the small
     * numeric index shape WhatsApp Web addresses preset wallpapers by, so both events
     * report the same wallpaper within a connect while successive connects differ.
     */
    private final String wallpaperId;

    /**
     * Constructs a new {@code SyntheticSettingsTelemetry} bound to the given client
     * and WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be
     *                   {@code null}
     * @param wamService the WAM service used to commit the emitted events, must not
     *                   be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticSettingsTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.deviceAppearance = ThreadLocalRandom.current().nextBoolean()
                ? DeviceAppearanceType.DARK
                : DeviceAppearanceType.LIGHT;
        this.chatThemeId = oneOf(CHAT_THEME_IDS);
        this.colorSchemeId = oneOf(COLOR_SCHEME_IDS);
        this.wallpaperId = Integer.toString(SyntheticTelemetryUtils.count(0, 30));
    }

    /**
     * Emits the synthetic settings-and-notifications snapshot for the current
     * connect.
     *
     * <p>The snapshot events that WhatsApp Web batches from its daily-stats task (the
     * notification-preferences snapshot, the notification-engagement counters, and
     * the chat-list wallpaper summary) are committed unconditionally, modelling the
     * once-per-day task as a per-connect heartbeat. The remaining interaction events
     * (settings taps and searches, per-notification delivery, the wallpaper and
     * theme picker screens, the per-thread wallpaper apply, and the contact
     * notification-setting journey) are committed under host-seeded probability gates
     * so a single session reports a plausible organic subset rather than an identical
     * every-connect burst. Across sessions every event in the group fires.
     *
     * @apiNote intended to be invoked once from the client's connect callback; it is
     *          cheap and non-blocking, reading only in-memory store state
     */
    public void emitSettingsTelemetry() {
        commitNotificationSetting();
        commitNotificationEngagement();
        commitChatListWallpaper();

        if (SyntheticTelemetryUtils.chance(33)) {
            commitSettingsClick();
        }
        if (SyntheticTelemetryUtils.chance(25)) {
            commitSettingsChange();
        }
        if (SyntheticTelemetryUtils.chance(20)) {
            commitSettingsSearchInitiate();
        }
        if (SyntheticTelemetryUtils.chance(16)) {
            commitSettingsSearchTap();
        }
        if (SyntheticTelemetryUtils.chance(50)) {
            commitNotificationDelivery();
        }
        if (SyntheticTelemetryUtils.chance(20)) {
            commitChatWallpaper();
        }
        if (SyntheticTelemetryUtils.chance(16)) {
            commitChatThreadWallpaper();
        }
        if (SyntheticTelemetryUtils.chance(20)) {
            commitChatThemeScreen();
        }
        if (SyntheticTelemetryUtils.chance(16)) {
            commitContactNotificationSettingUserJourney();
        }
    }

    /**
     * Builds and commits one {@code SettingsClick} event describing a tap on an
     * item in the settings screen.
     *
     * <p>The tapped item is drawn from the common top-level categories a user browses
     * ({@link #SETTINGS_ITEMS}); the entry point and page are fabricated as a tap that
     * originated from the settings screen itself, and the bookmark-app flag is
     * reported absent because Cobalt hosts no companion bookmark app.
     */
    private void commitSettingsClick() {
        wamService.commit(new SettingsClickEventBuilder()
                .settingsItem(oneOf(SETTINGS_ITEMS))
                .settingsClickEntryPoint(SettingsClickEntryPoint.SETTINGS_SCREEN)
                .settingsPageType(SettingsPageType.SETTINGS)
                .isBookmarkAppInstalled(false)
                .build());
    }

    /**
     * Builds and commits one {@code SettingsChange} event describing a preference
     * toggle, carrying the setting kind and its before/after string values.
     *
     * <p>WhatsApp Web reports the changed value as a stringified scalar. The change is
     * drawn from a small set of plausible toggles (app theme, font size,
     * enter-to-send, link-preview suppression) with the string values a real change
     * of that setting would carry.
     */
    private void commitSettingsChange() {
        var builder = new SettingsChangeEventBuilder();
        switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> builder.settingType(SettingType.APP_THEME)
                    .previousSettingValue("0")
                    .currentSettingValue("2");
            case 1 -> builder.settingType(SettingType.FONT_SIZE)
                    .previousSettingValue("1")
                    .currentSettingValue("2");
            case 2 -> builder.settingType(SettingType.IS_ENTER_TO_SEND_ENABLED)
                    .previousSettingValue("true")
                    .currentSettingValue("false");
            default -> builder.settingType(SettingType.DISABLE_LINK_PREVIEWS)
                    .previousSettingValue("false")
                    .currentSettingValue("true");
        }
        wamService.commit(builder.build());
    }

    /**
     * Builds and commits one {@code SettingsSearchInitiate} event describing the
     * start of a search within the settings screen.
     *
     * <p>The originating page is fabricated as the settings screen, the surface from
     * which an in-settings search is normally initiated.
     */
    private void commitSettingsSearchInitiate() {
        wamService.commit(new SettingsSearchInitiateEventBuilder()
                .settingsPageType(SettingsPageType.SETTINGS)
                .build());
    }

    /**
     * Builds and commits one {@code SettingsSearchTap} event describing a tap on a
     * settings search result.
     *
     * <p>The tapped result and its top-level parent category are fabricated as a
     * consistent pair: the human-readable label names a notifications sub-setting and
     * the parent category is reported as {@link SettingsItemType#NOTIFICATIONS}.
     */
    private void commitSettingsSearchTap() {
        wamService.commit(new SettingsSearchTapEventBuilder()
                .tapItemName("Notifications")
                .topLevelParentSetting(SettingsItemType.NOTIFICATIONS)
                .build());
    }

    /**
     * Builds and commits one {@code NotificationSetting} event carrying the daily
     * snapshot of the client's notification preferences.
     *
     * <p>WhatsApp Web commits this from its daily-stats task by reading the local
     * notification preferences. Cobalt renders no notification chrome, so the snapshot
     * is fabricated as the default enabled configuration a fresh install reports:
     * message and group notifications shown with previews, default sound tones, banner
     * in-app alerts, reactions and status notifications enabled, and the unread badge
     * tracking the current unread count.
     */
    private void commitNotificationSetting() {
        wamService.commit(new NotificationSettingEventBuilder()
                .groupReactionNotification(true)
                .groupShowNotification(true)
                .groupSoundTone(NotificationSoundTone.DEFAULT)
                .inAppNotificationAlertStyle(InAppNotificationAlertStyle.BANNERS)
                .inAppNotificationSound(true)
                .inAppNotificationVibrate(true)
                .messageReactionNotification(true)
                .messageShowNotification(true)
                .messageSoundTone(NotificationSoundTone.DEFAULT)
                .offlineNotification(true)
                .recommendedChannelsNotificationSetting(true)
                .showPreview(true)
                .statusNotificationPriority(false)
                .statusNotificationVibration(true)
                .statusReactionNotification(true)
                .statusReminderNotification(true)
                .statusShowNotification(true)
                .statusSoundTone(NotificationSoundTone.DEFAULT)
                .unreadBadgeSetting(UnreadBadgeSettingType.CURRENT_UNREAD_COUNT)
                .build());
    }

    /**
     * Builds and commits one {@code NotificationDelivery} event describing the
     * display of a single inbound-message notification.
     *
     * <p>The thread identity and destination kind are taken from a real chat when the
     * store holds one, so the reported {@code threadId} and
     * {@code notificationDestination} are genuine; the notification identity,
     * client-message identity, delivery latency, and trace id are fabricated to mirror
     * a text-message push that was shown promptly and not silenced.
     */
    private void commitNotificationDelivery() {
        var builder = new NotificationDeliveryEventBuilder()
                .notificationAction(NotificationActionType.SHOW)
                .notificationSource(NotificationSourceType.PUSH_TRIGGERED)
                .uiNotificationType(NotificationTypeEnum.TEXT_MESSAGE)
                .notificationId(SyntheticTelemetryUtils.newSessionId())
                .clientMessageId(messageId())
                .notificationDeliveryT(SyntheticTelemetryUtils.count(40, 900))
                .pushToNotifT(Instant.ofEpochMilli(SyntheticTelemetryUtils.count(30, 600)))
                .traceIdInt(traceId())
                .isSilenced(false)
                .triggeredByOfflineMessage(false);
        var chat = firstChat();
        if (chat.isPresent()) {
            var jid = chat.get().jid();
            builder.threadId(jid.toString())
                    .notificationDestination(jid.hasGroupOrCommunityServer()
                            ? NotificationDestinationType.GROUP
                            : NotificationDestinationType.INDIVIDUAL);
        } else {
            builder.notificationDestination(NotificationDestinationType.INDIVIDUAL);
        }
        wamService.commit(builder.build());
    }

    /**
     * Builds and commits one {@code NotificationEngagement} event carrying the daily
     * aggregated notification engagement counters for a thread.
     *
     * <p>WhatsApp Web accumulates per-thread engagement counts and commits them from
     * its daily-stats task. The thread identity, group flag, group-size bucket, and
     * group type are taken from a real group chat when the store holds one; the
     * shown, opened, read, reply, preview, and other tallies plus the average
     * engagement latency are fabricated as a lightly used day, with the opened count
     * kept at or below the shown count.
     */
    private void commitNotificationEngagement() {
        var builder = new NotificationEngagementEventBuilder();
        var group = firstGroupChat();
        if (group.isPresent()) {
            var chat = group.get();
            builder.isAGroup(true)
                    .threadId(chat.jid().toString())
                    .groupSizeBucket(sizeBucket(chat.participant().size()))
                    .groupTypeClient(chat.isParentGroup()
                            ? GroupTypeClient.PARENT_GROUP
                            : GroupTypeClient.REGULAR_GROUP);
        } else {
            firstChat().ifPresent(chat -> builder
                    .isAGroup(chat.jid().hasGroupOrCommunityServer())
                    .threadId(chat.jid().toString()));
        }
        var shown = SyntheticTelemetryUtils.count(6, 45);
        wamService.commit(builder
                .totalNotifShown(shown)
                .totalNotifTapToOpen(SyntheticTelemetryUtils.count(0, shown))
                .totalNotifMarkAsRead(SyntheticTelemetryUtils.count(0, 12))
                .totalNotifReply(SyntheticTelemetryUtils.count(0, 8))
                .totalNotifShowPreview(shown)
                .totalNotifOthers(SyntheticTelemetryUtils.count(0, 4))
                .avgNotifEngagementT(Instant.ofEpochMilli(SyntheticTelemetryUtils.count(600, 9000)))
                .build());
    }

    /**
     * Builds and commits one {@code ChatWallpaper} event describing a visit to the
     * wallpaper picker screen.
     *
     * <p>The visit is fabricated as an app-wide wallpaper screen the user browsed
     * without applying a change; the wallpaper type and device appearance are drawn to
     * mirror the default wallpaper under a randomly light or dark appearance.
     */
    private void commitChatWallpaper() {
        wamService.commit(new ChatWallpaperEventBuilder()
                .chatWallpaperVisit(true)
                .chatWallpaperChangeApplied(false)
                .chatWallpaperSource(ChatWallpaperEntryType.APP_WIDE)
                .chatWallpaperType(ChatWallpaperType.DEFAULT)
                .appearanceType(deviceAppearance)
                .build());
    }

    /**
     * Builds and commits one {@code ChatListWallpaper} event summarising the
     * chat-list wallpaper and theme configuration.
     *
     * <p>The applied flag is computed from live chat-store state (whether any chat
     * carries a wallpaper override) so the reported {@code anyWallpaperApplied} is
     * genuine; chat themes are reported as enabled, matching the default gating.
     */
    private void commitChatListWallpaper() {
        var anyWallpaper = client.store()
                .chatStore()
                .chats()
                .stream()
                .anyMatch(chat -> chat.wallpaper().isPresent());
        wamService.commit(new ChatListWallpaperEventBuilder()
                .anyWallpaperApplied(anyWallpaper)
                .chatThemesEnabled(true)
                .build());
    }

    /**
     * Builds and commits one {@code ChatThreadWallpaper} event describing a wallpaper
     * or theme apply on a specific chat thread.
     *
     * <p>The thread identity, chat type, and community-membership flag are taken from
     * a real chat when the store holds one, so the reported {@code threadId},
     * {@code chatType}, and {@code belongsToCommunity} are genuine; the applied theme,
     * colour scheme, and wallpaper identifiers are fabricated from the real WhatsApp
     * Web theme catalogue.
     */
    private void commitChatThreadWallpaper() {
        var builder = new ChatThreadWallpaperEventBuilder()
                .chatThemeId(chatThemeId)
                .chatThemeSource(ChatThemeEntryType.ONE_TO_ONE)
                .colorSchemeId(colorSchemeId)
                .wallpaperId(wallpaperId)
                .wallpaperApplied(true)
                .appearanceType(deviceAppearance);
        var chat = firstChat();
        if (chat.isPresent()) {
            var c = chat.get();
            builder.threadId(c.jid().toString())
                    .chatType(chatType(c))
                    .belongsToCommunity(c.parentGroupId().isPresent());
        }
        wamService.commit(builder.build());
    }

    /**
     * Builds and commits one {@code ChatThemeScreen} event describing an interaction
     * with the chat-theme picker screen.
     *
     * <p>The interaction is fabricated as opening the theme screen and applying a
     * theme drawn from the real WhatsApp Web catalogue, with a matching colour scheme
     * and wallpaper identifier under a randomly light or dark appearance.
     */
    private void commitChatThemeScreen() {
        wamService.commit(new ChatThemeScreenEventBuilder()
                .appearanceType(deviceAppearance)
                .chatThemeChangeApplied(SyntheticTelemetryUtils.chance(50))
                .chatThemeId(chatThemeId)
                .chatThemeSource(ChatThemeEntryType.APP_WIDE)
                .chatWallpaperType(ChatWallpaperType.DEFAULT)
                .colorSchemeId(colorSchemeId)
                .wallpaperId(wallpaperId)
                .build());
    }

    /**
     * Builds and commits one {@code ContactNotificationSettingUserJourney} event
     * describing a change to a contact or group notification setting.
     *
     * <p>The group size is taken from a real group chat when the store holds one, so
     * the reported {@code groupSize} is genuine; the app-session identity, the
     * mute-style action, and the originating surface are fabricated to mirror a user
     * muting a thread for eight hours from the chat-info page.
     */
    private void commitContactNotificationSettingUserJourney() {
        var builder = new ContactNotificationSettingUserJourneyEventBuilder()
                .appSessionId(SyntheticTelemetryUtils.newSessionId())
                .contactNotificationSettingActionType(oneOf(new ContactNotificationSettingActionType[]{
                        ContactNotificationSettingActionType.MESSAGES_MUTE_8H,
                        ContactNotificationSettingActionType.MESSAGES_MUTE_1W,
                        ContactNotificationSettingActionType.MESSAGES_UNMUTE,
                        ContactNotificationSettingActionType.MUTE_MENTION_EVERYONE_ON
                }))
                .uiSurface(TsSurface.CHAT_INFO_PAGE);
        firstGroupChat().ifPresent(chat -> builder.groupSize(chat.participant().size()));
        wamService.commit(builder.build());
    }

    /**
     * Returns any one chat from the store.
     *
     * @return the first chat the store yields, or empty when no chats exist
     */
    private Optional<Chat> firstChat() {
        return client.store()
                .chatStore()
                .chats()
                .stream()
                .findFirst();
    }

    /**
     * Returns any one group or community chat from the store.
     *
     * @return the first group-server chat the store yields, or empty when none exist
     */
    private Optional<Chat> firstGroupChat() {
        return client.store()
                .chatStore()
                .chats()
                .stream()
                .filter(chat -> chat.jid().hasGroupOrCommunityServer())
                .findFirst();
    }

    /**
     * Maps a chat to its coarse {@link MessageChatType} classification.
     *
     * @param chat the chat to classify
     * @return {@link MessageChatType#GROUP} for a group or community chat,
     *         {@link MessageChatType#INDIVIDUAL} otherwise
     */
    private MessageChatType chatType(Chat chat) {
        return chat.jid().hasGroupOrCommunityServer()
                ? MessageChatType.GROUP
                : MessageChatType.INDIVIDUAL;
    }

    /**
     * Buckets a group participant count into the coarse
     * {@link ClientGroupSizeBucket} the notification-engagement event reports.
     *
     * @param size the group participant count
     * @return the matching size bucket
     */
    private ClientGroupSizeBucket sizeBucket(int size) {
        if (size < 50) {
            return ClientGroupSizeBucket.SMALL;
        }
        if (size < 150) {
            return ClientGroupSizeBucket.MEDIUM;
        }
        if (size < 500) {
            return ClientGroupSizeBucket.LARGE;
        }
        if (size < 1024) {
            return ClientGroupSizeBucket.EXTRA_LARGE;
        }
        return ClientGroupSizeBucket.LARGEST_BUCKET;
    }

    /**
     * Fabricates a fresh client message identifier.
     *
     * <p>The value lands in the uppercase-hexadecimal range WhatsApp message ids
     * occupy: the literal {@code 3EB0} prefix a Web-minted id carries followed by
     * sixteen random hexadecimal characters. A distinct value is drawn on every call,
     * as WhatsApp Web mints a fresh id per notification, so no constant fingerprints
     * successive emissions.
     *
     * @return a freshly fabricated message identifier
     */
    private static String messageId() {
        return "3EB0" + SyntheticTelemetryUtils.randomHexLower(8).toUpperCase(Locale.ROOT);
    }

    /**
     * Fabricates a fresh notification trace identifier.
     *
     * <p>The value is drawn on every call from the positive range a real trace id
     * occupies, so no constant fingerprints successive notifications.
     *
     * @return a freshly fabricated trace identifier
     */
    private static long traceId() {
        return SyntheticTelemetryUtils.between(1_000_000_000L, 3_147_483_647L);
    }

    /**
     * Returns a uniformly random element of the given array.
     *
     * <p>The element is drawn from {@link ThreadLocalRandom} so each call is fresh;
     * callers that need a value stable across a connect capture the result in a final
     * field at construction rather than calling this per emit.
     *
     * @param options the candidate values, must be non-empty
     * @param <T>     the element type
     * @return one element of {@code options}
     */
    private static <T> T oneOf(T[] options) {
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }
}
