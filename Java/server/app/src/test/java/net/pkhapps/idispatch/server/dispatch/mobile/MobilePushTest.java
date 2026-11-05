package net.pkhapps.idispatch.server.dispatch.mobile;

import net.pkhapps.idispatch.server.dispatch.DestinationId;
import net.pkhapps.idispatch.server.dispatch.MobileDestination;
import net.pkhapps.idispatch.server.dispatch.MobilePushSubscriptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A dispatch notification is pushed to the app that has subscribed, encrypted for it, and an expired subscription is
 * removed.
 */
class MobilePushTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final MobilePushSubscriptions subscriptions = mock(MobilePushSubscriptions.class);
    private final MobileDestination destination = mock(MobileDestination.class);
    private final WebPushMessageTest.App app = WebPushMessageTest.App.create();
    private final CompletableFuture<WebPushMessage> posted = new CompletableFuture<>();
    private int status = 201;

    @BeforeEach
    void destination() {
        when(destination.id()).thenReturn(new DestinationId(42));
        when(destination.locale()).thenReturn(Locale.of("sv"));
    }

    private MobilePush push(VapidKeys keys) {
        return new MobilePush(keys, "mailto:admin@example.com", Duration.ofHours(1), subscriptions, jsonMapper,
                message -> {
                    posted.complete(message);
                    return status;
                }, Clock.systemUTC());
    }

    @Test
    void appThatHasSubscribedGetsTheNotification_inItsLanguage() throws Exception {
        when(subscriptions.find(new DestinationId(42))).thenReturn(Optional.of(app.subscription()));

        push(VapidKeys.generate()).send(destination, notification("Fire in the barn"));

        var message = posted.get(5, TimeUnit.SECONDS);
        var payload = jsonMapper.readTree(new String(app.decrypt(message.body()), StandardCharsets.UTF_8));
        assertThat(payload.get("locale").asString()).isEqualTo("sv");
        assertThat(payload.get("notification").get("id").asLong()).isEqualTo(7);
        assertThat(payload.get("notification").get("description").asString()).isEqualTo("Fire in the barn");
        assertThat(message.headers()).containsEntry("Urgency", "high");
    }

    @Test
    void appWithoutASubscriptionGetsNothing() throws Exception {
        when(subscriptions.find(new DestinationId(42))).thenReturn(Optional.empty());

        push(VapidKeys.generate()).send(destination, notification(""));

        verify(subscriptions, timeout(5000)).find(new DestinationId(42));
        assertThat(posted).isNotDone();
    }

    @Test
    void withoutVapidKeysNothingIsPushed() {
        var push = push(null);

        push.send(destination, notification(""));

        assertThat(push.publicKey()).isEmpty();
        verify(subscriptions, after(200).never()).find(new DestinationId(42));
    }

    @Test
    void subscriptionThatThePushServiceSaysIsGoneIsRemoved() throws Exception {
        when(subscriptions.find(new DestinationId(42))).thenReturn(Optional.of(app.subscription()));
        status = 410;

        push(VapidKeys.generate()).send(destination, notification(""));

        verify(subscriptions, timeout(5000)).expire(new DestinationId(42), app.subscription());
    }

    @Test
    void refusedPushKeepsTheSubscription() throws Exception {
        when(subscriptions.find(new DestinationId(42))).thenReturn(Optional.of(app.subscription()));
        status = 429;

        push(VapidKeys.generate()).send(destination, notification(""));

        posted.get(5, TimeUnit.SECONDS);
        verify(subscriptions, after(200).never()).expire(new DestinationId(42), app.subscription());
    }

    @Test
    void descriptionTooLongForAPushIsShortened() {
        var payload = push(VapidKeys.generate()).payload(destination, notification("ä".repeat(3000)));

        assertThat(payload.length).isLessThanOrEqualTo(WebPushMessage.MAX_PAYLOAD);
        var description = jsonMapper.readTree(new String(payload, StandardCharsets.UTF_8))
                .get("notification").get("description").asString();
        assertThat(description).startsWith("ää").endsWith("…").hasSizeLessThan(3000);
    }

    private static NotificationResponse notification(String description) {
        return new NotificationResponse(7, 3, Instant.parse("2026-11-04T12:00:00Z"), "A", "402", "Building fire",
                "Turku", "Main Street 1", description, List.of("RVS11"));
    }
}
