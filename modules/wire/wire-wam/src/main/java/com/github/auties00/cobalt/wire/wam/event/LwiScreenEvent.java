package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AdContentRecommendationType;
import com.github.auties00.cobalt.wire.wam.type.AudienceType;
import com.github.auties00.cobalt.wire.wam.type.BillingStatus;
import com.github.auties00.cobalt.wire.wam.type.CtwaAdAccountType;
import com.github.auties00.cobalt.wire.wam.type.CtwaLoginType;
import com.github.auties00.cobalt.wire.wam.type.DefaultAudienceLocationType;
import com.github.auties00.cobalt.wire.wam.type.LwiAdCreationAccountConsentFlow;
import com.github.auties00.cobalt.wire.wam.type.LwiAdMediaType;
import com.github.auties00.cobalt.wire.wam.type.LwiAdsContentType;
import com.github.auties00.cobalt.wire.wam.type.LwiAdsIdentityType;
import com.github.auties00.cobalt.wire.wam.type.LwiAlertReason;
import com.github.auties00.cobalt.wire.wam.type.LwiCtwaAdCtaType;
import com.github.auties00.cobalt.wire.wam.type.LwiCtwaAdStatusType;
import com.github.auties00.cobalt.wire.wam.type.LwiDefaultTargetingSpec;
import com.github.auties00.cobalt.wire.wam.type.LwiScreenAction;
import com.github.auties00.cobalt.wire.wam.type.LwiScreenReference;
import com.github.auties00.cobalt.wire.wam.type.MediaSource;
import com.github.auties00.cobalt.wire.wam.type.OnboardingEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.SmbiAdCreationAccessTokenSource;
import com.github.auties00.cobalt.wire.wam.type.ValidationStatus;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebLwiScreenWamEvent")
@WamEvent(id = 2772)
public interface LwiScreenEvent extends WamEventSpec {
    @WamProperty(index = 41, type = WamType.ENUM)
    Optional<AdContentRecommendationType> adContentRecommendation();

    @WamProperty(index = 61, type = WamType.STRING)
    Optional<String> adMediaTemplateName();

    @WamProperty(index = 40, type = WamType.ENUM)
    Optional<LwiAdMediaType> adMediaTypeSelected();

    @WamProperty(index = 42, type = WamType.ENUM)
    Optional<LwiAdsContentType> adsContentSelected();

    @WamProperty(index = 33, type = WamType.INTEGER)
    OptionalLong alertCount();

    @WamProperty(index = 45, type = WamType.ENUM)
    Optional<AudienceType> audienceType();

    @WamProperty(index = 36, type = WamType.ENUM)
    Optional<BillingStatus> billingStatus();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> createAdEnabled();

    @WamProperty(index = 58, type = WamType.ENUM)
    Optional<CtwaAdAccountType> ctwaAdAccountType();

    @WamProperty(index = 59, type = WamType.ENUM)
    Optional<CtwaLoginType> ctwaLoginType();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<LwiAdsContentType> defaultAdsContentSelected();

    @WamProperty(index = 57, type = WamType.ENUM)
    Optional<DefaultAudienceLocationType> defaultAudienceLocationType();

    @WamProperty(index = 39, type = WamType.INTEGER)
    OptionalLong itemCount();

    @WamProperty(index = 46, type = WamType.STRING)
    Optional<String> lwiAdCampaignId();

    @WamProperty(index = 63, type = WamType.ENUM)
    Optional<LwiAdCreationAccountConsentFlow> lwiAdCreationAccountConsentFlow();

    @WamProperty(index = 22, type = WamType.ENUM)
    Optional<LwiAdsIdentityType> lwiAdsIdentityType();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<LwiAlertReason> lwiAlertReason();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong lwiBudgetInLocal();

    @WamProperty(index = 54, type = WamType.STRING)
    Optional<String> lwiBudgetOptionsInLocal();

    @WamProperty(index = 24, type = WamType.ENUM)
    Optional<LwiCtwaAdCtaType> lwiCtwaAdCtaType();

