package it.auties.whatsapp.protobuf.action;

public sealed interface Action permits AndroidUnsupportedActions, ArchiveChatAction, ClearChatAction, ContactAction,
        DeleteChatAction, DeleteMessageForMeAction, FavoriteStickerAction, LabelAssociationAction, LabelEditAction,
        MarkChatAsReadAction, MuteAction, PinAction, QuickReplyAction, StarAction {
}
