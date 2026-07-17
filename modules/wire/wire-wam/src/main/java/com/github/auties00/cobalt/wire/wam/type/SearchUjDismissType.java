package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSearchUjDismissType")
@WamEnum
public enum SearchUjDismissType {
    @WamEnumConstant(1) BACK_PRESSED,
    @WamEnumConstant(2) OTHER,
    @WamEnumConstant(3) SWIPE
}
