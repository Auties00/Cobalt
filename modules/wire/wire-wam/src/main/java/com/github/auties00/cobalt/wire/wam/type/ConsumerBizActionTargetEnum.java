package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumConsumerBizActionTargetEnum")
@WamEnum
public enum ConsumerBizActionTargetEnum {
    @WamEnumConstant(0) SEARCH_RESULT_ITEM,
    @WamEnumConstant(1) PROFILE_BUTTON,
    @WamEnumConstant(2) SHARE_BUTTON,
    @WamEnumConstant(3) BACK_BUTTON,
    @WamEnumConstant(4) CONTACT_CARD,
    @WamEnumConstant(5) HEADER_SUBTITLE,
    @WamEnumConstant(6) FMX_AFFILIATION_ROW,
    @WamEnumConstant(7) AFFILIATION_SECTION,
    @WamEnumConstant(8) CONTACT_BIZ_CTA,
    @WamEnumConstant(9) BOTTOM_SHEET_OK,
    @WamEnumConstant(10) BLOCKED_COMPOSER_LEARN_MORE,
    @WamEnumConstant(11) BLOCKED_COMPOSER
}
