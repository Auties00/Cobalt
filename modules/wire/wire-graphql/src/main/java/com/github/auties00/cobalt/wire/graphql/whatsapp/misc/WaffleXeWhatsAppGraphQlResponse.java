package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingApplication;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingDestination;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingDestinationBuilder;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingDestinationParameters;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingDestinationParametersBuilder;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingDestinationResolution;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingDestinationResolutionBuilder;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingEligibility;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingEligibilityBuilder;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingPublicKeys;
import com.github.auties00.cobalt.wire.linked.business.crossposting.CrossPostingPublicKeysBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the cross-posting eligibility-check mutation built by
 * {@link WaffleXeWhatsAppGraphQlRequest} into a {@link CrossPostingEligibility}.
 *
 * <p>Reads the linked root {@code waffle_xe_root} and projects its per-purpose public keys, the
 * echoed unique-ids collection, the per-status resolved destination identities, and the
 * per-destination eligibility parameters onto the Cobalt domain model.
 *
 * @see WaffleXeWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebWaffleXEQuery")
public final class WaffleXeWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed eligibility tree.
     */
    private final CrossPostingEligibility eligibility;

    /**
     * Constructs a response wrapping the parsed eligibility tree.
     *
     * <p>Reserved for the static parser.
     *
     * @param eligibility the parsed eligibility tree, or {@code null} when the graph.whatsapp.com endpoint omitted the
     *                    field
     */
    private WaffleXeWhatsAppGraphQlResponse(CrossPostingEligibility eligibility) {
        this.eligibility = eligibility;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code waffle_xe_root} and projects it onto a
     * {@link CrossPostingEligibility}; the returned {@link Optional} is empty when {@code data} or
     * the root object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the root object is missing
     */
    public static Optional<WaffleXeWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("waffle_xe_root");
        if (root == null) {
            return Optional.empty();
        }

        var eligibility = new CrossPostingEligibilityBuilder()
                .publicKeys(parsePublicKeys(root.getJSONObject("purpose_public_keys")))
                .uniqueIds(root.getString("waffle_unique_ids"))
                .destinationResolutions(parseResolutions(root.getJSONArray("waffle_d")))
                .destinationParameters(parseParameters(root.getJSONArray("waffle_xps")))
                .build();
        return Optional.of(new WaffleXeWhatsAppGraphQlResponse(eligibility));
    }

    /**
     * Projects the {@code purpose_public_keys} object onto a {@link CrossPostingPublicKeys}.
     *
     * @param obj the JSON object to project
     * @return the projected public keys, or {@code null} when {@code obj} is {@code null}
     */
    private static CrossPostingPublicKeys parsePublicKeys(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        return new CrossPostingPublicKeysBuilder()
                .ephemeralPublicKey(obj.getString("purpose_public_ek"))
                .identityPublicKey(obj.getString("purpose_public_ik"))
                .identityPublicKeySignature(obj.getString("purpose_public_ik_sig"))
                .identityPublicKeyEncryptionCertificate(obj.getString("purpose_public_ik_enc_certificate"))
                .dummyCiphertext(obj.getString("purpose_dummy_ciphertext"))
                .dummyNonce(obj.getString("purpose_dummy_nonce"))
                .build();
    }

    /**
     * Projects the {@code waffle_d} array onto a list of {@link CrossPostingDestinationResolution}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<CrossPostingDestinationResolution> parseResolutions(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<CrossPostingDestinationResolution>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new CrossPostingDestinationResolutionBuilder()
                    .destination(parseDestination(obj.getJSONObject("waffle_xas")))
                    .destinationId(obj.getString("waffle_di"))
                    .build());
        }
        return result;
    }

    /**
     * Projects the {@code waffle_xps} array onto a list of {@link CrossPostingDestinationParameters}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<CrossPostingDestinationParameters> parseParameters(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<CrossPostingDestinationParameters>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new CrossPostingDestinationParametersBuilder()
                    .destination(parseDestination(obj.getJSONObject("waffle_xas")))
                    .hashedCodecParameters(obj.getString("waffle_hcbc"))
                    .build());
        }
        return result;
    }

    /**
     * Projects the {@code waffle_xas} object onto a {@link CrossPostingDestination}.
     *
     * @param obj the JSON object to project
     * @return the projected destination, or {@code null} when {@code obj} is {@code null}
     */
    private static CrossPostingDestination parseDestination(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        return new CrossPostingDestinationBuilder()
                .application(CrossPostingApplication.ofWireValue(obj.getString("waffle_xan")).orElse(null))
                .surface(obj.getString("waffle_xs"))
                .build();
    }

    /**
     * Returns the parsed eligibility tree.
     *
     * @return the parsed {@link CrossPostingEligibility}, never {@code null}
     */
    public CrossPostingEligibility eligibility() {
        return eligibility;
    }
}
