package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the MEX response of the fetch-newsletter-enforcements query built by
 * {@link FetchNewsletterEnforcementsMexRequest}.
 *
 * <p>Exposes the four enforcement buckets echoed under {@code xwa2_channel_enforcements}:
 * profile-picture deletions, full-account suspensions, per-message takedowns of violating messages
 * and per-country geo-suspensions. Each entry carries the policy information used to render the
 * admin moderation surface plus appeal state for those that are appealable.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterEnforcementsJob")
public final class FetchNewsletterEnforcementsMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the list of profile-picture-deletion enforcements.
     */
    private final List<ProfilePictureDeletions> profilePictureDeletions;

    /**
     * Holds the list of full-account suspension enforcements.
     */
    private final List<Suspensions> suspensions;

    /**
     * Holds the list of per-message takedown enforcements.
     */
    private final List<ViolatingMessages> violatingMessages;

    /**
     * Holds the list of geographical suspension enforcements.
     */
    private final List<Geosuspensions> geosuspensions;

    /**
     * Constructs a response wrapping the parsed enforcement buckets.
     *
     * @param profilePictureDeletions the profile-picture deletions bucket
     * @param suspensions             the full-account suspensions bucket
     * @param violatingMessages       the per-message takedowns bucket
     * @param geosuspensions          the geographical suspensions bucket
     */
    private FetchNewsletterEnforcementsMexResponse(List<ProfilePictureDeletions> profilePictureDeletions, List<Suspensions> suspensions, List<ViolatingMessages> violatingMessages, List<Geosuspensions> geosuspensions) {
        this.profilePictureDeletions = profilePictureDeletions;
        this.suspensions = suspensions;
        this.violatingMessages = violatingMessages;
        this.geosuspensions = geosuspensions;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_channel_enforcements} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<FetchNewsletterEnforcementsMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchNewsletterEnforcementsMexResponse::of);
    }

    /**
     * Returns the profile-picture-deletion enforcements.
     *
     * @return the parsed entries, empty when the relay returned none
     */
    public List<ProfilePictureDeletions> profilePictureDeletions() {
        return profilePictureDeletions;
    }

    /**
     * Returns the full-account suspension enforcements.
     *
     * @return the parsed entries, empty when the relay returned none
     */
    public List<Suspensions> suspensions() {
        return suspensions;
    }

    /**
     * Returns the per-message takedown enforcements.
     *
     * @return the parsed entries, empty when the relay returned none
     */
    public List<ViolatingMessages> violatingMessages() {
        return violatingMessages;
    }

    /**
     * Returns the geographical suspension enforcements.
     *
     * @return the parsed entries, empty when the relay returned none
     */
    public List<Geosuspensions> geosuspensions() {
        return geosuspensions;
    }

    /**
     * Wraps one entry of the {@code profile_picture_deletions} bucket.
     *
     * <p>Records a moderator-driven profile-picture removal; carries the enforcement timestamps,
     * appeal state, violation category, source, id, optional extra data and optional policy
     * information.
     */
    public static final class ProfilePictureDeletions {
        /**
         * Holds the enforcement creation epoch-second.
         */
        private final Long enforcementCreationTime;

        /**
         * Holds the appeal creation epoch-second, or {@code null} when no appeal has been filed.
         */
        private final Long appealCreationTime;

        /**
         * Holds the appeal state label.
         */
        private final String appealState;

        /**
         * Holds the violation category label.
         */
        private final String enforcementViolationCategory;

        /**
         * Holds the enforcement source label.
         */
        private final String enforcementSource;

        /**
         * Holds the enforcement identifier.
         */
        private final String enforcementId;

        /**
         * Holds the optional extra-data sub-object.
         */
        private final EnforcementExtraData enforcementExtraData;

        /**
         * Holds the optional policy-information sub-object.
         */
        private final EnforcementPolicyInformation enforcementPolicyInformation;

        /**
         * Constructs a profile-picture-deletion wrapper from the parsed sub-fields.
         *
         * @param enforcementCreationTime      the enforcement creation epoch-second
         * @param appealCreationTime           the appeal creation epoch-second, or {@code null}
         * @param appealState                  the appeal state label
         * @param enforcementViolationCategory the violation category label
         * @param enforcementSource            the enforcement source label
         * @param enforcementId                the enforcement identifier
         * @param enforcementExtraData         the extra-data sub-object
         * @param enforcementPolicyInformation the policy-information sub-object
         */
        private ProfilePictureDeletions(Long enforcementCreationTime, Long appealCreationTime, String appealState, String enforcementViolationCategory, String enforcementSource, String enforcementId, EnforcementExtraData enforcementExtraData, EnforcementPolicyInformation enforcementPolicyInformation) {
            this.enforcementCreationTime = enforcementCreationTime;
            this.appealCreationTime = appealCreationTime;
            this.appealState = appealState;
            this.enforcementViolationCategory = enforcementViolationCategory;
            this.enforcementSource = enforcementSource;
            this.enforcementId = enforcementId;
            this.enforcementExtraData = enforcementExtraData;
            this.enforcementPolicyInformation = enforcementPolicyInformation;
        }

        /**
         * Returns the enforcement creation instant.
         *
         * @return the creation instant, or empty when the relay omitted the field
         */
        public Optional<Instant> enforcementCreationTime() {
            return Optional.ofNullable(enforcementCreationTime).map(Instant::ofEpochSecond);
        }

        /**
         * Returns the appeal creation instant.
         *
         * @return the appeal instant, or empty when no appeal has been filed
         */
        public Optional<Instant> appealCreationTime() {
            return Optional.ofNullable(appealCreationTime).map(Instant::ofEpochSecond);
        }

        /**
         * Returns the appeal state label.
         *
         * @return the appeal state, or empty when the relay omitted the field
         */
        public Optional<String> appealState() {
            return Optional.ofNullable(appealState);
        }

        /**
         * Returns the violation category label.
         *
         * @return the violation category, or empty when the relay omitted the field
         */
        public Optional<String> enforcementViolationCategory() {
            return Optional.ofNullable(enforcementViolationCategory);
        }

        /**
         * Returns the enforcement source label.
         *
         * @return the enforcement source, or empty when the relay omitted the field
         */
        public Optional<String> enforcementSource() {
            return Optional.ofNullable(enforcementSource);
        }

        /**
         * Returns the enforcement identifier.
         *
         * @return the enforcement id, or empty when the relay omitted the field
         */
        public Optional<String> enforcementId() {
            return Optional.ofNullable(enforcementId);
        }

        /**
         * Returns the extra-data sub-object.
         *
         * @return the parsed {@link EnforcementExtraData}, or empty when the relay omitted the field
         */
        public Optional<EnforcementExtraData> enforcementExtraData() {
            return Optional.ofNullable(enforcementExtraData);
        }

        /**
         * Returns the policy-information sub-object.
         *
         * @return the parsed {@link EnforcementPolicyInformation}, or empty when the relay omitted
         *         the field
         */
        public Optional<EnforcementPolicyInformation> enforcementPolicyInformation() {
            return Optional.ofNullable(enforcementPolicyInformation);
        }

        /**
         * Wraps the {@code enforcement_extra_data} sub-object as projected for
         * profile-picture-deletion enforcements.
         *
         * <p>Carries the optional IP-violation report data attached to a profile-picture takedown.
         */
        public static final class EnforcementExtraData {
            /**
             * Holds the IP-violation report data sub-object.
             */
            private final IpViolationReportData ipViolationReportData;

            /**
             * Constructs an extra-data wrapper from the parsed sub-fields.
             *
             * @param ipViolationReportData the IP-violation report data sub-object
             */
            private EnforcementExtraData(IpViolationReportData ipViolationReportData) {
                this.ipViolationReportData = ipViolationReportData;
            }

            /**
             * Returns the IP-violation report data sub-object.
             *
             * @return the parsed {@link IpViolationReportData}, or empty when the relay omitted the
             *         field
             */
            public Optional<IpViolationReportData> ipViolationReportData() {
                return Optional.ofNullable(ipViolationReportData);
            }

            /**
             * Wraps the {@code ip_violation_report_data} sub-object.
             *
             * <p>Carries the Facebook report id, the appeal-form URL the admin can use to contest
             * the takedown, and the reporter's contact data when the takedown was filed against an
             * IP holder.
             */
            public static final class IpViolationReportData {
                /**
                 * Holds the Facebook report identifier.
                 */
                private final String reportFbid;

                /**
                 * Holds the appeal-form URL.
                 */
                private final String appealFormUrl;

                /**
                 * Holds the reporter contact email.
                 */
                private final String reporterEmail;

                /**
                 * Holds the reporter contact name.
                 */
                private final String reporterName;

                /**
                 * Constructs an IP-violation-report-data wrapper from the parsed sub-fields.
                 *
                 * @param reportFbid    the Facebook report identifier
                 * @param appealFormUrl the appeal-form URL
                 * @param reporterEmail the reporter contact email
                 * @param reporterName  the reporter contact name
                 */
                private IpViolationReportData(String reportFbid, String appealFormUrl, String reporterEmail, String reporterName) {
                    this.reportFbid = reportFbid;
                    this.appealFormUrl = appealFormUrl;
                    this.reporterEmail = reporterEmail;
                    this.reporterName = reporterName;
                }

                /**
                 * Returns the Facebook report identifier.
                 *
                 * @return the report id, or empty when the relay omitted the field
                 */
                public Optional<String> reportFbid() {
                    return Optional.ofNullable(reportFbid);
                }

                /**
                 * Returns the appeal-form URL.
                 *
                 * @return the URL, or empty when the relay omitted the field
                 */
                public Optional<String> appealFormUrl() {
                    return Optional.ofNullable(appealFormUrl);
                }

                /**
                 * Returns the reporter contact email.
                 *
                 * @return the email, or empty when the relay omitted the field
                 */
                public Optional<String> reporterEmail() {
                    return Optional.ofNullable(reporterEmail);
                }

                /**
                 * Returns the reporter contact name.
                 *
                 * @return the name, or empty when the relay omitted the field
                 */
                public Optional<String> reporterName() {
                    return Optional.ofNullable(reporterName);
                }

                /**
                 * Parses an {@link IpViolationReportData} from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed entry, or empty when {@code obj} is {@code null}
                 */
                static Optional<IpViolationReportData> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var reportFbid = obj.getString("report_fbid");
                    var appealFormUrl = obj.getString("appeal_form_url");
                    var reporterEmail = obj.getString("reporter_email");
                    var reporterName = obj.getString("reporter_name");
                    return Optional.of(new IpViolationReportData(reportFbid, appealFormUrl, reporterEmail, reporterName));
                }

                /**
                 * Parses a list of {@link IpViolationReportData} entries from the given JSON array.
                 *
                 * @param arr the JSON array to parse
                 * @return the parsed list, empty when {@code arr} is {@code null}
                 */
                static List<IpViolationReportData> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<IpViolationReportData>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Parses an {@link EnforcementExtraData} from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed entry, or empty when {@code obj} is {@code null}
             */
            static Optional<EnforcementExtraData> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var ipViolationReportData = IpViolationReportData.of(obj.getJSONObject("ip_violation_report_data")).orElse(null);
                return Optional.of(new EnforcementExtraData(ipViolationReportData));
            }

            /**
             * Parses a list of {@link EnforcementExtraData} entries from the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<EnforcementExtraData> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<EnforcementExtraData>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Wraps the {@code enforcement_policy_information} sub-object.
         *
         * <p>Carries the human-readable policy text surfaced to the admin in the enforcement
         * explanation: a one-line overview, headline, subtitle, full explanation body, and an
         * admin-only disclaimer.
         */
        public static final class EnforcementPolicyInformation {
            /**
             * Holds the policy overview text.
             */
            private final String overview;

            /**
             * Holds the policy headline text.
             */
            private final String headline;

            /**
             * Holds the policy subtitle text.
             */
            private final String subtitle;

            /**
             * Holds the policy explanation body.
             */
            private final String explanation;

            /**
             * Holds the admin-only disclaimer.
             */
            private final String adminDisclaimer;

            /**
             * Constructs a policy-information wrapper from the parsed sub-fields.
             *
             * @param overview        the policy overview text
             * @param headline        the policy headline text
             * @param subtitle        the policy subtitle text
             * @param explanation     the policy explanation body
             * @param adminDisclaimer the admin-only disclaimer
             */
            private EnforcementPolicyInformation(String overview, String headline, String subtitle, String explanation, String adminDisclaimer) {
                this.overview = overview;
                this.headline = headline;
                this.subtitle = subtitle;
                this.explanation = explanation;
                this.adminDisclaimer = adminDisclaimer;
            }

            /**
             * Returns the policy overview text.
             *
             * @return the overview, or empty when the relay omitted the field
             */
            public Optional<String> overview() {
                return Optional.ofNullable(overview);
            }

            /**
             * Returns the policy headline text.
             *
             * @return the headline, or empty when the relay omitted the field
             */
            public Optional<String> headline() {
                return Optional.ofNullable(headline);
            }

            /**
             * Returns the policy subtitle text.
             *
             * @return the subtitle, or empty when the relay omitted the field
             */
            public Optional<String> subtitle() {
                return Optional.ofNullable(subtitle);
            }

            /**
             * Returns the policy explanation body.
             *
             * @return the explanation, or empty when the relay omitted the field
             */
            public Optional<String> explanation() {
                return Optional.ofNullable(explanation);
            }

            /**
             * Returns the admin-only disclaimer.
             *
             * @return the disclaimer, or empty when the relay omitted the field
             */
            public Optional<String> adminDisclaimer() {
                return Optional.ofNullable(adminDisclaimer);
            }

            /**
             * Parses an {@link EnforcementPolicyInformation} from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed entry, or empty when {@code obj} is {@code null}
             */
            static Optional<EnforcementPolicyInformation> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var overview = obj.getString("overview");
                var headline = obj.getString("headline");
                var subtitle = obj.getString("subtitle");
                var explanation = obj.getString("explanation");
                var adminDisclaimer = obj.getString("admin_disclaimer");
                return Optional.of(new EnforcementPolicyInformation(overview, headline, subtitle, explanation, adminDisclaimer));
            }

            /**
             * Parses a list of {@link EnforcementPolicyInformation} entries from the given JSON
             * array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<EnforcementPolicyInformation> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<EnforcementPolicyInformation>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a {@link ProfilePictureDeletions} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed entry, or empty when {@code obj} is {@code null}
         */
        static Optional<ProfilePictureDeletions> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var enforcementCreationTime = obj.getLong("enforcement_creation_time");
            var appealCreationTime = obj.getLong("appeal_creation_time");
            var appealState = obj.getString("appeal_state");
            var enforcementViolationCategory = obj.getString("enforcement_violation_category");
            var enforcementSource = obj.getString("enforcement_source");
            var enforcementId = obj.getString("enforcement_id");
            var enforcementExtraData = EnforcementExtraData.of(obj.getJSONObject("enforcement_extra_data")).orElse(null);
            var enforcementPolicyInformation = EnforcementPolicyInformation.of(obj.getJSONObject("enforcement_policy_information")).orElse(null);
            return Optional.of(new ProfilePictureDeletions(enforcementCreationTime, appealCreationTime, appealState, enforcementViolationCategory, enforcementSource, enforcementId, enforcementExtraData, enforcementPolicyInformation));
        }

        /**
         * Parses a list of {@link ProfilePictureDeletions} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<ProfilePictureDeletions> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<ProfilePictureDeletions>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Wraps one entry of the {@code suspensions} bucket.
     *
     * <p>Records a full-account suspension applied to the newsletter; carries the enforcement
     * timestamps, appeal state, violation category, id, source, optional extra-data with the message
     * that triggered the action, and optional policy-information body.
     */
    public static final class Suspensions {
        /**
         * Holds the appeal creation epoch-second, or {@code null} when no appeal has been filed.
         */
        private final Long appealCreationTime;

        /**
         * Holds the enforcement creation epoch-second.
         */
        private final Long enforcementCreationTime;

        /**
         * Holds the appeal state label.
         */
        private final String appealState;

        /**
         * Holds the violation category label.
         */
        private final String enforcementViolationCategory;

        /**
         * Holds the enforcement identifier.
         */
        private final String enforcementId;

        /**
         * Holds the enforcement source label.
         */
        private final String enforcementSource;

        /**
         * Holds the optional extra-data sub-object.
         */
        private final EnforcementExtraData enforcementExtraData;

        /**
         * Holds the optional policy-information sub-object.
         */
        private final EnforcementPolicyInformation enforcementPolicyInformation;

        /**
         * Constructs a suspension wrapper from the parsed sub-fields.
         *
         * @param appealCreationTime           the appeal creation epoch-second, or {@code null}
         * @param enforcementCreationTime      the enforcement creation epoch-second
         * @param appealState                  the appeal state label
         * @param enforcementViolationCategory the violation category label
         * @param enforcementId                the enforcement identifier
         * @param enforcementSource            the enforcement source label
         * @param enforcementExtraData         the extra-data sub-object
         * @param enforcementPolicyInformation the policy-information sub-object
         */
        private Suspensions(Long appealCreationTime, Long enforcementCreationTime, String appealState, String enforcementViolationCategory, String enforcementId, String enforcementSource, EnforcementExtraData enforcementExtraData, EnforcementPolicyInformation enforcementPolicyInformation) {
            this.appealCreationTime = appealCreationTime;
            this.enforcementCreationTime = enforcementCreationTime;
            this.appealState = appealState;
            this.enforcementViolationCategory = enforcementViolationCategory;
            this.enforcementId = enforcementId;
            this.enforcementSource = enforcementSource;
            this.enforcementExtraData = enforcementExtraData;
            this.enforcementPolicyInformation = enforcementPolicyInformation;
        }

        /**
         * Returns the appeal creation instant.
         *
         * @return the appeal instant, or empty when no appeal has been filed
         */
        public Optional<Instant> appealCreationTime() {
            return Optional.ofNullable(appealCreationTime).map(Instant::ofEpochSecond);
        }

        /**
         * Returns the enforcement creation instant.
         *
         * @return the creation instant, or empty when the relay omitted the field
         */
        public Optional<Instant> enforcementCreationTime() {
            return Optional.ofNullable(enforcementCreationTime).map(Instant::ofEpochSecond);
        }

        /**
         * Returns the appeal state label.
         *
         * @return the appeal state, or empty when the relay omitted the field
         */
        public Optional<String> appealState() {
            return Optional.ofNullable(appealState);
        }

        /**
         * Returns the violation category label.
         *
         * @return the violation category, or empty when the relay omitted the field
         */
        public Optional<String> enforcementViolationCategory() {
            return Optional.ofNullable(enforcementViolationCategory);
        }

        /**
         * Returns the enforcement identifier.
         *
         * @return the enforcement id, or empty when the relay omitted the field
         */
        public Optional<String> enforcementId() {
            return Optional.ofNullable(enforcementId);
        }

        /**
         * Returns the enforcement source label.
         *
         * @return the enforcement source, or empty when the relay omitted the field
         */
        public Optional<String> enforcementSource() {
            return Optional.ofNullable(enforcementSource);
        }

        /**
         * Returns the extra-data sub-object.
         *
         * @return the parsed {@link EnforcementExtraData}, or empty when the relay omitted the field
         */
        public Optional<EnforcementExtraData> enforcementExtraData() {
            return Optional.ofNullable(enforcementExtraData);
        }

        /**
         * Returns the policy-information sub-object.
         *
         * @return the parsed {@link EnforcementPolicyInformation}, or empty when the relay omitted
         *         the field
         */
        public Optional<EnforcementPolicyInformation> enforcementPolicyInformation() {
            return Optional.ofNullable(enforcementPolicyInformation);
        }

        /**
         * Wraps the {@code enforcement_extra_data} sub-object as projected for suspension
         * enforcements.
         *
         * <p>Suspension extra data adds an {@link EnforcementTargetData} pointer to the triggering
         * message and an {@link AppealExtraData} sub-object carrying the appeal-form URL, on top of
         * the IP-violation reporter data.
         */
        public static final class EnforcementExtraData {
            /**
             * Holds the IP-violation report data sub-object.
             */
            private final IpViolationReportData ipViolationReportData;

            /**
             * Holds the target-message pointer sub-object.
             */
            private final EnforcementTargetData enforcementTargetData;

            /**
             * Holds the appeal-form-URL sub-object.
             */
            private final AppealExtraData appealExtraData;

            /**
             * Constructs an extra-data wrapper from the parsed sub-fields.
             *
             * @param ipViolationReportData the IP-violation report data sub-object
             * @param enforcementTargetData the target-message pointer sub-object
             * @param appealExtraData       the appeal-form-URL sub-object
             */
            private EnforcementExtraData(IpViolationReportData ipViolationReportData, EnforcementTargetData enforcementTargetData, AppealExtraData appealExtraData) {
                this.ipViolationReportData = ipViolationReportData;
                this.enforcementTargetData = enforcementTargetData;
                this.appealExtraData = appealExtraData;
            }

            /**
             * Returns the IP-violation report data sub-object.
             *
             * @return the parsed {@link IpViolationReportData}, or empty when the relay omitted the
             *         field
             */
            public Optional<IpViolationReportData> ipViolationReportData() {
                return Optional.ofNullable(ipViolationReportData);
            }

            /**
             * Returns the target-message pointer sub-object.
             *
             * @return the parsed {@link EnforcementTargetData}, or empty when the relay omitted the
             *         field
             */
            public Optional<EnforcementTargetData> enforcementTargetData() {
                return Optional.ofNullable(enforcementTargetData);
            }

            /**
             * Returns the appeal-form-URL sub-object.
             *
             * @return the parsed {@link AppealExtraData}, or empty when the relay omitted the field
             */
            public Optional<AppealExtraData> appealExtraData() {
                return Optional.ofNullable(appealExtraData);
            }

            /**
             * Wraps the {@code ip_violation_report_data} sub-object.
             *
             * <p>Shares the same shape as
             * {@link ProfilePictureDeletions.EnforcementExtraData.IpViolationReportData}.
             */
            public static final class IpViolationReportData {
                /**
                 * Holds the Facebook report identifier.
                 */
                private final String reportFbid;

                /**
                 * Holds the appeal-form URL.
                 */
                private final String appealFormUrl;

                /**
                 * Holds the reporter contact email.
                 */
                private final String reporterEmail;

                /**
                 * Holds the reporter contact name.
                 */
                private final String reporterName;

                /**
                 * Constructs an IP-violation-report-data wrapper from the parsed sub-fields.
                 *
                 * @param reportFbid    the Facebook report identifier
                 * @param appealFormUrl the appeal-form URL
                 * @param reporterEmail the reporter contact email
                 * @param reporterName  the reporter contact name
                 */
                private IpViolationReportData(String reportFbid, String appealFormUrl, String reporterEmail, String reporterName) {
                    this.reportFbid = reportFbid;
                    this.appealFormUrl = appealFormUrl;
                    this.reporterEmail = reporterEmail;
                    this.reporterName = reporterName;
                }

                /**
                 * Returns the Facebook report identifier.
                 *
                 * @return the report id, or empty when the relay omitted the field
                 */
                public Optional<String> reportFbid() {
                    return Optional.ofNullable(reportFbid);
                }

                /**
                 * Returns the appeal-form URL.
                 *
                 * @return the URL, or empty when the relay omitted the field
                 */
                public Optional<String> appealFormUrl() {
                    return Optional.ofNullable(appealFormUrl);
                }

                /**
                 * Returns the reporter contact email.
                 *
                 * @return the email, or empty when the relay omitted the field
                 */
                public Optional<String> reporterEmail() {
                    return Optional.ofNullable(reporterEmail);
                }

                /**
                 * Returns the reporter contact name.
                 *
                 * @return the name, or empty when the relay omitted the field
                 */
                public Optional<String> reporterName() {
                    return Optional.ofNullable(reporterName);
                }

                /**
                 * Parses an {@link IpViolationReportData} from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed entry, or empty when {@code obj} is {@code null}
                 */
                static Optional<IpViolationReportData> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var reportFbid = obj.getString("report_fbid");
                    var appealFormUrl = obj.getString("appeal_form_url");
                    var reporterEmail = obj.getString("reporter_email");
                    var reporterName = obj.getString("reporter_name");
                    return Optional.of(new IpViolationReportData(reportFbid, appealFormUrl, reporterEmail, reporterName));
                }

                /**
                 * Parses a list of {@link IpViolationReportData} entries from the given JSON array.
                 *
                 * @param arr the JSON array to parse
                 * @return the parsed list, empty when {@code arr} is {@code null}
                 */
                static List<IpViolationReportData> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<IpViolationReportData>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Wraps the {@code enforcement_target_data} sub-object.
             *
             * <p>Points to the triggering message by server message id; the presence of this
             * sub-object is what WhatsApp Web uses to flip the enforcement type from {@code SUSPEND}
             * to {@code SUSPEND_INFORM}.
             */
            public static final class EnforcementTargetData {
                /**
                 * Holds the server message id of the triggering message.
                 */
                private final String serverMsgId;

                /**
                 * Constructs a target-data wrapper from the parsed sub-fields.
                 *
                 * @param serverMsgId the server message id of the triggering message
                 */
                private EnforcementTargetData(String serverMsgId) {
                    this.serverMsgId = serverMsgId;
                }

                /**
                 * Returns the server message id of the triggering message.
                 *
                 * @return the server message id, or empty when the relay omitted the field
                 */
                public Optional<String> serverMsgId() {
                    return Optional.ofNullable(serverMsgId);
                }

                /**
                 * Parses an {@link EnforcementTargetData} from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed entry, or empty when {@code obj} is {@code null}
                 */
                static Optional<EnforcementTargetData> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var serverMsgId = obj.getString("server_msg_id");
                    return Optional.of(new EnforcementTargetData(serverMsgId));
                }

                /**
                 * Parses a list of {@link EnforcementTargetData} entries from the given JSON array.
                 *
                 * @param arr the JSON array to parse
                 * @return the parsed list, empty when {@code arr} is {@code null}
                 */
                static List<EnforcementTargetData> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<EnforcementTargetData>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Wraps the {@code appeal_extra_data} sub-object.
             *
             * <p>Carries the appeal-form URL the admin can open to contest the enforcement.
             */
            public static final class AppealExtraData {
                /**
                 * Holds the appeal-form URL.
                 */
                private final String appealFormUrl;

                /**
                 * Constructs an appeal-extra-data wrapper from the parsed sub-fields.
                 *
                 * @param appealFormUrl the appeal-form URL
                 */
                private AppealExtraData(String appealFormUrl) {
                    this.appealFormUrl = appealFormUrl;
                }

                /**
                 * Returns the appeal-form URL.
                 *
                 * @return the URL, or empty when the relay omitted the field
                 */
                public Optional<String> appealFormUrl() {
                    return Optional.ofNullable(appealFormUrl);
                }

                /**
                 * Parses an {@link AppealExtraData} from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed entry, or empty when {@code obj} is {@code null}
                 */
                static Optional<AppealExtraData> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var appealFormUrl = obj.getString("appeal_form_url");
                    return Optional.of(new AppealExtraData(appealFormUrl));
                }

                /**
                 * Parses a list of {@link AppealExtraData} entries from the given JSON array.
                 *
                 * @param arr the JSON array to parse
                 * @return the parsed list, empty when {@code arr} is {@code null}
                 */
                static List<AppealExtraData> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<AppealExtraData>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Parses an {@link EnforcementExtraData} from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed entry, or empty when {@code obj} is {@code null}
             */
            static Optional<EnforcementExtraData> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var ipViolationReportData = IpViolationReportData.of(obj.getJSONObject("ip_violation_report_data")).orElse(null);
                var enforcementTargetData = EnforcementTargetData.of(obj.getJSONObject("enforcement_target_data")).orElse(null);
                var appealExtraData = AppealExtraData.of(obj.getJSONObject("appeal_extra_data")).orElse(null);
                return Optional.of(new EnforcementExtraData(ipViolationReportData, enforcementTargetData, appealExtraData));
            }

            /**
             * Parses a list of {@link EnforcementExtraData} entries from the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<EnforcementExtraData> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<EnforcementExtraData>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Wraps the {@code enforcement_policy_information} sub-object.
         *
         * <p>Shares the same shape as
         * {@link ProfilePictureDeletions.EnforcementPolicyInformation}.
         */
        public static final class EnforcementPolicyInformation {
            /**
             * Holds the policy overview text.
             */
            private final String overview;

            /**
             * Holds the policy headline text.
             */
            private final String headline;

            /**
             * Holds the policy subtitle text.
             */
            private final String subtitle;

            /**
             * Holds the policy explanation body.
             */
            private final String explanation;

            /**
             * Holds the admin-only disclaimer.
             */
            private final String adminDisclaimer;

            /**
             * Constructs a policy-information wrapper from the parsed sub-fields.
             *
             * @param overview        the policy overview text
             * @param headline        the policy headline text
             * @param subtitle        the policy subtitle text
             * @param explanation     the policy explanation body
             * @param adminDisclaimer the admin-only disclaimer
             */
            private EnforcementPolicyInformation(String overview, String headline, String subtitle, String explanation, String adminDisclaimer) {
                this.overview = overview;
                this.headline = headline;
                this.subtitle = subtitle;
                this.explanation = explanation;
                this.adminDisclaimer = adminDisclaimer;
            }

            /**
             * Returns the policy overview text.
             *
             * @return the overview, or empty when the relay omitted the field
             */
            public Optional<String> overview() {
                return Optional.ofNullable(overview);
            }

            /**
             * Returns the policy headline text.
             *
             * @return the headline, or empty when the relay omitted the field
             */
            public Optional<String> headline() {
                return Optional.ofNullable(headline);
            }

            /**
             * Returns the policy subtitle text.
             *
             * @return the subtitle, or empty when the relay omitted the field
             */
            public Optional<String> subtitle() {
                return Optional.ofNullable(subtitle);
            }

            /**
             * Returns the policy explanation body.
             *
             * @return the explanation, or empty when the relay omitted the field
             */
            public Optional<String> explanation() {
                return Optional.ofNullable(explanation);
            }

            /**
             * Returns the admin-only disclaimer.
             *
             * @return the disclaimer, or empty when the relay omitted the field
             */
            public Optional<String> adminDisclaimer() {
                return Optional.ofNullable(adminDisclaimer);
            }

            /**
             * Parses an {@link EnforcementPolicyInformation} from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed entry, or empty when {@code obj} is {@code null}
             */
            static Optional<EnforcementPolicyInformation> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var overview = obj.getString("overview");
                var headline = obj.getString("headline");
                var subtitle = obj.getString("subtitle");
                var explanation = obj.getString("explanation");
                var adminDisclaimer = obj.getString("admin_disclaimer");
                return Optional.of(new EnforcementPolicyInformation(overview, headline, subtitle, explanation, adminDisclaimer));
            }

            /**
             * Parses a list of {@link EnforcementPolicyInformation} entries from the given JSON
             * array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<EnforcementPolicyInformation> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<EnforcementPolicyInformation>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a {@link Suspensions} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed entry, or empty when {@code obj} is {@code null}
         */
        static Optional<Suspensions> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var appealCreationTime = obj.getLong("appeal_creation_time");
            var enforcementCreationTime = obj.getLong("enforcement_creation_time");
            var appealState = obj.getString("appeal_state");
            var enforcementViolationCategory = obj.getString("enforcement_violation_category");
            var enforcementId = obj.getString("enforcement_id");
            var enforcementSource = obj.getString("enforcement_source");
            var enforcementExtraData = EnforcementExtraData.of(obj.getJSONObject("enforcement_extra_data")).orElse(null);
            var enforcementPolicyInformation = EnforcementPolicyInformation.of(obj.getJSONObject("enforcement_policy_information")).orElse(null);
            return Optional.of(new Suspensions(appealCreationTime, enforcementCreationTime, appealState, enforcementViolationCategory, enforcementId, enforcementSource, enforcementExtraData, enforcementPolicyInformation));
        }

        /**
         * Parses a list of {@link Suspensions} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<Suspensions> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Suspensions>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Wraps one entry of the {@code violating_messages} bucket.
     *
     * <p>Pairs the shared {@link BaseEnforcementData} with the {@link ContentData} pointer to the
     * violating message: either a chat message identified by {@code server_msg_id} or a status
     * identified by {@code server_id}.
     */
    public static final class ViolatingMessages {
        /**
         * Holds the shared enforcement record.
         */
        private final BaseEnforcementData baseEnforcementData;

        /**
         * Holds the pointer to the violating message.
         */
        private final ContentData contentData;

        /**
         * Constructs a violating-message wrapper from the parsed sub-fields.
         *
         * @param baseEnforcementData the shared enforcement record
         * @param contentData         the pointer to the violating message
         */
        private ViolatingMessages(BaseEnforcementData baseEnforcementData, ContentData contentData) {
            this.baseEnforcementData = baseEnforcementData;
            this.contentData = contentData;
        }

        /**
         * Returns the shared enforcement record.
         *
         * @return the parsed {@link BaseEnforcementData}, or empty when the relay omitted the field
         */
        public Optional<BaseEnforcementData> baseEnforcementData() {
            return Optional.ofNullable(baseEnforcementData);
        }

        /**
         * Returns the pointer to the violating message.
         *
         * @return the parsed {@link ContentData}, or empty when the relay omitted the field
         */
        public Optional<ContentData> contentData() {
            return Optional.ofNullable(contentData);
        }

        /**
         * Wraps the {@code content_data} polymorphic sub-object.
         *
         * <p>The {@link #typename()} discriminator selects which inline fragment the relay
         * populated: {@code XWA2ChannelServerMsgData} sets {@link #serverMsgId()} for a chat message,
         * and {@code XWA2ChannelStatusData} sets {@link #serverId()} for a status. Only one of the
         * two id fields is populated at a time.
         */
        public static final class ContentData {
            /**
             * Holds the GraphQL inline-fragment type-name discriminator.
             */
            private final String typename;

            /**
             * Holds the server message id from the chat-message inline fragment.
             */
            private final String serverMsgId;

            /**
             * Holds the server status id from the status inline fragment.
             */
            private final String serverId;

            /**
             * Constructs a content-data wrapper from the parsed sub-fields.
             *
             * @param typename    the GraphQL type-name discriminator
             * @param serverMsgId the chat-message server id
             * @param serverId    the status server id
             */
            private ContentData(String typename, String serverMsgId, String serverId) {
                this.typename = typename;
                this.serverMsgId = serverMsgId;
                this.serverId = serverId;
            }

            /**
             * Returns the GraphQL inline-fragment type-name discriminator.
             *
             * @return the typename, or empty when the relay omitted the field
             */
            public Optional<String> typename() {
                return Optional.ofNullable(typename);
            }

            /**
             * Returns the chat-message server id.
             *
             * <p>Populated only when {@link #typename()} is {@code "XWA2ChannelServerMsgData"}.
             *
             * @return the server message id, or empty when the relay omitted the field
             */
            public Optional<String> serverMsgId() {
                return Optional.ofNullable(serverMsgId);
            }

            /**
             * Returns the status server id.
             *
             * <p>Populated only when {@link #typename()} is {@code "XWA2ChannelStatusData"}.
             *
             * @return the status server id, or empty when the relay omitted the field
             */
            public Optional<String> serverId() {
                return Optional.ofNullable(serverId);
            }

            /**
             * Parses a {@link ContentData} from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed entry, or empty when {@code obj} is {@code null}
             */
            static Optional<ContentData> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var typename = obj.getString("__typename");
                var serverMsgId = obj.getString("server_msg_id");
                var serverId = obj.getString("server_id");
                return Optional.of(new ContentData(typename, serverMsgId, serverId));
            }

            /**
             * Parses a list of {@link ContentData} entries from the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<ContentData> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<ContentData>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Wraps the shared {@code base_enforcement_data} sub-object.
         *
         * <p>Carries the standard enforcement scaffolding (timestamps, appeal state, id, source,
         * optional extra data, optional policy information) that the violating-message and
         * geo-suspension buckets share.
         */
        public static final class BaseEnforcementData {
            /**
             * Holds the enforcement creation epoch-second.
             */
            private final Long enforcementCreationTime;

            /**
             * Holds the appeal creation epoch-second, or {@code null}.
             */
            private final Long appealCreationTime;

            /**
             * Holds the appeal state label.
             */
            private final String appealState;

            /**
             * Holds the enforcement identifier.
             */
            private final String enforcementId;

            /**
             * Holds the violation category label.
             */
            private final String enforcementViolationCategory;

            /**
             * Holds the enforcement source label.
             */
            private final String enforcementSource;

            /**
             * Holds the optional extra-data sub-object.
             */
            private final EnforcementExtraData enforcementExtraData;

            /**
             * Holds the optional policy-information sub-object.
             */
            private final EnforcementPolicyInformation enforcementPolicyInformation;

            /**
             * Constructs a base-enforcement-data wrapper from the parsed sub-fields.
             *
             * @param enforcementCreationTime      the enforcement creation epoch-second
             * @param appealCreationTime           the appeal creation epoch-second, or {@code null}
             * @param appealState                  the appeal state label
             * @param enforcementId                the enforcement identifier
             * @param enforcementViolationCategory the violation category label
             * @param enforcementSource            the enforcement source label
             * @param enforcementExtraData         the extra-data sub-object
             * @param enforcementPolicyInformation the policy-information sub-object
             */
            private BaseEnforcementData(Long enforcementCreationTime, Long appealCreationTime, String appealState, String enforcementId, String enforcementViolationCategory, String enforcementSource, EnforcementExtraData enforcementExtraData, EnforcementPolicyInformation enforcementPolicyInformation) {
                this.enforcementCreationTime = enforcementCreationTime;
                this.appealCreationTime = appealCreationTime;
                this.appealState = appealState;
                this.enforcementId = enforcementId;
                this.enforcementViolationCategory = enforcementViolationCategory;
                this.enforcementSource = enforcementSource;
                this.enforcementExtraData = enforcementExtraData;
                this.enforcementPolicyInformation = enforcementPolicyInformation;
            }

            /**
             * Returns the enforcement creation instant.
             *
             * @return the creation instant, or empty when the relay omitted the field
             */
            public Optional<Instant> enforcementCreationTime() {
                return Optional.ofNullable(enforcementCreationTime).map(Instant::ofEpochSecond);
            }

            /**
             * Returns the appeal creation instant.
             *
             * @return the appeal instant, or empty when no appeal has been filed
             */
            public Optional<Instant> appealCreationTime() {
                return Optional.ofNullable(appealCreationTime).map(Instant::ofEpochSecond);
            }

            /**
             * Returns the appeal state label.
             *
             * @return the appeal state, or empty when the relay omitted the field
             */
            public Optional<String> appealState() {
                return Optional.ofNullable(appealState);
            }

            /**
             * Returns the enforcement identifier.
             *
             * @return the enforcement id, or empty when the relay omitted the field
             */
            public Optional<String> enforcementId() {
                return Optional.ofNullable(enforcementId);
            }

            /**
             * Returns the violation category label.
             *
             * @return the violation category, or empty when the relay omitted the field
             */
            public Optional<String> enforcementViolationCategory() {
                return Optional.ofNullable(enforcementViolationCategory);
            }

            /**
             * Returns the enforcement source label.
             *
             * @return the enforcement source, or empty when the relay omitted the field
             */
            public Optional<String> enforcementSource() {
                return Optional.ofNullable(enforcementSource);
            }

            /**
             * Returns the extra-data sub-object.
             *
             * @return the parsed {@link EnforcementExtraData}, or empty when the relay omitted the
             *         field
             */
            public Optional<EnforcementExtraData> enforcementExtraData() {
                return Optional.ofNullable(enforcementExtraData);
            }

            /**
             * Returns the policy-information sub-object.
             *
             * @return the parsed {@link EnforcementPolicyInformation}, or empty when the relay
             *         omitted the field
             */
            public Optional<EnforcementPolicyInformation> enforcementPolicyInformation() {
                return Optional.ofNullable(enforcementPolicyInformation);
            }

            /**
             * Wraps the {@code enforcement_extra_data} sub-object as projected for violating-message
             * enforcements.
             *
             * <p>For violating messages the extra data carries only the IP-violation report data.
             */
            public static final class EnforcementExtraData {
                /**
                 * Holds the IP-violation report data sub-object.
                 */
                private final IpViolationReportData ipViolationReportData;

                /**
                 * Constructs an extra-data wrapper from the parsed sub-fields.
                 *
                 * @param ipViolationReportData the IP-violation report data sub-object
                 */
                private EnforcementExtraData(IpViolationReportData ipViolationReportData) {
                    this.ipViolationReportData = ipViolationReportData;
                }

                /**
                 * Returns the IP-violation report data sub-object.
                 *
                 * @return the parsed {@link IpViolationReportData}, or empty when the relay omitted
                 *         the field
                 */
                public Optional<IpViolationReportData> ipViolationReportData() {
                    return Optional.ofNullable(ipViolationReportData);
                }

                /**
                 * Wraps the {@code ip_violation_report_data} sub-object.
                 *
                 * <p>Shares the same shape as
                 * {@link ProfilePictureDeletions.EnforcementExtraData.IpViolationReportData}.
                 */
                public static final class IpViolationReportData {
                    /**
                     * Holds the Facebook report identifier.
                     */
                    private final String reportFbid;

                    /**
                     * Holds the appeal-form URL.
                     */
                    private final String appealFormUrl;

                    /**
                     * Holds the reporter contact email.
                     */
                    private final String reporterEmail;

                    /**
                     * Holds the reporter contact name.
                     */
                    private final String reporterName;

                    /**
                     * Constructs an IP-violation-report-data wrapper from the parsed sub-fields.
                     *
                     * @param reportFbid    the Facebook report identifier
                     * @param appealFormUrl the appeal-form URL
                     * @param reporterEmail the reporter contact email
                     * @param reporterName  the reporter contact name
                     */
                    private IpViolationReportData(String reportFbid, String appealFormUrl, String reporterEmail, String reporterName) {
                        this.reportFbid = reportFbid;
                        this.appealFormUrl = appealFormUrl;
                        this.reporterEmail = reporterEmail;
                        this.reporterName = reporterName;
                    }

                    /**
                     * Returns the Facebook report identifier.
                     *
                     * @return the report id, or empty when the relay omitted the field
                     */
                    public Optional<String> reportFbid() {
                        return Optional.ofNullable(reportFbid);
                    }

                    /**
                     * Returns the appeal-form URL.
                     *
                     * @return the URL, or empty when the relay omitted the field
                     */
                    public Optional<String> appealFormUrl() {
                        return Optional.ofNullable(appealFormUrl);
                    }

                    /**
                     * Returns the reporter contact email.
                     *
                     * @return the email, or empty when the relay omitted the field
                     */
                    public Optional<String> reporterEmail() {
                        return Optional.ofNullable(reporterEmail);
                    }

                    /**
                     * Returns the reporter contact name.
                     *
                     * @return the name, or empty when the relay omitted the field
                     */
                    public Optional<String> reporterName() {
                        return Optional.ofNullable(reporterName);
                    }

                    /**
                     * Parses an {@link IpViolationReportData} from the given JSON object.
                     *
                     * @param obj the JSON object to parse
                     * @return the parsed entry, or empty when {@code obj} is {@code null}
                     */
                    static Optional<IpViolationReportData> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var reportFbid = obj.getString("report_fbid");
                        var appealFormUrl = obj.getString("appeal_form_url");
                        var reporterEmail = obj.getString("reporter_email");
                        var reporterName = obj.getString("reporter_name");
                        return Optional.of(new IpViolationReportData(reportFbid, appealFormUrl, reporterEmail, reporterName));
                    }

                    /**
                     * Parses a list of {@link IpViolationReportData} entries from the given JSON
                     * array.
                     *
                     * @param arr the JSON array to parse
                     * @return the parsed list, empty when {@code arr} is {@code null}
                     */
                    static List<IpViolationReportData> ofArray(JSONArray arr) {
                        if (arr == null) {
                            return List.of();
                        }

                        var result = new ArrayList<IpViolationReportData>(arr.size());
                        for (var i = 0; i < arr.size(); i++) {
                            of(arr.getJSONObject(i)).ifPresent(result::add);
                        }
                        return result;
                    }
                }

                /**
                 * Parses an {@link EnforcementExtraData} from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed entry, or empty when {@code obj} is {@code null}
                 */
                static Optional<EnforcementExtraData> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var ipViolationReportData = IpViolationReportData.of(obj.getJSONObject("ip_violation_report_data")).orElse(null);
                    return Optional.of(new EnforcementExtraData(ipViolationReportData));
                }

                /**
                 * Parses a list of {@link EnforcementExtraData} entries from the given JSON array.
                 *
                 * @param arr the JSON array to parse
                 * @return the parsed list, empty when {@code arr} is {@code null}
                 */
                static List<EnforcementExtraData> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<EnforcementExtraData>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Wraps the {@code enforcement_policy_information} sub-object.
             *
             * <p>Shares the same shape as
             * {@link ProfilePictureDeletions.EnforcementPolicyInformation}.
             */
            public static final class EnforcementPolicyInformation {
                /**
                 * Holds the policy overview text.
                 */
                private final String overview;

                /**
                 * Holds the policy headline text.
                 */
                private final String headline;

                /**
                 * Holds the policy subtitle text.
                 */
                private final String subtitle;

                /**
                 * Holds the policy explanation body.
                 */
                private final String explanation;

                /**
                 * Holds the admin-only disclaimer.
                 */
                private final String adminDisclaimer;

                /**
                 * Constructs a policy-information wrapper from the parsed sub-fields.
                 *
                 * @param overview        the policy overview text
                 * @param headline        the policy headline text
                 * @param subtitle        the policy subtitle text
                 * @param explanation     the policy explanation body
                 * @param adminDisclaimer the admin-only disclaimer
                 */
                private EnforcementPolicyInformation(String overview, String headline, String subtitle, String explanation, String adminDisclaimer) {
                    this.overview = overview;
                    this.headline = headline;
                    this.subtitle = subtitle;
                    this.explanation = explanation;
                    this.adminDisclaimer = adminDisclaimer;
                }

                /**
                 * Returns the policy overview text.
                 *
                 * @return the overview, or empty when the relay omitted the field
                 */
                public Optional<String> overview() {
                    return Optional.ofNullable(overview);
                }

                /**
                 * Returns the policy headline text.
                 *
                 * @return the headline, or empty when the relay omitted the field
                 */
                public Optional<String> headline() {
                    return Optional.ofNullable(headline);
                }

                /**
                 * Returns the policy subtitle text.
                 *
                 * @return the subtitle, or empty when the relay omitted the field
                 */
                public Optional<String> subtitle() {
                    return Optional.ofNullable(subtitle);
                }

                /**
                 * Returns the policy explanation body.
                 *
                 * @return the explanation, or empty when the relay omitted the field
                 */
                public Optional<String> explanation() {
                    return Optional.ofNullable(explanation);
                }

                /**
                 * Returns the admin-only disclaimer.
                 *
                 * @return the disclaimer, or empty when the relay omitted the field
                 */
                public Optional<String> adminDisclaimer() {
                    return Optional.ofNullable(adminDisclaimer);
                }

                /**
                 * Parses an {@link EnforcementPolicyInformation} from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed entry, or empty when {@code obj} is {@code null}
                 */
                static Optional<EnforcementPolicyInformation> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var overview = obj.getString("overview");
                    var headline = obj.getString("headline");
                    var subtitle = obj.getString("subtitle");
                    var explanation = obj.getString("explanation");
                    var adminDisclaimer = obj.getString("admin_disclaimer");
                    return Optional.of(new EnforcementPolicyInformation(overview, headline, subtitle, explanation, adminDisclaimer));
                }

                /**
                 * Parses a list of {@link EnforcementPolicyInformation} entries from the given JSON
                 * array.
                 *
                 * @param arr the JSON array to parse
                 * @return the parsed list, empty when {@code arr} is {@code null}
                 */
                static List<EnforcementPolicyInformation> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<EnforcementPolicyInformation>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Parses a {@link BaseEnforcementData} from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed entry, or empty when {@code obj} is {@code null}
             */
            static Optional<BaseEnforcementData> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var enforcementCreationTime = obj.getLong("enforcement_creation_time");
                var appealCreationTime = obj.getLong("appeal_creation_time");
                var appealState = obj.getString("appeal_state");
                var enforcementId = obj.getString("enforcement_id");
                var enforcementViolationCategory = obj.getString("enforcement_violation_category");
                var enforcementSource = obj.getString("enforcement_source");
                var enforcementExtraData = EnforcementExtraData.of(obj.getJSONObject("enforcement_extra_data")).orElse(null);
                var enforcementPolicyInformation = EnforcementPolicyInformation.of(obj.getJSONObject("enforcement_policy_information")).orElse(null);
                return Optional.of(new BaseEnforcementData(enforcementCreationTime, appealCreationTime, appealState, enforcementId, enforcementViolationCategory, enforcementSource, enforcementExtraData, enforcementPolicyInformation));
            }

            /**
             * Parses a list of {@link BaseEnforcementData} entries from the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<BaseEnforcementData> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<BaseEnforcementData>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a {@link ViolatingMessages} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed entry, or empty when {@code obj} is {@code null}
         */
        static Optional<ViolatingMessages> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var baseEnforcementData = BaseEnforcementData.of(obj.getJSONObject("base_enforcement_data")).orElse(null);
            var contentData = ContentData.of(obj.getJSONObject("content_data")).orElse(null);
            return Optional.of(new ViolatingMessages(baseEnforcementData, contentData));
        }

        /**
         * Parses a list of {@link ViolatingMessages} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<ViolatingMessages> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<ViolatingMessages>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Wraps one entry of the {@code geosuspensions} bucket.
     *
     * <p>Pairs the shared {@link BaseEnforcementData}, whose richer extra-data carries the
     * enforcing-entity and workflow tags, with the raw list of ISO 2-letter country codes the
     * suspension applies to.
     */
    public static final class Geosuspensions {
        /**
         * Holds the shared enforcement record.
         */
        private final BaseEnforcementData baseEnforcementData;

        /**
         * Holds the raw ISO 2-letter country codes the suspension applies to.
         */
        private final List<String> countryCodes;

        /**
         * Constructs a geo-suspension wrapper from the parsed sub-fields.
         *
         * @param baseEnforcementData the shared enforcement record
         * @param countryCodes        the raw ISO 2-letter country codes
         */
        private Geosuspensions(BaseEnforcementData baseEnforcementData, List<String> countryCodes) {
            this.baseEnforcementData = baseEnforcementData;
            this.countryCodes = countryCodes;
        }

        /**
         * Returns the shared enforcement record.
         *
         * @return the parsed {@link BaseEnforcementData}, or empty when the relay omitted the field
         */
        public Optional<BaseEnforcementData> baseEnforcementData() {
            return Optional.ofNullable(baseEnforcementData);
        }

        /**
         * Returns the raw ISO 2-letter country codes.
         *
         * <p>The list carries the codes verbatim; WhatsApp Web enriches each entry into a
         * country-code and country-name pair for display in the moderation UI.
         *
         * @return the country codes, empty when the relay returned none
         */
        public List<String> countryCodes() {
            return countryCodes;
        }

        /**
         * Wraps the shared {@code base_enforcement_data} sub-object as projected for geo-suspension
         * enforcements.
         *
         * <p>Geo-suspensions reuse the same scaffolding as violating-message enforcements but expose
         * a richer {@link EnforcementExtraData} with enforcing-entity name and workflow/legal-basis
         * tags.
         */
        public static final class BaseEnforcementData {
            /**
             * Holds the enforcement creation epoch-second.
             */
            private final Long enforcementCreationTime;

            /**
             * Holds the appeal creation epoch-second, or {@code null}.
             */
            private final Long appealCreationTime;

            /**
             * Holds the appeal state label.
             */
            private final String appealState;

            /**
             * Holds the enforcement identifier.
             */
            private final String enforcementId;

            /**
             * Holds the violation category label.
             */
            private final String enforcementViolationCategory;

            /**
             * Holds the enforcement source label.
             */
            private final String enforcementSource;

            /**
             * Holds the optional extra-data sub-object.
             */
            private final EnforcementExtraData enforcementExtraData;

            /**
             * Holds the optional policy-information sub-object.
             */
            private final EnforcementPolicyInformation enforcementPolicyInformation;

            /**
             * Constructs a base-enforcement-data wrapper from the parsed sub-fields.
             *
             * @param enforcementCreationTime      the enforcement creation epoch-second
             * @param appealCreationTime           the appeal creation epoch-second, or {@code null}
             * @param appealState                  the appeal state label
             * @param enforcementId                the enforcement identifier
             * @param enforcementViolationCategory the violation category label
             * @param enforcementSource            the enforcement source label
             * @param enforcementExtraData         the extra-data sub-object
             * @param enforcementPolicyInformation the policy-information sub-object
             */
            private BaseEnforcementData(Long enforcementCreationTime, Long appealCreationTime, String appealState, String enforcementId, String enforcementViolationCategory, String enforcementSource, EnforcementExtraData enforcementExtraData, EnforcementPolicyInformation enforcementPolicyInformation) {
                this.enforcementCreationTime = enforcementCreationTime;
                this.appealCreationTime = appealCreationTime;
                this.appealState = appealState;
                this.enforcementId = enforcementId;
                this.enforcementViolationCategory = enforcementViolationCategory;
                this.enforcementSource = enforcementSource;
                this.enforcementExtraData = enforcementExtraData;
                this.enforcementPolicyInformation = enforcementPolicyInformation;
            }

            /**
             * Returns the enforcement creation instant.
             *
             * @return the creation instant, or empty when the relay omitted the field
             */
            public Optional<Instant> enforcementCreationTime() {
                return Optional.ofNullable(enforcementCreationTime).map(Instant::ofEpochSecond);
            }

            /**
             * Returns the appeal creation instant.
             *
             * @return the appeal instant, or empty when no appeal has been filed
             */
            public Optional<Instant> appealCreationTime() {
                return Optional.ofNullable(appealCreationTime).map(Instant::ofEpochSecond);
            }

            /**
             * Returns the appeal state label.
             *
             * @return the appeal state, or empty when the relay omitted the field
             */
            public Optional<String> appealState() {
                return Optional.ofNullable(appealState);
            }

            /**
             * Returns the enforcement identifier.
             *
             * @return the enforcement id, or empty when the relay omitted the field
             */
            public Optional<String> enforcementId() {
                return Optional.ofNullable(enforcementId);
            }

            /**
             * Returns the violation category label.
             *
             * @return the violation category, or empty when the relay omitted the field
             */
            public Optional<String> enforcementViolationCategory() {
                return Optional.ofNullable(enforcementViolationCategory);
            }

            /**
             * Returns the enforcement source label.
             *
             * @return the enforcement source, or empty when the relay omitted the field
             */
            public Optional<String> enforcementSource() {
                return Optional.ofNullable(enforcementSource);
            }

            /**
             * Returns the extra-data sub-object.
             *
             * @return the parsed {@link EnforcementExtraData}, or empty when the relay omitted the
             *         field
             */
            public Optional<EnforcementExtraData> enforcementExtraData() {
                return Optional.ofNullable(enforcementExtraData);
            }

            /**
             * Returns the policy-information sub-object.
             *
             * @return the parsed {@link EnforcementPolicyInformation}, or empty when the relay
             *         omitted the field
             */
            public Optional<EnforcementPolicyInformation> enforcementPolicyInformation() {
                return Optional.ofNullable(enforcementPolicyInformation);
            }

            /**
             * Wraps the {@code enforcement_extra_data} sub-object as projected for geo-suspension
             * enforcements.
             *
             * <p>Geo-suspension extra data extends the suspension shape with the enforcing-entity
             * name, the origin workflow tag and the origin legal-basis tag; the presence of
             * {@link EnforcementTargetData} flips the type from {@code GEOSUSPEND} to
             * {@code GEOSUSPEND_INFORM}.
             */
            public static final class EnforcementExtraData {
                /**
                 * Holds the IP-violation report data sub-object.
                 */
                private final IpViolationReportData ipViolationReportData;

                /**
                 * Holds the target-message pointer sub-object.
                 */
                private final EnforcementTargetData enforcementTargetData;

                /**
                 * Holds the appeal-form-URL sub-object.
                 */
                private final AppealExtraData appealExtraData;

                /**
                 * Holds the enforcing-entity-name sub-object.
                 */
                private final EnforcingEntityData enforcingEntityData;

                /**
                 * Holds the enforcement origin workflow tag.
                 */
                private final String enforcementOriginWorkflow;

                /**
                 * Holds the enforcement origin legal-basis tag.
                 */
                private final String enforcementOriginLegalBasis;

                /**
                 * Constructs an extra-data wrapper from the parsed sub-fields.
                 *
                 * @param ipViolationReportData       the IP-violation report data sub-object
                 * @param enforcementTargetData       the target-message pointer sub-object
                 * @param appealExtraData             the appeal-form-URL sub-object
                 * @param enforcingEntityData         the enforcing-entity-name sub-object
                 * @param enforcementOriginWorkflow   the origin workflow tag
                 * @param enforcementOriginLegalBasis the origin legal-basis tag
                 */
                private EnforcementExtraData(IpViolationReportData ipViolationReportData, EnforcementTargetData enforcementTargetData, AppealExtraData appealExtraData, EnforcingEntityData enforcingEntityData, String enforcementOriginWorkflow, String enforcementOriginLegalBasis) {
                    this.ipViolationReportData = ipViolationReportData;
                    this.enforcementTargetData = enforcementTargetData;
                    this.appealExtraData = appealExtraData;
                    this.enforcingEntityData = enforcingEntityData;
                    this.enforcementOriginWorkflow = enforcementOriginWorkflow;
                    this.enforcementOriginLegalBasis = enforcementOriginLegalBasis;
                }

                /**
                 * Returns the IP-violation report data sub-object.
                 *
                 * @return the parsed {@link IpViolationReportData}, or empty when the relay omitted
                 *         the field
                 */
                public Optional<IpViolationReportData> ipViolationReportData() {
                    return Optional.ofNullable(ipViolationReportData);
                }

                /**
                 * Returns the target-message pointer sub-object.
                 *
                 * @return the parsed {@link EnforcementTargetData}, or empty when the relay omitted
                 *         the field
                 */
                public Optional<EnforcementTargetData> enforcementTargetData() {
                    return Optional.ofNullable(enforcementTargetData);
                }

                /**
                 * Returns the appeal-form-URL sub-object.
                 *
                 * @return the parsed {@link AppealExtraData}, or empty when the relay omitted the
                 *         field
                 */
                public Optional<AppealExtraData> appealExtraData() {
                    return Optional.ofNullable(appealExtraData);
                }

                /**
                 * Returns the enforcing-entity-name sub-object.
                 *
                 * @return the parsed {@link EnforcingEntityData}, or empty when the relay omitted
                 *         the field
                 */
                public Optional<EnforcingEntityData> enforcingEntityData() {
                    return Optional.ofNullable(enforcingEntityData);
                }

                /**
                 * Returns the enforcement origin workflow tag.
                 *
                 * @return the workflow tag, or empty when the relay omitted the field
                 */
                public Optional<String> enforcementOriginWorkflow() {
                    return Optional.ofNullable(enforcementOriginWorkflow);
                }

                /**
                 * Returns the enforcement origin legal-basis tag.
                 *
                 * @return the legal-basis tag, or empty when the relay omitted the field
                 */
                public Optional<String> enforcementOriginLegalBasis() {
                    return Optional.ofNullable(enforcementOriginLegalBasis);
                }

                /**
                 * Wraps the {@code ip_violation_report_data} sub-object.
                 *
                 * <p>Shares the same shape as
                 * {@link ProfilePictureDeletions.EnforcementExtraData.IpViolationReportData}.
                 */
                public static final class IpViolationReportData {
                    /**
                     * Holds the Facebook report identifier.
                     */
                    private final String reportFbid;

                    /**
                     * Holds the appeal-form URL.
                     */
                    private final String appealFormUrl;

                    /**
                     * Holds the reporter contact email.
                     */
                    private final String reporterEmail;

                    /**
                     * Holds the reporter contact name.
                     */
                    private final String reporterName;

                    /**
                     * Constructs an IP-violation-report-data wrapper from the parsed sub-fields.
                     *
                     * @param reportFbid    the Facebook report identifier
                     * @param appealFormUrl the appeal-form URL
                     * @param reporterEmail the reporter contact email
                     * @param reporterName  the reporter contact name
                     */
                    private IpViolationReportData(String reportFbid, String appealFormUrl, String reporterEmail, String reporterName) {
                        this.reportFbid = reportFbid;
                        this.appealFormUrl = appealFormUrl;
                        this.reporterEmail = reporterEmail;
                        this.reporterName = reporterName;
                    }

                    /**
                     * Returns the Facebook report identifier.
                     *
                     * @return the report id, or empty when the relay omitted the field
                     */
                    public Optional<String> reportFbid() {
                        return Optional.ofNullable(reportFbid);
                    }

                    /**
                     * Returns the appeal-form URL.
                     *
                     * @return the URL, or empty when the relay omitted the field
                     */
                    public Optional<String> appealFormUrl() {
                        return Optional.ofNullable(appealFormUrl);
                    }

                    /**
                     * Returns the reporter contact email.
                     *
                     * @return the email, or empty when the relay omitted the field
                     */
                    public Optional<String> reporterEmail() {
                        return Optional.ofNullable(reporterEmail);
                    }

                    /**
                     * Returns the reporter contact name.
                     *
                     * @return the name, or empty when the relay omitted the field
                     */
                    public Optional<String> reporterName() {
                        return Optional.ofNullable(reporterName);
                    }

                    /**
                     * Parses an {@link IpViolationReportData} from the given JSON object.
                     *
                     * @param obj the JSON object to parse
                     * @return the parsed entry, or empty when {@code obj} is {@code null}
                     */
                    static Optional<IpViolationReportData> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var reportFbid = obj.getString("report_fbid");
                        var appealFormUrl = obj.getString("appeal_form_url");
                        var reporterEmail = obj.getString("reporter_email");
                        var reporterName = obj.getString("reporter_name");
                        return Optional.of(new IpViolationReportData(reportFbid, appealFormUrl, reporterEmail, reporterName));
                    }

                    /**
                     * Parses a list of {@link IpViolationReportData} entries from the given JSON
                     * array.
                     *
                     * @param arr the JSON array to parse
                     * @return the parsed list, empty when {@code arr} is {@code null}
                     */
                    static List<IpViolationReportData> ofArray(JSONArray arr) {
                        if (arr == null) {
                            return List.of();
                        }

                        var result = new ArrayList<IpViolationReportData>(arr.size());
                        for (var i = 0; i < arr.size(); i++) {
                            of(arr.getJSONObject(i)).ifPresent(result::add);
                        }
                        return result;
                    }
                }

                /**
                 * Wraps the {@code enforcement_target_data} sub-object.
                 *
                 * <p>Shares the same shape as
                 * {@link Suspensions.EnforcementExtraData.EnforcementTargetData}.
                 */
                public static final class EnforcementTargetData {
                    /**
                     * Holds the server message id of the triggering message.
                     */
                    private final String serverMsgId;

                    /**
                     * Constructs a target-data wrapper from the parsed sub-fields.
                     *
                     * @param serverMsgId the server message id of the triggering message
                     */
                    private EnforcementTargetData(String serverMsgId) {
                        this.serverMsgId = serverMsgId;
                    }

                    /**
                     * Returns the server message id of the triggering message.
                     *
                     * @return the server message id, or empty when the relay omitted the field
                     */
                    public Optional<String> serverMsgId() {
                        return Optional.ofNullable(serverMsgId);
                    }

                    /**
                     * Parses an {@link EnforcementTargetData} from the given JSON object.
                     *
                     * @param obj the JSON object to parse
                     * @return the parsed entry, or empty when {@code obj} is {@code null}
                     */
                    static Optional<EnforcementTargetData> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var serverMsgId = obj.getString("server_msg_id");
                        return Optional.of(new EnforcementTargetData(serverMsgId));
                    }

                    /**
                     * Parses a list of {@link EnforcementTargetData} entries from the given JSON
                     * array.
                     *
                     * @param arr the JSON array to parse
                     * @return the parsed list, empty when {@code arr} is {@code null}
                     */
                    static List<EnforcementTargetData> ofArray(JSONArray arr) {
                        if (arr == null) {
                            return List.of();
                        }

                        var result = new ArrayList<EnforcementTargetData>(arr.size());
                        for (var i = 0; i < arr.size(); i++) {
                            of(arr.getJSONObject(i)).ifPresent(result::add);
                        }
                        return result;
                    }
                }

                /**
                 * Wraps the {@code appeal_extra_data} sub-object.
                 *
                 * <p>Shares the same shape as
                 * {@link Suspensions.EnforcementExtraData.AppealExtraData}.
                 */
                public static final class AppealExtraData {
                    /**
                     * Holds the appeal-form URL.
                     */
                    private final String appealFormUrl;

                    /**
                     * Constructs an appeal-extra-data wrapper from the parsed sub-fields.
                     *
                     * @param appealFormUrl the appeal-form URL
                     */
                    private AppealExtraData(String appealFormUrl) {
                        this.appealFormUrl = appealFormUrl;
                    }

                    /**
                     * Returns the appeal-form URL.
                     *
                     * @return the URL, or empty when the relay omitted the field
                     */
                    public Optional<String> appealFormUrl() {
                        return Optional.ofNullable(appealFormUrl);
                    }

                    /**
                     * Parses an {@link AppealExtraData} from the given JSON object.
                     *
                     * @param obj the JSON object to parse
                     * @return the parsed entry, or empty when {@code obj} is {@code null}
                     */
                    static Optional<AppealExtraData> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var appealFormUrl = obj.getString("appeal_form_url");
                        return Optional.of(new AppealExtraData(appealFormUrl));
                    }

                    /**
                     * Parses a list of {@link AppealExtraData} entries from the given JSON array.
                     *
                     * @param arr the JSON array to parse
                     * @return the parsed list, empty when {@code arr} is {@code null}
                     */
                    static List<AppealExtraData> ofArray(JSONArray arr) {
                        if (arr == null) {
                            return List.of();
                        }

                        var result = new ArrayList<AppealExtraData>(arr.size());
                        for (var i = 0; i < arr.size(); i++) {
                            of(arr.getJSONObject(i)).ifPresent(result::add);
                        }
                        return result;
                    }
                }

                /**
                 * Wraps the {@code enforcing_entity_data} sub-object.
                 *
                 * <p>Carries the display name of the entity, typically a national authority, that
                 * requested the suspension.
                 */
                public static final class EnforcingEntityData {
                    /**
                     * Holds the display name of the enforcing entity.
                     */
                    private final String name;

                    /**
                     * Constructs an entity-data wrapper from the parsed sub-fields.
                     *
                     * @param name the display name of the enforcing entity
                     */
                    private EnforcingEntityData(String name) {
                        this.name = name;
                    }

                    /**
                     * Returns the display name of the enforcing entity.
                     *
                     * @return the name, or empty when the relay omitted the field
                     */
                    public Optional<String> name() {
                        return Optional.ofNullable(name);
                    }

                    /**
                     * Parses an {@link EnforcingEntityData} from the given JSON object.
                     *
                     * @param obj the JSON object to parse
                     * @return the parsed entry, or empty when {@code obj} is {@code null}
                     */
                    static Optional<EnforcingEntityData> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var name = obj.getString("name");
                        return Optional.of(new EnforcingEntityData(name));
                    }

                    /**
                     * Parses a list of {@link EnforcingEntityData} entries from the given JSON array.
                     *
                     * @param arr the JSON array to parse
                     * @return the parsed list, empty when {@code arr} is {@code null}
                     */
                    static List<EnforcingEntityData> ofArray(JSONArray arr) {
                        if (arr == null) {
                            return List.of();
                        }

                        var result = new ArrayList<EnforcingEntityData>(arr.size());
                        for (var i = 0; i < arr.size(); i++) {
                            of(arr.getJSONObject(i)).ifPresent(result::add);
                        }
                        return result;
                    }
                }

                /**
                 * Parses an {@link EnforcementExtraData} from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed entry, or empty when {@code obj} is {@code null}
                 */
                static Optional<EnforcementExtraData> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var ipViolationReportData = IpViolationReportData.of(obj.getJSONObject("ip_violation_report_data")).orElse(null);
                    var enforcementTargetData = EnforcementTargetData.of(obj.getJSONObject("enforcement_target_data")).orElse(null);
                    var appealExtraData = AppealExtraData.of(obj.getJSONObject("appeal_extra_data")).orElse(null);
                    var enforcingEntityData = EnforcingEntityData.of(obj.getJSONObject("enforcing_entity_data")).orElse(null);
                    var enforcementOriginWorkflow = obj.getString("enforcement_origin_workflow");
                    var enforcementOriginLegalBasis = obj.getString("enforcement_origin_legal_basis");
                    return Optional.of(new EnforcementExtraData(ipViolationReportData, enforcementTargetData, appealExtraData, enforcingEntityData, enforcementOriginWorkflow, enforcementOriginLegalBasis));
                }

                /**
                 * Parses a list of {@link EnforcementExtraData} entries from the given JSON array.
                 *
                 * @param arr the JSON array to parse
                 * @return the parsed list, empty when {@code arr} is {@code null}
                 */
                static List<EnforcementExtraData> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<EnforcementExtraData>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Wraps the {@code enforcement_policy_information} sub-object.
             *
             * <p>Shares the same shape as
             * {@link ProfilePictureDeletions.EnforcementPolicyInformation}.
             */
            public static final class EnforcementPolicyInformation {
                /**
                 * Holds the policy overview text.
                 */
                private final String overview;

                /**
                 * Holds the policy headline text.
                 */
                private final String headline;

                /**
                 * Holds the policy subtitle text.
                 */
                private final String subtitle;

                /**
                 * Holds the policy explanation body.
                 */
                private final String explanation;

                /**
                 * Holds the admin-only disclaimer.
                 */
                private final String adminDisclaimer;

                /**
                 * Constructs a policy-information wrapper from the parsed sub-fields.
                 *
                 * @param overview        the policy overview text
                 * @param headline        the policy headline text
                 * @param subtitle        the policy subtitle text
                 * @param explanation     the policy explanation body
                 * @param adminDisclaimer the admin-only disclaimer
                 */
                private EnforcementPolicyInformation(String overview, String headline, String subtitle, String explanation, String adminDisclaimer) {
                    this.overview = overview;
                    this.headline = headline;
                    this.subtitle = subtitle;
                    this.explanation = explanation;
                    this.adminDisclaimer = adminDisclaimer;
                }

                /**
                 * Returns the policy overview text.
                 *
                 * @return the overview, or empty when the relay omitted the field
                 */
                public Optional<String> overview() {
                    return Optional.ofNullable(overview);
                }

                /**
                 * Returns the policy headline text.
                 *
                 * @return the headline, or empty when the relay omitted the field
                 */
                public Optional<String> headline() {
                    return Optional.ofNullable(headline);
                }

                /**
                 * Returns the policy subtitle text.
                 *
                 * @return the subtitle, or empty when the relay omitted the field
                 */
                public Optional<String> subtitle() {
                    return Optional.ofNullable(subtitle);
                }

                /**
                 * Returns the policy explanation body.
                 *
                 * @return the explanation, or empty when the relay omitted the field
                 */
                public Optional<String> explanation() {
                    return Optional.ofNullable(explanation);
                }

                /**
                 * Returns the admin-only disclaimer.
                 *
                 * @return the disclaimer, or empty when the relay omitted the field
                 */
                public Optional<String> adminDisclaimer() {
                    return Optional.ofNullable(adminDisclaimer);
                }

                /**
                 * Parses an {@link EnforcementPolicyInformation} from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed entry, or empty when {@code obj} is {@code null}
                 */
                static Optional<EnforcementPolicyInformation> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var overview = obj.getString("overview");
                    var headline = obj.getString("headline");
                    var subtitle = obj.getString("subtitle");
                    var explanation = obj.getString("explanation");
                    var adminDisclaimer = obj.getString("admin_disclaimer");
                    return Optional.of(new EnforcementPolicyInformation(overview, headline, subtitle, explanation, adminDisclaimer));
                }

                /**
                 * Parses a list of {@link EnforcementPolicyInformation} entries from the given JSON
                 * array.
                 *
                 * @param arr the JSON array to parse
                 * @return the parsed list, empty when {@code arr} is {@code null}
                 */
                static List<EnforcementPolicyInformation> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<EnforcementPolicyInformation>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Parses a {@link BaseEnforcementData} from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed entry, or empty when {@code obj} is {@code null}
             */
            static Optional<BaseEnforcementData> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var enforcementCreationTime = obj.getLong("enforcement_creation_time");
                var appealCreationTime = obj.getLong("appeal_creation_time");
                var appealState = obj.getString("appeal_state");
                var enforcementId = obj.getString("enforcement_id");
                var enforcementViolationCategory = obj.getString("enforcement_violation_category");
                var enforcementSource = obj.getString("enforcement_source");
                var enforcementExtraData = EnforcementExtraData.of(obj.getJSONObject("enforcement_extra_data")).orElse(null);
                var enforcementPolicyInformation = EnforcementPolicyInformation.of(obj.getJSONObject("enforcement_policy_information")).orElse(null);
                return Optional.of(new BaseEnforcementData(enforcementCreationTime, appealCreationTime, appealState, enforcementId, enforcementViolationCategory, enforcementSource, enforcementExtraData, enforcementPolicyInformation));
            }

            /**
             * Parses a list of {@link BaseEnforcementData} entries from the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<BaseEnforcementData> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<BaseEnforcementData>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a {@link Geosuspensions} from the given JSON object.
         *
         * <p>The embedded {@code country_codes} array is copied as a list of raw ISO 2-letter
         * strings.
         *
         * @param obj the JSON object to parse
         * @return the parsed entry, or empty when {@code obj} is {@code null}
         */
        static Optional<Geosuspensions> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var baseEnforcementData = BaseEnforcementData.of(obj.getJSONObject("base_enforcement_data")).orElse(null);
            var countryCodesArr = obj.getJSONArray("country_codes");
            List<String> countryCodes;
            if (countryCodesArr == null) {
                countryCodes = List.of();
            } else {
                var codes = new ArrayList<String>(countryCodesArr.size());
                for (var i = 0; i < countryCodesArr.size(); i++) {
                    var code = countryCodesArr.getString(i);
                    if (code != null) {
                        codes.add(code);
                    }
                }
                countryCodes = List.copyOf(codes);
            }
            return Optional.of(new Geosuspensions(baseEnforcementData, countryCodes));
        }

        /**
         * Parses a list of {@link Geosuspensions} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<Geosuspensions> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Geosuspensions>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Parses the response from the raw UTF-8 JSON payload of the {@code <result>} child.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_channel_enforcements} root
     */
    private static Optional<FetchNewsletterEnforcementsMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_channel_enforcements");
        if (root == null) {
            return Optional.empty();
        }

        var profilePictureDeletions = ProfilePictureDeletions.ofArray(root.getJSONArray("profile_picture_deletions"));
        var suspensions = Suspensions.ofArray(root.getJSONArray("suspensions"));
        var violatingMessages = ViolatingMessages.ofArray(root.getJSONArray("violating_messages"));
        var geosuspensions = Geosuspensions.ofArray(root.getJSONArray("geosuspensions"));

        return Optional.of(new FetchNewsletterEnforcementsMexResponse(profilePictureDeletions, suspensions, violatingMessages, geosuspensions));
    }
}
