package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumEncryptionTypeCode")
@WamEnum
public enum EncryptionTypeCode {
    @WamEnumConstant(1) E2EE,
    @WamEnumConstant(2) COEX,
    @WamEnumConstant(3) SELF_COEX,
    @WamEnumConstant(4) CAPI,
    @WamEnumConstant(5) BSP,
    @WamEnumConstant(6) GUEST,
    @WamEnumConstant(7) TEE,
    @WamEnumConstant(8) BOT,
    @WamEnumConstant(9) BOT_GROUP,
    @WamEnumConstant(10) COEX_V2,
    @WamEnumConstant(11) SELF_COEX_V2
}
