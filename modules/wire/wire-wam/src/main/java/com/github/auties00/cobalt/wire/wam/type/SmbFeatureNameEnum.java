package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSmbFeatureNameEnum")
@WamEnum
public enum SmbFeatureNameEnum {
    @WamEnumConstant(0) NOTES,
    @WamEnumConstant(1) GEN_AI_AGENT,
    @WamEnumConstant(2) BROADCAST_LIST,
    @WamEnumConstant(3) BIZ_APP_ONBOARDING,
    @WamEnumConstant(4) BUSINESS_TOOLS_HOME,
    @WamEnumConstant(5) CATALOG,
    @WamEnumConstant(6) BUSINESS_PROFILE,
    @WamEnumConstant(7) LEARNING_HUB,
    @WamEnumConstant(8) BUSINESS_BROADCAST,
    @WamEnumConstant(9) ALERTS_CENTER,
    @WamEnumConstant(10) GOOGLE_ELIGIBILITY_OPT_OUT,
    @WamEnumConstant(11) QUICK_REPLY_REQUEST_CONTACT_INFO,
    @WamEnumConstant(12) BUSINESS_ACCOUNT_LINKING,
    @WamEnumConstant(13) DEVICE_LINKING,
    @WamEnumConstant(14) QUICK_REPLY,
    @WamEnumConstant(15) AUTOMATED_MESSAGE,
    @WamEnumConstant(16) CONSUMER_DOWNGRADE,
    @WamEnumConstant(17) CUSTOMER_MANAGER,
    @WamEnumConstant(18) AUTHORIZED_AGENT,
    @WamEnumConstant(19) BUSINESS_TOOLS_REC_CARD,
    @WamEnumConstant(20) LISTS_CREATION,
    @WamEnumConstant(21) LIST_APPLICATION,
    @WamEnumConstant(22) SEND_QUICK_REPLY,
    @WamEnumConstant(23) STATUS_CROSSPOST
}
