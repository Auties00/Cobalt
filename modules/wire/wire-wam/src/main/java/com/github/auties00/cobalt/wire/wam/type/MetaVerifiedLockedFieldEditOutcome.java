package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaVerifiedLockedFieldEditOutcome")
@WamEnum
public enum MetaVerifiedLockedFieldEditOutcome {
    @WamEnumConstant(1) APPROVED,
    @WamEnumConstant(2) REJECTED
}
