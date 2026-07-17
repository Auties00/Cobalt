package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPrivacyHighlightSurfaceEnum")
@WamEnum
public enum PrivacyHighlightSurfaceEnum {
    @WamEnumConstant(0) GOLDEN_BOX_CONTACT,
    @WamEnumConstant(1) GOLDEN_BOX_GROUP,
    @WamEnumConstant(2) GOLDEN_BOX_BROADCAST,
    @WamEnumConstant(3) INFO_SCREEN_CONTACT,
    @WamEnumConstant(4) INFO_SCREEN_GROUP,
    @WamEnumConstant(5) INFO_SCREEN_BROADCAST,
    @WamEnumConstant(6) CALLS_LIST,
    @WamEnumConstant(7) CHATS_LIST,
    @WamEnumConstant(8) STATUS_LIST,
    @WamEnumConstant(9) LINKED_DEVICES_SCREEN,
    @WamEnumConstant(10) CALLING_SCREEN_AUDIO,
    @WamEnumConstant(11) CALLING_SCREEN_VIDEO,
    @WamEnumConstant(12) SPLIT_VIEW_HOME_PLACEHOLDER
}
