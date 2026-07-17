package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Builds the Facebook GraphQL mutation that toggles the AI auto-reply (Maiba) control state for a single chat
 * thread of a WhatsApp Business account.
 *
 * <p>The mutation takes three top-level GraphQL variables. The {@code consumer_lid} names the
 * consumer side of the thread as a WhatsApp LID, the {@code phone_number} carries that consumer's
 * raw phone-number user part, and the {@code thread_status} requests the new AI control state.
 * WhatsApp Web's {@code WAWebAiAgentAutoReplyControlMutation.changeAiReplyStatus} resolves the chat
 * id into both LID and PN forms, then forwards them with the requested status; the Meta graph endpoint returns the
 * outcome under {@code xfb_whatsapp_smb_maiba_status_update}. The reply is consumed through
 * {@link AiAgentAutoReplyControlFacebookGraphQlResponse}.
 *
 * @see AiAgentAutoReplyControlFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebAiAgentAutoReplyControlMutation")
public final class AiAgentAutoReplyControlFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebAiAgentAutoReplyControlMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9175037952515083";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebAiAgentAutoReplyControlMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebAiAgentAutoReplyControlMutation";

    /**
     * Enumerates the closed set of AI auto-reply control states the mutation accepts in the
     * {@code thread_status} variable.
     *
     * <p>WhatsApp Web's {@code WAWebAIAgentAIReplyUtils.mutateAiReplyStatus} derives the requested
     * status from the chat's current AI state, sending {@link #ENABLED} when AI replies are being
     * turned on and {@link #MUTED} when they are being turned off; no other value is produced.
     */
    @WhatsAppWebModule(moduleName = "WAWebAIAgentAIReplyUtils")
    public enum ThreadStatus {
        /**
         * Requests that AI auto-replies be enabled for the thread.
         */
        ENABLED("ENABLED"),

        /**
         * Requests that AI auto-replies be muted for the thread.
         */
        MUTED("MUTED");

        /**
         * Holds the wire token emitted for this status.
         */
        private final String wireValue;

        /**
         * Constructs a status bound to its wire token.
         *
         * @param wireValue the literal token emitted in the {@code thread_status} variable
         */
        ThreadStatus(String wireValue) {
            this.wireValue = wireValue;
        }

        /**
         * Returns the wire token emitted for this status.
         *
         * @return the literal token written to the {@code thread_status} variable, never {@code null}
         */
        public String wireValue() {
            return wireValue;
        }

        /**
         * Resolves a wire token to its {@link ThreadStatus} constant.
         *
         * <p>The lookup is lenient: an unrecognized or {@code null} token maps to
         * {@link Optional#empty()} rather than throwing, so a future server-side status does not break
         * parsing.
         *
         * @param wireValue the token to resolve, may be {@code null}
         * @return the matching {@link ThreadStatus}, or empty when {@code wireValue} matches no constant
         */
        public static Optional<ThreadStatus> of(String wireValue) {
            if (wireValue == null) {
                return Optional.empty();
            }

            for (var status : values()) {
                if (status.wireValue.equals(wireValue)) {
                    return Optional.of(status);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * The {@code consumer_lid} GraphQL variable naming the consumer side of the thread, or
     * {@code null} to omit it.
     */
    private final Jid consumerLid;

    /**
     * The {@code phone_number} GraphQL variable carrying the consumer's raw phone-number user part, or
     * {@code null} to omit it.
     */
    private final String phoneNumber;

    /**
     * The {@code thread_status} GraphQL variable requesting the new AI control state, or {@code null}
     * to omit it.
     */
    private final ThreadStatus threadStatus;

    /**
     * Constructs an AI auto-reply control mutation request.
     *
     * <p>The {@code consumerLid} names the consumer side of the thread as a WhatsApp LID, the
     * {@code phoneNumber} carries that consumer's raw phone-number user part, and the
     * {@code threadStatus} requests the new AI control state. Each value that is {@code null} omits
     * its variable from the serialized object.
     *
     * @param consumerLid  the consumer-side LID of the thread, or {@code null} to omit the variable
     * @param phoneNumber  the consumer's raw phone-number user part, or {@code null} to omit the
     *                     variable
     * @param threadStatus the requested AI control state, or {@code null} to omit the variable
     */
    public AiAgentAutoReplyControlFacebookGraphQlRequest(Jid consumerLid, String phoneNumber, ThreadStatus threadStatus) {
        this.consumerLid = consumerLid;
        this.phoneNumber = phoneNumber;
        this.threadStatus = threadStatus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits {@code {"consumer_lid": <consumerLid>, "phone_number":
     * <phoneNumber>, "thread_status": <threadStatus>}}, writing each variable only when its value is
     * non-null, rendering {@code consumerLid} as its canonical JID string and {@code threadStatus} as
     * its {@link ThreadStatus#wireValue()} token, and emitting {@code "{}"} when all three are
     * {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebAiAgentAutoReplyControlMutation", exports = "changeAiReplyStatus",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (consumerLid != null) {
                writer.writeName("consumer_lid");
                writer.writeColon();
                writer.writeString(consumerLid.toString());
            }

            if (phoneNumber != null) {
                writer.writeName("phone_number");
                writer.writeColon();
                writer.writeString(phoneNumber);
            }

            if (threadStatus != null) {
                writer.writeName("thread_status");
                writer.writeColon();
                writer.writeString(threadStatus.wireValue());
            }
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
