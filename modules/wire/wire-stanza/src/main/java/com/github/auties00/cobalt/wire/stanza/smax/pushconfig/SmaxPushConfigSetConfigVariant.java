package com.github.auties00.cobalt.wire.stanza.smax.pushconfig;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the sealed disjunction of platform-specific {@code <config>} payloads carried by a
 * {@link SmaxPushConfigSetSetVariant.Config}.
 *
 * <p>Each variant maps to one supported push platform: Facebook ({@link FbConfig}), Android
 * ({@link AndroidConfig}), APNs/iOS ({@link AppleConfig}), Windows Notification Service
 * ({@link WnsConfig}), enterprise deployments ({@link EnterpriseConfig}), and W3C Web Push
 * ({@link WebConfig}). The caller picks the variant matching its notification backend and passes
 * it to {@link SmaxPushConfigSetSetVariant.Config}, which materialises it via {@link #toStanza()}.
 */
public sealed interface SmaxPushConfigSetConfigVariant
        permits SmaxPushConfigSetConfigVariant.FbConfig, SmaxPushConfigSetConfigVariant.AndroidConfig,
        SmaxPushConfigSetConfigVariant.AppleConfig, SmaxPushConfigSetConfigVariant.WnsConfig,
        SmaxPushConfigSetConfigVariant.EnterpriseConfig, SmaxPushConfigSetConfigVariant.WebConfig {

    /**
     * Builds the {@code <config platform=...>} child stanza for this variant.
     *
     * <p>{@link SmaxPushConfigSetSetVariant.Config#toStanza()} calls this method to materialise the
     * variant into the outbound stanza.
     *
     * @implSpec Implementations must return a single {@link Stanza} whose description is
     * {@code config} and whose {@code platform} attribute identifies the push backend.
     * @return the {@link Stanza} for this variant
     */
    Stanza toStanza();

    /**
     * Represents the Facebook-client {@code <config platform="fb">} variant.
     *
     * <p>Registers a Facebook-app push channel, carrying the FB app id and device id with an
     * optional FB user id.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutPushConfigFBClientMixin")
    final class FbConfig implements SmaxPushConfigSetConfigVariant {
        /**
         * Holds the mandatory {@code appid} attribute.
         */
        private final String configAppid;

        /**
         * Holds the mandatory {@code deviceid} attribute.
         */
        private final String configDeviceid;

        /**
         * Holds the optional {@code fbid} attribute.
         */
        private final String configFbid;

        /**
         * Constructs a Facebook-client config from its three attributes.
         *
         * @param configAppid    the {@code appid} attribute
         * @param configDeviceid the {@code deviceid} attribute
         * @param configFbid     the optional {@code fbid} attribute
         * @throws NullPointerException if {@code configAppid} or {@code configDeviceid} is
         *                              {@code null}
         */
        public FbConfig(String configAppid, String configDeviceid, String configFbid) {
            this.configAppid = Objects.requireNonNull(configAppid, "configAppid cannot be null");
            this.configDeviceid = Objects.requireNonNull(configDeviceid, "configDeviceid cannot be null");
            this.configFbid = configFbid;
        }

        /**
         * Returns the {@code appid} attribute.
         *
         * @return the appid
         */
        public String configAppid() {
            return configAppid;
        }

        /**
         * Returns the {@code deviceid} attribute.
         *
         * @return the device id
         */
        public String configDeviceid() {
            return configDeviceid;
        }

        /**
         * Returns the optional {@code fbid} attribute.
         *
         * @return an {@link Optional} carrying the FB id
         */
        public Optional<String> configFbid() {
            return Optional.ofNullable(configFbid);
        }

        /**
         * Builds the {@code <config platform="fb">} stanza, emitting {@code fbid} only when present.
         *
         * @implNote This implementation hard-codes {@code platform="fb"} per the
         * {@code mergeFBClientMixin} fixture and emits the optional {@code fbid} attribute only
         * when non-null.
         * @return the {@link Stanza}
         */
        @Override
        @WhatsAppWebExport(moduleName = "WASmaxOutPushConfigFBClientMixin",
                exports = "mergeFBClientMixin",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Stanza toStanza() {
            var builder = new StanzaBuilder()
                    .description("config")
                    .attribute("platform", "fb")
                    .attribute("appid", configAppid)
                    .attribute("deviceid", configDeviceid);
            if (configFbid != null) {
                builder.attribute("fbid", configFbid);
            }
            return builder.build();
        }

        /**
         * Compares this config to another object for equality on its three attributes.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an {@link FbConfig} with equal attributes
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (FbConfig) obj;
            return Objects.equals(this.configAppid, that.configAppid)
                    && Objects.equals(this.configDeviceid, that.configDeviceid)
                    && Objects.equals(this.configFbid, that.configFbid);
        }

        /**
         * Returns a hash code derived from the three carried attributes.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(configAppid, configDeviceid, configFbid);
        }

        /**
         * Returns a debug rendering of this config.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxPushConfigSetConfigVariant.FbConfig[configAppid=" + configAppid
                    + ", configDeviceid=" + configDeviceid
                    + ", configFbid=" + configFbid + ']';
        }
    }

    /**
     * Represents the Android-client {@code <config>} variant.
     *
     * <p>Carries a list of per-group mute items; the relay uses the list to suppress push
     * deliveries for muted groups on the Android notification channel.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutPushConfigAndroidClientMixin")
    @WhatsAppWebModule(moduleName = "WASmaxOutPushConfigAndroidClientConfigMixin")
    final class AndroidConfig implements SmaxPushConfigSetConfigVariant {
        /**
         * Holds the list of {@code <item jid mute/>} mute entries.
         */
        private final List<AndroidMuteItem> itemArgs;

        /**
         * Constructs an Android-client config from its mute items.
         *
         * <p>The supplied list is defensively copied to keep the variant immutable.
         *
         * @param itemArgs the mute items
         * @throws NullPointerException if {@code itemArgs} is {@code null}
         */
        public AndroidConfig(List<AndroidMuteItem> itemArgs) {
            Objects.requireNonNull(itemArgs, "itemArgs cannot be null");
            this.itemArgs = List.copyOf(itemArgs);
        }

        /**
         * Returns the mute items.
         *
         * @return an unmodifiable {@link List} of {@link AndroidMuteItem}
         */
        public List<AndroidMuteItem> itemArgs() {
            return itemArgs;
        }

        /**
         * Builds the {@code <config>} stanza with one {@code <item>} grandchild per mute entry.
         *
         * @implNote This implementation emits one {@code <item>} grandchild per entry under the
         * single {@code <config>} parent, mirroring the {@code mergeAndroidClientMixin} fixture.
         * @return the {@link Stanza}
         */
        @Override
        @WhatsAppWebExport(moduleName = "WASmaxOutPushConfigAndroidClientMixin",
                exports = "mergeAndroidClientMixin",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Stanza toStanza() {
            var children = new ArrayList<Stanza>(itemArgs.size());
            for (var item : itemArgs) {
                children.add(item.toStanza());
            }
            return new StanzaBuilder()
                    .description("config")
                    .content(children)
                    .build();
        }

        /**
         * Compares this config to another object for equality on the mute-items list.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an {@link AndroidConfig} with an equal list
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (AndroidConfig) obj;
            return Objects.equals(this.itemArgs, that.itemArgs);
        }

        /**
         * Returns a hash code derived from the mute-items list.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(itemArgs);
        }

        /**
         * Returns a debug rendering of this config.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxPushConfigSetConfigVariant.AndroidConfig[itemArgs=" + itemArgs + ']';
        }

        /**
         * Represents a single {@code <item jid mute/>} mute entry carried by an
         * {@link AndroidConfig}.
         *
         * <p>Pairs a group {@link Jid} with the numeric mute marker the relay forwards to the
         * Android notification channel.
         */
        public static final class AndroidMuteItem {
            /**
             * Holds the group {@link Jid} being muted.
             */
            private final Jid itemJid;

            /**
             * Holds the numeric mute marker.
             */
            private final long itemMute;

            /**
             * Constructs a mute item from a group JID and a mute marker.
             *
             * @param itemJid  the group {@link Jid}
             * @param itemMute the mute marker
             * @throws NullPointerException if {@code itemJid} is {@code null}
             */
            public AndroidMuteItem(Jid itemJid, long itemMute) {
                this.itemJid = Objects.requireNonNull(itemJid, "itemJid cannot be null");
                this.itemMute = itemMute;
            }

            /**
             * Returns the group {@link Jid}.
             *
             * @return the {@link Jid}
             */
            public Jid itemJid() {
                return itemJid;
            }

            /**
             * Returns the mute marker.
             *
             * @return the marker
             */
            public long itemMute() {
                return itemMute;
            }

            /**
             * Builds the {@code <item jid mute/>} child stanza.
             *
             * @implNote This implementation matches the {@code makeAndroidClientItem} fixture
             * verbatim.
             * @return the {@link Stanza}
             */
            @WhatsAppWebExport(moduleName = "WASmaxOutPushConfigAndroidClientMixin",
                    exports = "makeAndroidClientItem",
                    adaptation = WhatsAppAdaptation.DIRECT)
            public Stanza toStanza() {
                return new StanzaBuilder()
                        .description("item")
                        .attribute("jid", itemJid)
                        .attribute("mute", itemMute)
                        .build();
            }

            /**
             * Compares this item to another object for equality on the JID and mute marker.
             *
             * @param obj the object to compare against
             * @return {@code true} when {@code obj} is an {@link AndroidMuteItem} with an equal
             *         JID and marker
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (AndroidMuteItem) obj;
                return this.itemMute == that.itemMute
                        && Objects.equals(this.itemJid, that.itemJid);
            }

            /**
             * Returns a hash code derived from the JID and mute marker.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(itemJid, itemMute);
            }

            /**
             * Returns a debug rendering of this item.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "SmaxPushConfigSetConfigVariant.AndroidConfig.AndroidMuteItem[itemJid=" + itemJid
                        + ", itemMute=" + itemMute + ']';
            }
        }
    }

    /**
     * Represents the Apple-client {@code <config>} variant.
     *
     * <p>Carries the full set of APNs/iOS-specific attributes: device token, VoIP token,
     * Notification Service Extension flags, Apple Watch pairing, and the per-chat preference list.
     * Callers mirroring the iOS client populate the complete set; callers on other platforms pick a
     * different variant.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutPushConfigAppleClientMixin")
    final class AppleConfig implements SmaxPushConfigSetConfigVariant {
        /**
         * Holds the mandatory {@code platform} attribute, typically {@code "iphone"} or
         * {@code "ipad"}.
         */
        private final String configPlatform;

        /**
         * Holds whether the {@code version="2"} marker is set.
         */
        private final boolean hasConfigVersion2;

        /**
         * Holds the optional {@code id} attribute.
         */
        private final String configId;

        /**
         * Holds the optional {@code voip} attribute carrying the VoIP token.
         */
        private final String configVoip;

        /**
         * Holds the mandatory {@code preview} attribute.
         */
        private final String configPreview;

        /**
         * Holds the mandatory {@code default} attribute.
         */
        private final String configDefault;

        /**
         * Holds the mandatory {@code groups} attribute.
         */
        private final String configGroups;

        /**
         * Holds the mandatory {@code call} attribute.
         */
        private final String configCall;

        /**
         * Holds the optional {@code status_sound} attribute.
         */
        private final String configStatusSound;

        /**
         * Holds the mandatory {@code lg} (language) attribute.
         */
        private final String configLg;

        /**
         * Holds the mandatory {@code lc} (locale) attribute.
         */
        private final String configLc;

        /**
         * Holds the optional {@code background_location} attribute.
         */
        private final String configBackgroundLocation;

        /**
         * Holds the optional {@code nse_ver} attribute.
         */
        private final String configNseVer;

        /**
         * Holds the optional {@code nse_call} attribute.
         */
        private final String configNseCall;

        /**
         * Holds the optional {@code nse_read} attribute.
         */
        private final String configNseRead;

        /**
         * Holds the optional {@code nse_retry} attribute.
         */
        private final String configNseRetry;

        /**
         * Holds the optional {@code reg_push} attribute.
         */
        private final String configRegPush;

        /**
         * Holds the optional {@code pkey} attribute.
         */
        private final String configPkey;

        /**
         * Holds the mandatory {@code voip_payload_type} attribute.
         */
        private final String configVoipPayloadType;

        /**
         * Holds the optional {@code settings} attribute.
         */
        private final Long configSettings;

        /**
         * Holds the optional {@code app_mute} attribute.
         */
        private final Long configAppMute;

        /**
         * Holds the optional {@code apple_watch_id} attribute.
         */
        private final String configAppleWatchId;

        /**
         * Holds the optional {@code apple_watch_pkey} attribute.
         */
        private final String configAppleWatchPkey;

        /**
         * Holds the per-chat {@link AppleItem} entries.
         */
        private final List<AppleItem> itemArgs;

        /**
         * Constructs an Apple-client config from the full set of APNs attributes.
         *
         * @param configPlatform           the platform marker
         * @param hasConfigVersion2        whether the {@code version="2"} marker is set
         * @param configId                 the optional id
         * @param configVoip               the optional voip token
         * @param configPreview            the preview marker
         * @param configDefault            the default marker
         * @param configGroups             the groups marker
         * @param configCall               the call marker
         * @param configStatusSound        the optional status-sound marker
         * @param configLg                 the language tag
         * @param configLc                 the locale tag
         * @param configBackgroundLocation the optional background-location marker
         * @param configNseVer             the optional NSE version
         * @param configNseCall            the optional NSE call marker
         * @param configNseRead            the optional NSE read marker
         * @param configNseRetry           the optional NSE retry marker
         * @param configRegPush            the optional reg-push marker
         * @param configPkey               the optional pkey
         * @param configVoipPayloadType    the voip payload type
         * @param configSettings           the optional settings mask
         * @param configAppMute            the optional app-mute mask
         * @param configAppleWatchId       the optional Apple Watch id
         * @param configAppleWatchPkey     the optional Apple Watch pkey
         * @param itemArgs                 the per-item entries
         * @throws NullPointerException if any mandatory argument is {@code null}
         */
        public AppleConfig(String configPlatform, boolean hasConfigVersion2,
                           String configId, String configVoip,
                           String configPreview, String configDefault, String configGroups,
                           String configCall, String configStatusSound,
                           String configLg, String configLc, String configBackgroundLocation,
                           String configNseVer, String configNseCall, String configNseRead,
                           String configNseRetry, String configRegPush, String configPkey,
                           String configVoipPayloadType, Long configSettings, Long configAppMute,
                           String configAppleWatchId, String configAppleWatchPkey,
                           List<AppleItem> itemArgs) {
            this.configPlatform = Objects.requireNonNull(configPlatform, "configPlatform cannot be null");
            this.hasConfigVersion2 = hasConfigVersion2;
            this.configId = configId;
            this.configVoip = configVoip;
            this.configPreview = Objects.requireNonNull(configPreview, "configPreview cannot be null");
            this.configDefault = Objects.requireNonNull(configDefault, "configDefault cannot be null");
            this.configGroups = Objects.requireNonNull(configGroups, "configGroups cannot be null");
            this.configCall = Objects.requireNonNull(configCall, "configCall cannot be null");
            this.configStatusSound = configStatusSound;
            this.configLg = Objects.requireNonNull(configLg, "configLg cannot be null");
            this.configLc = Objects.requireNonNull(configLc, "configLc cannot be null");
            this.configBackgroundLocation = configBackgroundLocation;
            this.configNseVer = configNseVer;
            this.configNseCall = configNseCall;
            this.configNseRead = configNseRead;
            this.configNseRetry = configNseRetry;
            this.configRegPush = configRegPush;
            this.configPkey = configPkey;
            this.configVoipPayloadType = Objects.requireNonNull(configVoipPayloadType,
                    "configVoipPayloadType cannot be null");
            this.configSettings = configSettings;
            this.configAppMute = configAppMute;
            this.configAppleWatchId = configAppleWatchId;
            this.configAppleWatchPkey = configAppleWatchPkey;
            Objects.requireNonNull(itemArgs, "itemArgs cannot be null");
            this.itemArgs = List.copyOf(itemArgs);
        }

        /**
         * Returns the platform marker.
         *
         * <p>Distinguishes {@code iphone} from {@code ipad} on the relay side.
         *
         * @return the marker
         */
        public String configPlatform() {
            return configPlatform;
        }

        /**
         * Returns whether the {@code version="2"} marker is set.
         *
         * <p>Selects between the legacy and the modern v2 fixture.
         *
         * @return {@code true} when set
         */
        public boolean hasConfigVersion2() {
            return hasConfigVersion2;
        }

        /**
         * Returns the optional {@code id} attribute, surfaced by the relay only for legacy iOS
         * variants.
         *
         * @return an {@link Optional} carrying the id
         */
        public Optional<String> configId() {
            return Optional.ofNullable(configId);
        }

        /**
         * Returns the optional VoIP token, required when VoIP wake-ups are routed through APNs.
         *
         * @return an {@link Optional} carrying the token
         */
        public Optional<String> configVoip() {
            return Optional.ofNullable(configVoip);
        }

        /**
         * Returns the preview marker, controlling whether the relay attaches the message preview
         * to the APNs payload.
         *
         * @return the marker
         */
        public String configPreview() {
            return configPreview;
        }

        /**
         * Returns the default marker carrying the default chat-notification mode.
         *
         * @return the marker
         */
        public String configDefault() {
            return configDefault;
        }

        /**
         * Returns the groups marker carrying the default group-notification mode.
         *
         * @return the marker
         */
        public String configGroups() {
            return configGroups;
        }

        /**
         * Returns the call marker carrying the default call-notification mode.
         *
         * @return the marker
         */
        public String configCall() {
            return configCall;
        }

        /**
         * Returns the optional status-sound marker; absence falls back to the system default.
         *
         * @return an {@link Optional} carrying the marker
         */
        public Optional<String> configStatusSound() {
            return Optional.ofNullable(configStatusSound);
        }

        /**
         * Returns the language tag the relay uses when localising notification text.
         *
         * @return the tag
         */
        public String configLg() {
            return configLg;
        }

        /**
         * Returns the locale tag the relay uses when localising notification text.
         *
         * @return the tag
         */
        public String configLc() {
            return configLc;
        }

        /**
         * Returns the optional background-location marker.
         *
         * @return an {@link Optional} carrying the marker
         */
        public Optional<String> configBackgroundLocation() {
            return Optional.ofNullable(configBackgroundLocation);
        }

        /**
         * Returns the optional Notification Service Extension version.
         *
         * @return an {@link Optional} carrying the version
         */
        public Optional<String> configNseVer() {
            return Optional.ofNullable(configNseVer);
        }

        /**
         * Returns the optional NSE call marker indicating whether the NSE handles call wake-ups.
         *
         * @return an {@link Optional} carrying the marker
         */
        public Optional<String> configNseCall() {
            return Optional.ofNullable(configNseCall);
        }

        /**
         * Returns the optional NSE read marker indicating whether the NSE handles read-receipt
         * wake-ups.
         *
         * @return an {@link Optional} carrying the marker
         */
        public Optional<String> configNseRead() {
            return Optional.ofNullable(configNseRead);
        }

        /**
         * Returns the optional NSE retry marker indicating whether the NSE retries failed
         * decryptions.
         *
         * @return an {@link Optional} carrying the marker
         */
        public Optional<String> configNseRetry() {
            return Optional.ofNullable(configNseRetry);
        }

        /**
         * Returns the optional reg-push marker carrying the registration-push flag.
         *
         * @return an {@link Optional} carrying the marker
         */
        public Optional<String> configRegPush() {
            return Optional.ofNullable(configRegPush);
        }

        /**
         * Returns the optional pkey that pins the iOS push channel to a specific public key.
         *
         * @return an {@link Optional} carrying the pkey
         */
        public Optional<String> configPkey() {
            return Optional.ofNullable(configPkey);
        }

        /**
         * Returns the VoIP payload type that selects the APNs VoIP payload shape.
         *
         * <p>Required even when {@link #configVoip()} is absent.
         *
         * @return the type
         */
        public String configVoipPayloadType() {
            return configVoipPayloadType;
        }

        /**
         * Returns the optional bitmask of notification settings forwarded to the relay.
         *
         * @return an {@link Optional} carrying the mask
         */
        public Optional<Long> configSettings() {
            return Optional.ofNullable(configSettings);
        }

        /**
         * Returns the optional bitmask of app-mute settings forwarded to the relay.
         *
         * @return an {@link Optional} carrying the mask
         */
        public Optional<Long> configAppMute() {
            return Optional.ofNullable(configAppMute);
        }

        /**
         * Returns the optional Apple Watch id used when an Apple Watch is paired with the iOS
         * client.
         *
         * @return an {@link Optional} carrying the id
         */
        public Optional<String> configAppleWatchId() {
            return Optional.ofNullable(configAppleWatchId);
        }

        /**
         * Returns the optional Apple Watch pkey that pins the paired-watch push channel to a
         * specific public key.
         *
         * @return an {@link Optional} carrying the pkey
         */
        public Optional<String> configAppleWatchPkey() {
            return Optional.ofNullable(configAppleWatchPkey);
        }

        /**
         * Returns the per-chat entries.
         *
         * <p>Each entry carries non-default mute/notify/call preferences that the relay merges with
         * the global defaults.
         *
         * @return an unmodifiable {@link List} of {@link AppleItem}
         */
        public List<AppleItem> itemArgs() {
            return itemArgs;
        }

        /**
         * Builds the {@code <config platform=...>} stanza with the full APNs attribute set and one
         * {@code <item>} grandchild per per-chat entry.
         *
         * @implNote This implementation hard-codes the mandatory attributes per the
         * {@code mergeAppleClientMixin} fixture and adds every optional attribute only when its
         * backing field is non-null; the {@code version="2"} marker is emitted via the
         * {@link StanzaBuilder#attribute(String, String, boolean)} conditional overload keyed on
         * {@link #hasConfigVersion2()}.
         * @return the {@link Stanza}
         */
        @Override
        @WhatsAppWebExport(moduleName = "WASmaxOutPushConfigAppleClientMixin",
                exports = "mergeAppleClientMixin",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Stanza toStanza() {
            var children = new ArrayList<Stanza>(itemArgs.size());
            for (var item : itemArgs) {
                children.add(item.toStanza());
            }
            var builder = new StanzaBuilder()
                    .description("config")
                    .attribute("platform", configPlatform)
                    .attribute("version", "2", hasConfigVersion2)
                    .attribute("preview", configPreview)
                    .attribute("default", configDefault)
                    .attribute("groups", configGroups)
                    .attribute("call", configCall)
                    .attribute("lg", configLg)
                    .attribute("lc", configLc)
                    .attribute("voip_payload_type", configVoipPayloadType);
            if (configId != null) {
                builder.attribute("id", configId);
            }
            if (configVoip != null) {
                builder.attribute("voip", configVoip);
            }
            if (configStatusSound != null) {
                builder.attribute("status_sound", configStatusSound);
            }
            if (configBackgroundLocation != null) {
                builder.attribute("background_location", configBackgroundLocation);
            }
            if (configNseVer != null) {
                builder.attribute("nse_ver", configNseVer);
            }
            if (configNseCall != null) {
                builder.attribute("nse_call", configNseCall);
            }
            if (configNseRead != null) {
                builder.attribute("nse_read", configNseRead);
            }
            if (configNseRetry != null) {
                builder.attribute("nse_retry", configNseRetry);
            }
            if (configRegPush != null) {
                builder.attribute("reg_push", configRegPush);
            }
            if (configPkey != null) {
                builder.attribute("pkey", configPkey);
            }
            if (configSettings != null) {
                builder.attribute("settings", configSettings);
            }
            if (configAppMute != null) {
                builder.attribute("app_mute", configAppMute);
            }
            if (configAppleWatchId != null) {
                builder.attribute("apple_watch_id", configAppleWatchId);
            }
            if (configAppleWatchPkey != null) {
                builder.attribute("apple_watch_pkey", configAppleWatchPkey);
            }
            builder.content(children);
            return builder.build();
        }

        /**
         * Compares this config to another object for equality on every attribute and the items
         * list.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an {@link AppleConfig} with equal state
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (AppleConfig) obj;
            return this.hasConfigVersion2 == that.hasConfigVersion2
                    && Objects.equals(this.configPlatform, that.configPlatform)
                    && Objects.equals(this.configId, that.configId)
                    && Objects.equals(this.configVoip, that.configVoip)
                    && Objects.equals(this.configPreview, that.configPreview)
                    && Objects.equals(this.configDefault, that.configDefault)
                    && Objects.equals(this.configGroups, that.configGroups)
                    && Objects.equals(this.configCall, that.configCall)
                    && Objects.equals(this.configStatusSound, that.configStatusSound)
                    && Objects.equals(this.configLg, that.configLg)
                    && Objects.equals(this.configLc, that.configLc)
                    && Objects.equals(this.configBackgroundLocation, that.configBackgroundLocation)
                    && Objects.equals(this.configNseVer, that.configNseVer)
                    && Objects.equals(this.configNseCall, that.configNseCall)
                    && Objects.equals(this.configNseRead, that.configNseRead)
                    && Objects.equals(this.configNseRetry, that.configNseRetry)
                    && Objects.equals(this.configRegPush, that.configRegPush)
                    && Objects.equals(this.configPkey, that.configPkey)
                    && Objects.equals(this.configVoipPayloadType, that.configVoipPayloadType)
                    && Objects.equals(this.configSettings, that.configSettings)
                    && Objects.equals(this.configAppMute, that.configAppMute)
                    && Objects.equals(this.configAppleWatchId, that.configAppleWatchId)
                    && Objects.equals(this.configAppleWatchPkey, that.configAppleWatchPkey)
                    && Objects.equals(this.itemArgs, that.itemArgs);
        }

        /**
         * Returns a hash code derived from every attribute and the items list.
         *
         * @implNote This implementation combines a primary {@link Objects#hash(Object...)} call
         * over the scalar attributes with a separate combine for the items list to keep the
         * argument count within the {@link Objects#hash(Object...)} varargs limit.
         * @return the hash code
         */
        @Override
        public int hashCode() {
            var result = Objects.hash(configPlatform, hasConfigVersion2, configId, configVoip,
                    configPreview, configDefault, configGroups, configCall, configStatusSound,
                    configLg, configLc, configBackgroundLocation, configNseVer, configNseCall,
                    configNseRead, configNseRetry, configRegPush, configPkey, configVoipPayloadType,
                    configSettings, configAppMute, configAppleWatchId, configAppleWatchPkey);
            result = 31 * result + Objects.hashCode(itemArgs);
            return result;
        }

        /**
         * Returns a debug rendering of this config.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxPushConfigSetConfigVariant.AppleConfig[configPlatform=" + configPlatform
                    + ", hasConfigVersion2=" + hasConfigVersion2
                    + ", configId=" + configId
                    + ", configVoip=" + configVoip
                    + ", configPreview=" + configPreview
                    + ", configDefault=" + configDefault
                    + ", configGroups=" + configGroups
                    + ", configCall=" + configCall
                    + ", configStatusSound=" + configStatusSound
                    + ", configLg=" + configLg
                    + ", configLc=" + configLc
                    + ", configBackgroundLocation=" + configBackgroundLocation
                    + ", configNseVer=" + configNseVer
                    + ", configNseCall=" + configNseCall
                    + ", configNseRead=" + configNseRead
                    + ", configNseRetry=" + configNseRetry
                    + ", configRegPush=" + configRegPush
                    + ", configPkey=" + configPkey
                    + ", configVoipPayloadType=" + configVoipPayloadType
                    + ", configSettings=" + configSettings
                    + ", configAppMute=" + configAppMute
                    + ", configAppleWatchId=" + configAppleWatchId
                    + ", configAppleWatchPkey=" + configAppleWatchPkey
                    + ", itemArgs=" + itemArgs + ']';
        }

        /**
         * Represents a single {@code <item jid mute? notify? call?/>} entry carried by an
         * {@link AppleConfig}.
         *
         * <p>Pairs a chat {@link Jid} with the optional per-chat mute/notify/call overrides; the
         * relay merges the entry with the global Apple defaults.
         */
        public static final class AppleItem {
            /**
             * Holds the target chat {@link Jid}.
             */
            private final Jid itemJid;

            /**
             * Holds the optional mute marker.
             */
            private final Long itemMute;

            /**
             * Holds the optional notify marker.
             */
            private final String itemNotify;

            /**
             * Holds the optional call marker.
             */
            private final String itemCall;

            /**
             * Constructs an Apple per-chat entry.
             *
             * <p>A null marker falls back to the global Apple default for the corresponding
             * category.
             *
             * @param itemJid    the chat {@link Jid}
             * @param itemMute   the optional mute marker
             * @param itemNotify the optional notify marker
             * @param itemCall   the optional call marker
             * @throws NullPointerException if {@code itemJid} is {@code null}
             */
            public AppleItem(Jid itemJid, Long itemMute, String itemNotify, String itemCall) {
                this.itemJid = Objects.requireNonNull(itemJid, "itemJid cannot be null");
                this.itemMute = itemMute;
                this.itemNotify = itemNotify;
                this.itemCall = itemCall;
            }

            /**
             * Returns the target chat {@link Jid}.
             *
             * @return the {@link Jid}
             */
            public Jid itemJid() {
                return itemJid;
            }

            /**
             * Returns the optional mute marker.
             *
             * @return an {@link Optional} carrying the marker
             */
            public Optional<Long> itemMute() {
                return Optional.ofNullable(itemMute);
            }

            /**
             * Returns the optional notify marker.
             *
             * @return an {@link Optional} carrying the marker
             */
            public Optional<String> itemNotify() {
                return Optional.ofNullable(itemNotify);
            }

            /**
             * Returns the optional call marker.
             *
             * @return an {@link Optional} carrying the marker
             */
            public Optional<String> itemCall() {
                return Optional.ofNullable(itemCall);
            }

            /**
             * Builds the {@code <item>} child stanza, emitting each optional attribute only when
             * present.
             *
             * @implNote This implementation matches the {@code makeAppleClientItem} fixture
             * verbatim and emits each optional attribute only when its backing field is non-null.
             * @return the {@link Stanza}
             */
            @WhatsAppWebExport(moduleName = "WASmaxOutPushConfigAppleClientMixin",
                    exports = "makeAppleClientItem",
                    adaptation = WhatsAppAdaptation.DIRECT)
            public Stanza toStanza() {
                var builder = new StanzaBuilder()
                        .description("item")
                        .attribute("jid", itemJid);
                if (itemMute != null) {
                    builder.attribute("mute", itemMute);
                }
                if (itemNotify != null) {
                    builder.attribute("notify", itemNotify);
                }
                if (itemCall != null) {
                    builder.attribute("call", itemCall);
                }
                return builder.build();
            }

            /**
             * Compares this entry to another object for equality on all four fields.
             *
             * @param obj the object to compare against
             * @return {@code true} when {@code obj} is an {@link AppleItem} with equal fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (AppleItem) obj;
                return Objects.equals(this.itemJid, that.itemJid)
                        && Objects.equals(this.itemMute, that.itemMute)
                        && Objects.equals(this.itemNotify, that.itemNotify)
                        && Objects.equals(this.itemCall, that.itemCall);
            }

            /**
             * Returns a hash code derived from all four fields.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(itemJid, itemMute, itemNotify, itemCall);
            }

            /**
             * Returns a debug rendering of this entry.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "SmaxPushConfigSetConfigVariant.AppleConfig.AppleItem[itemJid=" + itemJid
                        + ", itemMute=" + itemMute
                        + ", itemNotify=" + itemNotify
                        + ", itemCall=" + itemCall + ']';
            }
        }
    }

    /**
     * Represents the Windows-Notification-Service-client {@code <config platform="wns">} variant.
     *
     * <p>Registers a WNS push channel for the Windows desktop client, carrying the WNS channel id
     * and an optional version marker.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutPushConfigWNSClientMixin")
    final class WnsConfig implements SmaxPushConfigSetConfigVariant {
        /**
         * Holds the optional {@code version} attribute.
         */
        private final String configVersion;

        /**
         * Holds the mandatory {@code id} attribute carrying the WNS channel id.
         */
        private final String configId;

        /**
         * Constructs a WNS config from an optional version and the WNS channel id.
         *
         * @param configVersion the optional version
         * @param configId      the WNS channel id
         * @throws NullPointerException if {@code configId} is {@code null}
         */
        public WnsConfig(String configVersion, String configId) {
            this.configVersion = configVersion;
            this.configId = Objects.requireNonNull(configId, "configId cannot be null");
        }

        /**
         * Returns the optional {@code version} attribute.
         *
         * @return an {@link Optional} carrying the version
         */
        public Optional<String> configVersion() {
            return Optional.ofNullable(configVersion);
        }

        /**
         * Returns the WNS channel id.
         *
         * @return the id
         */
        public String configId() {
            return configId;
        }

        /**
         * Builds the {@code <config platform="wns">} stanza, emitting {@code version} only when
         * present.
         *
         * @implNote This implementation hard-codes {@code platform="wns"} per the
         * {@code mergeWNSClientMixin} fixture and emits the optional {@code version} attribute only
         * when non-null.
         * @return the {@link Stanza}
         */
        @Override
        @WhatsAppWebExport(moduleName = "WASmaxOutPushConfigWNSClientMixin",
                exports = "mergeWNSClientMixin",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Stanza toStanza() {
            var builder = new StanzaBuilder()
                    .description("config")
                    .attribute("platform", "wns")
                    .attribute("id", configId);
            if (configVersion != null) {
                builder.attribute("version", configVersion);
            }
            return builder.build();
        }

        /**
         * Compares this config to another object for equality on its two fields.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link WnsConfig} with equal fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (WnsConfig) obj;
            return Objects.equals(this.configVersion, that.configVersion)
                    && Objects.equals(this.configId, that.configId);
        }

        /**
         * Returns a hash code derived from the two carried fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(configVersion, configId);
        }

        /**
         * Returns a debug rendering of this config.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxPushConfigSetConfigVariant.WnsConfig[configVersion=" + configVersion
                    + ", configId=" + configId + ']';
        }
    }

    /**
     * Represents the Enterprise-client {@code <config platform="ent">} variant.
     *
     * <p>Registers an enterprise-deployment push channel, carrying only the enterprise id.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutPushConfigEnterpriseClientMixin")
    final class EnterpriseConfig implements SmaxPushConfigSetConfigVariant {
        /**
         * Holds the mandatory {@code id} attribute carrying the enterprise id.
         */
        private final String configId;

        /**
         * Constructs an enterprise-client config from the enterprise id.
         *
         * @param configId the enterprise id
         * @throws NullPointerException if {@code configId} is {@code null}
         */
        public EnterpriseConfig(String configId) {
            this.configId = Objects.requireNonNull(configId, "configId cannot be null");
        }

        /**
         * Returns the enterprise id.
         *
         * @return the id
         */
        public String configId() {
            return configId;
        }

        /**
         * Builds the {@code <config platform="ent">} stanza.
         *
         * @implNote This implementation hard-codes {@code platform="ent"} per the
         * {@code mergeEnterpriseClientMixin} fixture.
         * @return the {@link Stanza}
         */
        @Override
        @WhatsAppWebExport(moduleName = "WASmaxOutPushConfigEnterpriseClientMixin",
                exports = "mergeEnterpriseClientMixin",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Stanza toStanza() {
            return new StanzaBuilder()
                    .description("config")
                    .attribute("platform", "ent")
                    .attribute("id", configId)
                    .build();
        }

        /**
         * Compares this config to another object for equality on the enterprise id.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an {@link EnterpriseConfig} with an equal id
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (EnterpriseConfig) obj;
            return Objects.equals(this.configId, that.configId);
        }

        /**
         * Returns a hash code derived from the enterprise id.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(configId);
        }

        /**
         * Returns a debug rendering of this config.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxPushConfigSetConfigVariant.EnterpriseConfig[configId=" + configId + ']';
        }
    }

    /**
     * Represents the Web-client {@code <config platform="web">} variant.
     *
     * <p>Registers a W3C Push API channel for the Web/Desktop client, carrying the
     * {@code PushSubscription.endpoint} URL plus the base64-encoded {@code auth} secret and
     * {@code p256dh} application-server key returned by {@code PushManager.subscribe}, alongside
     * optional language and locale tags. Callers subscribing to a custom push service populate the
     * endpoint, auth, and key fields from their {@code PushSubscription}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutPushConfigWebClientMixin")
    final class WebConfig implements SmaxPushConfigSetConfigVariant {
        /**
         * Holds the W3C Push API endpoint URL.
         */
        private final String configEndpoint;

        /**
         * Holds the base64-encoded Push API auth secret.
         */
        private final String configAuth;

        /**
         * Holds the base64-encoded P-256 application-server public key.
         */
        private final String configP256dh;

        /**
         * Holds the optional language tag.
         */
        private final String configLg;

        /**
         * Holds the optional locale tag.
         */
        private final String configLc;

        /**
         * Constructs a Web Push config from the subscription fields and optional locale tags.
         *
         * <p>Populate {@code configEndpoint}, {@code configAuth}, and {@code configP256dh} from the
         * {@code PushSubscription} returned by the W3C Push API; the language and locale tags are
         * forwarded verbatim to the relay for notification localisation.
         *
         * @param configEndpoint the endpoint URL
         * @param configAuth     the auth secret
         * @param configP256dh   the P-256 key
         * @param configLg       the optional language tag
         * @param configLc       the optional locale tag
         * @throws NullPointerException if {@code configEndpoint}, {@code configAuth}, or
         *                              {@code configP256dh} is {@code null}
         */
        public WebConfig(String configEndpoint, String configAuth, String configP256dh,
                         String configLg, String configLc) {
            this.configEndpoint = Objects.requireNonNull(configEndpoint, "configEndpoint cannot be null");
            this.configAuth = Objects.requireNonNull(configAuth, "configAuth cannot be null");
            this.configP256dh = Objects.requireNonNull(configP256dh, "configP256dh cannot be null");
            this.configLg = configLg;
            this.configLc = configLc;
        }

        /**
         * Returns the endpoint URL.
         *
         * @return the URL
         */
        public String configEndpoint() {
            return configEndpoint;
        }

        /**
         * Returns the auth secret.
         *
         * @return the secret
         */
        public String configAuth() {
            return configAuth;
        }

        /**
         * Returns the P-256 key.
         *
         * @return the key
         */
        public String configP256dh() {
            return configP256dh;
        }

        /**
         * Returns the optional language tag.
         *
         * @return an {@link Optional} carrying the tag
         */
        public Optional<String> configLg() {
            return Optional.ofNullable(configLg);
        }

        /**
         * Returns the optional locale tag.
         *
         * @return an {@link Optional} carrying the tag
         */
        public Optional<String> configLc() {
            return Optional.ofNullable(configLc);
        }

        /**
         * Builds the {@code <config platform="web">} stanza, emitting {@code lg} and {@code lc} only
         * when present.
         *
         * @implNote This implementation hard-codes {@code platform="web"} per the
         * {@code mergeWebClientMixin} fixture and emits the optional {@code lg} and {@code lc}
         * attributes only when non-null.
         * @return the {@link Stanza}
         */
        @Override
        @WhatsAppWebExport(moduleName = "WASmaxOutPushConfigWebClientMixin",
                exports = "mergeWebClientMixin",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Stanza toStanza() {
            var builder = new StanzaBuilder()
                    .description("config")
                    .attribute("platform", "web")
                    .attribute("endpoint", configEndpoint)
                    .attribute("auth", configAuth)
                    .attribute("p256dh", configP256dh);
            if (configLg != null) {
                builder.attribute("lg", configLg);
            }
            if (configLc != null) {
                builder.attribute("lc", configLc);
            }
            return builder.build();
        }

        /**
         * Compares this config to another object for equality on every attribute.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link WebConfig} with equal attributes
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (WebConfig) obj;
            return Objects.equals(this.configEndpoint, that.configEndpoint)
                    && Objects.equals(this.configAuth, that.configAuth)
                    && Objects.equals(this.configP256dh, that.configP256dh)
                    && Objects.equals(this.configLg, that.configLg)
                    && Objects.equals(this.configLc, that.configLc);
        }

        /**
         * Returns a hash code derived from every carried attribute.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(configEndpoint, configAuth, configP256dh, configLg, configLc);
        }

        /**
         * Returns a debug rendering of this config.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxPushConfigSetConfigVariant.WebConfig[configEndpoint=" + configEndpoint
                    + ", configAuth=" + configAuth
                    + ", configP256dh=" + configP256dh
                    + ", configLg=" + configLg
                    + ", configLc=" + configLc + ']';
        }
    }
}
