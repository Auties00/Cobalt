package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedDialogInteraction;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedLockedFieldEditOutcome;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedLockedProfileField;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedUserActionAction;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedUserActionAssetType;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedUserActionBannerType;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedUserActionErrorDetails;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedUserActionInteractionError;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedUserActionReferral;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedUserActionResult;
import com.github.auties00.cobalt.wire.wam.type.MetaVerifiedUserActionSurface;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebMetaVerifiedUserActionWamEvent")
@WamEvent(id = 4986)
public interface MetaVerifiedUserActionEvent extends WamEventSpec {
    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> isProfileLocked();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> isRetryAttempt();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> isVlevelHigh();

    @WamProperty(index = 20, type = WamType.ENUM)
    Optional<MetaVerifiedDialogInteraction> metaVerifiedDialogInteraction();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<MetaVerifiedLockedFieldEditOutcome> metaVerifiedLockedFieldEditOutcome();

    @WamProperty(index = 17, type = WamType.ENUM)
    Optional<MetaVerifiedLockedProfileField> metaVerifiedLockedProfileField();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> metaVerifiedLockedProfileFieldEligibility();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> metaVerifiedQuickPromotionId();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MetaVerifiedUserActionAction> metaVerifiedUserActionAction();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MetaVerifiedUserActionAssetType> metaVerifiedUserActionAssetType();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<MetaVerifiedUserActionBannerType> metaVerifiedUserActionBannerType();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<MetaVerifiedUserActionErrorDetails> metaVerifiedUserActionErrorDetails();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> metaVerifiedUserActionExtra();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> metaVerifiedUserActionGreenDotVisible();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MetaVerifiedUserActionInteractionError> metaVerifiedUserActionInteractionError();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> metaVerifiedUserActionIsSubscribed();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MetaVerifiedUserActionReferral> metaVerifiedUserActionReferral();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MetaVerifiedUserActionResult> metaVerifiedUserActionResult();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MetaVerifiedUserActionSurface> metaVerifiedUserActionSurface();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> metaVerifiedUserActionVerifiedBadgeVisible();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> userActionErrorCode();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> userActionSessionId();
}
