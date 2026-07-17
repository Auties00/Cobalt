package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLwiSubEntryPoint")
@WamEnum
public enum LwiSubEntryPoint {
    @WamEnumConstant(1) SMB_HOME_SCREEN_CONVERSATIONS_TAB,
    @WamEnumConstant(2) SMB_HOME_SCREEN_STATUS_TAB,
    @WamEnumConstant(3) SMB_HOME_SCREEN_CALL_HISTORY_TAB,
    @WamEnumConstant(4) SMB_HOME_SCREEN_COMMUNITIES_TAB,
    @WamEnumConstant(5) SMB_HOME_SCREEN_BIZ_HOME_TAB
}
