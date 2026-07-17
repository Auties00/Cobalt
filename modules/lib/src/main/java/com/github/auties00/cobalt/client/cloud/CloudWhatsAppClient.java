package com.github.auties00.cobalt.client.cloud;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudApiException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudAuthException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudUnsupportedVersionException;
import com.github.auties00.cobalt.listener.*;
import com.github.auties00.cobalt.listener.cloud.*;
import com.github.auties00.cobalt.meta.annotation.WhatsAppCloudMethod;
import com.github.auties00.cobalt.meta.model.WhatsAppCloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.*;
import com.github.auties00.cobalt.wire.cloud.template.*;
import com.github.auties00.cobalt.wire.cloud.template.library.*;
import com.github.auties00.cobalt.wire.cloud.analytics.*;
import com.github.auties00.cobalt.wire.cloud.flow.*;
import com.github.auties00.cobalt.wire.cloud.phone.*;
import com.github.auties00.cobalt.wire.cloud.signup.*;
import com.github.auties00.cobalt.wire.cloud.commerce.*;
import com.github.auties00.cobalt.wire.cloud.waba.*;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.core.message.MessageKey;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;

/**
 * Contract for the Meta WhatsApp Cloud API client flavour.
 *
 * <p>This client drives the WhatsApp Business Platform Cloud API: outbound traffic is a sequence of
 * stateless HTTPS requests to {@code graph.facebook.com}, and inbound traffic is delivered to a
 * webhook receiver the client hosts. It shares the cross-transport surface declared on
 * {@link WhatsAppClient} (lifecycle, listener registration, sending, reactions, read receipts,
 * business profile, blocking) and adds the management operations unique to the Cloud API: media
 * upload/download, message-template CRUD, phone-number registration and verification, webhook
 * subscription, the Calling API, Flows, and QR short-links.
 *
 * <p>Inbound events are surfaced through {@link CloudWhatsAppClientListener}
 * and its single-method listener relatives, registered either through the generic
 * {@link #addListener(CloudListener)} or the typed {@code addXxxListener} convenience methods.
 *
 * @apiNote
 * Build an instance with {@link WhatsAppClient#builder()}{@code .cloudApi()}. The client is inert until
 * {@link #connect()} validates the access token and starts the webhook receiver.
 *
 * @see WhatsAppClient
 * @see CloudWhatsAppClientBuilder
 */
public sealed interface CloudWhatsAppClient extends WhatsAppClient<CloudWhatsAppClient> permits LiveCloudWhatsAppClient {
    /**
     * Returns the entry point for assembling a configured {@link CloudWhatsAppClient}, backed by the
     * {@link CloudWhatsAppStoreFactory#persistent() persistent} store factory.
     *
     * @apiNote
     * Embedders chain the connection step
     * ({@link CloudWhatsAppClientBuilder}) followed by the optional configuration
     * steps to obtain a ready {@code CloudWhatsAppClient}.
     *
     * @return a fresh {@link CloudWhatsAppClientBuilder}
     */
    static CloudWhatsAppClientBuilder builder() {
        return new CloudWhatsAppClientBuilder();
    }

    /**
     * Returns the entry point for assembling a configured {@link CloudWhatsAppClient}, backed by the
     * given store factory.
     *
     * @apiNote
     * Supply {@link CloudWhatsAppStoreFactory#temporary()} for a RAM-only session or
     * {@link CloudWhatsAppStoreFactory#persistent(Path)} for a custom storage directory.
     *
     * @param storeFactory the factory that resolves the backing store
     * @return a fresh {@link CloudWhatsAppClientBuilder}
     * @throws NullPointerException if {@code storeFactory} is {@code null}
     */
    static CloudWhatsAppClientBuilder builder(CloudWhatsAppStoreFactory storeFactory) {
        return new CloudWhatsAppClientBuilder(storeFactory);
    }

    /**
     * Sends an interactive message that renders a single catalog product to a consumer.
     *
     * <p>Delivers the product carried by {@code product} as a rich card the consumer can tap to view
     * details and add to a cart. The returned {@link MessageKey} identifies the sent message for later
     * status correlation.
     *
     * @apiNote Drives the "Send product" affordance in the WhatsApp Business catalog: a business shares
     *          one item from its catalog directly inside the conversation.
     * @param recipient the consumer to message
     * @param product   the single-product message to send
     * @return the key identifying the sent message
     * @throws NullPointerException if {@code recipient} or {@code product} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #sendProductList(JidProvider, CloudProductListMessage)
     */
    MessageKey sendProduct(JidProvider recipient, CloudProductMessage product);

    /**
     * Sends an interactive message that renders a grouped list of catalog products to a consumer.
     *
     * <p>Delivers the sections and items carried by {@code productList} as a single browsable list the
     * consumer can scroll through and add to a cart. The returned {@link MessageKey} identifies the sent
     * message for later status correlation.
     *
     * @apiNote Drives the "Send products" affordance in the WhatsApp Business catalog: a business shares
     *          a curated selection of catalog items grouped into sections.
     * @param recipient   the consumer to message
     * @param productList the multi-product message to send
     * @return the key identifying the sent message
     * @throws NullPointerException if {@code recipient} or {@code productList} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #sendProduct(JidProvider, CloudProductMessage)
     */
    MessageKey sendProductList(JidProvider recipient, CloudProductListMessage productList);

    /**
     * Sends an interactive message offering a button that opens the business's full catalog.
     *
     * <p>Delivers a card carrying the body text of {@code catalog} alongside a button that opens the
     * connected catalog in the consumer's app. The returned {@link MessageKey} identifies the sent
     * message for later status correlation.
     *
     * @apiNote Drives the "View catalog" affordance in the WhatsApp Business catalog: a business invites
     *          a consumer to browse its entire catalog from within the chat.
     * @param recipient the consumer to message
     * @param catalog   the catalog message to send
     * @return the key identifying the sent message
     * @throws NullPointerException if {@code recipient} or {@code catalog} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    MessageKey sendCatalog(JidProvider recipient, CloudCatalogMessage catalog);

    /**
     * Sends an interactive order message presenting a consumer with an itemised order to review and pay.
     *
     * <p>Presents the items, totals, and payment options carried by {@code order} together with a
     * review-and-pay action. The order's {@linkplain CloudOrderDetailsMessage#referenceId() reference id}
     * correlates it with the later {@link #sendOrderStatus(JidProvider, CloudOrderStatusMessage)} updates
     * that report payment progress. The returned {@link MessageKey} identifies the sent message for later
     * status correlation.
     *
     * @apiNote Drives the "Send order" affordance in WhatsApp Pay: a business hands a consumer a checkout
     *          they can complete without leaving the chat. The named payment gateway is configured in
     *          WhatsApp Business Manager and only referenced here by name.
     * @param recipient the consumer to message
     * @param order     the order-details message to send
     * @return the key identifying the sent message
     * @throws NullPointerException if {@code recipient} or {@code order} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #sendOrderStatus(JidProvider, CloudOrderStatusMessage)
     */
    MessageKey sendOrderDetails(JidProvider recipient, CloudOrderDetailsMessage order);

    /**
     * Sends an interactive order message updating a consumer on the progress of an existing order.
     *
     * <p>Reports the new state of an order previously sent with
     * {@link #sendOrderDetails(JidProvider, CloudOrderDetailsMessage)}, correlated to it by the shared
     * reference id carried on {@code status}. The returned {@link MessageKey} identifies the sent message
     * for later status correlation.
     *
     * @apiNote Drives the order-status updates in WhatsApp Pay: a business keeps a consumer informed as
     *          their order moves through processing, shipping, and completion.
     * @param recipient the consumer to message
     * @param status    the order-status message to send
     * @return the key identifying the sent message
     * @throws NullPointerException if {@code recipient} or {@code status} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #sendOrderDetails(JidProvider, CloudOrderDetailsMessage)
     */
    MessageKey sendOrderStatus(JidProvider recipient, CloudOrderStatusMessage status);

    /**
     * Uploads media bytes and returns the media id that addresses them when sending.
     *
     * <p>Stores {@code data} against this phone number and returns a media id that may be set as the
     * media reference of an outbound image, video, audio, document, or sticker message. The returned id
     * is reusable until the asset is deleted or expires server-side.
     *
     * @apiNote Backs every "attach media" affordance: a business uploads an asset once and references it
     *          by id across as many sends as it needs.
     * @param data     the raw media bytes
     * @param mimeType the MIME type, for example {@code image/jpeg}
     * @param fileName the file name advertised with the upload
     * @return the media id usable as the media reference of a media message
     * @throws NullPointerException if {@code data}, {@code mimeType}, or {@code fileName} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #uploadMedia(Path, String)
     * @see #downloadMedia(String)
     */
    String uploadMedia(byte[] data, String mimeType, String fileName);

    /**
     * Uploads the bytes of a file and returns the media id that addresses them when sending.
     *
     * <p>Convenience for {@link #uploadMedia(byte[], String, String)} that reads {@code file}'s bytes and
     * advertises its file name, using the supplied {@code mimeType}.
     *
     * @param file     the file whose bytes to upload
     * @param mimeType the MIME type, for example {@code image/jpeg}
     * @return the media id usable as the media reference of a media message
     * @throws NullPointerException if {@code file} or {@code mimeType} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #uploadMedia(byte[], String, String)
     */
    String uploadMedia(Path file, String mimeType);

    /**
     * Uploads the bytes of a file, probing its MIME type, and returns the media id that addresses them.
     *
     * <p>Convenience for {@link #uploadMedia(Path, String)} that probes {@code file}'s MIME type from the
     * file system rather than requiring the caller to supply it.
     *
     * @param file the file whose bytes to upload
     * @return the media id usable as the media reference of a media message
     * @throws NullPointerException     if {@code file} is {@code null}
     * @throws IllegalArgumentException if the MIME type cannot be determined from the file
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #uploadMedia(Path, String)
     */
    String uploadMedia(Path file);

