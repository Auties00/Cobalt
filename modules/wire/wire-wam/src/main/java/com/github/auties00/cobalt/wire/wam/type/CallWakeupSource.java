package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallWakeupSource")
@WamEnum
public enum CallWakeupSource {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) UNKNOWN,
    @WamEnumConstant(2) APNS_PUSH,
    @WamEnumConstant(3) IOS_VOIP_PUSH,
    @WamEnumConstant(4) GCM_PUSH,
    @WamEnumConstant(5) ONLINE,
    @WamEnumConstant(6) RIM_PUSH,
    @WamEnumConstant(7) WNS_PUSH,
    @WamEnumConstant(8) GCM_CALL_PUSH
}
