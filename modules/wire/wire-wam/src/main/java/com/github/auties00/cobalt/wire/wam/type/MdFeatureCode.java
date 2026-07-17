package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdFeatureCode")
@WamEnum
public enum MdFeatureCode {
    @WamEnumConstant(1) PIN_MUTATION,
    @WamEnumConstant(2) UNPIN_4TH_CHAT_MUTATION,
    @WamEnumConstant(3) DELETE_MUTATION,
    @WamEnumConstant(4) CLEAR_CHAT_REMOVE_STARRED_MUTATION,
    @WamEnumConstant(5) CLEAR_CHAT_KEEP_STARRED_MUTATION
}
