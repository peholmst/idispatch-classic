package net.pkhapps.idispatch.runboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * The runboard's version, which Maven writes into {@code version.properties} when it builds the runboard.
 */
final class Version {

    private static final Logger logger = LoggerFactory.getLogger(Version.class);

    private static final String VERSION = load();

    private Version() {
    }

    /**
     * Returns the version, such as {@code 3.1.0}, or an empty string if it isn't known.
     */
    static String get() {
        return VERSION;
    }

    private static String load() {
        var properties = new Properties();
        try (var in = Version.class.getResourceAsStream("version.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException ex) {
            logger.warn("Could not read the version", ex);
        }
        var version = properties.getProperty("version", "");
        // Not filled in when the resources were copied by something other than Maven, such as an IDE
        return version.startsWith("@") ? "" : version;
    }
}
