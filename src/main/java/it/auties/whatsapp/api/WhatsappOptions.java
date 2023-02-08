package it.auties.whatsapp.api;

import it.auties.whatsapp.api.WhatsappOptions.MobileOptions;
import it.auties.whatsapp.api.WhatsappOptions.WebOptions;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.message.model.TextPreviewSetting;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.util.KeyHelper;
import java.util.function.Function;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that contains the registration options for a {@link Whatsapp} instance
 */
@SuperBuilder
@Data
@Accessors(fluent = true)
public abstract sealed class WhatsappOptions permits WebOptions, MobileOptions {
  /**
   * Constant for unlimited listeners size
   */
  private static final int UNLIMITED_LISTENERS = -1;

  /**
   * The id of the session. This id needs to be unique. By default, a random integer.
   */
  @Default
  private int id = KeyHelper.registrationId();

  /**
   * Whether listeners marked with @RegisteredListener should be registered automatically. By
   * default, this option is enabled.
   */
  @Default
  private boolean autodetectListeners = true;

  /**
   * Whether the api should automatically subscribe to all contacts' presences to have them always
   * up to date. Alternatively, you can subscribe manually to the ones you need using
   * {@link Whatsapp#subscribeToPresence(ContactJidProvider)}
   */
  @Default
  private boolean autoSubscribeToPresences = true;

  /**
   * Whether the default serialization mechanism should be used or not. Set this to false if you
   * want to implement a custom serializer.
   */
  @Default
  private boolean defaultSerialization = true;

  /**
   * Whether a preview should be automatically generated and attached to text messages that
   * contain links. By default, it's enabled with inference.
   */
  @Default
  private TextPreviewSetting textPreviewSetting = TextPreviewSetting.ENABLED_WITH_INFERENCE;

  /**
   * Handles failures in the WebSocket. By default, uses the simple handler and prints to the
   * terminal.
   */
  @Default
  private ErrorHandler errorHandler = ErrorHandler.toTerminal();

  /**
   * The number of maximum listeners that the linked Whatsapp instance supports. By default,
   * unlimited.
   */
  @Default
  private int listenersLimit = UNLIMITED_LISTENERS;

  /**
   * The version of WhatsappWeb to use. If the version is too outdated, the server will refuse to
   * connect.
   */
  @Default
  private final Version version = Version.latest();

  /**
   * Returns the type of client
   * 
   * @return the non-null client type
   */
  @NonNull
  public abstract ClientType clientType();
  
  /**
   * Options for the web client
   */
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @Data
  @Accessors(fluent = true)
  public final static class WebOptions extends WhatsappOptions {
    /**
     * The description provided to Whatsapp during the authentication process. This should be, for
     * example, the name of your service. By default, it's WhatsappWeb4j.
     */
    @Default
    @NonNull
    private String name = "WhatsappWeb4j";
  
    /**
     * Describes how much chat history Whatsapp should send when the QR is first scanned. By default,
     * one year.
     */
    @Default
    @NonNull
    private HistoryLength historyLength = HistoryLength.ONE_YEAR;
  
    /**
     * Handles the qr countryCode when a connection is first established with Whatsapp. By default, the qr
     * countryCode is printed on the terminal.
     */
    @Default
    @NonNull
    private QrHandler qrHandler = QrHandler.toTerminal();

    /**
     * Constructs a new instance with default options
     *
     * @return a non-null instance
     */
    public static WebOptions defaultOptions() {
      return WebOptions.builder().build();
    }

    /**
     * Returns the type of client
     *
     * @return the non-null client type
     */
    @NonNull
    public ClientType clientType() {
      return ClientType.WEB_CLIENT;
    }
  }

  /**
   * Options for the app client
   */
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @Data
  @Accessors(fluent = true)
  public final static class MobileOptions extends WhatsappOptions {
    /**
     * The phone number to register, including the prefix
     */
    @NonNull
    private String phoneNumber;

    /**
     * The method to use to confirm the phone number
     */
    @Default
    @NonNull
    private VerificationCodeMethod verificationCodeMethod = VerificationCodeMethod.SMS;
    
    /**
     * A function to retrieve the OTP sent to the registered phone number
     */
    @NonNull
    private Function<VerificationCodeMethod, String> verificationCodeHandler;

    /**
     * Returns the type of client
     *
     * @return the non-null client type
     */
    @NonNull
    public ClientType clientType() {
      return ClientType.APP_CLIENT;
    }
  }
}
