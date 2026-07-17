package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSearchActionName")
@WamEnum
public enum SearchActionName {
    @WamEnumConstant(1) SEARCH_START,
    @WamEnumConstant(2) CLICK_ON_CONTACT,
    @WamEnumConstant(3) CLICK_ON_NON_CONTACT,
    @WamEnumConstant(5) VIEW_PIN_VERIFICATION,
    @WamEnumConstant(6) PIN_VERFICATION_ERROR_SHOWN,
    @WamEnumConstant(7) INITIATION_SUCCESS,
    @WamEnumConstant(8) INITIATION_FAILURE,
    @WamEnumConstant(9) CLICK_ON_CONTACT_WITH_EXISTING_CHAT,
    @WamEnumConstant(10) CLICK_ON_NON_CONTACT_WITH_EXISTING_CHAT
}
