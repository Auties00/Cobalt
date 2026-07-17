package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumQbmFlag")
@WamEnum
public enum QbmFlag {
    @WamEnumConstant(0) OTHER,
    @WamEnumConstant(1) TRANSACTIONAL,
    @WamEnumConstant(2) PROMOTIONAL,
    @WamEnumConstant(3) OTP,
    @WamEnumConstant(4) MARKETING_MESSAGE_SMB
}
