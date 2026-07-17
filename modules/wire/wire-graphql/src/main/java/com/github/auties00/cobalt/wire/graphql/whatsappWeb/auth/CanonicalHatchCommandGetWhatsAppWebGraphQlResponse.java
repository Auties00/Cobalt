package com.github.auties00.cobalt.wire.graphql.whatsappWeb.auth;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelCommand;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelCommandBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the GenAI agent-channel command-catalog query built by
 * {@link CanonicalHatchCommandGetWhatsAppWebGraphQlRequest} into a list of {@link AiChannelCommand}.
 *
 * <p>Reads the plural root {@code wa_genai_hatch_command_get} and projects each entry (server-issued
 * command id, invoked name, description, and prompt template) onto the Cobalt domain model.
 *
 * @see CanonicalHatchCommandGetWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebCanonicalHatchCommandGetQuery")
public final class CanonicalHatchCommandGetWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed command catalog entries.
     */
    private final List<AiChannelCommand> commands;

    /**
     * Constructs a response wrapping the parsed command catalog.
     *
     * <p>Reserved for the static parser.
     *
     * @param commands the parsed command catalog entries
     */
    private CanonicalHatchCommandGetWhatsAppWebGraphQlResponse(List<AiChannelCommand> commands) {
        this.commands = commands;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the plural root {@code wa_genai_hatch_command_get} and projects each entry onto an
     * {@link AiChannelCommand}; the returned {@link Optional} is empty when {@code data} is
     * {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<CanonicalHatchCommandGetWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        return Optional.of(new CanonicalHatchCommandGetWhatsAppWebGraphQlResponse(parseCommands(data.getJSONArray("wa_genai_hatch_command_get"))));
    }

    /**
     * Projects the {@code wa_genai_hatch_command_get} array onto a list of {@link AiChannelCommand}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<AiChannelCommand> parseCommands(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<AiChannelCommand>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new AiChannelCommandBuilder()
                    .id(obj.getString("id"))
                    .name(obj.getString("name"))
                    .description(obj.getString("description"))
                    .prompt(obj.getString("prompt"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed command catalog entries.
     *
     * @return the parsed commands, empty when the relay returned none
     */
    public List<AiChannelCommand> commands() {
        return commands;
    }
}
