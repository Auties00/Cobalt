package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPrivacySettingsValueType")
@WamEnum
public enum PrivacySettingsValueType {
    @WamEnumConstant(1) NOBODY,
    @WamEnumConstant(2) ONLY_SHARE_WITH,
    @WamEnumConstant(3) MY_CONTACTS,
    @WamEnumConstant(4) MY_CONTACTS_EXCEPT,
    @WamEnumConstant(5) EVERYONE,
    @WamEnumConstant(6) KNOWN,
    @WamEnumConstant(7) CLOSE_FRIENDS,
    @WamEnumConstant(8) CUSTOM_LIST
}