    /**
     * Uploads media bytes and returns a one-shot handle for use as a message-template header asset.
     *
     * <p>Returns a handle that may be set as the asset of a message template's header component, unlike
     * {@link #uploadMedia(byte[], String, String)}, whose reusable media id addresses send-time media.
     * The handle is consumed when the template is created and is not reusable across templates.
     *
     * @apiNote Backs the header-image upload step of the template editor in WhatsApp Manager: a business
     *          attaches a sample header asset while authoring a media-header template.
     * @param data     the raw media bytes
     * @param mimeType the MIME type, for example {@code image/jpeg}
     * @param fileName the file name advertised with the upload, or {@code null} to omit it
     * @return the handle usable as a message-template header asset
     * @throws NullPointerException if {@code data} or {@code mimeType} is {@code null}
     * @throws IllegalStateException if no Meta app id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws CloudUnsupportedVersionException if the configured {@link CloudApiVersion} predates the operation
     * @see #createMessageTemplate(CloudMessageTemplate)
     */
    @WhatsAppCloudMethod(since = WhatsAppCloudApiVersion.V21_0)
    String uploadTemplateHeaderMedia(byte[] data, String mimeType, String fileName);

    /**
     * Opens a resumable upload session for a large media asset and returns its session locator.
     *
     * <p>Reserves a session sized to {@code fileLength} so the asset may be uploaded in resumable chunks
     * and resumed across interruptions. The returned locator is supplied to
     * {@link #uploadToResumableSession(String, long, byte[])} to append bytes and to
     * {@link #queryResumableUploadOffset(String)} to discover how many bytes the server already holds.
     *
     * @apiNote Backs the header-image upload step for large assets: a business resumes an interrupted
     *          upload instead of restarting it from the beginning.
     * @param fileLength the total byte length of the file to upload
     * @param fileType   the MIME type of the file, for example {@code image/jpeg}
     * @param fileName   the advertised file name, or {@code null} to omit it
     * @return the session locator addressing the opened session
     * @throws NullPointerException if {@code fileType} is {@code null}
     * @throws IllegalStateException if no Meta app id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #uploadToResumableSession(String, long, byte[])
     * @see #queryResumableUploadOffset(String)
     */
    String createResumableUploadSession(long fileLength, String fileType, String fileName);

    /**
     * Appends bytes to a resumable upload session and returns the resulting media handle.
     *
     * <p>Uploads {@code data} starting at {@code fileOffset} within the session opened by
     * {@link #createResumableUploadSession(long, String, String)}. Once the final chunk completes the
     * session yields the handle usable as a message-template header asset; before then a partial upload
     * may be resumed from {@link #queryResumableUploadOffset(String)}.
     *
     * @param uploadSessionId the session locator returned by
     *                        {@link #createResumableUploadSession(long, String, String)}
     * @param fileOffset      the byte offset to upload from, {@code 0} for a fresh upload
     * @param data            the raw file bytes to append
     * @return the media handle usable as a message-template header asset
     * @throws NullPointerException if {@code uploadSessionId} or {@code data} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryResumableUploadOffset(String)
     */
    String uploadToResumableSession(String uploadSessionId, long fileOffset, byte[] data);

    /**
     * Reports how many bytes a resumable upload session has already received.
     *
     * <p>Returns the next byte offset to upload, letting a caller resume an interrupted upload from where
     * the server left off rather than restarting it.
     *
     * @param uploadSessionId the session locator
     * @return the byte offset already received, the point from which to resume the upload
     * @throws NullPointerException if {@code uploadSessionId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #uploadToResumableSession(String, long, byte[])
     */
    long queryResumableUploadOffset(String uploadSessionId);

    /**
     * Downloads the binary content of a media asset by its id.
     *
     * <p>The whole asset is buffered into the returned array and held in memory for the duration of the
     * call; a caller expecting a large asset should instead resolve {@link #queryMediaUrl(String)} and
     * stream the returned CDN URL, since this method offers no streaming and no upper bound on the bytes
     * it holds at once.
     *
     * @apiNote Backs the "save media" affordance: a business retrieves an asset a consumer sent, by the
     *          id carried on the inbound media message.
     * @param mediaId the media id, typically taken from an inbound media message
     * @return the raw media bytes, fully buffered in memory
     * @throws NullPointerException if {@code mediaId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryMediaUrl(String)
     */
    byte[] downloadMedia(String mediaId);

    /**
     * Resolves the short-lived download URL of a media asset by its id.
     *
     * <p>Returns a CDN URL a caller can stream from directly, which is the preferred path for a large
     * asset that {@link #downloadMedia(String)} would otherwise buffer wholly in memory. The URL is
     * short-lived and must be fetched promptly.
     *
     * @apiNote Backs streaming retrieval of consumer-sent media: a business streams a large asset from
     *          the CDN rather than buffering it.
     * @param mediaId the media id
     * @return the resolved short-lived CDN download URL
     * @throws NullPointerException if {@code mediaId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #downloadMedia(String)
     */
    URI queryMediaUrl(String mediaId);

    /**
     * Deletes an uploaded media asset by its id.
     *
     * <p>Removes the asset from storage so its id can no longer be used as a send-time media reference.
     *
     * @param mediaId the media id to delete
     * @throws NullPointerException if {@code mediaId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #uploadMedia(byte[], String, String)
     */
    void deleteMedia(String mediaId);

    /**
     * Submits a new message template for review.
     *
     * <p>Registers the name, language, category, and components carried by {@code template} for approval.
     * The returned {@link CloudMessageTemplate} echoes the submission populated with its server-assigned
     * id and initial review status; a template is not sendable until that status reaches approval.
     *
     * @apiNote Drives the "Create template" affordance in the WhatsApp Manager Message Templates surface:
     *          a business authors a reusable template and submits it for Meta review.
     * @param template the template definition (name, language, category, components)
     * @return the submitted template, populated with its server-assigned id and review status
     * @throws NullPointerException if {@code template} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @see #createTemplateFromLibrary(CloudTemplateLibraryAdoption)
     * @see #editMessageTemplate(CloudMessageTemplate)
     */
    CloudMessageTemplate createMessageTemplate(CloudMessageTemplate template);

    /**
     * Submits a new message template adopted from a Template Library entry.
     *
     * <p>References a pre-approved library entry by name rather than authoring components, letting Meta
     * supply its approved content. The caller chooses the new template's name, language, and category;
     * the library entry's buttons are reproduced, and a button carrying a caller-bindable value (a dynamic
     * URL or a call phone number) is supplied through the adoption's button inputs. The returned
     * {@link CloudMessageTemplate} echoes the submission populated with its server-assigned id and review
     * status.
     *
     * @apiNote Drives the "Use a template from the library" affordance in WhatsApp Manager: a business
     *          starts from Meta's curated catalog instead of writing copy from scratch.
     * @param adoption the library-adoption request (name, language, category, library template name, and
     *                 button inputs), built via {@link CloudTemplateLibraryAdoptionBuilder}
     * @return the submitted template, populated with its server-assigned id and review status
     * @throws NullPointerException if {@code adoption} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @see #createMessageTemplate(CloudMessageTemplate)
     */
    CloudMessageTemplate createTemplateFromLibrary(CloudTemplateLibraryAdoption adoption);

    /**
     * Creates or updates an authentication message template across many languages in a single request.
     *
     * <p>Fans the definition carried by {@code template} out across every listed language: an existing
     * template matching a name and language is updated, and a missing one is created. The category is
     * always authentication, and the one-time-password button's text and autofill label are localized by
     * the server per language rather than supplied by the caller. The returned list reports, per language,
     * the template that was created or updated.
     *
     * @apiNote Drives bulk authoring of authentication templates in WhatsApp Manager: a business rolls a
     *          single one-time-password template out to every market it serves at once.
     * @param template the authentication template definition to create or update
     * @return the per-language created or updated templates the server reports
     * @throws NullPointerException  if {@code template} is {@code null}
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    List<CloudUpsertedTemplate> upsertMessageTemplates(CloudAuthenticationTemplateUpsert template);

    /**
     * Lists a single page of the message templates of the WhatsApp Business Account.
     *
     * <p>Returns the first page of templates in the account's default page size. To bound the page
     * explicitly use {@link #queryMessageTemplates(int)}; to retrieve the complete set across all pages
     * use {@link #queryAllMessageTemplates()}.
     *
     * @apiNote Backs the template list in the WhatsApp Manager Message Templates surface.
     * @return the first page of message templates
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @see #queryMessageTemplates(int)
     * @see #queryAllMessageTemplates()
     */
    List<CloudMessageTemplate> queryMessageTemplates();

    /**
     * Lists a single page of the message templates of the WhatsApp Business Account, bounding the page size.
     *
     * <p>Returns at most {@code limit} templates from the first page. The fewer-than-{@code limit} case
     * means the account holds no more templates on that page, not that the set is exhausted; use
     * {@link #queryAllMessageTemplates()} for the complete set.
     *
     * @param limit the maximum number of templates to return
     * @return the message templates, at most {@code limit}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @see #queryMessageTemplates()
     * @see #queryAllMessageTemplates()
     */
    List<CloudMessageTemplate> queryMessageTemplates(int limit);

    /**
     * Lists every message template of the WhatsApp Business Account across all pages.
     *
     * <p>Walks the full result set rather than returning a single page like
     * {@link #queryMessageTemplates()}. Because the underlying cursors are not durable and can expire mid
     * walk, the walk restarts once from the first page instead of failing when that happens; a template
     * list mutated concurrently with the walk may therefore yield slightly stale or duplicated entries
     * rather than an error.
     *
     * @apiNote Backs an exhaustive export of an account's templates; prefer {@link #queryMessageTemplates()}
     *          when a single page suffices.
     * @return every message template of the account
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @see #queryMessageTemplates()
     */
    List<CloudMessageTemplate> queryAllMessageTemplates();

    /**
     * Queries a single message template by name.
     *
     * <p>Resolves the template carrying the given name from the account's template list, returning
     * {@link Optional#empty()} when the account holds no template by that name.
     *
     * @param name the template name
     * @return the matching template, or {@link Optional#empty()} when none carries the name
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @see #queryMessageTemplateById(String)
     */
    Optional<CloudMessageTemplate> queryMessageTemplateByName(String name);

