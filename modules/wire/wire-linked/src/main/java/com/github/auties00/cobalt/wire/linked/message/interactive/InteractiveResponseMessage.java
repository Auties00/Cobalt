package com.github.auties00.cobalt.wire.linked.message.interactive;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents the reply a recipient sends after interacting with an
 * {@link InteractiveMessage}.
 *
 * <p>When a user completes a native flow embedded inside an interactive message, their
 * client wraps the produced payload in this response message. It contains an optional
 * {@link Body} that summarizes the response in human-readable form, a {@link ContextInfo}
 * pointing back to the originating interactive message, and a concrete
 * {@link InteractiveResponseMessageContent} carrying the structured payload itself.
 */
@ProtobufMessage(name = "Message.InteractiveResponseMessage")
public final class InteractiveResponseMessage implements ContextualMessage {
    /**
     * The optional text body that summarizes the response.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    Body body;

    /**
     * Contextual information linking this reply back to the originating interactive message.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * The native flow response variant, when the reply carries structured flow output.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    NativeFlowResponseMessage nativeFlowResponseMessage;


    /**
     * Constructs a new interactive response with the supplied body, context and content.
     *
     * @param body                      the optional text body, possibly {@code null}
     * @param contextInfo               the context linking this reply to the source, possibly
     *                                  {@code null}
     * @param nativeFlowResponseMessage the native flow response variant, possibly {@code null}
     */
    InteractiveResponseMessage(Body body, ContextInfo contextInfo, NativeFlowResponseMessage nativeFlowResponseMessage) {
        this.body = body;
        this.contextInfo = contextInfo;
        this.nativeFlowResponseMessage = nativeFlowResponseMessage;
    }

    /**
     * Returns the optional text body that summarizes the response.
     *
     * @return an {@code Optional} with the body, or empty if not set
     */
    public Optional<Body> body() {
        return Optional.ofNullable(body);
    }

    /**
     * Returns the context linking this reply back to the originating interactive message.
     *
     * @return an {@code Optional} with the context, or empty if not set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the structured payload of this response.
     *
     * <p>At present only the {@link NativeFlowResponseMessage} variant is defined; additional
     * variants may be added as the protocol evolves.
     *
     * @return an {@code Optional} with the structured content, or empty if none is set
     */
    public Optional<? extends InteractiveResponseMessageContent> content() {
        if (nativeFlowResponseMessage != null) return Optional.of(nativeFlowResponseMessage);
        return Optional.empty();
    }

    /**
     * Updates the optional text body of this response.
     *
     * @param body the new body, or {@code null} to clear the field
     */
    public void setBody(Body body) {
        this.body = body;
    }

    /**
     * Updates the context linking this reply back to the originating interactive message.
     *
     * @param contextInfo the new context, or {@code null} to clear the field
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets the native flow response variant.
     *
     * @param nativeFlowResponseMessage the new variant, or {@code null} to clear the field
     */
    public void setNativeFlowResponseMessage(NativeFlowResponseMessage nativeFlowResponseMessage) {
        this.nativeFlowResponseMessage = nativeFlowResponseMessage;
    }

    /**
     * Represents a human-readable summary attached to an interactive response.
     *
     * <p>The body carries the plain text rendering of the response together with a
     * {@link TemplateFormat} that hints at how the surrounding client should format it.
     */
    @ProtobufMessage(name = "Message.InteractiveResponseMessage.Body")
    public static final class Body {
        /**
         * The literal text rendering of the response.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String text;

        /**
         * The formatting style applied when rendering the text.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
        Body.TemplateFormat format;


        /**
         * Constructs a new body with the supplied text and formatting style.
         *
         * @param text   the literal text rendering, possibly {@code null}
         * @param format the formatting style, possibly {@code null}
         */
        Body(String text, TemplateFormat format) {
            this.text = text;
            this.format = format;
        }

