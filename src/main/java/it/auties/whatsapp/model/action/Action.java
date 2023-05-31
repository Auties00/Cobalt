package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.binary.PatchType;

/**
 * A model interface that represents an action
 */
public sealed interface Action extends ProtobufMessage permits AgentAction, AndroidUnsupportedActions, ArchiveChatAction, ChatAssignmentAction, ChatAssignmentOpenedStatusAction, ClearChatAction, ContactAction, DeleteChatAction, DeleteMessageForMeAction, FavoriteStickerAction, LabelAssociationAction, LabelEditAction, MarkChatAsReadAction, MuteAction, NuxAction, PinAction, PrimaryVersionAction, QuickReplyAction, RecentEmojiWeightsAction, RecentStickerWeightsAction, RemoveRecentStickerAction, StarAction, StickerAction, SubscriptionAction, TimeFormatAction, UserStatusMuteAction {
    /**
     * The name of this action
     *
     * @return a non-null string
     */
    String indexName();

    /**
     * The version of this action
     *
     * @return a non-null int
     */
    int version();

    /**
     * The type of this action
     *
     * @return a non-null type
     */
    PatchType type();
}