    /**
     * Queries a single message template by its server-assigned id.
     *
     * <p>Resolves the template directly from its stanza rather than from the account's template list, so it
     * is returned regardless of its review status. Returns {@link Optional#empty()} when no template
     * carries the id.
     *
     * @param templateId the server-assigned template id
     * @return the matching template, or {@link Optional#empty()} when no template carries the id
     * @throws NullPointerException if {@code templateId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryMessageTemplateByName(String)
     */
    Optional<CloudMessageTemplate> queryMessageTemplateById(String templateId);

    /**
     * Edits an existing message template, resubmitting it for review.
     *
     * <p>The template is keyed by the server-assigned id carried on {@code template}, as populated by
     * {@link #createMessageTemplate(CloudMessageTemplate)} or the query methods. Editing returns the
     * template to review, so it is not sendable again until approval.
     *
     * @apiNote Drives the "Edit template" affordance in the WhatsApp Manager Message Templates surface.
     * @param template the template to edit, carrying its server-assigned id
     * @throws NullPointerException     if {@code template} is {@code null}
     * @throws IllegalArgumentException if {@code template} carries no id
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #createMessageTemplate(CloudMessageTemplate)
     */
    void editMessageTemplate(CloudMessageTemplate template);

    /**
     * Deletes a message template by name, removing every language version of that name.
     *
     * <p>Removes all language versions sharing the given name. To remove a single language version while
     * leaving the others in place use {@link #deleteMessageTemplateLanguage(String, String)}.
     *
     * @apiNote Drives the "Delete template" affordance in the WhatsApp Manager Message Templates surface.
     * @param name the template name to delete
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #deleteMessageTemplate(CloudMessageTemplate)
     * @see #deleteMessageTemplateLanguage(String, String)
     */
    void deleteMessageTemplate(String name);

    /**
     * Deletes a message template, removing every language version of its name.
     *
     * <p>Convenience for {@link #deleteMessageTemplate(String)} keyed by the name carried on the model.
     *
     * @param template the template to delete
     * @throws NullPointerException if {@code template} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #deleteMessageTemplate(String)
     */
    default void deleteMessageTemplate(CloudMessageTemplate template) {
        deleteMessageTemplate(template.name());
    }

    /**
     * Deletes a single language version of a message template, identified by name and version id.
     *
     * <p>Removes only the one language version addressed by {@code languageTemplateId} and leaves the
     * other versions sharing {@code name} in place, in contrast to {@link #deleteMessageTemplate(String)},
     * which removes every language version of the name.
     *
     * @param name               the template name
     * @param languageTemplateId the id of the language version to delete
     * @throws NullPointerException if {@code name} or {@code languageTemplateId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #deleteMessageTemplate(String)
     */
    void deleteMessageTemplateLanguage(String name, String languageTemplateId);

    /**
     * Deletes several message templates in a single request, keyed by their ids.
     *
     * @param templateIds the ids of the templates to delete
     * @throws NullPointerException     if {@code templateIds} is {@code null}
     * @throws IllegalArgumentException if {@code templateIds} is empty
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws CloudUnsupportedVersionException if the configured {@link CloudApiVersion} predates the operation
     * @see #deleteMessageTemplate(String)
     */
    @WhatsAppCloudMethod(since = WhatsAppCloudApiVersion.V25_0)
    void deleteMessageTemplates(List<String> templateIds);

    /**
     * Copies approved message templates from another WhatsApp Business Account into the configured one.
     *
     * <p>Duplicates from the source account, identified by {@code sourceWabaId}, the high-quality approved
     * templates that the configured account does not already hold. Templates that are low-quality,
     * rejected, pending, or already present are skipped, and the configured account's existing templates
     * are never overwritten. The returned list reports the templates that were copied.
     *
     * @apiNote Drives the template-migration step when a business moves or consolidates accounts.
     * @param sourceWabaId the id of the WhatsApp Business Account to copy templates from
     * @return the templates that were copied into the configured account
     * @throws NullPointerException  if {@code sourceWabaId} is {@code null}
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    List<CloudMessageTemplate> migrateMessageTemplates(String sourceWabaId);

    /**
     * Compares the delivery performance of one base template against one or more peer templates over a window.
     *
     * <p>Returns the comparison flattened into the independent views carried by
     * {@link CloudTemplateComparison}. The base template and its peers must live in the same WhatsApp
     * Business Account and each must have accrued enough sends for Meta to report on; these constraints
     * are enforced by the server, not client-side.
     *
     * @apiNote Powers the template-comparison report in the WhatsApp Manager Message Templates surface:
     *          a business weighs two variants before settling on one.
     * @param request the comparison request (base template id, peer ids, window bounds), built via
     *                {@link CloudMessageTemplateComparisonRequestBuilder}
     * @return the flattened comparison result
     * @throws NullPointerException if {@code request} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    CloudTemplateComparison compareMessageTemplates(CloudMessageTemplateComparisonRequest request);

    /**
     * Queries the calling configuration of the phone number.
     *
     * <p>Returns the current calling configuration, including the server-generated SIP user credentials
     * where a SIP bridge is configured. Pair the result with {@link #updateCallSettings(CloudCallSettings)}
     * to change it.
     *
     * @apiNote Backs the "Calling" settings panel in WhatsApp Manager for a business phone number.
     * @return the calling configuration of the phone number
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #updateCallSettings(CloudCallSettings)
     */
    CloudCallSettings queryCallSettings();

    /**
     * Updates the calling configuration of the phone number.
     *
     * <p>Writes only the populated members of {@code settings}; the SIP bridge is written only when
     * {@link CloudCallSettings#sip()} is present, so members left absent stay unchanged.
     *
     * @apiNote Drives the "Calling" settings panel in WhatsApp Manager for a business phone number.
     * @param settings the calling configuration to write
     * @throws NullPointerException if {@code settings} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryCallSettings()
     */
    void updateCallSettings(CloudCallSettings settings);

    /**
     * Enables inbound and outbound calling on the phone number.
     *
     * @apiNote Drives the calling on/off toggle in the WhatsApp Manager "Calling" settings panel.
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #disableCalling()
     */
    void enableCalling();

    /**
     * Disables calling on the phone number.
     *
     * @apiNote Drives the calling on/off toggle in the WhatsApp Manager "Calling" settings panel.
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #enableCalling()
     */
    void disableCalling();

    /**
     * Sends an interactive message asking a consumer to permit the business to call them.
     *
     * <p>Delivers a permission-request prompt carrying {@code bodyText}. The consumer's accept or reject
     * reply is surfaced to {@link CloudCallPermissionListener} as a {@link CloudCallEvent.PermissionReply}.
     * The returned {@link MessageKey} identifies the sent prompt for later status correlation.
     *
     * @apiNote Drives the call-permission prompt a business shows before placing its first outbound call
     *          to a consumer who has not yet opted in.
     * @param recipient the consumer to ask
     * @param bodyText  the body text of the permission-request prompt
     * @return the key identifying the sent permission-request message
     * @throws NullPointerException if {@code recipient} or {@code bodyText} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryCallPermission(JidProvider)
     */
    MessageKey sendCallPermissionRequest(JidProvider recipient, String bodyText);

    /**
     * Queries the calling permission the business currently holds for a consumer.
     *
     * <p>Returns the consumer's {@link CloudCallPermission}, carrying the permission status, the
     * expiration of a temporary grant, and the actions the business may currently perform together with
     * their rate limits.
     *
     * @apiNote Lets a business check whether it may call a consumer before placing a call.
     * @param user the consumer whose permission to query
     * @return the consumer's calling permission
     * @throws NullPointerException if {@code user} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws CloudUnsupportedVersionException if the configured {@link CloudApiVersion} predates the operation
     * @see #sendCallPermissionRequest(JidProvider, String)
     */
    @WhatsAppCloudMethod(since = WhatsAppCloudApiVersion.V23_0)
    CloudCallPermission queryCallPermission(JidProvider user);

    /**
     * Places an outbound call to a consumer, offering the supplied session description.
     *
     * <p>Offers the WebRTC session description carried by {@code session}, whose
     * {@link CloudCallSession#sdpType() sdpType} must be an offer. The returned id addresses the later
     * {@link #preacceptCall(String, CloudCallSession)}, {@link #acceptCall(String, CloudCallSession)},
     * {@link #rejectCall(String)}, and {@link #terminateCall(String)} actions and correlates inbound call
     * events; it is {@link Optional#empty()} when the server returns none. {@code callbackData} is opaque
     * tracking data echoed back on subsequent call events.
     *
     * @apiNote Drives the "Call" affordance for a business calling a consumer through the Calling API. The
     *          Cloud Calling API uses a different call model from the like-named Linked call methods: they
     *          share the spelling per the naming rule but have different signatures and semantics and no
     *          common supertype.
     * @param recipient    the consumer to call
     * @param session      the session offer to send
     * @param callbackData opaque tracking data echoed back on call events, or {@code null} for none
     * @return the server-assigned call id, or {@link Optional#empty()} when none was returned
     * @throws NullPointerException if {@code recipient} or {@code session} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #startCall(JidProvider, CloudCallSession)
     */
    Optional<String> startCall(JidProvider recipient, CloudCallSession session, String callbackData);

    /**
     * Places an outbound call to a consumer, offering the supplied session description.
     *
     * <p>Convenience for {@link #startCall(JidProvider, CloudCallSession, String)} with no callback data.
     *
     * @apiNote The Cloud Calling API uses a different call model from the like-named Linked call methods:
     *          shared spelling, different signatures and semantics, no common supertype.
     * @param recipient the consumer to call
     * @param session   the session offer to send
     * @return the server-assigned call id, or {@link Optional#empty()} when none was returned
     * @throws NullPointerException if {@code recipient} or {@code session} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #startCall(JidProvider, CloudCallSession, String)
     */
    default Optional<String> startCall(JidProvider recipient, CloudCallSession session) {
        return startCall(recipient, session, null);
    }

