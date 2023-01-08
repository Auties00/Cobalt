package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.info.MessageIndexInfo;

public interface OnAction
    extends Listener {

  /**
   * Called when the socket receives a sync from Whatsapp.
   *
   * @param action           the sync that was executed
   * @param messageIndexInfo the data about this action
   */
  @Override
  void onAction(Action action, MessageIndexInfo messageIndexInfo);
}
