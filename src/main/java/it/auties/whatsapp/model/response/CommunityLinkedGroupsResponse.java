package it.auties.whatsapp.model.response;

import io.avaje.jsonb.Json;
import it.auties.whatsapp.model.chat.CommunityLinkedGroup;
import it.auties.whatsapp.model.chat.CommunityLinkedGroupBuilder;
import it.auties.whatsapp.model.jid.Jid;

import java.util.*;

public final class CommunityLinkedGroupsResponse {
    private static final CommunityLinkedGroupsResponse EMPTY = new CommunityLinkedGroupsResponse(List.of());

    private final SequencedSet<CommunityLinkedGroup> linkedGroups;

    private CommunityLinkedGroupsResponse(SequencedSet<CommunityLinkedGroup> linkedGroups) {
        this.linkedGroups = linkedGroups;
    }

    @Json.Creator
    static CommunityLinkedGroupsResponse of(@Json.Unmapped Map<String, Object> json) {
        if(!(json.get("data") instanceof Map<?,?> data)) {
            return EMPTY;
        }

        if(!(data.get("xwa2_group_query_by_id") instanceof Map<?,?> response)) {
            return EMPTY;
        }

        if(!(response.get("sub_groups") instanceof Map<?,?> subGroups)) {
            return EMPTY;
        }

        if(!(subGroups.get("edges") instanceof List<?> edges)) {
            return EMPTY;
        }

        var links = new LinkedHashSet<CommunityLinkedGroup>(edges.size());
        for(var edge : edges) {
            if(!(edge instanceof Map<?,?> map)) {
                continue;
            }

            if(!(map.get("id") instanceof String id)) {
                continue;
            }

            if(!(map.get("total_participants_count") instanceof Number totalParticipantsCount)) {
                continue;
            }

            var link = new CommunityLinkedGroupBuilder()
                    .jid(Jid.of(id))
                    .participants(totalParticipantsCount.intValue())
                    .build();
            links.add(link);
        }
        return new CommunityLinkedGroupsResponse(links);
    }

    public SequencedSet<CommunityLinkedGroup> linkedGroups() {
        return Collections.unmodifiableSequencedSet(linkedGroups);
    }
}