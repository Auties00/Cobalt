package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufMessage;

public sealed interface Action extends ProtobufMessage
        permits AndroidUnsupportedActions, ArchiveChatAction, ClearChatAction, ContactAction, DeleteChatAction,
        DeleteMessageForMeAction, FavoriteStickerAction, LabelAssociationAction, LabelEditAction, MarkChatAsReadAction,
        MuteAction, PinAction, QuickReplyAction, RecentEmojiWeightsAction, RecentStickerWeightsAction, StarAction {

    String indexName();
}
