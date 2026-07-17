package com.github.auties00.cobalt.wire.graphql.whatsappWeb.acs;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialEvaluation;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialEvaluationBuilder;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialIssuance;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialIssuanceBuilder;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialProof;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialProofBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the anonymous-credential batch issuance mutation built by
 * {@link AcsServerProviderIssuanceWhatsAppWebGraphQlRequest} into an {@link AnonymousCredentialIssuance}.
 *
 * <p>Reads the linked {@code xwa_wa_acs_issue_credentials} root and projects its {@code success}
 * marker, its per-token {@code creds.evaluation} and {@code creds.proof} arrays, and its
 * {@code error_message} onto an {@link AnonymousCredentialIssuance}.
 *
 * @see AcsServerProviderIssuanceWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebACSServerProviderIssuanceMutation")
public final class AcsServerProviderIssuanceWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed issuance outcome.
     */
    private final AnonymousCredentialIssuance issuance;

    /**
     * Constructs a response wrapping the parsed issuance outcome.
     *
     * <p>Reserved for the static parser.
     *
     * @param issuance the parsed issuance outcome
     */
    private AcsServerProviderIssuanceWhatsAppWebGraphQlResponse(AnonymousCredentialIssuance issuance) {
        this.issuance = issuance;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked {@code xwa_wa_acs_issue_credentials} root and projects it onto an
     * {@link AnonymousCredentialIssuance}; the returned {@link Optional} is empty when
     * {@code data} or the root is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the root is missing
     */
    public static Optional<AcsServerProviderIssuanceWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa_wa_acs_issue_credentials");
        if (root == null) {
            return Optional.empty();
        }

        var success = root.getBoolean("success");
        var creds = root.getJSONObject("creds");
        List<AnonymousCredentialEvaluation> evaluations = List.of();
        List<AnonymousCredentialProof> proofs = List.of();
        if (creds != null) {
            evaluations = parseEvaluations(creds.getJSONArray("evaluation"));
            proofs = parseProofs(creds.getJSONArray("proof"));
        }

        var issuance = new AnonymousCredentialIssuanceBuilder()
                .success(success != null && success)
                .evaluations(evaluations)
                .proofs(proofs)
                .errorMessage(root.getString("error_message"))
                .build();
        return Optional.of(new AcsServerProviderIssuanceWhatsAppWebGraphQlResponse(issuance));
    }

    /**
     * Projects the {@code evaluation} array onto a list of {@link AnonymousCredentialEvaluation}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<AnonymousCredentialEvaluation> parseEvaluations(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<AnonymousCredentialEvaluation>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new AnonymousCredentialEvaluationBuilder()
                    .data(obj.getString("data"))
                    .build());
        }
        return result;
    }

    /**
     * Projects the {@code proof} array onto a list of {@link AnonymousCredentialProof}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<AnonymousCredentialProof> parseProofs(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<AnonymousCredentialProof>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new AnonymousCredentialProofBuilder()
                    .firstComponent(obj.getString("c"))
                    .secondComponent(obj.getString("s"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed issuance outcome.
     *
     * @return the parsed {@link AnonymousCredentialIssuance}, never {@code null}
     */
    public AnonymousCredentialIssuance issuance() {
        return issuance;
    }
}
