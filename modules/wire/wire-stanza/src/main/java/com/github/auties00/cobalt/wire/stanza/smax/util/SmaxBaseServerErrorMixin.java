package com.github.auties00.cobalt.wire.stanza.smax.util;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Parses the {@code ClientError} and {@code ServerError} envelope shapes produced by every SMAX RPC.
 *
 * <p>Every WA Web SMAX domain ships its own per-domain base-server-error and
 * bad-request/internal-server-error mixins; they all follow the same shape (assert the {@code <iq>}
 * description, validate the envelope, extract the {@code <error code text/>} child, gate on the code
 * range). This class collapses every per-domain copy into two helpers: {@link #parseServerError(Stanza, Stanza)}
 * for codes greater than or equal to {@code 500} and {@link #parseClientError(Stanza, Stanza)} for the
 * complementary range. Per-domain code-to-semantic mappings stay at the per-RPC parser, but the
 * envelope and range checks live here so the near-identical WA Web copies are not duplicated.
 *
 * @implNote
 * This implementation lives outside the per-domain response classes so the sealed disjunctions stay
 * focused on RPC-specific data shaping. The {@code @WhatsAppWebModule} list enumerates every WA Web
 * module the helper adapts so the source manifest still surfaces the per-domain provenance.
 */
@WhatsAppWebModule(moduleName = "WASmaxInGroupsBaseServerErrorMixin")
@WhatsAppWebModule(moduleName = "WASmaxInGroupsServerErrors")
@WhatsAppWebModule(moduleName = "WASmaxInAbPropsIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInAbPropsIQErrorFeatureNotImplementedMixin")
@WhatsAppWebModule(moduleName = "WASmaxInAbPropsIQErrorInternalServerErrorMixin")
@WhatsAppWebModule(moduleName = "WASmaxInAbPropsNoRetryErrors")
@WhatsAppWebModule(moduleName = "WASmaxInAbPropsIQErrorBadRequestOrFeatureNotImplementedMixinGroup")
@WhatsAppWebModule(moduleName = "WASmaxInAccountIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenIQErrorForbiddenMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenIQErrorInternalServerErrorMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenIQErrorServiceUnavailableMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizAccessTokenHackBaseIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountHackBaseIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizLinkingHackBaseIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageHackBaseIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBugReportingHackBaseIQErrorResponseMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBugReportingIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBugReportingIQErrorInternalServerErrorMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorForbiddenMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorInternalServerErrorMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountIQErrorServiceUnavailableMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdIQErrorForbiddenMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdIQErrorInternalServerErrorMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdIQErrorServiceUnavailableMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizLinkingIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizLinkingIQErrorForbiddenMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizLinkingIQErrorInternalServerErrorMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizSettingsIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBotIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBotIQErrorForbiddenMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBotIQErrorInternalServerErrorMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBotIQErrorNotAllowedMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizSettingsIQErrorFeatureNotImplementedMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizSettingsIQErrorInternalServerErrorMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizSettingsIQErrorServiceUnavailableMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageIQErrorForbiddenMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageIQErrorInternalServerErrorMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageIQErrorServiceUnavailableMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageIQErrorBadRequestOrForbiddenOrInternalServerErrorOrServiceUnavailableMixinGroup")
@WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorFeatureNotImplementedMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorForbiddenMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorInternalServerErrorMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorNotAcceptableMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorRateOverlimitMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackUpdatePreferenceReqErrors")
@WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackUpdatePreferenceServerErrors")
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsIQErrorFeatureNotImplementedMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsIQErrorForbiddenMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsIQErrorInternalServerErrorMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsIQErrorNotAcceptableMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsIQErrorNotAllowedMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsIQErrorRateOverlimitMixin")
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsGetBlocklistErrors")
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsServerErrors")
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsUpdateBlocklistErrors")
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsUpdateOptoutErrors")
@WhatsAppWebModule(moduleName = "WASmaxInGroupsIQErrorNotAcceptableMixin")
@WhatsAppWebModule(moduleName = "WASmaxInGroupsIQErrorAlreadyExistsMixin")
@WhatsAppWebModule(moduleName = "WASmaxInGroupsIQErrorBadRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxInGroupsIQErrorConflictMixin")
@WhatsAppWebModule(moduleName = "WASmaxInGroupsIQErrorFallbackClientMixin")
@WhatsAppWebModule(moduleName = "WASmaxInGroupsIQErrorGoneMixin")
@WhatsAppWebModule(moduleName = "WASmaxInGroupsIQErrorItemNotFoundMixin")
@WhatsAppWebModule(moduleName = "WASmaxInGroupsIQErrorNotAllowedMixin")
@WhatsAppWebModule(moduleName = "WASmaxInGroupsIQErrorNotAuthorizedMixin")
@WhatsAppWebModule(moduleName = "WASmaxInGroupsIQErrorParentLinkedGroupsParticipantsResourceLimitMixin")
@WhatsAppWebModule(moduleName = "WASmaxInStatsIQErrorNotAcceptableMixin")
public final class SmaxBaseServerErrorMixin {

    /**
     * Refuses instantiation of the static-only utility.
     *
     * <p>The class exposes only static helpers; the throwing constructor guards against reflective
     * instantiation.
     */
    private SmaxBaseServerErrorMixin() {
        throw new AssertionError("SmaxBaseServerErrorMixin cannot be instantiated");
    }

    /**
     * Tries to parse a server-error envelope (codes greater than or equal to {@code 500}).
     *
     * <p>Every per-RPC {@code ServerError.of(...)} factory calls this. Returns
     * {@link Optional#empty()} when the envelope check fails (wrong description, wrong {@code type},
     * id/from echo mismatch), when the {@code <error>} child is missing or malformed, or when the
     * parsed code is below the {@code 500} threshold that distinguishes server-side from client-side
     * errors; otherwise returns the parsed {@link SmaxIqErrorResponseMixin.Envelope}.
     *
     * @implNote
     * This implementation delegates the envelope validation to
     * {@link SmaxIqErrorResponseMixin#validate(Stanza, Stanza)} and the {@code <error>} extraction to
     * {@link SmaxIqErrorResponseMixin#parseError(Stanza)}, then gates on {@code code >= 500}; the gate
     * is the complement of {@link #parseClientError(Stanza, Stanza)} so the two helpers partition the
     * non-negative code space.
     *
     * @param reply   the inbound error stanza
     * @param request the outbound request, used to validate echoed identifiers
     * @return an {@link Optional} carrying the parsed {@link SmaxIqErrorResponseMixin.Envelope}, or
     *         empty when the stanza does not match the server-error schema
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsBaseServerErrorMixin",
            exports = "parseBaseServerErrorMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsIQErrorFeatureNotImplementedMixin",
            exports = "parseIQErrorFeatureNotImplementedMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorFeatureNotImplementedMixin",
            exports = "parseIQErrorFeatureNotImplementedMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsIQErrorFeatureNotImplementedMixin",
            exports = "parseIQErrorFeatureNotImplementedMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsIQErrorFeatureNotImplementedMixin",
            exports = "parseIQErrorFeatureNotImplementedMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsIQErrorInternalServerErrorMixin",
            exports = "parseIQErrorInternalServerErrorMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenIQErrorInternalServerErrorMixin",
            exports = "parseIQErrorInternalServerErrorMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountIQErrorInternalServerErrorMixin",
            exports = "parseIQErrorInternalServerErrorMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdIQErrorInternalServerErrorMixin",
            exports = "parseIQErrorInternalServerErrorMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingIQErrorInternalServerErrorMixin",
            exports = "parseIQErrorInternalServerErrorMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageIQErrorInternalServerErrorMixin",
            exports = "parseIQErrorInternalServerErrorMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorInternalServerErrorMixin",
            exports = "parseIQErrorInternalServerErrorMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsIQErrorInternalServerErrorMixin",
            exports = "parseIQErrorInternalServerErrorMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsIQErrorInternalServerErrorMixin",
            exports = "parseIQErrorInternalServerErrorMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBotIQErrorInternalServerErrorMixin",
            exports = "parseIQErrorInternalServerErrorMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBugReportingIQErrorInternalServerErrorMixin",
            exports = "parseIQErrorInternalServerErrorMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenIQErrorServiceUnavailableMixin",
            exports = "parseIQErrorServiceUnavailableMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountIQErrorServiceUnavailableMixin",
            exports = "parseIQErrorServiceUnavailableMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdIQErrorServiceUnavailableMixin",
            exports = "parseIQErrorServiceUnavailableMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageIQErrorServiceUnavailableMixin",
            exports = "parseIQErrorServiceUnavailableMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsIQErrorServiceUnavailableMixin",
            exports = "parseIQErrorServiceUnavailableMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageIQErrorBadRequestOrForbiddenOrInternalServerErrorOrServiceUnavailableMixinGroup",
            exports = "parseIQErrorBadRequestOrForbiddenOrInternalServerErrorOrServiceUnavailableMixinGroup",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsNoRetryErrors",
            exports = "parseNoRetryErrors", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsIQErrorBadRequestOrFeatureNotImplementedMixinGroup",
            exports = "parseIQErrorBadRequestOrFeatureNotImplementedMixinGroup",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenHackBaseIQErrorResponseMixin",
            exports = "parseHackBaseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountHackBaseIQErrorResponseMixin",
            exports = "parseHackBaseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingHackBaseIQErrorResponseMixin",
            exports = "parseHackBaseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageHackBaseIQErrorResponseMixin",
            exports = "parseHackBaseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBugReportingHackBaseIQErrorResponseMixin",
            exports = "parseHackBaseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackUpdatePreferenceServerErrors",
            exports = "parseUpdatePreferenceServerErrors", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsServerErrors",
            exports = "parseServerErrors", adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxIqErrorResponseMixin.Envelope> parseServerError(Stanza reply, Stanza request) {
        Objects.requireNonNull(reply, "reply cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        if (!SmaxIqErrorResponseMixin.validate(reply, request)) {
            return Optional.empty();
        }
        var envelope = SmaxIqErrorResponseMixin.parseError(reply).orElse(null);
        if (envelope == null) {
            return Optional.empty();
        }
        if (envelope.code() < 500) {
            return Optional.empty();
        }
        return Optional.of(envelope);
    }

    /**
     * Tries to parse a client-error envelope (codes below {@code 500}).
     *
     * <p>Every per-RPC {@code ClientError.of(...)} factory calls this; it covers the complementary
     * range to {@link #parseServerError(Stanza, Stanza)}. Returns {@link Optional#empty()} when the
     * envelope check fails or the parsed code is at least {@code 500}; otherwise returns the parsed
     * {@link SmaxIqErrorResponseMixin.Envelope}.
     *
     * @implNote
     * This implementation accepts every non-negative code below {@code 500} without a floor because a
     * handful of WA Web mixins (notably the groups already-exists mixin with {@code code=304})
     * participate in {@code ClientErrors} disjunctions with values below {@code 400}; the open lower
     * bound mirrors the WA Web behaviour.
     *
     * @param reply   the inbound error stanza
     * @param request the outbound request, used to validate echoed identifiers
     * @return an {@link Optional} carrying the parsed {@link SmaxIqErrorResponseMixin.Envelope}, or
     *         empty when the stanza does not match the client-error schema
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsNoRetryErrors",
            exports = "parseNoRetryErrors", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsIQErrorBadRequestOrFeatureNotImplementedMixinGroup",
            exports = "parseIQErrorBadRequestOrFeatureNotImplementedMixinGroup",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInAccountIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizSettingsIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBotIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBugReportingIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorBadRequestMixin",
            exports = "parseIQErrorBadRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorConflictMixin",
            exports = "parseIQErrorConflictMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorFallbackClientMixin",
            exports = "parseIQErrorFallbackClientMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorGoneMixin",
            exports = "parseIQErrorGoneMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorItemNotFoundMixin",
            exports = "parseIQErrorItemNotFoundMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorLockedMixin",
            exports = "parseIQErrorLockedMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorNotAuthorizedMixin",
            exports = "parseIQErrorNotAuthorizedMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorParentLinkedGroupsParticipantsResourceLimitMixin",
            exports = "parseIQErrorParentLinkedGroupsParticipantsResourceLimitMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenIQErrorForbiddenMixin",
            exports = "parseIQErrorForbiddenMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountIQErrorForbiddenMixin",
            exports = "parseIQErrorForbiddenMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdIQErrorForbiddenMixin",
            exports = "parseIQErrorForbiddenMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingIQErrorForbiddenMixin",
            exports = "parseIQErrorForbiddenMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageIQErrorForbiddenMixin",
            exports = "parseIQErrorForbiddenMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorForbiddenMixin",
            exports = "parseIQErrorForbiddenMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsIQErrorForbiddenMixin",
            exports = "parseIQErrorForbiddenMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBotIQErrorForbiddenMixin",
            exports = "parseIQErrorForbiddenMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorForbiddenMixin",
            exports = "parseIQErrorForbiddenMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorNotAcceptableMixin",
            exports = "parseIQErrorNotAcceptableMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorNotAcceptableMixin",
            exports = "parseIQErrorNotAcceptableField", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackIQErrorRateOverlimitMixin",
            exports = "parseIQErrorRateOverlimitMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsIQErrorRateOverlimitMixin",
            exports = "parseIQErrorRateOverlimitMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackUpdatePreferenceReqErrors",
            exports = "parseUpdatePreferenceReqErrors", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetBlocklistErrors",
            exports = "parseGetBlocklistErrors", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateBlocklistErrors",
            exports = "parseUpdateBlocklistErrors", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateOptoutErrors",
            exports = "parseUpdateOptoutErrors", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsIQErrorNotAcceptableMixin",
            exports = "parseIQErrorNotAcceptableMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsIQErrorNotAcceptableMixin",
            exports = "parseIQErrorNotAcceptableField", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsIQErrorNotAllowedMixin",
            exports = "parseIQErrorNotAllowedMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBotIQErrorNotAllowedMixin",
            exports = "parseIQErrorNotAllowedMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorNotAllowedMixin",
            exports = "parseIQErrorNotAllowedMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorNotAcceptableMixin",
            exports = "parseIQErrorNotAcceptableMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorNotAcceptableMixin",
            exports = "parseIQErrorNotAcceptableField", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsIQErrorAlreadyExistsMixin",
            exports = "parseIQErrorAlreadyExistsMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInStatsIQErrorNotAcceptableMixin",
            exports = "parseIQErrorNotAcceptableMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInStatsIQErrorNotAcceptableMixin",
            exports = "parseIQErrorNotAcceptableField", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageIQErrorBadRequestOrForbiddenOrInternalServerErrorOrServiceUnavailableMixinGroup",
            exports = "parseIQErrorBadRequestOrForbiddenOrInternalServerErrorOrServiceUnavailableMixinGroup",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizAccessTokenHackBaseIQErrorResponseMixin",
            exports = "parseHackBaseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountHackBaseIQErrorResponseMixin",
            exports = "parseHackBaseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingHackBaseIQErrorResponseMixin",
            exports = "parseHackBaseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageHackBaseIQErrorResponseMixin",
            exports = "parseHackBaseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBugReportingHackBaseIQErrorResponseMixin",
            exports = "parseHackBaseIQErrorResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxIqErrorResponseMixin.Envelope> parseClientError(Stanza reply, Stanza request) {
        Objects.requireNonNull(reply, "reply cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        if (!SmaxIqErrorResponseMixin.validate(reply, request)) {
            return Optional.empty();
        }
        var envelope = SmaxIqErrorResponseMixin.parseError(reply).orElse(null);
        if (envelope == null) {
            return Optional.empty();
        }
        if (envelope.code() >= 500) {
            return Optional.empty();
        }
        return Optional.of(envelope);
    }
}
