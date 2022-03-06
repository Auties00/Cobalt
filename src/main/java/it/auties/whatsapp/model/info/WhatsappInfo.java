package it.auties.whatsapp.model.info;

public sealed interface WhatsappInfo permits AdReplyInfo, BusinessAccountInfo, BusinessIdentityInfo, CallInfo, ContextInfo, ExternalAdReplyInfo, MessageInfo, NativeFlowInfo, NotificationMessageInfo, PaymentInfo, ProductListInfo, WebNotificationsInfo {
}
