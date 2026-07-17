package com.github.auties00.cobalt.wire.linked.message.interactive;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.location.Location;
import com.github.auties00.cobalt.wire.linked.location.Point;
import com.github.auties00.cobalt.wire.linked.message.media.EmbeddedContent;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a clickable region overlaid on a media attachment.
 *
 * <p>An interactive annotation paints a polygon on top of an image, video or status update
 * and binds it to a concrete {@link InteractiveAction}. When the recipient taps inside the
 * polygon, the client performs the bound action: opening a location, navigating to a source
 * newsletter, following a URL, or triggering an embedded action.
 *
 * <p>Annotations can also carry extra presentation hints, such as a confirmation-skip flag
 * for frictionless navigation and a {@link StatusLinkType} that controls how a status update
 * renders associated link previews.
 */
@ProtobufMessage(name = "InteractiveAnnotation")
public final class InteractiveAnnotation {
    /**
     * The ordered list of {@link Point} vertices that describe the clickable polygon, in
     * normalized coordinates relative to the underlying media.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<Point> polygonVertices;

    /**
     * Flag that suppresses the tap-confirmation dialog for this annotation.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean shouldSkipConfirmation;

    /**
     * Optional content embedded inside the annotation (for example a previewable URL card).
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    EmbeddedContent embeddedContent;

    /**
     * Rendering style used when this annotation is attached to a status update link.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.ENUM)
    StatusLinkType statusLinkType;

    /**
     * Action variant that opens geographic coordinates.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    Location location;

    /**
     * Action variant that jumps to a source newsletter message.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    ContextInfo.ForwardedNewsletterMessageInfo newsletter;

    /**
     * Raw flag marking the annotation as carrying an embedded action.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    Boolean embeddedAction;

    /**
     * Action variant that opens a labelled URL.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    TapLinkAction tapAction;


    /**
     * Constructs a new interactive annotation with the supplied geometry, presentation hints
     * and action variants.
     *
     * <p>At most one of {@code location}, {@code newsletter}, {@code embeddedAction} and
     * {@code tapAction} should be non-null to keep the oneof semantics well defined.
     *
     * @param polygonVertices        the polygon vertices, possibly {@code null}
     * @param shouldSkipConfirmation the confirmation-skip flag, possibly {@code null}
     * @param embeddedContent        the embedded content, possibly {@code null}
     * @param statusLinkType         the status link rendering style, possibly {@code null}
     * @param location               the location action variant, possibly {@code null}
     * @param newsletter             the newsletter action variant, possibly {@code null}
     * @param embeddedAction         the embedded action flag, possibly {@code null}
     * @param tapAction              the tap link action variant, possibly {@code null}
     */
    InteractiveAnnotation(List<Point> polygonVertices, Boolean shouldSkipConfirmation, EmbeddedContent embeddedContent, StatusLinkType statusLinkType, Location location, ContextInfo.ForwardedNewsletterMessageInfo newsletter, Boolean embeddedAction, TapLinkAction tapAction) {
        this.polygonVertices = polygonVertices;
        this.shouldSkipConfirmation = shouldSkipConfirmation;
        this.embeddedContent = embeddedContent;
        this.statusLinkType = statusLinkType;
        this.location = location;
        this.newsletter = newsletter;
        this.embeddedAction = embeddedAction;
        this.tapAction = tapAction;
    }

    /**
     * Returns an unmodifiable view of the polygon vertices that define the clickable region.
     *
     * @return the polygon vertices, or an empty list if none were set
     */
    public List<Point> polygonVertices() {
        return polygonVertices == null ? List.of() : Collections.unmodifiableList(polygonVertices);
    }

    /**
     * Returns whether tapping this annotation should skip the usual confirmation dialog.
     *
     * <p>A missing value is treated as {@code false}.
     *
     * @return {@code true} if the confirmation dialog is suppressed, {@code false} otherwise
     */
    public boolean shouldSkipConfirmation() {
        return shouldSkipConfirmation != null && shouldSkipConfirmation;
    }

    /**
     * Returns the content embedded inside this annotation, such as a previewable link card.
     *
     * @return an {@code Optional} with the embedded content, or empty if not set
     */
    public Optional<EmbeddedContent> embeddedContent() {
        return Optional.ofNullable(embeddedContent);
    }

