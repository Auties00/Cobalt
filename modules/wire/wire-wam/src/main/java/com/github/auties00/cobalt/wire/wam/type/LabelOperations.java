package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLabelOperations")
@WamEnum
public enum LabelOperations {
    @WamEnumConstant(1) ADD,
    @WamEnumConstant(2) EDIT,
    @WamEnumConstant(3) DELETE,
    @WamEnumConstant(4) VIEW,
    @WamEnumConstant(5) CLICK_POSITIVE,
    @WamEnumConstant(6) CLICK_NEGATIVE,
    @WamEnumConstant(7) UPDATE_LABEL_COUNT,
    @WamEnumConstant(8) AUTO_ADDED,
    @WamEnumConstant(9) REORDER,
    @WamEnumConstant(10) RENAME,
    @WamEnumConstant(11) UPDATE_MEMBERS,
    @WamEnumConstant(12) MUTE,
    @WamEnumConstant(13) UNMUTE,
    @WamEnumConstant(14) SUGGESTION_CLICKED,
    @WamEnumConstant(15) UPDATED_COLOR,
    @WamEnumConstant(16) SWIPE
}
