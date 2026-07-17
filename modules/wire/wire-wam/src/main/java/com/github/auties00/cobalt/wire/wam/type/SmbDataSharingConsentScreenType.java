package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSmbDataSharingConsentScreenType")
@WamEnum
public enum SmbDataSharingConsentScreenType {
    @WamEnumConstant(0) SMB_DATA_SHARING_CONSENT_SCREEN_VIEW,
    @WamEnumConstant(1) SMB_DATA_SHARING_CONSENT_SCREEN_AGREE,
    @WamEnumConstant(2) SMB_DATA_SHARING_CONSENT_SCREEN_DISAGREE,
    @WamEnumConstant(3) SMB_DATA_SHARING_CONSENT_SCREEN_CANCEL,
    @WamEnumConstant(4) SMB_DATA_SHARING_OPT_OUT_CONFIRMATION_DIALOG_VIEW,
    @WamEnumConstant(5) SMB_DATA_SHARING_OPT_OUT_CONFIRMATION_DIALOG_CANCEL,
    @WamEnumConstant(6) SMB_DATA_SHARING_OPT_OUT_CONFIRMATION_DIALOG_CONFIRM
}
