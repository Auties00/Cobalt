package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateButton;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateButtonCopyCodeBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateButtonFlowBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateButtonOtpBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateButtonPhoneNumberBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateButtonQuickReplyBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateButtonUrlBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateComponent;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateComponentBodyBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateComponentFooterBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateComponentHeaderBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudOtpType;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateHeaderFormat;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.commerce.ButtonsMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactsArrayMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateButton;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateMessage;
import com.github.auties00.cobalt.wire.linked.message.list.ListMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.media.AudioMessage;
import com.github.auties00.cobalt.wire.linked.message.media.DocumentMessage;
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessage;
import com.github.auties00.cobalt.wire.linked.message.media.StickerMessage;
import com.github.auties00.cobalt.wire.linked.message.media.VideoMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.message.text.HighlyStructuredMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Translates Cobalt's universal {@link LinkedMessageContainer} model into the Cloud API {@code /messages}
 * request body.
 *
 * <p>The encoder switches over the active message variant returned by {@link LinkedMessageContainer#content()}
 * and produces the matching Cloud JSON shape: text, media (image, video, audio, document, sticker),
 * location, contacts, reaction, interactive (reply buttons, lists, native-flow call-to-action and flow,
 * location and address requests), and template messages. Media is
 * referenced by a hosted link when the message carries an {@code http(s)} URL and by media id
 * otherwise, so an upload id placed in the message's media-url field is sent as {@code {"id": ...}}.
 *
 * <p>A few Cloud-only refinements have no representation in the universal model and are silently
 * skipped: named template parameters, copy-code and OTP authentication buttons, and carousel or
 * limited-time-offer templates.
 */
public final class CloudMessageEncoder {
    /**
     * The logger for {@link CloudMessageEncoder}.
     */
    private static final System.Logger LOGGER = Log.get(CloudMessageEncoder.class);

    /**
     * Private constructor; the encoder exposes only static behaviour.
     */
    private CloudMessageEncoder() {

    }

    /**
     * Encodes a message for a recipient into the Cloud {@code /messages} request body.
     *
     * @param recipient the destination user
     * @param container the message content
     * @return the JSON request body ready to post to the messages edge
     * @throws IllegalArgumentException if the message variant has no Cloud API representation
     */
    public static JSONObject encode(JidProvider recipient, LinkedMessageContainer container) {
        var root = new JSONObject();
        root.put("messaging_product", "whatsapp");
        root.put("recipient_type", "individual");
        root.put("to", recipient.toJid().user());
        encodeContent(root, container);
        return root;
    }

    /**
     * Writes the type discriminator and the type-specific stanza for the container's active content.
     *
     * @param root      the request body being assembled
     * @param container the message content
     * @throws IllegalArgumentException if the message variant has no Cloud API representation
     */
    private static void encodeContent(JSONObject root, LinkedMessageContainer container) {
        switch (container.content()) {
            case ExtendedTextMessage text -> {
                root.put("type", "text");
                var node = new JSONObject();
                node.put("body", text.text().orElse(""));
                node.put("preview_url", text.matchedText().isPresent());
                root.put("text", node);
            }
            case ImageMessage image -> media(root, "image", image.mediaUrl().orElse(null), image.caption().orElse(null));
            case VideoMessage video -> media(root, "video", video.mediaUrl().orElse(null), video.caption().orElse(null));
            case AudioMessage audio -> media(root, "audio", audio.mediaUrl().orElse(null), null);
            case DocumentMessage document -> media(root, "document", document.mediaUrl().orElse(null), document.caption().orElse(null));
            case StickerMessage sticker -> media(root, "sticker", sticker.mediaUrl().orElse(null), null);
            case LocationMessage location -> {
                root.put("type", "location");
                var node = new JSONObject();
                node.put("latitude", location.degreesLatitude().orElse(0));
                node.put("longitude", location.degreesLongitude().orElse(0));
                location.name().ifPresent(value -> node.put("name", value));
                location.address().ifPresent(value -> node.put("address", value));
                root.put("location", node);
            }
            case ContactMessage contact -> {
                root.put("type", "contacts");
                var contacts = new JSONArray();
                contacts.add(contactNode(contact));
                root.put("contacts", contacts);
            }
            case ContactsArrayMessage contacts -> {
                root.put("type", "contacts");
                var array = new JSONArray();
                for (var contact : contacts.contacts()) {
                    array.add(contactNode(contact));
                }
                root.put("contacts", array);
            }
            case ReactionMessage reaction -> {
                root.put("type", "reaction");
                var node = new JSONObject();
                reaction.key().flatMap(key -> key.id()).ifPresent(id -> node.put("message_id", id));
                node.put("emoji", reaction.text().orElse(""));
                root.put("reaction", node);
            }
            case TemplateMessage template -> {
                root.put("type", "template");
                root.put("template", templateNode(template));
            }
            case HighlyStructuredMessage hsm -> {
                root.put("type", "template");
                root.put("template", hsmTemplateNode(hsm));
            }
            case ButtonsMessage buttons -> {
                root.put("type", "interactive");
                root.put("interactive", buttonsNode(buttons));
            }
            case ListMessage list -> {
                root.put("type", "interactive");
                root.put("interactive", listNode(list));
            }
            case InteractiveMessage interactive -> {
                root.put("type", "interactive");
                root.put("interactive", interactiveNode(interactive));
            }
            default -> {
                var typeName = container.content().getClass().getSimpleName();
                if (Log.WARNING) LOGGER.log(Level.WARNING, "message type has no cloud api representation: {0}", typeName);
                throw new IllegalArgumentException(
                        "message type has no Cloud API representation: " + typeName);
            }
        }
    }

    /**
     * Writes a media stanza, referencing the asset by hosted link or by media id.
     *
     * @param root    the request body being assembled
     * @param type    the media type discriminator, for example {@code "image"}
     * @param ref     the media reference (an {@code http(s)} link or a media id), or {@code null}
     * @param caption the optional caption, or {@code null} when the type carries none
     */
    private static void media(JSONObject root, String type, String ref, String caption) {
        root.put("type", type);
        var node = new JSONObject();
        putMediaReference(node, ref);
        if (caption != null) {
            node.put("caption", caption);
        }
        root.put(type, node);
    }

    /**
     * Builds a Cloud contact stanza from a vCard-backed contact message.
     *
     * @param contact the contact message
     * @return the Cloud contact stanza
     */
    private static JSONObject contactNode(ContactMessage contact) {
        var node = new JSONObject();
        var name = new JSONObject();
        name.put("formatted_name", contact.displayName().orElse(""));
        node.put("name", name);
        contact.vcard().ifPresent(vcard -> {
            var phones = phonesFromVcard(vcard);
            if (!phones.isEmpty()) {
                node.put("phones", phones);
            }
        });
        return node;
    }

    /**
     * Extracts the {@code TEL} entries of a vCard into a Cloud {@code phones} array.
     *
     * @param vcard the raw vCard text
     * @return a {@code phones} array, empty when the vCard carried no {@code TEL} lines
     */
    private static JSONArray phonesFromVcard(String vcard) {
        var phones = new JSONArray();
        for (var line : vcard.split("\\r?\\n")) {
            var upper = line.toUpperCase();
            if (upper.startsWith("TEL")) {
                var index = line.indexOf(':');
                if (index >= 0) {
                    var phone = new JSONObject();
                    phone.put("phone", line.substring(index + 1).trim());
                    phones.add(phone);
                }
            }
        }
        return phones;
    }

    /**
     * Builds a Cloud template stanza from a {@link TemplateMessage}.
     *
     * <p>The structured-content title becomes the header component, the highly-structured body becomes
     * the body parameters, and the template buttons become button components keyed by their index.
     *
     * @param template the template message
     * @return the Cloud {@code template} stanza
     */
    private static JSONObject templateNode(TemplateMessage template) {
        var node = new JSONObject();
        var components = new JSONArray();
        var format = template.format().orElse(null);
        if (format instanceof TemplateMessage.FourRowTemplate fourRow) {
            var content = fourRow.content().orElse(null);
            if (content != null) {
                applyHsmIdentity(node, content);
                var body = bodyComponent(content);
                if (body != null) {
                    components.add(body);
                }
            }
            fourRow.title().ifPresent(title -> {
                var header = headerComponent(title);
                if (header != null) {
                    components.add(header);
                }
            });
            for (var button : fourRow.buttons()) {
                var component = buttonComponent(button);
                if (component != null) {
                    components.add(component);
                }
            }
        }
        template.templateId().ifPresent(id -> node.putIfAbsent("name", id));
        if (!components.isEmpty()) {
            node.put("components", components);
        }
        return node;
    }

    /**
     * Builds a Cloud template stanza directly from a highly structured message.
     *
     * @param hsm the highly structured message
     * @return the Cloud {@code template} stanza
     */
    private static JSONObject hsmTemplateNode(HighlyStructuredMessage hsm) {
        var node = new JSONObject();
        applyHsmIdentity(node, hsm);
        var body = bodyComponent(hsm);
        if (body != null) {
            var components = new JSONArray();
            components.add(body);
            node.put("components", components);
        }
        return node;
    }

    /**
     * Writes the template name and language from a highly structured message into a template stanza.
     *
     * @param node the template stanza being assembled
     * @param hsm  the highly structured message carrying the identity
     */
    private static void applyHsmIdentity(JSONObject node, HighlyStructuredMessage hsm) {
        hsm.elementName().ifPresent(name -> node.put("name", name));
        var language = new JSONObject();
        var code = hsm.deterministicLg().or(hsm::fallbackLg).orElse("en_US");
        language.put("code", code);
        node.put("language", language);
    }

    /**
     * Builds the body component (positional text and localisable parameters) of a template.
     *
     * @param hsm the highly structured message carrying the body parameters
     * @return the body component, or {@code null} when the message has no parameters
     */
    private static JSONObject bodyComponent(HighlyStructuredMessage hsm) {
        var parameters = new JSONArray();
        for (var param : hsm.params()) {
            var node = new JSONObject();
            node.put("type", "text");
            node.put("text", param);
            parameters.add(node);
        }
        for (var localizable : hsm.localizableParams()) {
            var node = new JSONObject();
            var currency = localizable.paramOneof()
                    .filter(value -> value instanceof HighlyStructuredMessage.HSMLocalizableParameter.HSMCurrency)
                    .map(value -> (HighlyStructuredMessage.HSMLocalizableParameter.HSMCurrency) value);
            if (currency.isPresent()) {
                node.put("type", "currency");
                var inner = new JSONObject();
                currency.get().currencyCode().ifPresent(value -> inner.put("code", value));
                inner.put("amount_1000", currency.get().amount1000().orElse(0));
                inner.put("fallback_value", localizable.defaultValue().orElse(""));
                node.put("currency", inner);
            } else if (localizable.paramOneof().orElse(null) instanceof HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime) {
                node.put("type", "date_time");
                var inner = new JSONObject();
                inner.put("fallback_value", localizable.defaultValue().orElse(""));
                node.put("date_time", inner);
            } else {
                node.put("type", "text");
                node.put("text", localizable.defaultValue().orElse(""));
            }
            parameters.add(node);
        }
        if (parameters.isEmpty()) {
            return null;
        }
        var component = new JSONObject();
        component.put("type", "body");
        component.put("parameters", parameters);
        return component;
    }

    /**
     * Builds a header component from the title slot of a four-row template.
     *
     * @param title the title variant
     * @return the header component, or {@code null} when the title is unsupported
     */
    private static JSONObject headerComponent(TemplateMessage.Title title) {
        var parameter = new JSONObject();
        switch (title) {
            case ImageMessage image -> {
                parameter.put("type", "image");
                parameter.put("image", mediaParam(image.mediaUrl().orElse(null)));
            }
            case VideoMessage video -> {
                parameter.put("type", "video");
                parameter.put("video", mediaParam(video.mediaUrl().orElse(null)));
            }
            case DocumentMessage document -> {
                parameter.put("type", "document");
                parameter.put("document", mediaParam(document.mediaUrl().orElse(null)));
            }
            case HighlyStructuredMessage text -> {
                parameter.put("type", "text");
                parameter.put("text", text.params().isEmpty() ? "" : text.params().getFirst());
            }
            default -> {
                return null;
            }
        }
        var component = new JSONObject();
        component.put("type", "header");
        var parameters = new JSONArray();
        parameters.add(parameter);
        component.put("parameters", parameters);
        return component;
    }

    /**
     * Builds a media parameter stanza referencing an asset by link or id.
     *
     * @param ref the media reference, or {@code null}
     * @return the media parameter stanza
     */
    private static JSONObject mediaParam(String ref) {
        var node = new JSONObject();
        putMediaReference(node, ref);
        return node;
    }

    /**
     * Writes a media reference into a stanza, as a hosted {@code link} when it is an {@code http(s)} URL
     * and as a media {@code id} otherwise.
     *
     * @param node the stanza to write into
     * @param ref  the media reference, or {@code null} to write nothing
     */
    private static void putMediaReference(JSONObject node, String ref) {
        if (ref == null) {
            return;
        }
        if (ref.startsWith("http://") || ref.startsWith("https://")) {
            node.put("link", ref);
        } else {
            node.put("id", ref);
        }
    }

    /**
     * Builds a button component from a template button.
     *
     * @param button the template button
     * @return the button component, or {@code null} when the button kind is unsupported
     */
    private static JSONObject buttonComponent(TemplateButton button) {
        var index = button.index().orElse(0);
        var variant = button.button().orElse(null);
        if (variant instanceof TemplateButton.QuickReplyButton quickReply) {
            var payload = new JSONObject();
            payload.put("type", "payload");
            payload.put("payload", quickReply.id().orElse(""));
            return buttonNode(index, "quick_reply", payload);
        }
        if (variant instanceof TemplateButton.URLButton urlButton) {
            var suffix = urlButton.url().map(HighlyStructuredMessage::params).orElse(List.of());
            if (suffix.isEmpty()) {
                return null;
            }
            var text = new JSONObject();
            text.put("type", "text");
            text.put("text", suffix.getFirst());
            return buttonNode(index, "url", text);
        }
        // Call buttons are static: the phone number is baked into the approved template, so no
        // runtime button parameter is sent.
        return null;
    }

    /**
     * Builds a Cloud button component wrapping a single parameter.
     *
     * @param index     the zero-based button index within the template
     * @param subType   the button sub-type, for example {@code "quick_reply"} or {@code "url"}
     * @param parameter the single button parameter
     * @return the button component
     */
    private static JSONObject buttonNode(int index, String subType, JSONObject parameter) {
        var component = new JSONObject();
        component.put("type", "button");
        component.put("sub_type", subType);
        component.put("index", String.valueOf(index));
        var parameters = new JSONArray();
        parameters.add(parameter);
        component.put("parameters", parameters);
        return component;
    }

    /**
     * Builds a Cloud interactive stanza of type {@code button} from a buttons message.
     *
     * @param buttons the buttons message
     * @return the Cloud {@code interactive} stanza
     */
    private static JSONObject buttonsNode(ButtonsMessage buttons) {
        var node = new JSONObject();
        node.put("type", "button");
        buttons.contentText().ifPresent(text -> node.put("body", textNode(text)));
        buttons.footerText().ifPresent(text -> node.put("footer", textNode(text)));
        var action = new JSONObject();
        var array = new JSONArray();
        for (var variant : buttons.buttons()) {
            variant.buttonId().ifPresent(id -> {
                var reply = new JSONObject();
                var inner = new JSONObject();
                inner.put("id", id);
                inner.put("title", variant.buttonText().flatMap(text -> text.displayText()).orElse(""));
                reply.put("type", "reply");
                reply.put("reply", inner);
                array.add(reply);
            });
        }
        action.put("buttons", array);
        node.put("action", action);
        return node;
    }

    /**
     * Builds a Cloud interactive stanza of type {@code list} from a list message.
     *
     * @param list the list message
     * @return the Cloud {@code interactive} stanza
     */
    private static JSONObject listNode(ListMessage list) {
        var node = new JSONObject();
        node.put("type", "list");
        list.description().ifPresent(text -> node.put("body", textNode(text)));
        list.footerText().ifPresent(text -> node.put("footer", textNode(text)));
        list.title().ifPresent(text -> node.put("header", headerTextNode(text)));
        var action = new JSONObject();
        list.buttonText().ifPresent(text -> action.put("button", text));
        var sections = new JSONArray();
        for (var section : list.sections()) {
            var sectionNode = new JSONObject();
            section.title().ifPresent(title -> sectionNode.put("title", title));
            var rows = new JSONArray();
            for (var row : section.rows()) {
                var rowNode = new JSONObject();
                row.rowId().ifPresent(id -> rowNode.put("id", id));
                row.title().ifPresent(title -> rowNode.put("title", title));
                row.description().ifPresent(description -> rowNode.put("description", description));
                rows.add(rowNode);
            }
            sectionNode.put("rows", rows);
            sections.add(sectionNode);
        }
        action.put("sections", sections);
        node.put("action", action);
        return node;
    }

    /**
     * Builds a Cloud {@code interactive} stanza from an interactive message.
     *
     * <p>The native flow content variant maps onto one of the Cloud native-flow shapes
     * ({@code cta_url}, {@code flow}, {@code location_request_message} or {@code address_message}),
     * selected from the first native-flow button name. Any other content variant has no Cloud
     * representation.
     *
     * @param interactive the interactive message
     * @return the Cloud {@code interactive} stanza
     * @throws IllegalArgumentException if the content variant has no Cloud API representation
     */
    private static JSONObject interactiveNode(InteractiveMessage interactive) {
        var content = interactive.content().orElse(null);
        if (content instanceof InteractiveMessage.NativeFlowMessage flow) {
            return nativeFlowNode(interactive, flow);
        }
        var typeName = content == null ? "empty" : content.getClass().getSimpleName();
        if (Log.WARNING) LOGGER.log(Level.WARNING, "interactive content has no cloud api representation: {0}", typeName);
        throw new IllegalArgumentException(
                "interactive content has no Cloud API representation: " + typeName);
    }

    /**
     * Builds a Cloud {@code interactive} stanza from an interactive native flow message.
     *
     * <p>The single native-flow button name selects the Cloud shape. {@code cta_url} yields a
     * call-to-action URL message; {@code send_location} a location request; {@code address_message} an
     * address request; any other name whose button parameters carry a {@code flow_id}, or one of the
     * known flow-family names, yields a {@code flow} message.
     *
     * @param parent the interactive message carrying the sections and native flow content
     * @param flow   the native flow content variant
     * @return the Cloud {@code interactive} stanza
     * @throws IllegalArgumentException if the button name has no Cloud API representation
     */
    private static JSONObject nativeFlowNode(InteractiveMessage parent, InteractiveMessage.NativeFlowMessage flow) {
        var button = flow.buttons().isEmpty() ? null : flow.buttons().getFirst();
        var name = button == null ? "" : button.name().orElse("");
        var node = new JSONObject();
        switch (name) {
            case "cta_url" -> {
                node.put("type", "cta_url");
                applyInteractiveSections(node, parent);
                node.put("action", ctaUrlAction(button));
            }
            case "send_location" -> {
                node.put("type", "location_request_message");
                parent.body().flatMap(InteractiveMessage.Body::text).ifPresent(text -> node.put("body", textNode(text)));
                var action = new JSONObject();
                action.put("name", "send_location");
                node.put("action", action);
            }
            case "address_message" -> {
                node.put("type", "address_message");
                parent.body().flatMap(InteractiveMessage.Body::text).ifPresent(text -> node.put("body", textNode(text)));
                var action = new JSONObject();
                action.put("name", "address_message");
                button.buttonParamsJson()
                        .map(JSONObject::parseObject)
                        .ifPresent(params -> action.put("parameters", params));
                node.put("action", action);
            }
            case "review_and_pay", "flow", "single_select", "open_webview" -> {
                node.put("type", "flow");
                applyInteractiveSections(node, parent);
                node.put("action", flowAction(button));
            }
            default -> {
                if (!isFlowButton(button)) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "native flow button has no cloud api representation: {0}", name);
                    }
                    throw new IllegalArgumentException(
                            "native flow button has no Cloud API representation: " + name);
                }
                node.put("type", "flow");
                applyInteractiveSections(node, parent);
                node.put("action", flowAction(button));
            }
        }
        return node;
    }

    /**
     * Writes the optional header, body and footer sections of an interactive message.
     *
     * <p>A media header (image, video or document) is preferred over a text header when the header
     * carries a downloadable media reference; otherwise the header title is written as a text header.
     *
     * @param node   the interactive stanza being assembled
     * @param parent the interactive message carrying the sections
     */
    private static void applyInteractiveSections(JSONObject node, InteractiveMessage parent) {
        parent.body().flatMap(InteractiveMessage.Body::text).ifPresent(text -> node.put("body", textNode(text)));
        parent.footer().flatMap(InteractiveMessage.Footer::text).ifPresent(text -> node.put("footer", textNode(text)));
        var header = parent.header().orElse(null);
        if (header == null) {
            return;
        }
        var media = header.media().orElse(null);
        switch (media) {
            case ImageMessage image -> node.put("header", interactiveMediaHeader("image", image.mediaUrl().orElse(null)));
            case VideoMessage video -> node.put("header", interactiveMediaHeader("video", video.mediaUrl().orElse(null)));
            case DocumentMessage document -> node.put("header", interactiveMediaHeader("document", document.mediaUrl().orElse(null)));
            case null, default -> header.title().ifPresent(title -> node.put("header", headerTextNode(title)));
        }
    }

    /**
     * Builds an interactive media header referencing the asset by hosted link or media id.
     *
     * @param type the header media type, for example {@code "image"}
     * @param ref  the media reference (a link or a media id), or {@code null}
     * @return the header stanza
     */
    private static JSONObject interactiveMediaHeader(String type, String ref) {
        var node = new JSONObject();
        node.put("type", type);
        node.put(type, mediaParam(ref));
        return node;
    }

    /**
     * Builds the {@code cta_url} action from a native flow button.
     *
     * @param button the native flow button named {@code cta_url}
     * @return the Cloud {@code action} stanza
     */
    private static JSONObject ctaUrlAction(InteractiveMessage.NativeFlowMessage.NativeFlowButton button) {
        var action = new JSONObject();
        action.put("name", "cta_url");
        var params = button.buttonParamsJson()
                .map(JSONObject::parseObject)
                .orElseGet(JSONObject::new);
        var parameters = new JSONObject();
        var display = params.getString("display_text");
        var url = params.getString("url");
        if (display != null) {
            parameters.put("display_text", display);
        }
        if (url != null) {
            parameters.put("url", url);
        }
        action.put("parameters", parameters);
        return action;
    }

    /**
     * Builds the {@code flow} action from a native flow button.
     *
     * <p>The button parameters already match the Cloud flow {@code action.parameters} object
     * ({@code flow_message_version} defaulting to {@code "3"}, {@code flow_token}, {@code flow_id},
     * {@code flow_cta}, {@code flow_action}, {@code flow_action_payload}, {@code mode}), so they are
     * re-parsed and emitted verbatim, defaulting {@code flow_message_version} to {@code "3"}. An optional
     * {@code flow_metadata} member is uncommon and is passed through unchanged when present.
     *
     * @param button the native flow button carrying the flow parameters
     * @return the Cloud {@code action} stanza
     */
    private static JSONObject flowAction(InteractiveMessage.NativeFlowMessage.NativeFlowButton button) {
        var action = new JSONObject();
        action.put("name", "flow");
        var params = button == null
                ? new JSONObject()
                : button.buttonParamsJson().map(JSONObject::parseObject).orElseGet(JSONObject::new);
        params.putIfAbsent("flow_message_version", "3");
        action.put("parameters", params);
        return action;
    }

    /**
     * Returns whether a native flow button carries a flow id in its parameters.
     *
     * @param button the native flow button, or {@code null}
     * @return {@code true} if the button parameters contain a {@code flow_id}
     */
    private static boolean isFlowButton(InteractiveMessage.NativeFlowMessage.NativeFlowButton button) {
        if (button == null) {
            return false;
        }
        return button.buttonParamsJson()
                .map(JSONObject::parseObject)
                .map(params -> params.containsKey("flow_id"))
                .orElse(false);
    }

    /**
     * Builds an interactive {@code text} body or footer stanza.
     *
     * @param text the text content
     * @return the stanza carrying the text
     */
    private static JSONObject textNode(String text) {
        var node = new JSONObject();
        node.put("text", text);
        return node;
    }

    /**
     * Builds an interactive header stanza of type {@code text}.
     *
     * @param text the header text
     * @return the header stanza
     */
    private static JSONObject headerTextNode(String text) {
        var node = new JSONObject();
        node.put("type", "text");
        node.put("text", text);
        return node;
    }

    /**
     * Encodes a list of typed template components into the Cloud {@code message_templates} request shape.
     *
     * <p>Each {@link CloudTemplateComponent} becomes one object in the returned array, carrying the
     * uppercase {@code type} discriminator the Cloud API expects ({@code HEADER}, {@code BODY},
     * {@code FOOTER}, {@code BUTTONS}, {@code CAROUSEL}) and the variant-specific fields. The array is
     * empty when {@code components} is empty.
     *
     * @param components the typed template components to encode
     * @return the assembled {@code components} array
     */
    public static JSONArray encodeTemplateComponents(List<CloudTemplateComponent> components) {
        var array = new JSONArray();
        for (var component : components) {
            array.add(encodeTemplateComponent(component));
        }
        return array;
    }

    /**
     * Encodes one typed template component into its Cloud {@code components} entry.
     *
     * @param component the component to encode
     * @return the component object
     */
    private static JSONObject encodeTemplateComponent(CloudTemplateComponent component) {
        var node = new JSONObject();
        switch (component) {
            case CloudTemplateComponent.Header header -> {
                node.put("type", "HEADER");
                node.put("format", header.format().name());
                header.text().ifPresent(value -> node.put("text", value));
                header.example().ifPresent(value -> node.put("example", value));
            }
            case CloudTemplateComponent.Body body -> {
                node.put("type", "BODY");
                node.put("text", body.text());
                body.example().ifPresent(value -> node.put("example", value));
            }
            case CloudTemplateComponent.Footer footer -> {
                node.put("type", "FOOTER");
                node.put("text", footer.text());
            }
            case CloudTemplateComponent.Buttons buttons -> {
                node.put("type", "BUTTONS");
                var array = new JSONArray();
                for (var button : buttons.buttons()) {
                    array.add(encodeTemplateButton(button));
                }
                node.put("buttons", array);
            }
            case CloudTemplateComponent.Carousel carousel -> {
                node.put("type", "CAROUSEL");
                var cards = new JSONArray();
                for (var card : carousel.cards()) {
                    var cardNode = new JSONObject();
                    cardNode.put("components", encodeTemplateComponents(card.components()));
                    cards.add(cardNode);
                }
                node.put("cards", cards);
            }
        }
        return node;
    }

    /**
     * Encodes one typed template button into its Cloud {@code buttons} entry.
     *
     * @param button the button to encode
     * @return the button object
     */
    private static JSONObject encodeTemplateButton(CloudTemplateButton button) {
        var node = new JSONObject();
        switch (button) {
            case CloudTemplateButton.QuickReply quickReply -> {
                node.put("type", "QUICK_REPLY");
                node.put("text", quickReply.text());
            }
            case CloudTemplateButton.Url url -> {
                node.put("type", "URL");
                node.put("text", url.text());
                node.put("url", url.url());
            }
            case CloudTemplateButton.PhoneNumber phoneNumber -> {
                node.put("type", "PHONE_NUMBER");
                node.put("text", phoneNumber.text());
                node.put("phone_number", phoneNumber.phoneNumber());
            }
            case CloudTemplateButton.CopyCode copyCode -> {
                node.put("type", "COPY_CODE");
                copyCode.example().ifPresent(value -> node.put("example", value));
            }
            case CloudTemplateButton.Otp otp -> {
                node.put("type", "OTP");
                otp.otpType().ifPresent(value -> node.put("otp_type", value.token()));
                otp.text().ifPresent(value -> node.put("text", value));
            }
            case CloudTemplateButton.Flow flow -> {
                node.put("type", "FLOW");
                node.put("text", flow.text());
                flow.flowId().ifPresent(value -> node.put("flow_id", value));
                flow.flowAction().ifPresent(value -> node.put("flow_action", value));
                flow.navigateScreen().ifPresent(value -> node.put("navigate_screen", value));
            }
        }
        return node;
    }

    /**
     * Decodes a Cloud {@code components} array into a list of typed template components.
     *
     * <p>Each array entry is mapped to the {@link CloudTemplateComponent} variant selected by its
     * {@code type} discriminator; an entry carrying an unrecognised or absent {@code type} is skipped. The
     * returned list is empty when {@code components} is {@code null} or empty.
     *
     * @param components the Cloud {@code components} array, or {@code null}
     * @return the decoded typed components, empty when none were present
     */
    public static List<CloudTemplateComponent> decodeTemplateComponents(JSONArray components) {
        if (components == null) {
            return List.of();
        }
        var result = new ArrayList<CloudTemplateComponent>();
        for (var index = 0; index < components.size(); index++) {
            var component = decodeTemplateComponent(components.getJSONObject(index));
            if (component != null) {
                result.add(component);
            }
        }
        return result;
    }

    /**
     * Decodes one Cloud {@code components} entry into its typed component variant.
     *
     * @param node the component object
     * @return the decoded component, or {@code null} when the {@code type} is unrecognised or absent
     */
    private static CloudTemplateComponent decodeTemplateComponent(JSONObject node) {
        var type = node.getString("type");
        if (type == null) {
            return null;
        }
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "HEADER" -> new CloudTemplateComponentHeaderBuilder()
                    .format(CloudTemplateHeaderFormat.of(node.getString("format")))
                    .text(node.getString("text"))
                    .example(node.getString("example"))
                    .build();
            case "BODY" -> new CloudTemplateComponentBodyBuilder()
                    .text(node.getString("text"))
                    .example(node.getString("example"))
                    .build();
            case "FOOTER" -> new CloudTemplateComponentFooterBuilder()
                    .text(node.getString("text"))
                    .build();
            case "BUTTONS" -> {
                var buttons = new ArrayList<CloudTemplateButton>();
                var array = node.getJSONArray("buttons");
                if (array != null) {
                    for (var index = 0; index < array.size(); index++) {
                        var button = decodeTemplateButton(array.getJSONObject(index));
                        if (button != null) {
                            buttons.add(button);
                        }
                    }
                }
                yield new CloudTemplateComponent.Buttons(buttons);
            }
            case "CAROUSEL" -> {
                var cards = new ArrayList<CloudTemplateComponent.Carousel.Card>();
                var array = node.getJSONArray("cards");
                if (array != null) {
                    for (var index = 0; index < array.size(); index++) {
                        var cardComponents = decodeTemplateComponents(array.getJSONObject(index).getJSONArray("components"));
                        cards.add(new CloudTemplateComponent.Carousel.Card(cardComponents));
                    }
                }
                yield new CloudTemplateComponent.Carousel(cards);
            }
            default -> {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "unrecognized cloud template component type {0}", type);
                yield null;
            }
        };
    }

    /**
     * Decodes one Cloud {@code buttons} entry into its typed button variant.
     *
     * @param node the button object
     * @return the decoded button, or {@code null} when the {@code type} is unrecognised or absent
     */
    private static CloudTemplateButton decodeTemplateButton(JSONObject node) {
        var type = node.getString("type");
        if (type == null) {
            return null;
        }
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "QUICK_REPLY" -> new CloudTemplateButtonQuickReplyBuilder()
                    .text(node.getString("text"))
                    .build();
            case "URL" -> new CloudTemplateButtonUrlBuilder()
                    .text(node.getString("text"))
                    .url(node.getString("url"))
                    .build();
            case "PHONE_NUMBER" -> new CloudTemplateButtonPhoneNumberBuilder()
                    .text(node.getString("text"))
                    .phoneNumber(node.getString("phone_number"))
                    .build();
            case "COPY_CODE" -> new CloudTemplateButtonCopyCodeBuilder()
                    .example(node.getString("example"))
                    .build();
            case "OTP" -> new CloudTemplateButtonOtpBuilder()
                    .otpType(node.getString("otp_type") == null
                            ? null
                            : CloudOtpType.of(node.getString("otp_type")))
                    .text(node.getString("text"))
                    .build();
            case "FLOW" -> new CloudTemplateButtonFlowBuilder()
                    .text(node.getString("text"))
                    .flowId(node.getString("flow_id"))
                    .flowAction(node.getString("flow_action"))
                    .navigateScreen(node.getString("navigate_screen"))
                    .build();
            default -> {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "unrecognized cloud template button type {0}", type);
                yield null;
            }
        };
    }
}