    /**
     * Returns the rendering style used when this annotation decorates a status update link.
     *
     * @return an {@code Optional} with the status link style, or empty if not set
     */
    public Optional<StatusLinkType> statusLinkType() {
        return Optional.ofNullable(statusLinkType);
    }

    /**
     * Returns the concrete action bound to this annotation.
     *
     * <p>Variants are checked in a fixed order: location, newsletter, embedded action, tap
     * link action. If multiple variants are present on the same instance, only the first
     * non-null one is returned. The embedded action boolean is wrapped into a dedicated
     * {@link InteractiveAction.EmbeddedAction} instance on the fly.
     *
     * @return an {@code Optional} containing the bound action, or empty if none is set
     */
    public Optional<? extends InteractiveAction> action() {
        if (location != null) return Optional.of(location);
        if (newsletter != null) return Optional.of(newsletter);
        if (embeddedAction != null) return Optional.of(InteractiveAction.EmbeddedAction.of(embeddedAction));
        if (tapAction != null) return Optional.of(tapAction);
        return Optional.empty();
    }

    /**
     * Updates the polygon vertices that define the clickable region.
     *
     * @param polygonVertices the new list of vertices, or {@code null} to clear the field
     */
    public void setPolygonVertices(List<Point> polygonVertices) {
        this.polygonVertices = polygonVertices;
    }

    /**
     * Updates the confirmation-skip flag.
     *
     * @param shouldSkipConfirmation the new flag value, or {@code null} to clear the field
     */
    public void setShouldSkipConfirmation(Boolean shouldSkipConfirmation) {
        this.shouldSkipConfirmation = shouldSkipConfirmation;
    }

    /**
     * Updates the embedded content associated with this annotation.
     *
     * @param embeddedContent the new embedded content, or {@code null} to clear the field
     */
    public void setEmbeddedContent(EmbeddedContent embeddedContent) {
        this.embeddedContent = embeddedContent;
    }

    /**
     * Updates the rendering style used for status link decoration.
     *
     * @param statusLinkType the new style, or {@code null} to clear the field
     */
    public void setStatusLinkType(StatusLinkType statusLinkType) {
        this.statusLinkType = statusLinkType;
    }

    /**
     * Sets the location action variant, clearing the other action fields is the caller's
     * responsibility.
     *
     * @param location the new location action, or {@code null} to clear the field
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Sets the newsletter action variant, clearing the other action fields is the caller's
     * responsibility.
     *
     * @param newsletter the new newsletter action, or {@code null} to clear the field
     */
    public void setNewsletter(ContextInfo.ForwardedNewsletterMessageInfo newsletter) {
        this.newsletter = newsletter;
    }

    /**
     * Sets the embedded action flag, clearing the other action fields is the caller's
     * responsibility.
     *
     * @param embeddedAction the new flag value, or {@code null} to clear the field
     */
    public void setEmbeddedAction(Boolean embeddedAction) {
        this.embeddedAction = embeddedAction;
    }

    /**
     * Sets the tap link action variant, clearing the other action fields is the caller's
     * responsibility.
     *
     * @param tapAction the new tap action, or {@code null} to clear the field
     */
    public void setTapAction(TapLinkAction tapAction) {
        this.tapAction = tapAction;
    }

    /**
     * Enumerates how a status update renders the link associated with an annotation.
     *
     * <p>The chosen style is a hint to the client that decides whether the link is displayed
     * as a rasterized preview card, a truncated link, or the full URL.
     */
    @ProtobufEnum(name = "InteractiveAnnotation.StatusLinkType")
    public static enum StatusLinkType {
        /**
         * Render the link as a rasterized preview card.
         */
        RASTERIZED_LINK_PREVIEW(1),
        /**
         * Render the link as a rasterized but truncated URL.
         */
        RASTERIZED_LINK_TRUNCATED(2),
        /**
         * Render the link as the full, untruncated URL.
         */
        RASTERIZED_LINK_FULL_URL(3);

        /**
         * Constructs a new enum constant with the supplied protobuf index.
         *
         * @param index the numeric wire-format index
         */
        StatusLinkType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The numeric wire-format index of this constant.
         */
        final int index;

        /**
         * Returns the numeric wire-format index of this constant.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return this.index;
        }
    }
}
