package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MerchantTypeType;
import com.github.auties00.cobalt.wire.wam.type.P2mTypeType;
import com.github.auties00.cobalt.wire.wam.type.PaymentActionTargets;
import com.github.auties00.cobalt.wire.wam.type.PaymentActionTypes;
import com.github.auties00.cobalt.wire.wam.type.PaymentModeTypes;
import com.github.auties00.cobalt.wire.wam.type.PaymentTransactionStatusType;
import com.github.auties00.cobalt.wire.wam.type.PaymentsContactsBucketType;
import com.github.auties00.cobalt.wire.wam.type.PaymentsIqCall;
import com.github.auties00.cobalt.wire.wam.type.PaymentsRequestNameType;
import com.github.auties00.cobalt.wire.wam.type.PaymentsResponseResultType;
import com.github.auties00.cobalt.wire.wam.type.PaymentsUpiCheckPinUserErrorReasonType;
import com.github.auties00.cobalt.wire.wam.type.PaymentsVerifyCardResultType;
import com.github.auties00.cobalt.wire.wam.type.UpiPaymentsPspIdType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPaymentsUserActionWamEvent")
@WamEvent(id = 2162)
public interface PaymentsUserActionEvent extends WamEventSpec {
    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<PaymentActionTargets> actionTarget();

    @WamProperty(index = 43, type = WamType.ENUM)
    Optional<MerchantTypeType> merchantType();

    @WamProperty(index = 44, type = WamType.ENUM)
    Optional<P2mTypeType> p2mType();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong paymentAccountRowSelected();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<PaymentActionTypes> paymentActionType();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<PaymentModeTypes> paymentMode();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong paymentNumberOfAccountsAvailable();

    @WamProperty(index = 32, type = WamType.INTEGER)
    OptionalLong paymentNumberOfPeopleInvited();

    @WamProperty(index = 33, type = WamType.BOOLEAN)
    Optional<Boolean> paymentPinSetUp();

    @WamProperty(index = 34, type = WamType.BOOLEAN)
    Optional<Boolean> paymentSent();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> paymentSmsProviderNumber();

    @WamProperty(index = 45, type = WamType.ENUM)
    Optional<PaymentTransactionStatusType> paymentTransactionStatus();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> paymentsAccountsExist();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> paymentsBankId();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong paymentsBanksRowSelected();

    @WamProperty(index = 21, type = WamType.BOOLEAN)
    Optional<Boolean> paymentsBanksScrolled();

    @WamProperty(index = 18, type = WamType.BOOLEAN)
    Optional<Boolean> paymentsBanksSearchActivated();

    @WamProperty(index = 20, type = WamType.BOOLEAN)
    Optional<Boolean> paymentsBanksSearchSelected();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> paymentsBanksSearchString();

    @WamProperty(index = 25, type = WamType.STRING)
    Optional<String> paymentsBanksSelectedName();

    @WamProperty(index = 31, type = WamType.ENUM)
    Optional<PaymentsContactsBucketType> paymentsContactsBucket();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> paymentsCountryCode();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> paymentsErrorCode();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> paymentsErrorText();

    @WamProperty(index = 47, type = WamType.STRING)
    Optional<String> paymentsErrorTitle();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> paymentsEventId();

    @WamProperty(index = 29, type = WamType.BOOLEAN)
    Optional<Boolean> paymentsHasMultipleSims();

    @WamProperty(index = 42, type = WamType.ENUM)
    Optional<PaymentsIqCall> paymentsIqCallStatus();

    @WamProperty(index = 40, type = WamType.BOOLEAN)
    Optional<Boolean> paymentsIsMandate();

    @WamProperty(index = 36, type = WamType.BOOLEAN)
    Optional<Boolean> paymentsIsOrder();

    @WamProperty(index = 41, type = WamType.STRING)
    Optional<String> paymentsMandate();

    @WamProperty(index = 38, type = WamType.STRING)
    Optional<String> paymentsOrderType();

    @WamProperty(index = 39, type = WamType.STRING)
    Optional<String> paymentsP2mPaymentConfigId();

    @WamProperty(index = 17, type = WamType.ENUM)
    Optional<PaymentsRequestNameType> paymentsRequestName();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong paymentsRequestRetryCount();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong paymentsRequestRetryTimeDelaySeconds();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<PaymentsResponseResultType> paymentsResponseResult();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong paymentsSmsProviderRetryCount();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong paymentsSmsRequestRetryCount();

    @WamProperty(index = 28, type = WamType.ENUM)
    Optional<PaymentsUpiCheckPinUserErrorReasonType> paymentsUpiCheckPinErrorReason();

    @WamProperty(index = 30, type = WamType.ENUM)
    Optional<PaymentsVerifyCardResultType> paymentsVerifyCardResult();

    @WamProperty(index = 46, type = WamType.STRING)
    Optional<String> previousScreenName();

    @WamProperty(index = 35, type = WamType.STRING)
    Optional<String> queryParams();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> referral();

    @WamProperty(index = 37, type = WamType.STRING)
    Optional<String> referralContext();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> screen();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<UpiPaymentsPspIdType> upiPaymentsPspId();
}
