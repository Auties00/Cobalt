package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSetting")
@WamEnum
public enum Setting {
    @WamEnumConstant(1) ALL_CONTACTS,
    @WamEnumConstant(2) ALL_CONTACTS_EXCEPT,
    @WamEnumConstant(3) ONLY_SHARE_WITH_CONTACTS,
    @WamEnumConstant(4) ONLY_SHARE_WITH_CLOSE_FRIENDS,
    @WamEnumConstant(5) ONLY_SHARE_WITH_CUSTOM_LIST
}
