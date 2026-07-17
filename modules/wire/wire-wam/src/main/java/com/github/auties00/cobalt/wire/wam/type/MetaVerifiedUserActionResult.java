package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaVerifiedUserActionResult")
@WamEnum
public enum MetaVerifiedUserActionResult {
    @WamEnumConstant(1) OK,
    @WamEnumConstant(2) ERROR
}
