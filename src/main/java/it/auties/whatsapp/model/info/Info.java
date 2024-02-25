package it.auties.whatsapp.model.info;

public sealed interface Info permits AdReplyInfo, BusinessIdentityInfo, ContextInfo, DeviceContextInfo, ExternalAdReplyInfo, MessageIndexInfo, MessageInfo, MessageStatusInfo, NativeFlowInfo, NotificationMessageInfo, PaymentInfo, ProductListInfo, WebNotificationsInfo {

}
