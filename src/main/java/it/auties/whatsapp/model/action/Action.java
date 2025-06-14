package it.auties.whatsapp.model.action;

/**
 * A model interface that represents an action
 */
public sealed interface Action permits AgentAction, AndroidUnsupportedActions, ArchiveChatAction, ChatAssignmentAction, ChatAssignmentOpenedStatusAction, ClearChatAction, ContactAction, DeleteChatAction, DeleteMessageForMeAction, LabelAssociationAction, LabelEditAction, MarkChatAsReadAction, MuteAction, NuxAction, PinAction, PrimaryVersionAction, QuickReplyAction, RecentEmojiWeightsAction, RemoveRecentStickerAction, StarAction, StickerAction, SubscriptionAction, TimeFormatAction, UserStatusMuteAction {
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
    int actionVersion();
}
