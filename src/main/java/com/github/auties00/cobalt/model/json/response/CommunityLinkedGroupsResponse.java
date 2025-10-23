package com.github.auties00.cobalt.model.json.response;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.model.proto.chat.CommunityLinkedGroup;
import com.github.auties00.cobalt.model.proto.chat.CommunityLinkedGroupBuilder;
import com.github.auties00.cobalt.model.proto.jid.Jid;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;

public final class CommunityLinkedGroupsResponse {
    private final SequencedSet<CommunityLinkedGroup> linkedGroups;

    private CommunityLinkedGroupsResponse(SequencedSet<CommunityLinkedGroup> linkedGroups) {
        this.linkedGroups = linkedGroups;
    }

    public static Optional<CommunityLinkedGroupsResponse> ofJson(byte[] json) {
        if(json == null) {
            return Optional.empty();
        }

        var jsonObject = JSON.parseObject(json);
        if(jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if(data == null) {
            return Optional.empty();
        }

        var response = data.getJSONObject("xwa2_group_query_by_id");
        if(response == null) {
            return Optional.empty();
        }

        var subGroups = response.getJSONObject("sub_groups");
        if(subGroups == null) {
            return Optional.empty();
        }

        var edges = subGroups.getJSONArray("edges");
        if(edges == null) {
            return Optional.empty();
        }

        var length = edges.size();
        var links = new LinkedHashSet<CommunityLinkedGroup>(length);
        for (var i = 0; i < length; i++) {
            var edge = edges.getJSONObject(i);
            var node = edge.getJSONObject("node");
            if (node == null) {
                continue;
            }

            var id = node.getString("id");
            if (id == null) {
                continue;
            }

            var totalParticipantsCount = node.getInteger("total_participants_count");
            var link = new CommunityLinkedGroupBuilder()
                    .jid(Jid.of(id))
                    .participants(totalParticipantsCount)
                    .build();
            links.add(link);
        }
        var result = new CommunityLinkedGroupsResponse(links);
        return Optional.of(result);
    }

    public SequencedSet<CommunityLinkedGroup> linkedGroups() {
        return Collections.unmodifiableSequencedSet(linkedGroups);
    }
}