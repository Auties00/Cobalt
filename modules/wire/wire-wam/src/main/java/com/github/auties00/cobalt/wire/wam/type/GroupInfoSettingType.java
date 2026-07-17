package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupInfoSettingType")
@WamEnum
public enum GroupInfoSettingType {
    @WamEnumConstant(1) ADMINS_ONLY,
    @WamEnumConstant(2) ALL_PARTICIPANTS
}
