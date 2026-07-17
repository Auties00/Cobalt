package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumJidDomainType")
@WamEnum
public enum JidDomainType {
    @WamEnumConstant(0) S_WHATSAPP_NET,
    @WamEnumConstant(1) C_US,
    @WamEnumConstant(2) LID,
    @WamEnumConstant(3) MSGR,
    @WamEnumConstant(4) INTEROP,
    @WamEnumConstant(5) INTEROP_MSGR,
    @WamEnumConstant(6) G_US,
    @WamEnumConstant(7) BOT,
    @WamEnumConstant(8) BROADCAST,
    @WamEnumConstant(9) NEWSLETTER,
    @WamEnumConstant(10) CALL,
    @WamEnumConstant(11) HPHONE,
    @WamEnumConstant(12) NONE_US,
    @WamEnumConstant(13) ERROR_US,
    @WamEnumConstant(14) NONE
}
