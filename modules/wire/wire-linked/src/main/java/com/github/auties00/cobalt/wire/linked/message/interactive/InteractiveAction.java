package com.github.auties00.cobalt.wire.linked.message.interactive;

import com.github.auties00.cobalt.wire.linked.location.Location;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

/**
 * Represents the action triggered when a user taps an {@link InteractiveAnnotation} overlaid
 * on a media attachment.
 *
 * <p>An interactive annotation paints a clickable polygon onto a photo, video or document.
 * When the user taps inside that polygon, the client performs the action described by this
 * sealed interface. The supported actions are:
 * <ul>
 *   <li>{@link Location} opens the associated geographic coordinates in a map view</li>
 *   <li>{@link ContextInfo.ForwardedNewsletterMessageInfo} jumps to the source newsletter
 *       message that produced this content</li>
 *   <li>{@link EmbeddedAction} triggers an embedded client-side action tied to the
 *       surrounding media</li>
 *   <li>{@link TapLinkAction} opens a URL, typically rendered as a labelled call-to-action
 *       button</li>
 * </ul>
 */
public sealed interface InteractiveAction permits Location, ContextInfo.ForwardedNewsletterMessageInfo, InteractiveAction.EmbeddedAction, TapLinkAction {

    /**
     * Wraps a boolean flag that marks the annotation as carrying an embedded action.
     *
     * <p>This variant is serialized as a single {@code bool} field by the protobuf layer.
     * The exact semantics of the embedded action are determined by the surrounding media
     * content, the flag only signals that such an action is present.
     */
    final class EmbeddedAction implements InteractiveAction {
        /**
         * The raw boolean flag indicating that an embedded action is associated with the
         * annotation.
         */
        Boolean embeddedAction;

        /**
         * Constructs a new embedded action wrapper around the given flag.
         *
         * @param embeddedAction the raw flag value, possibly {@code null}
         */
        EmbeddedAction(Boolean embeddedAction) {
            this.embeddedAction = embeddedAction;
        }

        /**
         * Returns the raw boolean flag indicating the presence of an embedded action.
         *
         * @return the raw flag value, possibly {@code null}
         */
        @ProtobufSerializer
        public Boolean embeddedAction() {
            return embeddedAction;
        }

        /**
         * Creates a new {@code EmbeddedAction} from the supplied boolean flag.
         *
         * <p>This factory is invoked by the protobuf deserialization layer when materializing
         * an {@link InteractiveAnnotation} that carries an {@code embeddedAction} field.
         *
         * @param embeddedAction the raw flag value, possibly {@code null}
         * @return a new embedded action wrapping the flag
         */
        @ProtobufDeserializer
        public static EmbeddedAction of(Boolean embeddedAction) {
            return new EmbeddedAction(embeddedAction);
        }
    }
}
