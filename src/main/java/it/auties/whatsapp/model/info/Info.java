package it.auties.whatsapp.model.info;

import it.auties.protobuf.base.ProtobufMessage;

public sealed interface Info extends ProtobufMessage
        permits AdReplyInfo, BusinessAccountInfo, BusinessIdentityInfo, CallInfo, ContextInfo, ExternalAdReplyInfo,
        MessageContextInfo, MessageInfo, NativeFlowInfo, NotificationMessageInfo, PaymentInfo, ProductListInfo,
        WebNotificationsInfo {
}
