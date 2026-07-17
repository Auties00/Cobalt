package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDefenseModeClickDesiredState")
@WamEnum
public enum DefenseModeClickDesiredState {
    @WamEnumConstant(1) NOBODY,
    @WamEnumConstant(2) ONLY_SHARE_WITH,
    @WamEnumConstant(3) MY_CONTACTS,
    @WamEnumConstant(4) MY_CONTACTS_EXCEPT,
    @WamEnumConstant(5) EVERYONE,
    @WamEnumConstant(6) KNOWN,
    @WamEnumConstant(7) CLOSE_FRIENDS,
    @WamEnumConstant(11) MATCH_LAST_SEEN,
    @WamEnumConstant(8) OFF,
    @WamEnumConstant(9) ON,
    @WamEnumConstant(10) ON_STANDARD
}
