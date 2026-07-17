package com.github.auties00.cobalt.wire.stanza.mex.json.user;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Decodes the reply to the privacy settings fetch query.
 *
 * <p>Each {@link Item} holds the {@code privacy_settings.settings} array for one user (the array
 * carries one element for the authenticated caller); each {@link Item.PrivacySettings.Settings} entry
 * pairs a feature key (such as {@code LAST}, {@code ONLINE}, {@code PROFILE}) with its current
 * setting value (such as {@code all} or {@code contacts}). Consume this type after dispatching
 * {@link GetPrivacySettingsMexRequest}.
 *
 * @see GetPrivacySettingsMexRequest
 */
public final class GetPrivacySettingsMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the decoded {@code xwa2_fetch_wa_users} array, one entry per user.
     */
    private final List<Item> items;

    /**
     * Wraps a pre-parsed list of per-user records.
     *
     * @param items the per-user records produced by {@link #of(byte[])}
     */
    private GetPrivacySettingsMexResponse(List<Item> items) {
        this.items = items;
    }

    /**
     * Decodes the {@code <result>} child of an inbound MEX IQ.
     *
     * <p>Pass the IQ stanza received in reply to a stanza dispatched with
     * {@link GetPrivacySettingsMexRequest#toStanza()}.
     *
     * @param stanza the IQ reply stanza
     * @return the decoded reply, or {@link Optional#empty()} when the payload is missing or malformed
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGetPrivacySettingsQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<GetPrivacySettingsMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(GetPrivacySettingsMexResponse::of);
    }

    /**
     * Returns the decoded {@code xwa2_fetch_wa_users} entries.
     *
     * <p>A typical reply carries exactly one entry, the authenticated caller.
     *
     * @return the per-user records; may be empty, never {@code null}
     */
    public List<Item> items() {
        return items;
    }

    /**
     * Wraps a single decoded user record paired with the opaque relay identifier.
     *
     * <p>The {@code id} field is the relay's per-row identifier. WhatsApp Web does not display it, but
     * Cobalt preserves it so embedders mirroring the MEX tracing surface can correlate rows.
     */
    public static final class Item {
        /**
         * Holds the decoded {@code privacy_settings} sub-object, possibly {@code null}.
         */
        private final PrivacySettings privacySettings;

        /**
         * Holds the {@code id} field carrying the relay's row identifier.
         */
        private final String id;

        /**
         * Wraps the decoded fields of a user record.
         *
         * @param privacySettings the decoded {@code privacy_settings} sub-object
         * @param id the {@code id} row identifier reported by the relay
         */
        private Item(PrivacySettings privacySettings, String id) {
            this.privacySettings = privacySettings;
            this.id = id;
        }

        /**
         * Returns the decoded {@code privacy_settings} record.
         *
         * @return the record wrapped in an {@link Optional}, or {@link Optional#empty()} when the
         *         relay omitted the field
         */
        public Optional<PrivacySettings> privacySettings() {
            return Optional.ofNullable(privacySettings);
        }

        /**
         * Returns the relay-assigned row identifier.
         *
         * @return the identifier wrapped in an {@link Optional}, or {@link Optional#empty()} when the
         *         relay omitted the field
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Wraps the {@code privacy_settings} sub-object of an {@code xwa2_fetch_wa_users} entry.
         *
         * <p>Exposes the raw per-feature entries for callers to decode; WhatsApp Web instead loops
         * {@link #settings()} and dispatches on the {@code feature} key to populate a typed
         * privacy-setting object.
         */
        public static final class PrivacySettings {
            /**
             * Holds the decoded {@code settings} array of per-feature entries.
             */
            private final List<Settings> settings;

            /**
             * Wraps the decoded per-feature entries of one user record.
             *
             * @param settings the per-feature entries
             */
            private PrivacySettings(List<Settings> settings) {
                this.settings = settings;
            }

            /**
             * Returns the decoded per-feature entries.
             *
             * @return the entries; may be empty, never {@code null}
             */
            public List<Settings> settings() {
                return settings;
            }

            /**
             * Wraps a single {@code (feature, setting)} pair.
             *
             * <p>{@link #feature()} carries WA enum tokens such as {@code LAST}, {@code PROFILE},
             * {@code READRECEIPTS}; {@link #setting()} carries the policy value such as {@code all},
             * {@code contacts}, {@code none}, or the special {@code MYCONTACTSEXCEPT} and
             * {@code MYCONTACTS} tokens WhatsApp Web normalises to {@code contact_blacklist} and
             * {@code contacts}.
             */
            public static final class Settings {
                /**
                 * Holds the {@code feature} field carrying the enum token.
                 */
                private final String feature;

                /**
                 * Holds the {@code setting} field carrying the policy value.
                 */
                private final String setting;

                /**
                 * Wraps the decoded fields of one per-feature entry.
                 *
                 * @param feature the {@code feature} enum token
                 * @param setting the {@code setting} policy value
                 */
                private Settings(String feature, String setting) {
                    this.feature = feature;
                    this.setting = setting;
                }

                /**
                 * Returns the privacy-feature key.
                 *
                 * @return the feature wrapped in an {@link Optional}, or {@link Optional#empty()} when
                 *         the relay omitted the field
                 */
                public Optional<String> feature() {
                    return Optional.ofNullable(feature);
                }

                /**
                 * Returns the policy value bound to the feature.
                 *
                 * @return the setting wrapped in an {@link Optional}, or {@link Optional#empty()} when
                 *         the relay omitted the field
                 */
                public Optional<String> setting() {
                    return Optional.ofNullable(setting);
                }

                /**
                 * Decodes a single per-feature entry from a {@link JSONObject}.
                 *
                 * <p>Invoked by {@link #ofArray(JSONArray)} while walking the {@code settings} array.
                 *
                 * @param obj the JSON object to decode, possibly {@code null}
                 * @return the decoded entry, or {@link Optional#empty()} when {@code obj} is
                 *         {@code null}
                 */
                static Optional<Settings> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var feature = obj.getString("feature");
                    var setting = obj.getString("setting");
                    return Optional.of(new Settings(feature, setting));
                }

                /**
                 * Decodes the {@code settings} array of a {@link PrivacySettings}.
                 *
                 * <p>Invoked by {@link PrivacySettings#of(JSONObject)}.
                 *
                 * @param arr the JSON array to decode, possibly {@code null}
                 * @return the decoded entries in source order; empty when {@code arr} is {@code null}
                 */
                static List<Settings> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<Settings>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Decodes a {@code privacy_settings} sub-object from a {@link JSONObject}.
             *
             * <p>Invoked by {@link Item#of(JSONObject)}.
             *
             * @param obj the JSON object to decode, possibly {@code null}
             * @return the decoded record, or {@link Optional#empty()} when {@code obj} is {@code null}
             */
            static Optional<PrivacySettings> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var settings = Settings.ofArray(obj.getJSONArray("settings"));
                return Optional.of(new PrivacySettings(settings));
            }

            /**
             * Decodes a list of {@code privacy_settings} sub-objects from a {@link JSONArray}.
             *
             * <p>Provided for parity with other {@code ofArray} helpers; the response decoder does not
             * invoke it because each user record carries a single {@code privacy_settings} sub-object,
             * not an array.
             *
             * @param arr the JSON array to decode, possibly {@code null}
             * @return the decoded records in source order; empty when {@code arr} is {@code null}
             */
            static List<PrivacySettings> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<PrivacySettings>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Decodes a single user record from a {@link JSONObject}.
         *
         * <p>Invoked by {@link #ofArray(JSONArray)} while walking the {@code xwa2_fetch_wa_users}
         * array.
         *
         * @param obj the JSON object to decode, possibly {@code null}
         * @return the decoded record, or {@link Optional#empty()} when {@code obj} is {@code null}
         */
        static Optional<Item> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var privacySettings = PrivacySettings.of(obj.getJSONObject("privacy_settings")).orElse(null);
            var id = obj.getString("id");
            return Optional.of(new Item(privacySettings, id));
        }

        /**
         * Decodes the {@code xwa2_fetch_wa_users} array of the MEX payload.
         *
         * <p>Invoked by {@link GetPrivacySettingsMexResponse#of(byte[])} to project the array nested
         * under {@code data} of the {@code <result>} payload.
         *
         * @param arr the JSON array to decode, possibly {@code null}
         * @return the decoded records in source order; empty when {@code arr} is {@code null}
         */
        static List<Item> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Item>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Decodes the {@code <result>} payload bytes into a {@link GetPrivacySettingsMexResponse}.
     *
     * <p>The payload is projected from {@code data.xwa2_fetch_wa_users}. A missing {@code data}
     * envelope yields {@link Optional#empty()}.
     *
     * @param json the raw {@code <result>} payload bytes
     * @return the decoded reply, or {@link Optional#empty()} when the payload does not parse or lacks
     *         the {@code data} envelope
     */
    private static Optional<GetPrivacySettingsMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var rootArr = data.getJSONArray("xwa2_fetch_wa_users");
        var items = Item.ofArray(rootArr);

        return Optional.of(new GetPrivacySettingsMexResponse(items));
    }
}