    @WamProperty(index = 25, type = WamType.ENUM)
    Optional<LwiCtwaAdStatusType> lwiCtwaAdStatusType();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> lwiCurrency();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong lwiDefaultBudgetInLocal();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong lwiDefaultDurationInDays();

    @WamProperty(index = 19, type = WamType.ENUM)
    Optional<LwiDefaultTargetingSpec> lwiDefaultTargetingSpec();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong lwiDurationInDays();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong lwiEventSequenceNumber();

    @WamProperty(index = 30, type = WamType.STRING)
    Optional<String> lwiExtras();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> lwiFlowId();

    @WamProperty(index = 27, type = WamType.BOOLEAN)
    Optional<Boolean> lwiIsFbAppInstalled();

    @WamProperty(index = 51, type = WamType.BOOLEAN)
    Optional<Boolean> lwiIsIgAppInstalled();

    @WamProperty(index = 55, type = WamType.STRING)
    Optional<String> lwiLocationTypesSetOnAudienceSelection();

    @WamProperty(index = 52, type = WamType.INTEGER)
    OptionalLong lwiMaxDurationInDays();

    @WamProperty(index = 53, type = WamType.INTEGER)
    OptionalLong lwiMinDurationInDays();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<LwiScreenAction> lwiScreenAction();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<LwiScreenReference> lwiScreenReference();

    @WamProperty(index = 21, type = WamType.STRING)
    Optional<String> lwiTargetingSpec();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong lwiTotalCtwaAds();

    @WamProperty(index = 28, type = WamType.BOOLEAN)
    Optional<Boolean> lwiViewerHasEditPermission();

    @WamProperty(index = 32, type = WamType.BOOLEAN)
    Optional<Boolean> lwiViewerHasPromotePermission();

    @WamProperty(index = 49, type = WamType.BOOLEAN)
    Optional<Boolean> mediaEdited();

    @WamProperty(index = 66, type = WamType.ENUM)
    Optional<MediaSource> mediaSource();

    @WamProperty(index = 38, type = WamType.ENUM)
    Optional<OnboardingEntryPoint> onboardingEntryPoint();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> paymentMethodSet();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> productId();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> selectedProductsIdsList();

    @WamProperty(index = 64, type = WamType.ENUM)
    Optional<SmbiAdCreationAccessTokenSource> smbiAdCreationAccessTokenSource();

    @WamProperty(index = 50, type = WamType.INTEGER)
    OptionalLong totalMediaCount();

    @WamProperty(index = 31, type = WamType.BOOLEAN)
    Optional<Boolean> usedSavedAudience();

    @WamProperty(index = 35, type = WamType.BOOLEAN)
    Optional<Boolean> userHasAdvancedAudience();

    @WamProperty(index = 43, type = WamType.BOOLEAN)
    Optional<Boolean> userHasBpCredentials();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> userHasCatalogItemsToPromote();

    @WamProperty(index = 56, type = WamType.BOOLEAN)
    Optional<Boolean> userHasChangedDefaultCityLevelAudience();

    @WamProperty(index = 65, type = WamType.BOOLEAN)
    Optional<Boolean> userHasFbMediaToPromote();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> userHasLinkedFbPage();

    @WamProperty(index = 67, type = WamType.BOOLEAN)
    Optional<Boolean> userHasMediaCarousel();

    @WamProperty(index = 60, type = WamType.BOOLEAN)
    Optional<Boolean> userHasMultisourceMedia();

    @WamProperty(index = 34, type = WamType.BOOLEAN)
    Optional<Boolean> userHasSeenRecommendedBudget();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> userHasStatusToPromote();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> userProvidedFbConsent();

    @WamProperty(index = 23, type = WamType.BOOLEAN)
    Optional<Boolean> userWentThroughFbWebLogin();

    @WamProperty(index = 37, type = WamType.ENUM)
    Optional<ValidationStatus> validationStatus();

    @WamProperty(index = 44, type = WamType.STRING)
    Optional<String> waAdAccountId();
}
