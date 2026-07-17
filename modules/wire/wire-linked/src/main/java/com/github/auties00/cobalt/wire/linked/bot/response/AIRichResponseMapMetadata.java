package com.github.auties00.cobalt.wire.linked.bot.response;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.*;

/**
 * Metadata for a map fragment within a WhatsApp AI bot rich response.
 *
 * <p>The map is centred at the specified latitude and longitude, with
 * the visible region determined by the latitude and longitude deltas.
 * One or more {@link AIRichResponseMapAnnotation} pins are placed on
 * the map to highlight points of interest returned by the AI bot.
 *
 * <p>When {@linkplain #showInfoList() showInfoList} is {@code true},
 * the client renders an additional information panel listing the
 * annotation details alongside the map.
 *
 * <p>This type implements {@link AIRichResponseSubMessageContent} and
 * appears as the {@link AIRichResponseSubMessageType#MAP MAP} variant
 * within an {@link AIRichResponseSubMessage}.
 *
 * @see AIRichResponseSubMessage#content()
 */
@ProtobufMessage(name = "AIRichResponseMapMetadata")
public final class AIRichResponseMapMetadata implements AIRichResponseSubMessageContent {
    /**
     * The latitude of the map centre, in degrees.
     *
     * <p>Example: {@code 48.8566} (Paris, France)
     */
    @ProtobufProperty(index = 1, type = ProtobufType.DOUBLE)
    Double centerLatitude;

    /**
     * The longitude of the map centre, in degrees.
     *
     * <p>Example: {@code 2.3522} (Paris, France)
     */
    @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
    Double centerLongitude;

    /**
     * The latitude span of the visible map region, in degrees.
     *
     * <p>A smaller delta results in a more zoomed-in view. For example
     * a value of {@code 0.01} shows roughly a city block, while
     * {@code 1.0} shows a region-level view.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.DOUBLE)
    Double latitudeDelta;

    /**
     * The longitude span of the visible map region, in degrees.
     *
     * <p>A smaller delta results in a more zoomed-in view.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.DOUBLE)
    Double longitudeDelta;

    /**
     * The list of pin annotations placed on the map.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    List<AIRichResponseMapAnnotation> annotations;

    /**
     * Whether the client should render an information panel listing
     * the annotation details alongside the map view.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    Boolean showInfoList;


    /**
     * Constructs a new map metadata instance.
     *
     * @param centerLatitude  the latitude of the map centre, or {@code null}
     * @param centerLongitude the longitude of the map centre, or {@code null}
     * @param latitudeDelta   the latitude span of the visible region, or {@code null}
     * @param longitudeDelta  the longitude span of the visible region, or {@code null}
     * @param annotations     the list of pin annotations, or {@code null}
     * @param showInfoList    whether to display the info panel, or {@code null}
     */
    AIRichResponseMapMetadata(Double centerLatitude, Double centerLongitude, Double latitudeDelta, Double longitudeDelta, List<AIRichResponseMapAnnotation> annotations, Boolean showInfoList) {
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.latitudeDelta = latitudeDelta;
        this.longitudeDelta = longitudeDelta;
        this.annotations = annotations;
        this.showInfoList = showInfoList;
    }

    /**
     * Returns the latitude of the map centre, in degrees.
     *
     * @return an {@link OptionalDouble} containing the latitude, or
     *         empty if not set
     */
    public OptionalDouble centerLatitude() {
        return centerLatitude == null ? OptionalDouble.empty() : OptionalDouble.of(centerLatitude);
    }

    /**
     * Returns the longitude of the map centre, in degrees.
     *
     * @return an {@link OptionalDouble} containing the longitude, or
     *         empty if not set
     */
    public OptionalDouble centerLongitude() {
        return centerLongitude == null ? OptionalDouble.empty() : OptionalDouble.of(centerLongitude);
    }

    /**
     * Returns the latitude span of the visible map region.
     *
     * @return an {@link OptionalDouble} containing the latitude delta,
     *         or empty if not set
     */
    public OptionalDouble latitudeDelta() {
        return latitudeDelta == null ? OptionalDouble.empty() : OptionalDouble.of(latitudeDelta);
    }

    /**
     * Returns the longitude span of the visible map region.
     *
     * @return an {@link OptionalDouble} containing the longitude delta,
     *         or empty if not set
     */
    public OptionalDouble longitudeDelta() {
        return longitudeDelta == null ? OptionalDouble.empty() : OptionalDouble.of(longitudeDelta);
    }

    /**
     * Returns the list of pin annotations placed on the map.
     *
     * @return an unmodifiable list of annotations, never {@code null}
     */
    public List<AIRichResponseMapAnnotation> annotations() {
        return annotations == null ? List.of() : Collections.unmodifiableList(annotations);
    }

    /**
     * Returns whether the client should display an information panel
     * alongside the map.
     *
     * @return {@code true} if the info list should be shown,
     *         {@code false} otherwise
     */
    public boolean showInfoList() {
        return showInfoList != null && showInfoList;
    }

