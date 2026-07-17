package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Exposes the FMX integrity-signal flags returned by the {@link FetchIntegritySignalsMexRequest}
 * query.
 *
 * <p>The {@code is_new_account} and {@code is_suspicious_start_chat} flags are read from the first
 * {@code xwa2_fetch_wa_users} entry's {@code integrity_signals_info} sub-object and drive the FMX
 * safety nudges in the chat composer. Both flags are surfaced as {@link Optional} containers so
 * their absence on the wire is observable.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchIntegritySignals")
public final class FetchIntegritySignalsMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the {@code is_new_account} scalar projected from the first user entry's
     * {@code integrity_signals_info} sub-object.
     */
    private final Boolean isNewAccount;

    /**
     * Holds the {@code is_suspicious_start_chat} scalar projected from the first user entry's
     * {@code integrity_signals_info} sub-object.
     */
    private final Boolean isSuspicious;

    /**
     * Constructs a new response wrapping the two boolean scalars parsed from the
     * {@code integrity_signals_info} sub-object.
     *
     * <p>Instances are produced only by the {@link #of(Stanza)} parser.
     *
     * @param isNewAccount  the {@code is_new_account} scalar, may be {@code null}
     * @param isSuspicious  the {@code is_suspicious_start_chat} scalar, may be {@code null}
     */
    private FetchIntegritySignalsMexResponse(Boolean isNewAccount, Boolean isSuspicious) {
        this.isNewAccount = isNewAccount;
        this.isSuspicious = isSuspicious;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>Reads the {@code <result>} child's byte content and routes it through the private
     * byte-level parser. Yields {@link Optional#empty()} when the stanza carries no result, when
     * the {@code xwa2_fetch_wa_users} array is missing or empty, or when the first user entry's
     * {@code integrity_signals_info} sub-object is absent.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchIntegritySignals", exports = "fetchIntegritySignals",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<FetchIntegritySignalsMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchIntegritySignalsMexResponse::of);
    }

    /**
     * Returns the {@code is_new_account} scalar reflecting whether the relay considers the target
     * user to be a freshly registered account.
     *
     * @return an {@link Optional} containing the new-account flag, or {@link Optional#empty()} if
     *         the relay omitted the scalar
     */
    public Optional<Boolean> isNewAccount() {
        return Optional.ofNullable(isNewAccount);
    }

    /**
     * Returns the {@code is_suspicious_start_chat} scalar reflecting whether the relay considers
     * starting a chat with the target user to be suspicious.
     *
     * @return an {@link Optional} containing the suspicious-start flag, or {@link Optional#empty()}
     *         if the relay omitted the scalar
     */
    public Optional<Boolean> isSuspicious() {
        return Optional.ofNullable(isSuspicious);
    }

    /**
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link FetchIntegritySignalsMexResponse}.
     *
     * <p>Routed through {@link #of(Stanza)} after the byte content of the {@code <result>} child is
     * extracted. Yields {@link Optional#empty()} when the envelope, the {@code xwa2_fetch_wa_users}
     * array (empty array included), the first user entry, or the {@code integrity_signals_info}
     * sub-object is absent.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         envelope is missing
     */
    private static Optional<FetchIntegritySignalsMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var rootArr = data.getJSONArray("xwa2_fetch_wa_users");
        if (rootArr == null || rootArr.isEmpty()) {
            return Optional.empty();
        }

        var first = rootArr.getJSONObject(0);
        if (first == null) {
            return Optional.empty();
        }

        var info = first.getJSONObject("integrity_signals_info");
        if (info == null) {
            return Optional.empty();
        }

        var isNewAccount = info.getBoolean("is_new_account");
        var isSuspicious = info.getBoolean("is_suspicious_start_chat");

        return Optional.of(new FetchIntegritySignalsMexResponse(isNewAccount, isSuspicious));
    }
}
