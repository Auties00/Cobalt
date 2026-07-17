package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdLinkedDevicesWindowsXdrStage")
@WamEnum
public enum MdLinkedDevicesWindowsXdrStage {
    @WamEnumConstant(1) XDR_AVAILABLE,
    @WamEnumConstant(2) XDR_ENABLED,
    @WamEnumConstant(3) INITIALIZED,
    @WamEnumConstant(4) CONNECTED,
    @WamEnumConstant(5) DISCONNECTED,
    @WamEnumConstant(6) CONNECTION_FAILED,
    @WamEnumConstant(7) APP_OPEN,
    @WamEnumConstant(8) CHAT_OPEN,
    @WamEnumConstant(9) CHAT_CLOSE,
    @WamEnumConstant(10) APP_CLOSE,
    @WamEnumConstant(11) DEEPLINK_APP_OPEN,
    @WamEnumConstant(12) DEEPLINK_CHAT_OPEN,
    @WamEnumConstant(13) ME_LID_MATCH,
    @WamEnumConstant(14) ME_LID_MISMATCH,
    @WamEnumConstant(15) DEEPLINK_NAVIGATION_SUCCESS,
    @WamEnumConstant(16) DEEPLINK_NAVIGATION_FAILURE
}
