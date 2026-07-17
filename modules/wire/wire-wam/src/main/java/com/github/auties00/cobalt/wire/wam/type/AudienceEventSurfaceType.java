package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAudienceEventSurfaceType")
@WamEnum
public enum AudienceEventSurfaceType {
    @WamEnumConstant(0) SUGGESTED_CARD,
    @WamEnumConstant(1) MANUAL_PICK,
    @WamEnumConstant(2) CSV_IMPORT,
    @WamEnumConstant(3) LABEL_SELECTION,
    @WamEnumConstant(4) SYNCD_INCOMING,
    @WamEnumConstant(5) DUPLICATE
}
