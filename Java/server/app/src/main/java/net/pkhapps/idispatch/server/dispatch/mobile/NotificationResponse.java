package net.pkhapps.idispatch.server.dispatch.mobile;

import java.time.Instant;
import java.util.List;

/**
 * A dispatch notification as the mobile app gets it, in the destination's language.
 *
 * @param id          the notification's ID, which the app confirms receiving it with
 * @param timestamp   when it was sent
 * @param typeCode    the code of the assignment type, such as {@code 402}
 * @param type        the description of the assignment type
 * @param resources   the call signs of every resource on the assignment when it was sent, in call sign order
 */
record NotificationResponse(long id, Instant timestamp, String urgency, String typeCode, String type,
                            String municipality, String address, String description, List<String> resources) {
}
