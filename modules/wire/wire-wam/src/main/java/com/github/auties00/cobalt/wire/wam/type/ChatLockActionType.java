package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatLockActionType")
@WamEnum
public enum ChatLockActionType {
    @WamEnumConstant(0) CHAT_INFO_IMPRESSION,
    @WamEnumConstant(1) LOCK_CHAT_SCREEN_IMPRESSION,
    @WamEnumConstant(2) AUTH_INITIATED,
    @WamEnumConstant(3) AUTH_SUCCEEDED,
    @WamEnumConstant(4) SET_UP_AUTH_IMPRESSION,
    @WamEnumConstant(5) ADD_CHAT_LOCK,
    @WamEnumConstant(6) REMOVE_CHAT_LOCK,
    @WamEnumConstant(7) CLEAR_AND_UNLOCK_IMPRESSION,
    @WamEnumConstant(8) CLEAR_AND_UNLOCK_SUCCESS,
    @WamEnumConstant(9) CLEAR_AND_UNLOCK_FAILURE,
    @WamEnumConstant(10) AUTH_FAILURE,
    @WamEnumConstant(11) BOTTOM_SHEET_IMPRESSION,
    @WamEnumConstant(12) BOTTOM_SHEET_CONTINUE_CLICK,
    @WamEnumConstant(13) BOTTOM_SHEET_LEARN_MORE_CLICK,
    @WamEnumConstant(14) FOLDER_OPEN,
    @WamEnumConstant(15) FORGOT_SECRET_CODE,
    @WamEnumConstant(16) UNLOCK_AND_CLEAR_WARNING,
    @WamEnumConstant(17) USE_SECRET_CODE_AUTH
}
