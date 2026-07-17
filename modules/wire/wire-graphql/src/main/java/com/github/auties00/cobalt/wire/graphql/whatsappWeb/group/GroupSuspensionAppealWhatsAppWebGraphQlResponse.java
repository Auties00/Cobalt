package com.github.auties00.cobalt.wire.graphql.whatsappWeb.group;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupSuspensionAppeal;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupSuspensionAppealBuilder;

import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the group-suspension-appeal mutation built by
 * {@link GroupSuspensionAppealWhatsAppWebGraphQlRequest} into a {@link GroupSuspensionAppeal}.
 *
 * <p>Reads the linked {@code wa_create_group_suspension_appeal} root and projects its
 * {@code response_code} verdict, {@code error_message}, and {@code appeal_creation_time} onto a
 * {@link GroupSuspensionAppeal}.
 *
 * @see GroupSuspensionAppealWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebGroupSuspensionAppealMutation")
public final class GroupSuspensionAppealWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed appeal outcome.
     */
    private final GroupSuspensionAppeal appeal;

    /**
     * Constructs a response wrapping the parsed appeal outcome.
     *
     * <p>Reserved for the static parser.
     *
     * @param appeal the parsed appeal outcome
     */
    private GroupSuspensionAppealWhatsAppWebGraphQlResponse(GroupSuspensionAppeal appeal) {
        this.appeal = appeal;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked {@code wa_create_group_suspension_appeal} root and projects it onto a
     * {@link GroupSuspensionAppeal}; the returned {@link Optional} is empty when {@code data} or
     * the root is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the root is missing
     */
    public static Optional<GroupSuspensionAppealWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("wa_create_group_suspension_appeal");
        if (root == null) {
            return Optional.empty();
        }

        var appeal = new GroupSuspensionAppealBuilder()
                .verdict(root.getString("response_code"))
                .errorMessage(root.getString("error_message"))
                .filedAtEpochSecond(root.getLong("appeal_creation_time"))
                .build();
        return Optional.of(new GroupSuspensionAppealWhatsAppWebGraphQlResponse(appeal));
    }

    /**
     * Returns the parsed appeal outcome.
     *
     * @return the parsed {@link GroupSuspensionAppeal}, never {@code null}
     */
    public GroupSuspensionAppeal appeal() {
        return appeal;
    }
}
