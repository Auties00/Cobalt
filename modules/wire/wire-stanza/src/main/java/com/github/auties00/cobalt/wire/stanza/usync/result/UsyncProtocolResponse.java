package com.github.auties00.cobalt.wire.stanza.usync.result;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocolResult;

/**
 * Groups every protocol-specific success variant a per-user, per-protocol USync parser can return.
 *
 * <p>This is the sibling permit of {@link UsyncProtocolError} under {@link UsyncProtocolResult}:
 * the success branch carries a typed protocol payload, the error branch carries an
 * {@link UsyncProtocolError}. Each permitted variant corresponds to one protocol and is produced
 * by {@link UsyncProtocol#parseUserResult(Stanza)}. A call site that
 * asks for several protocols at once switches on this type to route each result to its handler
 * without an intermediate cast, reaching the concrete payload through
 * {@link UsyncUserResult#getProtocolResult(String)}.
 *
 * @implNote
 * This implementation permits exactly the eleven protocols that ship a parser today:
 * {@link BotProfileResult}, {@link BusinessResult}, {@link ContactResult}, {@link DeviceResult},
 * {@link DisappearingModeResult}, {@link FeatureResult}, {@link LidResult}, {@link PictureResult},
 * {@link StatusResult}, {@link TextStatusResult}, and {@link UsernameResult}. Adding a twelfth
 * parser requires extending this permits list to keep the {@code switch} pattern matching
 * exhaustive.
 */
@WhatsAppWebModule(moduleName = "WAWebUsync")
public sealed interface UsyncProtocolResponse extends UsyncProtocolResult permits
        BotProfileResult,
        BusinessResult,
        ContactResult,
        DeviceResult,
        DisappearingModeResult,
        FeatureResult,
        LidResult,
        PictureResult,
        StatusResult,
        TextStatusResult,
        UsernameResult {
}
