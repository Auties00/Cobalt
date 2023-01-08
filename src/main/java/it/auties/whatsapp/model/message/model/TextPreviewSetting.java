package it.auties.whatsapp.model.message.model;

/**
 * The constants of this enumerated type describe the various types of text preview that can be
 * used
 */
public enum TextPreviewSetting {
  /**
   * Link previews will be generated. If a message contains an url without a schema(for example
   * wikipedia.com), the message will be autocorrected to include it and a preview will be
   * generated
   */
  ENABLED_WITH_INFERENCE,
  /**
   * Link previews will be generated. No inference will be used.
   */
  ENABLED,
  /**
   * Link previews will not be generated
   */
  DISABLED
}
