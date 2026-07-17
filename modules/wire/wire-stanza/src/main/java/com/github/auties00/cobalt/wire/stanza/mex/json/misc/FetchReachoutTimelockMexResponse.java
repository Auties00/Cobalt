package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Exposes the reachout-timelock enforcement state returned by the
 * {@link FetchReachoutTimelockMexRequest} query.
 *
 * <p>The {@code xwa2_fetch_account_reachout_timelock} envelope carries whether the timelock is
 * active, when the enforcement window ends and how severe the enforcement is. These scalars gate
 * the reach-out UI.
 *
 * @implNote This implementation surfaces {@link #isActive()} as a primitive {@code boolean} that
 * defaults to {@code false} when the relay omits the field, while the two string scalars stay
 * {@link Optional} so their absence is observable.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchReachoutTimelockJob")
public final class FetchReachoutTimelockMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the {@code is_active} scalar reflecting whether the timelock is currently being
     * enforced.
     */
    private final Boolean isActive;

    /**
     * Holds the {@code time_enforcement_ends} scalar marking when the enforcement window expires.
     */
    private final String timeEnforcementEnds;

    /**
     * Holds the {@code enforcement_type} scalar classifying the enforcement severity (for example
     * soft warning, hard block).
     */
    private final String enforcementType;

    /**
     * Constructs a new response wrapping the parsed scalar fields of the
     * {@code xwa2_fetch_account_reachout_timelock} envelope.
     *
     * <p>Instances are produced only by the {@link #of(Stanza)} parser.
     *
     * @param isActive             the {@code is_active} scalar, may be {@code null}
     * @param timeEnforcementEnds  the {@code time_enforcement_ends} scalar, may be {@code null}
     * @param enforcementType      the {@code enforcement_type} scalar, may be {@code null}
     */
    private FetchReachoutTimelockMexResponse(Boolean isActive, String timeEnforcementEnds, String enforcementType) {
        this.isActive = isActive;
        this.timeEnforcementEnds = timeEnforcementEnds;
        this.enforcementType = enforcementType;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>Reads the {@code <result>} child's byte content and routes it through the private
     * byte-level parser. Yields {@link Optional#empty()} when the stanza carries no result or when
     * the {@code data.xwa2_fetch_account_reachout_timelock} envelope is absent.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchReachoutTimelockJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<FetchReachoutTimelockMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchReachoutTimelockMexResponse::of);
    }

    /**
     * Returns whether the reachout timelock is currently being enforced.
     *
     * <p>A {@code null} scalar on the wire collapses to {@code false} so the gate is open by
     * default; callers needing to distinguish "absent" from "explicitly false" must parse the raw
     * JSON themselves.
     *
     * @return {@code true} when the relay set {@code is_active} to true, {@code false} otherwise
     */
    public boolean isActive() {
        return isActive != null && isActive;
    }

    /**
     * Returns the {@code time_enforcement_ends} scalar marking when the enforcement window expires.
     *
     * @return an {@link Optional} containing the enforcement-end timestamp, or
     *         {@link Optional#empty()} if the relay omitted the scalar
     */
    public Optional<String> timeEnforcementEnds() {
        return Optional.ofNullable(timeEnforcementEnds);
    }

    /**
     * Returns the {@code enforcement_type} scalar classifying the enforcement severity.
     *
     * @return an {@link Optional} containing the enforcement type, or {@link Optional#empty()} if
     *         the relay omitted the scalar
     */
    public Optional<String> enforcementType() {
        return Optional.ofNullable(enforcementType);
    }

    /**
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link FetchReachoutTimelockMexResponse}.
     *
     * <p>Routed through {@link #of(Stanza)} after the byte content of the {@code <result>} child is
     * extracted. Yields {@link Optional#empty()} when the envelope, the {@code data} branch, or the
     * {@code xwa2_fetch_account_reachout_timelock} child is absent.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         {@code data.xwa2_fetch_account_reachout_timelock} envelope is absent
     */
    private static Optional<FetchReachoutTimelockMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_fetch_account_reachout_timelock");
        if (root == null) {
            return Optional.empty();
        }

        var isActive = root.getBoolean("is_active");
        var timeEnforcementEnds = root.getString("time_enforcement_ends");
        var enforcementType = root.getString("enforcement_type");

        return Optional.of(new FetchReachoutTimelockMexResponse(isActive, timeEnforcementEnds, enforcementType));
    }
}
