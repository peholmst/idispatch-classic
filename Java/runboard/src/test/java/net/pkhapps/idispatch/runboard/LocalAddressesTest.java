package net.pkhapps.idispatch.runboard;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalAddressesTest {

    @Test
    void format_joinsTheIpv4Addresses() {
        assertEquals("192.168.1.20  10.0.0.5", LocalAddresses.format(addresses("192.168.1.20", "10.0.0.5")));
    }

    @Test
    void format_leavesOutLoopbackLinkLocalAndIpv6Addresses() {
        assertEquals("192.168.1.20", LocalAddresses.format(
                addresses("127.0.0.1", "169.254.10.1", "fe80::1", "2001:db8::1", "192.168.1.20")));
    }

    @Test
    void format_showsEachAddressOnce() {
        assertEquals("192.168.1.20", LocalAddresses.format(addresses("192.168.1.20", "192.168.1.20")));
    }

    @Test
    void format_isEmptyWithoutAddresses() {
        assertEquals("", LocalAddresses.format(addresses("127.0.0.1")));
    }

    private static List<InetAddress> addresses(String... literals) {
        var result = new ArrayList<InetAddress>();
        for (var literal : literals) {
            try {
                result.add(InetAddress.getByName(literal));
            } catch (UnknownHostException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
        return result;
    }
}
