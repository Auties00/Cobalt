package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatsFolderType")
@WamEnum
public enum ChatsFolderType {
    @WamEnumConstant(1) INBOX,
    @WamEnumConstant(2) ARCHIVED,
    @WamEnumConstant(3) BUSINESS_FOLDER
}
