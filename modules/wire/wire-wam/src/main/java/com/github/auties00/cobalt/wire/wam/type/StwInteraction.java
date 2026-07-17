package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStwInteraction")
@WamEnum
public enum StwInteraction {
    @WamEnumConstant(0) ENTRY_POINT_SURFACED,
    @WamEnumConstant(1) ENTRY_POINT_CLICKED,
    @WamEnumConstant(2) LINK_SEARCH_CLICKED,
    @WamEnumConstant(3) IMAGE_SEARCH_CLICKED,
    @WamEnumConstant(4) TEXT_SEARCH_CLICKED,
    @WamEnumConstant(5) SEARCH_CLICKED,
    @WamEnumConstant(6) STW_DISMISSED,
    @WamEnumConstant(7) LEARN_MORE_CLICKED,
    @WamEnumConstant(8) LINK_OPEN_CLICKED,
    @WamEnumConstant(9) LINK_COPY_CLICKED,
    @WamEnumConstant(10) LINK_ADD_TO_READING_LIST,
    @WamEnumConstant(11) IMAGE_SEARCH_REDIRECT,
    @WamEnumConstant(12) IMAGE_SEARCH_FAILED
}
