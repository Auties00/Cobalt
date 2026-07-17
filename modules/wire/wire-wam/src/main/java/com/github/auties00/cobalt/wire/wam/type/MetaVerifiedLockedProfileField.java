package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaVerifiedLockedProfileField")
@WamEnum
public enum MetaVerifiedLockedProfileField {
    @WamEnumConstant(1) PHOTO,
    @WamEnumConstant(2) NAME,
    @WamEnumConstant(3) ADDRESS,
    @WamEnumConstant(4) WEBSITE,
    @WamEnumConstant(5) EMAIL
}
