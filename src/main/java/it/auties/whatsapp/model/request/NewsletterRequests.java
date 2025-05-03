package it.auties.whatsapp.model.request;

import io.avaje.jsonb.Jsonb;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.newsletter.NewsletterViewerRole;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

@SuppressWarnings("UnusedLabel")
public final class NewsletterRequests {
    public static String createAdminInviteNewsletter(Jid newsletterJid, Jid adminJid) {
        try (
                var result = new StringWriter();
                var writer = Jsonb.builder().build().writer(result)
        ) {
            request: {
                writer.beginObject();
                writer.name("variables");
                variables: {
                    writer.beginObject();
                    writer.name("newsletter_id");
                    writer.value(newsletterJid.toString());
                    writer.name("user_id");
                    writer.value(adminJid.toString());
                    writer.endObject();
                }
                writer.endObject();
            }
            return result.toString();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static String createNewsletter(String name, String description, String picture) {
        try (
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
                        writer.name("name");
                        writer.value(name);
                        writer.name("description");
                        writer.value(description);
                        writer.name("picture");
                        writer.value(picture);
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

    public static String joinNewsletter(Jid newsletterJid) {
        try (
                var result = new StringWriter();
                var writer = Jsonb.builder().build().writer(result)
        ) {
            request: {
                writer.beginObject();
                writer.name("variables");
                variables: {
                    writer.beginObject();
                    writer.name("newsletter_id");
                    writer.value(newsletterJid.toString());
                    writer.endObject();
                }
                writer.endObject();
            }
            return result.toString();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static String leaveNewsletter(Jid newsletterJid) {
        try (
                var result = new StringWriter();
                var writer = Jsonb.builder().build().writer(result)
        ) {
            request: {
                writer.beginObject();
                writer.name("variables");
                variables: {
                    writer.beginObject();
                    writer.name("newsletter_id");
                    writer.value(newsletterJid.toString());
                    writer.endObject();
                }
                writer.endObject();
            }
            return result.toString();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static String newsletterSubscribers(Jid key, String type, NewsletterViewerRole role) {
        try (
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
                        writer.name("key");
                        writer.value(key.toString());
                        writer.name("type");
                        writer.value(type);
                        writer.name("role");
                        writer.value(role.name());
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

    public static String queryNewsletter(Jid key, String type, NewsletterViewerRole viewRole, boolean fetchViewerMetadata, boolean fetchFullImage, boolean fetchCreationTime) {
        try (
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
                        writer.name("key");
                        writer.value(key.toString());
                        writer.name("type");
                        writer.value(type);
                        writer.name("view_role");
                        writer.value(viewRole.name());
                        writer.endObject();
                    }
                    writer.name("fetch_viewer_metadata");
                    writer.value(fetchViewerMetadata);
                    writer.name("fetch_full_image");
                    writer.value(fetchFullImage);
                    writer.name("fetch_creation_time");
                    writer.value(fetchCreationTime);
                    writer.endObject();
                }
                writer.endObject();
            }
            return result.toString();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static String recommendedNewsletters(String view, List<String> countryCodes, int limit) {
        try (
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
                        writer.name("view");
                        writer.value(view);
                        writer.name("filters");
                        filters: {
                            writer.beginObject();
                            writer.name("country_codes");
                            writer.beginArray();
                            for (String code : countryCodes) {
                                writer.value(code);
                            }
                            writer.endArray();
                            writer.endObject();
                        }
                        writer.name("limit");
                        writer.value(limit);
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

    public static String revokeAdminInviteNewsletter(Jid newsletterJid, Jid adminJid) {
        try (
                var result = new StringWriter();
                var writer = Jsonb.builder().build().writer(result)
        ) {
            request: {
                writer.beginObject();
                writer.name("variables");
                variables: {
                    writer.beginObject();
                    writer.name("newsletter_id");
                    writer.value(newsletterJid.toString());
                    writer.name("user_id");
                    writer.value(adminJid.toString());
                    writer.endObject();
                }
                writer.endObject();
            }
            return result.toString();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static String subscribedNewsletters() {
        try (
                var result = new StringWriter();
                var writer = Jsonb.builder().build().writer(result)
        ) {
            request: {
                writer.beginObject();
                writer.name("variables");
                variables: {
                    writer.beginObject();
                    writer.endObject();
                }
                writer.endObject();
            }
            return result.toString();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static String updateNewsletter(Jid newsletterJid, String description) {
        try (
                var result = new StringWriter();
                var writer = Jsonb.builder().build().writer(result)
        ) {
            request: {
                writer.beginObject();
                writer.name("variables");
                variables: {
                    writer.beginObject();
                    writer.name("newsletter_id");
                    writer.value(newsletterJid.toString());
                    writer.name("updates");
                    updates: {
                        writer.beginObject();
                        writer.name("description");
                        writer.value(description);
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

    public static String acceptAdminInviteNewsletter(Jid newsletterJid) {
        try (
                var result = new StringWriter();
                var writer = Jsonb.builder().build().writer(result)
        ) {
            request: {
                writer.beginObject();
                writer.name("variables");
                variables: {
                    writer.beginObject();
                    writer.name("newsletter_id");
                    writer.value(newsletterJid.toString());
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
