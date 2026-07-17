package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUsernameCreationCurrentScreen")
@WamEnum
public enum UsernameCreationCurrentScreen {
    @WamEnumConstant(1) USERNAME_MANAGE,
    @WamEnumConstant(2) USERNAME_EDUCATION,
    @WamEnumConstant(3) USERNAME_EDIT,
    @WamEnumConstant(4) USERNAME_EDIT_OPTIONS,
    @WamEnumConstant(5) USERNAME_DELETE_DIALOG,
    @WamEnumConstant(6) USERNAME_COMPLETE,
    @WamEnumConstant(7) USERNAME_PIN_EDUCATION,
    @WamEnumConstant(8) USERNAME_PIN_UPSELL,
    @WamEnumConstant(9) USERNAME_PIN_MANAGE,
    @WamEnumConstant(10) USERNAME_PIN_EDIT,
    @WamEnumConstant(11) USERNAME_PIN_DELETE_DIALOG,
    @WamEnumConstant(12) BACKGROUND_NO_UI,
    @WamEnumConstant(13) USE_EXISTING_FOA_USERNAME_VIA_LINK_UPSELL_BOTTOMSHEET,
    @WamEnumConstant(14) USE_EXISTING_FOA_LINKED_USERNAMES_BOTTOMSHEET,
    @WamEnumConstant(15) FOA_USERNAME_NOT_AVAILABLE_BOTTOMSHEET,
    @WamEnumConstant(16) ACCOUNT_CENTER_HELP_ARTICLE,
    @WamEnumConstant(17) FB_ACCOUNT_ALREADY_LINKED_DIALOG,
    @WamEnumConstant(18) SMB_LINKING_BOTTOMSHEET,
    @WamEnumConstant(19) USERNAME_PIN_GENERATOR
}
