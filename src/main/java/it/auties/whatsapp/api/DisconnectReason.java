package it.auties.whatsapp.api;

/**
 * The constants of this enumerated type describe the various reasons for which a session can be
 * terminated
 */
public enum DisconnectReason {
  /**
   * Default reason
   */
  DISCONNECTED,
  /**
   * Reconnect
   */
  RECONNECTING,
  /**
   * Logged out
   */
  LOGGED_OUT
}
