package it.auties.whatsapp.model.info;

import it.auties.protobuf.api.model.ProtobufMessage;

public sealed interface Info extends ProtobufMessage permits AdReplyInfo, BusinessAccountInfo, BusinessIdentityInfo, CallInfo,
        ContextInfo, ExternalAdReplyInfo, MessageInfo, NativeFlowInfo, NotificationMessageInfo,
        PaymentInfo, ProductListInfo, WebNotificationsInfo {
}
