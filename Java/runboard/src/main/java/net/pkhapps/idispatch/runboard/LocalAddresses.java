package net.pkhapps.idispatch.runboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Finds the addresses this machine can be reached at, so that they can be shown on the screen for whoever needs to log
 * in over SSH for maintenance.
 */
final class LocalAddresses {

    private static final Logger logger = LoggerFactory.getLogger(LocalAddresses.class);

    private LocalAddresses() {
    }

    /**
     * Returns the IPv4 addresses of the network interfaces that are up, joined into one line, or an empty string if
     * there are none.
     */
    static String lookup() {
        var addresses = new ArrayList<InetAddress>();
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                var networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback() && !networkInterface.isVirtual()) {
                    networkInterface.getInetAddresses().asIterator().forEachRemaining(addresses::add);
                }
            }
        } catch (SocketException ex) {
            logger.warn("Could not look up the network addresses", ex);
        }
        return format(addresses);
    }

    /**
     * Joins the IPv4 addresses that can be reached from another machine into one line. Loopback and link-local
     * addresses, and IPv6 addresses, which are too long to read off a screen and type in, are left out.
     */
    static String format(Collection<InetAddress> addresses) {
        return addresses.stream()
                .filter(address -> address instanceof Inet4Address)
                .filter(address -> !address.isLoopbackAddress() && !address.isLinkLocalAddress())
                .map(InetAddress::getHostAddress)
                .distinct()
                .collect(Collectors.joining("  "));
    }

}
