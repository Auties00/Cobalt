package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumViewBusinessProfileAction")
@WamEnum
public enum ViewBusinessProfileAction {
    @WamEnumConstant(1) ACTION_IMPRESSION,
    @WamEnumConstant(2) ACTION_CLICK_WEBSITE,
    @WamEnumConstant(3) ACTION_CLICK_LOCATION,
    @WamEnumConstant(4) ACTION_CLICK_HOURS,
    @WamEnumConstant(5) ACTION_CLICK_DESCRIPTION,
    @WamEnumConstant(6) ACTION_CLICK_EMAIL,
    @WamEnumConstant(7) ACTION_CLICK_ADDTOCONTACT,
    @WamEnumConstant(8) ACTION_CLICK_MORE_BIZ_PROFILE,
    @WamEnumConstant(9) ACTION_CLICK_MESSAGE,
    @WamEnumConstant(10) ACTION_CLICK_VOICE_CALL,
    @WamEnumConstant(11) ACTION_CLICK_VIDEO_CALL,
    @WamEnumConstant(12) ACTION_CLICK_FORWARD,
    @WamEnumConstant(13) ACTION_CLICK_CATALOG_ICON,
    @WamEnumConstant(14) ACTION_CLICK_SHOPS_ICON,
    @WamEnumConstant(15) ACTION_CLICK_APP_LINK,
    @WamEnumConstant(16) ACTION_APP_IMPRESSION,
    @WamEnumConstant(17) ACTION_CLICK_STATUS,
    @WamEnumConstant(18) ACTION_EXIT,
    @WamEnumConstant(19) ACTION_COVER_PHOTO_IMPRESSION,
    @WamEnumConstant(20) ACTION_CLICK_VERIFIED_BADGE,
    @WamEnumConstant(21) ACTION_LOAD
}
