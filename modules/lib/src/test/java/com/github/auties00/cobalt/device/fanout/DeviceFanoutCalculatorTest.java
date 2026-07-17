package com.github.auties00.cobalt.device.fanout;

import com.github.auties00.cobalt.wire.linked.device.info.DeviceInfo;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceList;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceListBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link DeviceFanoutCalculator#calculate} and
 * {@link DeviceFanoutCalculator#filterIdentityChanges} across self-device filtering (PN and LID),
 * primary-fallback when a user has no device list, hosted-device gating via
 * {@link ABProp#ADV_ACCEPT_HOSTED_DEVICES}, multi-user merging, hosted-business coexistence, and
 * bot-JID handling.
 *
 * <p>Inputs are built in-process via {@link DeviceListBuilder} so the filter logic runs without
 * the production parse path; {@link TestABPropsService} supplies the AB props the calculator
 * consults.
 */
@DisplayName("DeviceFanoutCalculator")
class DeviceFanoutCalculatorTest {
    private static final Jid SELF_PN_USER = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid SELF_LID_USER = Jid.of("83116928594056@lid");
    private static final Jid SELF_PN_DEVICE_15 = Jid.of("19254863482:15@s.whatsapp.net");
    private static final Jid SELF_LID_DEVICE_15 = Jid.of("83116928594056:15@lid");
    private static final Jid PEER_USER = Jid.of("393495089819@s.whatsapp.net");
    private static final Jid PEER_LID_USER = Jid.of("72104938291847@lid");

    // Passing no devices yields the empty-device-list state that drives the primary-fallback branch.
    private static DeviceList list(Jid userJid, DeviceInfo... devices) {
        return new DeviceListBuilder()
                .userJid(userJid)
                .devices(List.of(devices))
                .timestamp(Instant.now())
                .currentIndex(0)
                .validIndexes(new LinkedHashSet<>())
                .build();
    }

    private static Set<DeviceList> deviceLists(DeviceList... lists) {
        return new HashSet<>(List.of(lists));
    }

    @Nested
    @DisplayName("self-device filtering")
    class SelfDeviceFiltering {
        @Test
        @DisplayName("excludes the sender's own PN device from the fanout")
        void excludesOwnPnDevice() {
            var calculator = new DeviceFanoutCalculator(TestABPropsService.builder().build());
            var selfDevices = list(SELF_PN_USER, DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(15, 1));

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(selfDevices), null);

            assertTrue(fanout.contains(Jid.of("19254863482@s.whatsapp.net")));
            assertFalse(fanout.contains(SELF_PN_DEVICE_15));
        }

        @Test
        @DisplayName("excludes the sender's own LID device from the fanout")
        void excludesOwnLidDevice() {
            var calculator = new DeviceFanoutCalculator(TestABPropsService.builder().build());
            var selfDevices = list(SELF_LID_USER, DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(15, 1));

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(selfDevices), null);

            assertTrue(fanout.contains(Jid.of("83116928594056@lid")));
            assertFalse(fanout.contains(SELF_LID_DEVICE_15));
        }

        @Test
        @DisplayName("excludes the sender across PN and LID when both addressing modes are present")
        void excludesAcrossBothAddressingModes() {
            var calculator = new DeviceFanoutCalculator(TestABPropsService.builder().build());
            var selfPnList = list(SELF_PN_USER,
                    DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(15, 1), DeviceInfo.ofE2EE(16, 2));
            var selfLidList = list(SELF_LID_USER,
                    DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(15, 1), DeviceInfo.ofE2EE(16, 2));

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(selfPnList, selfLidList), null);

            assertFalse(fanout.contains(SELF_PN_DEVICE_15));
            assertFalse(fanout.contains(SELF_LID_DEVICE_15));
            assertTrue(fanout.contains(Jid.of("19254863482:16@s.whatsapp.net")));
            assertTrue(fanout.contains(Jid.of("83116928594056:16@lid")));
        }
    }

    @Nested
    @DisplayName("primary-fallback when device list is empty")
    class PrimaryFallback {
        @Test
        @DisplayName("falls back to the primary JID when a user has no device entries")
        void primaryFallback() {
            var calculator = new DeviceFanoutCalculator(TestABPropsService.builder().build());
            var emptyList = list(PEER_USER);

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(emptyList), null);

            assertEquals(Set.of(PEER_USER.toUserJid()), fanout);
        }

        @Test
        @DisplayName("does NOT fall back to the self primary when the empty list belongs to the sender (PN)")
        void selfPrimaryNotFallback() {
            var calculator = new DeviceFanoutCalculator(TestABPropsService.builder().build());
            var emptyList = list(SELF_PN_USER);

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(emptyList), null);

            assertFalse(fanout.contains(SELF_PN_USER.toUserJid()));
        }

        @Test
        @DisplayName("does NOT fall back to the self primary when the empty list belongs to the sender (LID)")
        void selfLidPrimaryNotFallback() {
            var calculator = new DeviceFanoutCalculator(TestABPropsService.builder().build());
            var emptyList = list(SELF_LID_USER);

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(emptyList), null);

            assertFalse(fanout.contains(SELF_LID_USER.toUserJid()));
        }
    }

    @Nested
    @DisplayName("hosted-device gating")
    class HostedDeviceGating {
        @Test
        @DisplayName("excludes hosted devices when ADV_ACCEPT_HOSTED_DEVICES is off")
        void excludesHostedWhenDisabled() {
            var props = TestABPropsService.builder()
                    .with(ABProp.ADV_ACCEPT_HOSTED_DEVICES, false)
                    .build();
            var calculator = new DeviceFanoutCalculator(props);
            var peerList = list(PEER_USER, DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofHosted(2));

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(peerList), PEER_USER);

            assertTrue(fanout.contains(PEER_USER.toUserJid()));
            assertFalse(fanout.stream().anyMatch(jid -> jid.toString().contains(":99")));
        }

        @Test
        @DisplayName("includes hosted devices for a user-type chat when the AB prop is enabled")
        void includesHostedForUserChat() {
            var props = TestABPropsService.builder()
                    .with(ABProp.ADV_ACCEPT_HOSTED_DEVICES, true)
                    .build();
            var calculator = new DeviceFanoutCalculator(props);
            var peerList = list(PEER_USER, DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofHosted(2));

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(peerList), PEER_USER);

            assertTrue(fanout.stream().anyMatch(jid -> jid.toString().contains(":99")));
        }

        @Test
        @DisplayName("excludes hosted devices when the chat JID is null (group send)")
        void groupSendNeverIncludesHosted() {
            var props = TestABPropsService.builder()
                    .with(ABProp.ADV_ACCEPT_HOSTED_DEVICES, true)
                    .build();
            var calculator = new DeviceFanoutCalculator(props);
            var peerList = list(PEER_USER, DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofHosted(2));

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(peerList), null);

            assertFalse(fanout.stream().anyMatch(jid -> jid.toString().contains(":99")));
        }
    }

    @Nested
    @DisplayName("multi-user fanout")
    class MultiUserFanout {
        @Test
        @DisplayName("merges device JIDs across multiple users")
        void mergesAcrossUsers() {
            var calculator = new DeviceFanoutCalculator(TestABPropsService.builder().build());
            var peerA = list(PEER_USER, DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(1, 1));
            var peerB = list(PEER_LID_USER, DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofE2EE(3, 1));

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(peerA, peerB), null);

            assertTrue(fanout.contains(Jid.of("393495089819@s.whatsapp.net")));
            assertTrue(fanout.contains(Jid.of("393495089819:1@s.whatsapp.net")));
            assertTrue(fanout.contains(Jid.of("72104938291847@lid")));
            assertTrue(fanout.contains(Jid.of("72104938291847:3@lid")));
        }
    }

    @Nested
    @DisplayName("hosted-business coex fanout")
    class HostedCoexFanout {
        @Test
        @DisplayName("Cloud-API business (hostStorage=2) with primary-only device list: fanout = primary JID")
        void hostedPrimaryOnlyFanout() {
            var calculator = new DeviceFanoutCalculator(TestABPropsService.builder()
                    .with(ABProp.ADV_ACCEPT_HOSTED_DEVICES, true).build());
            var hostedBiz = Jid.of("15086146312@s.whatsapp.net");
            var hostedList = list(hostedBiz);

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(hostedList), hostedBiz);

            assertEquals(Set.of(hostedBiz.toUserJid()), fanout,
                    "hosted business with primary-only list fans out to the bare primary JID");
        }

        @Test
        @DisplayName("explicit hosted-id (99) is gated by ADV_ACCEPT_HOSTED_DEVICES")
        void hostedDeviceIdGatedByProp() {
            var enabled = new DeviceFanoutCalculator(TestABPropsService.builder()
                    .with(ABProp.ADV_ACCEPT_HOSTED_DEVICES, true).build());
            var disabled = new DeviceFanoutCalculator(TestABPropsService.builder()
                    .with(ABProp.ADV_ACCEPT_HOSTED_DEVICES, false).build());

            var peer = Jid.of("393495089819@s.whatsapp.net");
            var peerWithHosted = list(peer, DeviceInfo.ofE2EE(0, 0), DeviceInfo.ofHosted(2));

            var withHosted = enabled.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(peerWithHosted), peer);
            assertTrue(withHosted.stream().anyMatch(jid -> jid.toString().endsWith(":99@s.whatsapp.net")),
                    "ADV_ACCEPT_HOSTED_DEVICES=true: id=99 hosted device included");

            var withoutHosted = disabled.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(peerWithHosted), peer);
            assertFalse(withoutHosted.stream().anyMatch(jid -> jid.toString().endsWith(":99@s.whatsapp.net")),
                    "ADV_ACCEPT_HOSTED_DEVICES=false: id=99 hosted device excluded");
        }
    }

    @Nested
    @DisplayName("bot JID fanout")
    class BotJidFanout {
        @Test
        @DisplayName("fanout for a bot DM with primary-only device list returns the bare bot JID")
        void botPrimaryFanout() {
            var calculator = new DeviceFanoutCalculator(TestABPropsService.builder().build());
            var bot = Jid.of("13135550002@s.whatsapp.net");
            var botList = list(bot);

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(botList), null);

            assertEquals(Set.of(bot.toUserJid()), fanout,
                    "bot DM with no companion devices fans out to the bot's primary JID alone");
        }

        // Pins the "bot" branch of WA Web's isUser predicate: the @bot server must be treated as
        // user-shaped so an FBID @bot can be fanned out through the primary-fallback path.
        @Test
        @DisplayName("fanout for the @bot-server address resolves through the isUserJid predicate")
        void botServerAddressIsUserJid() {
            var calculator = new DeviceFanoutCalculator(TestABPropsService.builder().build());
            var botFbid = Jid.of("867051314767696@bot");
            var botList = list(botFbid);

            var fanout = calculator.calculate(SELF_PN_DEVICE_15, SELF_LID_DEVICE_15,
                    deviceLists(botList), botFbid);

            assertTrue(fanout.contains(botFbid.toUserJid()),
                    "the @bot-server JID survives the user-jid predicate and lands in the fanout");
        }
    }

    @Nested
    @DisplayName("filterIdentityChanges")
    class FilterIdentityChanges {
        @Test
        @DisplayName("returns the input unchanged when no identities have rotated")
        void noChanges() {
            var calculator = new DeviceFanoutCalculator(TestABPropsService.builder().build());
            var devices = Set.of(Jid.of("393495089819:1@s.whatsapp.net"), Jid.of("393495089819@s.whatsapp.net"));
            var filtered = calculator.filterIdentityChanges(devices, Set.of());
            assertEquals(devices, filtered);
        }

        @Test
        @DisplayName("removes devices whose identities have rotated without acknowledgement")
        void removesRotated() {
            var calculator = new DeviceFanoutCalculator(TestABPropsService.builder().build());
            var rotated = Jid.of("393495089819:1@s.whatsapp.net");
            var devices = Set.of(rotated, Jid.of("393495089819@s.whatsapp.net"));
            var filtered = calculator.filterIdentityChanges(devices, Set.of(rotated));
            assertFalse(filtered.contains(rotated));
            assertTrue(filtered.contains(Jid.of("393495089819@s.whatsapp.net")));
        }
    }
}
