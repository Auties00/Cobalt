package com.github.auties00.cobalt.wire.linked.setting.push;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries the push-configuration payload registered with the relay.
 *
 * <p>WhatsApp's push-config protocol carries six platform-specific
 * config arms (Facebook, Apple/APNs, Android/FCM, Windows/WNS,
 * Enterprise, and Web) plus a {@link Clear} variant that drops the
 * push registration entirely. This sealed family flattens the wire
 * shape into a caller-friendly model: each arm carries only the
 * fields the caller actually needs to populate, with the wire-format
 * boilerplate (the {@code <config platform>} envelope, attribute
 * naming) hidden behind {@code LinkedWhatsAppClient#editPushConfig} which
 * translates this model into the corresponding wire stanza.
 *
 * <p>Pattern match on the variant to construct the right config; the
 * sealed hierarchy guarantees the {@code switch} is exhaustive.
 */
public sealed interface PushConfig
        permits PushConfig.Fcm, PushConfig.Apns, PushConfig.Web,
        PushConfig.Windows, PushConfig.Enterprise, PushConfig.Facebook,
        PushConfig.Clear {

    /**
     * The Android-FCM (Firebase Cloud Messaging) push-config variant.
     *
     * <p>Carries the per-group mute items that the FCM channel
     * surfaces server-side to suppress wakeup pings for muted groups.
     */
    final class Fcm implements PushConfig {
        /**
         * The per-group mute items.
         */
        private final List<MuteItem> muteItems;

        /**
         * Constructs a new FCM config.
         *
         * @param muteItems the mute items; never {@code null}
         * @throws NullPointerException if {@code muteItems} is
         *                              {@code null}
         */
        public Fcm(List<MuteItem> muteItems) {
            Objects.requireNonNull(muteItems, "muteItems cannot be null");
            this.muteItems = List.copyOf(muteItems);
        }

        /**
         * Returns the per-group mute items.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<MuteItem> muteItems() {
            return muteItems;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Fcm that && Objects.equals(muteItems, that.muteItems);
        }

        @Override
        public int hashCode() {
            return Objects.hash(muteItems);
        }

        @Override
        public String toString() {
            return "PushConfig.Fcm[muteItems=" + muteItems + ']';
        }

        /**
         * A single per-group mute item carried by an {@link Fcm}
         * config.
         */
        public static final class MuteItem {
            /**
             * The group JID being muted.
             */
            private final Jid groupJid;

            /**
             * The numeric mute marker.
             */
            private final long muteMarker;

            /**
             * Constructs a new mute item.
             *
             * @param groupJid   the group JID; never {@code null}
             * @param muteMarker the mute marker
             * @throws NullPointerException if {@code groupJid} is
             *                              {@code null}
             */
            public MuteItem(Jid groupJid, long muteMarker) {
                this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
                this.muteMarker = muteMarker;
            }

            /**
             * Returns the group JID.
             *
             * @return the JID; never {@code null}
             */
            public Jid groupJid() {
                return groupJid;
            }

            /**
             * Returns the mute marker.
             *
             * @return the marker
             */
            public long muteMarker() {
                return muteMarker;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof MuteItem that
                        && muteMarker == that.muteMarker
                        && Objects.equals(groupJid, that.groupJid);
            }

            @Override
            public int hashCode() {
                return Objects.hash(groupJid, muteMarker);
            }

            @Override
            public String toString() {
                return "PushConfig.Fcm.MuteItem[groupJid=" + groupJid
                        + ", muteMarker=" + muteMarker + ']';
            }
        }
    }

    /**
     * The Apple-APNs (iOS / iPadOS / macOS Catalyst) push-config
     * variant.
     *
     * <p>Carries the rich set of APNs-specific attributes the relay
     * stores to drive notification rendering, sound selection,
     * Notification Service Extension routing and Apple Watch
     * companion delivery. Most attributes are optional; only the
     * platform marker, the four notification toggles
     * ({@code preview}, {@code default}, {@code groups},
     * {@code call}), the language/locale and the voip payload type
     * are always required by the wire schema.
     */
    final class Apns implements PushConfig {
        private final String platform;
        private final boolean version2;
        private final String id;
        private final String voipToken;
        private final String previewToggle;
        private final String defaultToggle;
        private final String groupsToggle;
        private final String callToggle;
        private final String statusSoundToggle;
        private final String language;
        private final String locale;
        private final String backgroundLocationToggle;
        private final String nseVersion;
        private final String nseCallToggle;
        private final String nseReadToggle;
        private final String nseRetryToggle;
        private final String regPushToggle;
        private final String pkey;
        private final String voipPayloadType;
        private final Long settingsMask;
        private final Long appMuteMask;
        private final String appleWatchId;
        private final String appleWatchPkey;
        private final List<AppleItem> items;

        /**
         * Constructs a new APNs config.
         *
         * @param platform                 the platform marker (e.g.
         *                                 {@code "iphone"},
         *                                 {@code "ipad"}); never
         *                                 {@code null}
         * @param version2                 whether the version-2 marker
         *                                 is set
         * @param id                       the optional id; may be
         *                                 {@code null}
         * @param voipToken                the optional voip token;
         *                                 may be {@code null}
         * @param previewToggle            the preview toggle; never
         *                                 {@code null}
         * @param defaultToggle            the default toggle; never
         *                                 {@code null}
         * @param groupsToggle             the groups toggle; never
         *                                 {@code null}
         * @param callToggle               the call toggle; never
         *                                 {@code null}
         * @param statusSoundToggle        the optional status-sound
         *                                 toggle; may be {@code null}
         * @param language                 the language tag; never
         *                                 {@code null}
         * @param locale                   the locale tag; never
         *                                 {@code null}
         * @param backgroundLocationToggle the optional background-
         *                                 location toggle; may be
         *                                 {@code null}
         * @param nseVersion               the optional NSE version;
         *                                 may be {@code null}
         * @param nseCallToggle            the optional NSE-call
         *                                 toggle; may be {@code null}
         * @param nseReadToggle            the optional NSE-read
         *                                 toggle; may be {@code null}
         * @param nseRetryToggle           the optional NSE-retry
         *                                 toggle; may be {@code null}
         * @param regPushToggle            the optional reg-push
         *                                 toggle; may be {@code null}
         * @param pkey                     the optional pkey; may be
         *                                 {@code null}
         * @param voipPayloadType          the voip payload type;
         *                                 never {@code null}
         * @param settingsMask             the optional settings mask;
         *                                 may be {@code null}
         * @param appMuteMask              the optional app-mute mask;
         *                                 may be {@code null}
         * @param appleWatchId             the optional Apple Watch id
         * @param appleWatchPkey           the optional Apple Watch
         *                                 pkey
         * @param items                    the per-item entries; never
         *                                 {@code null}
         * @throws NullPointerException if any required argument is
         *                              {@code null}
         */
        public Apns(String platform, boolean version2,
                    String id, String voipToken,
                    String previewToggle, String defaultToggle, String groupsToggle,
                    String callToggle, String statusSoundToggle,
                    String language, String locale, String backgroundLocationToggle,
                    String nseVersion, String nseCallToggle, String nseReadToggle,
                    String nseRetryToggle, String regPushToggle, String pkey,
                    String voipPayloadType, Long settingsMask, Long appMuteMask,
                    String appleWatchId, String appleWatchPkey, List<AppleItem> items) {
            this.platform = Objects.requireNonNull(platform, "platform cannot be null");
            this.version2 = version2;
            this.id = id;
            this.voipToken = voipToken;
            this.previewToggle = Objects.requireNonNull(previewToggle, "previewToggle cannot be null");
            this.defaultToggle = Objects.requireNonNull(defaultToggle, "defaultToggle cannot be null");
            this.groupsToggle = Objects.requireNonNull(groupsToggle, "groupsToggle cannot be null");
            this.callToggle = Objects.requireNonNull(callToggle, "callToggle cannot be null");
            this.statusSoundToggle = statusSoundToggle;
            this.language = Objects.requireNonNull(language, "language cannot be null");
            this.locale = Objects.requireNonNull(locale, "locale cannot be null");
            this.backgroundLocationToggle = backgroundLocationToggle;
            this.nseVersion = nseVersion;
            this.nseCallToggle = nseCallToggle;
            this.nseReadToggle = nseReadToggle;
            this.nseRetryToggle = nseRetryToggle;
            this.regPushToggle = regPushToggle;
            this.pkey = pkey;
            this.voipPayloadType = Objects.requireNonNull(voipPayloadType, "voipPayloadType cannot be null");
            this.settingsMask = settingsMask;
            this.appMuteMask = appMuteMask;
            this.appleWatchId = appleWatchId;
            this.appleWatchPkey = appleWatchPkey;
            Objects.requireNonNull(items, "items cannot be null");
            this.items = List.copyOf(items);
        }

        /**
         * Returns the platform marker.
         *
         * @return the marker; never {@code null}
         */
        public String platform() {
            return platform;
        }

        /**
         * Returns whether the version-2 marker is set.
         *
         * @return {@code true} when set
         */
        public boolean version2() {
            return version2;
        }

        /**
         * Returns the optional id.
         *
         * @return an {@link Optional} carrying the id
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the optional voip token.
         *
         * @return an {@link Optional} carrying the token
         */
        public Optional<String> voipToken() {
            return Optional.ofNullable(voipToken);
        }

        /**
         * Returns the preview toggle.
         *
         * @return the toggle; never {@code null}
         */
        public String previewToggle() {
            return previewToggle;
        }

        /**
         * Returns the default toggle.
         *
         * @return the toggle; never {@code null}
         */
        public String defaultToggle() {
            return defaultToggle;
        }

        /**
         * Returns the groups toggle.
         *
         * @return the toggle; never {@code null}
         */
        public String groupsToggle() {
            return groupsToggle;
        }

        /**
         * Returns the call toggle.
         *
         * @return the toggle; never {@code null}
         */
        public String callToggle() {
            return callToggle;
        }

        /**
         * Returns the optional status-sound toggle.
         *
         * @return an {@link Optional} carrying the toggle
         */
        public Optional<String> statusSoundToggle() {
            return Optional.ofNullable(statusSoundToggle);
        }

        /**
         * Returns the language tag.
         *
         * @return the tag; never {@code null}
         */
        public String language() {
            return language;
        }

        /**
         * Returns the locale tag.
         *
         * @return the tag; never {@code null}
         */
        public String locale() {
            return locale;
        }

        /**
         * Returns the optional background-location toggle.
         *
         * @return an {@link Optional} carrying the toggle
         */
        public Optional<String> backgroundLocationToggle() {
            return Optional.ofNullable(backgroundLocationToggle);
        }

        /**
         * Returns the optional NSE version.
         *
         * @return an {@link Optional} carrying the version
         */
        public Optional<String> nseVersion() {
            return Optional.ofNullable(nseVersion);
        }

        /**
         * Returns the optional NSE-call toggle.
         *
         * @return an {@link Optional} carrying the toggle
         */
        public Optional<String> nseCallToggle() {
            return Optional.ofNullable(nseCallToggle);
        }

        /**
         * Returns the optional NSE-read toggle.
         *
         * @return an {@link Optional} carrying the toggle
         */
        public Optional<String> nseReadToggle() {
            return Optional.ofNullable(nseReadToggle);
        }

        /**
         * Returns the optional NSE-retry toggle.
         *
         * @return an {@link Optional} carrying the toggle
         */
        public Optional<String> nseRetryToggle() {
            return Optional.ofNullable(nseRetryToggle);
        }

        /**
         * Returns the optional reg-push toggle.
         *
         * @return an {@link Optional} carrying the toggle
         */
        public Optional<String> regPushToggle() {
            return Optional.ofNullable(regPushToggle);
        }

        /**
         * Returns the optional pkey.
         *
         * @return an {@link Optional} carrying the pkey
         */
        public Optional<String> pkey() {
            return Optional.ofNullable(pkey);
        }

        /**
         * Returns the voip payload type.
         *
         * @return the type; never {@code null}
         */
        public String voipPayloadType() {
            return voipPayloadType;
        }

        /**
         * Returns the optional settings mask.
         *
         * @return an {@link Optional} carrying the mask
         */
        public Optional<Long> settingsMask() {
            return Optional.ofNullable(settingsMask);
        }

        /**
         * Returns the optional app-mute mask.
         *
         * @return an {@link Optional} carrying the mask
         */
        public Optional<Long> appMuteMask() {
            return Optional.ofNullable(appMuteMask);
        }

        /**
         * Returns the optional Apple Watch id.
         *
         * @return an {@link Optional} carrying the id
         */
        public Optional<String> appleWatchId() {
            return Optional.ofNullable(appleWatchId);
        }

        /**
         * Returns the optional Apple Watch pkey.
         *
         * @return an {@link Optional} carrying the pkey
         */
        public Optional<String> appleWatchPkey() {
            return Optional.ofNullable(appleWatchPkey);
        }

        /**
         * Returns the per-item entries.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<AppleItem> items() {
            return items;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Apns that)) {
                return false;
            }
            return version2 == that.version2
                    && Objects.equals(platform, that.platform)
                    && Objects.equals(id, that.id)
                    && Objects.equals(voipToken, that.voipToken)
                    && Objects.equals(previewToggle, that.previewToggle)
                    && Objects.equals(defaultToggle, that.defaultToggle)
                    && Objects.equals(groupsToggle, that.groupsToggle)
                    && Objects.equals(callToggle, that.callToggle)
                    && Objects.equals(statusSoundToggle, that.statusSoundToggle)
                    && Objects.equals(language, that.language)
                    && Objects.equals(locale, that.locale)
                    && Objects.equals(backgroundLocationToggle, that.backgroundLocationToggle)
                    && Objects.equals(nseVersion, that.nseVersion)
                    && Objects.equals(nseCallToggle, that.nseCallToggle)
                    && Objects.equals(nseReadToggle, that.nseReadToggle)
                    && Objects.equals(nseRetryToggle, that.nseRetryToggle)
                    && Objects.equals(regPushToggle, that.regPushToggle)
                    && Objects.equals(pkey, that.pkey)
                    && Objects.equals(voipPayloadType, that.voipPayloadType)
                    && Objects.equals(settingsMask, that.settingsMask)
                    && Objects.equals(appMuteMask, that.appMuteMask)
                    && Objects.equals(appleWatchId, that.appleWatchId)
                    && Objects.equals(appleWatchPkey, that.appleWatchPkey)
                    && Objects.equals(items, that.items);
        }

        @Override
        public int hashCode() {
            var result = Objects.hash(platform, version2, id, voipToken,
                    previewToggle, defaultToggle, groupsToggle, callToggle,
                    statusSoundToggle, language, locale, backgroundLocationToggle,
                    nseVersion, nseCallToggle, nseReadToggle, nseRetryToggle,
                    regPushToggle, pkey, voipPayloadType, settingsMask, appMuteMask,
                    appleWatchId, appleWatchPkey);
            return 31 * result + Objects.hashCode(items);
        }

        @Override
        public String toString() {
            return "PushConfig.Apns[platform=" + platform
                    + ", version2=" + version2
                    + ", id=" + id + ']';
        }

        /**
         * A single APNs per-item entry. Carries the target JID and the
         * optional per-JID mute / notify / call toggles.
         */
        public static final class AppleItem {
            /**
             * The target JID.
             */
            private final Jid jid;

            /**
             * The optional mute marker.
             */
            private final Long muteMarker;

            /**
             * The optional notify marker.
             */
            private final String notifyMarker;

            /**
             * The optional call marker.
             */
            private final String callMarker;

            /**
             * Constructs a new entry.
             *
             * @param jid          the target JID; never {@code null}
             * @param muteMarker   the optional mute marker; may be
             *                     {@code null}
             * @param notifyMarker the optional notify marker; may be
             *                     {@code null}
             * @param callMarker   the optional call marker; may be
             *                     {@code null}
             * @throws NullPointerException if {@code jid} is
             *                              {@code null}
             */
            public AppleItem(Jid jid, Long muteMarker, String notifyMarker, String callMarker) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.muteMarker = muteMarker;
                this.notifyMarker = notifyMarker;
                this.callMarker = callMarker;
            }

            /**
             * Returns the target JID.
             *
             * @return the JID; never {@code null}
             */
            public Jid jid() {
                return jid;
            }

            /**
             * Returns the optional mute marker.
             *
             * @return an {@link Optional} carrying the marker
             */
            public Optional<Long> muteMarker() {
                return Optional.ofNullable(muteMarker);
            }

            /**
             * Returns the optional notify marker.
             *
             * @return an {@link Optional} carrying the marker
             */
            public Optional<String> notifyMarker() {
                return Optional.ofNullable(notifyMarker);
            }

            /**
             * Returns the optional call marker.
             *
             * @return an {@link Optional} carrying the marker
             */
            public Optional<String> callMarker() {
                return Optional.ofNullable(callMarker);
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof AppleItem that
                        && Objects.equals(jid, that.jid)
                        && Objects.equals(muteMarker, that.muteMarker)
                        && Objects.equals(notifyMarker, that.notifyMarker)
                        && Objects.equals(callMarker, that.callMarker);
            }

            @Override
            public int hashCode() {
                return Objects.hash(jid, muteMarker, notifyMarker, callMarker);
            }

            @Override
            public String toString() {
                return "PushConfig.Apns.AppleItem[jid=" + jid
                        + ", muteMarker=" + muteMarker
                        + ", notifyMarker=" + notifyMarker
                        + ", callMarker=" + callMarker + ']';
            }
        }
    }

    /**
     * The Web Push (W3C Push API) variant. Carries the endpoint URL,
     * the auth secret and the P-256 application server key the relay
     * needs to encrypt outbound pushes for the browser.
     */
    final class Web implements PushConfig {
        /**
         * The W3C Push API endpoint URL.
         */
        private final String endpoint;

        /**
         * The Push API auth secret.
         */
        private final String auth;

        /**
         * The P-256 application-server public key.
         */
        private final String p256dh;

        /**
         * The optional language tag.
         */
        private final String language;

        /**
         * The optional locale tag.
         */
        private final String locale;

        /**
         * Constructs a new web-push config.
         *
         * @param endpoint the endpoint URL; never {@code null}
         * @param auth     the auth secret; never {@code null}
         * @param p256dh   the P-256 key; never {@code null}
         * @param language the optional language; may be {@code null}
         * @param locale   the optional locale; may be {@code null}
         * @throws NullPointerException if any required argument is
         *                              {@code null}
         */
        public Web(String endpoint, String auth, String p256dh,
                   String language, String locale) {
            this.endpoint = Objects.requireNonNull(endpoint, "endpoint cannot be null");
            this.auth = Objects.requireNonNull(auth, "auth cannot be null");
            this.p256dh = Objects.requireNonNull(p256dh, "p256dh cannot be null");
            this.language = language;
            this.locale = locale;
        }

        /**
         * Returns the endpoint URL.
         *
         * @return the URL; never {@code null}
         */
        public String endpoint() {
            return endpoint;
        }

        /**
         * Returns the auth secret.
         *
         * @return the secret; never {@code null}
         */
        public String auth() {
            return auth;
        }

        /**
         * Returns the P-256 key.
         *
         * @return the key; never {@code null}
         */
        public String p256dh() {
            return p256dh;
        }

        /**
         * Returns the optional language tag.
         *
         * @return an {@link Optional} carrying the tag
         */
        public Optional<String> language() {
            return Optional.ofNullable(language);
        }

        /**
         * Returns the optional locale tag.
         *
         * @return an {@link Optional} carrying the tag
         */
        public Optional<String> locale() {
            return Optional.ofNullable(locale);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Web that
                    && Objects.equals(endpoint, that.endpoint)
                    && Objects.equals(auth, that.auth)
                    && Objects.equals(p256dh, that.p256dh)
                    && Objects.equals(language, that.language)
                    && Objects.equals(locale, that.locale);
        }

        @Override
        public int hashCode() {
            return Objects.hash(endpoint, auth, p256dh, language, locale);
        }

        @Override
        public String toString() {
            return "PushConfig.Web[endpoint=" + endpoint
                    + ", language=" + language
                    + ", locale=" + locale + ']';
        }
    }

    /**
     * The Windows Notification Service (WNS) variant. Carries the
     * UWP-issued WNS id and the optional ring marker.
     */
    final class Windows implements PushConfig {
        /**
         * The optional ring/version attribute.
         */
        private final String ring;

        /**
         * The mandatory WNS id.
         */
        private final String wnsId;

        /**
         * Constructs a new WNS config.
         *
         * @param ring  the optional ring marker; may be {@code null}
         * @param wnsId the WNS id; never {@code null}
         * @throws NullPointerException if {@code wnsId} is
         *                              {@code null}
         */
        public Windows(String ring, String wnsId) {
            this.ring = ring;
            this.wnsId = Objects.requireNonNull(wnsId, "wnsId cannot be null");
        }

        /**
         * Returns the optional ring marker.
         *
         * @return an {@link Optional} carrying the ring
         */
        public Optional<String> ring() {
            return Optional.ofNullable(ring);
        }

        /**
         * Returns the WNS id.
         *
         * @return the id; never {@code null}
         */
        public String wnsId() {
            return wnsId;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Windows that
                    && Objects.equals(ring, that.ring)
                    && Objects.equals(wnsId, that.wnsId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ring, wnsId);
        }

        @Override
        public String toString() {
            return "PushConfig.Windows[ring=" + ring
                    + ", wnsId=" + wnsId + ']';
        }
    }

    /**
     * The Enterprise client variant. Carries only the enterprise-id
     * marker.
     */
    final class Enterprise implements PushConfig {
        /**
         * The mandatory enterprise id.
         */
        private final String enterpriseId;

        /**
         * Constructs a new enterprise config.
         *
         * @param enterpriseId the enterprise id; never {@code null}
         * @throws NullPointerException if {@code enterpriseId} is
         *                              {@code null}
         */
        public Enterprise(String enterpriseId) {
            this.enterpriseId = Objects.requireNonNull(enterpriseId, "enterpriseId cannot be null");
        }

        /**
         * Returns the enterprise id.
         *
         * @return the id; never {@code null}
         */
        public String enterpriseId() {
            return enterpriseId;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Enterprise that && Objects.equals(enterpriseId, that.enterpriseId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enterpriseId);
        }

        @Override
        public String toString() {
            return "PushConfig.Enterprise[enterpriseId=" + enterpriseId + ']';
        }
    }

    /**
     * The Facebook-client variant. Carries the FB app id, device id
     * and the optional FB user id.
     */
    final class Facebook implements PushConfig {
        /**
         * The Facebook app id.
         */
        private final String appId;

        /**
         * The device id.
         */
        private final String deviceId;

        /**
         * The optional Facebook user id.
         */
        private final String fbId;

        /**
         * Constructs a new Facebook-client config.
         *
         * @param appId    the app id; never {@code null}
         * @param deviceId the device id; never {@code null}
         * @param fbId     the optional FB id; may be {@code null}
         * @throws NullPointerException if {@code appId} or
         *                              {@code deviceId} are
         *                              {@code null}
         */
        public Facebook(String appId, String deviceId, String fbId) {
            this.appId = Objects.requireNonNull(appId, "appId cannot be null");
            this.deviceId = Objects.requireNonNull(deviceId, "deviceId cannot be null");
            this.fbId = fbId;
        }

        /**
         * Returns the app id.
         *
         * @return the app id; never {@code null}
         */
        public String appId() {
            return appId;
        }

        /**
         * Returns the device id.
         *
         * @return the device id; never {@code null}
         */
        public String deviceId() {
            return deviceId;
        }

        /**
         * Returns the optional FB id.
         *
         * @return an {@link Optional} carrying the id
         */
        public Optional<String> fbId() {
            return Optional.ofNullable(fbId);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Facebook that
                    && Objects.equals(appId, that.appId)
                    && Objects.equals(deviceId, that.deviceId)
                    && Objects.equals(fbId, that.fbId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(appId, deviceId, fbId);
        }

        @Override
        public String toString() {
            return "PushConfig.Facebook[appId=" + appId
                    + ", deviceId=" + deviceId
                    + ", fbId=" + fbId + ']';
        }
    }

    /**
     * The clear / de-register variant. Drops the device's push
     * registration so the relay stops sending wakeup pings.
     */
    final class Clear implements PushConfig {
        /**
         * The optional platform scope to clear ({@code "fb"},
         * {@code "apple"}, {@code "android"}, {@code "wns"},
         * {@code "ent"}, {@code "web"}). When {@code null}, every
         * registered platform for the device is dropped.
         */
        private final String platform;

        /**
         * Constructs a new clear config.
         *
         * @param platform the optional platform scope; may be
         *                 {@code null}
         */
        public Clear(String platform) {
            this.platform = platform;
        }

        /**
         * Returns the optional platform scope.
         *
         * @return an {@link Optional} carrying the scope
         */
        public Optional<String> platform() {
            return Optional.ofNullable(platform);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Clear that && Objects.equals(platform, that.platform);
        }

        @Override
        public int hashCode() {
            return Objects.hash(platform);
        }

        @Override
        public String toString() {
            return "PushConfig.Clear[platform=" + platform + ']';
        }
    }
}
