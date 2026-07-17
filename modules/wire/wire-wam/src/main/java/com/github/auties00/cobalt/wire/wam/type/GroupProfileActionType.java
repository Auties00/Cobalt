package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupProfileActionType")
@WamEnum
public enum GroupProfileActionType {
    @WamEnumConstant(1) CHANGE_PROFILE_PHOTO,
    @WamEnumConstant(2) TAP_ACTION_ITEM_TAKE_PHOTO,
    @WamEnumConstant(3) TAP_ACTION_ITEM_VIEW_PHOTO,
    @WamEnumConstant(4) TAP_ACTION_ITEM_EMOJI_STICKER,
    @WamEnumConstant(5) TAP_ACTION_ITEM_UPLOAD_PHOTO,
    @WamEnumConstant(6) TAP_ACTION_ITEM_REMOVE_PHOTO,
    @WamEnumConstant(7) TAP_ACTION_ITEM_WEB_SEARCH,
    @WamEnumConstant(8) EMOJI_PANEL_OPEN,
    @WamEnumConstant(9) STICKER_PANEL_OPEN,
    @WamEnumConstant(10) PROFILE_PIC_UPDATED
}
