package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStickerSendOriginType")
@WamEnum
public enum StickerSendOriginType {
    @WamEnumConstant(1) STICKER_SEARCH,
    @WamEnumConstant(2) FORWARD,
    @WamEnumConstant(3) STICKER_PICKER_TAB_RECENTS,
    @WamEnumConstant(4) STICKER_PICKER_TAB_FAVORITES,
    @WamEnumConstant(5) STICKER_PICKER_TAB_EMOTION,
    @WamEnumConstant(6) STICKER_PICKER_TAB_PACK,
    @WamEnumConstant(7) STICKER_PICKER_TAB_CONTEXTUAL_SUGGESTIONS,
    @WamEnumConstant(8) STICKER_MAKER,
    @WamEnumConstant(9) STICKER_STORE,
    @WamEnumConstant(10) AI_STICKER_CREATE,
    @WamEnumConstant(11) AI_STICKER_CREATE_TRAY,
    @WamEnumConstant(12) AI_STICKER_CREATE_CHAT,
    @WamEnumConstant(13) STATUS_QUICK_REPLY,
    @WamEnumConstant(14) DISCOVERY_PACK,
    @WamEnumConstant(15) STICKER_FROM_DEVICE_KEYBOARD,
    @WamEnumConstant(16) STICKER_PACK_INFO,
    @WamEnumConstant(17) CONVERSATION_STARTER,
    @WamEnumConstant(18) STICKER_SEARCH_TRENDING
}
