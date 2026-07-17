package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumEditProfileAction")
@WamEnum
public enum EditProfileAction {
    @WamEnumConstant(1) ACTION_OPEN,
    @WamEnumConstant(2) ACTION_SAVE,
    @WamEnumConstant(3) ACTION_DISCARD,
    @WamEnumConstant(4) ACTION_PROFILE_FIELD_OPEN,
    @WamEnumConstant(5) ACTION_PROFILE_FIELD_SAVE,
    @WamEnumConstant(6) ACTION_PROFILE_FIELD_DISCARD,
    @WamEnumConstant(7) ACTION_FACEBOOK_ENABLED,
    @WamEnumConstant(8) ACTION_FACEBOOK_DISABLED,
    @WamEnumConstant(9) ACTION_INSTAGRAM_ENABLED,
    @WamEnumConstant(10) ACTION_INSTAGRAM_DISABLED,
    @WamEnumConstant(11) ACTION_FB_IG_POP_SHOWN,
    @WamEnumConstant(12) ACTION_EDIT_COVER_PHOTO_CLICK,
    @WamEnumConstant(13) ACTION_REMOVE_COVER_PHOTO,
    @WamEnumConstant(14) UPGRADE_TO_CUSTOM_LINK_CLICK,
    @WamEnumConstant(15) DIALOG_BOX_GEOCODE_IMPRESSION,
    @WamEnumConstant(16) DIALOG_BOX_GEOCODE_ACCEPT,
    @WamEnumConstant(17) DIALOG_BOX_GEOCODE_REVOKE
}
