package com.github.auties00.cobalt.wire.stanza.usync;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.stanza.usync.result.UsyncProtocolError;
import com.github.auties00.cobalt.wire.stanza.usync.result.UsyncProtocolResponse;

/**
 * Sealed sum type of the value a per-user, per-protocol parser returns.
 *
 * <p>Callers pattern-match at either level: switching first on
 * {@link UsyncProtocolResponse} versus {@link UsyncProtocolError} routes the
 * success and error branches separately, while switching directly on a concrete
 * success permit lets a call site that only cares about one protocol reach the
 * typed payload without an intermediate cast.
 *
 * @implNote
 * WA Web discriminates errors by the presence of an {@code errorCode} key; this
 * implementation uses the sealed-interface permits to make the discrimination
 * compile-time exhaustive.
 */
@WhatsAppWebModule(moduleName = "WAWebUsync")
public sealed interface UsyncProtocolResult permits UsyncProtocolResponse, UsyncProtocolError {

}
