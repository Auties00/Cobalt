package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Exposes the messaging quota counters and per-policy status flags returned by the
 * {@link FetchNewChatMessageCappingInfoMexRequest} query.
 *
 * <p>The {@code xwa2_message_capping_info} envelope carries the counters and flags that drive the
 * individual new-chat messaging capping UI, including the per-message warning banner and the
 * throttling gate. Every scalar is surfaced as an {@link Optional}.
 *
 * @implNote This implementation does not coerce absent counters to {@code "0"} nor map the status
 * flags into a status-type enum; embedders perform that mapping themselves so Cobalt does not bake
 * WhatsApp Web's IDB shape into the response.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewChatMessageCappingInfoJob")
public final class FetchNewChatMessageCappingInfoMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the {@code total_quota} scalar projected from the envelope.
     */
    private final String totalQuota;

    /**
     * Holds the {@code used_quota} scalar projected from the envelope.
     */
    private final String usedQuota;

    /**
     * Holds the {@code cycle_start_timestamp} scalar projected from the envelope.
     */
    private final String cycleStartTimestamp;

    /**
     * Holds the {@code cycle_end_timestamp} scalar projected from the envelope.
     */
    private final String cycleEndTimestamp;

    /**
     * Holds the {@code server_sent_timestamp} scalar projected from the envelope.
     */
    private final String serverSentTimestamp;

    /**
     * Holds the {@code ote_status} scalar (one-time-experience policy state) projected from the
     * envelope.
     */
    private final String oteStatus;

    /**
     * Holds the {@code mv_status} scalar (message-verification policy state) projected from the
     * envelope.
     */
    private final String mvStatus;

    /**
     * Holds the {@code capping_status} scalar reflecting the active throttle verdict, projected
     * from the envelope.
     */
    private final String cappingStatus;

    /**
     * Constructs a new response wrapping the parsed scalar fields of the
     * {@code xwa2_message_capping_info} envelope.
     *
     * <p>Instances are produced only by the {@link #of(Stanza)} parser.
     *
     * @param totalQuota          the {@code total_quota} scalar, may be {@code null}
     * @param usedQuota           the {@code used_quota} scalar, may be {@code null}
     * @param cycleStartTimestamp the {@code cycle_start_timestamp} scalar, may be {@code null}
     * @param cycleEndTimestamp   the {@code cycle_end_timestamp} scalar, may be {@code null}
     * @param serverSentTimestamp the {@code server_sent_timestamp} scalar, may be {@code null}
     * @param oteStatus           the {@code ote_status} scalar, may be {@code null}
     * @param mvStatus            the {@code mv_status} scalar, may be {@code null}
     * @param cappingStatus       the {@code capping_status} scalar, may be {@code null}
     */
    private FetchNewChatMessageCappingInfoMexResponse(String totalQuota, String usedQuota, String cycleStartTimestamp, String cycleEndTimestamp, String serverSentTimestamp, String oteStatus, String mvStatus, String cappingStatus) {
        this.totalQuota = totalQuota;
        this.usedQuota = usedQuota;
        this.cycleStartTimestamp = cycleStartTimestamp;
        this.cycleEndTimestamp = cycleEndTimestamp;
        this.serverSentTimestamp = serverSentTimestamp;
        this.oteStatus = oteStatus;
        this.mvStatus = mvStatus;
        this.cappingStatus = cappingStatus;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>Reads the {@code <result>} child's byte content and routes it through the private
     * byte-level parser. Yields {@link Optional#empty()} when the stanza carries no result or when
     * the {@code data.xwa2_message_capping_info} envelope is absent.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchNewChatMessageCappingInfoJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<FetchNewChatMessageCappingInfoMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchNewChatMessageCappingInfoMexResponse::of);
    }

    /**
     * Returns the {@code total_quota} scalar, the total number of new chats the account may
     * initiate in the current billing cycle.
     *
     * @return an {@link Optional} containing the total quota, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> totalQuota() {
        return Optional.ofNullable(totalQuota);
    }

    /**
     * Returns the {@code used_quota} scalar, the number of new chats already initiated in the
     * current billing cycle.
     *
     * @return an {@link Optional} containing the used quota, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> usedQuota() {
        return Optional.ofNullable(usedQuota);
    }

    /**
     * Returns the {@code cycle_start_timestamp} scalar marking when the current billing cycle
     * began.
     *
     * @return an {@link Optional} containing the cycle-start timestamp, or {@link Optional#empty()}
     *         if the relay omitted the scalar
     */
    public Optional<String> cycleStartTimestamp() {
        return Optional.ofNullable(cycleStartTimestamp);
    }

    /**
     * Returns the {@code cycle_end_timestamp} scalar marking when the current billing cycle
     * expires.
     *
     * @return an {@link Optional} containing the cycle-end timestamp, or {@link Optional#empty()}
     *         if the relay omitted the scalar
     */
    public Optional<String> cycleEndTimestamp() {
        return Optional.ofNullable(cycleEndTimestamp);
    }

    /**
     * Returns the {@code server_sent_timestamp} scalar marking when the relay produced this
     * snapshot.
     *
     * @return an {@link Optional} containing the server-sent timestamp, or {@link Optional#empty()}
     *         if the relay omitted the scalar
     */
    public Optional<String> serverSentTimestamp() {
        return Optional.ofNullable(serverSentTimestamp);
    }

    /**
     * Returns the {@code ote_status} scalar reflecting the one-time-experience policy state.
     *
     * @return an {@link Optional} containing the OTE status, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> oteStatus() {
        return Optional.ofNullable(oteStatus);
    }

    /**
     * Returns the {@code mv_status} scalar reflecting the message-verification policy state.
     *
     * @return an {@link Optional} containing the MV status, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> mvStatus() {
        return Optional.ofNullable(mvStatus);
    }

    /**
     * Returns the {@code capping_status} scalar reflecting the active throttle verdict.
     *
     * @return an {@link Optional} containing the capping status, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> cappingStatus() {
        return Optional.ofNullable(cappingStatus);
    }

    /**
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link FetchNewChatMessageCappingInfoMexResponse}.
     *
     * <p>Routed through {@link #of(Stanza)} after the byte content of the {@code <result>} child is
     * extracted. Yields {@link Optional#empty()} when the envelope, the {@code data} branch, or the
     * {@code xwa2_message_capping_info} child is absent.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         {@code data.xwa2_message_capping_info} envelope is absent
     */
    private static Optional<FetchNewChatMessageCappingInfoMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_message_capping_info");
        if (root == null) {
            return Optional.empty();
        }

        var totalQuota = root.getString("total_quota");
        var usedQuota = root.getString("used_quota");
        var cycleStartTimestamp = root.getString("cycle_start_timestamp");
        var cycleEndTimestamp = root.getString("cycle_end_timestamp");
        var serverSentTimestamp = root.getString("server_sent_timestamp");
        var oteStatus = root.getString("ote_status");
        var mvStatus = root.getString("mv_status");
        var cappingStatus = root.getString("capping_status");

        return Optional.of(new FetchNewChatMessageCappingInfoMexResponse(totalQuota, usedQuota, cycleStartTimestamp, cycleEndTimestamp, serverSentTimestamp, oteStatus, mvStatus, cappingStatus));
    }
}
