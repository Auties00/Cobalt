package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcChatCreateCreationMethod")
@WamEnum
public enum WebcChatCreateCreationMethod {
    @WamEnumConstant(0) MISSING_WHEN_SAVING_MESSAGE
}