    /**
     * Pre-accepts an inbound call, answering early to begin media negotiation before fully accepting.
     *
     * <p>Answers with the session description carried by {@code session}, whose
     * {@link CloudCallSession#sdpType() sdpType} must be an answer, to begin media negotiation early and
     * cut connection latency. A later {@link #acceptCall(String, CloudCallSession)} completes the
     * acceptance.
     *
     * @apiNote Drives the early-answer step of answering an inbound business call. The Cloud Calling API
     *          uses a different call model from the like-named Linked call methods: shared spelling,
     *          different signatures and semantics, no common supertype.
     * @param callId  the server-assigned call id of the inbound call
     * @param session the session answer to send
     * @throws NullPointerException if {@code callId} or {@code session} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #acceptCall(String, CloudCallSession)
     */
    void preacceptCall(String callId, CloudCallSession session);

    /**
     * Accepts an inbound call, answering with the supplied session description.
     *
     * <p>Answers with the session description carried by {@code session}, whose
     * {@link CloudCallSession#sdpType() sdpType} must be an answer. {@code callbackData} is opaque
     * tracking data echoed back on subsequent call events.
     *
     * @apiNote Drives the "Answer" affordance for an inbound business call. The Cloud Calling API uses a
     *          different call model from the like-named Linked call methods: shared spelling, different
     *          signatures and semantics, no common supertype.
     * @param callId       the server-assigned call id of the inbound call
     * @param session      the session answer to send
     * @param callbackData opaque tracking data echoed back on call events, or {@code null} for none
     * @throws NullPointerException if {@code callId} or {@code session} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #acceptCall(String, CloudCallSession)
     * @see #preacceptCall(String, CloudCallSession)
     */
    void acceptCall(String callId, CloudCallSession session, String callbackData);

    /**
     * Accepts an inbound call, answering with the supplied session description.
     *
     * <p>Convenience for {@link #acceptCall(String, CloudCallSession, String)} with no callback data.
     *
     * @apiNote The Cloud Calling API uses a different call model from the like-named Linked call methods:
     *          shared spelling, different signatures and semantics, no common supertype.
     * @param callId  the server-assigned call id of the inbound call
     * @param session the session answer to send
     * @throws NullPointerException if {@code callId} or {@code session} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #acceptCall(String, CloudCallSession, String)
     */
    default void acceptCall(String callId, CloudCallSession session) {
        acceptCall(callId, session, null);
    }

    /**
     * Rejects an inbound call without answering it.
     *
     * <p>Declines the call addressed by {@code callId}; no session description is exchanged.
     *
     * @apiNote Drives the "Decline" affordance for an inbound business call. The Cloud Calling API uses a
     *          different call model from the like-named Linked call methods: shared spelling, different
     *          signatures and semantics, no common supertype.
     * @param callId the server-assigned call id of the inbound call
     * @throws NullPointerException if {@code callId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #acceptCall(String, CloudCallSession)
     */
    void rejectCall(String callId);

    /**
     * Terminates an active or ringing call.
     *
     * <p>Ends the call addressed by {@code callId}; no session description is exchanged. The call's final
     * disposition (status, duration, timestamps) is delivered later as a terminate call event.
     *
     * @apiNote Drives the "Hang up" affordance for a business call. The Cloud Calling API uses a different
     *          call model from the like-named Linked call methods: shared spelling, different signatures
     *          and semantics, no common supertype.
     * @param callId the server-assigned call id
     * @throws NullPointerException if {@code callId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    void terminateCall(String callId);

    /**
     * Requests a verification code be sent to the phone number.
     *
     * <p>Asks the server to deliver a one-time code over the chosen {@code method}, localized to
     * {@code language}; the code is later submitted with {@link #verifyCode(String)}.
     *
     * @apiNote Drives the "Verify your number" step of onboarding a phone number to the Cloud API.
     * @param method   the delivery channel for the code
     * @param language the language the verification message is delivered in
     * @throws NullPointerException if {@code method} or {@code language} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #verifyCode(String)
     */
    void requestVerificationCode(CloudVerificationMethod method, Locale language);

    /**
     * Submits a verification code previously requested for the phone number.
     *
     * <p>Confirms the code the consumer received, completing the request started by
     * {@link #requestVerificationCode(CloudVerificationMethod, Locale)}.
     *
     * @apiNote Drives the code-entry step of onboarding a phone number to the Cloud API.
     * @param code the verification code received over the chosen channel
     * @throws NullPointerException if {@code code} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #requestVerificationCode(CloudVerificationMethod, Locale)
     */
    void verifyCode(String code);

    /**
     * Runs the full phone-number verification ceremony through a handler.
     *
     * <p>Convenience that requests the code over the handler's chosen channel, blocks while the handler
     * supplies the received code, then submits it. It chains
     * {@link #requestVerificationCode(CloudVerificationMethod, Locale)} and {@link #verifyCode(String)}
     * so a caller does not orchestrate the two steps directly.
     *
     * @apiNote Drives the full "Verify your number" flow when a caller wants the request-then-confirm
     *          steps handled in one call.
     * @param handler  the strategy that chooses the delivery channel and supplies the received code
     * @param language the language the verification message is delivered in
     * @throws NullPointerException if {@code handler} or {@code language} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #requestVerificationCode(CloudVerificationMethod, Locale)
     * @see #verifyCode(String)
     */
    default void verifyPhoneNumber(CloudWhatsAppClientVerificationHandler handler, Locale language) {
        requestVerificationCode(handler.method(), language);
        verifyCode(handler.verificationCode());
    }

    /**
     * Registers the phone number for Cloud API use, setting its two-step verification PIN.
     *
     * <p>Completes onboarding and simultaneously enables two-step verification with {@code pin}, which
     * may later be rotated through {@link #editTwoStepPin(String)}. The returned
     * {@link CloudRegistrationResult} reports the outcome.
     *
     * @apiNote Drives the final "Register number" step of onboarding a phone number to the Cloud API.
     * @param pin the six-digit two-step verification PIN to set during registration
     * @return the registration result
     * @throws NullPointerException if {@code pin} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #registerPhoneNumber(String, CloudRegistrationBackup)
     */
    CloudRegistrationResult registerPhoneNumber(String pin);

    /**
     * Registers the phone number for Cloud API use, setting its PIN and restoring prior state from a backup.
     *
     * <p>Behaves like {@link #registerPhoneNumber(String)} and additionally attaches {@code backup} so the
     * server restores the account's prior state during registration. The returned
     * {@link CloudRegistrationResult} reports the outcome.
     *
     * @param pin    the six-digit two-step verification PIN to set during registration
     * @param backup the end-to-end-encrypted backup material to restore
     * @return the registration result
     * @throws NullPointerException if {@code pin} or {@code backup} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #registerPhoneNumber(String)
     */
    CloudRegistrationResult registerPhoneNumber(String pin, CloudRegistrationBackup backup);

    /**
     * Rotates the two-step verification PIN of the phone number.
     *
     * <p>Replaces the PIN first set during {@link #registerPhoneNumber(String)}; the number must already
     * be registered.
     *
     * @apiNote Drives the "Change two-step verification PIN" affordance for a registered number.
     * @param pin the new six-digit PIN
     * @throws NullPointerException if {@code pin} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #registerPhoneNumber(String)
     */
    void editTwoStepPin(String pin);

    /**
     * Deregisters the phone number from Cloud API use.
     *
     * <p>Releases the number so it can be onboarded again or moved elsewhere.
     *
     * @apiNote Drives the "Deregister number" affordance for a registered number.
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    void deregisterPhoneNumber();

    /**
     * Queries the details of the phone number this client operates.
     *
     * <p>Returns the full phone-number view, carrying the display-name review status, messaging limit
     * tier, throughput level, hosting platform type, onboarding certificate, official-business-account
     * flag, and account mode where the server provides them.
     *
     * @apiNote Backs the phone-number overview in WhatsApp Manager.
     * @return the phone-number details (status, quality rating, verified name, and enriched fields)
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryPhoneNumbers()
     */
    CloudPhoneNumber queryPhoneNumber();

    /**
     * Lists a single page of the phone numbers registered under the WhatsApp Business Account.
     *
     * <p>Returns the first page of phone numbers in the account's default page size; bound the page
     * explicitly with {@link #queryPhoneNumbers(int)}.
     *
     * @apiNote Backs the phone-number roster in WhatsApp Manager.
     * @return the first page of phone numbers
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @see #queryPhoneNumbers(int)
     */
    List<CloudPhoneNumber> queryPhoneNumbers();

    /**
     * Lists a single page of the phone numbers registered under the WhatsApp Business Account, bounding the page size.
     *
     * @param limit the maximum number of phone numbers to return
     * @return the phone numbers, at most {@code limit}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @see #queryPhoneNumbers()
     */
    List<CloudPhoneNumber> queryPhoneNumbers(int limit);

    /**
     * Adds a phone number to the WhatsApp Business Account and returns its new phone-number id.
     *
     * <p>Registers the number under the account so it can subsequently be verified and registered. The
     * returned id addresses the messaging and profile operations of the new number.
     *
     * @apiNote Drives the "Add phone number" affordance in WhatsApp Manager.
     * @param add the add-phone-number request (country code, phone number, verified name), built via
     *            {@link CloudPhoneNumberAddBuilder}
     * @return the new phone-number id
     * @throws NullPointerException  if {@code add} is {@code null}
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    String addPhoneNumber(CloudPhoneNumberAdd add);

    /**
     * Queries the data-localization (local-storage) configuration of the phone number.
     *
     * <p>Returns where the number's data is stored, with an absent status when none is configured.
     *
     * @apiNote Backs the data-localization panel for businesses subject to in-country storage rules.
     * @return the local-storage configuration, with an absent status when none is set
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws CloudUnsupportedVersionException if the configured {@link CloudApiVersion} predates the operation
     * @see #updateLocalStorageSettings(CloudLocalStorageSettings)
     */
    @WhatsAppCloudMethod(since = WhatsAppCloudApiVersion.V21_0)
    CloudLocalStorageSettings queryLocalStorageSettings();

