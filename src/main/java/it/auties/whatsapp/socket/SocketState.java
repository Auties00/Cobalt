package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.DisconnectReason;

enum SocketState {
  WAITING,
  CONNECTED,
  DISCONNECTED,
  RECONNECTING,
  LOGGED_OUT,
  RESTORE;

  static SocketState of(DisconnectReason reason) {
    return switch (reason) {
      case DISCONNECTED -> DISCONNECTED;
      case RECONNECTING -> RECONNECTING;
      case LOGGED_OUT -> LOGGED_OUT;
      case RESTORE -> RESTORE;
    };
  }

  boolean isDisconnected() {
    return this == DISCONNECTED || this == LOGGED_OUT;
  }

  DisconnectReason toReason() {
    return switch (this) {
      case WAITING, CONNECTED, RECONNECTING -> DisconnectReason.RECONNECTING;
      case DISCONNECTED -> DisconnectReason.DISCONNECTED;
      case LOGGED_OUT -> DisconnectReason.LOGGED_OUT;
      case RESTORE -> DisconnectReason.RESTORE;
    };
  }
}