        /**
         * Returns the literal text rendering of the response.
         *
         * @return an {@code Optional} with the text, or empty if not set
         */
        public Optional<String> text() {
            return Optional.ofNullable(text);
        }

        /**
         * Returns the formatting style applied when rendering the text.
         *
         * @return an {@code Optional} with the format, or empty if not set
         */
        public Optional<TemplateFormat> format() {
            return Optional.ofNullable(format);
        }

        /**
         * Updates the literal text rendering of the response.
         *
         * @param text the new text, or {@code null} to clear the field
         */
        public void setText(String text) {
            this.text = text;
    }

        /**
         * Updates the formatting style applied when rendering the text.
         *
         * @param format the new format, or {@code null} to clear the field
         */
        public void setFormat(TemplateFormat format) {
            this.format = format;
    }

        /**
         * Enumerates the formatting styles supported for the body of an interactive
         * response.
         *
         * <p>The default style renders the text as-is, while extension styles let the
         * server opt into richer formatting rules.
         */
        @ProtobufEnum(name = "Message.InteractiveResponseMessage.Body.Format")
        public static enum TemplateFormat {
            /**
             * Default, plain-text rendering.
             */
            DEFAULT(0),
            /**
             * First extended formatting style reserved by the protocol.
             */
            EXTENSIONS_1(1);

            /**
             * Constructs a new enum constant with the supplied protobuf index.
             *
             * @param index the numeric wire-format index
             */
            TemplateFormat(@ProtobufEnumIndex int index) {
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

    /**
     * Represents the structured output produced when the recipient completes a native flow.
     *
     * <p>A native flow response carries the flow {@link #name()}, a JSON-encoded payload of
     * the captured parameters in {@link #paramsJson()}, and an integer schema
     * {@link #version()} that lets the sender handle payload evolution.
     */
    @ProtobufMessage(name = "Message.InteractiveResponseMessage.NativeFlowResponseMessage")
    public static final class NativeFlowResponseMessage implements InteractiveResponseMessageContent {
        /**
         * The name of the native flow that produced this response.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String name;

        /**
         * JSON string containing the parameters captured from the native flow.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String paramsJson;

        /**
         * The schema version of the native flow payload.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        Integer version;


        /**
         * Constructs a new native flow response with the supplied name, parameters and
         * version.
         *
         * @param name       the flow name, possibly {@code null}
         * @param paramsJson the JSON-encoded parameters, possibly {@code null}
         * @param version    the schema version, possibly {@code null}
         */
        NativeFlowResponseMessage(String name, String paramsJson, Integer version) {
            this.name = name;
            this.paramsJson = paramsJson;
            this.version = version;
        }

        /**
         * Returns the name of the native flow that produced this response.
         *
         * @return an {@code Optional} with the flow name, or empty if not set
         */
        public Optional<String> name() {
            return Optional.ofNullable(name);
        }

        /**
         * Returns the JSON-encoded parameters captured by the flow.
         *
         * @return an {@code Optional} with the JSON payload, or empty if not set
         */
        public Optional<String> paramsJson() {
            return Optional.ofNullable(paramsJson);
        }

        /**
         * Returns the schema version of the native flow payload.
         *
         * @return an {@code OptionalInt} with the version, or empty if not set
         */
        public OptionalInt version() {
            return version == null ? OptionalInt.empty() : OptionalInt.of(version);
        }

        /**
         * Updates the name of the native flow that produced this response.
         *
         * @param name the new flow name, or {@code null} to clear the field
         */
        public void setName(String name) {
            this.name = name;
    }

        /**
         * Updates the JSON-encoded parameters captured by the flow.
         *
         * @param paramsJson the new JSON payload, or {@code null} to clear the field
         */
        public void setParamsJson(String paramsJson) {
            this.paramsJson = paramsJson;
    }

        /**
         * Updates the schema version of the native flow payload.
         *
         * @param version the new version, or {@code null} to clear the field
         */
        public void setVersion(Integer version) {
            this.version = version;
    }
    }
}