    /**
     * Updates the data-localization (local-storage) configuration of the phone number.
     *
     * <p>Writes the supplied configuration. Local storage is mutable only while the number is
     * unregistered; the server rejects the update otherwise.
     *
     * @apiNote Drives the data-localization panel for businesses subject to in-country storage rules.
     * @param settings the local-storage configuration to write
     * @throws NullPointerException if {@code settings} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws CloudUnsupportedVersionException if the configured {@link CloudApiVersion} predates the operation
     * @see #queryLocalStorageSettings()
     */
    @WhatsAppCloudMethod(since = WhatsAppCloudApiVersion.V21_0)
    void updateLocalStorageSettings(CloudLocalStorageSettings settings);

    /**
     * Queries the business encryption configuration of the phone number.
     *
     * <p>Returns the RSA public key currently stored for the number together with the signature status
     * Meta computed for it, or {@link Optional#empty()} when no key has been uploaded.
     *
     * @apiNote Backs the business-encryption setup for end-to-end-encrypted Flows and payment responses.
     * @return the business encryption configuration, or {@link Optional#empty()} when no key is set
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #editBusinessEncryption(CloudBusinessEncryption)
     */
    Optional<CloudBusinessEncryption> queryBusinessEncryption();

    /**
     * Uploads the RSA public key used for the phone number's business encryption.
     *
     * <p>Stores the PEM-encoded 2048-bit RSA public key carried on {@code encryption}, replacing any key
     * previously stored.
     *
     * @apiNote Drives the business-encryption setup for end-to-end-encrypted Flows and payment responses.
     * @param encryption the business encryption configuration carrying the PEM-encoded RSA public key
     * @throws NullPointerException if {@code encryption} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryBusinessEncryption()
     */
    void editBusinessEncryption(CloudBusinessEncryption encryption);

    /**
     * Writes the conversational automation configuration of the phone number.
     *
     * <p>Sets the welcome-message flag, the ice-breaker prompts, and the slash commands carried by
     * {@code automation}; an absent welcome flag, an empty prompt list, and an empty command list each
     * leave that part unchanged. Command names are unique per phone number.
     *
     * @apiNote Drives the welcome message, ice-breakers, and commands a business configures to guide the
     *          start of a conversation.
     * @param automation the conversational automation configuration to write
     * @throws NullPointerException if {@code automation} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryConversationalAutomation()
     */
    void editConversationalAutomation(CloudConversationalAutomation automation);

    /**
     * Queries the conversational automation configuration of the phone number.
     *
     * <p>Returns the configured ice-breaker prompts, slash commands, and welcome-message flag, or
     * {@link Optional#empty()} when none is set.
     *
     * @apiNote Backs the welcome message, ice-breakers, and commands a business configures to guide the
     *          start of a conversation.
     * @return the conversational automation configuration, or {@link Optional#empty()} when none is set
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #editConversationalAutomation(CloudConversationalAutomation)
     */
    Optional<CloudConversationalAutomation> queryConversationalAutomation();

    /**
     * Queries the product catalog connected to the WhatsApp Business Account.
     *
     * <p>An account connects at most one catalog, so this returns that single catalog or
     * {@link Optional#empty()} when none is connected.
     *
     * @apiNote Backs the connected-catalog view for a business selling through WhatsApp.
     * @return the connected product catalog, or {@link Optional#empty()} when none is connected
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    Optional<CloudProductCatalog> queryConnectedProductCatalog();

    /**
     * Queries the commerce settings of the phone number.
     *
     * <p>Returns whether the shopping cart is enabled and whether the connected catalog is visible to
     * consumers, with absent toggles when none are set.
     *
     * @apiNote Backs the commerce settings panel in WhatsApp Manager.
     * @return the commerce settings, with absent toggles when none are set
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #editCommerceSettings(CloudCommerceSettings)
     */
    CloudCommerceSettings queryCommerceSettings();

    /**
     * Updates the commerce settings of the phone number.
     *
     * <p>Writes only the populated toggles of {@code settings}, which must carry at least one. A
     * server-assigned id present on the model is ignored on a write.
     *
     * @apiNote Drives the commerce settings panel in WhatsApp Manager.
     * @param settings the commerce settings to write
     * @throws NullPointerException     if {@code settings} is {@code null}
     * @throws IllegalArgumentException if {@code settings} carries no toggle to update
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryCommerceSettings()
     */
    void editCommerceSettings(CloudCommerceSettings settings);

    /**
     * Lists the payment configurations of the WhatsApp Business Account.
     *
     * <p>Returns one {@link CloudPaymentConfiguration} per configured payment gateway, or an empty list
     * when none is configured.
     *
     * @apiNote Backs the payments setup in WhatsApp Manager; payments availability is region-gated.
     * @return the payment configurations, empty when none are configured
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryPaymentConfiguration(String)
     */
    List<CloudPaymentConfiguration> queryPaymentConfigurations();

    /**
     * Queries a single payment configuration of the WhatsApp Business Account by name.
     *
     * <p>Returns the configuration carrying the given name, or {@link Optional#empty()} when none does.
     *
     * @apiNote Backs the payments setup in WhatsApp Manager; payments availability is region-gated.
     * @param name the name of the payment configuration to query
     * @return the payment configuration, or {@link Optional#empty()} when no configuration carries the name
     * @throws NullPointerException  if {@code name} is {@code null}
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryPaymentConfigurations()
     */
    Optional<CloudPaymentConfiguration> queryPaymentConfiguration(String name);

    /**
     * Creates a payment configuration of the WhatsApp Business Account.
     *
     * <p>Registers the gateway carried by {@code configuration}; the configuration name and provider name
     * are required, the provider merchant id is written when present.
     *
     * @apiNote Drives the payments setup in WhatsApp Manager; payments availability is region-gated.
     * @param configuration the payment configuration to create
     * @throws NullPointerException  if {@code configuration} is {@code null}
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #deletePaymentConfiguration(String)
     */
    void createPaymentConfiguration(CloudPaymentConfiguration configuration);

    /**
     * Deletes a payment configuration of the WhatsApp Business Account by name.
     *
     * @apiNote Drives the payments setup in WhatsApp Manager; payments availability is region-gated.
     * @param name the name of the payment configuration to delete
     * @throws NullPointerException  if {@code name} is {@code null}
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #deletePaymentConfiguration(CloudPaymentConfiguration)
     */
    void deletePaymentConfiguration(String name);

    /**
     * Deletes a payment configuration of the WhatsApp Business Account.
     *
     * <p>Convenience for {@link #deletePaymentConfiguration(String)} keyed by the name carried on the
     * model.
     *
     * @param configuration the payment configuration to delete
     * @throws NullPointerException  if {@code configuration} is {@code null}
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #deletePaymentConfiguration(String)
     */
    default void deletePaymentConfiguration(CloudPaymentConfiguration configuration) {
        deletePaymentConfiguration(configuration.configurationName());
    }

    /**
     * Creates a Flow under the WhatsApp Business Account.
     *
     * <p>Registers the Flow defined by {@code flow} in the draft state. The returned {@link CloudFlow}
     * echoes the definition populated with its server-assigned id; the Flow's JSON is uploaded separately
     * with {@link #uploadFlowJson(String, byte[])} and published with {@link #publishFlow(String)}.
     *
     * @apiNote Drives the "Create flow" affordance in the WhatsApp Manager Flows surface, where a business
     *          builds an interactive in-chat form.
     * @param flow the flow definition
     * @return the created flow, populated with its server-assigned id
     * @throws NullPointerException if {@code flow} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @see #uploadFlowJson(String, byte[])
     * @see #publishFlow(String)
     */
    CloudFlow createFlow(CloudFlow flow);

    /**
     * Lists a single page of the Flows of the WhatsApp Business Account.
     *
     * <p>Returns the first page of Flows in the account's default page size; bound the page explicitly
     * with {@link #queryFlows(int)} or retrieve the complete set with {@link #queryAllFlows()}.
     *
     * @apiNote Backs the Flow list in the WhatsApp Manager Flows surface.
     * @return the first page of flows
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @see #queryFlows(int)
     * @see #queryAllFlows()
     */
    List<CloudFlow> queryFlows();

    /**
     * Lists a single page of the Flows of the WhatsApp Business Account, bounding the page size.
     *
     * @param limit the maximum number of flows to return
     * @return the flows, at most {@code limit}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @see #queryFlows()
     * @see #queryAllFlows()
     */
    List<CloudFlow> queryFlows(int limit);

    /**
     * Lists every Flow of the WhatsApp Business Account across all pages.
     *
     * <p>Walks the full result set rather than returning a single page like {@link #queryFlows()}. Because
     * the underlying cursors are not durable and can expire mid walk, the walk restarts once from the
     * first page instead of failing when that happens; a Flow list mutated concurrently with the walk may
     * therefore yield slightly stale or duplicated entries rather than an error.
     *
     * @apiNote Backs an exhaustive export of an account's Flows; prefer {@link #queryFlows()} when a
     *          single page suffices.
     * @return every Flow of the account
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @see #queryFlows()
     */
    List<CloudFlow> queryAllFlows();

    /**
     * Queries the rich management view of a single Flow.
     *
     * <p>Returns the full {@link CloudFlowDetails} projection, carrying the Flow's lifecycle status,
     * declared categories, pending validation errors, JSON and data-API versions, configured endpoint,
     * web preview link, linked application, and endpoint health status.
     *
     * @apiNote Backs the Flow detail panel in the WhatsApp Manager Flows surface.
     * @param flowId the server-assigned flow id
     * @return the flow detail view
     * @throws NullPointerException if {@code flowId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryFlow(CloudFlow)
     */
    CloudFlowDetails queryFlow(String flowId);

    /**
     * Queries the rich management view of a single Flow.
     *
     * <p>Convenience for {@link #queryFlow(String)} keyed by the id carried on the model.
     *
     * @param flow the flow to read, carrying its server-assigned id
     * @return the flow detail view
     * @throws NullPointerException     if {@code flow} is {@code null}
     * @throws IllegalArgumentException if {@code flow} carries no id
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryFlow(String)
     */
    default CloudFlowDetails queryFlow(CloudFlow flow) {
        return queryFlow(flow.id().orElseThrow(
                () -> new IllegalArgumentException("flow must carry an id")));
    }

