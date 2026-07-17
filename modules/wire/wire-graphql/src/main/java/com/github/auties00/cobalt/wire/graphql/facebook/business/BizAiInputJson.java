package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.linked.business.ai.AiChatHistoryMessage;
import com.github.auties00.cobalt.wire.linked.business.ai.AiChatHistoryThread;
import com.github.auties00.cobalt.wire.linked.business.ai.AiChatHistoryUploadRequest;
import com.github.auties00.cobalt.wire.linked.business.ai.AiEmojisConfig;
import com.github.auties00.cobalt.wire.linked.business.ai.AiFaqEntry;
import com.github.auties00.cobalt.wire.linked.business.ai.AiPriceConfig;
import com.github.auties00.cobalt.wire.linked.business.ai.AiProductImage;
import com.github.auties00.cobalt.wire.linked.business.ai.AiRuleInput;
import com.github.auties00.cobalt.wire.linked.business.ai.AiLeadGenFlowInput;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiLeadGenField;

import java.util.List;

/**
 * Serializes the WhatsApp Business AI agent input models to their Facebook GraphQL JSON shape.
 *
 * <p>The model types are pure, transport-agnostic domain holders carrying camelCase fields; the
 * snake_case GraphQL keys the Meta graph endpoint expects are a transport concern that lives here. Each
 * helper writes one input model as a JSON object into a caller-provided {@link JSONWriter}, omitting any
 * field whose value is {@code null} or, for lists, empty, so an unset field never appears in the output.
 * The helpers are shared by the {@code BizAi*FacebookGraphQlRequest} operations that carry these models.
 */
final class BizAiInputJson {
    /**
     * Prevents instantiation of this static-helper holder.
     */
    private BizAiInputJson() {
        throw new AssertionError();
    }

    /**
     * Writes a lead-generation flow input as the {@code {custom_moment, moment_type, fields, id}}
     * object.
     *
     * <p>The {@code fields} array is written only when non-empty, and each entry is written through
     * {@link #writeLeadGenField(JSONWriter, BusinessAiLeadGenField)}; every scalar is written only when
     * present.
     *
     * @param writer the writer to append the object to
     * @param input  the lead-generation flow input to serialize
     */
    static void writeLeadGenFlowInput(JSONWriter writer, AiLeadGenFlowInput input) {
        writer.startObject();
        input.customMoment().ifPresent(value -> {
            writer.writeName("custom_moment");
            writer.writeColon();
            writer.writeString(value);
        });
        input.momentType().ifPresent(value -> {
            writer.writeName("moment_type");
            writer.writeColon();
            writer.writeString(value);
        });
        var fields = input.fields();
        if (!fields.isEmpty()) {
            writer.writeName("fields");
            writer.writeColon();
            writeArray(writer, fields, BizAiInputJson::writeLeadGenField);
        }
        input.id().ifPresent(value -> {
            writer.writeName("id");
            writer.writeColon();
            writer.writeString(value);
        });
        writer.endObject();
    }

    /**
     * Writes one lead-capture field as the {@code {label, is_enabled}} object.
     *
     * @param writer the writer to append the object to
     * @param field  the lead-capture field to serialize
     */
    static void writeLeadGenField(JSONWriter writer, BusinessAiLeadGenField field) {
        writer.startObject();
        field.label().ifPresent(value -> {
            writer.writeName("label");
            writer.writeColon();
            writer.writeString(value);
        });
        writer.writeName("is_enabled");
        writer.writeColon();
        writer.writeBool(field.enabled());
        writer.endObject();
    }

    /**
     * Writes a rule input as the {@code {custom_rule, rule_type, emojis_config, price_config, rule_id}}
     * object.
     *
     * <p>The {@code emojis_config} and {@code price_config} sub-objects are written only when present;
     * every scalar is written only when present.
     *
     * @param writer the writer to append the object to
     * @param input  the rule input to serialize
     */
    static void writeRuleInput(JSONWriter writer, AiRuleInput input) {
        writer.startObject();
        input.customRule().ifPresent(value -> {
            writer.writeName("custom_rule");
            writer.writeColon();
            writer.writeString(value);
        });
        input.ruleType().ifPresent(value -> {
            writer.writeName("rule_type");
            writer.writeColon();
            writer.writeString(value);
        });
        input.emojisConfig().ifPresent(config -> {
            writer.writeName("emojis_config");
            writer.writeColon();
            writeEmojisConfig(writer, config);
        });
        input.priceConfig().ifPresent(config -> {
            writer.writeName("price_config");
            writer.writeColon();
            writePriceConfig(writer, config);
        });
        input.ruleId().ifPresent(value -> {
            writer.writeName("rule_id");
            writer.writeColon();
            writer.writeString(value);
        });
        writer.endObject();
    }

    /**
     * Writes an emoji-usage configuration as the {@code {emojis_freq}} object.
     *
     * @param writer the writer to append the object to
     * @param config the emoji-usage configuration to serialize
     */
    static void writeEmojisConfig(JSONWriter writer, AiEmojisConfig config) {
        writer.startObject();
        config.emojisFreq().ifPresent(value -> {
            writer.writeName("emojis_freq");
            writer.writeColon();
            writer.writeString(value);
        });
        writer.endObject();
    }

