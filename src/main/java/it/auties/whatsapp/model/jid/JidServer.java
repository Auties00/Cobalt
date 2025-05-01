package it.auties.whatsapp.model.jid;

import java.util.Objects;

/**
 * The constants of this enumerated type describe the various servers that a jid might be linked
 * to
 */
public sealed interface JidServer extends JidProvider {
    String address();

    @Override
    default Jid toJid() {
        return Jid.of(this);
    }

    static JidServer user() {
        return User.INSTANCE;
    }

    static JidServer groupOrCommunity() {
        return GroupOrCommunity.INSTANCE;
    }

    static JidServer broadcast() {
        return Broadcast.INSTANCE;
    }

    static JidServer groupCall() {
        return GroupCall.INSTANCE;
    }

    static JidServer whatsapp() {
        return Whatsapp.INSTANCE;
    }

    static JidServer lid() {
        return Lid.INSTANCE;
    }

    static JidServer newsletter() {
        return Newsletter.INSTANCE;
    }

    static JidServer unknown(String address) {
        return new Unknown(address);
    }

    static JidServer of(String address) {
        return switch (address) {
            case User.ADDRESS -> User.INSTANCE;
            case GroupOrCommunity.ADDRESS -> GroupOrCommunity.INSTANCE;
            case Broadcast.ADDRESS -> Broadcast.INSTANCE;
            case GroupCall.ADDRESS -> GroupCall.INSTANCE;
            case Whatsapp.ADDRESS -> Whatsapp.INSTANCE;
            case Lid.ADDRESS -> Lid.INSTANCE;
            case Newsletter.ADDRESS -> Newsletter.INSTANCE;
            default -> new Unknown(address);
        };
    }

    final class User implements JidServer {
        private static final String ADDRESS = "c.us";
        private static final User INSTANCE = new User();
        private User() {

        }

        @Override
        public String address() {
            return ADDRESS;
        }

        @Override
        public String toString() {
            return address();
        }
    }

    final class GroupOrCommunity implements JidServer {
        private static final String ADDRESS = "g.us";
        private static final GroupOrCommunity INSTANCE = new GroupOrCommunity();
        private GroupOrCommunity() {

        }

        @Override
        public String address() {
            return ADDRESS;
        }

        @Override
        public String toString() {
            return address();
        }
    }

    final class Broadcast implements JidServer {
        private static final String ADDRESS = "broadcast";
        private static final Broadcast INSTANCE = new Broadcast();
        private Broadcast() {

        }

        @Override
        public String address() {
            return ADDRESS;
        }

        @Override
        public String toString() {
            return address();
        }
    }

    final class GroupCall implements JidServer {
        private static final String ADDRESS = "call";
        private static final GroupCall INSTANCE = new GroupCall();
        private GroupCall() {

        }

        @Override
        public String address() {
            return ADDRESS;
        }

        @Override
        public String toString() {
            return address();
        }
    }

    final class Whatsapp implements JidServer {
        private static final String ADDRESS = "s.whatsapp.net";
        private static final Whatsapp INSTANCE = new Whatsapp();
        private Whatsapp() {

        }

        @Override
        public String address() {
            return ADDRESS;
        }

        @Override
        public String toString() {
            return address();
        }
    }

    final class Lid implements JidServer {
        private static final String ADDRESS = "lid";
        private static final Lid INSTANCE = new Lid();
        private Lid() {

        }

        @Override
        public String address() {
            return ADDRESS;
        }

        @Override
        public String toString() {
            return address();
        }
    }

    final class Newsletter implements JidServer {
        private static final String ADDRESS = "newsletter";
        private static final Newsletter INSTANCE = new Newsletter();
        private Newsletter() {

        }

        @Override
        public String address() {
            return ADDRESS;
        }

        @Override
        public String toString() {
            return address();
        }
    }

    final class Unknown implements JidServer {
        private final String address;

        private Unknown(String address) {
            this.address = Objects.requireNonNull(address, "Invalid address");
        }

        @Override
        public String address() {
            return address;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Unknown) obj;
            return Objects.equals(this.address, that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address);
        }

        @Override
        public String toString() {
            return address;
        }
    }
}
