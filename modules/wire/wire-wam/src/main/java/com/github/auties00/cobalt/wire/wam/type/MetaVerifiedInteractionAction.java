package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaVerifiedInteractionAction")
@WamEnum
public enum MetaVerifiedInteractionAction {
    @WamEnumConstant(2) MV_INTERACTION_ACTION_VIEW_MV_EDUCATION_BOTTOM_SHEET,
    @WamEnumConstant(3) MV_INTERACTION_ACTION_CLICK_MV_EDUCATION_LINK,
    @WamEnumConstant(4) MV_INTERACTION_ACTION_VIEW_CROSS_SELL_PROFILE_INTERSTITIAL,
    @WamEnumConstant(5) MV_INTERACTION_ACTION_CLICK_MV_MORE_LINK,
    @WamEnumConstant(6) MV_INTERACTION_ACTION_CLICK_GET_WA_BUSINESS,
    @WamEnumConstant(7) MV_INTERACTION_ACTION_CLICK_SUPPORT,
    @WamEnumConstant(8) MV_INTERACTION_ACTION_CLICK_CUSTOM_WEBPAGE_AND_LINK,
    @WamEnumConstant(9) MV_INTERACTION_ACTION_CLICK_MULTI_DEVICE,
    @WamEnumConstant(10) MV_INTERACTION_ACTION_CLICK_MV_LEARN_MORE,
    @WamEnumConstant(11) MV_INTERACTION_ACTION_CLICK_MV_HOME,
    @WamEnumConstant(12) MV_INTERACTION_ACTION_CLICK_BLOCK,
    @WamEnumConstant(13) MV_INTERACTION_ACTION_CLICK_PROFILE,
    @WamEnumConstant(14) VIEW_NOT_MV_LABEL,
    @WamEnumConstant(15) CLICK_NOT_MV_LABEL,
    @WamEnumConstant(16) VIEW,
    @WamEnumConstant(17) CLICK_LEARN_MORE,
    @WamEnumConstant(18) CLICK_GET_MV,
    @WamEnumConstant(19) CLICK_DISMISS,
    @WamEnumConstant(20) CLICK_BADGE,
    @WamEnumConstant(21) VIEW_BADGE
}
