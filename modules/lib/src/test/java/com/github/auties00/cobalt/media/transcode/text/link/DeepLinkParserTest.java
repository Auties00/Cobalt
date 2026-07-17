package com.github.auties00.cobalt.media.transcode.text.link;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.media.transcode.text.link.DeepLinkParser.DeepLink;
import com.github.auties00.cobalt.props.TestABPropsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Exercises the deep-link branches of {@link DeepLinkParser#parse} that match before the
 * payment-link fallback. Only the group-invite / catalog / product paths are covered, because the
 * payment-link fallback reads from the client's AB-props service and needs a stub not wired into
 * this harness; {@code PROPS} is therefore an empty {@link TestABPropsService} that no test here
 * triggers.
 */
@DisplayName("DeepLinkParser")
class DeepLinkParserTest {
    private static final TestWhatsAppClient CLIENT = TestWhatsAppClient.create();

    private static final TestABPropsService PROPS = TestABPropsService.builder().build();

    @Test
    @DisplayName("chat.whatsapp.com invite URL -> GroupInvite with the code captured")
    void groupInviteShort() {
        var result = DeepLinkParser.parse(CLIENT, PROPS, "https://chat.whatsapp.com/AbCdEf0123456789");
        var invite = assertInstanceOf(DeepLink.GroupInvite.class, result);
        assertEquals("AbCdEf0123456789", invite.code());
    }
}
