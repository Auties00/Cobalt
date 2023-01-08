package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufMessage;

/**
 * A model interface that represents an action
 */
public sealed interface Action
    extends ProtobufMessage
    permits AgentAction, AndroidUnsupportedActions, ArchiveChatAction, ChatAssignmentAction,
    ChatAssignmentOpenedStatusAction, ClearChatAction, ContactAction, DeleteChatAction,
    DeleteMessageForMeAction, FavoriteStickerAction, LabelAssociationAction, LabelEditAction,
    MarkChatAsReadAction, MuteAction, NuxAction, PinAction, PrimaryVersionAction, QuickReplyAction,
    RecentEmojiWeightsAction, RecentStickerWeightsAction, RemoveRecentStickerAction, StarAction,
    StickerAction, SubscriptionAction, TimeFormatAction, UserStatusMuteAction {

  /**
   * The name of this action
   *
   * @return a non-null string
   * @throws UnsupportedOperationException if this action cannot be serialized
   */
  String indexName();
}