    /**
     * Sets the latitude of the map centre, in degrees.
     *
     * @param centerLatitude the latitude to set
     */
    public void setCenterLatitude(Double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    /**
     * Sets the longitude of the map centre, in degrees.
     *
     * @param centerLongitude the longitude to set
     */
    public void setCenterLongitude(Double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    /**
     * Sets the latitude span of the visible map region.
     *
     * @param latitudeDelta the latitude delta to set
     */
    public void setLatitudeDelta(Double latitudeDelta) {
        this.latitudeDelta = latitudeDelta;
    }

    /**
     * Sets the longitude span of the visible map region.
     *
     * @param longitudeDelta the longitude delta to set
     */
    public void setLongitudeDelta(Double longitudeDelta) {
        this.longitudeDelta = longitudeDelta;
    }

    /**
     * Sets the list of pin annotations placed on the map.
     *
     * @param annotations the annotations to set
     */
    public void setAnnotations(List<AIRichResponseMapAnnotation> annotations) {
        this.annotations = annotations;
    }

    /**
     * Sets whether the client should display an information panel
     * alongside the map.
     *
     * @param showInfoList {@code true} to show the info list
     */
    public void setShowInfoList(Boolean showInfoList) {
        this.showInfoList = showInfoList;
    }

    /**
     * A pin annotation placed on an AI rich response map.
     *
     * <p>Each annotation marks a point of interest at the given
     * latitude and longitude, with a numbered marker, a title, and
     * an optional descriptive body.
     */
    @ProtobufMessage(name = "AIRichResponseMapMetadata.AIRichResponseMapAnnotation")
    public static final class AIRichResponseMapAnnotation {
        /**
         * The ordinal number displayed on the map pin marker.
         *
         * <p>Typically starts at {@code 1} and increments for each
         * annotation.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
        Integer annotationNumber;

        /**
         * The latitude of this annotation, in degrees.
         *
         * <p>Example: {@code 48.8584} (Eiffel Tower)
         */
        @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
        Double latitude;

        /**
         * The longitude of this annotation, in degrees.
         *
         * <p>Example: {@code 2.2945} (Eiffel Tower)
         */
        @ProtobufProperty(index = 3, type = ProtobufType.DOUBLE)
        Double longitude;

        /**
         * The title of this annotation displayed in the pin callout.
         *
         * <p>Example: {@code "Eiffel Tower"}
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String title;

        /**
         * An optional descriptive body displayed beneath the title
         * in the pin callout or information list.
         *
         * <p>Example: {@code "Iconic iron lattice tower on the Champ de Mars"}
         */
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String body;


        /**
         * Constructs a new map annotation.
         *
         * @param annotationNumber the ordinal number for the pin marker, or {@code null}
         * @param latitude         the latitude in degrees, or {@code null}
         * @param longitude        the longitude in degrees, or {@code null}
         * @param title            the annotation title, or {@code null}
         * @param body             the annotation description, or {@code null}
         */
        AIRichResponseMapAnnotation(Integer annotationNumber, Double latitude, Double longitude, String title, String body) {
            this.annotationNumber = annotationNumber;
            this.latitude = latitude;
            this.longitude = longitude;
            this.title = title;
            this.body = body;
        }

        /**
         * Returns the ordinal number displayed on the map pin marker.
         *
         * @return an {@link OptionalInt} containing the annotation
         *         number, or empty if not set
         */
        public OptionalInt annotationNumber() {
            return annotationNumber == null ? OptionalInt.empty() : OptionalInt.of(annotationNumber);
        }

        /**
         * Returns the latitude of this annotation, in degrees.
         *
         * @return an {@link OptionalDouble} containing the latitude,
         *         or empty if not set
         */
        public OptionalDouble latitude() {
            return latitude == null ? OptionalDouble.empty() : OptionalDouble.of(latitude);
        }

        /**
         * Returns the longitude of this annotation, in degrees.
         *
         * @return an {@link OptionalDouble} containing the longitude,
         *         or empty if not set
         */
        public OptionalDouble longitude() {
            return longitude == null ? OptionalDouble.empty() : OptionalDouble.of(longitude);
        }

        /**
         * Returns the title of this annotation.
         *
         * @return an {@link Optional} containing the title, or empty
         *         if not set
         */
        public Optional<String> title() {
            return Optional.ofNullable(title);
        }

        /**
         * Returns the descriptive body of this annotation.
         *
         * @return an {@link Optional} containing the body text, or
         *         empty if not set
         */
        public Optional<String> body() {
            return Optional.ofNullable(body);
        }

        /**
         * Sets the ordinal number displayed on the map pin marker.
         *
         * @param annotationNumber the annotation number to set
         */
        public void setAnnotationNumber(Integer annotationNumber) {
            this.annotationNumber = annotationNumber;
    }

        /**
         * Sets the latitude of this annotation, in degrees.
         *
         * @param latitude the latitude to set
         */
        public void setLatitude(Double latitude) {
            this.latitude = latitude;
    }

        /**
         * Sets the longitude of this annotation, in degrees.
         *
         * @param longitude the longitude to set
         */
        public void setLongitude(Double longitude) {
            this.longitude = longitude;
    }

        /**
         * Sets the title of this annotation.
         *
         * @param title the title to set
         */
        public void setTitle(String title) {
            this.title = title;
    }

        /**
         * Sets the descriptive body of this annotation.
         *
         * @param body the body text to set
         */
        public void setBody(String body) {
            this.body = body;
    }
    }
}
