package it.auties.whatsapp.protobuf.contact;

import java.util.Objects;

public record ContactId(String user, int device, int agent, String server, boolean ad) {
    public static final String OFFICIAL_BIZ_WID = "16505361212@c.us";
    public static final String USER_JID_SUFFIX = "s.whatsapp.net";
    public static final ContactId GROUP = createServer("g.us");
    public static final ContactId WHATSAPP_SERVER = createServer("s.whatsapp.net");

    public static ContactId create(String jid) {
        return new ContactId(toUserId(jid), 0, 0, USER_JID_SUFFIX, false);
    }

    public static ContactId create(String jid, String server) {
        return new ContactId(toUserId(jid), 0, 0, server, false);
    }

    public static ContactId createServer(String server) {
        return new ContactId(null, 0, 0, server, false);
    }

    public static ContactId createAd(String jid, int agent, int device) {
        return new ContactId(toUserId(jid), device, agent, USER_JID_SUFFIX, true);
    }

    public static String toUserId(String jid) {
        if(jid == null) {
            return null;
        }

        return jid.replace("@c.us", "").replace("@s.whatsapp.net", "").replace("@g.us", "");
    }

    public Type type() {
        if (device() != 0) {
            return Type.COMPANION;
        }

        if (Objects.equals(server(), "s.whatsapp.net")) {
            return Type.USER;
        }

        if (Objects.equals(server(), "broadcast")) {
            return Type.BROADCAST;
        }

        if (toString().equals(OFFICIAL_BIZ_WID)) {
            return Type.OFFICIAL_BIZ_ACCOUNT;
        }

        if (Objects.equals(server(), "g.us")) {
            return Type.SERVER;
        }

        if (Objects.equals(server(), "call")) {
            return Type.GROUP_CALL;
        }

        if (Objects.equals(server(), "c.us") && Objects.equals(user(), "server")) {
            return Type.SERVER;
        }

        if (Objects.equals(server(), "c.us") && Objects.equals(user(), "0")) {
            return Type.PSA;
        }

        if(Objects.equals(user(), "status") && Objects.equals(server(), "broadcast")){
            return Type.STATUS_V3;
        }

        return Type.UNKNOWN;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ContactId jid && Objects.equals(server(), jid.server());
    }

    @Override
    public String toString() {
        if (ad()) {
            if (agent == 0 && device == 0) {
                return "%s@%s".formatted(user, USER_JID_SUFFIX);
            }

            if (agent != 0 && device == 0) {
                return "%s.%s@%s".formatted(user, agent, USER_JID_SUFFIX);
            }

            if (agent == 0) {
                return "%s:%s@%s".formatted(user, device, USER_JID_SUFFIX);
            }

            return "%s.%s:%s@%s".formatted(user, agent, device, USER_JID_SUFFIX);
        }

        if (user() != null) {
            return "%s@%s".formatted(user(), server);
        }

        return server;
    }

    enum Type {
        COMPANION, USER, BROADCAST, OFFICIAL_BIZ_ACCOUNT, GROUP, GROUP_CALL, SERVER, PSA, STATUS_V3, UNKNOWN
    }
}