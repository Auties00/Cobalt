package com.github.auties00.cobalt.wire.graphql.facebook.promotion;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionLogAcknowledgement;
import com.github.auties00.cobalt.wire.linked.business.promotion.QuickPromotionLogAcknowledgementBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the WhatsApp Business quick-promotion log-event mutation built by
 * {@link QuickPromotionActionFacebookGraphQlRequest} into a {@link QuickPromotionLogAcknowledgement}.
 *
 * <p>Reads the linked {@code wa_quick_promotion_log_event} root and projects its
 * {@code client_mutation_id} scalar onto a {@link QuickPromotionLogAcknowledgement}.
 *
 * @see QuickPromotionActionFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebQuickPromotionActionMutation")
public final class QuickPromotionActionFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed acknowledgement.
     */
    private final QuickPromotionLogAcknowledgement acknowledgement;

    /**
     * Constructs a response wrapping the parsed acknowledgement.
     *
     * <p>Reserved for the static parser.
     *
     * @param acknowledgement the parsed acknowledgement
     */
    private QuickPromotionActionFacebookGraphQlResponse(QuickPromotionLogAcknowledgement acknowledgement) {
        this.acknowledgement = acknowledgement;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked {@code wa_quick_promotion_log_event} root and projects it onto a
     * {@link QuickPromotionLogAcknowledgement}; the returned {@link Optional} is empty when
     * {@code data} or the root is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the root is missing
     */
    public static Optional<QuickPromotionActionFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("wa_quick_promotion_log_event");
        if (root == null) {
            return Optional.empty();
        }

        var acknowledgement = new QuickPromotionLogAcknowledgementBuilder()
                .clientMutationId(root.getString("client_mutation_id"))
                .build();
        return Optional.of(new QuickPromotionActionFacebookGraphQlResponse(acknowledgement));
    }

    /**
     * Returns the parsed acknowledgement.
     *
     * @return the parsed {@link QuickPromotionLogAcknowledgement}, never {@code null}
     */
    public QuickPromotionLogAcknowledgement acknowledgement() {
        return acknowledgement;
    }
}
