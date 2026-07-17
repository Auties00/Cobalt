package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUiMessageYourselfFunnelName")
@WamEnum
public enum UiMessageYourselfFunnelName {
    @WamEnumConstant(1) NEW_CHAT,
    @WamEnumConstant(2) CONTACT_AND_GLOBAL_SEARCH
}