    /**
     * Updates the metadata of an existing Flow.
     *
     * <p>Writes only the fields {@code edit} populates; an absent field, and an empty categories list,
     * leave the corresponding metadata unchanged.
     *
     * @apiNote Drives editing a Flow's name, categories, and endpoint in the WhatsApp Manager Flows
     *          surface.
     * @param edit the Flow-metadata edit (flow id and the fields to change), built via
     *             {@link CloudFlowMetadataEditBuilder}
     * @throws NullPointerException if {@code edit} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    void editFlowMetadata(CloudFlowMetadataEdit edit);

    /**
     * Publishes a Flow, making it available to send.
     *
     * <p>Promotes the Flow out of the draft state so it can be attached to outbound messages. Once
     * published a Flow can no longer be deleted, only {@linkplain #deprecateFlow(String) deprecated}.
     *
     * @apiNote Drives the "Publish flow" affordance in the WhatsApp Manager Flows surface.
     * @param flowId the server-assigned flow id
     * @throws NullPointerException if {@code flowId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #publishFlow(CloudFlow)
     * @see #deprecateFlow(String)
     */
    void publishFlow(String flowId);

    /**
     * Publishes a Flow, making it available to send.
     *
     * <p>Convenience for {@link #publishFlow(String)} keyed by the id carried on the model.
     *
     * @param flow the flow to publish, carrying its server-assigned id
     * @throws NullPointerException     if {@code flow} is {@code null}
     * @throws IllegalArgumentException if {@code flow} carries no id
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #publishFlow(String)
     */
    default void publishFlow(CloudFlow flow) {
        publishFlow(flow.id().orElseThrow(
                () -> new IllegalArgumentException("flow must carry an id")));
    }

    /**
     * Deprecates a published Flow, retiring it from new sends.
     *
     * <p>Marks the Flow so it can no longer be attached to new messages while leaving in-flight
     * conversations intact.
     *
     * @apiNote Drives the "Deprecate flow" affordance in the WhatsApp Manager Flows surface.
     * @param flowId the server-assigned flow id
     * @throws NullPointerException if {@code flowId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #deprecateFlow(CloudFlow)
     */
    void deprecateFlow(String flowId);

    /**
     * Deprecates a published Flow, retiring it from new sends.
     *
     * <p>Convenience for {@link #deprecateFlow(String)} keyed by the id carried on the model.
     *
     * @param flow the flow to deprecate, carrying its server-assigned id
     * @throws NullPointerException     if {@code flow} is {@code null}
     * @throws IllegalArgumentException if {@code flow} carries no id
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #deprecateFlow(String)
     */
    default void deprecateFlow(CloudFlow flow) {
        deprecateFlow(flow.id().orElseThrow(
                () -> new IllegalArgumentException("flow must carry an id")));
    }

    /**
     * Deletes a draft Flow.
     *
     * <p>Accepted only while the Flow is still a draft; the server rejects deletion of a published Flow,
     * surfacing the rejection as a {@link WhatsAppCloudException}. No client-side status pre-check is
     * performed, so a published Flow should be {@linkplain #deprecateFlow(String) deprecated} instead.
     *
     * @apiNote Drives the "Delete flow" affordance in the WhatsApp Manager Flows surface.
     * @param flowId the server-assigned flow id
     * @throws NullPointerException if {@code flowId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #deleteFlow(CloudFlow)
     * @see #deprecateFlow(String)
     */
    void deleteFlow(String flowId);

    /**
     * Deletes a draft Flow.
     *
     * <p>Convenience for {@link #deleteFlow(String)} keyed by the id carried on the model.
     *
     * @param flow the flow to delete, carrying its server-assigned id
     * @throws NullPointerException     if {@code flow} is {@code null}
     * @throws IllegalArgumentException if {@code flow} carries no id
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #deleteFlow(String)
     */
    default void deleteFlow(CloudFlow flow) {
        deleteFlow(flow.id().orElseThrow(
                () -> new IllegalArgumentException("flow must carry an id")));
    }

    /**
     * Uploads the JSON document that defines a Flow's screens and logic.
     *
     * <p>Attaches {@code flowJson} as the Flow's JSON asset and returns the upload outcome: whether the
     * upload succeeded and any validation errors the server raised against the document. A successful
     * upload with no validation errors means the document validated cleanly.
     *
     * @apiNote Drives the "Upload JSON" step of the WhatsApp Manager Flow builder.
     * @param flowId   the server-assigned flow id
     * @param flowJson the raw Flow JSON bytes
     * @return the upload outcome carrying the success flag and any validation errors
     * @throws NullPointerException if {@code flowId} or {@code flowJson} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #uploadFlowJson(CloudFlow, byte[])
     */
    CloudFlowJsonUploadResult uploadFlowJson(String flowId, byte[] flowJson);

    /**
     * Uploads the JSON document that defines a Flow's screens and logic.
     *
     * <p>Convenience for {@link #uploadFlowJson(String, byte[])} keyed by the id carried on the model.
     *
     * @param flow     the flow to upload to, carrying its server-assigned id
     * @param flowJson the raw Flow JSON bytes
     * @return the upload outcome carrying the success flag and any validation errors
     * @throws NullPointerException     if {@code flow} or {@code flowJson} is {@code null}
     * @throws IllegalArgumentException if {@code flow} carries no id
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #uploadFlowJson(String, byte[])
     */
    default CloudFlowJsonUploadResult uploadFlowJson(CloudFlow flow, byte[] flowJson) {
        return uploadFlowJson(flow.id().orElseThrow(
                () -> new IllegalArgumentException("flow must carry an id")), flowJson);
    }

    /**
     * Lists the assets attached to a Flow.
     *
     * @apiNote Backs the assets view in the WhatsApp Manager Flow builder.
     * @param flowId the server-assigned flow id
     * @return the flow assets
     * @throws NullPointerException if {@code flowId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryFlowAssets(CloudFlow)
     */
    List<CloudFlowAsset> queryFlowAssets(String flowId);

    /**
     * Lists the assets attached to a Flow.
     *
     * <p>Convenience for {@link #queryFlowAssets(String)} keyed by the id carried on the model.
     *
     * @param flow the flow to read, carrying its server-assigned id
     * @return the flow assets
     * @throws NullPointerException     if {@code flow} is {@code null}
     * @throws IllegalArgumentException if {@code flow} carries no id
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryFlowAssets(String)
     */
    default List<CloudFlowAsset> queryFlowAssets(CloudFlow flow) {
        return queryFlowAssets(flow.id().orElseThrow(
                () -> new IllegalArgumentException("flow must carry an id")));
    }

    /**
     * Queries the web preview link of a Flow.
     *
     * <p>Returns a short-lived URL that renders the Flow in a browser together with its expiry instant.
     *
     * @apiNote Backs the "Preview" affordance in the WhatsApp Manager Flow builder.
     * @param flowId the server-assigned flow id
     * @return the flow preview
     * @throws NullPointerException if {@code flowId} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryFlowPreview(CloudFlow)
     */
    CloudFlowPreview queryFlowPreview(String flowId);

    /**
     * Queries the web preview link of a Flow.
     *
     * <p>Convenience for {@link #queryFlowPreview(String)} keyed by the id carried on the model.
     *
     * @param flow the flow to read, carrying its server-assigned id
     * @return the flow preview
     * @throws NullPointerException     if {@code flow} is {@code null}
     * @throws IllegalArgumentException if {@code flow} carries no id
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryFlowPreview(String)
     */
    default CloudFlowPreview queryFlowPreview(CloudFlow flow) {
        return queryFlowPreview(flow.id().orElseThrow(
                () -> new IllegalArgumentException("flow must carry an id")));
    }

    /**
     * Copies Flows from another WhatsApp Business Account into the configured account.
     *
     * <p>Copies the Flows named by {@code sourceFlowNames}, or every Flow in the source account when that
     * list is empty, from the account identified by {@code sourceWabaId} into the configured account, and
     * reports the per-Flow outcome.
     *
     * @apiNote Drives the Flow-migration step when a business moves or consolidates accounts.
     * @param sourceWabaId    the source WhatsApp Business Account id
     * @param sourceFlowNames the source Flow names to copy, or an empty list to copy all
     * @return the migration result
     * @throws NullPointerException if {@code sourceWabaId} or {@code sourceFlowNames} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    CloudFlowMigrationResult migrateFlows(String sourceWabaId, List<String> sourceFlowNames);

    /**
     * Creates a QR code short-link that opens a chat with this business and prefills the composer.
     *
     * <p>The created code, when scanned or followed, opens a WhatsApp chat addressed to the configured
     * phone number with {@code prefilledMessage} already typed into the composer. The returned
     * {@link CloudQrCode} carries the stable code that addresses it later, the prefilled message, and the
     * resolved deep-link and QR-image URLs.
     *
     * @apiNote Drives the "QR code" affordance in the WhatsApp Business Tools "Short links" surface: a
     *          business hands out a scannable code that drops a customer straight into a chat with a
     *          ready-made first message.
     * @param prefilledMessage the message text prefilled in the opened chat
     * @return the created QR code short-link
     * @throws NullPointerException if {@code prefilledMessage} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryQrCodes()
     * @see #deleteQrCode(String)
     */
    CloudQrCode createQrCode(String prefilledMessage);

    /**
     * Lists the QR code short-links of the phone number.
     *
     * @apiNote Backs the "Short links" list in WhatsApp Business Tools.
     * @return the QR code short-links
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #createQrCode(String)
     */
    List<CloudQrCode> queryQrCodes();

    /**
     * Deletes a QR code short-link by its code.
     *
     * @apiNote Drives the delete action in the WhatsApp Business Tools "Short links" surface.
     * @param code the QR code short-link code
     * @throws NullPointerException if {@code code} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #deleteQrCode(CloudQrCode)
     */
    void deleteQrCode(String code);

