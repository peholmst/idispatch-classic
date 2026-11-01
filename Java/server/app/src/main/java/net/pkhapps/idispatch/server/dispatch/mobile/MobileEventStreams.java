package net.pkhapps.idispatch.server.dispatch.mobile;

import jakarta.annotation.PreDestroy;
import net.pkhapps.idispatch.server.dispatch.DestinationId;
import net.pkhapps.idispatch.server.dispatch.DestinationQueries;
import net.pkhapps.idispatch.server.dispatch.MobileDestination;
import net.pkhapps.idispatch.server.dispatch.MobileKeyHash;
import net.pkhapps.idispatch.server.resource.ResourceStatusChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * The open event streams of the mobile apps. A stream starts with a {@value #UNIT_EVENT} event of the unit's current
 * status and a {@value #NOTIFICATION_EVENT} event for each notification waiting for the app, and then gets a
 * {@value #UNIT_EVENT} event whenever the unit's status changes, whoever changed it, and a
 * {@value #NOTIFICATION_EVENT} event for each new notification (see {@link MobileDispatcher}). The app treats an open
 * stream as the server being reachable.
 * <p>
 * Every {@code idispatch.mobile.heartbeat-interval} each stream gets a comment, which keeps proxies from closing it and
 * shows when the app is gone, and is checked against the destination: a stream whose destination has been deleted or
 * has a new key is closed, so that the app's next connection is refused, and a destination whose details changed sends
 * the unit again. A stream also ends after {@code idispatch.mobile.stream-timeout}, and the app connects again.
 * <p>
 * Events are sent one at a time, in the order they happened, on a thread of their own, so that a slow app never holds
 * up the dispatcher whose change caused the event.
 * <p>
 * When the server shuts down, the streams are closed before the web server's graceful shutdown starts, which would
 * otherwise wait for them, as for any open request, until it gives up. The apps connect again once the server is back.
 */
@Component
class MobileEventStreams implements SmartLifecycle {

    static final String UNIT_EVENT = "unit";
    static final String NOTIFICATION_EVENT = "notification";

    private static final Logger log = LoggerFactory.getLogger(MobileEventStreams.class);

    private final UnitResponses unitResponses;
    private final DestinationQueries destinationQueries;
    private final Duration timeout;
    private final Set<Stream> streams = ConcurrentHashMap.newKeySet();
    private final ExecutorService sender = Executors.newSingleThreadExecutor(Thread.ofVirtual()
            .name("mobile-events").factory());

    MobileEventStreams(UnitResponses unitResponses, DestinationQueries destinationQueries,
                       @Value("${idispatch.mobile.stream-timeout:30m}") Duration timeout) {
        this.unitResponses = unitResponses;
        this.destinationQueries = destinationQueries;
        this.timeout = timeout;
    }

    /**
     * An open stream, and the destination as it was when the stream was last checked.
     */
    private static final class Stream {

        final SseEmitter emitter;
        final MobileKeyHash keyHash;
        volatile MobileDestination destination;

        Stream(SseEmitter emitter, MobileKeyHash keyHash, MobileDestination destination) {
            this.emitter = emitter;
            this.keyHash = keyHash;
            this.destination = destination;
        }
    }

    /**
     * Opens a stream for the app of the destination, which authenticated with the key of the given hash.
     *
     * @param waiting the notifications waiting for the app, looked up once the stream is open, so that a notification
     *                sent meanwhile is not missed; one sent at that very moment may come twice, which the app expects
     */
    SseEmitter open(MobileDestination destination, MobileKeyHash keyHash,
                    Supplier<List<NotificationResponse>> waiting) {
        var stream = new Stream(new SseEmitter(timeout.toMillis()), keyHash, destination);
        streams.add(stream);
        stream.emitter.onCompletion(() -> streams.remove(stream));
        stream.emitter.onTimeout(() -> streams.remove(stream));
        stream.emitter.onError(error -> streams.remove(stream));
        log.debug("Mobile destination {} opened an event stream", destination.id().value());
        sendUnit(stream, unitResponses.of(destination));
        waiting.get().forEach(notification -> sendNotification(stream, notification));
        return stream.emitter;
    }

    /**
     * Sends the notification to every open stream of the destination.
     */
    void sendNotification(DestinationId destination, NotificationResponse notification) {
        streams.stream()
                .filter(stream -> stream.destination.id().equals(destination))
                .forEach(stream -> sendNotification(stream, notification));
    }

    private void sendNotification(Stream stream, NotificationResponse notification) {
        send(stream, SseEmitter.event().name(NOTIFICATION_EVENT).id(String.valueOf(notification.id()))
                .data(notification));
    }

    @EventListener
    void onResourceStatusChanged(ResourceStatusChanged event) {
        var status = event.getResourceStatus();
        streams.stream()
                .filter(stream -> stream.destination.resource().equals(status.resourceId()))
                .forEach(stream -> sendUnit(stream, unitResponses.of(stream.destination, status)));
    }

    @Scheduled(fixedDelayString = "${idispatch.mobile.heartbeat-interval:20s}")
    void heartbeat() {
        for (var stream : streams) {
            var current = destinationQueries.findActiveMobile(stream.keyHash);
            if (current.isEmpty()) {
                log.debug("Closing the event stream of mobile destination {}, which was deleted or got a new key",
                        stream.destination.id().value());
                streams.remove(stream);
                sender.execute(stream.emitter::complete);
            } else if (!current.get().equals(stream.destination)) {
                stream.destination = current.get();
                sendUnit(stream, unitResponses.of(stream.destination));
            } else {
                send(stream, SseEmitter.event().comment("heartbeat"));
            }
        }
    }

    private void sendUnit(Stream stream, UnitResponse unit) {
        send(stream, SseEmitter.event().name(UNIT_EVENT).data(unit));
    }

    private void send(Stream stream, SseEmitter.SseEventBuilder event) {
        sender.execute(() -> {
            try {
                stream.emitter.send(event);
            } catch (IOException | IllegalStateException ex) {
                // The app is gone, or the stream already ended
                log.debug("Could not send to the event stream of mobile destination {}: {}",
                        stream.destination.id().value(), ex.getMessage());
                streams.remove(stream);
            }
        });
    }

    private volatile boolean running;

    @Override
    public void start() {
        running = true;
    }

    /**
     * Closes every stream. Runs before the web server's graceful shutdown, whose phase is lower.
     */
    @Override
    public void stop() {
        running = false;
        log.debug("Closing {} mobile event streams for shutdown", streams.size());
        streams.forEach(stream -> {
            streams.remove(stream);
            stream.emitter.complete();
        });
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @PreDestroy
    void close() {
        sender.shutdownNow();
    }
}
