package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the MEX response of the fetch-newsletter-reports query built by
 * {@link FetchNewsletterReportsMexRequest}.
 *
 * <p>Surfaces one {@link ChannelsReports} per report row in the relay's
 * {@code data.xwa2_channels_reports.channels_reports} array; each row carries the report id, status,
 * creation and last-update timestamps, the channel name and Jid, a polymorphic
 * {@link ChannelsReports.ReportedContentData} payload identifying the offending message, and an
 * optional {@link ChannelsReports.Appeal} block when the offender has appealed.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterReportsJob")
public final class FetchNewsletterReportsMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the parsed list of channel reports, ordered as returned by the relay.
     */
    private final List<ChannelsReports> channelsReports;

    /**
     * Constructs a response wrapping the parsed list of channel reports.
     *
     * @param channelsReports the parsed list of reports
     */
    private FetchNewsletterReportsMexResponse(List<ChannelsReports> channelsReports) {
        this.channelsReports = channelsReports;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser; the returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_channels_reports} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result payload
     */
    public static Optional<FetchNewsletterReportsMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchNewsletterReportsMexResponse::of);
    }

    /**
     * Returns the parsed list of channel reports.
     *
     * <p>The list is ordered as returned by the relay and may be empty when no reports are
     * outstanding against newsletters the local user administers.
     *
     * @return the parsed list, empty when the {@code channels_reports} array was missing or empty
     */
    public List<ChannelsReports> channelsReports() {
        return channelsReports;
    }

    /**
     * Wraps a single entry of the {@code channels_reports} array returned by the relay.
     *
     * <p>Carries the metadata WA Web's moderation UI renders for one outstanding report: the report
     * id, current {@code status}, creation and last-update timestamps, the channel display name and
     * Jid, the {@link ReportedContentData} payload identifying the offending content, and an optional
     * {@link Appeal} block when the offender has filed an appeal.
     */
    public static final class ChannelsReports {
        /**
         * Holds the relay-issued report identifier echoed under {@code report_id}.
         */
        private final String reportId;

        /**
         * Holds the current moderation status echoed under {@code status}.
         */
        private final String status;

        /**
         * Holds the report-creation timestamp in epoch seconds, echoed under {@code creation_time}.
         */
        private final Long creationTime;

        /**
         * Holds the last-update timestamp in epoch seconds, echoed under {@code last_update_time}.
         */
        private final Long lastUpdateTime;

        /**
         * Holds the display name of the reported channel, echoed under {@code channel_name}.
         */
        private final String channelName;

        /**
         * Holds the Jid of the reported channel, echoed under {@code channel_jid}.
         */
        private final String channelJid;

        /**
         * Holds the polymorphic payload identifying the reported content, echoed under
         * {@code reported_content_data}.
         */
        private final ReportedContentData reportedContentData;

        /**
         * Holds the optional appeal record echoed under {@code appeal}.
         */
        private final Appeal appeal;

        /**
         * Constructs a single channel-report entry.
         *
         * @param reportId            the relay-issued report identifier
         * @param status              the current moderation status
         * @param creationTime        the report-creation epoch seconds
         * @param lastUpdateTime      the last-update epoch seconds
         * @param channelName         the reported channel display name
         * @param channelJid          the reported channel Jid
         * @param reportedContentData the polymorphic reported-content payload
         * @param appeal              the optional appeal record
         */
        private ChannelsReports(String reportId, String status, Long creationTime, Long lastUpdateTime, String channelName, String channelJid, ReportedContentData reportedContentData, Appeal appeal) {
            this.reportId = reportId;
            this.status = status;
            this.creationTime = creationTime;
            this.lastUpdateTime = lastUpdateTime;
            this.channelName = channelName;
            this.channelJid = channelJid;
            this.reportedContentData = reportedContentData;
            this.appeal = appeal;
        }

        /**
         * Returns the relay-issued report identifier.
         *
         * @return the {@code report_id} value, or empty when omitted
         */
        public Optional<String> reportId() {
            return Optional.ofNullable(reportId);
        }

        /**
         * Returns the current moderation status of the report.
         *
         * <p>Carries the relay-defined status code (for example {@code "PENDING"},
         * {@code "ACTIONED"}) when present.
         *
         * @return the {@code status} value, or empty when omitted
         */
        public Optional<String> status() {
            return Optional.ofNullable(status);
        }

        /**
         * Returns the moment the report was filed.
         *
         * <p>The underlying value is the wire-level epoch-second integer remapped to an
         * {@link Instant}.
         *
         * @return the {@code creation_time} as an {@link Instant}, or empty when omitted
         */
        public Optional<Instant> creationTime() {
            return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
        }

        /**
         * Returns the moment the report was last updated.
         *
         * <p>The underlying value is the wire-level epoch-second integer remapped to an
         * {@link Instant}.
         *
         * @return the {@code last_update_time} as an {@link Instant}, or empty when omitted
         */
        public Optional<Instant> lastUpdateTime() {
            return Optional.ofNullable(lastUpdateTime).map(Instant::ofEpochSecond);
        }

        /**
         * Returns the display name of the reported channel.
         *
         * @return the {@code channel_name} value, or empty when omitted
         */
        public Optional<String> channelName() {
            return Optional.ofNullable(channelName);
        }

        /**
         * Returns the Jid of the reported channel.
         *
         * @return the {@code channel_jid} value, or empty when omitted
         */
        public Optional<String> channelJid() {
            return Optional.ofNullable(channelJid);
        }

        /**
         * Returns the polymorphic payload identifying the reported content.
         *
         * @return the parsed {@link ReportedContentData}, or empty when omitted
         */
        public Optional<ReportedContentData> reportedContentData() {
            return Optional.ofNullable(reportedContentData);
        }

        /**
         * Returns the appeal record attached to the report.
         *
         * <p>Empty when no appeal has been filed (or the GraphQL envelope omits {@code appeal});
         * otherwise carries the appellant's reason and state.
         *
         * @return the parsed {@link Appeal}, or empty when omitted
         */
        public Optional<Appeal> appeal() {
            return Optional.ofNullable(appeal);
        }

        /**
         * Wraps the polymorphic {@code reported_content_data} payload embedded in each report entry.
         *
         * <p>The relay returns one of three GraphQL inline-fragment shapes keyed by the
         * {@code __typename} discriminator:
         * <ul>
         *   <li>{@code XWA2ChannelServerMsgData} carries {@code server_msg_id}.</li>
         *   <li>{@code XWA2ChannelStatusData} carries {@code server_id}.</li>
         *   <li>{@code XWA2ChannelQuestionResponseData} carries {@code server_response_id},
         *       {@code notify_name} and a nested {@code question_data} fragment of type
         *       {@code XWA2ChannelServerMsgData}.</li>
         * </ul>
         */
        public static final class ReportedContentData {
            /**
             * Holds the GraphQL {@code __typename} discriminator selecting which inline-fragment
             * fields are populated.
             */
            private final String typename;

            /**
             * Holds the {@code server_msg_id} populated on {@code XWA2ChannelServerMsgData} fragments.
             */
            private final String serverMsgId;

            /**
             * Holds the {@code server_id} populated on {@code XWA2ChannelStatusData} fragments.
             */
            private final String serverId;

            /**
             * Holds the {@code server_response_id} populated on
             * {@code XWA2ChannelQuestionResponseData} fragments.
             */
            private final String serverResponseId;

            /**
             * Holds the {@code notify_name} populated on {@code XWA2ChannelQuestionResponseData}
             * fragments.
             */
            private final String notifyName;

            /**
             * Holds the nested {@code question_data} fragment populated on
             * {@code XWA2ChannelQuestionResponseData}.
             */
            private final QuestionData questionData;

            /**
             * Constructs a parsed {@code reported_content_data} value.
             *
             * <p>Only the fields matching the active {@code __typename} fragment are populated.
             *
             * @param typename         the {@code __typename} discriminator
             * @param serverMsgId      the {@code server_msg_id} value
             * @param serverId         the {@code server_id} value
             * @param serverResponseId the {@code server_response_id} value
             * @param notifyName       the {@code notify_name} value
             * @param questionData     the nested {@code question_data} fragment
             */
            private ReportedContentData(String typename, String serverMsgId, String serverId, String serverResponseId, String notifyName, QuestionData questionData) {
                this.typename = typename;
                this.serverMsgId = serverMsgId;
                this.serverId = serverId;
                this.serverResponseId = serverResponseId;
                this.notifyName = notifyName;
                this.questionData = questionData;
            }

            /**
             * Returns the {@code __typename} discriminator.
             *
             * <p>Selects which inline-fragment getters carry data.
             *
             * @return the {@code __typename} value, or empty when omitted
             */
            public Optional<String> typename() {
                return Optional.ofNullable(typename);
            }

            /**
             * Returns the message identifier of the reported channel message.
             *
             * <p>Populated only when {@link #typename()} resolves to {@code XWA2ChannelServerMsgData}.
             *
             * @return the {@code server_msg_id} value, or empty when not applicable to the active
             *         fragment
             */
            public Optional<String> serverMsgId() {
                return Optional.ofNullable(serverMsgId);
            }

            /**
             * Returns the status identifier of the reported channel status.
             *
             * <p>Populated only when {@link #typename()} resolves to {@code XWA2ChannelStatusData}.
             *
             * @return the {@code server_id} value, or empty when not applicable to the active fragment
             */
            public Optional<String> serverId() {
                return Optional.ofNullable(serverId);
            }

            /**
             * Returns the response identifier of the reported question response.
             *
             * <p>Populated only when {@link #typename()} resolves to
             * {@code XWA2ChannelQuestionResponseData}.
             *
             * @return the {@code server_response_id} value, or empty when not applicable to the active
             *         fragment
             */
            public Optional<String> serverResponseId() {
                return Optional.ofNullable(serverResponseId);
            }

            /**
             * Returns the notify-name of the question respondent.
             *
             * <p>Populated only when {@link #typename()} resolves to
             * {@code XWA2ChannelQuestionResponseData}.
             *
             * @return the {@code notify_name} value, or empty when not applicable to the active
             *         fragment
             */
            public Optional<String> notifyName() {
                return Optional.ofNullable(notifyName);
            }

            /**
             * Returns the nested question-message fragment carried by question-response reports.
             *
             * <p>Populated only when {@link #typename()} resolves to
             * {@code XWA2ChannelQuestionResponseData}; identifies the underlying question whose
             * response was reported.
             *
             * @return the parsed {@link QuestionData}, or empty when not applicable to the active
             *         fragment
             */
            public Optional<QuestionData> questionData() {
                return Optional.ofNullable(questionData);
            }

            /**
             * Parses a {@code reported_content_data} fragment from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed value, or empty when {@code obj} is {@code null}
             */
            static Optional<ReportedContentData> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var typename = obj.getString("__typename");
                var serverMsgId = obj.getString("server_msg_id");
                var serverId = obj.getString("server_id");
                var serverResponseId = obj.getString("server_response_id");
                var notifyName = obj.getString("notify_name");
                var questionData = QuestionData.of(obj.getJSONObject("question_data")).orElse(null);
                return Optional.of(new ReportedContentData(typename, serverMsgId, serverId, serverResponseId, notifyName, questionData));
            }

            /**
             * Wraps the {@code question_data} nested fragment carried by
             * {@code XWA2ChannelQuestionResponseData} reports.
             *
             * <p>Identifies the underlying question message that produced the reported response.
             */
            public static final class QuestionData {
                /**
                 * Holds the GraphQL {@code __typename} discriminator on the question fragment.
                 */
                private final String typename;

                /**
                 * Holds the {@code server_msg_id} of the underlying question message.
                 */
                private final String serverMsgId;

                /**
                 * Constructs a parsed {@code question_data} value.
                 *
                 * @param typename    the {@code __typename} discriminator
                 * @param serverMsgId the question message identifier
                 */
                private QuestionData(String typename, String serverMsgId) {
                    this.typename = typename;
                    this.serverMsgId = serverMsgId;
                }

                /**
                 * Returns the {@code __typename} discriminator on the question fragment.
                 *
                 * @return the {@code __typename} value, or empty when omitted
                 */
                public Optional<String> typename() {
                    return Optional.ofNullable(typename);
                }

                /**
                 * Returns the message identifier of the underlying question message.
                 *
                 * @return the {@code server_msg_id} value, or empty when omitted
                 */
                public Optional<String> serverMsgId() {
                    return Optional.ofNullable(serverMsgId);
                }

                /**
                 * Parses a {@code question_data} fragment from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed value, or empty when {@code obj} is {@code null}
                 */
                static Optional<QuestionData> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var typename = obj.getString("__typename");
                    var serverMsgId = obj.getString("server_msg_id");
                    return Optional.of(new QuestionData(typename, serverMsgId));
                }
            }
        }

        /**
         * Wraps the {@code appeal} record attached to reports the offender has appealed.
         *
         * <p>Carries the appellant's stated reason, the appeal state, and the cross-reference
         * identifiers linking the appeal back to its report.
         */
        public static final class Appeal {
            /**
             * Holds the appeal state (for example {@code "PENDING"}, {@code "RESOLVED"}).
             */
            private final String state;

            /**
             * Holds the free-text reason the offender supplied when filing the appeal.
             */
            private final String appealReason;

            /**
             * Holds the appeal-creation timestamp in epoch seconds.
             */
            private final Long creationTime;

            /**
             * Holds the cross-reference to the appealed report's identifier.
             */
            private final String reportId;

            /**
             * Holds the relay-issued identifier of the appeal record itself.
             */
            private final String appealId;

            /**
             * Constructs a parsed {@code appeal} value.
             *
             * @param state        the appeal state
             * @param appealReason the appellant's stated reason
             * @param creationTime the appeal-creation epoch seconds
             * @param reportId     the cross-reference to the appealed report
             * @param appealId     the relay-issued appeal identifier
             */
            private Appeal(String state, String appealReason, Long creationTime, String reportId, String appealId) {
                this.state = state;
                this.appealReason = appealReason;
                this.creationTime = creationTime;
                this.reportId = reportId;
                this.appealId = appealId;
            }

            /**
             * Returns the appeal state.
             *
             * @return the {@code state} value, or empty when omitted
             */
            public Optional<String> state() {
                return Optional.ofNullable(state);
            }

            /**
             * Returns the appellant's stated reason.
             *
             * @return the {@code appeal_reason} value, or empty when omitted
             */
            public Optional<String> appealReason() {
                return Optional.ofNullable(appealReason);
            }

            /**
             * Returns the moment the appeal was filed.
             *
             * <p>The underlying value is the wire-level epoch-second integer remapped to an
             * {@link Instant}.
             *
             * @return the {@code creation_time} as an {@link Instant}, or empty when omitted
             */
            public Optional<Instant> creationTime() {
                return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
            }

            /**
             * Returns the cross-reference to the appealed report.
             *
             * @return the {@code report_id} value, or empty when omitted
             */
            public Optional<String> reportId() {
                return Optional.ofNullable(reportId);
            }

            /**
             * Returns the relay-issued appeal identifier.
             *
             * @return the {@code appeal_id} value, or empty when omitted
             */
            public Optional<String> appealId() {
                return Optional.ofNullable(appealId);
            }

            /**
             * Parses an {@code appeal} record from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed value, or empty when {@code obj} is {@code null}
             */
            static Optional<Appeal> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var state = obj.getString("state");
                var appealReason = obj.getString("appeal_reason");
                var creationTime = obj.getLong("creation_time");
                var reportId = obj.getString("report_id");
                var appealId = obj.getString("appeal_id");
                return Optional.of(new Appeal(state, appealReason, creationTime, reportId, appealId));
            }

            /**
             * Parses every {@code appeal} record in the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed values, empty when {@code arr} is {@code null}
             */
            static List<Appeal> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<Appeal>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a single {@code channels_reports} entry from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed value, or empty when {@code obj} is {@code null}
         */
        static Optional<ChannelsReports> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var reportId = obj.getString("report_id");
            var status = obj.getString("status");
            var creationTime = obj.getLong("creation_time");
            var lastUpdateTime = obj.getLong("last_update_time");
            var channelName = obj.getString("channel_name");
            var channelJid = obj.getString("channel_jid");
            var reportedContentData = ReportedContentData.of(obj.getJSONObject("reported_content_data")).orElse(null);
            var appeal = Appeal.of(obj.getJSONObject("appeal")).orElse(null);
            return Optional.of(new ChannelsReports(reportId, status, creationTime, lastUpdateTime, channelName, channelJid, reportedContentData, appeal));
        }

        /**
         * Parses every {@code channels_reports} entry in the given JSON array.
         *
         * <p>Materialises the {@code data.xwa2_channels_reports.channels_reports} array into a fresh
         * {@link List}; returns {@link List#of()} when {@code arr} is {@code null} so an absent or
         * empty server response surfaces as an empty list.
         *
         * @param arr the JSON array to parse
         * @return the list of parsed values, empty when {@code arr} is {@code null}
         */
        static List<ChannelsReports> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<ChannelsReports>(arr.size());
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
     *         {@code data.xwa2_channels_reports} root
     */
    private static Optional<FetchNewsletterReportsMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_channels_reports");
        if (root == null) {
            return Optional.empty();
        }

        var channelsReports = ChannelsReports.ofArray(root.getJSONArray("channels_reports"));

        return Optional.of(new FetchNewsletterReportsMexResponse(channelsReports));
    }
}
