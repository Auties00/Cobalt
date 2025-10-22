import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.api.WhatsappSixPartsKeys;
import com.github.auties00.cobalt.model.jid.JidDevice;

void main() {
    var sixParts = promptSixParts();
    var business = promptBusiness();
    Whatsapp.builder()
            .mobileClient()
            .loadConnection(WhatsappSixPartsKeys.of(sixParts))
            // .proxy(URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy
            .device(JidDevice.ios(business)) // Make sure to select the correct account type(business or personal) or you'll get error 401
            .registered()
            .orElseThrow()
            .addNodeReceivedListener((_, incoming) -> System.out.printf("Received node %s%n", incoming))
            .addNodeSentListener((_, outgoing )-> System.out.printf("Sent node %s%n", outgoing))
            .addLoggedInListener(_ -> System.out.println("Logged in"))
            .connect() // If you get error 403 o 503 the account is banned
            .waitForDisconnection();
}

String promptSixParts() {
    return IO.readln("Enter the six parts segment: ")
            .trim();
}

boolean promptBusiness() {
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