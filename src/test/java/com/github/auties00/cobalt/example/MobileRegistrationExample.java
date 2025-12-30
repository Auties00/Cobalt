import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.model.jid.JidCompanion;

void main() {
    var phoneNumber = promptPhoneNumber();
    System.out.println("Select if the account is business or personal:\n(1) Business (2) Personal");
    var business = promptBusiness();
    WhatsAppClient.builder()
            .mobileClient()
            .createConnection()
            // .proxy(URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy
            .device(JidCompanion.ios(business)) // Make sure to select the correct account type(business or personal) or you'll get error 401
            .register(phoneNumber, WhatsAppClientVerificationHandler.Mobile.sms(this::promptVerificationCode))
            .addNodeReceivedListener((_, incoming) -> System.out.printf("Received node %s%n", incoming))
            .addNodeSentListener((_, outgoing) -> System.out.printf("Sent node %s%n", outgoing))
            .addLoggedInListener(_ -> System.out.println("Logged in"))
            .connect() // If you get error 403 o 503 the account is banned
            .waitForDisconnection();
}

// You can get a value from https://daisysms.com, do not spam registrations, or you'll get banned
private long promptPhoneNumber() {
    while (true) {
        try {
            var input = IO.readln("Enter the phone number: ");
            return Long.parseUnsignedLong(input);
        } catch(NumberFormatException _) {
            IO.println("Invalid phone number!");
        }
    }
}

private boolean promptBusiness() {
    while (true) {
        var type = IO.readln("Select if the account is business or personal:\n(1) Business (2) Personal")
                .trim();
        if(type.equals("1")) {
            return true;
        }else if(type.equals("2")) {
            return false;
        }else {
            IO.println("Invalid option!");
        }
    }
}

private String promptVerificationCode() {
    return IO.readln("Enter the verification code: ")
            .trim()
            .replace("-", "");
}