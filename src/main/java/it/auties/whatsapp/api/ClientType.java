package it.auties.whatsapp.api;

/**
 * The constants of this enumerated type describe the various types of API that can be used to make
 * {@link Whatsapp} work
 */
public enum ClientType {
  /**
   * A standalone client that requires the QR countryCode to be scanned by its companion on log-in Reversed
   * from <a href="https://web.whatsapp.com">Whatsapp Web Client</a>
   */
  WEB_CLIENT,
  /**
   * A standalone client that requires an SMS countryCode sent to the companion's phone number on log-in
   * Reversed from <a href="https://github.com/tgalal/yowsup/issues/2910">KaiOS Mobile App</a>
   */
  APP_CLIENT
}
