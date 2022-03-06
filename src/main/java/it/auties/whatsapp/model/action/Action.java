package it.auties.whatsapp.model.action;

import java.util.List;

public sealed interface Action permits AndroidUnsupportedActions, ArchiveChatAction,
        ClearChatAction, ContactAction, DeleteChatAction, DeleteMessageForMeAction,
        FavoriteStickerAction, LabelAssociationAction, LabelEditAction, MarkChatAsReadAction,
        MuteAction, PinAction, QuickReplyAction, RecentEmojiWeightsAction, RecentStickerWeightsAction, StarAction {

    String indexName();

    default List<String> indexArguments(){
        return List.of();
    }
}
