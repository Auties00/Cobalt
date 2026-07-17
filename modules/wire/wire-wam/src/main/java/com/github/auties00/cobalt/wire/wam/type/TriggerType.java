package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumTriggerType")
@WamEnum
public enum TriggerType {
    @WamEnumConstant(1) CHAT_ENTRY,
    @WamEnumConstant(2) SYSTEM_MESSAGE,
    @WamEnumConstant(3) KEEP_MESSAGE_FIRST_TIME,
    @WamEnumConstant(4) USER_MESSAGE_KEPT,
    @WamEnumConstant(5) KEPT_FOLDER_TAP_FIRST_TIME,
    @WamEnumConstant(6) UNKEEP_MESSAGE_FIRST_TIME,
    @WamEnumConstant(7) EPHEMERAL_SETTINGS
}