    /**
     * Writes a price-sharing configuration as the {@code {price_sharing}} object.
     *
     * @param writer the writer to append the object to
     * @param config the price-sharing configuration to serialize
     */
    static void writePriceConfig(JSONWriter writer, AiPriceConfig config) {
        writer.startObject();
        config.priceSharing().ifPresent(value -> {
            writer.writeName("price_sharing");
            writer.writeColon();
            writer.writeString(value);
        });
        writer.endObject();
    }

    /**
     * Writes one FAQ entry as the {@code {question, answer, id}} object.
     *
     * @param writer the writer to append the object to
     * @param entry  the FAQ entry to serialize
     */
    static void writeFaqEntry(JSONWriter writer, AiFaqEntry entry) {
        writer.startObject();
        entry.question().ifPresent(value -> {
            writer.writeName("question");
            writer.writeColon();
            writer.writeString(value);
        });
        entry.answer().ifPresent(value -> {
            writer.writeName("answer");
            writer.writeColon();
            writer.writeString(value);
        });
        entry.id().ifPresent(value -> {
            writer.writeName("id");
            writer.writeColon();
            writer.writeString(value);
        });
        writer.endObject();
    }

    /**
     * Writes one product-image reference as the {@code {image_id, image_url}} object.
     *
     * @param writer the writer to append the object to
     * @param image  the product-image reference to serialize
     */
    static void writeProductImage(JSONWriter writer, AiProductImage image) {
        writer.startObject();
        image.imageId().ifPresent(value -> {
            writer.writeName("image_id");
            writer.writeColon();
            writer.writeString(value);
        });
        image.imageUrl().ifPresent(value -> {
            writer.writeName("image_url");
            writer.writeColon();
            writer.writeString(value);
        });
        writer.endObject();
    }

    /**
     * Writes a chat-history upload request as the {@code {threads}} object.
     *
     * <p>The {@code threads} array is written only when non-empty, and each entry is written through
     * {@link #writeChatHistoryThread(JSONWriter, AiChatHistoryThread)}.
     *
     * @param writer  the writer to append the object to
     * @param request the chat-history upload request to serialize
     */
    static void writeChatHistoryUploadRequest(JSONWriter writer, AiChatHistoryUploadRequest request) {
        writer.startObject();
        var threads = request.threads();
        if (!threads.isEmpty()) {
            writer.writeName("threads");
            writer.writeColon();
            writeArray(writer, threads, BizAiInputJson::writeChatHistoryThread);
        }
        writer.endObject();
    }

    /**
     * Writes one chat-history conversation as the {@code {consumer_uid, messages}} object.
     *
     * @param writer the writer to append the object to
     * @param thread the conversation to serialize
     */
    static void writeChatHistoryThread(JSONWriter writer, AiChatHistoryThread thread) {
        writer.startObject();
        thread.consumerUid().ifPresent(value -> {
            writer.writeName("consumer_uid");
            writer.writeColon();
            writer.writeString(value);
        });
        var messages = thread.messages();
        if (!messages.isEmpty()) {
            writer.writeName("messages");
            writer.writeColon();
            writeArray(writer, messages, BizAiInputJson::writeChatHistoryMessage);
        }
        writer.endObject();
    }

    /**
     * Writes one chat-history message as the {@code {author, message_type, text, timestamp}} object.
     *
     * @param writer  the writer to append the object to
     * @param message the message to serialize
     */
    static void writeChatHistoryMessage(JSONWriter writer, AiChatHistoryMessage message) {
        writer.startObject();
        message.author().ifPresent(value -> {
            writer.writeName("author");
            writer.writeColon();
            writer.writeString(value);
        });
        message.messageType().ifPresent(value -> {
            writer.writeName("message_type");
            writer.writeColon();
            writer.writeString(value);
        });
        message.text().ifPresent(value -> {
            writer.writeName("text");
            writer.writeColon();
            writer.writeString(value);
        });
        message.timestamp().ifPresent(value -> {
            writer.writeName("timestamp");
            writer.writeColon();
            writer.writeString(value);
        });
        writer.endObject();
    }

    /**
     * Writes each element of {@code values} as a JSON array using {@code elementWriter}.
     *
     * @param writer        the writer to append the array to
     * @param values        the elements to serialize, in order
     * @param elementWriter the per-element serializer
     * @param <T>           the element type
     */
    private static <T> void writeArray(JSONWriter writer, List<T> values, ElementWriter<T> elementWriter) {
        writer.startArray();
        for (var i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.writeComma();
            }
            elementWriter.write(writer, values.get(i));
        }
        writer.endArray();
    }

    /**
     * Serializes a single element of a JSON array into a {@link JSONWriter}.
     *
     * @param <T> the element type this writer serializes
     */
    @FunctionalInterface
    private interface ElementWriter<T> {
        /**
         * Writes {@code value} into {@code writer}.
         *
         * @param writer the writer to append to
         * @param value  the element to serialize
         */
        void write(JSONWriter writer, T value);
    }
}
