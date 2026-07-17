package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPaymentsRequestNameType")
@WamEnum
public enum PaymentsRequestNameType {
    @WamEnumConstant(1) LIST_KEYS,
    @WamEnumConstant(2) GET_TOKEN,
    @WamEnumConstant(3) UPI_BATCH,
    @WamEnumConstant(4) GET_BANKS,
    @WamEnumConstant(5) REGISTER,
    @WamEnumConstant(6) SET_PIN,
    @WamEnumConstant(7) CHANGE_PIN,
    @WamEnumConstant(8) GET_VPA,
    @WamEnumConstant(9) VPA_SYNC,
    @WamEnumConstant(10) GET_ONE_TRANSACTION,
    @WamEnumConstant(11) GET_TRANSACTIONS,
    @WamEnumConstant(12) GET_METHODS,
    @WamEnumConstant(13) REMOVE_ONE_ACCOUNT,
    @WamEnumConstant(14) DEREGISTER,
    @WamEnumConstant(15) CHANGE_PRIMARY,
    @WamEnumConstant(16) GENERATE_OTP,
    @WamEnumConstant(17) SET_TOS,
    @WamEnumConstant(18) GET_ACCOUNTS,
    @WamEnumConstant(19) SEND_UPI_RAISE_COMPLAINT,
    @WamEnumConstant(20) DEVICE_BINDING,
    @WamEnumConstant(21) PRECHECK,
    @WamEnumConstant(22) REGISTER_ALIAS,
    @WamEnumConstant(23) DEREGISTER_ALIAS,
    @WamEnumConstant(24) PORT_ALIAS,
    @WamEnumConstant(25) ACCOUNT_RECOVERY,
    @WamEnumConstant(26) RECOVER_ACCOUNT,
    @WamEnumConstant(27) RETOKENIZE_CARD,
    @WamEnumConstant(28) TRANSACTION_STATUS_UPDATE,
    @WamEnumConstant(29) CHECK_BALANCE,
    @WamEnumConstant(30) CHECK_PIN,
    @WamEnumConstant(31) COLLECT_REQUEST,
    @WamEnumConstant(32) ACTIVATE_INTERNATIONAL_PAYMENTS,
    @WamEnumConstant(33) DEACTIVATE_INTERNATIONAL_PAYMENTS,
    @WamEnumConstant(34) VALIDATE_INTERNATIONAL_QR,
    @WamEnumConstant(35) PAYMENT_NOTIFICATION,
    @WamEnumConstant(36) GET_VPA_NAME,
    @WamEnumConstant(37) SEND_TO_VPA,
    @WamEnumConstant(38) REGISTER_INIT,
    @WamEnumConstant(39) REGISTER_ALL
}
