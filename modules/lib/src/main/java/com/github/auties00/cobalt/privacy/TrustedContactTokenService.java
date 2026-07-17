package com.github.auties00.cobalt.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;

import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Computes the lifetime gates for trusted-contact (TC) privacy tokens.
 *
 * <p>A TC token is the privacy-tier credential a contact pair exchanges to opt into
 * privacy-enhanced features such as verified history, identity pin, and call privacy. The token's
 * validity is bucketed: time is divided into fixed-size buckets whose duration and count come from
 * AB props, and a token remains usable while its issue timestamp falls inside the rolling window
 * of recent buckets. The window parameters differ by role; {@link TcTokenMode} selects between the
 * sender-side and receiver-side AB props.
 *
 * <p>Two flows gate on this service: the outgoing-message and call-offer paths check
 * {@link #hasTokenExpired(Instant, TcTokenMode)} before presenting a cached peer token, and the
 * token rotation flow checks {@link #shouldSendNewToken(Instant)} before issuing a fresh token to
 * a peer.
 *
 * <p>{@link LiveTrustedContactTokenService} is the production implementation injected through the
 * client.
 *
 * @implSpec
 * Implementations must be thread-safe; the expected shape is a stateless read over the AB props
 * and the system clock.
 */
public interface TrustedContactTokenService {
    /**
     * Returns whether a token issued at the given timestamp has aged out of the role's validity
     * window and can no longer be presented to the peer.
     *
     * <p>The token is past its window when its issue timestamp falls before the oldest bucket the
     * role's AB props still consider valid; a peer's voip engine rejects a {@code <privacy>} token
     * outside this window with status {@code 70019}.
     *
     * @param tokenTimestamp the token's issue timestamp
     * @param mode           the {@link TcTokenMode}
     * @return {@code true} when the token is past its validity window
     * @throws NullPointerException if any argument is {@code null}
     */
    boolean hasTokenExpired(Instant tokenTimestamp, TcTokenMode mode);

    /**
     * Returns whether the sender should issue a fresh TC token to the recipient.
     *
     * <p>A new token is required when no prior token exists or when the prior token falls into an
     * earlier sender bucket than the current time. The sender lifetime is read from
     * {@link ABProp#TCTOKEN_DURATION_SENDER}, uncapped unlike the receiver-side window.
     *
     * @param tokenTimestamp the prior sender-token timestamp, or {@code null} when no token has
     *                       been issued
     * @return {@code true} when a new token should be sent
     */
    boolean shouldSendNewToken(Instant tokenTimestamp);

    /**
     * Encodes a raw TC token as a standard base64 string suitable for inlining in a MEX (GraphQL)
     * privacy-token argument.
     *
     * <p>Used by callers that need to pass the token through a JSON or GraphQL surface rather than
     * a binary stanza child. The encoding is standard base64 with padding.
     *
     * @param tcToken the raw token bytes
     * @return the base64-encoded token
     * @throws NullPointerException if {@code tcToken} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebTrustedContactsUtils", exports = "encodeTcTokenForMex",
            adaptation = WhatsAppAdaptation.DIRECT)
    static String encodeTcTokenForMex(byte[] tcToken) {
        return Base64.getEncoder().encodeToString(Objects.requireNonNull(tcToken, "tcToken"));
    }

    /**
     * Identifies the role a TC token plays when its lifetime parameters are looked up.
     *
     * <p>Each role is backed by a distinct pair of {@code tctoken_duration*} and
     * {@code tctoken_num_buckets*} AB props, so the same token bytes have different validity
     * windows depending on whether the local device is the sender or the receiver of the token.
     */
    @WhatsAppWebExport(moduleName = "WAWebTrustedContactsUtils", exports = "TcTokenMode",
            adaptation = WhatsAppAdaptation.DIRECT)
    enum TcTokenMode {
        /**
         * Denotes that the current device is the token's sender.
         *
         * <p>Lifetime parameters are read from {@link ABProp#TCTOKEN_DURATION_SENDER} and
         * {@link ABProp#TCTOKEN_NUM_BUCKETS_SENDER}.
         */
        SENDER,

        /**
         * Denotes that the current device is the token's receiver.
         *
         * <p>Lifetime parameters are read from {@link ABProp#TCTOKEN_DURATION} and
         * {@link ABProp#TCTOKEN_NUM_BUCKETS}.
         */
        RECEIVER
    }
}
