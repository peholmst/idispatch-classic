package net.pkhapps.idispatch.server.dispatch.mobile;

import net.pkhapps.idispatch.server.dispatch.MobileDestination;
import net.pkhapps.idispatch.server.dispatch.MobilePushSubscriptions;
import net.pkhapps.idispatch.server.dispatch.PushSubscription;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;

/**
 * The API of the mobile app, for the unit of the destination whose key the app sent (see {@link MobileSecurityConfig}).
 * The unit is always the destination's; nothing the app sends can name another.
 */
@RestController
@RequestMapping(MobileSecurityConfig.API_PATH)
class MobileApiController {

    private static final Logger log = LoggerFactory.getLogger(MobileApiController.class);

    private final UnitResponses unitResponses;
    private final ResourceStatusService resourceStatusService;
    private final MobileEventStreams eventStreams;
    private final MobileDispatcher dispatcher;
    private final MobilePush push;
    private final MobilePushSubscriptions pushSubscriptions;

    MobileApiController(UnitResponses unitResponses, ResourceStatusService resourceStatusService,
                        MobileEventStreams eventStreams, MobileDispatcher dispatcher, MobilePush push,
                        MobilePushSubscriptions pushSubscriptions) {
        this.unitResponses = unitResponses;
        this.resourceStatusService = resourceStatusService;
        this.eventStreams = eventStreams;
        this.dispatcher = dispatcher;
        this.push = push;
        this.pushSubscriptions = pushSubscriptions;
    }

    /**
     * The unit the app is paired with, its current status, and the states it may set next.
     */
    @GetMapping("/unit")
    UnitResponse unit(@AuthenticationPrincipal MobileDestination destination) {
        return unitResponses.of(destination);
    }

    /**
     * What the app asks the unit's state to become.
     */
    record StatusChange(String state) {
    }

    /**
     * Sets the state of the unit, if it may follow the current one, the way the dispatcher's status buttons do.
     *
     * @return the unit with its new status, or with its current one and 409 Conflict if the state may not follow it
     */
    @PostMapping("/unit/status")
    ResponseEntity<UnitResponse> changeStatus(@AuthenticationPrincipal MobileDestination destination,
                                              @RequestBody StatusChange change) {
        var state = Arrays.stream(ResourceState.values())
                .filter(candidate -> candidate.name().equals(change.state()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown state"));
        return resourceStatusService.setResourceStateManually(destination.resource(), state)
                .map(status -> ResponseEntity.ok(unitResponses.of(destination, status)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).body(unitResponses.of(destination)));
    }

    /**
     * Answers a request that fails, such as one with an unknown state or a body that isn't JSON, with its status and no
     * body. Left to Spring Boot, the error would go to {@code /error}, which is behind the Keycloak login, and the app
     * would get a redirect to the login page instead.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Void> failed(Exception ex) {
        HttpStatusCode status;
        if (ex instanceof ErrorResponse error) {
            status = error.getStatusCode();
        } else if (ex instanceof HttpMessageNotReadableException) {
            status = HttpStatus.BAD_REQUEST;
        } else {
            log.error("The mobile API failed", ex);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).build();
    }

    /**
     * A stream of Server-Sent Events about the unit, starting with its current status; see {@link MobileEventStreams}.
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<SseEmitter> events(Authentication authentication) {
        var app = (MobileAppAuthentication) authentication;
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                // Tells NGINX to pass each event on right away instead of buffering the response
                .header("X-Accel-Buffering", "no")
                .body(eventStreams.open(app.getPrincipal(), app.keyHash(),
                        () -> dispatcher.waitingFor(app.getPrincipal())));
    }

    /**
     * Confirms that the app has received and stored the notification, which stores a receipt and stops it from being
     * sent again. Confirming it again does nothing.
     *
     * @return 204, or 404 if the notification was not sent to the app's destination
     */
    @PutMapping("/notifications/{id}/receipt")
    ResponseEntity<Void> confirm(@AuthenticationPrincipal MobileDestination destination, @PathVariable long id) {
        return dispatcher.confirm(destination, id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * The server's public VAPID key, which the app subscribes to Web Push with.
     */
    record PushKey(String publicKey) {
    }

    /**
     * The public key the app subscribes to Web Push with.
     *
     * @return the key, or 404 if the server has no VAPID keys, and so sends no pushes
     */
    @GetMapping("/push/key")
    ResponseEntity<PushKey> pushKey() {
        return push.publicKey()
                .map(key -> ResponseEntity.ok(new PushKey(key)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * A push subscription as the browser's {@code PushSubscription.toJSON()} gives it.
     */
    record PushSubscriptionRequest(String endpoint, Keys keys) {

        record Keys(String p256dh, String auth) {
        }
    }

    /**
     * Stores the app's push subscription, replacing any earlier one of the destination. The subscription belongs to
     * the key the app sent, and a new key for the destination removes it.
     *
     * @return 204, or 400 if it is not a valid subscription
     */
    @PutMapping("/push/subscription")
    ResponseEntity<Void> subscribe(Authentication authentication, @RequestBody PushSubscriptionRequest request) {
        var app = (MobileAppAuthentication) authentication;
        var keys = request.keys();
        if (keys == null || !PushSubscription.isValid(request.endpoint(), keys.p256dh(), keys.auth())) {
            return ResponseEntity.badRequest().build();
        }
        var subscription = PushSubscription.of(request.endpoint(), keys.p256dh(), keys.auth());
        log.debug("Mobile destination {} subscribes to Web Push", app.getPrincipal().id().value());
        return pushSubscriptions.subscribe(app.keyHash(), subscription)
                ? ResponseEntity.noContent().build()
                // The key was replaced after the request was authenticated
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Removes the app's push subscription, if it has one.
     */
    @DeleteMapping("/push/subscription")
    ResponseEntity<Void> unsubscribe(Authentication authentication) {
        pushSubscriptions.unsubscribe(((MobileAppAuthentication) authentication).keyHash());
        return ResponseEntity.noContent().build();
    }
}
