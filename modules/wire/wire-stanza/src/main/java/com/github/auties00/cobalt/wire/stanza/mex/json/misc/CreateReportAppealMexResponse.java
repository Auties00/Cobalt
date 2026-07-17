package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Exposes the parsed {@code xwa2_create_channel_report_appeal_v2} envelope returned by the
 * {@link CreateReportAppealMexRequest} mutation.
 *
 * <p>The envelope projects the updated channel-report record so callers can render an appeal entry
 * on a report management surface. Each top-level scalar is exposed as an {@link Optional}, the two
 * epoch-second timestamps as {@link Instant}, the polymorphic reported-content payload through the
 * nested {@link ReportedContentData} value, and the appeal-specific scalars through the nested
 * {@link Appeal} record.
 */
@WhatsAppWebModule(moduleName = "WAWebMexCreateReportAppealJob")
public final class CreateReportAppealMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the {@code report_id} scalar projected from the envelope.
     */
    private final String reportId;

    /**
     * Holds the {@code status} scalar projected from the envelope.
     */
    private final String status;

    /**
     * Holds the {@code creation_time} scalar (Unix epoch second) projected from the envelope.
     */
    private final Long creationTime;

    /**
     * Holds the {@code last_update_time} scalar (Unix epoch second) projected from the envelope.
     */
    private final Long lastUpdateTime;

    /**
     * Holds the {@code channel_name} scalar projected from the envelope.
     */
    private final String channelName;

    /**
     * Holds the {@code channel_jid} scalar projected from the envelope.
     */
    private final String channelJid;

    /**
     * Holds the parsed {@code reported_content_data} inline-fragment union projecting the content
     * the appeal targets.
     */
    private final ReportedContentData reportedContentData;

    /**
     * Holds the parsed {@code appeal} sub-object carrying the appeal-specific scalars.
     */
    private final Appeal appeal;

    /**
     * Constructs a new response wrapping the parsed scalar and nested fields of the
     * {@code xwa2_create_channel_report_appeal_v2} envelope.
     *
     * <p>Instances are produced only by the {@link #of(Stanza)} parser.
     *
     * @param reportId             the {@code report_id} scalar, may be {@code null}
     * @param status               the {@code status} scalar, may be {@code null}
     * @param creationTime         the {@code creation_time} scalar (Unix epoch second), may be {@code null}
     * @param lastUpdateTime       the {@code last_update_time} scalar (Unix epoch second), may be {@code null}
     * @param channelName          the {@code channel_name} scalar, may be {@code null}
     * @param channelJid           the {@code channel_jid} scalar, may be {@code null}
     * @param reportedContentData  the parsed {@link ReportedContentData} union, may be {@code null}
     * @param appeal               the parsed {@link Appeal} sub-object, may be {@code null}
     */
    private CreateReportAppealMexResponse(String reportId, String status, Long creationTime, Long lastUpdateTime, String channelName, String channelJid, ReportedContentData reportedContentData, Appeal appeal) {
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
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>Reads the {@code <result>} child's byte content and routes it through the private
     * byte-level parser. Yields {@link Optional#empty()} when the stanza carries no result or when
     * the {@code data.xwa2_create_channel_report_appeal_v2} envelope is absent.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexCreateReportAppealJob", exports = "createReportAppeal",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<CreateReportAppealMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(CreateReportAppealMexResponse::of);
    }

    /**
     * Returns the {@code report_id} scalar projected from the envelope.
     *
     * @return an {@link Optional} containing the report identifier, or {@link Optional#empty()} if
     *         the relay omitted the scalar
     */
    public Optional<String> reportId() {
        return Optional.ofNullable(reportId);
    }

    /**
     * Returns the {@code status} scalar projected from the envelope.
     *
     * @return an {@link Optional} containing the report status, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the {@code creation_time} scalar converted from Unix epoch seconds to an
     * {@link Instant}.
     *
     * @return an {@link Optional} containing the creation instant, or {@link Optional#empty()} if
     *         the relay omitted the scalar
     */
    public Optional<Instant> creationTime() {
        return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
    }

    /**
     * Returns the {@code last_update_time} scalar converted from Unix epoch seconds to an
     * {@link Instant}.
     *
     * @return an {@link Optional} containing the last update instant, or {@link Optional#empty()}
     *         if the relay omitted the scalar
     */
    public Optional<Instant> lastUpdateTime() {
        return Optional.ofNullable(lastUpdateTime).map(Instant::ofEpochSecond);
    }

    /**
     * Returns the {@code channel_name} scalar projected from the envelope.
     *
     * @return an {@link Optional} containing the channel name, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> channelName() {
        return Optional.ofNullable(channelName);
    }

    /**
     * Returns the {@code channel_jid} scalar projected from the envelope.
     *
     * @return an {@link Optional} containing the channel JID, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> channelJid() {
        return Optional.ofNullable(channelJid);
    }

    /**
     * Returns the parsed {@code reported_content_data} inline-fragment union projecting the content
     * the appeal targets.
     *
     * @return an {@link Optional} containing the parsed {@link ReportedContentData}, or
     *         {@link Optional#empty()} if the relay omitted the sub-object
     */
    public Optional<ReportedContentData> reportedContentData() {
        return Optional.ofNullable(reportedContentData);
    }

    /**
     * Returns the parsed {@code appeal} sub-object exposing the appeal-specific scalars.
     *
     * @return an {@link Optional} containing the parsed {@link Appeal}, or {@link Optional#empty()}
     *         if the relay omitted the sub-object
     */
    public Optional<Appeal> appeal() {
        return Optional.ofNullable(appeal);
    }

    /**
     * Holds the parsed projection of the polymorphic {@code reported_content_data} sub-object nested
     * under {@code xwa2_create_channel_report_appeal_v2}.
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
     *
     * @implNote This implementation parses every inline-fragment field leniently regardless of the
     * active {@code __typename}; fields absent on the active fragment collapse to
     * {@link Optional#empty()} rather than triggering a discriminator branch.
     */
    public static final class ReportedContentData {
        /**
         * Holds the GraphQL {@code __typename} discriminator selecting which inline-fragment fields
         * are populated.
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
         * Instances are produced only by the {@link #of(JSONObject)} parser.
         *
         * @param typename         the {@code __typename} discriminator, may be {@code null}
         * @param serverMsgId      the {@code server_msg_id} value, may be {@code null}
         * @param serverId         the {@code server_id} value, may be {@code null}
         * @param serverResponseId the {@code server_response_id} value, may be {@code null}
         * @param notifyName       the {@code notify_name} value, may be {@code null}
         * @param questionData     the nested {@code question_data} fragment, may be {@code null}
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
         * @return an {@link Optional} containing the {@code __typename} value, or
         *         {@link Optional#empty()} if the relay omitted the scalar
         */
        public Optional<String> typename() {
            return Optional.ofNullable(typename);
        }

        /**
         * Returns the message identifier of the reported channel message.
         *
         * <p>Populated only when {@link #typename()} resolves to {@code XWA2ChannelServerMsgData}.
         *
         * @return an {@link Optional} containing the {@code server_msg_id} value, or
         *         {@link Optional#empty()} if not applicable to the active fragment
         */
        public Optional<String> serverMsgId() {
            return Optional.ofNullable(serverMsgId);
        }

        /**
         * Returns the status identifier of the reported channel status.
         *
         * <p>Populated only when {@link #typename()} resolves to {@code XWA2ChannelStatusData}.
         *
         * @return an {@link Optional} containing the {@code server_id} value, or
         *         {@link Optional#empty()} if not applicable to the active fragment
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
         * @return an {@link Optional} containing the {@code server_response_id} value, or
         *         {@link Optional#empty()} if not applicable to the active fragment
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
         * @return an {@link Optional} containing the {@code notify_name} value, or
         *         {@link Optional#empty()} if not applicable to the active fragment
         */
        public Optional<String> notifyName() {
            return Optional.ofNullable(notifyName);
        }

        /**
         * Returns the nested question-message fragment carried by question-response reports.
         *
         * <p>Populated only when {@link #typename()} resolves to
         * {@code XWA2ChannelQuestionResponseData}; identifies the underlying question whose response
         * was reported.
         *
         * @return an {@link Optional} containing the parsed {@link QuestionData}, or
         *         {@link Optional#empty()} if not applicable to the active fragment
         */
        public Optional<QuestionData> questionData() {
            return Optional.ofNullable(questionData);
        }

        /**
         * Parses a {@code reported_content_data} fragment from the given JSON object.
         *
         * <p>Used by the enclosing {@link CreateReportAppealMexResponse#of(byte[])} byte-level
         * parser. Reads every inline-fragment field leniently so the fields absent on the active
         * {@code __typename} fragment collapse to {@code null}. Yields {@link Optional#empty()} when
         * {@code obj} is {@code null}.
         *
         * @param obj the JSON object carrying the reported-content union, may be {@code null}
         * @return an {@link Optional} wrapping the parsed value, or {@link Optional#empty()} if
         *         {@code obj} is {@code null}
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
         * Holds the parsed {@code question_data} nested fragment carried by
         * {@code XWA2ChannelQuestionResponseData} reports.
         *
         * <p>Identifies the underlying question message that produced the reported response. The
         * relay projects this fragment as type {@code XWA2ChannelServerMsgData}.
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
             * <p>Instances are produced only by the {@link #of(JSONObject)} parser.
             *
             * @param typename    the {@code __typename} discriminator, may be {@code null}
             * @param serverMsgId the question message identifier, may be {@code null}
             */
            private QuestionData(String typename, String serverMsgId) {
                this.typename = typename;
                this.serverMsgId = serverMsgId;
            }

            /**
             * Returns the {@code __typename} discriminator on the question fragment.
             *
             * @return an {@link Optional} containing the {@code __typename} value, or
             *         {@link Optional#empty()} if the relay omitted the scalar
             */
            public Optional<String> typename() {
                return Optional.ofNullable(typename);
            }

            /**
             * Returns the message identifier of the underlying question message.
             *
             * @return an {@link Optional} containing the {@code server_msg_id} value, or
             *         {@link Optional#empty()} if the relay omitted the scalar
             */
            public Optional<String> serverMsgId() {
                return Optional.ofNullable(serverMsgId);
            }

            /**
             * Parses a {@code question_data} fragment from the given JSON object.
             *
             * <p>Used by {@link ReportedContentData#of(JSONObject)} to hydrate the nested
             * {@code question_data} entry. Yields {@link Optional#empty()} when {@code obj} is
             * {@code null}.
             *
             * @param obj the JSON object carrying the question fragment, may be {@code null}
             * @return an {@link Optional} wrapping the parsed value, or {@link Optional#empty()} if
             *         {@code obj} is {@code null}
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
     * Holds the parsed projection of the {@code appeal} sub-object nested under
     * {@code xwa2_create_channel_report_appeal_v2}.
     *
     * <p>Carries the appeal-specific scalars (state, reason, creation timestamp, original report
     * id, generated appeal id) that drive the appeal timeline entry on a report management surface.
     */
    public static final class Appeal {
        /**
         * Holds the {@code state} scalar of the appeal (for example {@code "PENDING"},
         * {@code "ACCEPTED"}, {@code "REJECTED"}).
         */
        private final String state;

        /**
         * Holds the {@code appeal_reason} scalar carrying the free-form justification submitted by
         * the appellant.
         */
        private final String appealReason;

        /**
         * Holds the {@code creation_time} scalar (Unix epoch second) marking when the appeal record
         * was created.
         */
        private final Long creationTime;

        /**
         * Holds the {@code report_id} scalar identifying the original report this appeal targets.
         */
        private final String reportId;

        /**
         * Holds the {@code appeal_id} scalar carrying the relay-assigned appeal identifier.
         */
        private final String appealId;

        /**
         * Constructs a new appeal record from the parsed scalar fields.
         *
         * <p>Instances are produced only by the {@link #of(JSONObject)} parser.
         *
         * @param state         the {@code state} scalar, may be {@code null}
         * @param appealReason  the {@code appeal_reason} scalar, may be {@code null}
         * @param creationTime  the {@code creation_time} scalar (Unix epoch second), may be {@code null}
         * @param reportId      the {@code report_id} scalar, may be {@code null}
         * @param appealId      the {@code appeal_id} scalar, may be {@code null}
         */
        private Appeal(String state, String appealReason, Long creationTime, String reportId, String appealId) {
            this.state = state;
            this.appealReason = appealReason;
            this.creationTime = creationTime;
            this.reportId = reportId;
            this.appealId = appealId;
        }

        /**
         * Returns the {@code state} scalar of the appeal.
         *
         * @return an {@link Optional} containing the appeal state, or {@link Optional#empty()} if
         *         the relay omitted the scalar
         */
        public Optional<String> state() {
            return Optional.ofNullable(state);
        }

        /**
         * Returns the {@code appeal_reason} scalar.
         *
         * @return an {@link Optional} containing the appeal reason, or {@link Optional#empty()} if
         *         the relay omitted the scalar
         */
        public Optional<String> appealReason() {
            return Optional.ofNullable(appealReason);
        }

        /**
         * Returns the {@code creation_time} scalar converted from Unix epoch seconds to an
         * {@link Instant}.
         *
         * @return an {@link Optional} containing the appeal creation instant, or
         *         {@link Optional#empty()} if the relay omitted the scalar
         */
        public Optional<Instant> creationTime() {
            return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
        }

        /**
         * Returns the {@code report_id} scalar identifying the original report.
         *
         * @return an {@link Optional} containing the report identifier, or {@link Optional#empty()}
         *         if the relay omitted the scalar
         */
        public Optional<String> reportId() {
            return Optional.ofNullable(reportId);
        }

        /**
         * Returns the {@code appeal_id} scalar carrying the relay-assigned appeal identifier.
         *
         * @return an {@link Optional} containing the appeal identifier, or {@link Optional#empty()}
         *         if the relay omitted the scalar
         */
        public Optional<String> appealId() {
            return Optional.ofNullable(appealId);
        }

        /**
         * Parses a single {@link Appeal} record from the given JSON object.
         *
         * <p>Used by the enclosing {@link CreateReportAppealMexResponse#of(byte[])} byte-level
         * parser and by {@link #ofArray(JSONArray)} when an appeal collection is returned. Yields
         * {@link Optional#empty()} when {@code obj} is {@code null}.
         *
         * @param obj the JSON object carrying the appeal scalars, may be {@code null}
         * @return an {@link Optional} wrapping the parsed appeal, or {@link Optional#empty()} if
         *         {@code obj} is {@code null}
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
         * Parses a list of {@link Appeal} records from the given JSON array.
         *
         * <p>Walks every element through {@link #of(JSONObject)}; {@code null} entries inside the
         * array are skipped. A {@code null} array collapses to {@link List#of()}.
         *
         * @param arr the JSON array carrying the appeal records, may be {@code null}
         * @return an unmodifiable list of parsed appeals, empty when {@code arr} is {@code null}
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
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link CreateReportAppealMexResponse}.
     *
     * <p>Routed through {@link #of(Stanza)} after the byte content of the {@code <result>} child is
     * extracted. Yields {@link Optional#empty()} when the envelope, the {@code data} branch, or the
     * {@code xwa2_create_channel_report_appeal_v2} child is absent.
     *
     * @implNote This implementation collapses the {@code reported_content_data} sub-object to
     * {@code null} when that inline-fragment block is absent rather than throwing, leaving the
     * choice of how to surface absence to the caller via {@link Optional}.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         {@code data.xwa2_create_channel_report_appeal_v2} envelope is absent
     */
    private static Optional<CreateReportAppealMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_create_channel_report_appeal_v2");
        if (root == null) {
            return Optional.empty();
        }

        var reportId = root.getString("report_id");
        var status = root.getString("status");
        var creationTime = root.getLong("creation_time");
        var lastUpdateTime = root.getLong("last_update_time");
        var channelName = root.getString("channel_name");
        var channelJid = root.getString("channel_jid");
        var reportedContentData = ReportedContentData.of(root.getJSONObject("reported_content_data")).orElse(null);
        var appeal = Appeal.of(root.getJSONObject("appeal")).orElse(null);

        return Optional.of(new CreateReportAppealMexResponse(reportId, status, creationTime, lastUpdateTime, channelName, channelJid, reportedContentData, appeal));
    }
}
