package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumShareContentUserJourneyAction")
@WamEnum
public enum ShareContentUserJourneyAction {
    @WamEnumConstant(1) CONTACT_PICKER_DISPLAYED,
    @WamEnumConstant(2) RECIPIENTS_SELECTED,
    @WamEnumConstant(4) CONTENT_SHARED,
    @WamEnumConstant(5) CANCEL,
    @WamEnumConstant(6) ABANDONED,
    @WamEnumConstant(7) MEDIA_COMPOSER_DISPLAYED,
    @WamEnumConstant(8) SHARED_TEXT_COMPOSER_DISPLAYED,
    @WamEnumConstant(9) FUNNEL_START,
    @WamEnumConstant(10) SAVE_STATUS
}
