package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.chat.CommunityLinkedGroup;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Json;

import java.util.List;
import java.util.Optional;

public record CommunityLinkedGroupsResponse(List<CommunityLinkedGroup> linkedGroups) {
    public static Optional<CommunityLinkedGroupsResponse> ofJson(byte[] json) {
        return Json.readValue(json, JsonData.class)
                .data()
                .map(result -> {
                    var subGroups = result.value()
                            .subGroups()
                            .edges()
                            .stream()
                            .map(entry -> new CommunityLinkedGroup(entry.node().id(), entry.node().totalParticipantsCount()))
                            .toList();
                    return new CommunityLinkedGroupsResponse(subGroups);
                });
    }

    private record JsonData(Optional<JsonResponse> data) {

    }

    private record JsonResponse(
            @JsonProperty("xwa2_group_query_by_id") Value value) {

    }

    private record Value(@JsonProperty("sub_groups") SubGroups subGroups) {

    }

    private record SubGroups(List<Edge> edges) {

    }

    private record Edge(Node node) {

    }

    private record Node(Jid id, @JsonProperty("total_participants_count") Integer totalParticipantsCount) {

    }
}