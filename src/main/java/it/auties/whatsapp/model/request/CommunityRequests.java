package it.auties.whatsapp.model.request;

import com.alibaba.fastjson2.JSONWriter;
import it.auties.whatsapp.model.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

@SuppressWarnings("UnusedLabel")
public final class CommunityRequests {
    public static String linkedGroups(Jid community, String context) {
        try(var writer = JSONWriter.ofUTF8()) {
            request: {
                writer.startObject();
                writer.writeName("variables");
                writer.writeColon();
                variables: {
                    writer.startObject();
                    writer.writeName("input");
                    writer.writeColon();
                    input: {
                        writer.startObject();
                        writer.writeName("group_jid");
                        writer.writeColon();
                        writer.writeString(community.toString());
                        writer.writeName("query_context");
                        writer.writeColon();
                        writer.writeString(context);
                        writer.endObject();
                    }
                    writer.endObject();
                }
                writer.endObject();
            }
            try(var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static String changeModifyGroupsSetting(Jid community, boolean anyone) {
        try(var writer = JSONWriter.ofUTF8()) {
            request: {
                writer.startObject();
                writer.writeName("variables");
                writer.writeColon();
                variables: {
                    writer.startObject();
                    writer.writeName("allow_non_admin_sub_group_creation");
                    writer.writeColon();
                    writer.writeBool(anyone);
                    writer.writeName("id");
                    writer.writeColon();
                    writer.writeString(community.toString());
                    writer.endObject();
                }
                writer.endObject();
            }
            try(var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
