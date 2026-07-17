package com.github.auties00.cobalt.wam.threadlogging;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.commerce.OrderMessage;
import com.github.auties00.cobalt.wire.linked.message.commerce.ProductMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.list.ListMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;

import java.util.List;
import java.util.Optional;

/**
 * Static helpers classifying a message for the thread-interaction counters.
 *
 * <p>The sole entry point is {@link #isCommerceMessage(LinkedMessageContainer)}, used by the send and receive
 * producer hooks to decide whether a message bumps the per-thread {@code commerceMsgs} counter on the
 * business thread-interaction event.
 */
@WhatsAppWebModule(moduleName = "WAWebChatThreadLoggingUtils")
public final class ThreadLoggingMessages {
    /**
     * The native-flow button name marking an order-details interactive card.
     */
    private static final String ORDER_DETAILS_FLOW = "order_details";

    /**
     * The native-flow button name marking an order-status interactive card.
     */
    private static final String ORDER_STATUS_FLOW = "order_status";

    /**
     * Prevents instantiation of this static-helper holder.
     */
    private ThreadLoggingMessages() {
        throw new AssertionError("ThreadLoggingMessages cannot be instantiated");
    }

    /**
     * Returns whether a message counts as a commerce message for the thread-interaction counters.
     *
     * <p>A message is a commerce message when it is itself a product, order, or list message; when it
     * quotes a product, order, or list message; when it is an interactive native-flow message whose
     * button is named {@code order_details} or {@code order_status}; or when it is a text message whose
     * detected link is a catalog ({@code wa.me/c/...}) or product ({@code wa.me/p/...}) deep link. This
     * mirrors WhatsApp Web's {@code isCommerceMessage}.
     *
     * @param container the message to classify
     * @return {@code true} if the message is a commerce message
     */
    @WhatsAppWebExport(moduleName = "WAWebChatThreadLoggingUtils", exports = "isCommerceMessage", adaptation = WhatsAppAdaptation.DIRECT)
    public static boolean isCommerceMessage(LinkedMessageContainer container) {
        if (isCommerceContent(container)) {
            return true;
        }
        var quotedIsCommerce = container.contextualContent()
                .flatMap(contextual -> contextual.contextInfo())
                .flatMap(context -> context.quotedMessageContent())
                .map(ThreadLoggingMessages::isCommerceContent)
                .orElse(false);
        if (quotedIsCommerce) {
            return true;
        }
        if (isOrderNativeFlow(container)) {
            return true;
        }
        return container.content() instanceof ExtendedTextMessage text
                && text.matchedText().map(ThreadLoggingMessages::isCatalogOrProductUrl).orElse(false);
    }

    /**
     * Returns whether a message container's content is a product, order, or list message.
     *
     * @param container the container whose content is inspected
     * @return {@code true} if the content is a commerce-typed message
     */
    private static boolean isCommerceContent(LinkedMessageContainer container) {
        var content = container.content();
        return content instanceof ProductMessage
                || content instanceof OrderMessage
                || content instanceof ListMessage;
    }

    /**
     * Returns whether a message is an interactive native-flow order card.
     *
     * <p>WhatsApp Web treats an interactive native-flow message as commerce when its native-flow name
     * is {@code ORDER_DETAILS} or {@code ORDER_STATUS}. Cobalt models the native-flow name as the
     * button name, so this checks the message's native flow for a button named {@link #ORDER_DETAILS_FLOW}
     * or {@link #ORDER_STATUS_FLOW}.
     *
     * @param container the container whose content is inspected
     * @return {@code true} if the message carries an order-details or order-status native flow
     */
    private static boolean isOrderNativeFlow(LinkedMessageContainer container) {
        if (!(container.content() instanceof InteractiveMessage interactive)) {
            return false;
        }
        return interactive.content()
                .filter(InteractiveMessage.NativeFlowMessage.class::isInstance)
                .map(InteractiveMessage.NativeFlowMessage.class::cast)
                .map(InteractiveMessage.NativeFlowMessage::buttons)
                .orElseGet(List::of)
                .stream()
                .map(InteractiveMessage.NativeFlowMessage.NativeFlowButton::name)
                .flatMap(Optional::stream)
                .anyMatch(name -> name.equals(ORDER_DETAILS_FLOW) || name.equals(ORDER_STATUS_FLOW));
    }

    /**
     * Returns whether a detected link is a WhatsApp catalog or product deep link.
     *
     * @param url the detected link text
     * @return {@code true} if the link is a {@code wa.me/c/} catalog or {@code wa.me/p/} product link
     */
    private static boolean isCatalogOrProductUrl(String url) {
        return url.contains("wa.me/c/") || url.contains("wa.me/p/");
    }
}
