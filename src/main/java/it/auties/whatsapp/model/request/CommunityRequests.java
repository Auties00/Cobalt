package it.auties.whatsapp.model.request;

import io.avaje.jsonb.Jsonb;
import it.auties.whatsapp.model.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

@SuppressWarnings("UnusedLabel")
public final class CommunityRequests {
    public static String linkedGroups(Jid community, String context) {
        try(
                var result = new StringWriter();
                var writer = Jsonb.builder().build().writer(result)
        ) {
            request: {
                writer.beginObject();
                writer.name("variables");
                variables: {
                    writer.beginObject();
                    writer.name("input");
                    input: {
                        writer.beginObject();
                        writer.name("group_jid");
                        writer.value(community.toString());
                        writer.name("query_context");
                        writer.value(context);
                        writer.endObject();
                    }
                    writer.endObject();
                }
                writer.endObject();
            }
            return result.toString();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static String changeModifyGroupsSetting(Jid community, boolean anyone) {
        try(
                var result = new StringWriter();
                var writer = Jsonb.builder().build().writer(result)
        ) {
            request: {
                writer.beginObject();
                writer.name("variables");
                variables: {
                    writer.beginObject();
                    writer.name("allow_non_admin_sub_group_creation");
                    writer.value(anyone);
                    writer.name("id");
                    writer.value(community.toString());
                    writer.endObject();
                }
                writer.endObject();
            }
            return result.toString();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
