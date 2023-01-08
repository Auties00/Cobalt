package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.request.Node;

public interface OnNodeSent
    extends Listener {

  /**
   * Called when the socket sends a node to Whatsapp
   *
   * @param outgoing the non-null node that was just sent
   */
  @Override
  void onNodeSent(Node outgoing);
}