    /**
     * Deletes a QR code short-link.
     *
     * <p>Convenience for {@link #deleteQrCode(String)} keyed by the code carried on the model.
     *
     * @param qrCode the QR code short-link to delete
     * @throws NullPointerException if {@code qrCode} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #deleteQrCode(String)
     */
    default void deleteQrCode(CloudQrCode qrCode) {
        deleteQrCode(qrCode.code());
    }

    /**
     * Reports messaging volume of the WhatsApp Business Account over a time window.
     *
     * <p>Returns the count of messages sent and delivered, bucketed at the granularity carried by
     * {@code query}, optionally narrowed by phone number, product type, and country.
     *
     * @apiNote Powers the "Messaging" overview in the WhatsApp Manager Insights surface.
     * @param query the messaging-analytics request (window, granularity, filters), built via
     *              {@link CloudMessagingAnalyticsQueryBuilder}
     * @return the messaging analytics
     * @throws NullPointerException if {@code query} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    CloudMessagingAnalytics queryMessagingAnalytics(CloudMessagingAnalyticsQuery query);

    /**
     * Reports conversation volume and cost of the WhatsApp Business Account over a time window.
     *
     * <p>Returns conversation counts and their cost, bucketed at the granularity carried by {@code query}
     * and broken down along its requested dimensions, optionally narrowed by phone number, country,
     * conversation category, conversation type, and conversation direction.
     *
     * @apiNote Powers the "Conversations" report in the WhatsApp Manager Insights surface.
     * @param query the conversation-analytics request (window, granularity, filters, dimensions), built
     *              via {@link CloudConversationAnalyticsQueryBuilder}
     * @return the conversation analytics
     * @throws NullPointerException if {@code query} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    CloudConversationAnalytics queryConversationAnalytics(CloudConversationAnalyticsQuery query);

    /**
     * Reports billed message volume and cost of the WhatsApp Business Account over a time window.
     *
     * <p>Returns billed message volume and cost, bucketed at the granularity carried by {@code query} and
     * broken down along its requested dimensions, optionally narrowed by phone number, country, pricing
     * type, and pricing category.
     *
     * @apiNote Powers the "Pricing" report in the WhatsApp Manager Insights surface, where a business
     *          reviews what its messaging is costing.
     * @param query the pricing-analytics request (window, granularity, filters, dimensions), built via
     *              {@link CloudPricingAnalyticsQueryBuilder}
     * @return the pricing analytics
     * @throws NullPointerException if {@code query} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     */
    CloudPricingAnalytics queryPricingAnalytics(CloudPricingAnalyticsQuery query);

    /**
     * Reports the delivery performance of message templates over a time window.
     *
     * <p>Returns the sent, delivered, read, and per-button click counts for the templates named in
     * {@code query}, flattened into one list of per-template, per-bucket {@link CloudTemplateAnalytics}
     * data points. When the request names no metrics the default set (sent, delivered, read, clicked) is
     * reported.
     *
     * @apiNote Powers the "Template performance" report in the WhatsApp Manager Message Templates surface:
     *          a business compares how its templates land before adjusting copy or pacing.
     * @param query the template-analytics request (window, template ids, optional metrics and timezone),
     *              built via {@link CloudTemplateAnalyticsQueryBuilder}
     * @return the flattened per-template, per-bucket data points
     * @throws NullPointerException if {@code query} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryTemplateGroupAnalytics(CloudTemplateGroupAnalyticsQuery)
     */
    List<CloudTemplateAnalytics> queryTemplateAnalytics(CloudTemplateAnalyticsQuery query);

    /**
     * Reports the delivery performance of template groups over a time window.
     *
     * <p>Returns the sent, delivered, read, and per-button click counts for the template groups named in
     * {@code query}, where a group aggregates several templates that share a logical purpose, flattened
     * into one list of per-group, per-bucket {@link CloudTemplateAnalytics} data points. When the request
     * names no metrics the default set (sent, delivered, read, clicked) is reported.
     *
     * @apiNote Powers the grouped "Template performance" report in the WhatsApp Manager Message Templates
     *          surface, where a business compares whole families of related templates.
     * @param query the template-group-analytics request (window, template group ids, optional metrics
     *              and timezone), built via {@link CloudTemplateGroupAnalyticsQueryBuilder}
     * @return the flattened per-group, per-bucket data points
     * @throws NullPointerException if {@code query} is {@code null}
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryTemplateAnalytics(CloudTemplateAnalyticsQuery)
     */
    List<CloudTemplateAnalytics> queryTemplateGroupAnalytics(CloudTemplateGroupAnalyticsQuery query);

    /**
     * Subscribes the configured app to the WhatsApp Business Account's webhooks.
     *
     * <p>Begins delivery of the account's webhook notifications to the configured app's callback URL.
     *
     * @apiNote Drives the webhook-subscription step of wiring an app to a WhatsApp Business Account.
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #unsubscribeApp()
     * @see #querySubscribedApps()
     */
    void subscribeApp();

    /**
     * Lists the apps subscribed to the WhatsApp Business Account's webhooks.
     *
     * @apiNote Backs the webhook-subscription overview of a WhatsApp Business Account.
     * @return the subscribed apps
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #subscribeApp()
     */
    List<CloudSubscribedApp> querySubscribedApps();

    /**
     * Unsubscribes the configured app from the WhatsApp Business Account's webhooks.
     *
     * <p>Stops delivery of the account's webhook notifications to the configured app.
     *
     * @apiNote Drives the webhook-unsubscription step of unwiring an app from a WhatsApp Business Account.
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #subscribeApp()
     */
    void unsubscribeApp();

    /**
     * Queries the management view of the configured WhatsApp Business Account.
     *
     * <p>Returns the account's id, name, currency, timezone, message-template namespace, country,
     * verification and review statuses, status, and ownership type.
     *
     * @apiNote Backs the account overview in WhatsApp Manager.
     * @return the account view
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryOwnedBusinessAccounts()
     */
    CloudWaba queryBusinessAccount();

    /**
     * Lists the WhatsApp Business Accounts owned by the configured business portfolio.
     *
     * @apiNote Backs the partner view of accounts a business portfolio owns directly.
     * @return the first page of owned accounts
     * @throws IllegalStateException if no business portfolio id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryClientBusinessAccounts()
     */
    List<CloudWaba> queryOwnedBusinessAccounts();

    /**
     * Lists the WhatsApp Business Accounts shared with the configured business portfolio by clients.
     *
     * <p>Returns the partner or solution-provider view of the accounts that client businesses have shared
     * with the portfolio, as distinct from the accounts the portfolio owns outright.
     *
     * @apiNote Backs the partner view of client accounts shared with a business portfolio.
     * @return the first page of client accounts
     * @throws IllegalStateException if no business portfolio id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #queryOwnedBusinessAccounts()
     */
    List<CloudWaba> queryClientBusinessAccounts();

    /**
     * Lists the business users assigned to the WhatsApp Business Account and the tasks each holds.
     *
     * @apiNote Backs the user-management roster in WhatsApp Manager.
     * @return the assigned users
     * @throws IllegalStateException if no WhatsApp Business Account id or business portfolio id is
     *                               configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #addBusinessAccountUser(String, Set)
     */
    List<CloudWabaAssignedUser> queryBusinessAccountUsers();

    /**
     * Assigns a business user to the WhatsApp Business Account, granting the supplied tasks.
     *
     * <p>Grants {@code businessUserId} the set of {@link CloudBusinessAccountUserTask tasks} carried by
     * {@code tasks} on the account.
     *
     * @apiNote Drives the "Add people" affordance of the WhatsApp Manager user-management surface.
     * @param businessUserId the business user id to assign
     * @param tasks          the tasks to grant
     * @throws NullPointerException  if {@code businessUserId} or {@code tasks} is {@code null}
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #removeBusinessAccountUser(String)
     */
    void addBusinessAccountUser(String businessUserId, Set<CloudBusinessAccountUserTask> tasks);

    /**
     * Removes a business user's assignment from the WhatsApp Business Account.
     *
     * @apiNote Drives the "Remove person" affordance of the WhatsApp Manager user-management surface.
     * @param businessUserId the business user id to remove
     * @throws NullPointerException  if {@code businessUserId} is {@code null}
     * @throws IllegalStateException if no WhatsApp Business Account id is configured
     * @throws CloudAuthException if the access token is missing or rejected
     * @throws CloudApiException if the Graph request fails
     * @see #addBusinessAccountUser(String, Set)
     */
    void removeBusinessAccountUser(String businessUserId);

    /**
     * Exchanges an Embedded Signup authorization code for a business-scoped access token.
     *
     * <p>Completes the server-side leg of Embedded Signup: the authorization code returned by the
     * front-end JavaScript SDK is exchanged for a business-scoped token. This call authenticates with the
     * Meta app credentials carried on {@code exchange}, not with the per-number bearer token, and the
     * returned {@link CloudOAuthToken} is the token a freshly onboarded business is operated with.
     *
     * @apiNote Drives the final server-side step of the Embedded Signup onboarding flow a business
     *          partner embeds in its own product.
     * @param exchange the signup-code exchange (app credentials, redirect URI, code), built via
     *                 {@link CloudSignupCodeExchangeBuilder}
     * @return the exchanged business-scoped token
     * @throws NullPointerException if {@code exchange} is {@code null}
     * @throws CloudApiException if the exchange is rejected, including when the app credentials, redirect
     *                           URI, or code are invalid (carries code/subcode/fbtrace_id)
     */
    CloudOAuthToken exchangeSignupCode(CloudSignupCodeExchange exchange);

    /**
     * Exchanges a short-lived access token for a long-lived one.
     *
     * <p>Extends a short-lived user token to a long-lived (roughly 60-day) token. This call authenticates
     * with the Meta app {@code credentials}, not with the per-number bearer token.
     *
     * @apiNote Lets a business partner promote the short-lived token Embedded Signup yields into a token
     *          durable enough to operate the account with.
     * @param credentials     the Meta app credentials authenticating the exchange
     * @param shortLivedToken the short-lived token to extend
     * @return the long-lived token
     * @throws NullPointerException if {@code credentials} or {@code shortLivedToken} is {@code null}
     * @throws CloudApiException if the exchange is rejected, including when the app credentials or
     *                           short-lived token are invalid (carries code/subcode/fbtrace_id)
     */
    CloudOAuthToken exchangeLongLivedToken(CloudAppCredentials credentials, String shortLivedToken);

