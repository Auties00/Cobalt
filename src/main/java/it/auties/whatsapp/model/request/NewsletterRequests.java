package it.auties.whatsapp.model.request;

import com.alibaba.fastjson2.JSONWriter;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.newsletter.NewsletterViewerRole;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

@SuppressWarnings("UnusedLabel")
public final class NewsletterRequests {
    public static String createAdminInviteNewsletter(Jid newsletterJid, Jid adminJid) {
        try(var writer = JSONWriter.ofUTF8()) {
            request: {
                writer.startObject();
                writer.writeName("variables");
                writer.writeColon();
                variables: {
                    writer.startObject();
                    writer.writeName("newsletter_id");
                    writer.writeColon();
                    writer.writeString(newsletterJid.toString());
                    writer.writeName("user_id");
                    writer.writeColon();
                    writer.writeString(adminJid.toString());
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

    public static String createNewsletter(String name, String description, String picture) {
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
                        writer.writeName("name");
                        writer.writeColon();
                        writer.writeString(name);
                        writer.writeName("description");
                        writer.writeColon();
                        writer.writeString(description);
                        writer.writeName("picture");
                        writer.writeColon();
                        writer.writeString(picture);
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

    public static String joinNewsletter(Jid newsletterJid) {
        try(var writer = JSONWriter.ofUTF8()) {
            request: {
                writer.startObject();
                writer.writeName("variables");
                writer.writeColon();
                variables: {
                    writer.startObject();
                    writer.writeName("newsletter_id");
                    writer.writeColon();
                    writer.writeString(newsletterJid.toString());
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

    public static String leaveNewsletter(Jid newsletterJid) {
        try(var writer = JSONWriter.ofUTF8()) {
            request: {
                writer.startObject();
                writer.writeName("variables");
                writer.writeColon();
                variables: {
                    writer.startObject();
                    writer.writeName("newsletter_id");
                    writer.writeColon();
                    writer.writeString(newsletterJid.toString());
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

    public static String newsletterSubscribers(Jid key, String type, NewsletterViewerRole role) {
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
                        writer.writeName("key");
                        writer.writeColon();
                        writer.writeString(key.toString());
                        writer.writeName("type");
                        writer.writeColon();
                        writer.writeString(type);
                        writer.writeName("role");
                        writer.writeColon();
                        writer.writeString(role.name());
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

    public static String queryNewsletter(Jid key, String type, NewsletterViewerRole viewRole, boolean fetchViewerMetadata, boolean fetchFullImage, boolean fetchCreationTime) {
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
                        writer.writeName("key");
                        writer.writeColon();
                        writer.writeString(key.toString());
                        writer.writeName("type");
                        writer.writeColon();
                        writer.writeString(type);
                        writer.writeName("view_role");
                        writer.writeColon();
                        writer.writeString(viewRole.name());
                        writer.endObject();
                    }
                    writer.writeName("fetch_viewer_metadata");
                    writer.writeColon();
                    writer.writeBool(fetchViewerMetadata);
                    writer.writeName("fetch_full_image");
                    writer.writeColon();
                    writer.writeBool(fetchFullImage);
                    writer.writeName("fetch_creation_time");
                    writer.writeColon();
                    writer.writeBool(fetchCreationTime);
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

    public static String recommendedNewsletters(String view, List<String> countryCodes, int limit) {
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
                        writer.writeName("view");
                        writer.writeColon();
                        writer.writeString(view);
                        writer.writeName("filters");
                        writer.writeColon();
                        filters: {
                            writer.startObject();
                            writer.writeName("country_codes");
                            writer.writeColon();
                            writer.startArray();
                            for (String code : countryCodes) {
                                writer.writeString(code);
                            }
                            writer.endArray();
                            writer.endObject();
                        }
                        writer.writeName("limit");
                        writer.writeColon();
                        writer.writeInt32(limit);
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

    public static String revokeAdminInviteNewsletter(Jid newsletterJid, Jid adminJid) {
        try(var writer = JSONWriter.ofUTF8()) {
            request: {
                writer.startObject();
                writer.writeName("variables");
                writer.writeColon();
                variables: {
                    writer.startObject();
                    writer.writeName("newsletter_id");
                    writer.writeColon();
                    writer.writeString(newsletterJid.toString());
                    writer.writeName("user_id");
                    writer.writeColon();
                    writer.writeString(adminJid.toString());
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

    public static String subscribedNewsletters() {
        try(var writer = JSONWriter.ofUTF8()) {
            request: {
                writer.startObject();
                writer.writeName("variables");
                writer.writeColon();
                variables: {
                    writer.startObject();
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

    public static String updateNewsletter(Jid newsletterJid, String description) {
        try(var writer = JSONWriter.ofUTF8()) {
            request: {
                writer.startObject();
                writer.writeName("variables");
                writer.writeColon();
                variables: {
                    writer.startObject();
                    writer.writeName("newsletter_id");
                    writer.writeColon();
                    writer.writeString(newsletterJid.toString());
                    writer.writeName("updates");
                    writer.writeColon();
                    updates: {
                        writer.startObject();
                        writer.writeName("description");
                        writer.writeColon();
                        writer.writeString(description);
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

    public static String acceptAdminInviteNewsletter(Jid newsletterJid) {
        try(var writer = JSONWriter.ofUTF8()) {
            request: {
                writer.startObject();
                writer.writeName("variables");
                writer.writeColon();
                variables: {
                    writer.startObject();
                    writer.writeName("newsletter_id");
                    writer.writeColon();
                    writer.writeString(newsletterJid.toString());
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
