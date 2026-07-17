package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumContactSuggestion")
@WamEnum
public enum ContactSuggestion {
    @WamEnumConstant(1) DEFAULT,
    @WamEnumConstant(2) FREQUENTS,
    @WamEnumConstant(3) RECENTS,
    @WamEnumConstant(4) PINNED,
    @WamEnumConstant(5) RANKER
}