    /**
     * Inspects an access token, reporting its metadata and validity.
     *
     * <p>Returns the inspected token's app, type, validity, issue and expiry instants, granted scopes,
     * and user id, typically used after Embedded Signup to confirm a token carries the required WhatsApp
     * scopes. This call authenticates with the supplied {@code appAccessToken} (an app or developer
     * access token), not with the per-number bearer token; the token under inspection is the payload, not
     * the credential.
     *
     * @apiNote Lets a business partner audit a token before relying on it, for example to verify the
     *          scopes Embedded Signup granted.
     * @param token          the token to inspect
     * @param appAccessToken the app or developer access token authorizing the inspection
     * @return the token inspection result
     * @throws NullPointerException if {@code token} or {@code appAccessToken} is {@code null}
     * @throws CloudApiException if the inspection is rejected, including when {@code appAccessToken} is
     *                           invalid (carries code/subcode/fbtrace_id)
     */
    CloudTokenInspection inspectToken(String token, String appAccessToken);

    /**
     * Shares a partner's line of credit with a WhatsApp Business Account.
     *
     * <p>Attaches the partner's extended credit line to a (typically newly onboarded) client account so
     * the account can be billed against that line. The extended credit id is obtained out of band from
     * the business portfolio's extended-credit listing. This call authenticates with the partner's own
     * credentials carried by the underlying request, not with the per-number bearer token.
     *
     * @apiNote Lets a business partner put a newly onboarded client onto the partner's billing line.
     * @param extendedCreditId  the partner's extended credit line id
     * @param businessAccountId the account id to share the credit line with
     * @param currency          the currency of the account
     * @return the created credit allocation
     * @throws NullPointerException if any argument is {@code null}
     * @throws CloudApiException if the sharing is rejected, including when the partner credentials or the
     *                           credit line are invalid (carries code/subcode/fbtrace_id)
     */
    CloudCreditAllocation shareCreditLine(String extendedCreditId, String businessAccountId, Currency currency);

    /**
     * {@inheritDoc}
     *
     * @apiNote On the Cloud transport an inbound message is delivered when its webhook is received and
     *          decoded; the listener fires once per inbound message change.
     * @param listener {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    CloudWhatsAppClient addNewMessageListener(NewMessageListener<? super CloudWhatsAppClient> listener);

    /**
     * {@inheritDoc}
     *
     * @apiNote On the Cloud transport a status transition is delivered as a webhook status entry; the
     *          listener fires once per such entry.
     * @param listener {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    CloudWhatsAppClient addMessageStatusListener(MessageStatusListener<? super CloudWhatsAppClient> listener);

    /**
     * {@inheritDoc}
     *
     * @apiNote On the Cloud transport a deletion is delivered as a webhook status entry whose status is
     *          deleted rather than as a distinct event, so this listener fires off that transition.
     * @param listener {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    CloudWhatsAppClient addMessageDeletedListener(MessageDeletedListener<? super CloudWhatsAppClient> listener);

    /**
     * {@inheritDoc}
     *
     * @apiNote On the Cloud transport this fires once the access token has been validated and the webhook
     *          receiver has started accepting deliveries.
     * @param listener {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    CloudWhatsAppClient addLoggedInListener(LoggedInListener<? super CloudWhatsAppClient> listener);

    /**
     * {@inheritDoc}
     *
     * @apiNote On the Cloud transport this fires when the hosted webhook receiver stops.
     * @param listener {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    CloudWhatsAppClient addDisconnectedListener(DisconnectedListener<? super CloudWhatsAppClient> listener);

    /**
     * Registers an event listener for this client's webhook events.
     *
     * <p>The argument may be any {@link CloudListener} subtype: a per-event functional interface (for
     * example {@link CloudCallListener} or {@link CloudTemplateStatusListener}) or the aggregator
     * {@link CloudWhatsAppClientListener}. The dispatch layer recovers the concrete event interface
     * through {@code instanceof} pattern matching, so a single-event lambda only ever receives the one
     * event it implements.
     *
     * @apiNote Prefer a typed {@code addXxxListener} convenience when registering a single event; use this
     *          registrar for an aggregator or a listener implementing several event interfaces.
     * @param listener the listener to register
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     * @see #removeListener(WhatsAppListener)
     */
    CloudWhatsAppClient addListener(CloudListener listener);

    /**
     * Registers a listener for echoes of messages the business sent from the WhatsApp app.
     *
     * @apiNote Lets a business observe replies its agents typed in the WhatsApp app, alongside the
     *          messages it sends through the API.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addMessageEchoListener(CloudMessageEchoListener listener);

    /**
     * Registers a listener for inbound call signaling events.
     *
     * @apiNote Surfaces incoming-call offers and terminations so a business can answer or end a call.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addCallListener(CloudCallListener listener);

    /**
     * Registers a listener for the lifecycle transitions of a business-initiated call.
     *
     * @apiNote Surfaces the ringing, accepted, and rejected transitions of an outbound business call.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addCallStatusListener(CloudCallStatusListener listener);

    /**
     * Registers a listener for consumer replies to call-permission requests.
     *
     * @apiNote Surfaces a consumer's accept or reject reply to a
     *          {@linkplain #sendCallPermissionRequest(JidProvider, String) call-permission request}.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addCallPermissionListener(CloudCallPermissionListener listener);

    /**
     * Registers a listener for message-template review-status updates.
     *
     * @apiNote Surfaces a template moving through Meta's approval pipeline so a business knows when it
     *          becomes sendable.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addTemplateStatusListener(CloudTemplateStatusListener listener);

    /**
     * Registers a listener for message-template quality-rating updates.
     *
     * @apiNote Surfaces the quality rating Meta assigns a template from how consumers receive it.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addTemplateQualityListener(CloudTemplateQualityListener listener);

    /**
     * Registers a listener for message-template category reclassifications.
     *
     * @apiNote Surfaces Meta reclassifying a template's category, which can change how it is priced.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addTemplateCategoryListener(CloudTemplateCategoryListener listener);

    /**
     * Registers a listener for message-template component updates.
     *
     * @apiNote Surfaces changes Meta makes to a template's approved components.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addTemplateComponentsListener(CloudTemplateComponentsListener listener);

    /**
     * Registers a listener for message-template pause and unpause updates.
     *
     * @apiNote Surfaces Meta pausing a template that performed poorly, and its later unpausing.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addTemplatePauseListener(CloudTemplatePauseListener listener);

    /**
     * Registers a listener for phone-number display-name and quality-rating updates.
     *
     * @apiNote Surfaces the review of a number's display name and its messaging quality rating.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addPhoneNumberListener(CloudPhoneNumberListener listener);

    /**
     * Registers a listener for account updates such as restrictions, bans, and verification changes.
     *
     * @apiNote Surfaces enforcement and verification changes a business must react to.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addAccountUpdateListener(CloudAccountUpdateListener listener);

    /**
     * Registers a listener for phone-number account-settings updates, including the calling configuration.
     *
     * @apiNote Surfaces changes to a number's settings, such as its calling configuration.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addAccountSettingsListener(CloudAccountSettingsListener listener);

    /**
     * Registers a listener for business-capability updates such as messaging limits and tier.
     *
     * @apiNote Surfaces changes to how many consumers a business may message and at what tier.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addBusinessCapabilityListener(CloudBusinessCapabilityListener listener);

    /**
     * Registers a listener for consumer marketing-preference updates.
     *
     * @apiNote Surfaces a consumer opting out of marketing messages so a business stops sending them.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addUserPreferenceListener(CloudUserPreferenceListener listener);

    /**
     * Registers a listener for phone-number security events.
     *
     * @apiNote Surfaces security-relevant changes on a number, such as a two-step verification change.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addSecurityListener(CloudSecurityListener listener);

    /**
     * Registers a listener for payment-configuration updates.
     *
     * @apiNote Surfaces changes to a configured payment gateway.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addPaymentConfigurationListener(CloudPaymentConfigurationListener listener);

    /**
     * Registers a listener for per-message billing information.
     *
     * <p>Fires for each outbound status transition that carries pricing detail, in addition to the shared
     * message-status or message-deleted event the same transition produces.
     *
     * @apiNote Lets a business attribute cost to individual messages as their statuses transition.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addMessagePricingListener(CloudMessagePricingListener listener);

    /**
     * Registers a listener for Flow status updates.
     *
     * @apiNote Surfaces a Flow's lifecycle transitions (published, deprecated, throttled).
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addFlowListener(CloudFlowListener listener);

    /**
     * Registers a listener for system updates such as consumer phone-number or account identity changes.
     *
     * @apiNote Surfaces a consumer changing their phone number so a business can re-key its records.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addSystemListener(CloudSystemListener listener);

    /**
     * Registers a listener for history-sync deliveries on newly onboarded numbers.
     *
     * @apiNote Surfaces the conversation history delivered when a number is onboarded with Coexistence.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addHistoryListener(CloudHistoryListener listener);

    /**
     * Registers a listener for Coexistence app-state contact syncs delivered after onboarding.
     *
     * @apiNote Surfaces the contact sync delivered when a number is onboarded with Coexistence.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addAppStateSyncListener(CloudAppStateSyncListener listener);

    /**
     * Registers a listener that receives every raw webhook envelope before it is decoded.
     *
     * @apiNote The catch-all escape hatch: a business observes a webhook change that has no typed
     *          listener, or audits the raw payloads.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addWebhookReceivedListener(CloudWebhookReceivedListener listener);

    /**
     * Registers a listener for decode, signature-verification, and dispatch failures.
     *
     * @apiNote Surfaces a malformed or unverifiable webhook delivery so a business can alert on it.
     * @param listener the listener
     * @return {@code this}, for fluent chaining
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    CloudWhatsAppClient addErrorListener(CloudErrorListener listener);

    /**
     * {@inheritDoc}
     *
     * @param listener {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    CloudWhatsAppClient removeListener(WhatsAppListener listener);
}
