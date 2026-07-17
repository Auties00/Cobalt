package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed family of inbound reply variants produced by the relay in response to a
 * {@link SmaxGetLinkedAccountsRequest}.
 * <p>
 * Each variant projects a distinct outcome of the SMB business-linking enumeration bridge:
 * {@link Success} carries up to four typed identity projections, {@link Forbidden} captures
 * the documented {@code (403, "forbidden")} rejection raised when SMB linking is disabled,
 * {@link ClientError} carries any other {@code 4xx}, and {@link ServerError} carries a
 * transient {@code 5xx} relay failure. The {@link Forbidden}/{@link ClientError} split is
 * required because the WA Web pipeline treats forbidden as a non-error null return while
 * admitting other {@code 4xx} as a failure.
 */
public sealed interface SmaxGetLinkedAccountsResponse extends SmaxStanza.Response
        permits SmaxGetLinkedAccountsResponse.Success, SmaxGetLinkedAccountsResponse.Forbidden,
        SmaxGetLinkedAccountsResponse.ClientError, SmaxGetLinkedAccountsResponse.ServerError {

    /**
     * Tries each variant in priority order and returns the first that parses cleanly.
     * <p>
     * Attempts {@link Success#of(Stanza, Stanza)} first, then {@link Forbidden#of(Stanza, Stanza)},
     * {@link ClientError#of(Stanza, Stanza)} and finally {@link ServerError#of(Stanza, Stanza)}, so
     * that a malformed success stanza falls through to the error variants rather than masking
     * them. {@link Forbidden} is tried before {@link ClientError} because the latter is a
     * catch-all that admits all non-forbidden {@code 4xx} pairs.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza, used to validate echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBizLinkingGetLinkedAccountsRPC",
            exports = "sendGetLinkedAccountsRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGetLinkedAccountsResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var forbidden = Forbidden.of(stanza, request);
        if (forbidden.isPresent()) {
            return forbidden;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Carries up to four optional typed projections of the linked external identities.
     * <p>
     * Each sub-projection is present only when the corresponding identity is currently linked
     * to the active business account. The CTWA ad-creation pipeline consumes {@link #fbPage()}
     * as the primary linked page, falling back to {@link #whatsAppAdIdentity()} for WhatsApp Ads
     * accounts.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingGetLinkedAccountsResponseSuccess")
    final class Success implements SmaxGetLinkedAccountsResponse {
        /**
         * The optional Facebook-page projection.
         */
        private final FbPage fbPage;

        /**
         * The optional Facebook-business projection.
         */
        private final FbBiz fbBiz;

        /**
         * The optional Instagram-professional projection.
         */
        private final IgProfessional igProfessional;

        /**
         * The optional WhatsApp ad-identity projection.
         */
        private final WhatsAppAdIdentity whatsAppAdIdentity;

        /**
         * Constructs a successful reply from the four already-parsed optional projections.
         *
         * @param fbPage             the optional Facebook-page projection; may be {@code null}
         * @param fbBiz              the optional Facebook-business projection; may be {@code null}
         * @param igProfessional     the optional Instagram-professional projection; may be {@code null}
         * @param whatsAppAdIdentity the optional WhatsApp ad-identity projection; may be {@code null}
         */
        public Success(FbPage fbPage, FbBiz fbBiz,
                       IgProfessional igProfessional,
                       WhatsAppAdIdentity whatsAppAdIdentity) {
            this.fbPage = fbPage;
            this.fbBiz = fbBiz;
            this.igProfessional = igProfessional;
            this.whatsAppAdIdentity = whatsAppAdIdentity;
        }

        /**
         * Returns the optional Facebook-page projection.
         *
         * @return an {@link Optional} carrying the projection, or empty when the relay omitted the {@code <fb_page/>} child
         */
        public Optional<FbPage> fbPage() {
            return Optional.ofNullable(fbPage);
        }

        /**
         * Returns the optional Facebook-business projection.
         *
         * @return an {@link Optional} carrying the projection, or empty when the relay omitted the {@code <fb_biz/>} child
         */
        public Optional<FbBiz> fbBiz() {
            return Optional.ofNullable(fbBiz);
        }

        /**
         * Returns the optional Instagram-professional projection.
         *
         * @return an {@link Optional} carrying the projection, or empty when the relay omitted the {@code <ig_professional/>} child
         */
        public Optional<IgProfessional> igProfessional() {
            return Optional.ofNullable(igProfessional);
        }

        /**
         * Returns the optional WhatsApp ad-identity projection.
         *
         * @return an {@link Optional} carrying the projection, or empty when the relay omitted the {@code <whatsapp_ad_identity/>} child
         */
        public Optional<WhatsAppAdIdentity> whatsAppAdIdentity() {
            return Optional.ofNullable(whatsAppAdIdentity);
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         * <p>
         * Enforces the {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} envelope check
         * first, then walks the {@code <linked_accounts/>} parent and dispatches each optional
         * child to its typed parser. A present child that fails to parse fails the whole parse
         * while a missing child is admitted.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingGetLinkedAccountsResponseSuccess",
                exports = "parseGetLinkedAccountsResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var linkedAccounts = stanza.getChild("linked_accounts").orElse(null);
            if (linkedAccounts == null) {
                return Optional.empty();
            }
            FbPage fbPage = null;
            var fbPageNode = linkedAccounts.getChild("fb_page").orElse(null);
            if (fbPageNode != null) {
                var parsed = FbPage.of(fbPageNode);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                fbPage = parsed.get();
            }
            FbBiz fbBiz = null;
            var fbBizNode = linkedAccounts.getChild("fb_biz").orElse(null);
            if (fbBizNode != null) {
                var parsed = FbBiz.of(fbBizNode);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                fbBiz = parsed.get();
            }
            IgProfessional igProfessional = null;
            var igProfNode = linkedAccounts.getChild("ig_professional").orElse(null);
            if (igProfNode != null) {
                var parsed = IgProfessional.of(igProfNode);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                igProfessional = parsed.get();
            }
            WhatsAppAdIdentity whatsAppAdIdentity = null;
            var waaiNode = linkedAccounts.getChild("whatsapp_ad_identity").orElse(null);
            if (waaiNode != null) {
                var parsed = WhatsAppAdIdentity.of(waaiNode);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                whatsAppAdIdentity = parsed.get();
            }
            return Optional.of(new Success(fbPage, fbBiz, igProfessional, whatsAppAdIdentity));
        }

        /**
         * Compares this reply to another object for value equality across the four projections.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Success} with equal projections
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.fbPage, that.fbPage)
                    && Objects.equals(this.fbBiz, that.fbBiz)
                    && Objects.equals(this.igProfessional, that.igProfessional)
                    && Objects.equals(this.whatsAppAdIdentity, that.whatsAppAdIdentity);
        }

        /**
         * Returns a hash code derived from the four projections.
         *
         * @return the combined hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(fbPage, fbBiz, igProfessional, whatsAppAdIdentity);
        }

        /**
         * Returns a debug rendering listing the four projections.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetLinkedAccountsResponse.Success[fbPage=" + fbPage
                    + ", fbBiz=" + fbBiz
                    + ", igProfessional=" + igProfessional
                    + ", whatsAppAdIdentity=" + whatsAppAdIdentity + ']';
        }

        /**
         * Carries the linked Facebook page identity and its assorted sub-states.
         * <p>
         * Drives the SMB linked-pages settings surface and the CTWA ad-creation pipeline's
         * primary identity selection; the {@link #adStatus()} mixin lets the ad-creation flow
         * short-circuit the binding nonce handshake when the page already has an active CTWA ad.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingGetLinkedAccountsResponseSuccess")
        @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingFBPageResponseBaseMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingHasDisplayNameMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingHasProfilePictureMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingHasShowOnProfileMixin")
        public static final class FbPage {
            /**
             * The Facebook-page identifier.
             */
            private final String id;

            /**
             * The display-name element-content.
             */
            private final String displayName;

            /**
             * The mandatory ad-status mixin projection carrying the two boolean flags
             * {@code has_created_ad} and {@code has_active_ctwa_ad}.
             */
            private final SmaxAdStatusMixin adStatus;

            /**
             * The optional {@code <profile_sync state/>} state.
             */
            private final SmaxGetLinkedAccountsDisableImportState profileSyncState;

            /**
             * The mandatory {@code <whatsapp_as_page_button state/>} state.
             */
            private final SmaxGetLinkedAccountsOffOnState whatsAppAsPageButtonState;

            /**
             * The optional profile-picture URL element-content; {@code null} when the upstream
             * profile-picture mixin failed and the result was swallowed.
             */
            private final String profilePictureUrl;

            /**
             * The optional profile-picture blob; {@code null} when omitted.
             */
            private final byte[] profilePictureBytes;

            /**
             * The optional {@code <show_on_profile/>} content flag; {@code null} when the
             * upstream show-on-profile mixin failed and the result was swallowed.
             */
            private final SmaxGetLinkedAccountsFalseTrueFlag showOnProfile;

            /**
             * Constructs a projection from the parsed page fields.
             *
             * @param id                        the page id; never {@code null}
             * @param displayName               the display name; never {@code null}
             * @param adStatus                  the mandatory ad-status mixin; never {@code null}
             * @param profileSyncState          the optional profile-sync state; may be {@code null}
             * @param whatsAppAsPageButtonState the WhatsApp-as-page-button state; never {@code null}
             * @param profilePictureUrl         the optional profile picture URL; may be {@code null}
             * @param profilePictureBytes       the optional profile picture bytes; may be {@code null}
             * @param showOnProfile             the optional show-on-profile flag; may be {@code null}
             * @throws NullPointerException if {@code id}, {@code displayName}, {@code adStatus} or {@code whatsAppAsPageButtonState} is {@code null}
             */
            public FbPage(String id, String displayName, SmaxAdStatusMixin adStatus,
                          SmaxGetLinkedAccountsDisableImportState profileSyncState,
                          SmaxGetLinkedAccountsOffOnState whatsAppAsPageButtonState,
                          String profilePictureUrl, byte[] profilePictureBytes,
                          SmaxGetLinkedAccountsFalseTrueFlag showOnProfile) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
                this.adStatus = Objects.requireNonNull(adStatus, "adStatus cannot be null");
                this.profileSyncState = profileSyncState;
                this.whatsAppAsPageButtonState = Objects.requireNonNull(whatsAppAsPageButtonState,
                        "whatsAppAsPageButtonState cannot be null");
                this.profilePictureUrl = profilePictureUrl;
                this.profilePictureBytes = profilePictureBytes;
                this.showOnProfile = showOnProfile;
            }

            /**
             * Returns the Facebook-page identifier.
             *
             * @return the id; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the page display name.
             *
             * @return the name; never {@code null}
             */
            public String displayName() {
                return displayName;
            }

            /**
             * Returns the mandatory ad-status mixin projection.
             *
             * @return the projection; never {@code null}
             */
            public SmaxAdStatusMixin adStatus() {
                return adStatus;
            }

            /**
             * Returns the optional profile-sync state.
             *
             * @return an {@link Optional} carrying the state, or empty when the relay omitted the {@code <profile_sync/>} child
             */
            public Optional<SmaxGetLinkedAccountsDisableImportState> profileSyncState() {
                return Optional.ofNullable(profileSyncState);
            }

            /**
             * Returns the WhatsApp-as-page-button state.
             *
             * @return the state; never {@code null}
             */
            public SmaxGetLinkedAccountsOffOnState whatsAppAsPageButtonState() {
                return whatsAppAsPageButtonState;
            }

            /**
             * Returns the optional profile-picture URL.
             * <p>
             * Empty when the upstream profile-picture mixin branch failed; see {@link #of(Stanza)}
             * for the swallow semantics inherited from the WA Web caller.
             *
             * @return an {@link Optional} carrying the URL, or empty
             */
            public Optional<String> profilePictureUrl() {
                return Optional.ofNullable(profilePictureUrl);
            }

            /**
             * Returns the optional profile-picture bytes.
             *
             * @return an {@link Optional} carrying the bytes, or empty
             */
            public Optional<byte[]> profilePictureBytes() {
                return Optional.ofNullable(profilePictureBytes);
            }

            /**
             * Returns the optional show-on-profile flag.
             * <p>
             * Empty when the upstream show-on-profile mixin branch failed; see {@link #of(Stanza)}.
             *
             * @return an {@link Optional} carrying the flag, or empty
             */
            public Optional<SmaxGetLinkedAccountsFalseTrueFlag> showOnProfile() {
                return Optional.ofNullable(showOnProfile);
            }

            /**
             * Tries to parse the projection from the given stanza.
             * <p>
             * Walks the four mandatory positions (tag assertion, {@code id} attribute,
             * {@code <display_name>} content, {@code adStatus} mixin via
             * {@link SmaxAdStatusMixin#of(Stanza)}) and the mandatory
             * {@code <whatsapp_as_page_button state>} attribute, then admits three optional
             * mixins. The profile-sync state is hard-required when its child is present, while
             * the {@code <profile_picture>} and {@code <show_on_profile>} branches leave their
             * fields {@code null} on any sub-failure.
             *
             * @implNote This implementation mirrors WA Web's upstream {@code i.success ? i.value : null}
             * wrapping by leaving the {@code <profile_picture>} and {@code <show_on_profile>}
             * fields {@code null} on sub-failure rather than propagating {@link Optional#empty()}
             * to the parent parse.
             * @param stanza the {@code <fb_page/>} stanza
             * @return an {@link Optional} carrying the projection, or empty when the stanza does not match the documented schema
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingGetLinkedAccountsResponseSuccess",
                    exports = "parseGetLinkedAccountsResponseSuccessLinkedAccountsFbPage",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingFBPageResponseBaseMixin",
                    exports = "parseFBPageResponseBaseMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingFBPageResponseBaseMixin",
                    exports = "parseFBPageResponseBaseProfileSync",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingHasProfilePictureMixin",
                    exports = "parseHasProfilePictureMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingHasProfilePictureMixin",
                    exports = "parseHasProfilePictureProfilePictureBytes",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingHasShowOnProfileMixin",
                    exports = "parseHasShowOnProfileMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<FbPage> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("fb_page")) {
                    return Optional.empty();
                }
                var id = stanza.getAttributeAsString("id").orElse(null);
                if (id == null) {
                    return Optional.empty();
                }
                var displayNameNode = stanza.getChild("display_name").orElse(null);
                if (displayNameNode == null) {
                    return Optional.empty();
                }
                var displayName = displayNameNode.toContentString().orElse(null);
                if (displayName == null) {
                    return Optional.empty();
                }
                var adStatus = SmaxAdStatusMixin.of(stanza).orElse(null);
                if (adStatus == null) {
                    return Optional.empty();
                }
                var buttonNode = stanza.getChild("whatsapp_as_page_button").orElse(null);
                if (buttonNode == null) {
                    return Optional.empty();
                }
                var buttonStateStr = buttonNode.getAttributeAsString("state").orElse(null);
                var buttonState = SmaxGetLinkedAccountsOffOnState.of(buttonStateStr).orElse(null);
                if (buttonState == null) {
                    return Optional.empty();
                }
                SmaxGetLinkedAccountsDisableImportState profileSyncState = null;
                var profileSyncNode = stanza.getChild("profile_sync").orElse(null);
                if (profileSyncNode != null) {
                    var stateStr = profileSyncNode.getAttributeAsString("state").orElse(null);
                    profileSyncState = SmaxGetLinkedAccountsDisableImportState.of(stateStr).orElse(null);
                    if (profileSyncState == null) {
                        return Optional.empty();
                    }
                }
                String profilePictureUrl = null;
                byte[] profilePictureBytes = null;
                var profilePicture = stanza.getChild("profile_picture").orElse(null);
                if (profilePicture != null) {
                    var urlNode = profilePicture.getChild("url").orElse(null);
                    if (urlNode != null) {
                        var url = urlNode.toContentString().orElse(null);
                        if (url != null) {
                            profilePictureUrl = url;
                            var bytesNode = profilePicture.getChild("bytes").orElse(null);
                            if (bytesNode != null) {
                                profilePictureBytes = bytesNode.toContentBytes().orElse(null);
                            }
                        }
                    }
                }
                SmaxGetLinkedAccountsFalseTrueFlag showOnProfile = null;
                var showNode = stanza.getChild("show_on_profile").orElse(null);
                if (showNode != null) {
                    var contentStr = showNode.toContentString().orElse(null);
                    if (contentStr != null) {
                        showOnProfile = SmaxGetLinkedAccountsFalseTrueFlag.of(contentStr).orElse(null);
                    }
                }
                return Optional.of(new FbPage(id, displayName, adStatus, profileSyncState,
                        buttonState, profilePictureUrl, profilePictureBytes, showOnProfile));
            }

            /**
             * Compares this projection to another object for value equality across all fields.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is an {@link FbPage} with equal fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (FbPage) obj;
                return Objects.equals(this.id, that.id)
                        && Objects.equals(this.displayName, that.displayName)
                        && Objects.equals(this.adStatus, that.adStatus)
                        && this.profileSyncState == that.profileSyncState
                        && this.whatsAppAsPageButtonState == that.whatsAppAsPageButtonState
                        && Objects.equals(this.profilePictureUrl, that.profilePictureUrl)
                        && Arrays.equals(this.profilePictureBytes, that.profilePictureBytes)
                        && this.showOnProfile == that.showOnProfile;
            }

            /**
             * Returns a hash code derived from all fields, folding the picture bytes via {@link Arrays#hashCode(byte[])}.
             *
             * @return the combined hash code
             */
            @Override
            public int hashCode() {
                var result = Objects.hash(id, displayName, adStatus, profileSyncState,
                        whatsAppAsPageButtonState, profilePictureUrl, showOnProfile);
                result = 31 * result + Arrays.hashCode(profilePictureBytes);
                return result;
            }

            /**
             * Returns a debug rendering listing the page fields except the raw picture bytes.
             *
             * @return the string representation
             */
            @Override
            public String toString() {
                return "SmaxGetLinkedAccountsResponse.Success.FbPage[id=" + id
                        + ", displayName=" + displayName
                        + ", adStatus=" + adStatus
                        + ", profileSyncState=" + profileSyncState
                        + ", whatsAppAsPageButtonState=" + whatsAppAsPageButtonState
                        + ", profilePictureUrl=" + profilePictureUrl
                        + ", showOnProfile=" + showOnProfile + ']';
            }
        }

        /**
         * Carries the linked Facebook-business identity plus its catalog sub-state.
         * <p>
         * Surfaced as the secondary linked-business identity on the SMB linked-pages settings
         * surface; the optional {@link Catalog} sub-projection drives the catalog import-status
         * badge.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingFBBizResponseMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingHasDisplayNameMixin")
        public static final class FbBiz {
            /**
             * The Facebook-business identifier.
             */
            private final String id;

            /**
             * The display-name element-content.
             */
            private final String displayName;

            /**
             * The optional {@code <catalog id state/>} child.
             */
            private final Catalog catalog;

            /**
             * Constructs a projection from the parsed business fields.
             *
             * @param id          the business id; never {@code null}
             * @param displayName the display name; never {@code null}
             * @param catalog     the optional catalog projection; may be {@code null}
             * @throws NullPointerException if {@code id} or {@code displayName} is {@code null}
             */
            public FbBiz(String id, String displayName, Catalog catalog) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
                this.catalog = catalog;
            }

            /**
             * Returns the business identifier.
             *
             * @return the id; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the display name.
             *
             * @return the name; never {@code null}
             */
            public String displayName() {
                return displayName;
            }

            /**
             * Returns the optional catalog projection.
             *
             * @return an {@link Optional} carrying the projection, or empty
             */
            public Optional<Catalog> catalog() {
                return Optional.ofNullable(catalog);
            }

            /**
             * Tries to parse the projection from the given stanza.
             * <p>
             * Asserts the {@code fb_biz} tag, projects the mandatory {@code id} attribute and
             * the {@code <display_name>} child content, and only then delegates the optional
             * {@code <catalog>} grandchild to {@link Catalog#of(Stanza)}.
             *
             * @implNote This implementation treats a present-but-malformed catalog as a hard
             * failure, unlike the swallow semantics on {@link FbPage}'s optional mixins.
             * @param stanza the {@code <fb_biz/>} stanza
             * @return an {@link Optional} carrying the projection, or empty when the stanza does not match the documented schema
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingGetLinkedAccountsResponseSuccess",
                    exports = "parseGetLinkedAccountsResponseSuccessLinkedAccountsFbBiz",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingFBBizResponseMixin",
                    exports = "parseFBBizResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<FbBiz> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("fb_biz")) {
                    return Optional.empty();
                }
                var id = stanza.getAttributeAsString("id").orElse(null);
                if (id == null) {
                    return Optional.empty();
                }
                var displayNameNode = stanza.getChild("display_name").orElse(null);
                if (displayNameNode == null) {
                    return Optional.empty();
                }
                var displayName = displayNameNode.toContentString().orElse(null);
                if (displayName == null) {
                    return Optional.empty();
                }
                Catalog catalog = null;
                var catalogNode = stanza.getChild("catalog").orElse(null);
                if (catalogNode != null) {
                    var parsed = Catalog.of(catalogNode);
                    if (parsed.isEmpty()) {
                        return Optional.empty();
                    }
                    catalog = parsed.get();
                }
                return Optional.of(new FbBiz(id, displayName, catalog));
            }

            /**
             * Compares this projection to another object for value equality across all three fields.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is an {@link FbBiz} with equal fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (FbBiz) obj;
                return Objects.equals(this.id, that.id)
                        && Objects.equals(this.displayName, that.displayName)
                        && Objects.equals(this.catalog, that.catalog);
            }

            /**
             * Returns a hash code derived from all three fields.
             *
             * @return the combined hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(id, displayName, catalog);
            }

            /**
             * Returns a debug rendering listing the business fields.
             *
             * @return the string representation
             */
            @Override
            public String toString() {
                return "SmaxGetLinkedAccountsResponse.Success.FbBiz[id=" + id
                        + ", displayName=" + displayName
                        + ", catalog=" + catalog + ']';
            }

            /**
             * Carries the linked Facebook-business catalog identifier and sync-state toggle.
             * <p>
             * Drives the catalog import-status badge on the SMB linked-pages settings surface;
             * the {@link #state()} value mirrors
             * {@link SmaxGetLinkedAccountsDisableImportState} on the
             * {@code <fb_page><profile_sync>} position.
             */
            @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingFBBizResponseMixin")
            public static final class Catalog {
                /**
                 * The catalog identifier.
                 */
                private final String id;

                /**
                 * The sync-state toggle.
                 */
                private final SmaxGetLinkedAccountsDisableImportState state;

                /**
                 * Constructs a catalog projection from the parsed id and state.
                 *
                 * @param id    the catalog id; never {@code null}
                 * @param state the state; never {@code null}
                 * @throws NullPointerException if either argument is {@code null}
                 */
                public Catalog(String id, SmaxGetLinkedAccountsDisableImportState state) {
                    this.id = Objects.requireNonNull(id, "id cannot be null");
                    this.state = Objects.requireNonNull(state, "state cannot be null");
                }

                /**
                 * Returns the catalog identifier.
                 *
                 * @return the id; never {@code null}
                 */
                public String id() {
                    return id;
                }

                /**
                 * Returns the sync-state toggle.
                 *
                 * @return the state; never {@code null}
                 */
                public SmaxGetLinkedAccountsDisableImportState state() {
                    return state;
                }

                /**
                 * Tries to parse the projection from the given stanza.
                 * <p>
                 * Asserts the {@code catalog} tag and projects the mandatory {@code id} and
                 * {@code state} attributes, validating the latter through
                 * {@link SmaxGetLinkedAccountsDisableImportState#of(String)}; any missing or
                 * malformed mandatory is a parse failure.
                 *
                 * @param stanza the {@code <catalog/>} stanza
                 * @return an {@link Optional} carrying the projection, or empty when the stanza does not match the documented schema
                 */
                @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingFBBizResponseMixin",
                        exports = "parseFBBizResponseCatalog", adaptation = WhatsAppAdaptation.ADAPTED)
                public static Optional<Catalog> of(Stanza stanza) {
                    Objects.requireNonNull(stanza, "stanza cannot be null");
                    if (!stanza.hasDescription("catalog")) {
                        return Optional.empty();
                    }
                    var id = stanza.getAttributeAsString("id").orElse(null);
                    if (id == null) {
                        return Optional.empty();
                    }
                    var stateStr = stanza.getAttributeAsString("state").orElse(null);
                    var state = SmaxGetLinkedAccountsDisableImportState.of(stateStr).orElse(null);
                    if (state == null) {
                        return Optional.empty();
                    }
                    return Optional.of(new Catalog(id, state));
                }

                /**
                 * Compares this projection to another object for value equality across both fields.
                 *
                 * @param obj the object to compare against; may be {@code null}
                 * @return {@code true} when {@code obj} is a {@link Catalog} with equal fields
                 */
                @Override
                public boolean equals(Object obj) {
                    if (obj == this) {
                        return true;
                    }
                    if (obj == null || obj.getClass() != this.getClass()) {
                        return false;
                    }
                    var that = (Catalog) obj;
                    return Objects.equals(this.id, that.id) && this.state == that.state;
                }

                /**
                 * Returns a hash code derived from both fields.
                 *
                 * @return the combined hash code
                 */
                @Override
                public int hashCode() {
                    return Objects.hash(id, state);
                }

                /**
                 * Returns a debug rendering listing the catalog id and state.
                 *
                 * @return the string representation
                 */
                @Override
                public String toString() {
                    return "SmaxGetLinkedAccountsResponse.Success.FbBiz.Catalog[id=" + id
                            + ", state=" + state + ']';
                }
            }
        }

        /**
         * Carries the linked Instagram-professional identity plus profile-picture, display-name
         * and show-on-profile sub-states.
         * <p>
         * Surfaced on the SMB linked-pages settings surface and propagated through the CTWA ads
         * pipeline alongside {@link FbPage} when the calling business has both platforms linked.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingIGProfessionalResponseBaseMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingHasDisplayNameMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingHasProfilePictureMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingHasShowOnProfileMixin")
        public static final class IgProfessional {
            /**
             * The Instagram handle (element-content of the {@code <ig_handle/>} child).
             */
            private final String igHandle;

            /**
             * The optional profile-picture URL element-content; {@code null} when the upstream
             * profile-picture mixin failed and the result was swallowed.
             */
            private final String profilePictureUrl;

            /**
             * The optional profile-picture bytes; {@code null} when omitted.
             */
            private final byte[] profilePictureBytes;

            /**
             * The optional display-name element-content; {@code null} when the upstream
             * display-name mixin failed and the result was swallowed.
             */
            private final String displayName;

            /**
             * The optional show-on-profile flag; {@code null} when the upstream show-on-profile
             * mixin failed and the result was swallowed.
             */
            private final SmaxGetLinkedAccountsFalseTrueFlag showOnProfile;

            /**
             * Constructs a projection from the parsed Instagram fields.
             *
             * @param igHandle            the Instagram handle; never {@code null}
             * @param profilePictureUrl   the optional profile picture URL; may be {@code null}
             * @param profilePictureBytes the optional profile picture bytes; may be {@code null}
             * @param displayName         the optional display name; may be {@code null}
             * @param showOnProfile       the optional show-on-profile flag; may be {@code null}
             * @throws NullPointerException if {@code igHandle} is {@code null}
             */
            public IgProfessional(String igHandle,
                                  String profilePictureUrl,
                                  byte[] profilePictureBytes,
                                  String displayName,
                                  SmaxGetLinkedAccountsFalseTrueFlag showOnProfile) {
                this.igHandle = Objects.requireNonNull(igHandle, "igHandle cannot be null");
                this.profilePictureUrl = profilePictureUrl;
                this.profilePictureBytes = profilePictureBytes;
                this.displayName = displayName;
                this.showOnProfile = showOnProfile;
            }

            /**
             * Returns the Instagram handle.
             *
             * @return the handle; never {@code null}
             */
            public String igHandle() {
                return igHandle;
            }

            /**
             * Returns the optional profile-picture URL.
             *
             * @return an {@link Optional} carrying the URL, or empty
             */
            public Optional<String> profilePictureUrl() {
                return Optional.ofNullable(profilePictureUrl);
            }

            /**
             * Returns the optional profile-picture bytes.
             *
             * @return an {@link Optional} carrying the bytes, or empty
             */
            public Optional<byte[]> profilePictureBytes() {
                return Optional.ofNullable(profilePictureBytes);
            }

            /**
             * Returns the optional display name.
             *
             * @return an {@link Optional} carrying the name, or empty
             */
            public Optional<String> displayName() {
                return Optional.ofNullable(displayName);
            }

            /**
             * Returns the optional show-on-profile flag.
             *
             * @return an {@link Optional} carrying the flag, or empty
             */
            public Optional<SmaxGetLinkedAccountsFalseTrueFlag> showOnProfile() {
                return Optional.ofNullable(showOnProfile);
            }

            /**
             * Tries to parse the projection from the given stanza.
             * <p>
             * Walks the two mandatory positions (tag assertion and the {@code <ig_handle>}
             * content) and admits three optional mixins; the {@code <profile_picture>},
             * {@code <display_name>} and {@code <show_on_profile>} branches all leave their
             * fields {@code null} on sub-failure.
             *
             * @implNote This implementation mirrors WA Web's upstream {@code r.success ? r.value : null}
             * wrapping by leaving the optional mixin fields {@code null} on sub-failure rather
             * than propagating {@link Optional#empty()} to the parent parse.
             * @param stanza the {@code <ig_professional/>} stanza
             * @return an {@link Optional} carrying the projection, or empty when the stanza does not match the documented schema
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingGetLinkedAccountsResponseSuccess",
                    exports = "parseGetLinkedAccountsResponseSuccessLinkedAccountsIgProfessional",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingIGProfessionalResponseBaseMixin",
                    exports = "parseIGProfessionalResponseBaseMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingHasProfilePictureMixin",
                    exports = "parseHasProfilePictureMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingHasProfilePictureMixin",
                    exports = "parseHasProfilePictureProfilePictureBytes",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingHasShowOnProfileMixin",
                    exports = "parseHasShowOnProfileMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<IgProfessional> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("ig_professional")) {
                    return Optional.empty();
                }
                var handleNode = stanza.getChild("ig_handle").orElse(null);
                if (handleNode == null) {
                    return Optional.empty();
                }
                var handle = handleNode.toContentString().orElse(null);
                if (handle == null) {
                    return Optional.empty();
                }
                String profilePictureUrl = null;
                byte[] profilePictureBytes = null;
                var profilePicture = stanza.getChild("profile_picture").orElse(null);
                if (profilePicture != null) {
                    var urlNode = profilePicture.getChild("url").orElse(null);
                    if (urlNode != null) {
                        var url = urlNode.toContentString().orElse(null);
                        if (url != null) {
                            profilePictureUrl = url;
                            var bytesNode = profilePicture.getChild("bytes").orElse(null);
                            if (bytesNode != null) {
                                profilePictureBytes = bytesNode.toContentBytes().orElse(null);
                            }
                        }
                    }
                }
                String displayName = null;
                var displayNameNode = stanza.getChild("display_name").orElse(null);
                if (displayNameNode != null) {
                    displayName = displayNameNode.toContentString().orElse(null);
                }
                SmaxGetLinkedAccountsFalseTrueFlag showOnProfile = null;
                var showNode = stanza.getChild("show_on_profile").orElse(null);
                if (showNode != null) {
                    var content = showNode.toContentString().orElse(null);
                    if (content != null) {
                        showOnProfile = SmaxGetLinkedAccountsFalseTrueFlag.of(content).orElse(null);
                    }
                }
                return Optional.of(new IgProfessional(handle, profilePictureUrl,
                        profilePictureBytes, displayName, showOnProfile));
            }

            /**
             * Compares this projection to another object for value equality across all fields.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is an {@link IgProfessional} with equal fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (IgProfessional) obj;
                return Objects.equals(this.igHandle, that.igHandle)
                        && Objects.equals(this.profilePictureUrl, that.profilePictureUrl)
                        && Arrays.equals(this.profilePictureBytes, that.profilePictureBytes)
                        && Objects.equals(this.displayName, that.displayName)
                        && this.showOnProfile == that.showOnProfile;
            }

            /**
             * Returns a hash code derived from all fields, folding the picture bytes via {@link Arrays#hashCode(byte[])}.
             *
             * @return the combined hash code
             */
            @Override
            public int hashCode() {
                var result = Objects.hash(igHandle, profilePictureUrl, displayName, showOnProfile);
                result = 31 * result + Arrays.hashCode(profilePictureBytes);
                return result;
            }

            /**
             * Returns a debug rendering listing the Instagram fields except the raw picture bytes.
             *
             * @return the string representation
             */
            @Override
            public String toString() {
                return "SmaxGetLinkedAccountsResponse.Success.IgProfessional[igHandle=" + igHandle
                        + ", profilePictureUrl=" + profilePictureUrl
                        + ", displayName=" + displayName
                        + ", showOnProfile=" + showOnProfile + ']';
            }
        }

        /**
         * Carries the linked WhatsApp ad-account identifier plus its ad-status mixin.
         * <p>
         * Drives the WhatsApp Ads account-type branch of the CTWA pipeline, used as the fallback
         * identity when the calling business has no linked Facebook page.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingWhatsAppAdIdentityResponseMixin")
        public static final class WhatsAppAdIdentity {
            /**
             * The ad-identity identifier.
             */
            private final String id;

            /**
             * The mandatory ad-status mixin projection carrying the two boolean flags
             * {@code has_created_ad} and {@code has_active_ctwa_ad}.
             */
            private final SmaxAdStatusMixin adStatus;

            /**
             * Constructs a projection from the parsed id and ad-status mixin.
             *
             * @param id       the identifier; never {@code null}
             * @param adStatus the mandatory ad-status mixin; never {@code null}
             * @throws NullPointerException if either argument is {@code null}
             */
            public WhatsAppAdIdentity(String id, SmaxAdStatusMixin adStatus) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.adStatus = Objects.requireNonNull(adStatus, "adStatus cannot be null");
            }

            /**
             * Returns the identifier.
             *
             * @return the id; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the mandatory ad-status mixin projection.
             *
             * @return the projection; never {@code null}
             */
            public SmaxAdStatusMixin adStatus() {
                return adStatus;
            }

            /**
             * Tries to parse the projection from the given stanza.
             * <p>
             * Asserts the {@code whatsapp_ad_identity} tag, projects the mandatory {@code id}
             * attribute, and then delegates to {@link SmaxAdStatusMixin#of(Stanza)} for the
             * mandatory {@code <ad_status/>} child; any missing mandatory is a parse failure.
             *
             * @param stanza the {@code <whatsapp_ad_identity/>} stanza
             * @return an {@link Optional} carrying the projection, or empty when the stanza does not match the documented schema
             */
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingGetLinkedAccountsResponseSuccess",
                    exports = "parseGetLinkedAccountsResponseSuccessLinkedAccountsWhatsappAdIdentity",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingWhatsAppAdIdentityResponseMixin",
                    exports = "parseWhatsAppAdIdentityResponseMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<WhatsAppAdIdentity> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("whatsapp_ad_identity")) {
                    return Optional.empty();
                }
                var id = stanza.getAttributeAsString("id").orElse(null);
                if (id == null) {
                    return Optional.empty();
                }
                var adStatus = SmaxAdStatusMixin.of(stanza).orElse(null);
                if (adStatus == null) {
                    return Optional.empty();
                }
                return Optional.of(new WhatsAppAdIdentity(id, adStatus));
            }

            /**
             * Compares this projection to another object for value equality across both fields.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is a {@link WhatsAppAdIdentity} with equal fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (WhatsAppAdIdentity) obj;
                return Objects.equals(this.id, that.id)
                        && Objects.equals(this.adStatus, that.adStatus);
            }

            /**
             * Returns a hash code derived from both fields.
             *
             * @return the combined hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(id, adStatus);
            }

            /**
             * Returns a debug rendering listing the id and ad-status mixin.
             *
             * @return the string representation
             */
            @Override
            public String toString() {
                return "SmaxGetLinkedAccountsResponse.Success.WhatsAppAdIdentity[id=" + id
                        + ", adStatus=" + adStatus + ']';
            }
        }
    }

    /**
     * Carries the documented {@code (403, "forbidden")} rejection.
     * <p>
     * Surfaced when the relay refuses the enumeration because the calling business is not
     * authorised to query linked accounts, for example because the SMB linking feature has not
     * been enabled on the relay side. The WA Web pipeline treats this as a non-error null
     * result, so this variant is intentionally separated from {@link ClientError} for that
     * branch.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingGetLinkedAccountsResponseForbidden")
    @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingIQErrorForbiddenMixin")
    final class Forbidden implements SmaxGetLinkedAccountsResponse {
        /**
         * The numeric error code; always {@code 403}.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied one.
         */
        private final String errorText;

        /**
         * Constructs a forbidden reply from a validated {@code (403, "forbidden")} literal pair.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public Forbidden(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link Forbidden} variant from the given inbound stanza.
         * <p>
         * Routes the error extraction through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, then enforces the
         * literal pair {@code code == 403 && text.equals("forbidden")}. Any other {@code 4xx}
         * pair, including a {@code 403} with a non-{@code "forbidden"} text, falls through to
         * {@link ClientError}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the {@code 403}/forbidden schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingGetLinkedAccountsResponseForbidden",
                exports = "parseGetLinkedAccountsResponseForbidden",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Forbidden> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            if (envelope.code() != 403 || !"forbidden".equals(envelope.text())) {
                return Optional.empty();
            }
            return Optional.of(new Forbidden(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for value equality across both fields.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Forbidden} with equal fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Forbidden) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from both fields.
         *
         * @return the combined hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering listing the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetLinkedAccountsResponse.Forbidden[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries any other documented {@code 4xx} error code that did not match {@link Forbidden}.
     * <p>
     * Surfaced when the relay rejected the request with a {@code 4xx} that is not the specific
     * {@code (403, "forbidden")} pair; callers treat this as a hard failure rather than the
     * non-error null return reserved for {@link Forbidden}.
     */
    final class ClientError implements SmaxGetLinkedAccountsResponse {
        /**
         * The numeric error code in the {@code 4xx} range, excluding the
         * {@code (403, "forbidden")} pair.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied one.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from a validated {@code 4xx} envelope that is not the forbidden literal pair.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         * <p>
         * Routes the error extraction through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, then explicitly
         * excludes the {@code (403, "forbidden")} literal pair so that {@link Forbidden} retains
         * exclusive ownership of that case in the priority order of {@link #of(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the client-error schema or when {@link Forbidden} would have matched first
         */
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            if (envelope.code() == 403 && "forbidden".equals(envelope.text())) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for value equality across both fields.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ClientError} with equal fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from both fields.
         *
         * @return the combined hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering listing the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetLinkedAccountsResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a transient {@code 5xx} relay failure.
     * <p>
     * Indicates the relay could not complete the linked-accounts enumeration for an internal
     * reason; callers can re-issue the request with backoff.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingGetLinkedAccountsResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizLinkingIQErrorInternalServerErrorMixin")
    final class ServerError implements SmaxGetLinkedAccountsResponse {
        /**
         * The numeric server-side error code in the {@code 5xx} range.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied one.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from a validated {@code 5xx} envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         * <p>
         * Delegates the {@code 5xx} range check to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}; a stanza outside the
         * {@code 5xx} range yields {@link Optional#empty()}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizLinkingGetLinkedAccountsResponseError",
                exports = "parseGetLinkedAccountsResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for value equality across both fields.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ServerError} with equal fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from both fields.
         *
         * @return the combined hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering listing the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetLinkedAccountsResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
