package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPresetType")
@WamEnum
public enum PresetType {
    @WamEnumConstant(1) FREE_TO_CHAT,
    @WamEnumConstant(2) SLOW_TO_RESPOND,
    @WamEnumConstant(3) HANGING_WITH_FRIENDS,
    @WamEnumConstant(4) TRAVELING,
    @WamEnumConstant(5) EXCITED,
    @WamEnumConstant(6) HAPPY_NEW_YEAR,
    @WamEnumConstant(7) HAPPY_ST_PATRICKS_DAY,
    @WamEnumConstant(8) HAPPY_EASTER,
    @WamEnumConstant(9) FIFA_WORLDCUP_WATCHING_THE_MATCH,
    @WamEnumConstant(10) FIFA_WORLDCUP_EXCITED_FOR_FINALS,
    @WamEnumConstant(11) PREVIOUS_SET_ABOUT
}
