package com.github.auties00.cobalt.wire.stanza.smax.bugreporting;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the outbound {@code <iq xmlns="fb:thrift_iq" smax_id="105" type="set">} bug-report
 * submission.
 *
 * <p>An instance carries one bug report end to end: a mandatory description and debug-information
 * JSON blob, an optional device-log handle, up to ten {@link SmaxBugReportingReportBugMediaUpload}
 * attachments, and optional title, category, client-server join key, and reproducibility markers.
 * {@link #toStanza()} serialises these into the IQ-set stanza the relay routes to the internal
 * bug-tracking backend; the backend replies with a {@link SmaxBugReportingReportBugResponse}
 * variant carrying either the assigned task id or a rejection code. Every field is intrinsic to a
 * single submission, so an instance is built once per report and not reused.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBugReportingReportBugRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutBugReportingHackBaseIQSetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBugReportingBaseIQSetRequestMixin")
public final class SmaxBugReportingReportBugRequest implements SmaxStanza.Request {
    /**
     * Holds the optional sender JID stamped onto the {@code from} attribute of the IQ envelope, or
     * {@code null} so the relay derives the sender from the authenticated session.
     */
    private final Jid iqFrom;

    /**
     * Holds the free-form description text shown to the user.
     *
     * <p>Rendered as the mandatory {@code <description>} child of the outbound IQ.
     */
    private final String descriptionElementValue;

    /**
     * Holds the debug-information JSON blob attached to the report.
     *
     * <p>Rendered as the mandatory {@code <debug_information_json>} child. WhatsApp Web populates
     * this with a JSON-stringified snapshot of platform, gating, and storage diagnostics.
     */
    private final String debugInformationJsonElementValue;

    /**
     * Holds the optional handle of a server-side device-log artefact previously uploaded by the
     * client, or {@code null} when none was uploaded.
     *
     * <p>Rendered as an optional {@code <device_log_handle>} child so the bug tracker can correlate
     * the report with full device logs.
     */
    private final String deviceLogHandleElementValue;

    /**
     * Holds the media uploads attached to the report, as an immutable list of at most ten entries.
     *
     * <p>Each entry is rendered by {@link SmaxBugReportingReportBugMediaUpload#toNode()} into the
     * repeated {@code media} slot.
     */
    private final List<SmaxBugReportingReportBugMediaUpload> mediaUploads;

    /**
     * Holds the optional subject-line label for the report, or {@code null} when the user left it
     * blank.
     *
     * <p>Rendered as an optional {@code <title>} child.
     */
    private final String titleElementValue;

    /**
     * Holds the optional category label for the report, or {@code null} when none was selected.
     *
     * <p>Rendered as an optional {@code <category>} child.
     */
    private final String categoryElementValue;

    /**
     * Holds the optional cross-correlation token shared with the telemetry record, or {@code null}
     * when the submission is not paired with one.
     *
     * <p>Rendered as an optional {@code <client_server_join_key>} child so the report can be joined
     * back to its telemetry event.
     */
    private final String clientServerJoinKeyElementValue;

    /**
     * Holds the optional reproducibility-bucket marker the user picked, or {@code null} when none
     * was selected.
     *
     * <p>Rendered as an optional {@code <reproducibility>} child.
     */
    private final String reproducibilityElementValue;

    /**
     * Constructs a bug-report request from its mandatory text fields and optional metadata.
     *
     * <p>The required arguments are null-checked eagerly. The {@code mediaUploads} list is bounded
     * at ten entries and copied into an immutable list so later mutation by the caller cannot race
     * with {@link #toStanza()}.
     *
     * @implNote
     * This implementation rejects an oversized {@code mediaUploads} list in the constructor,
     * whereas WhatsApp Web defers the ten-entry bound to its repeated-child renderer at stanza
     * build time; the eager check fails the caller before any partial stanza is assembled.
     *
     * @param iqFrom                           the optional sender JID; may be {@code null}
     * @param descriptionElementValue          the description text; never {@code null}
     * @param debugInformationJsonElementValue the debug JSON; never {@code null}
     * @param deviceLogHandleElementValue      the optional log handle; may be {@code null}
     * @param mediaUploads                     the media uploads; never {@code null}; at most ten
     *                                         entries
     * @param titleElementValue                the optional title; may be {@code null}
     * @param categoryElementValue             the optional category; may be {@code null}
     * @param clientServerJoinKeyElementValue  the optional join key; may be {@code null}
     * @param reproducibilityElementValue      the optional reproducibility marker; may be
     *                                         {@code null}
     * @throws NullPointerException     if {@code descriptionElementValue},
     *                                  {@code debugInformationJsonElementValue}, or
     *                                  {@code mediaUploads} is {@code null}
     * @throws IllegalArgumentException if {@code mediaUploads} carries more than ten entries
     */
    public SmaxBugReportingReportBugRequest(Jid iqFrom,
                   String descriptionElementValue,
                   String debugInformationJsonElementValue,
                   String deviceLogHandleElementValue,
                   List<SmaxBugReportingReportBugMediaUpload> mediaUploads,
                   String titleElementValue,
                   String categoryElementValue,
                   String clientServerJoinKeyElementValue,
                   String reproducibilityElementValue) {
        this.iqFrom = iqFrom;
        this.descriptionElementValue = Objects.requireNonNull(descriptionElementValue,
                "descriptionElementValue cannot be null");
        this.debugInformationJsonElementValue = Objects.requireNonNull(debugInformationJsonElementValue,
                "debugInformationJsonElementValue cannot be null");
        this.deviceLogHandleElementValue = deviceLogHandleElementValue;
        Objects.requireNonNull(mediaUploads, "mediaUploads cannot be null");
        if (mediaUploads.size() > 10) {
            throw new IllegalArgumentException(
                    "mediaUploads must carry at most 10 entries");
        }
        this.mediaUploads = List.copyOf(mediaUploads);
        this.titleElementValue = titleElementValue;
        this.categoryElementValue = categoryElementValue;
        this.clientServerJoinKeyElementValue = clientServerJoinKeyElementValue;
        this.reproducibilityElementValue = reproducibilityElementValue;
    }

    /**
     * Returns the optional sender JID.
     *
     * <p>Empty when the relay should derive the sender from the authenticated session.
     *
     * @return an {@link Optional} carrying the JID
     */
    public Optional<Jid> iqFrom() {
        return Optional.ofNullable(iqFrom);
    }

    /**
     * Returns the description text.
     *
     * @return the description; never {@code null}
     */
    public String descriptionElementValue() {
        return descriptionElementValue;
    }

    /**
     * Returns the debug-information JSON blob.
     *
     * @return the blob; never {@code null}
     */
    public String debugInformationJsonElementValue() {
        return debugInformationJsonElementValue;
    }

    /**
     * Returns the optional device-log handle.
     *
     * <p>Empty when the embedder has not pre-uploaded a device-log artefact for the backend to
     * correlate.
     *
     * @return an {@link Optional} carrying the handle
     */
    public Optional<String> deviceLogHandleElementValue() {
        return Optional.ofNullable(deviceLogHandleElementValue);
    }

    /**
     * Returns the media uploads.
     *
     * <p>The returned list is unmodifiable; callers that want to mutate it must copy it first.
     *
     * @return the uploads; never {@code null}
     */
    public List<SmaxBugReportingReportBugMediaUpload> mediaUploads() {
        return mediaUploads;
    }

    /**
     * Returns the optional title label.
     *
     * <p>Empty when the user left the optional subject-line field blank.
     *
     * @return an {@link Optional} carrying the title
     */
    public Optional<String> titleElementValue() {
        return Optional.ofNullable(titleElementValue);
    }

    /**
     * Returns the optional category label.
     *
     * <p>Empty when no category was selected.
     *
     * @return an {@link Optional} carrying the category
     */
    public Optional<String> categoryElementValue() {
        return Optional.ofNullable(categoryElementValue);
    }

    /**
     * Returns the optional client-server join key.
     *
     * <p>Empty when the embedder did not pair this submission with a telemetry record.
     *
     * @return an {@link Optional} carrying the key
     */
    public Optional<String> clientServerJoinKeyElementValue() {
        return Optional.ofNullable(clientServerJoinKeyElementValue);
    }

    /**
     * Returns the optional reproducibility marker.
     *
     * <p>Empty when no reproducibility bucket was selected.
     *
     * @return an {@link Optional} carrying the marker
     */
    public Optional<String> reproducibilityElementValue() {
        return Optional.ofNullable(reproducibilityElementValue);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Assembles the {@code <iq xmlns="fb:thrift_iq" smax_id="105" type="set">} envelope
     * addressed to {@link Jid#userServer()}, stamping the {@code from} attribute from
     * {@link #iqFrom} when present. The children are appended in the WhatsApp Web declared order:
     * the mandatory {@code <description>} and {@code <debug_information_json>}, then the optional
     * {@code <device_log_handle>}, then each {@link SmaxBugReportingReportBugMediaUpload#toNode()}
     * media child, then the optional {@code <title>}, {@code <category>},
     * {@code <client_server_join_key>}, and {@code <reproducibility>} children. The builder is
     * returned unbuilt so the dispatch path can stamp the IQ {@code id} before flushing.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBugReportingReportBugRequest",
            exports = "makeReportBugRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBugReportingReportBugRequest",
            exports = "makeReportBugRequestDeviceLogHandle", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBugReportingReportBugRequest",
            exports = "makeReportBugRequestTitle", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBugReportingReportBugRequest",
            exports = "makeReportBugRequestCategory", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBugReportingReportBugRequest",
            exports = "makeReportBugRequestClientServerJoinKey", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBugReportingReportBugRequest",
            exports = "makeReportBugRequestReproducibility", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBugReportingHackBaseIQSetRequestMixin",
            exports = "mergeHackBaseIQSetRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBugReportingBaseIQSetRequestMixin",
            exports = "mergeBaseIQSetRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var children = new ArrayList<Stanza>();
        var descriptionNode = new StanzaBuilder()
                .description("description")
                .content(descriptionElementValue)
                .build();
        children.add(descriptionNode);
        var debugNode = new StanzaBuilder()
                .description("debug_information_json")
                .content(debugInformationJsonElementValue)
                .build();
        children.add(debugNode);
        if (deviceLogHandleElementValue != null) {
            var deviceLogNode = new StanzaBuilder()
                    .description("device_log_handle")
                    .content(deviceLogHandleElementValue)
                    .build();
            children.add(deviceLogNode);
        }
        for (var media : mediaUploads) {
            children.add(media.toStanza());
        }
        if (titleElementValue != null) {
            var titleNode = new StanzaBuilder()
                    .description("title")
                    .content(titleElementValue)
                    .build();
            children.add(titleNode);
        }
        if (categoryElementValue != null) {
            var categoryNode = new StanzaBuilder()
                    .description("category")
                    .content(categoryElementValue)
                    .build();
            children.add(categoryNode);
        }
        if (clientServerJoinKeyElementValue != null) {
            var joinKeyNode = new StanzaBuilder()
                    .description("client_server_join_key")
                    .content(clientServerJoinKeyElementValue)
                    .build();
            children.add(joinKeyNode);
        }
        if (reproducibilityElementValue != null) {
            var reproducibilityNode = new StanzaBuilder()
                    .description("reproducibility")
                    .content(reproducibilityElementValue)
                    .build();
            children.add(reproducibilityNode);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "fb:thrift_iq")
                .attribute("smax_id", 105)
                .attribute("from", iqFrom)
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .content(children);
    }

    /**
     * Indicates whether the given object is a bug-report request equal to this one.
     *
     * <p>Two requests are equal when every field, including the {@code mediaUploads} list, is
     * equal.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is an equal request; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxBugReportingReportBugRequest) obj;
        return Objects.equals(this.iqFrom, that.iqFrom)
                && Objects.equals(this.descriptionElementValue, that.descriptionElementValue)
                && Objects.equals(this.debugInformationJsonElementValue, that.debugInformationJsonElementValue)
                && Objects.equals(this.deviceLogHandleElementValue, that.deviceLogHandleElementValue)
                && Objects.equals(this.mediaUploads, that.mediaUploads)
                && Objects.equals(this.titleElementValue, that.titleElementValue)
                && Objects.equals(this.categoryElementValue, that.categoryElementValue)
                && Objects.equals(this.clientServerJoinKeyElementValue, that.clientServerJoinKeyElementValue)
                && Objects.equals(this.reproducibilityElementValue, that.reproducibilityElementValue);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(iqFrom, descriptionElementValue, debugInformationJsonElementValue,
                deviceLogHandleElementValue, mediaUploads, titleElementValue, categoryElementValue,
                clientServerJoinKeyElementValue, reproducibilityElementValue);
    }

    /**
     * Returns a debug representation listing every field.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxBugReportingReportBugRequest[iqFrom=" + iqFrom
                + ", descriptionElementValue=" + descriptionElementValue
                + ", debugInformationJsonElementValue=" + debugInformationJsonElementValue
                + ", deviceLogHandleElementValue=" + deviceLogHandleElementValue
                + ", mediaUploads=" + mediaUploads
                + ", titleElementValue=" + titleElementValue
                + ", categoryElementValue=" + categoryElementValue
                + ", clientServerJoinKeyElementValue=" + clientServerJoinKeyElementValue
                + ", reproducibilityElementValue=" + reproducibilityElementValue + ']';
    }
}
