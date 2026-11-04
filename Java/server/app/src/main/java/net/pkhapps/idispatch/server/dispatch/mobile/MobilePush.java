package net.pkhapps.idispatch.server.dispatch.mobile;

import jakarta.annotation.PreDestroy;
import net.pkhapps.idispatch.server.dispatch.MobileDestination;
import net.pkhapps.idispatch.server.dispatch.MobilePushSubscriptions;
import net.pkhapps.idispatch.server.dispatch.PushSubscription;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends the dispatch notifications to the mobile apps by Web Push, which reaches an app also when it is in the
 * background or closed: the app's service worker shows the alert as a notification, stores it and confirms it. Sent
 * next to the event stream, which an app in the foreground gets the alert from; the app shows an alert that comes both
 * ways once.
 * <p>
 * The server identifies itself to the push services with its VAPID key pair, {@code idispatch.mobile.push.public-key}
 * and {@code idispatch.mobile.push.private-key}; without them, the apps can't subscribe and nothing is pushed. A push
 * service keeps trying to deliver a message for {@code idispatch.mobile.push.time-to-live}, after which the alert is
 * no longer news and the app gets it from the event stream when it next opens.
 * <p>
 * Messages are posted on threads of their own, so that a slow push service never holds up the dispatcher. One that the
 * push service refuses is not tried again; a subscription that the push service says has expired is removed.
 */
@Component
class MobilePush {

    private static final Logger log = LoggerFactory.getLogger(MobilePush.class);

    /**
     * Posts a message to its push service and returns the HTTP status of the answer.
     */
    @FunctionalInterface
    interface Transport {
        int post(WebPushMessage message) throws IOException, InterruptedException;
    }

    /**
     * What the service worker gets: the alert, and the language to show it in.
     */
    record Payload(String locale, NotificationResponse notification) {
    }

    private final @Nullable VapidKeys vapidKeys;
    private final @Nullable String subject;
    private final Duration timeToLive;
    private final MobilePushSubscriptions subscriptions;
    private final JsonMapper jsonMapper;
    private final Transport transport;
    private final Clock clock;
    private final ExecutorService sender = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
            .name("mobile-push-", 0).factory());

    @Autowired
    MobilePush(@Value("${idispatch.mobile.push.public-key:}") String publicKey,
               @Value("${idispatch.mobile.push.private-key:}") String privateKey,
               @Value("${idispatch.mobile.push.subject:}") String subject,
               @Value("${idispatch.mobile.push.time-to-live:1h}") Duration timeToLive,
               MobilePushSubscriptions subscriptions, JsonMapper jsonMapper) {
        this(vapidKeys(publicKey, privateKey), subject.isBlank() ? null : subject.trim(), timeToLive, subscriptions,
                jsonMapper, httpTransport(), Clock.systemUTC());
    }

    MobilePush(@Nullable VapidKeys vapidKeys, @Nullable String subject, Duration timeToLive,
               MobilePushSubscriptions subscriptions, JsonMapper jsonMapper, Transport transport, Clock clock) {
        this.vapidKeys = vapidKeys;
        this.subject = subject;
        this.timeToLive = timeToLive;
        this.subscriptions = subscriptions;
        this.jsonMapper = jsonMapper;
        this.transport = transport;
        this.clock = clock;
        if (vapidKeys == null) {
            log.warn("Web Push is off, as idispatch.mobile.push.public-key and idispatch.mobile.push.private-key are "
                     + "not set: the mobile apps get alerts only while they are open. See docs/deployment.md.");
        }
    }

    private static @Nullable VapidKeys vapidKeys(String publicKey, String privateKey) {
        if (publicKey.isBlank() && privateKey.isBlank()) {
            return null;
        }
        if (publicKey.isBlank() || privateKey.isBlank()) {
            throw new IllegalStateException("Set both idispatch.mobile.push.public-key and "
                                            + "idispatch.mobile.push.private-key, or neither");
        }
        try {
            return VapidKeys.of(publicKey, privateKey);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("idispatch.mobile.push.public-key and idispatch.mobile.push.private-key "
                                            + "are not a VAPID key pair", ex);
        }
    }

    /**
     * The public key the apps subscribe with, or empty if Web Push is off.
     */
    Optional<String> publicKey() {
        return Optional.ofNullable(vapidKeys).map(VapidKeys::publicKeyBase64);
    }

    /**
     * Pushes the notification to the destination's app, if it has subscribed, on a thread of its own.
     */
    void send(MobileDestination destination, NotificationResponse notification) {
        if (vapidKeys == null) {
            log.warn("Not pushing notification {} to mobile destination {}, as Web Push is off", notification.id(),
                    destination.id().value());
            return;
        }
        sender.execute(() -> {
            try {
                subscriptions.find(destination.id()).ifPresentOrElse(
                        subscription -> push(destination, subscription, notification),
                        () -> log.debug("Mobile destination {} has no push subscription", destination.id().value()));
            } catch (RuntimeException ex) {
                log.error("Could not push notification {} to mobile destination {}", notification.id(),
                        destination.id().value(), ex);
            }
        });
    }

    private void push(MobileDestination destination, PushSubscription subscription,
                      NotificationResponse notification) {
        var message = WebPushMessage.of(subscription, payload(destination, notification), vapidKeys, subject,
                timeToLive, clock.instant());
        int status;
        try {
            status = transport.post(message);
        } catch (IOException ex) {
            log.warn("Could not reach the push service of mobile destination {}: {}", destination.id().value(),
                    ex.toString());
            return;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
        if (status >= 200 && status < 300) {
            log.debug("Pushed notification {} to mobile destination {}", notification.id(), destination.id().value());
        } else if (status == 404 || status == 410) {
            log.info("The push subscription of mobile destination {} has expired; removing it",
                    destination.id().value());
            subscriptions.expire(destination.id(), subscription);
        } else {
            log.warn("The push service refused notification {} to mobile destination {} with {}", notification.id(),
                    destination.id().value(), status);
        }
    }

    /**
     * The payload, with the description shortened if it would not otherwise fit in a push message.
     */
    byte[] payload(MobileDestination destination, NotificationResponse notification) {
        var locale = destination.locale().getLanguage();
        var payload = json(new Payload(locale, notification));
        var excess = payload.length - WebPushMessage.MAX_PAYLOAD;
        if (excess <= 0) {
            return payload;
        }
        var description = notification.description();
        // Shortening by as many characters as there are bytes too many is enough, as no character is less than a byte
        var shortened = description.substring(0, Math.max(0, description.length() - excess - 1)) + "…";
        return json(new Payload(locale, new NotificationResponse(notification.id(), notification.assignmentId(),
                notification.timestamp(), notification.urgency(), notification.typeCode(), notification.type(),
                notification.municipality(), notification.address(), shortened, notification.resources())));
    }

    private byte[] json(Payload payload) {
        return jsonMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
    }

    private static Transport httpTransport() {
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        return message -> {
            var request = HttpRequest.newBuilder(message.endpoint())
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(message.body()));
            message.headers().forEach(request::header);
            return client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
        };
    }

    @PreDestroy
    void close() {
        sender.shutdownNow();
    }
}
