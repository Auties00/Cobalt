package it.auties.whatsapp.model.info;

public sealed interface Info permits AdReplyInfo, BusinessIdentityInfo, ContextInfo,
        ExternalAdReplyInfo, DeviceContextInfo, MessageInfo, NativeFlowInfo,
        NotificationMessageInfo, PaymentInfo, ProductListInfo, WebNotificationsInfo, MessageIndexInfo {

}
